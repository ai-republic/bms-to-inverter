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
package com.airepublic.bmstoinverter.bms.jk.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.BitUtil;

/**
 * The class to handle CAN messages from a JK {@link BMS}.
 */
public class JKBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(JKBmsCANProcessor.class);

    @Override
    protected void collectData(final Port port) {
        try {
            final ByteBuffer frame = port.receiveFrame();
            frame.order(ByteOrder.LITTLE_ENDIAN);
            final int frameId = frame.getInt();
            final byte[] bytes = new byte[8];
            frame.get(bytes);
            final ByteBuffer data = ByteBuffer.wrap(bytes);

            final int cmd = frameId & 0xFFFFFFF0;
            final int bmsId = (frameId & 0x0000000F) - 4;
            final BatteryPack pack = getBatteryPack(bmsId);

            switch (cmd) {
                case 0x2F0:
                    readBatteryStatus(pack, data);
                break;
                case 0x4F0:
                    readCellVoltage(pack, data);
                break;
                case 0x5F0:
                    readCellTemperature(pack, data);
                break;
                case 0x7F0:
                    readAlarms(pack, data);
                break;
                case 0x18F128F0:
                    readBatteryStatus2(pack, data);
                break;
                case 0x18F228F0:
                    readCelTemperatures2(pack, data);
                break;
                case 0x18F328F0:
                    readAlarms2(pack, data);
                break;
                case 0x18F428F0:
                    readBMSInfo2(pack, data);
                break;
                case 0x18F528F0:
                    readBmsStatus2(pack, data);
                break;
                case 0x18E028F0:
                    readCellVoltages(pack, data, 0);
                break;
                case 0x18E128F0:
                    readCellVoltages(pack, data, 4);
                break;
                case 0x18E228F0:
                    readCellVoltages(pack, data, 8);
                break;
                case 0x18E328F0:
                    readCellVoltages(pack, data, 12);
                break;
                case 0x18E428F0:
                    readCellVoltages(pack, data, 16);
                break;
                case 0x18E528F0:
                    readCellVoltages(pack, data, 20);
                break;
                case 0x18E628F0:
                    readCellVoltages(pack, data, 24);
                break;
                case 0x1806E5F0:
                    readChargingInfo(pack, data);
                break;
            }
        } catch (final IOException e) {
            LOG.error("Error receiving frame!", e);
        }
    }


    // 0x2F4
    protected void readBatteryStatus(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Battery voltage (0.1V)
        pack.packVoltage = data.getChar();
        // Battery current (0.1A) offset 4000
        pack.packCurrent = data.getChar() - 4000;
        // Battery SOC (1%)
        pack.packSOC = data.get() * 10;
        // skip 1 byte
        data.get();
        // discharge time, e.g. 100h (not mapped)
        data.getShort();
    }


    // 0x4F4
    private void readCellVoltage(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell voltage (1mV)
        pack.maxCellmV = data.getChar();
        // Maximum cell voltage cell number
        pack.maxCellVNum = data.get();
        // Minimum cell voltage (1mV)
        pack.minCellmV = data.getChar();
        // Minimum cell voltage cell number
        pack.minCellVNum = data.get();
    }


    // 0x5F4
    private void readCellTemperature(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell temperature (1C) offset -50
        pack.tempMax = (data.get() - 50) * 10;
        // Maximum cell temperature cell number
        pack.tempMaxCellNum = data.get();
        // Minimum cell temperature (1C) offset -50
        pack.tempMin = (data.get() - 50) * 10;
        // Minimum cell temperature cell number
        pack.tempMinCellNum = data.get();
        // Average cell temperature (1C) offset -50
        pack.tempAverage = (data.get() - 50) * 10;
    }


    private AlarmLevel getAlarmLevel(final int value) {
        return value == 0 ? AlarmLevel.NONE : value == 1 ? AlarmLevel.WARNING : AlarmLevel.ALARM;
    }


    // 0x7F4
    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.NONE);

        // read first 8 bits
        byte value = data.get();

        // unit overvoltage
        pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, getAlarmLevel(BitUtil.bits(value, 0, 2)));

        // unit undervoltage
        pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, getAlarmLevel(BitUtil.bits(value, 2, 2)));

        // total voltage overvoltage
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, getAlarmLevel(BitUtil.bits(value, 4, 2)));

        // total voltage undervoltage
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, getAlarmLevel(BitUtil.bits(value, 6, 2)));

        // read next 8 bits
        value = data.get();

        // Large pressure difference in cell
        pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, getAlarmLevel(BitUtil.bits(value, 0, 2)));

        // discharge overcurrent
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, getAlarmLevel(BitUtil.bits(value, 2, 2)));

        // charge overcurrent
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, getAlarmLevel(BitUtil.bits(value, 4, 2)));

        // temperature too high
        pack.setAlarm(Alarm.PACK_TEMPERATURE_HIGH, getAlarmLevel(BitUtil.bits(value, 6, 2)));

        // read next 8 bits
        value = data.get();

        // temperature too low
        pack.setAlarm(Alarm.PACK_TEMPERATURE_LOW, getAlarmLevel(BitUtil.bits(value, 0, 2)));

        // excessive temperature difference
        pack.setAlarm(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH, getAlarmLevel(BitUtil.bits(value, 2, 2)));

        // SOC too low
        pack.setAlarm(Alarm.SOC_LOW, getAlarmLevel(BitUtil.bits(value, 4, 2)));

        // insulation too low (not mapped)
        pack.setAlarm(Alarm.FAILURE_OTHER, getAlarmLevel(BitUtil.bits(value, 6, 2)));

        // read next 8 bits
        value = data.get();

        // high voltage interlock fault
        pack.setAlarm(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, getAlarmLevel(BitUtil.bits(value, 0, 2)));

        // external communication failure
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_EXTERNAL, getAlarmLevel(BitUtil.bits(value, 2, 2)));

        // internal communication failure
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_INTERNAL, getAlarmLevel(BitUtil.bits(value, 4, 2)));

    }


    // 0x18F128F4
    private void readBatteryStatus2(final BatteryPack pack, final ByteBuffer data) {
        // Remaining capacity AH (0.1AH)
        pack.remainingCapacitymAh = data.getChar() * 100;
        // Rated capacity AH (0.1AH)
        pack.ratedCapacitymAh = data.getChar() * 100;
        data.getChar();
        // bms cycles
        pack.bmsCycles = data.getChar();
    }


    // 0x18F228F4
    private void readCelTemperatures2(final BatteryPack pack, final ByteBuffer data) {
        final byte mask = data.get();
        int i = 0;

        while (i < 5 && BitUtil.bit(mask, i)) {
            pack.cellTemperature[i] = (data.get() - 50) * 10;
            i++;
        }
    }


    // 0x18F328F4
    private void readAlarms2(final BatteryPack pack, final ByteBuffer data) {
    }


    // 0x18F428F4
    private void readBMSInfo2(final BatteryPack pack, final ByteBuffer data) {
    }


    // 0x18F528F4
    private void readBmsStatus2(final BatteryPack pack, final ByteBuffer data) {
    }


    // 0x18E028F4
    private void readCellVoltages(final BatteryPack pack, final ByteBuffer data, final int offset) {
        // max 25 cells are read
        for (int i = 0; i < 4 && offset + i < 25; i++) {
            pack.cellVmV[i + offset] = data.getChar();
            i++;
        }
    }


    // 0x1806E5F4
    private void readChargingInfo(final BatteryPack pack, final ByteBuffer data) {
        // Charging voltage (0.1V)
        data.getChar();
        // Charging current (0.1A)
        pack.packCurrent = data.getChar();
    }


    public static void main(final String[] args) {
        final JKBmsCANProcessor jk = new JKBmsCANProcessor();

        final ByteBuffer data = ByteBuffer.wrap(new byte[] { (byte) 0xEC, 0x01, (byte) 0xA0, 0x0F, 0x33, 0x00, 0x00, 0x00 }).order(ByteOrder.LITTLE_ENDIAN);
        System.out.println(Port.printBuffer(data));
        final BatteryPack pack = new BatteryPack();
        jk.readBatteryStatus(pack, data);
        System.out.println(pack.packVoltage / 10 + "V, " + pack.packCurrent / 10 + "A, " + pack.packSOC / 10 + "%");
        ;
    }
}
