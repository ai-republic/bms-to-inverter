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
package com.airepublic.bmstoinverter.inverter.pylon.rs485;

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
import com.airepublic.bmstoinverter.core.util.ByteAsciiConverter;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The class to handle RS485 messages for Pylontech {@link Inverter}.
 */
@ApplicationScoped
public class PylonInverterRS485Processor extends Inverter {
    private final static byte ADDRESS = 0x12;

    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        final List<ByteBuffer> frames = new ArrayList<>();

        frames.add(createSystemInfo(aggregatedPack)); // 0x60
        frames.add(createBatteryInformation(aggregatedPack)); // 0x61
        frames.add(createAlarms(aggregatedPack)); // 0x62

        return frames;
    }


    // 0x60
    private ByteBuffer createSystemInfo(final BatteryPack aggregatedPack) {
        final ByteBuffer buffer = ByteBuffer.allocate(4096);

        buffer.put(ByteAsciiConverter.convertStringToAsciiBytes("Battery", 10));
        buffer.put(ByteAsciiConverter.convertStringToAsciiBytes(aggregatedPack.manufacturerCode, 20));
        buffer.put(ByteAsciiConverter.convertStringToAsciiBytes(aggregatedPack.softwareVersion, 2));
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) aggregatedPack.numberOfCells));

        for (int i = 0; i < aggregatedPack.numberOfCells; i++) {
            buffer.put(ByteAsciiConverter.convertStringToAsciiBytes("Battery S/N #" + i, 16));
        }

        final byte[] data = new byte[buffer.position()];
        buffer.get(data, 0, buffer.position());

        return prepareSendFrame(ADDRESS, (byte) 0x46, (byte) 0x60, data);
    }


    // 0x61
    private ByteBuffer createBatteryInformation(final BatteryPack aggregatedPack) {
        final ByteBuffer buffer = ByteBuffer.allocate(4096);

        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) (aggregatedPack.packVoltage * 100)));
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) (aggregatedPack.packCurrent * 10)));
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) (aggregatedPack.packSOC / 10)));
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) aggregatedPack.bmsCycles)); // average
                                                                                                   // cycles
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) 10000)); // maximum cycles
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) (aggregatedPack.packSOH / 10))); // average
                                                                                                      // SOH
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) (aggregatedPack.packSOH / 10))); // lowest
                                                                                                      // SOH

        // find the pack with the highest/lowest cell voltage
        int maxPack = 0;
        int minPack = 0;

        for (int i = 0; i < getEnergyStorage().getBatteryPacks().size(); i++) {
            final BatteryPack pack = getEnergyStorage().getBatteryPack(i);

            if (pack.maxCellmV == aggregatedPack.maxCellmV) {
                maxPack = i;
            }

            if (pack.minCellmV == aggregatedPack.minCellmV) {
                minPack = i;
            }
        }

        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) aggregatedPack.maxCellmV));
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) maxPack)); // battery pack with
                                                                                // highest voltage
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) aggregatedPack.maxCellVNum)); // cell
                                                                                                   // with
                                                                                                   // highest
        // voltage

        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) aggregatedPack.minCellmV));
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) minPack)); // battery pack with
                                                                                // lowest voltage
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) aggregatedPack.minCellVNum)); // cell
                                                                                                   // with
                                                                                                   // lowest
        // voltage

        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));

        // find the pack with the highest/lowest cell temperature
        maxPack = 0;
        minPack = 0;

        for (int i = 0; i < getEnergyStorage().getBatteryPacks().size(); i++) {
            final BatteryPack pack = getEnergyStorage().getBatteryPack(i);

            if (pack.maxCellmV == aggregatedPack.tempMaxCellNum) {
                maxPack = i;
            }

            if (pack.minCellmV == aggregatedPack.tempMinCellNum) {
                minPack = i;
            }
        }

        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) (aggregatedPack.tempMax + 2731)));
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) maxPack));
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) aggregatedPack.tempMaxCellNum));

        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) (aggregatedPack.tempMin + 2731)));
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) minPack));
        buffer.put(ByteAsciiConverter.convertByteToAsciiBytes((byte) aggregatedPack.tempMinCellNum));

        // MOSFET average temperature
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // MOSFET max temperature
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // MOSFET max temperature pack
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) 0));
        // MOSFET min temperature
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // MOSFET min temperature pack
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) 0));

        // BMS average temperature
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // BMS max temperature
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // BMS max temperature pack
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) 0));
        // BMS min temperature
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // BMS min temperature pack
        buffer.put(ByteAsciiConverter.convertShortToAsciiBytes((short) 0));

        final byte[] data = new byte[buffer.position()];
        buffer.get(data, 0, buffer.position());

        return prepareSendFrame(ADDRESS, (byte) 0x46, (byte) 0x61, data);
    }


    // 0x62
    private ByteBuffer createAlarms(final BatteryPack pack) {
        final byte[] alarms = new byte[8];

        // warning alarms 1
        byte value = 0;
        value = BitUtil.setBit(value, 7, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        value = BitUtil.setBit(value, 6, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.WARNING);
        value = BitUtil.setBit(value, 5, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        value = BitUtil.setBit(value, 4, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.WARNING);
        value = BitUtil.setBit(value, 3, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        value = BitUtil.setBit(value, 2, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_LOW) == AlarmLevel.WARNING);
        value = BitUtil.setBit(value, 1, false);
        value = BitUtil.setBit(value, 0, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.WARNING);
        byte[] bytes = ByteAsciiConverter.convertByteToAsciiBytes(value);
        alarms[0] = bytes[0];
        alarms[1] = bytes[1];

        // warning alarms 2
        value = 0;
        value = BitUtil.setBit(value, 7, pack.getAlarmLevel(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH) == AlarmLevel.WARNING);
        value = BitUtil.setBit(value, 6, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);
        value = BitUtil.setBit(value, 5, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);
        value = BitUtil.setBit(value, 4, pack.getAlarmLevel(Alarm.FAILURE_COMMUNICATION_INTERNAL) == AlarmLevel.WARNING);
        value = BitUtil.setBit(value, 3, false);
        value = BitUtil.setBit(value, 2, false);
        value = BitUtil.setBit(value, 1, false);
        value = BitUtil.setBit(value, 0, false);
        bytes = ByteAsciiConverter.convertByteToAsciiBytes(value);
        alarms[2] = bytes[0];
        alarms[3] = bytes[1];

        // protection alarms 1
        value = 0;
        value = BitUtil.setBit(value, 7, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        value = BitUtil.setBit(value, 6, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.ALARM);
        value = BitUtil.setBit(value, 5, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        value = BitUtil.setBit(value, 4, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.ALARM);
        value = BitUtil.setBit(value, 3, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        value = BitUtil.setBit(value, 2, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        value = BitUtil.setBit(value, 1, false);
        value = BitUtil.setBit(value, 0, false);
        bytes = ByteAsciiConverter.convertByteToAsciiBytes(value);
        alarms[4] = bytes[0];
        alarms[5] = bytes[1];

        // protection alarms 2
        value = 0;
        value = BitUtil.setBit(value, 7, false);
        value = BitUtil.setBit(value, 6, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        value = BitUtil.setBit(value, 5, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        value = BitUtil.setBit(value, 4, false);
        value = BitUtil.setBit(value, 3, pack.getAlarmLevel(Alarm.FAILURE_OTHER) == AlarmLevel.ALARM);
        BitUtil.setBit(value, 2, false);
        value = BitUtil.setBit(value, 1, false);
        value = BitUtil.setBit(value, 0, false);
        bytes = ByteAsciiConverter.convertByteToAsciiBytes(value);
        alarms[6] = bytes[0];
        alarms[7] = bytes[1];

        return prepareSendFrame(ADDRESS, (byte) 0x46, (byte) 0x62, alarms);
    }


    @Override
    protected ByteBuffer readRequest(final Port port) throws IOException {
        return null;
    }


    @Override
    protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
        port.sendFrame(frame);
    }


    ByteBuffer prepareSendFrame(final byte address, final byte cid1, final byte cid2, final byte[] data) {
        final ByteBuffer sendFrame = ByteBuffer.allocate(18 + data.length * 2).order(ByteOrder.BIG_ENDIAN);
        sendFrame.put((byte) 0x7E); // Start flag
        sendFrame.put((byte) 0x32); // version
        sendFrame.put((byte) 0x30); // version
        sendFrame.put(ByteAsciiConverter.convertByteToAsciiBytes(address)); // address
        sendFrame.put(ByteAsciiConverter.convertByteToAsciiBytes(cid1)); // command CID1
        sendFrame.put(ByteAsciiConverter.convertByteToAsciiBytes(cid2)); // command CID2
        // Frame Length Byte
        sendFrame.put(createLengthCheckSum(data.length * 2));
        // data
        sendFrame.put(data);
        // checksum
        sendFrame.put(createChecksum(sendFrame));
        sendFrame.put((byte) 0x0D); // End flag

        return sendFrame;
    }


    private byte[] createChecksum(final ByteBuffer sendFrame) {
        long checksum = 0;

        // add all values except SOI, checksum and EOI
        for (int i = 1; i < sendFrame.capacity() - 5; i++) {
            checksum += sendFrame.get(i);
        }

        // modulo remainder of 65535
        checksum %= 65535;

        // invert
        checksum = ~checksum;
        // add 1
        checksum++;

        // extract the high and low bytes
        final byte high = (byte) (checksum >> 8);
        final byte low = (byte) (checksum & 0x000000FF);
        // convert them to ascii
        final byte[] highBytes = ByteAsciiConverter.convertByteToAsciiBytes(high);
        final byte[] lowBytes = ByteAsciiConverter.convertByteToAsciiBytes(low);
        final byte[] data = new byte[4];
        data[0] = highBytes[0];
        data[1] = highBytes[1];
        data[2] = lowBytes[0];
        data[3] = lowBytes[1];

        return data;

    }


    private byte[] createLengthCheckSum(final int length) {

        // spit the first 12 bits into groups of 4 bits and accumulate
        int chksum = (byte) BitUtil.bits(length, 0, 4) + (byte) BitUtil.bits(length, 4, 4) + (byte) BitUtil.bits(length, 8, 4);
        // modulo 16 remainder
        chksum %= 16;
        // invert
        chksum = ~chksum & 0xff;
        chksum &= 0x0000000f;
        // and finally +1
        chksum++;

        // combine the checksum and length
        int dataValue = chksum;
        dataValue = dataValue << 12;
        dataValue += length;

        // extract the high and low bytes
        final byte high = (byte) (dataValue >> 8);
        final byte low = (byte) (dataValue & 0x000000FF);
        // convert them to ascii
        final byte[] highBytes = ByteAsciiConverter.convertByteToAsciiBytes(high);
        final byte[] lowBytes = ByteAsciiConverter.convertByteToAsciiBytes(low);
        final byte[] data = new byte[4];
        data[0] = highBytes[0];
        data[1] = highBytes[1];
        data[2] = lowBytes[0];
        data[3] = lowBytes[1];

        return data;
    }


    public static void main(final String[] args) {
        byte value;
        value = 0;

        // System.out.println(value &= ~(1 << 7));
        value = BitUtil.setBit(value, 0, false);
        value = BitUtil.setBit(value, 1, false);
        value = BitUtil.setBit(value, 2, false);
        value = BitUtil.setBit(value, 3, false);
        value = BitUtil.setBit(value, 4, false);
        value = BitUtil.setBit(value, 5, false);
        value = BitUtil.setBit(value, 6, false);
        value = BitUtil.setBit(value, 7, true);

        System.out.println(value);

        final BatteryPack pack = new BatteryPack();
        value = 0;
        BitUtil.setBit(value, 7, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        BitUtil.setBit(value, 6, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.WARNING);
        BitUtil.setBit(value, 5, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        BitUtil.setBit(value, 4, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.WARNING);
        BitUtil.setBit(value, 3, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        BitUtil.setBit(value, 2, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_LOW) == AlarmLevel.WARNING);
        BitUtil.setBit(value, 1, false);
        BitUtil.setBit(value, 0, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.WARNING);
        System.out.println(value);

    }
}
