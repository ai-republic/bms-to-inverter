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
package com.airepublic.bmstoinverter.bms.seplos.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.NoDataAvailableException;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.TooManyInvalidFramesException;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.ByteAsciiConverter;

/**
 * The class to handle RS485 messages from a SacredSun (TIAN) BMS.
 */
public class SeplosBmsRS485Processor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(SeplosBmsRS485Processor.class);

    @Override
    protected void collectData(final Port port) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
        sendMessage(port); // battery information
    }


    private List<ByteBuffer> sendMessage(final Port port) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
        // first convert the bmsId to ascii bytes
        final String bmsId = new String(ByteAsciiConverter.convertByteToAsciiBytes((byte) getBmsId()));
        // insert the ascii byte bmsId into the request bytes
        final String command = "20" + bmsId + "464200021";

        final String frame = "~" + command + createChecksum(command) + "\r";
        final ByteBuffer sendBuffer = ByteBuffer.wrap(frame.getBytes()).order(ByteOrder.LITTLE_ENDIAN);
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

                if (receiveBuffer != null) {
                    // check start and end flag
                    valid = receiveBuffer.get(0) == 0x7E && receiveBuffer.get(receiveBuffer.capacity() - 1) == 0x0D;

                    if (valid) {
                        LOG.debug("RECEIVED: {}", Port.printBuffer(receiveBuffer));
                        receiveBuffer.rewind();

                        // extract address
                        final byte[] addressAscii = new byte[2];
                        receiveBuffer.position(3);
                        receiveBuffer.get(addressAscii);
                        final byte address = ByteAsciiConverter.convertAsciiBytesToByte(addressAscii[0], addressAscii[1]);

                        // extract length
                        final byte[] lengthAscii = new byte[4];
                        receiveBuffer.position(9);
                        receiveBuffer.get(lengthAscii);
                        final short length = ByteAsciiConverter.convertAsciiBytesToShort(lengthAscii);

                        receiveBuffer.position(13);
                        receiveBuffer.limit(13 + length);
                        final ByteBuffer data = receiveBuffer.slice();

                        readBatteryInformation(pack, data);
                        done = true;
                    } else {
                        LOG.warn("Frame is not valid: " + Port.printBuffer(receiveBuffer));
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

        LOG.warn("Command {} to sent to BMS successfully and received!", command);

        return readBuffers;
    }


    private String createChecksum(final String command) {
        short sum = 0;

        for (final byte b : command.getBytes()) {
            sum += b;
        }

        short checksum = (short) (sum % 65536);
        checksum = (short) ~checksum;
        checksum++;

        return String.format("%04X", checksum);
    }


    // 0x61
    private void readBatteryInformation(final BatteryPack pack, final ByteBuffer data) {
        final byte[] shortAsciiBuffer = new byte[4];

        // read the first 4 bytes (data flag and command group)
        data.getInt();

        // cell quantity
        pack.numberOfCells = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        // cell voltages 1mV
        int cellNo = 0;

        for (; cellNo < pack.numberOfCells; cellNo++) {
            data.get(shortAsciiBuffer);
            pack.cellVmV[cellNo] = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);
        }

        while (cellNo < 16) {
            data.get(shortAsciiBuffer);
        }

        // temp sensor quantity
        pack.numOfTempSensors = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        // temperature sensors 0.1K
        for (int tempSensorNo = 0; tempSensorNo < 4; tempSensorNo++) {
            data.get(shortAsciiBuffer);
            pack.cellTemperature[tempSensorNo] = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) - 2731;
        }

        // ambient temperature 0.1C
        data.get(shortAsciiBuffer);
        pack.tempAverage = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get()) - 2731;

        // component temperature 0.1C
        data.get(shortAsciiBuffer);

        // charge and discharge current 0.01A
        data.get(shortAsciiBuffer);
        pack.maxPackChargeCurrent = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) / 10;
        pack.maxPackDischargeCurrent = pack.maxPackChargeCurrent;

        // pack voltage 0.01V
        data.get(shortAsciiBuffer);
        pack.packVoltage = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) / 10;

        // remaining capacity 0.01Ah
        data.get(shortAsciiBuffer);
        pack.remainingCapacitymAh = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) * 10;

        // customize info
        data.getShort();

        // battery capacity 0.01Ah
        data.get(shortAsciiBuffer);
        pack.moduleRatedCapacityAh = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) / 100;

        // SOC 1%
        data.get(shortAsciiBuffer);
        pack.packSOC = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);

        // rated capacity 0.01Ah
        data.get(shortAsciiBuffer);
        pack.ratedCapacitymAh = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) * 10;

        // cycle life
        data.get(shortAsciiBuffer);
        pack.bmsCycles = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);

        // SOH 0.1%
        data.get(shortAsciiBuffer);
        pack.packSOH = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get()) / 10;
    }


    public static void main(final String[] args) {
        final String bmsId = new String(ByteAsciiConverter.convertByteToAsciiBytes((byte) 1));
        // insert the ascii byte bmsId into the request bytes
        final String command = "1203400456ABCEFE";
        // final String command = new String(new byte[] { 0x32, 0x30, 0x30, 0x30, 0x34, 0x36, 0x34,
        // 0x32, 0x45, 0x30, 0x30, 0x32, 0x30, 0x31 }, Charset.forName("ASCII"));
        short sum = 0;

        for (final byte b : command.getBytes()) {
            sum += b;
            System.out.print(String.format("%02X ", b));
        }

        System.out.println(String.format("%04X ", sum));
        short checksum = (short) (sum % 65536);
        System.out.println(String.format("%04X ", checksum));
        System.out.println(Integer.toBinaryString(checksum));
        checksum = (short) ~checksum;
        System.out.println(Integer.toBinaryString(checksum));
        System.out.println(String.format("%04X ", checksum));
        checksum++;
        System.out.println(String.format("%04X ", checksum));
    }
}
