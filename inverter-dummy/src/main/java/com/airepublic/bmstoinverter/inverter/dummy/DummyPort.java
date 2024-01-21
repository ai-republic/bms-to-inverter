package com.airepublic.bmstoinverter.inverter.dummy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Predicate;

import com.airepublic.bmstoinverter.core.Port;

/**
 * Dummy {@link Port} implementation.
 */
public class DummyPort extends Port {

    @Override
    public void open() throws Exception {
    }


    @Override
    public boolean isOpen() {
        return true;
    }


    @Override
    public ByteBuffer receiveFrame(final Predicate<byte[]> validator) throws IOException {
        return null;
    }


    @Override
    public void sendFrame(final ByteBuffer frame) throws IOException {
    }


    @Override
    public void clearBuffers() {
    }


    @Override
    public void close() {
    }

}
