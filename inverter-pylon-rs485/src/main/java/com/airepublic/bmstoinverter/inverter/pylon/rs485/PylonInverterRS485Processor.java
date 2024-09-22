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
import java.util.BitSet;
import java.util.HexFormat;
import java.util.List;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.Util;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The class to handle CAN messages for Pylontech {@link Inverter}.
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

        buffer.put(convertStringToAsciiBytes("Battery", 10));
        buffer.put(convertStringToAsciiBytes(aggregatedPack.manufacturerCode, 20));
        buffer.put(convertStringToAsciiBytes(aggregatedPack.softwareVersion, 2));
        buffer.put(convertByteToAsciiBytes((byte) aggregatedPack.numberOfCells));

        for (int i = 0; i < aggregatedPack.numberOfCells; i++) {
            buffer.put(convertStringToAsciiBytes("Battery S/N #" + i, 16));
        }

        final byte[] data = new byte[buffer.position()];
        buffer.get(data, 0, buffer.position());

        return prepareSendFrame(ADDRESS, (byte) 0x46, (byte) 0x60, data);
    }


    // 0x61
    private ByteBuffer createBatteryInformation(final BatteryPack aggregatedPack) {
        final ByteBuffer buffer = ByteBuffer.allocate(4096);

        buffer.put(convertShortToAsciiBytes((short) (aggregatedPack.packVoltage * 100)));
        buffer.put(convertShortToAsciiBytes((short) (aggregatedPack.packCurrent * 10)));
        buffer.put(convertByteToAsciiBytes((byte) (aggregatedPack.packSOC / 10)));
        buffer.put(convertShortToAsciiBytes((short) aggregatedPack.bmsCycles)); // average cycles
        buffer.put(convertShortToAsciiBytes((short) 10000)); // maximum cycles
        buffer.put(convertByteToAsciiBytes((byte) (aggregatedPack.packSOH / 10))); // average SOH
        buffer.put(convertByteToAsciiBytes((byte) (aggregatedPack.packSOH / 10))); // lowest SOH

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

        buffer.put(convertShortToAsciiBytes((short) aggregatedPack.maxCellmV));
        buffer.put(convertByteToAsciiBytes((byte) maxPack)); // battery pack with highest voltage
        buffer.put(convertByteToAsciiBytes((byte) aggregatedPack.maxCellVNum)); // cell with highest
                                                                                // voltage

        buffer.put(convertShortToAsciiBytes((short) aggregatedPack.minCellmV));
        buffer.put(convertByteToAsciiBytes((byte) minPack)); // battery pack with lowest voltage
        buffer.put(convertByteToAsciiBytes((byte) aggregatedPack.minCellVNum)); // cell with lowest
                                                                                // voltage

        buffer.put(convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));

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

        buffer.put(convertShortToAsciiBytes((short) (aggregatedPack.tempMax + 2731)));
        buffer.put(convertByteToAsciiBytes((byte) maxPack));
        buffer.put(convertByteToAsciiBytes((byte) aggregatedPack.tempMaxCellNum));

        buffer.put(convertShortToAsciiBytes((short) (aggregatedPack.tempMin + 2731)));
        buffer.put(convertByteToAsciiBytes((byte) minPack));
        buffer.put(convertByteToAsciiBytes((byte) aggregatedPack.tempMinCellNum));

        // MOSFET average temperature
        buffer.put(convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // MOSFET max temperature
        buffer.put(convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // MOSFET max temperature pack
        buffer.put(convertShortToAsciiBytes((short) 0));
        // MOSFET min temperature
        buffer.put(convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // MOSFET min temperature pack
        buffer.put(convertShortToAsciiBytes((short) 0));

        // BMS average temperature
        buffer.put(convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // BMS max temperature
        buffer.put(convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // BMS max temperature pack
        buffer.put(convertShortToAsciiBytes((short) 0));
        // BMS min temperature
        buffer.put(convertShortToAsciiBytes((short) (aggregatedPack.tempAverage + 2731)));
        // BMS min temperature pack
        buffer.put(convertShortToAsciiBytes((short) 0));

        final byte[] data = new byte[buffer.position()];
        buffer.get(data, 0, buffer.position());

        return prepareSendFrame(ADDRESS, (byte) 0x46, (byte) 0x61, data);
    }


    // 0x62
    private ByteBuffer createAlarms(final BatteryPack pack) {
        final byte[] alarms = new byte[8];

        // warning alarms 1
        BitSet bits = new BitSet(8);
        bits.set(7, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        bits.set(6, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.WARNING);
        bits.set(5, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        bits.set(4, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.WARNING);
        bits.set(3, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        bits.set(2, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_LOW) == AlarmLevel.WARNING);
        bits.set(1, false);
        bits.set(0, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.WARNING);
        byte[] bytes = convertByteToAsciiBytes(bits.toByteArray()[0]);
        alarms[0] = bytes[0];
        alarms[1] = bytes[1];

        // warning alarms 2
        bits = new BitSet(8);
        bits.set(7, pack.getAlarmLevel(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH) == AlarmLevel.WARNING);
        bits.set(6, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);
        bits.set(5, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);
        bits.set(4, pack.getAlarmLevel(Alarm.FAILURE_COMMUNICATION_INTERNAL) == AlarmLevel.WARNING);
        bits.set(3, false);
        bits.set(2, false);
        bits.set(1, false);
        bits.set(0, false);
        bytes = convertByteToAsciiBytes(bits.toByteArray()[0]);
        alarms[2] = bytes[0];
        alarms[3] = bytes[1];

        // protection alarms 1
        bits = new BitSet(8);
        bits.set(7, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        bits.set(6, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.ALARM);
        bits.set(5, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        bits.set(4, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.ALARM);
        bits.set(3, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        bits.set(2, pack.getAlarmLevel(Alarm.CELL_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        bits.set(1, false);
        bits.set(0, false);
        bytes = convertByteToAsciiBytes(bits.toByteArray()[0]);
        alarms[4] = bytes[0];
        alarms[5] = bytes[1];

        // protection alarms 2
        bits = new BitSet(8);
        bits.set(7, false);
        bits.set(6, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        bits.set(5, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        bits.set(4, false);
        bits.set(3, pack.getAlarmLevel(Alarm.FAILURE_OTHER) == AlarmLevel.ALARM);
        bits.set(2, false);
        bits.set(1, false);
        bits.set(0, false);
        bytes = convertByteToAsciiBytes(bits.toByteArray()[0]);
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
        sendFrame.put(convertByteToAsciiBytes(address)); // address
        sendFrame.put(convertByteToAsciiBytes(cid1)); // command CID1
        sendFrame.put(convertByteToAsciiBytes(cid2)); // command CID2
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
        final byte[] highBytes = convertByteToAsciiBytes(high);
        final byte[] lowBytes = convertByteToAsciiBytes(low);
        final byte[] data = new byte[4];
        data[0] = highBytes[0];
        data[1] = highBytes[1];
        data[2] = lowBytes[0];
        data[3] = lowBytes[1];

        return data;

    }


    private byte[] createLengthCheckSum(final int length) {

        // spit the first 12 bits into groups of 4 bits and accumulate
        int chksum = (byte) Util.bits(length, 0, 4) + (byte) Util.bits(length, 4, 4) + (byte) Util.bits(length, 8, 4);
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
        final byte[] highBytes = convertByteToAsciiBytes(high);
        final byte[] lowBytes = convertByteToAsciiBytes(low);
        final byte[] data = new byte[4];
        data[0] = highBytes[0];
        data[1] = highBytes[1];
        data[2] = lowBytes[0];
        data[3] = lowBytes[1];

        return data;
    }


    private byte convertAsciiBytesToByte(final byte high, final byte low) {
        final String ascii = new String(new char[] { (char) high, (char) low });
        return (byte) HexFormat.fromHexDigits(ascii);
    }


    private byte[] convertByteToAsciiBytes(final byte value) {
        final byte[] bytes = new byte[2];
        final String byteStr = String.format("%02X", value);
        bytes[0] = (byte) byteStr.charAt(0);
        bytes[1] = (byte) byteStr.charAt(1);

        return bytes;
    }


    private byte[] convertStringToAsciiBytes(final String value, final int noOfCharacters) {
        // create byte array (2 bytes per ascii char representation)
        final byte[] bytes = new byte[noOfCharacters * 2];
        int byteIdx = 0;
        int charIdx = 0;

        while (byteIdx < bytes.length) {
            // check if there are enough characters in the string
            if (charIdx < value.length()) {
                // translate the next character to ascii bytes
                final byte[] hex = convertByteToAsciiBytes((byte) value.charAt(charIdx++));
                bytes[byteIdx++] = hex[0];
                bytes[byteIdx++] = hex[1];
            } else {
                // otherwise fill it up with ascii bytes 0x30 (0x0)
                bytes[byteIdx++] = 0x30;
                bytes[byteIdx++] = 0x30;
            }
        }

        return bytes;
    }


    private byte[] convertShortToAsciiBytes(final short value) {
        final byte first = (byte) ((value & 0xFF00) >> 8);
        final byte second = (byte) (value & 0x00FF);
        final byte[] data = new byte[4];
        System.arraycopy(convertByteToAsciiBytes(first), 0, data, 0, 2);
        System.arraycopy(convertByteToAsciiBytes(second), 0, data, 2, 2);

        return data;
    }


    void printAscii(final String str) {
        final String[] valueStr = str.split(" ");
        int i = 0;

        while (i < valueStr.length) {
            final byte high = (byte) HexFormat.fromHexDigits(valueStr[i++]);
            final byte low = (byte) HexFormat.fromHexDigits(valueStr[i++]);
            System.out.print("" + (char) convertAsciiBytesToByte(high, low));
        }

        System.out.println();
    }


    public static void main(final String[] args) {
        final BitSet b = new BitSet(8);
        b.set(0, true);
        b.set(1, true);
        b.set(2, true);
        b.set(3, true);
        b.set(4, true);
        b.set(5, true);
        b.set(6, true);
        b.set(7, false);

        System.out.println(b.toByteArray()[0]);

    }
}
