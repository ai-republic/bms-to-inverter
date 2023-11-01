package com.airepublic.bmstoinverter.jk.can;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Bms;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortProcessor;
import com.airepublic.bmstoinverter.core.Portname;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CAN;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import jakarta.inject.Inject;

/**
 * The {@link PortProcessor} to handle CAN messages from a JK BMS.
 */
@Bms
public class JKBmsCANProcessor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(JKBmsCANProcessor.class);
    @Inject
    @CAN
    @Portname("bms.portname")
    private CANPort port;
    @Inject
    private EnergyStorage energyStorage;

    @Override
    public Port getPort() {
        return port;
    }


    @Override
    public void process() {
        try {
            final ByteBuffer frame = port.receiveFrame(null);
            final int frameId = frame.getInt();
            final byte[] bytes = new byte[8];
            frame.get(bytes);
            final ByteBuffer data = ByteBuffer.wrap(bytes);
            final int bmsNo = frameId >> 8;

            switch (frameId) {
                case 0x2F4:
                    readBatteryStatus(bmsNo, data);
                break;
                case 0x4F4:
                    readCellVoltage(bmsNo, data);
                break;
                case 0x5F4:
                    readCellTemperature(bmsNo, data);
                break;
                case 0x7F4:
                    readAlarms(bmsNo, data);
                break;
            }
        } catch (final IOException e) {
            LOG.error("Error receiving frame!", e);
        }
    }


    private void readBatteryStatus(final int bmsNo, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Battery voltage (0.1V)
        energyStorage.getBatteryPack(bmsNo).packVoltage = data.getShort();
        // Battery current (0.1A) offset 4000
        energyStorage.getBatteryPack(bmsNo).packCurrent = data.getShort() - 4000;
        // Battery SOC (1%)
        energyStorage.getBatteryPack(bmsNo).packSOC = data.get();
        // skip 1 byte
        data.get();
        // discharge time, e.g. 100h (not mapped)
        data.getShort();
    }


    private void readCellVoltage(final int bmsNo, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell voltage (1mV)
        energyStorage.getBatteryPack(bmsNo).maxCellmV = data.getShort();
        // Maximum cell voltage cell number
        energyStorage.getBatteryPack(bmsNo).maxCellVNum = data.get();
        // Minimum cell voltage (1mV)
        energyStorage.getBatteryPack(bmsNo).minCellmV = data.getShort();
        // Minimum cell voltage cell number
        energyStorage.getBatteryPack(bmsNo).minCellVNum = data.get();
    }


    private void readCellTemperature(final int bmsNo, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell temperature (C) offset -50
        energyStorage.getBatteryPack(bmsNo).tempMax = data.get();
        // Maximum cell temperature cell number
        data.get();
        // Minimum cell temperature (C) offset -50
        energyStorage.getBatteryPack(bmsNo).tempMin = data.get();
        // Minimum cell temperature cell number
        data.get();
        // Average cell temperature (C) offset -50
        energyStorage.getBatteryPack(bmsNo).tempAverage = data.get();
    }


    private void readAlarms(final int bmsNo, final ByteBuffer data) {
        // read first 8 bits
        byte value = data.get();

        // unit overvoltage
        int bits = read2Bits(value, 0);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneCellVoltageTooHigh.value = bits == 1;
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoCellVoltageTooHigh.value = bits >= 2;

        // unit undervoltage
        bits = read2Bits(value, 2);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneCellVoltageTooLow.value = bits == 1;
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoCellVoltageTooLow.value = bits >= 2;

        // total voltage overvoltage
        bits = read2Bits(value, 4);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOnePackVoltageTooHigh.value = bits == 1;
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoPackVoltageTooHigh.value = bits >= 2;

        // total voltage undervoltage
        bits = read2Bits(value, 6);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOnePackVoltageTooLow.value = bits == 1;
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoPackVoltageTooLow.value = bits >= 2;

        // read next 8 bits
        value = data.get();

        // Large pressure difference in cell (not mapped)
        bits = read2Bits(value, 0);

        // discharge overcurrent
        bits = read2Bits(value, 2);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneDischargeCurrentTooHigh.value = bits == 1;
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoDischargeCurrentTooHigh.value = bits >= 2;

        // charge overcurrent
        bits = read2Bits(value, 4);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneChargeCurrentTooHigh.value = bits == 1;
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoChargeCurrentTooHigh.value = bits >= 2;

        // temperature too high
        bits = read2Bits(value, 6);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneChargeTempTooHigh.value = bits == 1;
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoChargeTempTooHigh.value = bits >= 2;

        // read next 8 bits
        value = data.get();

        // temperature too low
        bits = read2Bits(value, 0);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneChargeTempTooLow.value = bits == 1;
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoChargeTempTooLow.value = bits >= 2;

        // excessive temperature difference
        bits = read2Bits(value, 2);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneTempSensorDifferenceTooHigh.value = bits == 1;
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoTempSensorDifferenceTooHigh.value = bits >= 2;

        // SOC too low
        bits = read2Bits(value, 4);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneStateOfChargeTooLow.value = bits == 1;
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoStateOfChargeTooLow.value = bits >= 2;

        // insulation too low (not mapped)
        bits = read2Bits(value, 6);

        // read next 8 bits
        value = data.get();

        // high voltage interlock fault
        bits = read2Bits(value, 0);
        energyStorage.getBatteryPack(bmsNo).alarms.failureOfVoltageSensorModule.value = bits != 0;

        // external communication failure
        bits = read2Bits(value, 2);
        energyStorage.getBatteryPack(bmsNo).alarms.failureOfVehicleCommunicationModule.value = bits != 0;

        // internal communication failure
        bits = read2Bits(value, 4);
        energyStorage.getBatteryPack(bmsNo).alarms.failureOfIntranetCommunicationModule.value = bits != 0;

    }


    private static int read2Bits(final byte value, final int index) {
        String str = Integer.toBinaryString(value);
        System.out.println("Str to parse: " + str);

        // remove leading bits
        if (str.length() > 8) {
            str = str.substring(str.length() - 8);
        }

        // pad leading 0's
        while (str.length() < 8) {
            str = "0" + str;
        }

        System.out.println("Padded str: " + str);
        final String bits = str.substring(index, 2);
        System.out.println("Read 2bits: " + bits);

        switch (bits) {
            case "00":
                return 0;
            case "01":
                return 1;
            case "10":
                return 2;
            case "11":
                return 3;
        }

        return 0;
    }

}
