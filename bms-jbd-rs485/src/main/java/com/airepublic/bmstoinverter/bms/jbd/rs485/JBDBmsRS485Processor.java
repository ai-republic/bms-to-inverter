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
package com.airepublic.bmstoinverter.bms.jbd.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
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
import com.airepublic.bmstoinverter.core.util.HexUtil;

/**
 * The class to handle RS485 messages from a Daly BMS.
 */
public class JBDBmsRS485Processor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(JBDBmsRS485Processor.class);
    private final Predicate<ByteBuffer> validator = buffer -> {
        // check if null
        if (buffer == null) {
            return false;
        }

        buffer.rewind();
        final byte[] data = buffer.array();
        final short checksum = compute(data);

        // compare the checksum
        return data[data.length - 3] == (byte) (checksum >> 8 & 0x00FF) && data[data.length - 2] == (byte) (checksum & 0x00FF);
    };

    public static class DataEntry {
        private JBDRS485DataId id;
        private ByteBuffer data;

        /**
         * @return the id
         */
        public JBDRS485DataId getId() {
            return id;
        }


        /**
         * @param id the id to set
         */
        public void setId(final JBDRS485DataId id) {
            this.id = id;
        }


        /**
         * @return the data
         */
        public ByteBuffer getData() {
            return data;
        }


        /**
         * @param data the data to set
         */
        public void setData(final ByteBuffer data) {
            this.data = data;
        }

    }

    @Override
    public void collectData(final Port port) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
        sendMessage(port, 0x03);
        sendMessage(port, 0x04);
        sendMessage(port, 0x05);
    }


    private List<ByteBuffer> sendMessage(final Port port, final int command) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
        final ByteBuffer sendBuffer = prepareSendFrame(command, 0, new byte[] {});
        final List<ByteBuffer> readBuffers = new ArrayList<>();
        int failureCount = 0;
        int noDataReceived = 0;
        boolean done = false;
        final BatteryPack pack = getBatteryPack(0);

        // read frames until the requested frame is read
        do {

            // send the request command frame
            LOG.debug("SENDING: {}", Port.printBuffer(sendBuffer));
            port.sendFrame(sendBuffer);
            LOG.debug("SENT: {}", Port.printBuffer(sendBuffer));

            try {
                Thread.sleep(92);
            } catch (final InterruptedException e) {
            }

            // read the expected response frame(s)
            boolean valid = false;
            ByteBuffer receiveBuffer = null;

            try {
                receiveBuffer = port.receiveFrame();
                LOG.debug("RECEIVED: {}", Port.printBuffer(receiveBuffer));

                valid = validator.test(receiveBuffer);

                if (valid) {
                    receiveBuffer.rewind();
                    final int length = receiveBuffer.get(3);
                    receiveBuffer.position(4);
                    final byte[] dataBytes = new byte[length];
                    receiveBuffer.get(dataBytes);
                    final ByteBuffer data = ByteBuffer.wrap(dataBytes).order(ByteOrder.BIG_ENDIAN);

                    switch (receiveBuffer.get(1)) {
                        case 0x03: {
                            readStatus(pack, data);
                            done = true;
                        }
                        break;
                        case 0x04: {
                            readCellVoltages(pack, data);
                            done = true;
                        }
                        break;
                        case 0x05: {
                            readHardwareVersion(pack, data);
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

        LOG.warn("Command {} to sent to BMS successfully and received!", HexUtil.formatHex(new byte[] { (byte) command }));

        return readBuffers;
    }


    private void readStatus(final BatteryPack pack, final ByteBuffer data) {
        // total voltage (10mV)
        pack.packVoltage = data.getShort() / 10;
        LOG.debug("Pack voltage: {} V", pack.packVoltage / 10.0);
        // current (10mAh)
        pack.packCurrent = data.getShort() / 10;
        LOG.debug("Pack current: {} A", pack.packCurrent / 10.0);
        // remaining capacity (10mAh)
        pack.remainingCapacitymAh = data.getShort() * 10;
        LOG.debug("Remaining capacity: {} Ah", pack.remainingCapacitymAh / 1000.0);
        // nominal capacity (10mAh)
        pack.ratedCapacitymAh = data.getShort() * 10;
        LOG.debug("Rated capacity: {} Ah", pack.ratedCapacitymAh / 1000.0);
        // bms cycles
        pack.bmsCycles = data.getShort();
        LOG.debug("BMS Cycles: {}", pack.bmsCycles);
        // production date (not mapped)
        data.getShort();
        // balancing states
        final int balanceStates = data.getInt();

        for (int idx = 0; idx < 32; idx++) {
            pack.cellBalanceState[idx] = BitUtil.bit(balanceStates, idx);
            LOG.debug("Cell {} balance state: {}", idx + 1, pack.cellBalanceState[idx]);
        }

        // protection status
        readAlarms(pack, data.getShort());
        // software version
        final byte sw = data.get();
        pack.softwareVersion = String.format("%02X", sw >> 4) + "." + String.format("%02X", sw & 0x0F);
        LOG.debug("Software version: {}", pack.softwareVersion);

        // remaining SOC (1%)
        pack.packSOC = data.get() * 10;
        LOG.debug("Pack SOC: {} %", pack.packSOC / 10.0);

        final byte mosState = data.get();
        pack.chargeMOSState = BitUtil.bit(mosState, 0);
        LOG.debug("Charge MOS State: {}", pack.chargeMOSState);
        pack.dischargeMOSState = BitUtil.bit(mosState, 1);
        LOG.debug("Discharge MOS State: {}", pack.dischargeMOSState);

        // number of battery strings
        data.get();

        // number of temperature sensors
        pack.numOfTempSensors = data.get();
        LOG.debug("Number of temperature sensors: {}", pack.numOfTempSensors);

        if (pack.numOfTempSensors != 0) {
            // temperature
            pack.tempAverage = 0;

            for (int i = 0; i < pack.numOfTempSensors; i++) {
                pack.tempAverage += data.getShort() - 2731;
            }

            pack.tempAverage = pack.tempAverage / pack.numOfTempSensors;
            LOG.debug("Average temperature: {} °C", pack.tempAverage / 10.0);
        }
    }


    private void readCellVoltages(final BatteryPack pack, final ByteBuffer data) {
        pack.numberOfCells = data.capacity() / 2;

        for (int cellNo = 0; cellNo < pack.numberOfCells; cellNo++) {
            pack.cellVmV[cellNo] = data.getShort();
            LOG.debug("Cell {} voltage: {} V", cellNo + 1, pack.cellVmV[cellNo] / 1000.0);
        }
    }


    private void readAlarms(final BatteryPack pack, final short short1) {
        // TODO Auto-generated method stub

    }


    private void readHardwareVersion(final BatteryPack pack, final ByteBuffer data) {
        String hardwareVersion = "";

        for (int i = 0; i < data.capacity(); i++) {
            hardwareVersion += (char) data.get(); // ascii chars
        }

        pack.hardwareVersion = hardwareVersion;
        LOG.debug("Hardware version: {}", pack.hardwareVersion);
    }


    ByteBuffer prepareSendFrame(final int command, final int length, final byte[] data) {
        final ByteBuffer sendFrame = ByteBuffer.allocate(7 + length).order(ByteOrder.BIG_ENDIAN);
        sendFrame.put((byte) 0xDD); // Start flag
        sendFrame.put((byte) 0xA5); // sending flag
        sendFrame.put((byte) command); // command id
        sendFrame.put((byte) length); // Frame Length Byte
        sendFrame.put(data);

        // checksum
        int sum = 0x0000;
        sum -= (byte) command;

        for (final byte element : data) {
            sum -= element;
        }
        sendFrame.put((byte) (sum >> 8 & 0xFF)); // Checksum Byte 1
        sendFrame.put((byte) (sum & 0xFF)); // Checksum Byte 2

        sendFrame.put((byte) 0x77); // End flag

        return sendFrame;
    }


    public static short compute(final byte[] frame) {
        if (frame == null || frame.length < 5) {
            throw new IllegalArgumentException("Invalid frame or index range");
        }

        short sum = 0;
        for (int i = 2; i < frame.length - 3; i++) {
            sum += frame[i] & 0xFF;
        }

        sum &= 0xFFFF;
        final int checksum = ~sum + 1 & 0xFFFF;

        return (short) checksum;
    }


    public static void main(final String[] args) {
        final byte[] frame = new byte[] {
                (byte) 0xDD, (byte) 0x03, (byte) 0x00, (byte) 0x28, (byte) 0x15, (byte) 0x84, (byte) 0x03, (byte) 0xA4,
                (byte) 0xC8, (byte) 0xE3, (byte) 0x80, (byte) 0xE8, (byte) 0x00, (byte) 0x10, (byte) 0x31, (byte) 0x3D,
                (byte) 0xBA, (byte) 0xAA, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x54, (byte) 0x64,
                (byte) 0xCB, (byte) 0x10, (byte) 0x04, (byte) 0x0B, (byte) 0x49, (byte) 0x0B, (byte) 0x62, (byte) 0x0B,
                (byte) 0xFD, (byte) 0x0B, (byte) 0x57, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0xE8,
                (byte) 0xC8, (byte) 0xE3, (byte) 0x00, (byte) 0x00, (byte) 0xF4, (byte) 0xCE, (byte) 0x77
        };

        final short checksum = compute(frame);
        System.out.printf("Calculated checksum: 0x%02X%n", checksum & 0xFFFF);
    }

}
