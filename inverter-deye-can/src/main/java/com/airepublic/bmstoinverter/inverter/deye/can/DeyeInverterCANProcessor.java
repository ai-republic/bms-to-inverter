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
package com.airepublic.bmstoinverter.inverter.deye.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.BitUtil;

import javax.enterprise.context.ApplicationScoped;

/**
 * The class to handle CAN messages for Deye {@link Inverter}.
 */
@ApplicationScoped
public class DeyeInverterCANProcessor extends Inverter {

    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        final List<ByteBuffer> frames = new ArrayList<>();

        frames.add(createChargeDischargeLimits(aggregatedPack)); // 0x351
        frames.add(createSOC(aggregatedPack)); // 0x355
        frames.add(createBatteryVoltage(aggregatedPack)); // 0x356
        frames.add(createChargeDischargeFlags(aggregatedPack)); // 0x35C
        frames.add(createManufacturer(aggregatedPack)); // 0x35E
        frames.add(createAlarms(aggregatedPack)); // 0x359

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


    protected ByteBuffer prepareSendFrame(final int frameId) {
        final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);
        sendFrame.putInt(frameId);

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        return sendFrame;
    }


    // 0x351
    private ByteBuffer createChargeDischargeLimits(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x0351);

        // Battery charge voltage (0.1V) - uint_16
        frame.putChar((char) pack.maxPackVoltageLimit);
        // Charge current limit (0.1A) - sint_16
        frame.putShort((short) pack.maxPackChargeCurrent);
        // Discharge current limit (0.1A) - sint_16
        frame.putShort((short) (pack.maxPackDischargeCurrent * -1));
        // Battery discharge voltage (0.1V) - uint_16
        frame.putChar((char) pack.minPackVoltageLimit);

        return frame;

    }


    // 0x355
    private ByteBuffer createSOC(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x0355);

        // SOC (1%) - uint_16
        frame.putChar((char) (pack.packSOC / 10));
        // SOH (1%) - uint_16
        frame.putChar((char) (pack.packSOH / 10));

        return frame;
    }


    // 0x356
    private ByteBuffer createBatteryVoltage(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x0356);

        // Battery voltage (0.01V) - uint_16
        frame.putShort((short) (pack.packVoltage * 10));
        // Battery current (0.1A) - uint_16
        frame.putShort((short) pack.packCurrent);
        // Battery temperature (0.1C) - uint_16
        frame.putShort((short) pack.tempAverage);

        return frame;
    }


    // 0x35C
    private ByteBuffer createChargeDischargeFlags(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x035E);
        byte flags = 0x00;

        // request full charge
        flags = BitUtil.setBit(flags, 3, false);
        // request force charge
        flags = BitUtil.setBit(flags, 4, pack.forceCharge);
        flags = BitUtil.setBit(flags, 5, pack.forceCharge);
        flags = BitUtil.setBit(flags, 6, pack.dischargeMOSState);
        flags = BitUtil.setBit(flags, 7, pack.chargeMOSState);

        frame.put(flags);

        return frame;
    }


    // 0x35E
    private ByteBuffer createManufacturer(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x035E);
        int idx = 0;

        while (idx < pack.manufacturerCode.length() && idx < 8) {
            frame.putChar(pack.manufacturerCode.charAt(idx));
            idx++;
        }

        return frame;
    }


    // 0x359
    private ByteBuffer createAlarms(final BatteryPack pack) {

        long bits = 0;
        final ByteBuffer frame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);
        frame.putInt(0x0359)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // protection alarms

        bits = BitUtil.setBit(bits, 1, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        bits = BitUtil.setBit(bits, 2, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.ALARM);
        bits = BitUtil.setBit(bits, 3, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        bits = BitUtil.setBit(bits, 4, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        bits = BitUtil.setBit(bits, 7, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        bits = BitUtil.setBit(bits, 8, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        bits = BitUtil.setBit(bits, 11, pack.getAlarmLevel(Alarm.FAILURE_OTHER) == AlarmLevel.ALARM);

        // warning alarms
        bits = BitUtil.setBit(bits, 17, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        bits = BitUtil.setBit(bits, 18, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.WARNING);
        bits = BitUtil.setBit(bits, 19, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        bits = BitUtil.setBit(bits, 20, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_LOW) == AlarmLevel.WARNING);
        bits = BitUtil.setBit(bits, 23, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);
        bits = BitUtil.setBit(bits, 24, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);
        bits = BitUtil.setBit(bits, 27, pack.getAlarmLevel(Alarm.FAILURE_COMMUNICATION_INTERNAL) == AlarmLevel.WARNING);

        frame.putInt((int) bits);
        frame.put((byte) 0); // module number
        frame.put((byte) 0x50);
        frame.put((byte) 0x4E);

        return frame;
    }

}
