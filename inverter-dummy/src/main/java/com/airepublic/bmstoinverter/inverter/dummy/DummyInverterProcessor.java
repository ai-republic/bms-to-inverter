package com.airepublic.bmstoinverter.inverter.dummy;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import com.airepublic.bmstoinverter.core.Inverter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

/**
 * The class to handle CAN messages for a SMA {@link Inverter}.
 */
@ApplicationScoped
@Default
public class DummyInverterProcessor extends Inverter {

    @Override
    protected List<ByteBuffer> updateCANMessages() {
        return Collections.emptyList();
    }
}
