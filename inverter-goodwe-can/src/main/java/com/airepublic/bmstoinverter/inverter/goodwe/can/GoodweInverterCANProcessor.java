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
package com.airepublic.bmstoinverter.inverter.goodwe.can;

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
import com.airepublic.bmstoinverter.core.util.BitUtil;

import javax.enterprise.context.ApplicationScoped;

/**
 * The class to handle CAN messages for a Goodwe HV {@link Inverter}.
 */
@ApplicationScoped
public class GoodweInverterCANProcessor extends Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(GoodweInverterCANProcessor.class);

    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        final List<ByteBuffer> frames = new ArrayList<>();

        frames.add(createModulesInfo(aggregatedPack)); // 0x453
        frames.add(createAlarms(aggregatedPack)); // 0x455
        frames.add(createChargeDischargeInfo(aggregatedPack)); // 0x456
        frames.add(createSOC(aggregatedPack)); // 0x457
        frames.add(createBatteryVoltage(aggregatedPack)); // 0x458

        LOG.info("Sending Goodwe frame: Batt(V)={}, Batt(A)={}, SOC={}", aggregatedPack.packVoltage / 10f, aggregatedPack.packCurrent / 10f, aggregatedPack.packSOC / 10f);

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


    // 0x453
    private ByteBuffer createModulesInfo(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x453);

        // Battery packs
        frame.putChar((char) getEnergyStorage().getBatteryPacks().size());

        return frame;
    }


    // 0x456
    private ByteBuffer createChargeDischargeInfo(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x456);

        // Battery charge voltage (0.1V) - s_int_16
        frame.putShort((short) pack.maxPackVoltageLimit);
        // Charge current limit (0.1A) - s_int_16
        frame.putShort((short) pack.maxPackChargeCurrent);
        // Discharge current limit (0.1A) - s_int_16
        frame.putShort((short) (pack.maxPackDischargeCurrent * -1)); // needs positive value
        // Battery discharge voltage (0.1V) - s_int_16
        frame.putShort((short) pack.minPackVoltageLimit);

        return frame;

    }


    // 0x457
    private ByteBuffer createSOC(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x457);

        // SOC (0.01%) - u_int_16
        frame.putShort((short) (pack.packSOC * 10));
        // SOH (0.01%) - u_int_16
        frame.putShort((short) (pack.packSOH * 10));

        return frame;
    }


    // 0x458
    private ByteBuffer createBatteryVoltage(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x458);

        // Battery voltage (0.1V) - u_int_16
        frame.putShort((short) pack.packVoltage);
        // Battery current (0.1A) - u_int_16
        frame.putShort((short) pack.packCurrent);
        // Battery temperature (0.1C) - u_int_16
        frame.putShort((short) pack.tempAverage);

        return frame;
    }


    // 0x455
    private ByteBuffer createAlarms(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x455);
        short alarms = 0;
        short warnings = 0;

        // alarms
        alarms = BitUtil.setBit(alarms, 0, pack.getAlarmLevel(Alarm.CHARGE_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 1, pack.getAlarmLevel(Alarm.DISCHARGE_VOLTAGE_LOW) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 2, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 3, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 4, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 5, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 6, pack.getAlarmLevel(Alarm.FAILURE_PRECHARGE_MODULE) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 7, pack.getAlarmLevel(Alarm.FAILURE_OTHER) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 8, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.ALARM);

        alarms = BitUtil.setBit(alarms, 10, pack.getAlarmLevel(Alarm.FAILURE_DISCHARGE_BREAKER) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 11, pack.getAlarmLevel(Alarm.FAILURE_CHARGE_BREAKER) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 12, pack.getAlarmLevel(Alarm.FAILURE_COMMUNICATION_INTERNAL) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 13, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 14, pack.getAlarmLevel(Alarm.DISCHARGE_VOLTAGE_LOW) == AlarmLevel.ALARM);
        alarms = BitUtil.setBit(alarms, 15, pack.getAlarmLevel(Alarm.CHARGE_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        frame.putShort(alarms);

        // warnings
        warnings = BitUtil.setBit(alarms, 0, pack.getAlarmLevel(Alarm.CHARGE_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        warnings = BitUtil.setBit(warnings, 1, pack.getAlarmLevel(Alarm.DISCHARGE_VOLTAGE_LOW) == AlarmLevel.ALARM);
        warnings = BitUtil.setBit(warnings, 2, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        warnings = BitUtil.setBit(warnings, 3, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        warnings = BitUtil.setBit(warnings, 4, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        warnings = BitUtil.setBit(warnings, 5, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        warnings = BitUtil.setBit(warnings, 6, pack.getAlarmLevel(Alarm.FAILURE_COMMUNICATION_INTERNAL) == AlarmLevel.ALARM);

        warnings = BitUtil.setBit(warnings, 8, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.ALARM);
        warnings = BitUtil.setBit(warnings, 9, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_LOW) == AlarmLevel.ALARM);

        warnings = BitUtil.setBit(warnings, 11, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_HIGH) == AlarmLevel.ALARM);

        frame.putShort(warnings);

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
