package com.airepublic.bmstoinverter.bms.jk.rs485;

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
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.Util;
import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;

/**
 * The class to handle RS485 messages from a JK {@link BMS}.
 */
public class JKBmsRS485Processor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(JKBmsRS485Processor.class);
    private final static int BATTERY_ID = 0;

    public static class DataEntry {
        private JKRS485DataId id;
        private ByteBuffer data;

        /**
         * @return the id
         */
        public JKRS485DataId getId() {
            return id;
        }


        /**
         * @param id the id to set
         */
        public void setId(final JKRS485DataId id) {
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
    protected void collectData(final Port port) {
        final byte[] cmdIds = new byte[] { (byte) 0x00, (byte) 0x79, (byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87, (byte) 0x89, (byte) 0x8A, (byte) 0x8B, (byte) 0x8C, (byte) 0x8E, (byte) 0x8F, (byte) 0x90, (byte) 0x93, (byte) 0x97, (byte) 0xAA, (byte) 0xAF };
        int noDataReceived = 0;

        for (final byte cmdId : cmdIds) {
            final ByteBuffer sendFrame = prepareSendFrame(cmdId);

            try {
                port.sendFrame(sendFrame);

                try {
                    boolean valid = true;

                    // read frames until the requested frame is read
                    do {
                        final List<DataEntry> dataEntries = readFrame(port);

                        if (dataEntries != null) {
                            final BatteryPack pack = getBatteryPack(BATTERY_ID);

                            for (final var dataEntry : dataEntries) {
                                final var dataId = dataEntry.getId();

                                switch (dataId) {
                                    case READ_CELL_VOLTAGES:
                                        readCellVoltages(pack, dataEntry.getData());
                                    break;
                                    case READ_TUBE_TEMPERATURE:
                                        readTubeTemperature(pack, dataEntry.getData());
                                    break;
                                    case READ_BOX_TEMPERATURE:
                                        readBoxTemperature(pack, dataEntry.getData());
                                    break;
                                    case READ_BATTERY_TEMPERATURE:
                                        readBatteryTemperature(pack, dataEntry.getData());
                                    break;
                                    case READ_TOTAL_VOLTAGE:
                                        readTotalVoltage(pack, dataEntry.getData());
                                    break;
                                    case READ_TOTAL_CURRENT:
                                        readTotalCurrent(pack, dataEntry.getData());
                                    break;
                                    case READ_BATTERY_SOC:
                                        readBatterySOC(pack, dataEntry.getData());
                                    break;
                                    case READ_NUMBER_OF_TEMPERATURE_SENSORS:
                                        readNumberOfTemperatureSensors(pack, dataEntry.getData());
                                    break;
                                    case READ_CYCLE_TIMES:
                                        readCycleTimes(pack, dataEntry.getData());
                                    break;
                                    case READ_TOTAL_CAPACITY:
                                        readTotalCapacity(pack, dataEntry.getData());
                                    break;
                                    case READ_NUMBER_OF_BATTERY_STRINGS:
                                        readNumberOfBatteryStrings(pack, dataEntry.getData());
                                    break;
                                    case READ_ALARMS:
                                        readAlarms(pack, dataEntry.getData());
                                    break;
                                    case READ_BATTERY_STATUS:
                                        readBatteryStatus(pack, dataEntry.getData());
                                    break;
                                    case READ_BATTERY_OVER_VOLTAGE_LIMIT:
                                        readBatteryOverVoltageLimit(pack, dataEntry.getData());
                                    break;
                                    case READ_BATTERY_UNDER_VOLTAGE_LIMIT:
                                        readBatteryUnderVoltageLimit(pack, dataEntry.getData());
                                    break;
                                    case READ_CELL_OVER_VOLTAGE_LIMIT:
                                        readCellOverVoltageLimit(pack, dataEntry.getData());
                                    break;
                                    case READ_CELL_UNDER_VOLTAGE_LIMIT:
                                        readCellUnderVoltageLimit(pack, dataEntry.getData());
                                    break;
                                    case READ_DISCHARGE_CURRENT_LIMIT:
                                        readDischargeCurrentLimit(pack, dataEntry.getData());
                                    break;
                                    case READ_CHARGE_CURRENT_LIMIT:
                                        readChargeCurrentLimit(pack, dataEntry.getData());
                                    break;
                                    case READ_RATED_CAPACITY:
                                        readRatedCapacity(pack, dataEntry.getData());
                                    break;
                                    case READ_BATTERY_TYPE:
                                        readBatteryType(pack, dataEntry.getData());
                                    break;
                                    default:
                                        LOG.error("command not recognized...", dataId);
                                    break;
                                }

                            }
                        } else { // received nothing
                            // keep track of how often no bytes could be read
                            noDataReceived++;
                            LOG.debug("No bytes received: " + noDataReceived + " times!");

                            // if we received no bytes more than 10 times we stop and notify the
                            // handler to re-open the port
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
                    } while (!valid);
                } catch (final IOException e) {
                    LOG.error("Error receiving frame!", e);
                }
            } catch (final Exception e) {
                LOG.error("Error sending frame: " + Port.printBuffer(sendFrame));
            }
        }
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
                final var dataIdType = JKRS485DataId.fromDataId(dataId[0]);

                if (dataIdType != null) {
                    final var dataEntry = new DataEntry();
                    dataEntry.setId(dataIdType);

                    // get the length of the data segment
                    int length = dataIdType.getLength();

                    // special handling for cell voltages and end flag
                    if (dataIdType.equals(JKRS485DataId.READ_CELL_VOLTAGES)) {
                        // the first data byte declares the number bytes for all cells
                        buffer = new byte[1];
                        serialPort.readBytes(buffer, 200);
                        length = buffer[0];
                    } else if (dataIdType.equals(JKRS485DataId.END_FLAG)) {
                        endFlagFound = true;
                    }

                    // do not add the endflag as entry
                    if (!endFlagFound) {
                        // copy the relevant data bytes and set them for this entry
                        final var datacopy = new byte[length];
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


    ByteBuffer prepareSendFrame(final byte commandId) {
        final ByteBuffer sendFrame = ByteBuffer.allocate(21).order(ByteOrder.LITTLE_ENDIAN);
        sendFrame.put((byte) 0x4E); // Start flag
        sendFrame.put((byte) 0x57); // Additional start flag or part of a combined flag
        sendFrame.put((byte) 0x00); // Frame Length Byte 1
        sendFrame.put((byte) 0x13); // Frame Length Byte 2
        sendFrame.put((byte) 0x00); // Terminal Number Byte 1
        sendFrame.put((byte) 0x00); // Terminal Number Byte 2
        sendFrame.put((byte) 0x00); // Terminal Number Byte 3
        sendFrame.put((byte) 0x00); // Terminal Number Byte 4
        sendFrame.put((byte) 0x06); // command id (0x01 - activation instruction, 0x02 - write
                                    // instruction, 0x03 - read identifier data, 0x05 - pair code
                                    // 0x06, - read all data
        sendFrame.put((byte) 0x03); // frame source id (0x00 - BMS, 0x01- BT, 0x02-GPS, 0x03 - PC)
        sendFrame.put((byte) 0x00); // 0.Read data, 1.Answer frame 2.Data box active upload
        sendFrame.put(commandId); // Read a single data reference (5.1 table);Read all data and fill
                                  // in 0x00
        sendFrame.putInt(0x00000000); // record number - 4 bytes (1st random, 2-4 recorde number)
        sendFrame.put((byte) 0x68); // End Identity

        int sum = 0;
        for (int i = 0; i < sendFrame.array().length; i++) {
            sum += sendFrame.array()[i] & 0xFF; // Ensure the byte is treated as unsigned
        }
        sendFrame.put((byte) 0x00); // Checksum Byte 1
        sendFrame.put((byte) 0x00); // Checksum Byte 2
        sendFrame.put((byte) (sum >> 8 & 0xFF)); // Checksum Byte 3
        sendFrame.put((byte) (sum & 0xFF)); // Checksum Byte 4

        return sendFrame;
    }


    // 0x79
    private void readCellVoltages(final BatteryPack pack, final ByteBuffer data) {
        // data is packed in 3 bytes per cell
        pack.numberOfCells = data.capacity() / 3;

        // ensure that batterypack cell array is big enough
        if (pack.cellVmV.length < pack.numberOfCells) {
            final int[] swp = new int[pack.numberOfCells];
            System.arraycopy(pack.cellVmV, 0, swp, 0, pack.cellVmV.length);
            pack.cellVmV = swp;
        }

        int value = 0;

        for (int i = 0; i < pack.numberOfCells; i++) {
            value = data.get() << 16;
            value &= data.get() << 8;
            value &= data.get();

            pack.cellVmV[i] = value;
        }
    }


    // 0x80
    private void readTubeTemperature(final BatteryPack pack, final ByteBuffer data) {

    }


    // 0x81
    private void readBoxTemperature(final BatteryPack pack, final ByteBuffer data) {

    }


    // 0x82
    private void readBatteryTemperature(final BatteryPack pack, final ByteBuffer data) {
        final int temp = data.getChar();
        pack.tempAverage = temp > 100 ? -(temp - 100) * 10 : temp * 10;
    }


    // 0x83
    private void readTotalVoltage(final BatteryPack pack, final ByteBuffer data) {
        pack.packVoltage = (int) (data.getChar() / 10f);
    }


    // 0x84
    private void readTotalCurrent(final BatteryPack pack, final ByteBuffer data) {
        // total current (0.01A) offset 10000
        // pack.packCurrent = (int) ((10000 - data.getChar()) / 10f);

        // or C 0:0 x 01 redefines highest bit 0 = charge
        char value = data.getChar();
        final boolean charging = Util.bit(value, 15);
        value &= 0x7FFF;

        pack.packCurrent = (int) (charging ? value / 10f : -(value / 10f));
    }


    // 0x85
    private void readBatterySOC(final BatteryPack pack, final ByteBuffer data) {
        // Battery SOC (1%)
        pack.packSOC = data.get() * 10;
    }


    // 0x86
    private void readNumberOfTemperatureSensors(final BatteryPack pack, final ByteBuffer data) {
        // Number of battery temperature sensors
        pack.numOfTempSensors = data.get();
    }


    // 0x87
    private void readCycleTimes(final BatteryPack pack, final ByteBuffer data) {
        // Number of battery cycles
        pack.bmsCycles = data.getChar();
    }


    // 0x89
    private void readTotalCapacity(final BatteryPack pack, final ByteBuffer data) {
        // Total capacity of battery cycles
        pack.ratedCapacitymAh = data.getInt();
    }


    // 0x8A
    private void readNumberOfBatteryStrings(final BatteryPack pack, final ByteBuffer data) {
        // Total capacity of battery cycles
        pack.modulesInSeries = data.getChar();
    }


    // 0x8B
    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        // alarms
        byte value = data.get();
        pack.alarms.levelOneStateOfChargeTooLow.value = Util.bit(value, 0);
        pack.alarms.chargeFETTemperatureTooHigh.value = Util.bit(value, 1);
        pack.alarms.levelOnePackVoltageTooHigh.value = Util.bit(value, 2);
        pack.alarms.levelOnePackVoltageTooLow.value = Util.bit(value, 3);
        pack.alarms.levelOneChargeTempTooHigh.value = Util.bit(value, 4);
        pack.alarms.levelOneChargeCurrentTooHigh.value = Util.bit(value, 5);
        pack.alarms.levelOneDischargeCurrentTooHigh.value = Util.bit(value, 6);
        pack.alarms.levelOneCellVoltageDifferenceTooHigh.value = Util.bit(value, 7);

        value = data.get();
        pack.alarms.levelOneChargeTempTooHigh.value = Util.bit(value, 1);
        pack.alarms.levelOneChargeTempTooLow.value = Util.bit(value, 2);
        pack.alarms.levelOneCellVoltageTooHigh.value = Util.bit(value, 3);
        pack.alarms.levelOneCellVoltageTooLow.value = Util.bit(value, 4);
    }


    // 0x8C
    private void readBatteryStatus(final BatteryPack pack, final ByteBuffer data) {
        final byte value = data.get();
        pack.chargeMOSState = Util.bit(value, 0);
        pack.dischargeMOSState = Util.bit(value, 1);
        pack.cellBalanceActive = Util.bit(value, 2);
    }


    // 0x8E
    private void readBatteryOverVoltageLimit(final BatteryPack pack, final ByteBuffer data) {
        pack.maxPackVoltageLimit = data.getChar() / 10;
    }


    // 0x8F
    private void readBatteryUnderVoltageLimit(final BatteryPack pack, final ByteBuffer data) {
        pack.minPackVoltageLimit = data.getChar() / 10;
    }


    // 0x90
    private void readCellOverVoltageLimit(final BatteryPack pack, final ByteBuffer data) {
        pack.maxCellVoltageLimit = data.getChar() / 10;
    }


    // 0x93
    private void readCellUnderVoltageLimit(final BatteryPack pack, final ByteBuffer data) {
        pack.minCellVoltageLimit = data.getChar() / 10;
    }


    // 0x97
    private void readDischargeCurrentLimit(final BatteryPack pack, final ByteBuffer data) {
        pack.maxPackDischargeCurrent = data.getChar() * 10;
    }


    // 0x99
    private void readChargeCurrentLimit(final BatteryPack pack, final ByteBuffer data) {
        pack.maxPackChargeCurrent = data.getChar() * 10;
    }


    // 0xAA
    private void readRatedCapacity(final BatteryPack pack, final ByteBuffer data) {
        // rated capacity of battery (1A)
        pack.ratedCapacitymAh = data.getInt() * 1000;
    }


    // 0xAF
    private void readBatteryType(final BatteryPack pack, final ByteBuffer data) {
        // 0: lithium iron phosphate, 1: ternary, 2: lithium titanate
        pack.type = data.get();
    }


    public static void main(final String[] args) {
        final JKBmsRS485Processor p = new JKBmsRS485Processor();
        final BatteryPack pack = new BatteryPack();
        ByteBuffer data = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putChar((char) 0x07D0).rewind();
        p.readTotalCurrent(pack, data);
        System.out.println(pack.packCurrent);

        data = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putChar((char) 0x87D0).rewind();
        p.readTotalCurrent(pack, data);
        System.out.println(pack.packCurrent);
    }
}
