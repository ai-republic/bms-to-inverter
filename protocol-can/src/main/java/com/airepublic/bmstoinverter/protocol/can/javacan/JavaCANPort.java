package com.airepublic.bmstoinverter.protocol.can.javacan;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.airepublic.bmstoinverter.core.protocol.can.CAN;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import tel.schich.javacan.CanChannels;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.RawCanChannel;

@CAN
public class JavaCANPort extends CANPort {
    private RawCanChannel canChannel;
    private ExecutorService executor;

    public JavaCANPort() {
    }


    public JavaCANPort(final String portname) {
        super(portname);
    }


    @Override
    public void open() throws IOException {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }

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
        final Future<ByteBuffer> result = executor.submit(() -> {
            final CanFrame frame = canChannel.read();
            final ByteBuffer buffer = frame.getBuffer();
            buffer.rewind();
            buffer.putInt(frame.getId());
            buffer.rewind();
            return buffer;
        });

        try {
            return result.get(500, TimeUnit.MILLISECONDS);
        } catch (final Exception e) {
            throw new IOException("Failed to read response!", e);
        }
    }


    @Override
    public void sendFrame(final ByteBuffer frame) throws IOException {
        final CanFrame sendFrame = CanFrame.create(frame);
        canChannel.write(sendFrame);
    }


    @Override
    public void sendExtendedFrame(final ByteBuffer frame) throws IOException {
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
    public void close() throws IOException {
        try {
            executor.shutdownNow();
            executor = null;
        } catch (final Exception e) {
        }

        // close old channel first
        if (canChannel != null) {
            canChannel.close();
        }
    }

}
