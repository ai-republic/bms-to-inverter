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
package com.airepublic.bmstoinverter.bms.sacredsun.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.NoDataAvailableException;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.TooManyInvalidFramesException;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.BitUtil;
import com.airepublic.bmstoinverter.core.util.ByteAsciiConverter;

/**
 * The class to handle RS485 messages from a SacredSun (TIAN) BMS.
 */
public class SacredsunBmsRS485Processor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(SacredsunBmsRS485Processor.class);
    private final Predicate<ByteBuffer> validator = buffer -> {
        // check if null
        if (buffer == null) {
            return false;
        }

        final byte[] checksumBytes = new byte[4];
        buffer.get(buffer.capacity() - 5, checksumBytes);
        System.out.println(Port.printBytes(checksumBytes));
        byte high = ByteAsciiConverter.convertAsciiBytesToByte(checksumBytes[0], checksumBytes[1]);
        byte low = ByteAsciiConverter.convertAsciiBytesToByte(checksumBytes[2], checksumBytes[3]);
        int checksum = high;
        checksum = checksum << 8 & low;

        final byte[] checkBytes = createChecksum(buffer);
        System.out.println(Port.printBytes(checkBytes));
        high = ByteAsciiConverter.convertAsciiBytesToByte(checkBytes[0], checkBytes[1]);
        low = ByteAsciiConverter.convertAsciiBytesToByte(checkBytes[2], checkBytes[3]);
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
        final String bmsId = new String(ByteAsciiConverter.convertByteToAsciiBytes((byte) getBmsId()));
        final ByteBuffer sendBuffer = ByteBuffer.wrap(("~22" + bmsId + "4A42E00201FD28\r").getBytes()).order(ByteOrder.LITTLE_ENDIAN);
        final List<ByteBuffer> readBuffers = new ArrayList<>();
        int failureCount = 0;
        int noDataReceived = 0;
        boolean done = false;
        final BatteryPack pack = getBatteryPack(getBmsId());

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
                    receiveBuffer.get(3, addressAscii);
                    final byte address = ByteAsciiConverter.convertAsciiBytesToByte(addressAscii[0], addressAscii[1]);

                    // extract length
                    final byte[] lengthAscii = new byte[2];
                    receiveBuffer.get(10, lengthAscii);
                    final short length = (short) (ByteAsciiConverter.convertAsciiBytesToShort(lengthAscii) & 0x0FFF);

                    final ByteBuffer data = receiveBuffer.slice(13, receiveBuffer.capacity() - 18);

                    readBatteryInformation(pack, data);
                    done = true;
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

        LOG.warn("Command {} to sent to BMS successfully and received!", HexFormat.of().withPrefix("0x").formatHex(new byte[] { cid1, cid2 }));

        return readBuffers;
    }


    // 0x61
    private void readBatteryInformation(final BatteryPack aggregatedPack, final ByteBuffer data) {
        final byte[] shortAsciiBuffer = new byte[4];
        data.get(shortAsciiBuffer);
        aggregatedPack.packVoltage = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) / 100;
        data.get(shortAsciiBuffer);
        aggregatedPack.packCurrent = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) / 10;
        aggregatedPack.packSOC = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get()) * 10;
        data.get(shortAsciiBuffer);
        aggregatedPack.bmsCycles = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);
        data.get(shortAsciiBuffer); // maximum cycles
        aggregatedPack.packSOH = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get()) * 10;
        data.getShort(); // lowest SOH
        data.get(shortAsciiBuffer);
        aggregatedPack.maxCellmV = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);
        data.getShort(); // battery pack with highest voltage
        aggregatedPack.maxCellVNum = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        data.get(shortAsciiBuffer);
        aggregatedPack.minCellmV = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);
        data.getShort(); // battery pack with lowest voltage
        aggregatedPack.minCellVNum = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        data.get(shortAsciiBuffer);
        aggregatedPack.tempAverage = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) - 2731;

        data.get(shortAsciiBuffer);
        aggregatedPack.tempMax = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) - 2731;
        data.getShort(); // battery pack with lowest temp
        aggregatedPack.tempMaxCellNum = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        data.get(shortAsciiBuffer);
        aggregatedPack.tempMin = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) - 2731;
        data.getShort(); // battery pack with lowest temp
        aggregatedPack.tempMinCellNum = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
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
        System.out.println(new String(ByteAsciiConverter.convertByteToAsciiBytes((byte) 12)));
    }
}
