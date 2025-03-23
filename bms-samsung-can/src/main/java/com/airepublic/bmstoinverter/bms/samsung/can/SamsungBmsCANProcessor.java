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
package com.airepublic.bmstoinverter.bms.samsung.can;

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
 * The class to handle CAN messages from a Pylon {@link BMS}.
 */
public class SamsungBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(SamsungBmsCANProcessor.class);
    private final static int BATTERY_ID = 0;

    @Override
    public void collectData(final Port port) {
        try {
            final BatteryPack pack = getBatteryPack(BATTERY_ID);
            final ByteBuffer frame = port.receiveFrame();
            final int frameId = frame.getInt();
            final byte[] bytes = new byte[8];
            frame.position(8);
            frame.get(bytes);
            final ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

            switch (frameId) {
                case 0x500:
                    readBatteryInfo(pack, data);
                break;
                case 0x501:
                    readAlarms(pack, data);
                break;
                case 0x502:
                    readChargeDischargeInfo(pack, data);
                break;
                case 0x503:
                    readMinMaxVoltage(pack, data);
                break;
                case 0x504:
                    readMinMaxTemperature(pack, data);
                break;
                case 0x505:
                    readProtocolVersion(pack, data);
                break;
                case 0x5F0:
                    readCellVoltages(pack, data, 0);
                break;
                case 0x5F1:
                    readCellVoltages(pack, data, 3);
                break;
                case 0x5F2:
                    readCellVoltages(pack, data, 6);
                break;
                case 0x5F3:
                    readCellVoltages(pack, data, 9);
                break;
                case 0x5F4:
                    readCellVoltages(pack, data, 12);
                break;
            }
        } catch (final IOException e) {
            LOG.error("Error receiving frame!", e);
        }
    }


    // 0x500
    protected void readBatteryInfo(final BatteryPack pack, final ByteBuffer data) {
        // Battery voltage (0.01V) - uint_16
        pack.packVoltage = data.getChar() / 10;
        // Battery current (1A) - sint_16
        pack.packCurrent = data.getShort() * 10;
        // SOC (1%) - uint_8
        pack.packSOC = data.get() * 10;
        // SOH (1%) - uint_8
        pack.packSOH = data.get() * 10;
        // BMS cycles
        pack.bmsCycles = data.getChar();

        LOG.debug("\nPack V \t Pack A \t Avg \nSOC \tSOH\\n{} \t{}  {}\t\t  {}", pack.packVoltage / 10f, pack.packCurrent / 10f, pack.packSOC / 10f, pack.packSOH / 10f);
    }


    // 0x501
    protected void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        // read first 4 bytes
        final byte alarm1 = data.get();
        data.get();
        final byte protection1 = data.get();
        final byte protection2 = data.get();

        // alarms
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(alarm1, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(alarm1, 1) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_TEMPERATURE_HIGH, BitUtil.bit(alarm1, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_TEMPERATURE_LOW, BitUtil.bit(alarm1, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_CURRENT_HIGH, BitUtil.bit(alarm1, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_CURRENT_LOW, BitUtil.bit(alarm1, 5) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(alarm1, 6) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, BitUtil.bit(alarm1, 7) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // protection
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(protection1, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(protection1, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_TEMPERATURE_HIGH, BitUtil.bit(protection1, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_TEMPERATURE_LOW, BitUtil.bit(protection1, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(protection1, 4) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(protection1, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(protection1, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        pack.setAlarm(Alarm.FAILURE_OTHER, BitUtil.bit(protection2, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_INTERNAL, BitUtil.bit(protection2, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_CHARGE_BREAKER, BitUtil.bit(protection2, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(protection2, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(protection2, 4) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, BitUtil.bit(protection2, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(protection2, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // Number of total trays
        pack.modulesInSeries = data.get();
        // Each tray has 14 cells
        pack.numberOfCells = pack.modulesInSeries * 14;
    }


    // 0x502
    protected void readChargeDischargeInfo(final BatteryPack pack, final ByteBuffer data) {
        // Battery charge voltage (0.1V) - uint_16
        pack.maxPackVoltageLimit = data.getChar();
        // Charge current limit (0.1A) - uint_16
        pack.maxPackChargeCurrent = data.getChar();
        // Discharge current limit (0.1A) - uint_16
        pack.maxPackDischargeCurrent = data.getChar();
        // Battery discharge voltage (0.1V) - uint_16
        pack.minPackVoltageLimit = data.getChar();

        LOG.debug("\nMax Voltage \tMax Charge \tMax Discharge \tMin Voltage\n  {}\t\t{}\t\t{}\t\t", pack.maxPackVoltageLimit / 10f, pack.maxPackChargeCurrent / 10f, pack.maxPackDischargeCurrent / 10f, pack.minPackVoltageLimit / 10f);
    }


    // 0x503
    protected void readMinMaxVoltage(final BatteryPack pack, final ByteBuffer data) {
        // Average cell voltage (1mV) - uint_16
        data.getShort();
        // Maximum cell voltage (1mV) - uint_16
        pack.maxCellmV = data.getChar();
        // Minimum cell voltage (1mV) - uint_16
        pack.minCellmV = data.getChar();
        // Average tray voltage (1mV) - uint_16
        data.getShort();

        LOG.debug("\nMax Cell mV \tMin Cell mV\n{}\t{}", pack.maxCellmV / 1000f, pack.minCellmV / 1000f);
    }


    // 0x504
    protected void readMinMaxTemperature(final BatteryPack pack, final ByteBuffer data) {
        // Maximum tray voltage (1mV) - uint_16
        data.getShort();
        // Minimum tray voltage (1mV) - uint_16
        data.getShort();
        // Average cell temperature (1C) - uint_8
        pack.tempAverage = data.get() * 10;
        // Maximum cell temperature (1C) - uint_8
        pack.tempMax = data.get() * 10;
        // Minimum cell temperature (1C) - uint_8
        pack.tempMin = data.get() * 10;

        LOG.debug("\nAvg Temp \tMax Temp \tMin Temp\n\t{}\t{}\t{}", pack.tempAverage, pack.tempMax / 10f, pack.tempMin / 10f);
    }


    // 0x505
    protected void readProtocolVersion(final BatteryPack pack, final ByteBuffer data) {
        // Major version - uint_8
        pack.softwareVersion = "" + data.get();
        // Minor version - uint_8
        pack.softwareVersion = pack.softwareVersion + "." + data.get();

        LOG.debug("\nProtocol version\n{}", pack.softwareVersion);
    }


    // 0x5F0 - 0x5F4
    protected void readCellVoltages(final BatteryPack pack, final ByteBuffer data, final int cellNoStart) {
        // Tray id
        final int tray = data.getShort() - 1;
        // Cell voltage (1mV)
        pack.cellVmV[cellNoStart + tray * 14] = data.getChar();
        // Cell voltage (1mV)
        pack.cellVmV[cellNoStart + tray * 14 + 1] = data.getChar();

        if (cellNoStart != 12) {
            // Cell voltage (1mV)
            pack.cellVmV[cellNoStart + tray * 14 + 2] = data.getChar();
        }
    }

}
