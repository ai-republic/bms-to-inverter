package com.airepublic.bmstoinverter.inverter.growatt.rs485;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.inject.Inject;

/**
 * The class to handle RS485 messages for a Growatt low voltage (12V/24V/48V) {@link Inverter}.
 */
public class GrowattInverterRS485Processor extends Inverter {
    @Inject
    private EnergyStorage energyStorage;

    @Override
    protected List<ByteBuffer> updateCANMessages() {

        final List<ByteBuffer> frames = new ArrayList<>();
        final int slaveAddress = 11;
        final int function = 6;
        final int regHi = 0;
        final int regLow = 0;
        final int presetHi = 0;
        final int presetLow = 0;
        final int crc = 0;

        return frames;
    }

}
