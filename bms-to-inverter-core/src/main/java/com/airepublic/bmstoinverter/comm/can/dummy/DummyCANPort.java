package com.airepublic.bmstoinverter.comm.can.dummy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Predicate;

import com.airepublic.bmstoinverter.can.CANPort;

public class DummyCANPort extends CANPort {
    private boolean open = false;

    @Override
    public void open() throws IOException {
        open = true;
    }


    @Override
    public boolean isOpen() {
        return open;
    }


    @Override
    public ByteBuffer receiveFrame(final Predicate<byte[]> validator) throws IOException {
        return null;
    }


    @Override
    public void sendFrame(final ByteBuffer frame) throws IOException {

    }


    @Override
    public void sendExtendedFrame(final ByteBuffer frame) throws IOException {
    }


    @Override
    public void close() throws IOException {
        open = false;
    }

}
