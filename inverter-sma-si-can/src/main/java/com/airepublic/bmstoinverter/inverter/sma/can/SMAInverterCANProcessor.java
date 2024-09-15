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
package com.airepublic.bmstoinverter.inverter.sma.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.plugin.inverter.PresetBatteryPackDataPlugin;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.core.util.Util;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The class to handle CAN messages for a SMA {@link Inverter}.
 */
@ApplicationScoped
public class SMAInverterCANProcessor extends Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(SMAInverterCANProcessor.class);

    public SMAInverterCANProcessor() {
        final BatteryPack presetPack = new BatteryPack();
        // set some default values
        presetPack.packSOC = 500; // 50%
        presetPack.packSOH = 1000; // 100%
        presetPack.packCurrent = 0; // 0A
        presetPack.packVoltage = 550;// 55.0V
        presetPack.maxPackChargeCurrent = 500; // 50.0A
        presetPack.maxPackDischargeCurrent = -500; // -50.0A
        presetPack.maxPackVoltageLimit = 560; // 56.0V
        presetPack.minPackVoltageLimit = 480; // 48.0V
        presetPack.tempAverage = 250; // 25.0C

        setPlugin(new PresetBatteryPackDataPlugin());
    }


    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        final List<ByteBuffer> frames = new ArrayList<>();

        frames.add(createChargeDischargeInfo(aggregatedPack)); // 0x351
        frames.add(createSOC(aggregatedPack)); // 0x355
        frames.add(createBatteryVoltage(aggregatedPack)); // 0x356
        frames.add(createAlarms(aggregatedPack)); // 0x35A

        LOG.info("Sending SMA frame: Batt(V)={}, Batt(A)={}, SOC={}", aggregatedPack.packVoltage / 10f, aggregatedPack.packCurrent / 10f, aggregatedPack.packSOC / 10f);

        return frames;
    }


    @Override
    protected ByteBuffer readRequest(final Port port) throws IOException {
        return null;
    }


    @Override
    protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
        ((CANPort) port).sendExtendedFrame(frame);
    }


    // 0x351
    private ByteBuffer createChargeDischargeInfo(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x351);

        // Battery charge voltage (0.1V) - u_int_16
        frame.putChar((char) pack.maxPackVoltageLimit);
        // Charge current limit (0.1A) - s_int_16
        frame.putShort((short) pack.maxPackChargeCurrent);
        // Discharge current limit (0.1A) - s_int_16
        frame.putShort((short) (pack.maxPackDischargeCurrent * -1)); // needs positive value
        // Battery discharge voltage (0.1V) - u_int_16
        frame.putChar((char) pack.minPackVoltageLimit);

        return frame;

    }


    // 0x355
    private ByteBuffer createSOC(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x355);

        // SOC (1%) - u_int_16
        frame.putChar((char) (pack.packSOC / 10));
        // SOH (1%) - u_int_16
        frame.putChar((char) (pack.packSOH / 10));

        return frame;
    }


    // 0x356
    private ByteBuffer createBatteryVoltage(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x356);

        // Battery voltage (0.01V) - u_int_16
        frame.putShort((short) (pack.packVoltage * 10));
        // Battery current (0.1A) - u_int_16
        frame.putShort((short) pack.packCurrent);
        // Battery temperature (0.1C) - u_int_16
        frame.putShort((short) pack.tempAverage);

        return frame;
    }


    // 0x35A
    private ByteBuffer createAlarms(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x359);
        final byte alarm1 = 0;
        final byte alarm2 = 0;
        final byte alarm3 = 0;
        final byte alarm4 = 0;
        final byte warning1 = 0;
        final byte warning2 = 0;
        final byte warning3 = 0;
        final byte warning4 = 0;

        // alarms
        Util.setBit(alarm1, 0, false);
        Util.setBit(alarm1, 1, false);

        Util.setBit(alarm1, 2, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        Util.setBit(alarm1, 3, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.NONE); // pack
                                                                                                // voltage
        // to high
        Util.setBit(alarm1, 4, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.ALARM); // pack
                                                                                                // voltage
        // to low
        Util.setBit(alarm1, 5, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.NONE);
        Util.setBit(alarm1, 6, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_HIGH) == AlarmLevel.ALARM);// pack
        // temp to
        // high
        Util.setBit(alarm1, 7, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_HIGH) == AlarmLevel.NONE);

        frame.put(alarm1);

        Util.setBit(alarm2, 0, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_LOW) == AlarmLevel.ALARM); // pack
        // temp to
        // low
        Util.setBit(alarm2, 1, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_LOW) == AlarmLevel.NONE);
        Util.setBit(alarm2, 2, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);// charge
        // temp
        // to
        // high
        Util.setBit(alarm2, 3, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.NONE);
        Util.setBit(alarm2, 4, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM); // charge
        // temp
        // to low
        Util.setBit(alarm2, 5, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.NONE);
        Util.setBit(alarm2, 6, pack.getAlarmLevel(Alarm.PACK_CURRENT_HIGH) == AlarmLevel.ALARM); // pack
        // current to
        // high
        Util.setBit(alarm2, 7, pack.getAlarmLevel(Alarm.PACK_CURRENT_HIGH) == AlarmLevel.NONE);

        frame.put(alarm2);

        Util.setBit(alarm3, 0, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM); // charge
        // current
        // to high
        Util.setBit(alarm3, 1, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        Util.setBit(alarm3, 2, false); // contactor
        Util.setBit(alarm3, 3, false);
        Util.setBit(alarm3, 4, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.ALARM); // short
        // circuit
        Util.setBit(alarm3, 5, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.NONE);
        Util.setBit(alarm3, 6, false); // other bms internal error
        Util.setBit(alarm3, 7, false);

        frame.put(alarm3);

        Util.setBit(alarm4, 0, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.ALARM); // cell
        // difference
        // to
        // high
        Util.setBit(alarm4, 1, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.NONE);
        frame.put(alarm4);

        // warnings
        Util.setBit(warning1, 0, false);
        Util.setBit(warning1, 1, false);

        Util.setBit(warning1, 2, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        Util.setBit(warning1, 3, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.NONE); // pack
                                                                                                  // voltage
        // to high
        Util.setBit(warning1, 4, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.ALARM); // pack
                                                                                                  // voltage
        // to low
        Util.setBit(warning1, 5, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.NONE);
        Util.setBit(warning1, 6, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_HIGH) == AlarmLevel.ALARM);// pack
        // temp to
        // high
        Util.setBit(warning1, 7, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_HIGH) == AlarmLevel.NONE);

        frame.put(warning1);

        Util.setBit(warning2, 0, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_LOW) == AlarmLevel.ALARM); // pack
        // temp to
        // low
        Util.setBit(warning2, 1, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_LOW) == AlarmLevel.NONE);
        Util.setBit(warning2, 2, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);// charge
        // temp
        // to
        // high
        Util.setBit(warning2, 3, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.NONE);
        Util.setBit(warning2, 4, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM); // charge
        // temp
        // to low
        Util.setBit(warning2, 5, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.NONE);
        Util.setBit(warning2, 6, pack.getAlarmLevel(Alarm.PACK_CURRENT_HIGH) == AlarmLevel.ALARM); // pack
        // current to
        // high
        Util.setBit(warning2, 7, pack.getAlarmLevel(Alarm.PACK_CURRENT_HIGH) == AlarmLevel.NONE);

        frame.put(warning2);

        Util.setBit(warning3, 0, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM); // charge
        // current
        // to high
        Util.setBit(warning3, 1, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        Util.setBit(warning3, 2, false); // contactor
        Util.setBit(warning3, 3, false);
        Util.setBit(warning3, 4, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.ALARM); // short
        // circuit
        Util.setBit(warning3, 5, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.NONE);
        Util.setBit(warning3, 6, false); // other bms internal error
        Util.setBit(warning3, 7, false);

        frame.put(warning3);

        Util.setBit(warning4, 0, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.ALARM); // cell
        // difference
        // to
        // high
        Util.setBit(warning4, 1, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.NONE);
        frame.put(warning4);
        return frame;
    }


    private ByteBuffer prepareFrame(final int cmd) {
        final ByteBuffer frame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);
        frame.putInt(cmd)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        return frame;
    }

}
