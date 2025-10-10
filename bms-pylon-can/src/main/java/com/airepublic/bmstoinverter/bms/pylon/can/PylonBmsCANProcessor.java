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
package com.airepublic.bmstoinverter.bms.pylon.can;

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
public class PylonBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(PylonBmsCANProcessor.class);
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
                case 0x351:
                    readChargeDischargeInfo(pack, data);
                break;
                case 0x354:
                    readCapacity(pack, data);
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
    protected void readChargeDischargeInfo(final BatteryPack pack, final ByteBuffer data) {
        // Battery charge voltage (0.1V) - uint_16
        pack.maxPackVoltageLimit = data.getChar();
        // Charge current limit (0.1A) - sint_16
        pack.maxPackChargeCurrent = data.getShort();
        // Discharge current limit (0.1A) - sint_16
        pack.maxPackDischargeCurrent = data.getShort();
        // Battery discharge voltage (0.1V) - uint_16
        pack.minPackVoltageLimit = data.getChar();

        LOG.debug("\nMax Voltage \tMax Charge \tMax Discharge \tMin Voltage\n  {}\t\t{}\t\t{}\t\t", pack.maxPackVoltageLimit / 10f, pack.maxPackChargeCurrent / 10f, pack.maxPackDischargeCurrent / 10f, pack.minPackVoltageLimit / 10f);
    }


    // 0x354
    protected void readCapacity(final BatteryPack pack, final ByteBuffer data) {
        // Battery capacity (0.1Ah) - uint_16
        pack.ratedCapacitymAh = data.getChar() * 100;
        // Remaining capacity (0.1Ah) - uint_16
        pack.remainingCapacitymAh = data.getChar() * 100;

        LOG.debug("\nCapacity \tRemaining\n \t{}\t\t{}", pack.ratedCapacitymAh / 1000f, pack.remainingCapacitymAh / 1000f);
    }


    // 0x355
    protected void readSOC(final BatteryPack pack, final ByteBuffer data) {
        // SOC (1%) - uint_16
        pack.packSOC = data.getChar() * 10;
        // SOH (1%) - uint_16
        pack.packSOH = data.getChar() * 10;

        LOG.debug("\nSOC \tSOH\n{} \t{}", pack.packSOC / 10f, pack.packSOH / 10f);
    }


    // 0x356
    protected void readBatteryVoltage(final BatteryPack pack, final ByteBuffer data) {
        // Battery voltage (0.01V) - sint_16
        pack.packVoltage = data.getShort() / 10;
        // Battery current (0.1A) - sint_16
        pack.packCurrent = data.getShort();
        // Battery current (0.1C) - sint_16
        pack.tempAverage = data.getShort();

        LOG.debug("\nPack V \t Pack A \t Avg Temp\n {}\t  {}\t\t  {}", pack.packVoltage / 10f, pack.packCurrent / 10f, pack.tempAverage / 10f);
    }


    // 0x35C
    protected void requestChargeDischargeConfigChange(final BatteryPack pack, final ByteBuffer data) {
        final byte bits = data.get();

        if (BitUtil.bit(bits, 4)) {
            // request force-charge II
        }

        if (BitUtil.bit(bits, 5)) {
            // request force-charge I
        }

        if (BitUtil.bit(bits, 6)) {
            // request discharge enable
        }

        if (BitUtil.bit(bits, 7)) {
            // request charge enable
        }
    }


    // 0x370
    protected void readMinMaxTemperatureVoltage(final BatteryPack pack, final ByteBuffer data) {
        // Maximum cell temperature (0.1C) - uint_16
        pack.tempMax = data.getShort();
        // Minimum cell temperature (0.1C) - uint_16
        pack.tempMin = data.getShort();
        // Maximum cell voltage (0.1V) - uint_16
        pack.maxCellmV = data.getShort();
        // Minimum cell voltage (0.1V) - uint_16
        pack.minCellmV = data.getShort();

        LOG.debug("\nMax Temp \tMin Temp \tMax Cell mV \tMin Cell mV\n{} \t {}\t\t{}\t\t{}", pack.tempMax / 10f, pack.tempMin / 10f, pack.maxCellmV, pack.minCellmV);
    }


    // 0x371
    protected void readTemperatureIds(final BatteryPack pack, final ByteBuffer data) {
        // Maximum cell temperature (0.1C) - uint_16
        // pack.tempMax = data.getShort();
        // Minimum cell temperature (0.1C) - uint_16
        // pack.tempMin = data.getShort();
        // Maximum cell voltage id - uint_16
        pack.maxCellVNum = data.getShort();
        // Minimum cell voltage id - uint_16
        pack.minCellVNum = data.getShort();

        LOG.debug("\nMax V Cell \t Min V Cell\n\t{}\t\t{}", pack.maxCellVNum, pack.minCellVNum);
    }


    // 0x35E
    protected void readManufacturer(final BatteryPack pack, final ByteBuffer data) {
        pack.manufacturerCode = "";
        byte chr;

        do {
            chr = data.get();

            if (chr != 0x00) {
                pack.manufacturerCode += (char) chr;
            }
        } while (chr != 0x00 && data.position() < data.capacity());

        LOG.debug("\nManufacturer: {}", pack.manufacturerCode);
    }


    private AlarmLevel getAlarmLevel(final boolean warning, final boolean alarm) {
        return alarm ? AlarmLevel.ALARM : warning ? AlarmLevel.WARNING : AlarmLevel.NONE;
    }


    // 0x359
    protected void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        // read first 4 bytes
        final int protection1 = data.get();
        final int protection2 = data.get();
        final int alarm1 = data.get();
        final int alarm2 = data.get();

        // clear all alarms
        pack.alarms.clear();

        // protection and alarms
        pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, getAlarmLevel(BitUtil.bit(protection1, 1), BitUtil.bit(alarm1, 1)));
        pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, getAlarmLevel(BitUtil.bit(protection1, 2), BitUtil.bit(alarm1, 2)));
        pack.setAlarm(Alarm.CELL_TEMPERATURE_HIGH, getAlarmLevel(BitUtil.bit(protection1, 3), BitUtil.bit(alarm1, 3)));
        pack.setAlarm(Alarm.CELL_TEMPERATURE_LOW, getAlarmLevel(BitUtil.bit(protection1, 4), BitUtil.bit(alarm1, 4)));
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, getAlarmLevel(BitUtil.bit(protection1, 7), BitUtil.bit(alarm1, 7)));
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, getAlarmLevel(BitUtil.bit(protection2, 0), BitUtil.bit(alarm2, 7)));
        pack.setAlarm(Alarm.CELL_TEMPERATURE_HIGH, BitUtil.bit(protection2, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_INTERNAL, BitUtil.bit(alarm2, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        pack.numberOfCells = data.get();

        // skip two bytes ('P' and 'N')
        data.getShort();

        // dip switch
        data.get();

    }


    public static void main(final String[] args) {
        // Create a dummy BatteryPack
        final BatteryPack pack = new BatteryPack();
        // Prepare the test data: 00 00 00 00 01 50 4E
        final byte[] testData = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x01, 0x50, 0x4E };
        // Pad to 8 bytes as expected by the method (CAN frame is 8 bytes)
        final byte[] paddedData = new byte[8];
        System.arraycopy(testData, 0, paddedData, 0, testData.length);
        final ByteBuffer data = ByteBuffer.wrap(paddedData);
        // Create processor and call readAlarms
        final PylonBmsCANProcessor processor = new PylonBmsCANProcessor();
        processor.readAlarms(pack, data);
        // Print the alarms and number of cells for verification
        System.out.println("Alarms: " + pack.alarms);
        System.out.println("Number of cells: " + pack.numberOfCells);
    }

}
