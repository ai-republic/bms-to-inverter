package com.airepublic.bmstoinverter.comm.rs485.jserialcomm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.Port;
import com.airepublic.bmstoinverter.rs485.RS485;
import com.airepublic.bmstoinverter.rs485.RS485Port;
import com.fazecast.jSerialComm.SerialPort;

@RS485
public class JSerialCommPort extends RS485Port {
    private final static Logger LOG = LoggerFactory.getLogger(JSerialCommPort.class);
    private SerialPort port;

    @Override
    public void open() throws IOException {
        try {
            port = SerialPort.getCommPort(getPortname());
            // set port configuration
            port.setComPortParameters(getBaudrate(), 8, 1, SerialPort.NO_PARITY, true);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
            // open port
            port.openPort();

        } catch (final Exception e) {
            LOG.error("Could not open port {}!", getPortname(), e);
        }
    }


    @Override
    public boolean isOpen() {

        return port != null && port.isOpen();
    }


    @Override
    public void close() throws IOException {
        try {
            port.closePort();
        } catch (final Exception e) {
            throw new IOException("Port could not be closed!", e);
        }
    }


    @Override
    public ByteBuffer receiveFrame(final Predicate<byte[]> validator) throws IOException {

        // read frame
        final byte[] bytes = new byte[getFrameLength()];
        final int read = port.getInputStream().read(bytes);

        LOG.debug("Initial read: {}", Port.printBytes(bytes));

        if (read != bytes.length) {
            throw new IOException("Wrong number of bytes read!");
        }

        do {
            int startPos = 0;

            // verify it starts with the startflag
            if (bytes[0] != (byte) getStartFlag()) {
                // otherwise search within the received bytes for a startflag
                do {
                    startPos++;
                } while (startPos < bytes.length && bytes[startPos] != (byte) getStartFlag());

                // if a startflag was found
                if (startPos < bytes.length && bytes[startPos] == (byte) getStartFlag()) {
                    // shift the bytes left from the found startflag
                    System.arraycopy(bytes, startPos, bytes, 0, bytes.length - startPos);
                    LOG.debug("Copied startflag to beginning: {}", Port.printBytes(bytes));

                    // fill up the rest with the next bytes from the stream
                    startPos = bytes.length - startPos;
                    port.getInputStream().read(bytes, startPos, bytes.length - startPos);

                    LOG.debug("Final filling up:  {}", Port.printBytes(bytes));
                } else {
                    // otherwise continue to read the next frame
                    LOG.debug("Ignoring frame: {}", Port.printBytes(bytes));
                    return receiveFrame(validator);
                }
            }

            // now we should have a frame starting with the start flag
            // next is to validate the frame

            if (!validator.test(bytes)) {
                LOG.debug("Validation of frame failed!");

                // read the bytes from pos 1 to find possible other startflag
                // and remove the startflag as first byte
                bytes[0] = 0;
            }
        } while (bytes[0] != (byte) getStartFlag());

        return ByteBuffer.wrap(bytes);
    }


    @Override
    public void sendFrame(final ByteBuffer frame) throws IOException {
        final byte[] bytes = frame.array();
        LOG.debug("Send: {}", Port.printBytes(bytes));
        while (!port.getRTS() && !port.setRTS()) {
            ;
        }
        port.getOutputStream().write(bytes);
        port.getOutputStream().flush();
        while (port.getRTS() && !port.clearRTS()) {
            ;
        }
    }
}
