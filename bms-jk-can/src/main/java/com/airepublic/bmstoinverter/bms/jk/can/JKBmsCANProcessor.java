package com.airepublic.bmstoinverter.bms.jk.can;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.Util;

/**
 * The class to handle CAN messages from a JK {@link BMS}.
 */
public class JKBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(JKBmsCANProcessor.class);

    @Override
    protected void collectData(final Port port) {
        try {
            final int batteryNo = 0;
            final BatteryPack pack = getBatteryPack(batteryNo);
            final ByteBuffer frame = port.receiveFrame();
            final int frameId = frame.getInt();
            final byte[] bytes = new byte[8];
            frame.get(bytes);
            final ByteBuffer data = ByteBuffer.wrap(bytes);

            switch (frameId) {
                case 0x2F4:
                    readBatteryStatus(pack, data);
                break;
                case 0x4F4:
                    readCellVoltage(pack, data);
                break;
                case 0x5F4:
                    readCellTemperature(pack, data);
                break;
                case 0x7F4:
                    readAlarms(pack, data);
                break;
            }

        } catch (final IOException e) {
            LOG.error("Error receiving frame!", e);
        }
    }


    protected void readBatteryStatus(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Battery voltage (0.1V)
        pack.packVoltage = data.getShort();
        // Battery current (0.1A) offset 4000
        pack.packCurrent = data.getShort() - 4000;
        // Battery SOC (1%)
        pack.packSOC = data.get() * 10;
        // skip 1 byte
        data.get();
        // discharge time, e.g. 100h (not mapped)
        data.getShort();
    }


    private void readCellVoltage(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell voltage (1mV)
        pack.maxCellmV = data.getShort();
        // Maximum cell voltage cell number
        pack.maxCellVNum = data.get();
        // Minimum cell voltage (1mV)
        pack.minCellmV = data.getShort();
        // Minimum cell voltage cell number
        pack.minCellVNum = data.get();
    }


    private void readCellTemperature(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell temperature (1C) offset -50
        pack.tempMax = (data.get() - 50) * 10;
        // Maximum cell temperature cell number
        data.get();
        // Minimum cell temperature (1C) offset -50
        pack.tempMin = (data.get() - 50) * 10;
        // Minimum cell temperature cell number
        data.get();
        // Average cell temperature (1C) offset -50
        pack.tempAverage = (data.get() - 50) * 10;
    }


    private AlarmLevel getAlarmLevel(final int value) {
        return value == 0 ? AlarmLevel.NONE : value == 1 ? AlarmLevel.WARNING : AlarmLevel.ALARM;
    }


    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.NONE);

        // read first 8 bits
        byte value = data.get();

        // unit overvoltage
        pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, getAlarmLevel(Util.bits(value, 0, 2)));

        // unit undervoltage
        pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, getAlarmLevel(Util.bits(value, 2, 2)));

        // total voltage overvoltage
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, getAlarmLevel(Util.bits(value, 4, 2)));

        // total voltage undervoltage
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, getAlarmLevel(Util.bits(value, 6, 2)));

        // read next 8 bits
        value = data.get();

        // Large pressure difference in cell
        pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, getAlarmLevel(Util.bits(value, 0, 2)));

        // discharge overcurrent
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, getAlarmLevel(Util.bits(value, 2, 2)));

        // charge overcurrent
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, getAlarmLevel(Util.bits(value, 4, 2)));

        // temperature too high
        pack.setAlarm(Alarm.PACK_TEMPERATURE_HIGH, getAlarmLevel(Util.bits(value, 6, 2)));

        // read next 8 bits
        value = data.get();

        // temperature too low
        pack.setAlarm(Alarm.PACK_TEMPERATURE_LOW, getAlarmLevel(Util.bits(value, 0, 2)));

        // excessive temperature difference
        pack.setAlarm(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH, getAlarmLevel(Util.bits(value, 2, 2)));

        // SOC too low
        pack.setAlarm(Alarm.SOC_LOW, getAlarmLevel(Util.bits(value, 4, 2)));

        // insulation too low (not mapped)
        pack.setAlarm(Alarm.FAILURE_OTHER, getAlarmLevel(Util.bits(value, 6, 2)));

        // read next 8 bits
        value = data.get();

        // high voltage interlock fault
        pack.setAlarm(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, getAlarmLevel(Util.bits(value, 0, 2)));

        // external communication failure
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_EXTERNAL, getAlarmLevel(Util.bits(value, 2, 2)));

        // internal communication failure
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_INTERNAL, getAlarmLevel(Util.bits(value, 4, 2)));

    }
}
