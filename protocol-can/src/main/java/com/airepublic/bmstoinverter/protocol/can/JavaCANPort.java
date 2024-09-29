/**
 * This software is free to use and to distribute in its unchanged form for private use.
 * Commercial use is prohibited without an explicit license agreement of the copyright holder.
 * Any changes to this software must be made solely in the project repository at https://github.com/ai-republic/bms-to-inverter.
 * The copyright holder is not liable for any damages in whatever form that may occur by using this software.
 *
 * (c) Copyright 2022 and onwards - Torsten Oltmanns
 *
 * @author Torsten Oltmanns - bms-to-inverter''AT''gmail.com
 */
package com.airepublic.bmstoinverter.protocol.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import tel.schich.javacan.CanChannels;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.CanSocketOptions;
import tel.schich.javacan.RawCanChannel;

/**
 * The implementation of the {@link CANPort} using the javacan implementation.
 */
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
     * @param baudrate the baudrate
     */
    public JavaCANPort(final String portname, final int baudrate) {
        super(portname, baudrate);
    }


    @Override
    public void open() throws Exception {
        if (!isOpen()) {
            // close old channel first
            if (canChannel != null) {
                close();
            }

            canChannel = CanChannels.newRawChannel(getPortname());
            canChannel.setOption(CanSocketOptions.SO_RCVTIMEO, Duration.ofMillis(1000));
        }
    }


    @Override
    public boolean isOpen() {
        return canChannel != null && canChannel.isOpen();
    }


    @Override
    public ByteBuffer receiveFrame() throws IOException {
        ensureOpen();

        LOG.debug("CAN frame read...");
        final CanFrame frame = canChannel.read();
        final ByteBuffer buffer = frame.getBuffer();
        buffer.rewind();
        buffer.putInt(frame.getId());
        buffer.rewind();
        LOG.debug("CAN read frame {}", printBuffer(frame.getBuffer()));
        return buffer;
    }


    @Override
    public void sendFrame(final ByteBuffer frame) throws IOException {
        ensureOpen();

        frame.rewind();

        final CanFrame sendFrame = CanFrame.create(frame);
        canChannel.write(sendFrame);
    }


    @Override
    public void sendExtendedFrame(final ByteBuffer frame) throws IOException {
        ensureOpen();

        frame.rewind();

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

        LOG.debug("CAN frame sending: {}", printBuffer(frame));
        final CanFrame sendFrame = CanFrame.createExtended(frameId, flags, data, 0, length);
        canChannel.write(sendFrame);
    }


    @Override
    public void close() {
        // close old channel first
        if (isOpen() && canChannel != null) {
            try {
                canChannel.close();
                LOG.info("Shutting down port '{}'...OK", getPortname());
            } catch (final Exception e) {
                LOG.error("Shutting down port '{}'...FAILED", getPortname(), e);
            }
        }

        canChannel = null;
    }


    @Override
    public void clearBuffers() {
    }
}
