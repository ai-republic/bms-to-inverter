package com.airepublic.bmstoinverter.bms.jk.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.Util;

/**
 * The class to handle RS485 messages from a JK {@link BMS}.
 */
public class JKBmsRS485Processor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(JKBmsRS485Processor.class);
    private final static int BATTERY_ID = 0;

    @Override
    protected void collectData(final Port port) {
        final ByteBuffer sendFrame = prepareSendFrame((byte) 0x85); // SOC

        try {
            port.sendFrame(sendFrame);

            try {
                final ByteBuffer frame = port.receiveFrame();
                final BatteryPack pack = getBatteryPack(BATTERY_ID);
                final int dataLength = frame.getShort(2) - 1; // -1 because of command id byte is
                                                              // first data byte
                final int commandId = frame.get(11);
                final byte[] bytes = new byte[dataLength];
                frame.position(12);
                frame.get(bytes);
                final ByteBuffer data = ByteBuffer.wrap(bytes);

                switch (commandId) {
                    case 0x79:
                        readCellVoltages(pack, data);
                    break;
                    case 0x80:
                        readTubeTemperature(pack, data);
                    break;
                    case 0x81:
                        readBoxTemperature(pack, data);
                    break;
                    case 0x82:
                        readBatteryTemperature(pack, data);
                    break;
                    case 0x83:
                        readTotalVoltage(pack, data);
                    break;
                    case 0x84:
                        readTotalCurrent(pack, data);
                    break;
                    case 0x85:
                        readBatterySOC(pack, data);
                    break;
                    case 0x86:
                        readNumberOfTemperatureSensors(pack, data);
                    break;
                    case 0x87:
                        readCycleTimes(pack, data);
                    break;
                    case 0x89:
                        readTotalCapacity(pack, data);
                    break;
                    case 0x8A:
                        readNumberOfBatteryStrings(pack, data);
                    break;
                    case 0x8B:
                        readAlarms(pack, data);
                    break;
                    case 0x8C:
                        readBatteryStatus(pack, data);
                    break;
                    case 0x8E:
                        readBatteryOverVoltageLimit(pack, data);
                    break;
                    case 0x8F:
                        readBatteryUnderVoltageLimit(pack, data);
                    break;
                    case 0x90:
                        readCellOverVoltageLimit(pack, data);
                    break;
                    case 0x93:
                        readCellUnderVoltageLimit(pack, data);
                    break;
                    case 0x97:
                        readDischargeCurrentLimit(pack, data);
                    break;
                    case 0x99:
                        readChargeCurrentLimit(pack, data);
                    break;
                    case 0xAA:
                        readRatedCapacity(pack, data);
                    break;
                    case 0xAF:
                        readBatteryType(pack, data);
                    break;
                }

            } catch (final IOException e) {
                LOG.error("Error receiving frame!", e);
            }
        } catch (final IOException e) {
            LOG.error("Error sending frame: " + Port.printBuffer(sendFrame));
        }
    }


    ByteBuffer prepareSendFrame(final byte commandId) {
        final ByteBuffer sendFrame = ByteBuffer.allocate(21).order(ByteOrder.LITTLE_ENDIAN);
        sendFrame.put((byte) 0x4E); // start flag 2 bytes
        sendFrame.put((byte) 0x57);
        sendFrame.put((byte) 0x00); // frame length including this 2 bytes
        sendFrame.put((byte) 0x13);
        sendFrame.put((byte) 0x00); // terminal number 4 bytes
        sendFrame.put((byte) 0x00);
        sendFrame.put((byte) 0x00);
        sendFrame.put((byte) 0x00);
        sendFrame.put((byte) 0x03); // command id (0x01 - activation instruction, 0x02 - write
                                    // instruction, 0x03 - read identifier data, 0x05 - pair code,
                                    // 0x06 - read all data
        sendFrame.put((byte) 0x03); // frame source id (0x00 - BMS, 0x01- BT, 0x02-GPS, 0x03 - PC)
        sendFrame.put((byte) 0x00); // transport type (0x00 - request, 0x01 - response)
        sendFrame.put(commandId);
        sendFrame.putInt(0x00000000); // record number - 4 bytes (1st random, 2-4 recorde number)
        sendFrame.put((byte) 0x68); // end flag

        int crc = 0;

        for (int i = 2; i < sendFrame.capacity() - 4; i++) {
            crc += sendFrame.get(i);
        }
        sendFrame.putInt(crc); // CRC 4 byts

        return sendFrame;
    }


    // 0x79
    private void readCellVoltages(final BatteryPack pack, final ByteBuffer data) {
        // first byte is the length of cells
        pack.numberOfCells = data.get() / 3;

        // ensure that batterypack cell array is big enough
        if (pack.cellVmV.length < pack.numberOfCells) {
            final int[] swp = new int[pack.numberOfCells];
            System.arraycopy(pack.cellVmV, 0, swp, 0, pack.cellVmV.length);
            pack.cellVmV = swp;
        }

        for (int i = 0; i < pack.numberOfCells; i++) {
            pack.cellVmV[data.get()] = data.getChar();
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
