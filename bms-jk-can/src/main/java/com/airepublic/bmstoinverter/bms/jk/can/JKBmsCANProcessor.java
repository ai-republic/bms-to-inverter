package com.airepublic.bmstoinverter.bms.jk.can;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * The class to handle CAN messages from a JK {@link BMS}.
 */
public class JKBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(JKBmsCANProcessor.class);

    @Override
    protected void collectData(final Port port) {
        try {
            final int bmsNo = 0;
            final BatteryPack pack = getBatteryPack(bmsNo);
            final ByteBuffer frame = port.receiveFrame();
            final int frameId = frame.getInt();
            final byte[] bytes = new byte[8];
            frame.get(bytes);
            final ByteBuffer data = ByteBuffer.wrap(bytes);

            switch (frameId) {
                case 0x2F4:
                    readBatteryStatus(pack, data);
                break;
                case 0x4F4:
                    readCellVoltage(pack, data);
                break;
                case 0x5F4:
                    readCellTemperature(pack, data);
                break;
                case 0x7F4:
                    readAlarms(pack, data);
                break;
            }

        } catch (final IOException e) {
            LOG.error("Error receiving frame!", e);
        }
    }


    private void readBatteryStatus(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Battery voltage (0.1V)
        pack.packVoltage = data.getShort();
        // Battery current (0.1A) offset 4000
        pack.packCurrent = data.getShort() - 4000;
        // Battery SOC (1%)
        pack.packSOC = data.get();
        // skip 1 byte
        data.get();
        // discharge time, e.g. 100h (not mapped)
        data.getShort();
    }


    private void readCellVoltage(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell voltage (1mV)
        pack.maxCellmV = data.getShort();
        // Maximum cell voltage cell number
        pack.maxCellVNum = data.get();
        // Minimum cell voltage (1mV)
        pack.minCellmV = data.getShort();
        // Minimum cell voltage cell number
        pack.minCellVNum = data.get();
    }


    private void readCellTemperature(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell temperature (1C) offset -50
        pack.tempMax = (data.get() - 50) * 10;
        // Maximum cell temperature cell number
        data.get();
        // Minimum cell temperature (1C) offset -50
        pack.tempMin = (data.get() - 50) * 10;
        // Minimum cell temperature cell number
        data.get();
        // Average cell temperature (1C) offset -50
        pack.tempAverage = (data.get() - 50) * 10;
    }


    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        // read first 8 bits
        byte value = data.get();

        // unit overvoltage
        int bits = read2Bits(value, 0);
        pack.alarms.levelOneCellVoltageTooHigh.value = bits == 1;
        pack.alarms.levelTwoCellVoltageTooHigh.value = bits >= 2;

        // unit undervoltage
        bits = read2Bits(value, 2);
        pack.alarms.levelOneCellVoltageTooLow.value = bits == 1;
        pack.alarms.levelTwoCellVoltageTooLow.value = bits >= 2;

        // total voltage overvoltage
        bits = read2Bits(value, 4);
        pack.alarms.levelOnePackVoltageTooHigh.value = bits == 1;
        pack.alarms.levelTwoPackVoltageTooHigh.value = bits >= 2;

        // total voltage undervoltage
        bits = read2Bits(value, 6);
        pack.alarms.levelOnePackVoltageTooLow.value = bits == 1;
        pack.alarms.levelTwoPackVoltageTooLow.value = bits >= 2;

        // read next 8 bits
        value = data.get();

        // Large pressure difference in cell (not mapped)
        bits = read2Bits(value, 0);

        // discharge overcurrent
        bits = read2Bits(value, 2);
        pack.alarms.levelOneDischargeCurrentTooHigh.value = bits == 1;
        pack.alarms.levelTwoDischargeCurrentTooHigh.value = bits >= 2;

        // charge overcurrent
        bits = read2Bits(value, 4);
        pack.alarms.levelOneChargeCurrentTooHigh.value = bits == 1;
        pack.alarms.levelTwoChargeCurrentTooHigh.value = bits >= 2;

        // temperature too high
        bits = read2Bits(value, 6);
        pack.alarms.levelOneChargeTempTooHigh.value = bits == 1;
        pack.alarms.levelTwoChargeTempTooHigh.value = bits >= 2;

        // read next 8 bits
        value = data.get();

        // temperature too low
        bits = read2Bits(value, 0);
        pack.alarms.levelOneChargeTempTooLow.value = bits == 1;
        pack.alarms.levelTwoChargeTempTooLow.value = bits >= 2;

        // excessive temperature difference
        bits = read2Bits(value, 2);
        pack.alarms.levelOneTempSensorDifferenceTooHigh.value = bits == 1;
        pack.alarms.levelTwoTempSensorDifferenceTooHigh.value = bits >= 2;

        // SOC too low
        bits = read2Bits(value, 4);
        pack.alarms.levelOneStateOfChargeTooLow.value = bits == 1;
        pack.alarms.levelTwoStateOfChargeTooLow.value = bits >= 2;

        // insulation too low (not mapped)
        bits = read2Bits(value, 6);

        // read next 8 bits
        value = data.get();

        // high voltage interlock fault
        bits = read2Bits(value, 0);
        pack.alarms.failureOfVoltageSensorModule.value = bits != 0;

        // external communication failure
        bits = read2Bits(value, 2);
        pack.alarms.failureOfInternalCommunicationModule.value = bits != 0;

        // internal communication failure
        bits = read2Bits(value, 4);
        pack.alarms.failureOfIntranetCommunicationModule.value = bits != 0;

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
