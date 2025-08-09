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
package com.airepublic.bmstoinverter.inverter.growatt.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.core.util.BitUtil;

import javax.enterprise.context.ApplicationScoped;

/**
 * The class to handle CAN messages for a Growatt low voltage (12V/24V/48V) {@link Inverter}.
 */
@ApplicationScoped
public class GrowattInverterCANProcessor extends Inverter {

    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        final List<ByteBuffer> frames = new ArrayList<>();

        frames.add(createChargeDischargeInfo(aggregatedPack)); // 0x311
        frames.add(createAlarms(aggregatedPack)); // 0x312
        frames.add(createBatteryVoltage(aggregatedPack)); // 0x313
        frames.add(createBatteryStatus(aggregatedPack)); // 0x314
        frames.add(createMinMaxVoltageCell(aggregatedPack)); // 0x319
        frames.add(createBMSInfo(aggregatedPack)); // 0x320

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


    // 0x311
    private ByteBuffer createChargeDischargeInfo(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x311);

        // Battery charge voltage (0.1V) - uint_16
        frame.putChar((char) pack.maxPackVoltageLimit);
        // Charge current limit (0.1A) - sint_16
        frame.putChar((char) pack.maxPackChargeCurrent);
        // Discharge current limit (0.1A) - sint_16
        frame.putChar((char) pack.maxPackDischargeCurrent);
        // status bits (see documentation)
        frame.put(get311Status(pack));

