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
package com.airepublic.bmstoinverter.bms.seplos.can;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.BitUtil;

/**
 * The class to handle CAN messages from a Seplos {@link BMS}.
 */
public class SeplosBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(SeplosBmsCANProcessor.class);
    private final static int BATTERY_ID = 0;

    @Override
    public void collectData(final Port port) {
        try {
            final BatteryPack pack = getBatteryPack(BATTERY_ID);
            final ByteBuffer frame = port.receiveFrame();
            final int frameId = frame.getInt();
            final byte[] bytes = new byte[8];
            frame.get(bytes);
            final ByteBuffer data = ByteBuffer.wrap(bytes);

            switch (frameId) {
                case 0x351:
                    readChargeDischargeInfo(pack, data);
                break;
                case 0x355:
                    readSOC(pack, data);
                break;
                case 0x356:
                    readBatteryVoltage(pack, data);
                break;
                case 0x35C:
                    requestChargeDischargeConfigChange(pack, data);
                break;
                case 0x370:
                    readMinMaxTemperatureVoltage(pack, data);
                break;
                case 0x371:
                    readTemperatureIds(pack, data);
                break;
                case 0x35E:
                    readManufacturer(pack, data);
                break;
                case 0x359:
                    readAlarms(pack, data);
                break;
            }
        } catch (final IOException e) {
            LOG.error("Error receiving frame!", e);
        }
    }


    // 0x351
    private void readChargeDischargeInfo(final BatteryPack pack, final ByteBuffer data) {
        // Battery charge voltage (0.1V) - uint_16
        pack.maxPackVoltageLimit = data.getChar();
        // Charge current limit (0.1A) - sint_16
        pack.maxPackChargeCurrent = data.getShort();
        // Discharge current limit (0.1A) - sint_16
        pack.maxPackDischargeCurrent = data.getShort();
        // Battery discharge voltage (0.1V) - uint_16
        pack.minPackVoltageLimit = data.getChar();

    }


    // 0x355
    private void readSOC(final BatteryPack pack, final ByteBuffer data) {
        // SOC (1%) - uint_16
        pack.maxPackDischargeCurrent = data.getChar();
        // SOH (1%) - uint_16
        pack.packVoltage = data.getChar();
    }


    // 0x356
    private void readBatteryVoltage(final BatteryPack pack, final ByteBuffer data) {
        // Battery voltage (0.01V) - uint_16
        pack.packVoltage = data.getShort();
        // Battery current (0.1A) - uint_16
        pack.packCurrent = data.getShort();
        // Battery current (0.1C) - uint_16
        pack.tempAverage = data.getShort();
    }


    // 0x35C
    private void requestChargeDischargeConfigChange(final BatteryPack pack, final ByteBuffer data) {
        final byte bits = data.get();

        if (bitRead(bits, 4)) {
            // request force-charge II
        }

        if (bitRead(bits, 5)) {
            // request force-charge I
        }

        if (bitRead(bits, 6)) {
            // request discharge enable
        }

        if (bitRead(bits, 7)) {
            // request charge enable
        }
    }


    // 0x370
    private void readMinMaxTemperatureVoltage(final BatteryPack pack, final ByteBuffer data) {
        // Maximum cell temperature (0.1C) - uint_16
        pack.tempMax = data.getShort();
        // Minimum cell temperature (0.1C) - uint_16
        pack.tempMin = data.getShort();
        // Maximum cell voltage (0.1V) - uint_16
        pack.maxCellmV = data.getShort();
        // Minimum cell voltage (0.1V) - uint_16
        pack.minCellmV = data.getShort();
    }


    // 0x371
    private void readTemperatureIds(final BatteryPack pack, final ByteBuffer data) {
        // Maximum cell temperature (0.1C) - uint_16
        // pack.tempMax = data.getShort();
        // Minimum cell temperature (0.1C) - uint_16
        // pack.tempMin = data.getShort();
        // Maximum cell voltage id - uint_16
        pack.maxCellVNum = data.getShort();
        // Minimum cell voltage id - uint_16
        pack.minCellVNum = data.getShort();
    }


    // 0x35E
    private void readManufacturer(final BatteryPack pack, final ByteBuffer data) {
        final char first = (char) data.get();
        final char second = (char) data.get();

        pack.manufacturerCode = "" + first + second;
    }


    private AlarmLevel getAlarmLevel(final boolean warning, final boolean alarm) {
        return alarm ? AlarmLevel.ALARM : warning ? AlarmLevel.WARNING : AlarmLevel.NONE;
    }


    // 0x359
    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        // read first 8 bytes
        final int protection1 = data.get();
        final int protection2 = data.get();
        final int alarm1 = data.get();
        final int alarm2 = data.get();

        // protection and alarms
        pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, getAlarmLevel(BitUtil.bit(protection1, 1), BitUtil.bit(alarm1, 1)));
        pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, getAlarmLevel(BitUtil.bit(protection1, 2), BitUtil.bit(alarm1, 2)));
        pack.setAlarm(Alarm.CELL_TEMPERATURE_HIGH, getAlarmLevel(BitUtil.bit(protection1, 3), BitUtil.bit(alarm1, 3)));
        pack.setAlarm(Alarm.CELL_TEMPERATURE_LOW, getAlarmLevel(BitUtil.bit(protection1, 4), BitUtil.bit(alarm1, 4)));
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, getAlarmLevel(BitUtil.bit(protection1, 7), BitUtil.bit(alarm1, 7)));
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, getAlarmLevel(BitUtil.bit(protection2, 0), BitUtil.bit(alarm2, 7)));
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_INTERNAL, BitUtil.bit(alarm2, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, BitUtil.bit(protection2, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        pack.numberOfCells = data.get();

        // skip two bytes
        data.getShort();

        // dip switch
        data.get();
    }


    private boolean bitRead(final int value, final int index) {
        return (value >> index & 1) == 1;
    }

}
