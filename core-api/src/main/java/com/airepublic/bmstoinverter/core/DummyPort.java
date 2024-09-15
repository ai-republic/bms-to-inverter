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
package com.airepublic.bmstoinverter.core;

import java.io.IOException;
import java.nio.ByteBuffer;

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
    public ByteBuffer receiveFrame() throws IOException {
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
