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
public class SacredsunBmsRS485Processor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(SacredsunBmsRS485Processor.class);

    @Override
    protected void collectData(final Port port) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
        sendMessage(port); // battery information
    }


    private List<ByteBuffer> sendMessage(final Port port) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
        // first convert the bmsId to ascii bytes
        final String bmsId = new String(ByteAsciiConverter.convertByteToAsciiBytes((byte) getBmsId()));
        // insert the ascii byte bmsId into the request bytes
        final String command = "~22" + bmsId + "4A42E00201FD" + String.format("%02X", 0x28 - (getBmsId() - 1)) + "\r";
        final ByteBuffer sendBuffer = ByteBuffer.wrap(command.getBytes()).order(ByteOrder.LITTLE_ENDIAN);
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
                        final byte[] lengthAscii = new byte[2];
                        receiveBuffer.position(11);
                        receiveBuffer.get(lengthAscii);
                        final short length = ByteAsciiConverter.convertAsciiBytesToShort(lengthAscii);

                        receiveBuffer.position(15);
                        receiveBuffer.limit(15 + length);
                        final ByteBuffer data = receiveBuffer.slice();

                        readBatteryInformation(pack, data);
                        done = true;
                    } else {
                        LOG.warn("Frame is not value: " + Port.printBuffer(receiveBuffer));
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


    // 0x61
    private void readBatteryInformation(final BatteryPack aggregatedPack, final ByteBuffer data) {
        final byte[] shortAsciiBuffer = new byte[4];
        data.get(shortAsciiBuffer);
        // SOC 0.01%
        aggregatedPack.packSOC = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) / 10;
        data.get(shortAsciiBuffer);
        // pack voltage 0.01V
        aggregatedPack.packVoltage = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) / 10;
        // number of cells
        aggregatedPack.numberOfCells = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        for (int cellNo = 0; cellNo < aggregatedPack.numberOfCells; cellNo++) {
            data.get(shortAsciiBuffer);
            aggregatedPack.cellVmV[cellNo] = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);
        }

        // Temperature (avg, max, min) 0.1C
        data.get(shortAsciiBuffer);
        aggregatedPack.tempAverage = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);
        data.get(shortAsciiBuffer);
        aggregatedPack.tempMax = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);
        data.get(shortAsciiBuffer);
        aggregatedPack.tempMin = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);

        // number of temperature sensors
        aggregatedPack.numOfTempSensors = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        // cell temperature 0.1C
        for (int tempSensor = 0; tempSensor < aggregatedPack.numOfTempSensors; tempSensor++) {
            data.get(shortAsciiBuffer);
            aggregatedPack.cellTemperature[tempSensor] = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);
        }

        // current 0.1A
        data.get(shortAsciiBuffer);
        aggregatedPack.packCurrent = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);

        data.getInt(); // ???

        // SOH %
        data.get(shortAsciiBuffer);
        aggregatedPack.packSOH = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get()) * 10;

        data.getShort(); // ???

        // nominal capacity 0.01A
        data.get(shortAsciiBuffer);
        aggregatedPack.ratedCapacitymAh = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) * 10;

        // remaining capacity 0.01A
        data.get(shortAsciiBuffer);
        aggregatedPack.remainingCapacitymAh = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) * 10;

        // bms cycles
        data.get(shortAsciiBuffer);
        aggregatedPack.bmsCycles = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);
    }


    public static void main(final String[] args) {
        System.out.println((byte) '~');
    }
}
