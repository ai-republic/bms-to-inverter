package com.airepublic.bmstoinverter.protocol.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.protocol.can.CAN;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import tel.schich.javacan.CanChannels;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.RawCanChannel;

/**
 * The implementation of the {@link CANPort} using the javacan implementation.
 */
@CAN
public class JavaCANPort extends CANPort {
    private final static Logger LOG = LoggerFactory.getLogger(JavaCANPort.class);
    private RawCanChannel canChannel;

    /**
     * Constructor.
     */
    public JavaCANPort() {
    }


    /**
     * Constructor.
     *
     * @param portname the portname
     */
    public JavaCANPort(final String portname) {
        super(portname);
    }


    @Override
    public JavaCANPort create(final String portname) {
        return new JavaCANPort(portname);
    }


    @Override
    public void open() throws Exception {
        // close old channel first
        if (canChannel != null) {
            close();
        }

        canChannel = CanChannels.newRawChannel(getPortname());
    }


    @Override
    public boolean isOpen() {
        return canChannel != null && canChannel.isOpen();
    }


    @Override
    public ByteBuffer receiveFrame(final Predicate<byte[]> validator) throws IOException {
        ensureOpen();

        final CanFrame frame = canChannel.read();
        final ByteBuffer buffer = frame.getBuffer();
        buffer.rewind();
        buffer.putInt(frame.getId());
        buffer.rewind();
        return buffer;
    }


    @Override
    public void sendFrame(final ByteBuffer frame) throws IOException {
        ensureOpen();

        final CanFrame sendFrame = CanFrame.create(frame);
        canChannel.write(sendFrame);
    }


    @Override
    public void sendExtendedFrame(final ByteBuffer frame) throws IOException {
        ensureOpen();
        /**
         * Frame bytes 0-3 frame-id as int, 4 data length, 5 flags for FD frames, 6 ?, 7 - 15 data
         * bytes
         */
        final int frameId = frame.getInt(); // first 4 bytes frameid
        final byte length = frame.get(); // 5th byte data length
        final byte flags = frame.get(); // 6th byte flags
        frame.getShort(); // skip 2 bytes
        final byte[] data = new byte[length];
        frame.get(data); // last 8 bytes data

        final CanFrame sendFrame = CanFrame.createExtended(frameId, flags, data, 0, length);
        canChannel.write(sendFrame);
    }


    @Override
    public void close() {
        // close old channel first
        if (isOpen()) {
            try {
                canChannel.close();
                LOG.info("Shutting down port '{}'...OK", getPortname());
            } catch (final Exception e) {
                LOG.error("Shutting down port '{}'...FAILED", getPortname(), e);
            }
        }

        canChannel = null;
    }


    private boolean ensureOpen() {
        if (!isOpen()) {
            // open port
            try {
                LOG.info("Opening " + getPortname() + " ...");
                open();
                LOG.info("Opening port {} SUCCESSFUL", getPortname());

            } catch (final Throwable e) {
                LOG.error("Opening port {} FAILED!", getPortname(), e);
            }
        }

        if (isOpen()) {
            return true;
        }

        return false;
    }

}
