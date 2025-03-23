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
import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;

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

        final int command = buffer.get(1);
        final int length = buffer.get(3);
        final byte[] data = new byte[length];
        buffer.get(data, 4, length);

        // calculate checksum
        int sum = 0x0000;
        sum -= (byte) command;

        for (final byte element : data) {
            sum -= element;
        }

        // compare the checksum
        return buffer.get(length + 4) == (byte) (sum >> 8 & 0xFF) && buffer.get(length + 5) == (byte) (sum & 0xFF);
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
    protected void collectData(final Port port) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
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
                    final int length = receiveBuffer.get(3);
                    receiveBuffer.position(3);
                    receiveBuffer.limit(3 + length + 4);
                    final ByteBuffer data = receiveBuffer.slice();

                    switch (receiveBuffer.get(1)) {
                        case 0x03: {
                            readStatus(pack, data);
                            done = true;
                        }
                        break;
                        case 0x04: {
                            done = true;
                        }
                        break;
                        case 0x05: {
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
        // current (10mAh)
        pack.packCurrent = data.getShort() / 10;
        // remaining capacity (10mAh)
        pack.remainingCapacitymAh = data.getShort() * 10;
        // nominal capacity (10mAh)
        pack.ratedCapacitymAh = data.getShort() * 10;
        // bms cycles
        pack.bmsCycles = data.getShort();
        // production date (not mapped)
        data.getShort();
        // balancing states
        final int balanceStates = data.getInt();

        for (int idx = 0; idx < 32; idx++) {
            pack.cellBalanceState[idx] = BitUtil.bit(balanceStates, idx);
        }

        // protection status
        readAlarms(pack, data.getShort());
        // software version
        final byte sw = data.get();
        pack.softwareVersion = String.format("%02X", sw >> 4) + "." + String.format("%02X", sw & 0x0F);

        // remaining SOC (1%)
        pack.packSOC = data.get() * 10;

        final byte mosState = data.get();
        pack.chargeMOSState = BitUtil.bit(mosState, 0);
        pack.dischargeMOSState = BitUtil.bit(mosState, 1);

        // number of battery strings
        data.get();

        // number of temperature sensors
        pack.numOfTempSensors = data.get();

        // temperature
        pack.tempAverage = 0;
        for (int i = 0; i < pack.numOfTempSensors; i++) {
            pack.tempAverage += data.getShort() - 2731;
        }
        pack.tempAverage = pack.tempAverage / pack.numOfTempSensors;
    }


    private void readAlarms(final BatteryPack pack, final short short1) {
        // TODO Auto-generated method stub

    }


    List<DataEntry> readFrame(final Port port) throws IOException {
        final JSerialCommPort serialPort = (JSerialCommPort) port;
        byte[] buffer = new byte[1];

        // try to read the start flag
        if (serialPort.readBytes(buffer, 200) == -1) {
            // no bytes available
            return null;
        }

        // check for correct start flag for response
        if (buffer[0] != (byte) 0x01) {
            throw new IOException("Error reading data - got wrong start flag!");
        }

        boolean endFlagFound = false;
        final List<DataEntry> dataEntries = new ArrayList<>();

        do {
            final byte[] dataId = new byte[1];

            // check if bytes are available
            if (serialPort.readBytes(dataId, 200) != -1) {
                final JBDRS485DataId dataIdType = JBDRS485DataId.fromDataId(dataId[0]);

                if (dataIdType != null) {
                    final DataEntry dataEntry = new DataEntry();
                    dataEntry.setId(dataIdType);

                    // get the length of the data segment
                    int length = dataIdType.getLength();

                    // special handling for cell voltages and end flag
                    if (dataIdType.equals(JBDRS485DataId.READ_CELL_VOLTAGES)) {
                        // the first data byte declares the number bytes for all cells
                        buffer = new byte[1];
                        serialPort.readBytes(buffer, 200);
                        length = buffer[0];
                    } else if (dataIdType.equals(JBDRS485DataId.END_FLAG)) {
                        endFlagFound = true;
                    }

                    // do not add the endflag as entry
                    if (!endFlagFound) {
                        // copy the relevant data bytes and set them for this entry
                        final byte[] datacopy = new byte[length];
                        serialPort.readBytes(datacopy, 200);
                        dataEntry.setData(ByteBuffer.wrap(datacopy));
                        dataEntries.add(dataEntry);
                    }
                }
            } else {
                return null;
            }
        } while (!endFlagFound);

        return dataEntries;
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


    public static void main(final String[] args) {
        final JBDBmsRS485Processor p = new JBDBmsRS485Processor();
        final ByteBuffer sendFrame = p.prepareSendFrame(0x03, 0, new byte[] {});
        System.out.println(Port.printBuffer(sendFrame));
    }
}
