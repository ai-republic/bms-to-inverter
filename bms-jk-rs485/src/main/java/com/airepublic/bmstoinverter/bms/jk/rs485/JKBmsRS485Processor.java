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
package com.airepublic.bmstoinverter.bms.jk.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.NoDataAvailableException;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.BitUtil;
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
        int noDataReceived = 0;

        final ByteBuffer sendFrame = prepareSendFrame();

        try {
            port.sendFrame(sendFrame);

            try {
                boolean valid = true;

                // read frames until the requested frame is read
                do {
                    final List<DataEntry> dataEntries = readFrame(port);

                    if (dataEntries != null) {
                        final BatteryPack pack = getBatteryPack(BATTERY_ID);

                        for (final DataEntry dataEntry : dataEntries) {
                            final JKRS485DataId dataId = dataEntry.getId();

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
                                case READ_SOFTWARE_VERSION:
                                    readSoftwareVersion(pack, sendFrame);
                                break;
                                case READ_MANUFACTURER:
                                    readManufacturer(pack, dataEntry.getData());
                                break;
                                default:

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
                final JKRS485DataId dataIdType = JKRS485DataId.fromDataId(dataId[0]);

                if (dataIdType != null) {
                    final DataEntry dataEntry = new DataEntry();
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


    ByteBuffer prepareSendFrame() {
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
        sendFrame.put((byte) 0x00); // Read a single data reference (5.1 table);Read all data and
                                    // fill
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
        pack.minCellmV = Integer.MAX_VALUE;
        pack.maxCellmV = 0;

        // data is packed in 3 bytes per cell
        pack.numberOfCells = data.capacity() / 3;

        // ensure that batterypack cell array is big enough
        if (pack.cellVmV.length < pack.numberOfCells) {
            final int[] swp = new int[pack.numberOfCells];
            System.arraycopy(pack.cellVmV, 0, swp, 0, pack.cellVmV.length);
            pack.cellVmV = swp;
        }

        LOG.debug("Cell voltages\n");

        for (int i = 0; i < pack.numberOfCells; i++) {
            final int cellNo = data.get();
            pack.cellVmV[cellNo - 1] = data.getShort();

            if (pack.cellVmV[cellNo - 1] > pack.maxCellmV) {
                pack.maxCellVNum = cellNo - 1;
                pack.maxCellmV = pack.cellVmV[cellNo - 1];
            }

            if (pack.cellVmV[cellNo - 1] < pack.minCellmV) {
                pack.minCellVNum = cellNo - 1;
                pack.minCellmV = pack.cellVmV[cellNo - 1];
            }

            LOG.debug("\tCell #{}: {} mV\n", cellNo, pack.cellVmV[cellNo - 1]);
        }

        pack.cellDiffmV = pack.maxCellmV - pack.minCellmV;
        LOG.debug("\n\tCell low: {} mV\n\tCell high: {} mV\n\tCell-diff: {} mV", pack.minCellmV, pack.maxCellmV, pack.cellDiffmV);
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
        LOG.debug("Battery temperature: {} C", pack.tempAverage / 10f);
    }


    // 0x83
    private void readTotalVoltage(final BatteryPack pack, final ByteBuffer data) {
        pack.packVoltage = (int) (data.getChar() / 10f);
        LOG.debug("Pack voltage: {} V", pack.packVoltage / 10f);
    }


    // 0x84
    private void readTotalCurrent(final BatteryPack pack, final ByteBuffer data) {
        // total current (0.01A) offset 10000
        // pack.packCurrent = (int) ((10000 - data.getChar()) / 10f);

        // or C 0:0 x 01 redefines highest bit 0 = charge
        char value = data.getChar();
        final boolean charging = BitUtil.bit(value, 15);
        value &= 0x7FFF;

        pack.packCurrent = (int) (charging ? value / 10f : -(value / 10f));
        LOG.debug("Pack current: {} A", pack.packCurrent / 10f);
    }


    // 0x85
    private void readBatterySOC(final BatteryPack pack, final ByteBuffer data) {
        // Battery SOC (1%)
        pack.packSOC = data.get() * 10;
        LOG.debug("Battery SOC: {} %", pack.packSOC / 10f);
    }


    // 0x86
    private void readNumberOfTemperatureSensors(final BatteryPack pack, final ByteBuffer data) {
        // Number of battery temperature sensors
        pack.numOfTempSensors = data.get();
        LOG.debug("No of temperature sensors: {}", pack.numOfTempSensors);
    }


    // 0x87
    private void readCycleTimes(final BatteryPack pack, final ByteBuffer data) {
        // Number of battery cycles
        pack.bmsCycles = data.getChar();
        LOG.debug("Battery cycles: {}", pack.bmsCycles);
    }


    // 0x89
    private void readTotalCapacity(final BatteryPack pack, final ByteBuffer data) {
        // Total capacity of battery cycles

        // not mapped
        LOG.debug("Total capacity of battery cyles: {} AH", data.getInt());
    }


    // 0x8A
    private void readNumberOfBatteryStrings(final BatteryPack pack, final ByteBuffer data) {
        // Total capacity of battery cycles
        pack.modulesInSeries = data.getChar();
        LOG.debug("Battery modules in series: {}", pack.modulesInSeries);
    }


    // 0x8B
    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.NONE);

        // alarms
        byte value = data.get();
        pack.setAlarm(Alarm.SOC_LOW, BitUtil.bit(value, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(value, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(value, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_VOLTAGE_HIGH, BitUtil.bit(value, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_VOLTAGE_LOW, BitUtil.bit(value, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_TEMPERATURE_HIGH, BitUtil.bit(value, 4) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(value, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(value, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, BitUtil.bit(value, 7) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        value = data.get();
        pack.setAlarm(Alarm.ENCASING_TEMPERATURE_HIGH, BitUtil.bit(value, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_TEMPERATURE_LOW, BitUtil.bit(value, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(value, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(value, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        if (pack.getAlarmLevel(Alarm.FAILURE_OTHER) != AlarmLevel.ALARM) {
            pack.setAlarm(Alarm.FAILURE_OTHER, BitUtil.bit(value, 4) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        }

        if (pack.getAlarmLevel(Alarm.FAILURE_OTHER) != AlarmLevel.ALARM) {
            pack.setAlarm(Alarm.FAILURE_OTHER, BitUtil.bit(value, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        }
    }


    // 0x8C
    private void readBatteryStatus(final BatteryPack pack, final ByteBuffer data) {
        final byte value = data.get(1);
        pack.chargeMOSState = BitUtil.bit(value, 0);
        pack.dischargeMOSState = BitUtil.bit(value, 1);
        pack.cellBalanceActive = BitUtil.bit(value, 2);
        LOG.debug("Battery status: \n\tCharge MOS={}\n\tDischarge MOS={}\n\tBalancing={}", pack.chargeMOSState ? "ON" : "OFF", pack.dischargeMOSState ? "ON" : "OFF", pack.cellBalanceActive ? "ON" : "OFF");
    }


    // 0x8E
    private void readBatteryOverVoltageLimit(final BatteryPack pack, final ByteBuffer data) {
        // maximum pack voltage (10mv)
        pack.maxPackVoltageLimit = data.getChar() / 10;
        LOG.debug("Battery max voltage limit: {} V", pack.maxPackVoltageLimit / 10f);
    }


    // 0x8F
    private void readBatteryUnderVoltageLimit(final BatteryPack pack, final ByteBuffer data) {
        // minimum pack voltage (10mv)
        pack.minPackVoltageLimit = data.getChar() / 10;
        LOG.debug("Battery min voltage limit: {} V", pack.minPackVoltageLimit / 10f);
    }


    // 0x90
    private void readCellOverVoltageLimit(final BatteryPack pack, final ByteBuffer data) {
        // maximum cell voltage (1mv)
        pack.maxCellVoltageLimit = data.getChar();
        LOG.debug("Cell max voltage limit: {} V", pack.maxCellVoltageLimit / 1000f);
    }


    // 0x93
    private void readCellUnderVoltageLimit(final BatteryPack pack, final ByteBuffer data) {
        // minimum cell voltage (1mv)
        pack.minCellVoltageLimit = data.getChar();
        LOG.debug("Cell min voltage limit: {} V", pack.minCellVoltageLimit / 1000f);
    }


    // 0x97
    private void readDischargeCurrentLimit(final BatteryPack pack, final ByteBuffer data) {
        pack.maxPackDischargeCurrent = data.getChar() * 10;
        LOG.debug("Cell max discharge limit: {} A", pack.maxPackDischargeCurrent / 10f);
    }


    // 0x99
    private void readChargeCurrentLimit(final BatteryPack pack, final ByteBuffer data) {
        pack.maxPackChargeCurrent = data.getChar() * 10;
        LOG.debug("Cell max charge limit: {} A", pack.maxPackChargeCurrent / 10f);
    }


    // 0xAA
    private void readRatedCapacity(final BatteryPack pack, final ByteBuffer data) {
        // rated capacity of battery (1A)
        pack.ratedCapacitymAh = data.getInt() * 1000;
        LOG.debug("Battery rated capacity: {} AH", pack.ratedCapacitymAh / 1000f);
    }


    // 0xAF
    private void readBatteryType(final BatteryPack pack, final ByteBuffer data) {
        // 0: lithium iron phosphate, 1: ternary, 2: lithium titanate
        pack.type = data.get();
        LOG.debug("Battery type: {}", pack.type == 0 ? "LiFePo" : pack.type == 1 ? "Ternary" : "Lithium titanate");
    }


    // 0xB7
    private void readSoftwareVersion(final BatteryPack pack, final ByteBuffer data) {
        pack.softwareVersion = new String(data.array());
        LOG.debug("BMS software version: {}", pack.softwareVersion);
    }


    // 0xBA
    private void readManufacturer(final BatteryPack pack, final ByteBuffer data) {
        pack.manufacturerCode = new String(data.array());
        LOG.debug("BMS manufacturer: {}", pack.softwareVersion);
    }


    public static void main(final String[] args) {
        final JKBmsRS485Processor p = new JKBmsRS485Processor();
        final BatteryPack pack = new BatteryPack();
        ByteBuffer data = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        data.putChar((char) 0x07D0).rewind();
        p.readTotalCurrent(pack, data);
        System.out.println(pack.packCurrent);

        data = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        data.putChar((char) 0x87D0).rewind();
        p.readTotalCurrent(pack, data);
        System.out.println(pack.packCurrent);
    }
}
