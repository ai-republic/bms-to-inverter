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
package com.airepublic.bmstoinverter.inverter.growatt.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * The class to handle RS485 messages for a Growatt low voltage (12V/24V/48V) {@link Inverter}.
 */
@ApplicationScoped
public class GrowattInverterRS485Processor extends Inverter {
    @Inject
    private EnergyStorage energyStorage;

    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {

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


    @Override
    protected ByteBuffer readRequest(final Port port) throws IOException {
        return null;
    }


    @Override
    protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
        port.sendFrame(frame);
    }

}
