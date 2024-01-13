package com.airepublic.bmstoinverter.protocol.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.protocol.rs485.RS485;
import com.airepublic.bmstoinverter.core.protocol.rs485.RS485Port;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

/**
 * The implementation of the {@link RS485Port} using the JSerialComm implementation.
 */
@RS485
public class JSerialCommPort extends RS485Port implements SerialPortDataListener {
    private final static Logger LOG = LoggerFactory.getLogger(JSerialCommPort.class);
    private SerialPort port;
    private final ConcurrentLinkedQueue<byte[]> queue = new ConcurrentLinkedQueue<>();

    /**
     * Constructor.
     */
    public JSerialCommPort() {
    }


    /**
     * Constructor.
     * 
     * @param portname the portname
     */
    public JSerialCommPort(final String portname) {
        super(portname);
    }


    @Override
    protected Port create(final String portname) {
        final JSerialCommPort port = new JSerialCommPort(portname);
        port.init();
        return port;
    }


    @Override
    public synchronized void open() throws IOException {
        if (!isOpen()) {
            try {
                port = SerialPort.getCommPort(getPortname());
                // set port configuration
                port.setComPortParameters(getBaudrate(), 8, 1, SerialPort.NO_PARITY, true);
                port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING |
                        SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
                // port.setRs485ModeParameters(true, true, true, false, 100000, 100000);
                // port.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED |
                // SerialPort.FLOW_CONTROL_CTS_ENABLED);
                // open port

                port.addDataListener(this);
                port.openPort();

            } catch (final Exception e) {
                LOG.error("Could not open port {}!", getPortname(), e);
                try {
                    port.closePort();
                    port.openPort();
                } catch (final Throwable t) {
                }
            }

        }
    }


    @Override
    public boolean isOpen() {
        return port != null && port.isOpen();
    }


    @Override
    public void close() {
        if (isOpen()) {
            try {
                port.removeDataListener();
                port.closePort();
                LOG.info("Shutting down port '{}'...OK", getPortname());
            } catch (final Throwable e) {
                LOG.error("Shutting down port '{}'...FAILED", getPortname(), e);
            }
        }

        port = null;
    }


    @Override
    public ByteBuffer receiveFrame(final Predicate<byte[]> validator) throws IOException {
        ensureOpen();

        LOG.debug("RX queue: {}", queue);
        final byte[] bytes = queue.poll();
        // int failureCount = 0;
        //
        // while (bytes == null && failureCount < 10) {
        // failureCount++;
        // // if the queue is empty we have to wait for the next bytes
        // try {
        // synchronized (queue) {
        // queue.wait(500);
        // }
        // } catch (final InterruptedException e) {
        // }
        //
        // bytes = queue.poll();
        // }

        if (bytes == null) {
            return null;
        }
        return ByteBuffer.wrap(bytes);
    }


    @Override
    public void sendFrame(final ByteBuffer frame) throws IOException {
        ensureOpen();

        final byte[] bytes = frame.array();
        LOG.debug("Send: {}", Port.printBytes(bytes));
        // while (!port.getRTS() && !port.setRTS()) {
        // ;
        // }

        port.getOutputStream().write(bytes);
        port.getOutputStream().flush();

        // while (port.getRTS() && !port.clearRTS()) {
        // ;
        // }

        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
        }
    }


    /**
     * Gets the queue of frames.
     *
     * @return queue of frames
     */
    public Queue<byte[]> getQueue() {
        return queue;
    }


    @Override
    public void clearBuffers() {
        synchronized (queue) {
            LOG.debug("Clearing RX buffers");
            queue.clear();
            getFrameBuffer().clear();

            if (isOpen()) {
                port.flushIOBuffers();
            }
        }
    }


    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }


    @Override
    public void serialEvent(final SerialPortEvent event) {
        if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
            final byte[] bytes = event.getReceivedData();

            LOG.debug("Received: {}", Port.printBytes(bytes));

            if (bytes != null) {
                final ByteBuffer frameBuffer = getFrameBuffer();

                synchronized (frameBuffer) {
                    // check if the bytes still fit into the framebuffer
                    if (bytes.length <= frameBuffer.remaining()) {
                        frameBuffer.put(bytes);

                        // check if the framebuffer is full
                        if (frameBuffer.remaining() == 0) {
                            // add the frame to the queue
                            addFrameBufferToQueue();
                        }
                    } else {
                        int idx = 0;

                        // first fill up the current framebuffer
                        if (frameBuffer.remaining() != 0) {
                            idx += frameBuffer.remaining();
                            frameBuffer.put(bytes, 0, frameBuffer.remaining());

                            addFrameBufferToQueue();
                        }

                        // then add all complete frames
                        while (bytes.length - idx >= getFrameLength()) {
                            // put a complete frame into the framebuffer
                            frameBuffer.put(bytes, idx, getFrameLength());

                            idx += getFrameLength();

                            // add the frame to the queue
                            addFrameBufferToQueue();
                        }

                        // if bytes are left over put them in the framebuffer
                        if (idx < bytes.length) {
                            // check if the bytes fit into the remaining buffer (could happen if
                            // startflag was not at first position)
                            if (bytes.length - idx > frameBuffer.remaining()) {
                                final int remaining = frameBuffer.remaining();
                                frameBuffer.put(bytes, idx, frameBuffer.remaining());
                                idx += remaining;

                                // add the frame to the queue
                                addFrameBufferToQueue();
                            }

                            if (idx < bytes.length) {
                                // add the remaining bytes
                                frameBuffer.put(bytes, idx, bytes.length - idx);
                            }
                        }
                    }
                }
            }

        } else {
            LOG.debug("Error unknown serial event: " + event);
        }
    }


    private void addFrameBufferToQueue() {
        synchronized (getFrameBuffer()) {
            int start = 0;
            while (start < getFrameLength() && Byte.toUnsignedInt(getFrameBuffer().get(start)) != getStartFlag()) {
                start++;
            }

            if (start > 0) {
                final byte[] tmp = new byte[getFrameLength() - start];
                System.arraycopy(getFrameBuffer().array(), start, tmp, 0, tmp.length);
                System.arraycopy(tmp, 0, getFrameBuffer().array(), 0, tmp.length);
                getFrameBuffer().position(tmp.length);
                return;
            }

            final byte[] frame = new byte[getFrameLength()];
            System.arraycopy(getFrameBuffer().array(), 0, frame, 0, getFrameLength());

            LOG.debug("Adding frame to TX queue: {}", printBytes(frame));
            synchronized (queue) {
                queue.add(frame);
                queue.notify();
            }

            // clear the framebuffer
            getFrameBuffer().clear();
        }
    }
}
