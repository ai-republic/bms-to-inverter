package com.airepublic.bmstoinverter.inverter.dummy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

/**
 * The class to handle CAN messages for a SMA {@link Inverter}.
 */
@ApplicationScoped
@Default
public class DummyInverterProcessor extends Inverter {

    @Override
    protected List<ByteBuffer> createSendFrames() {
        return Collections.emptyList();
    }


    @Override
    protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
    }

}
