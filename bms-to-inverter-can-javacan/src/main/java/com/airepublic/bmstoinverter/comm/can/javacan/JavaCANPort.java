package com.airepublic.bmstoinverter.comm.can.javacan;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.airepublic.bmstoinverter.can.CAN;
import com.airepublic.bmstoinverter.can.CANPort;

import tel.schich.javacan.CanChannels;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.RawCanChannel;

@CAN
public class JavaCANPort extends CANPort {
    private RawCanChannel canChannel;
    private ExecutorService executor;

    public JavaCANPort() {
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
            return result.get(200, TimeUnit.MILLISECONDS);
        } catch (final Exception e) {
            throw new IOException("Failed to read response!", e);
        }
    }


    @Override
    public void sendFrame(final ByteBuffer frame) throws IOException {
        frame.order(ByteOrder.LITTLE_ENDIAN);
        frame.rewind();
        final CanFrame sendFrame = CanFrame.create(frame);
        canChannel.write(sendFrame);
    }


    @Override
    public void sendExtendedFrame(final ByteBuffer frame) throws IOException {
        frame.rewind();
        final byte[] data = new byte[frame.get(4)];
        frame.get(7, data);
        final CanFrame sendFrame = CanFrame.createExtended(frame.getInt(0), frame.get(5), data, 0, frame.get(4));
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