        return frame;
    }


    private byte[] get311Status(final BatteryPack pack) {
        final byte[] status = new byte[2];

        // charging status
        final boolean charging = pack.chargeMOSState;
        status[0] = BitUtil.setBit(status[0], 0, charging);
        status[0] = BitUtil.setBit(status[0], 1, false);

        // error bit flag
        status[0] = BitUtil.setBit(status[0], 2, false);

        // balancing status
        status[0] = BitUtil.setBit(status[0], 3, pack.cellBalanceActive);

        // sleep status
        status[0] = BitUtil.setBit(status[0], 4, false);

        // output discharge status
        status[0] = BitUtil.setBit(status[0], 5, false);

        // output charge status
        status[0] = BitUtil.setBit(status[0], 6, false);

        // battery terminal status
        status[0] = BitUtil.setBit(status[0], 7, false);

        // master box operation mode 00-standalone, 01-parallel, 10-parallel ready
        status[1] = BitUtil.setBit(status[1], 0, false);
        status[1] = BitUtil.setBit(status[1], 1, false);

        // SP status 00-none, 01-standby, 10-charging, 11-discharging
        status[1] = BitUtil.setBit(status[1], 2, true);
        status[1] = BitUtil.setBit(status[1], 3, !charging);

        return status;
    }


    // 0x312
    private ByteBuffer createAlarms(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x312);

        final boolean aggregatedSystemError = pack.getAlarmLevel(Alarm.FAILURE_OTHER) == AlarmLevel.ALARM || pack.getAlarmLevel(Alarm.FAILURE_OTHER) == AlarmLevel.WARNING;

        // protection alarms
        byte alarms1 = 0;
        alarms1 = BitUtil.setBit(alarms1, 0, false);
        alarms1 = BitUtil.setBit(alarms1, 1, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.ALARM);
        alarms1 = BitUtil.setBit(alarms1, 2, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        alarms1 = BitUtil.setBit(alarms1, 3, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.ALARM);
        alarms1 = BitUtil.setBit(alarms1, 4, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        alarms1 = BitUtil.setBit(alarms1, 5, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.ALARM);
        alarms1 = BitUtil.setBit(alarms1, 6, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        alarms1 = BitUtil.setBit(alarms1, 7, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        frame.put(alarms1);

        byte alarms2 = 0;
        alarms2 = BitUtil.setBit(alarms2, 0, false);
        alarms2 = BitUtil.setBit(alarms2, 1, false);
        alarms2 = BitUtil.setBit(alarms2, 2, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.ALARM);
        alarms2 = BitUtil.setBit(alarms2, 3, aggregatedSystemError);
        alarms2 = BitUtil.setBit(alarms2, 4, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        alarms2 = BitUtil.setBit(alarms2, 5, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        alarms2 = BitUtil.setBit(alarms2, 6, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        alarms2 = BitUtil.setBit(alarms2, 7, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        frame.put(alarms2);

        // warning alarms
        byte warnings1 = 0;
        warnings1 = BitUtil.setBit(warnings1, 0, false);
        warnings1 = BitUtil.setBit(warnings1, 1, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.WARNING);
        warnings1 = BitUtil.setBit(warnings1, 2, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        warnings1 = BitUtil.setBit(warnings1, 3, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.WARNING);
        warnings1 = BitUtil.setBit(warnings1, 4, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        warnings1 = BitUtil.setBit(warnings1, 5, false);
        warnings1 = BitUtil.setBit(warnings1, 6, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);
        warnings1 = BitUtil.setBit(warnings1, 7, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);
        frame.put(warnings1);

        byte warnings2 = 0;
        warnings2 = BitUtil.setBit(warnings2, 0, pack.getAlarmLevel(Alarm.FAILURE_COMMUNICATION_INTERNAL) == AlarmLevel.WARNING);
        warnings2 = BitUtil.setBit(warnings2, 1, false);
        warnings2 = BitUtil.setBit(warnings2, 2, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.WARNING);
        warnings2 = BitUtil.setBit(warnings2, 3, false);
        warnings2 = BitUtil.setBit(warnings2, 4, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.WARNING);
        warnings2 = BitUtil.setBit(warnings2, 5, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_LOW) == AlarmLevel.WARNING);
        warnings2 = BitUtil.setBit(warnings2, 6, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        warnings2 = BitUtil.setBit(warnings2, 7, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        frame.put(warnings2);

        frame.put((byte) pack.numberOfCells);
        frame.putChar((char) 0); // skip 2 manufacturer codes
        frame.put((byte) pack.numberOfCells);

        return frame;
    }


    // 0x313
    private ByteBuffer createBatteryVoltage(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x313);

        // Battery voltage (0.01V) - uint_16
        frame.putShort((short) (pack.packVoltage * 10));
        // Battery current (0.1A) - uint_16
        frame.putShort((short) pack.packCurrent);
        // Battery temperature (0.1C) - uint_16
        frame.putShort((short) pack.tempAverage);
        // Battery SOC
        frame.put((byte) (pack.packSOC / 10));
        // Battery SOH
        frame.put((byte) (pack.packSOH / 10));

        return frame;
    }


    // 0x314
    private ByteBuffer createBatteryStatus(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x314);
        // remaining capacity (10mAh)
        frame.putChar((char) (pack.remainingCapacitymAh / 10f));
        // full capacity (10mAh)
        frame.putChar((char) (pack.ratedCapacitymAh / 10f));
        // cell difference (1mv)
        frame.putChar((char) pack.cellDiffmV);
        // bms cycles
        frame.putChar((char) pack.bmsCycles);

        return frame;
    }


    // 0x319
    private ByteBuffer createMinMaxVoltageCell(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x319);
        // charge state and battery status
        frame.put(getChargeStates(pack));

        frame.putChar((char) pack.maxCellmV);
        frame.putChar((char) pack.minCellmV);
        frame.put((byte) pack.maxCellVNum);
        frame.put((byte) pack.minCellVNum);
        // pack id of faulty battery
        frame.put((byte) 0);

        return frame;
    }


    // 0x320
    private ByteBuffer createBMSInfo(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x320);

        // manufacturer
        frame.put((byte) 0x00);
        frame.put((byte) 0x01);
        // hardware version
        frame.put((byte) 1);
        // software version
        frame.put((byte) 1);
        final byte[] dateTime = getDateTimeBits();
        frame.put(dateTime[0]);
        frame.put(dateTime[1]);
        frame.put(dateTime[2]);
        frame.put(dateTime[3]);

        return frame;
    }


    /**
     * See documentation Table 7.
     *
     * @return the charge state bits
     */
    private byte getChargeStates(final BatteryPack pack) {
        byte value = 0;
        // 0x319 table 5
        value = BitUtil.setBit(value, 7, pack.chargeMOSState);
        value = BitUtil.setBit(value, 6, pack.dischargeMOSState);
        value = BitUtil.setBit(value, 5, false);
        value = BitUtil.setBit(value, 4, false);
        value = BitUtil.setBit(value, 3, false);
        value = BitUtil.setBit(value, 2, false);
        switch (pack.type) {
            case 0: // lithium iron phosphate
                value = BitUtil.setBit(value, 1, false);
                value = BitUtil.setBit(value, 0, false);
            break;
            case 1: // ternary lithium
                value = BitUtil.setBit(value, 1, false);
                value = BitUtil.setBit(value, 0, true);
            break;
            case 2: // lithium titanate
                value = BitUtil.setBit(value, 1, true);
                value = BitUtil.setBit(value, 0, false);
            break;
        }

        return value;
    }


    private ByteBuffer prepareFrame(final int cmd) {
        final ByteBuffer frame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);
        frame.putInt(cmd)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        return frame;
    }


    private byte[] getDateTimeBits() {
        final LocalDateTime time = LocalDateTime.now();
        final byte[] dateTime = new byte[4];

        // seconds
        int value = time.getSecond();
        dateTime[0] = BitUtil.setBit(dateTime[0], 0, BitUtil.bit(value, 0));
        dateTime[0] = BitUtil.setBit(dateTime[0], 1, BitUtil.bit(value, 1));
        dateTime[0] = BitUtil.setBit(dateTime[0], 2, BitUtil.bit(value, 2));
        dateTime[0] = BitUtil.setBit(dateTime[0], 3, BitUtil.bit(value, 3));
        dateTime[0] = BitUtil.setBit(dateTime[0], 4, BitUtil.bit(value, 4));
        dateTime[0] = BitUtil.setBit(dateTime[0], 5, BitUtil.bit(value, 5));

        // minutes
        value = time.getMinute();
        dateTime[0] = BitUtil.setBit(dateTime[0], 6, BitUtil.bit(value, 0));
        dateTime[0] = BitUtil.setBit(dateTime[0], 7, BitUtil.bit(value, 1));
        dateTime[1] = BitUtil.setBit(dateTime[1], 0, BitUtil.bit(value, 2));
        dateTime[1] = BitUtil.setBit(dateTime[1], 1, BitUtil.bit(value, 3));
        dateTime[1] = BitUtil.setBit(dateTime[1], 2, BitUtil.bit(value, 4));
        dateTime[1] = BitUtil.setBit(dateTime[1], 3, BitUtil.bit(value, 5));

        // hours
        value = time.getHour();
        dateTime[1] = BitUtil.setBit(dateTime[1], 4, BitUtil.bit(value, 0));
        dateTime[1] = BitUtil.setBit(dateTime[1], 5, BitUtil.bit(value, 1));
        dateTime[1] = BitUtil.setBit(dateTime[1], 6, BitUtil.bit(value, 2));
        dateTime[1] = BitUtil.setBit(dateTime[1], 7, BitUtil.bit(value, 3));
        dateTime[2] = BitUtil.setBit(dateTime[2], 0, BitUtil.bit(value, 4));

        // day
        value = time.getDayOfMonth();
        dateTime[2] = BitUtil.setBit(dateTime[2], 1, BitUtil.bit(value, 0));
        dateTime[2] = BitUtil.setBit(dateTime[2], 2, BitUtil.bit(value, 1));
        dateTime[2] = BitUtil.setBit(dateTime[2], 3, BitUtil.bit(value, 2));
        dateTime[2] = BitUtil.setBit(dateTime[2], 4, BitUtil.bit(value, 3));
        dateTime[2] = BitUtil.setBit(dateTime[2], 5, BitUtil.bit(value, 4));

        // month
        value = time.getMonthValue();
        dateTime[2] = BitUtil.setBit(dateTime[2], 6, BitUtil.bit(value, 0));
        dateTime[2] = BitUtil.setBit(dateTime[2], 7, BitUtil.bit(value, 1));
        dateTime[3] = BitUtil.setBit(dateTime[3], 0, BitUtil.bit(value, 2));
        dateTime[3] = BitUtil.setBit(dateTime[3], 1, BitUtil.bit(value, 3));

        // year
        value = time.getYear();
        dateTime[3] = BitUtil.setBit(dateTime[3], 2, BitUtil.bit(value, 0));
        dateTime[3] = BitUtil.setBit(dateTime[3], 3, BitUtil.bit(value, 1));
        dateTime[3] = BitUtil.setBit(dateTime[3], 4, BitUtil.bit(value, 2));
        dateTime[3] = BitUtil.setBit(dateTime[3], 5, BitUtil.bit(value, 3));
        dateTime[3] = BitUtil.setBit(dateTime[3], 6, BitUtil.bit(value, 4));
        dateTime[3] = BitUtil.setBit(dateTime[3], 7, BitUtil.bit(value, 5));

        return dateTime;
    }


    public static void main(final String[] args) {
        final BatteryPack pack = new BatteryPack();
        pack.packVoltage = 535;
        pack.packCurrent = 15;
        pack.packSOC = 940;
        pack.packSOH = 1000;
        pack.tempMax = 220;
        final EnergyStorage es = new EnergyStorage();
        es.getBatteryPacks().add(pack);

        final GrowattInverterCANProcessor processor = new GrowattInverterCANProcessor();
        final ByteBuffer frame = processor.createBatteryVoltage(pack);

        System.out.println(Port.printBuffer(frame));
    }
}
