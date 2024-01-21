package com.airepublic.bmstoinverter.inverter.dummy;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import com.airepublic.bmstoinverter.core.Inverter;

/**
 * The class to handle CAN messages for a SMA {@link Inverter}.
 */
public class DummyInverterProcessor extends Inverter {

    @Override
    protected List<ByteBuffer> updateCANMessages() {
        return Collections.emptyList();
    }
}
