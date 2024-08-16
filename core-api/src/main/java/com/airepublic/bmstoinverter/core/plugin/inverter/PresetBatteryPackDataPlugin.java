package com.airepublic.bmstoinverter.core.plugin.inverter;

import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.InverterConfig;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * Checks the aggregated battery information of an {@link Inverter} if its SOC has already been read
 * (positive value). If its negative (not yet read), then the preset battery information will be
 * written to the aggregated battery information.
 */
public class PresetBatteryPackDataPlugin implements InverterPlugin {
    private final BatteryPack presetPack;

    public PresetBatteryPackDataPlugin(final BatteryPack presetPack) {
        this.presetPack = presetPack;
    }


    @Override
    public void onInitialize(final Inverter inverter, final InverterConfig config) {
    }


    @Override
    public ByteBuffer onSend(final ByteBuffer frame) {
        return frame;
    }


    @Override
    public ByteBuffer onReceive(final ByteBuffer frame) {
        return frame;
    }


    @Override
    public void onBatteryAggregation(final BatteryPack aggregatedPack) {
        // if the SOC has not yet been set
        if (aggregatedPack.packSOC < 0) {
            // set some default values
            aggregatedPack.packSOC = presetPack.packSOC;
            aggregatedPack.packSOH = presetPack.packSOH;
            aggregatedPack.packCurrent = presetPack.packCurrent;
            aggregatedPack.packVoltage = presetPack.packVoltage;
            aggregatedPack.maxPackChargeCurrent = presetPack.maxPackChargeCurrent;
            aggregatedPack.maxPackDischargeCurrent = presetPack.maxPackDischargeCurrent;
            aggregatedPack.maxPackVoltageLimit = presetPack.maxPackVoltageLimit;
            aggregatedPack.minPackVoltageLimit = presetPack.minPackVoltageLimit;
            aggregatedPack.tempAverage = presetPack.tempAverage;
        }
    }
}
