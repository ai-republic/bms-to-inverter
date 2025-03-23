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
package com.airepublic.bmstoinverter.bms.pylon.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.NoDataAvailableException;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.TooManyInvalidFramesException;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.BitUtil;
import com.airepublic.bmstoinverter.core.util.HexUtil;

/**
 * The class to handle RS485 messages from a Pylon BMS.
 */
public class PylonBmsRS485Processor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(PylonBmsRS485Processor.class);
    private final Predicate<ByteBuffer> validator = buffer -> {
        // check if null
        if (buffer == null) {
            return false;
        }

        final byte[] checksumBytes = new byte[4];
        buffer.position(buffer.capacity() - 5);
        buffer.get(checksumBytes);
        System.out.println(Port.printBytes(checksumBytes));
        byte high = convertAsciiBytesToByte(checksumBytes[0], checksumBytes[1]);
        byte low = convertAsciiBytesToByte(checksumBytes[2], checksumBytes[3]);
        int checksum = high;
        checksum = checksum << 8 & low;

        final byte[] checkBytes = createChecksum(buffer);
        System.out.println(Port.printBytes(checkBytes));
        high = convertAsciiBytesToByte(checkBytes[0], checkBytes[1]);
        low = convertAsciiBytesToByte(checkBytes[2], checkBytes[3]);
        int check = high;
        check = check << 8 & low;

        return check == checksum;
    };

    @Override
    protected void collectData(final Port port) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
        sendMessage(port, (byte) 0x46, (byte) 0x60); // system information
        sendMessage(port, (byte) 0x46, (byte) 0x61); // battery information
        sendMessage(port, (byte) 0x46, (byte) 0x62); // alarm information
    }


    private List<ByteBuffer> sendMessage(final Port port, final byte cid1, final byte cid2) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
        final ByteBuffer sendBuffer = prepareSendFrame((byte) getBmsId(), cid1, cid2, new byte[] {});
        final List<ByteBuffer> readBuffers = new ArrayList<>();
        int failureCount = 0;
        int noDataReceived = 0;
        boolean done = false;
        final BatteryPack pack = getBatteryPack(0);

        // read frames until the requested frame is read
        do {

            // send the request command frame
            port.sendFrame(sendBuffer);
            LOG.debug("SEND: {}", Port.printBuffer(sendBuffer));

            try {
                Thread.sleep(92);
            } catch (final InterruptedException e) {
            }

            // read the expected response frame(s)
            boolean valid = false;
            ByteBuffer receiveBuffer = null;

            try {
                receiveBuffer = port.receiveFrame();

                valid = validator.test(receiveBuffer);

                if (valid) {
                    LOG.debug("RECEIVED: {}", Port.printBuffer(receiveBuffer));
                    receiveBuffer.rewind();

                    // extract address
                    final byte[] addressAscii = new byte[2];
                    receiveBuffer.position(3);
                    receiveBuffer.get(addressAscii);
                    final byte address = convertAsciiBytesToByte(addressAscii[0], addressAscii[1]);

                    // extract command
                    final byte[] cmdAscii = new byte[2];
                    receiveBuffer.position(7);
                    receiveBuffer.get(cmdAscii);
                    final byte cmd = convertAsciiBytesToByte(cmdAscii[0], cmdAscii[1]);

                    // extract length
                    final byte[] lengthAscii = new byte[4];
                    receiveBuffer.position(9);
                    receiveBuffer.get(lengthAscii);
                    final short length = (short) (convertAsciiBytesToShort(lengthAscii) & 0x0FFF);

                    receiveBuffer.position(13);
                    receiveBuffer.limit(13 + receiveBuffer.capacity() - 18);
                    final ByteBuffer data = receiveBuffer.slice();

                    switch (cmd) {
                        case 0x60: {
                            readSystemInfo(pack, data);
                            done = true;
                        }
                        break;
                        case 0x61: {
                            readBatteryInformation(pack, data);
                            done = true;
                        }
                        break;
                        case 0x62: {
                            readAlarms(pack, data);
                            done = true;
                        }
                        break;
                        default: {
                            LOG.warn("Message could not be interpreted " + Port.printBuffer(receiveBuffer));
                            valid = false;
                        }
                    }
                } else if (receiveBuffer == null) { // received nothing
                    // keep track of how often no bytes could be read
                    noDataReceived++;
                    LOG.debug("No bytes received: " + noDataReceived + " times!");

                    // if we received no bytes more than 10 times we stop and notify the handler
                    // to re-open the port
                    if (noDataReceived >= 10) {
                        throw new NoDataAvailableException();
                    }

                    // try and wait for the next message to arrive
                    try {
                        LOG.debug("Waiting for messages to arrive....");
                        Thread.sleep(getDelayAfterNoBytes());
                    } catch (final InterruptedException e) {
                    }

                    // try to receive the response again
                    valid = false;
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                valid = false;
            }

            if (!valid) { // we received an invalid frame
                // keep track of how often invalid frames were received
                failureCount++;
                LOG.debug("Invalid frame received! {}", Port.printBuffer(receiveBuffer));

                if (failureCount >= 10) {
                    // try and wait for the bus to get quiet
                    try {
                        LOG.debug("Waiting for bus to idle....");
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                    }

                    port.clearBuffers();

                    throw new TooManyInvalidFramesException();
                }
            }
        } while (!done);

        LOG.warn("Command {} to sent to BMS successfully and received!", HexUtil.formatHex(new byte[] { cid1, cid2 }));

        return readBuffers;
    }


    // 0x60
    private void readSystemInfo(final BatteryPack pack, final ByteBuffer data) {
        data.position(20); // skip battery name
        pack.manufacturerCode = convertAsciiBytesToString(data, 20);
        pack.softwareVersion = convertAsciiBytesToString(data, 2);
        final byte high = data.get();
        final byte low = data.get();
        pack.numberOfCells = convertAsciiBytesToByte(high, low);
    }


    // 0x61
    private void readBatteryInformation(final BatteryPack aggregatedPack, final ByteBuffer data) {
        final byte[] shortAsciiBuffer = new byte[4];
        data.get(shortAsciiBuffer);
        aggregatedPack.packVoltage = convertAsciiBytesToShort(shortAsciiBuffer) / 100;
        data.get(shortAsciiBuffer);
        aggregatedPack.packCurrent = convertAsciiBytesToShort(shortAsciiBuffer) / 10;
        aggregatedPack.packSOC = convertAsciiBytesToByte(data.get(), data.get()) * 10;
        data.get(shortAsciiBuffer);
        aggregatedPack.bmsCycles = convertAsciiBytesToShort(shortAsciiBuffer);
        data.get(shortAsciiBuffer); // maximum cycles
        aggregatedPack.packSOH = convertAsciiBytesToByte(data.get(), data.get()) * 10;
        data.getShort(); // lowest SOH
        data.get(shortAsciiBuffer);
        aggregatedPack.maxCellmV = convertAsciiBytesToShort(shortAsciiBuffer);
        data.getShort(); // battery pack with highest voltage
        aggregatedPack.maxCellVNum = convertAsciiBytesToByte(data.get(), data.get());
        data.get(shortAsciiBuffer);
        aggregatedPack.minCellmV = convertAsciiBytesToShort(shortAsciiBuffer);
        data.getShort(); // battery pack with lowest voltage
        aggregatedPack.minCellVNum = convertAsciiBytesToByte(data.get(), data.get());
        data.get(shortAsciiBuffer);
        aggregatedPack.tempAverage = convertAsciiBytesToShort(shortAsciiBuffer) - 2731;

        data.get(shortAsciiBuffer);
        aggregatedPack.tempMax = convertAsciiBytesToShort(shortAsciiBuffer) - 2731;
        data.getShort(); // battery pack with lowest temp
        aggregatedPack.tempMaxCellNum = convertAsciiBytesToByte(data.get(), data.get());
        data.get(shortAsciiBuffer);
        aggregatedPack.tempMin = convertAsciiBytesToShort(shortAsciiBuffer) - 2731;
        data.getShort(); // battery pack with lowest temp
        aggregatedPack.tempMinCellNum = convertAsciiBytesToByte(data.get(), data.get());
    }


    // 0x62
    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        // warning alarms 1
        byte bits = convertAsciiBytesToByte(data.get(), data.get());
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(bits, 7) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(bits, 6) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(bits, 5) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(bits, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_TEMPERATURE_HIGH, BitUtil.bit(bits, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_TEMPERATURE_LOW, BitUtil.bit(bits, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, BitUtil.bit(bits, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // warning alarms 2
        bits = convertAsciiBytesToByte(data.get(), data.get());
        pack.setAlarm(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH, BitUtil.bit(bits, 7) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(bits, 6) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(bits, 5) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_INTERNAL, BitUtil.bit(bits, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // protection alarms 1
        bits = convertAsciiBytesToByte(data.get(), data.get());
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(bits, 7) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(bits, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(bits, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(bits, 4) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_TEMPERATURE_HIGH, BitUtil.bit(bits, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_TEMPERATURE_LOW, BitUtil.bit(bits, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // protection alarms 2
        bits = convertAsciiBytesToByte(data.get(), data.get());
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(bits, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(bits, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_OTHER, BitUtil.bit(bits, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
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
        return HexUtil.fromHexDigits(ascii);
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


    private String convertAsciiBytesToString(final ByteBuffer data, final int noOfCharacters) {
        // create byte array (2 bytes per ascii char representation)
        final byte[] asciiBytes = new byte[noOfCharacters * 2];
        data.get(asciiBytes);

        int asciiIdx = 0;
        final StringBuffer buf = new StringBuffer();

        while (asciiIdx < asciiBytes.length + 1) {
            final byte high = asciiBytes[asciiIdx++];
            final byte low = asciiBytes[asciiIdx++];
            final char chr = (char) convertAsciiBytesToByte(high, low);

            if (chr != 0) {
                buf.append(chr);
            }
        }

        return buf.toString();
    }


    private byte[] convertShortToAsciiBytes(final short value) {
        final byte first = (byte) ((value & 0xFF00) >> 8);
        final byte second = (byte) (value & 0x00FF);
        final byte[] data = new byte[4];
        System.arraycopy(convertByteToAsciiBytes(first), 0, data, 0, 2);
        System.arraycopy(convertByteToAsciiBytes(second), 0, data, 2, 2);

        return data;
    }


    private short convertAsciiBytesToShort(final byte[] value) {
        final byte first = convertAsciiBytesToByte(value[0], value[1]);
        final byte second = convertAsciiBytesToByte(value[2], value[3]);
        short result = first;
        result = (short) (result << 8 & second);

        return result;
    }


    void printAscii(final String str) {
        final String[] valueStr = str.split(" ");
        int i = 0;

        while (i < valueStr.length) {
            final byte high = HexUtil.fromHexDigits(valueStr[i++]);
            final byte low = HexUtil.fromHexDigits(valueStr[i++]);
            System.out.print("" + (char) convertAsciiBytesToByte(high, low));
        }

        System.out.println();
    }


    public static void main(final String[] args) {
        final PylonBmsRS485Processor p = new PylonBmsRS485Processor();
        // final String str = "~1203400456ABCEFEFC71\r";
        // final byte[] bytes = new byte[str.length()];
        // for (int i = 0; i < str.length(); i++) {
        // bytes[i] = (byte) str.charAt(i);
        // }
        // final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        final ByteBuffer buffer = ByteBuffer.wrap(new byte[] { (byte) 0x7E, (byte) 0x32, (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x34, (byte) 0x36, (byte) 0x36, (byte) 0x30,
                (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x46, (byte) 0x44, (byte) 0x41, (byte) 0x42, (byte) 0x0D });

        System.out.println(p.validator.test(buffer));
    }
}
