package com.airepublic.bmstoinverter.bms.pylon.can;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.Util;

/**
 * The class to handle CAN messages from a Pylon {@link BMS}.
 */
public class PylonBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(PylonBmsCANProcessor.class);

    @Override
    public void collectData(final Port port) {
        try {
            final BatteryPack pack = getBatteryPack();
            final ByteBuffer frame = port.receiveFrame();
            final int frameId = frame.getInt();
            final byte[] bytes = new byte[8];
            frame.get(bytes);
            final ByteBuffer data = ByteBuffer.wrap(bytes);

            switch (frameId) {
                case 0x351:
                    readChargeDischargeInfo(pack, data);
                break;
                case 0x355:
                    readSOC(pack, data);
                break;
                case 0x356:
                    readBatteryVoltage(pack, data);
                break;
                case 0x35C:
                    requestChargeDischargeConfigChange(pack, data);
                break;
                case 0x370:
                    readMinMaxTemperatureVoltage(pack, data);
                break;
                case 0x371:
                    readTemperatureIds(pack, data);
                break;
                case 0x35E:
                    readManufacturer(pack, data);
                break;
                case 0x359:
                    readAlarms(pack, data);
                break;
            }
        } catch (final IOException e) {
            LOG.error("Error receiving frame!", e);
        }
    }


    // 0x351
    private void readChargeDischargeInfo(final BatteryPack pack, final ByteBuffer data) {
        // Battery charge voltage (0.1V) - uint_16
        pack.maxPackVoltageLimit = data.getChar();
        // Charge current limit (0.1A) - sint_16
        pack.maxPackChargeCurrent = data.getShort();
        // Discharge current limit (0.1A) - sint_16
        pack.maxPackDischargeCurrent = data.getShort();
        // Battery discharge voltage (0.1V) - uint_16
        pack.minPackVoltageLimit = data.getChar();

    }


    // 0x355
    private void readSOC(final BatteryPack pack, final ByteBuffer data) {
        // SOC (1%) - uint_16
        pack.maxPackDischargeCurrent = data.getChar();
        // SOH (1%) - uint_16
        pack.packVoltage = data.getChar();
    }


    // 0x356
    private void readBatteryVoltage(final BatteryPack pack, final ByteBuffer data) {
        // Battery voltage (0.01V) - uint_16
        pack.packVoltage = data.getShort();
        // Battery current (0.1A) - uint_16
        pack.packCurrent = data.getShort();
        // Battery current (0.1C) - uint_16
        pack.tempAverage = data.getShort();
    }


    // 0x35C
    private void requestChargeDischargeConfigChange(final BatteryPack pack, final ByteBuffer data) {
        final byte bits = data.get();

        if (Util.bit(bits, 4)) {
            // request force-charge II
        }

        if (Util.bit(bits, 5)) {
            // request force-charge I
        }

        if (Util.bit(bits, 6)) {
            // request discharge enable
        }

        if (Util.bit(bits, 7)) {
            // request charge enable
        }
    }


    // 0x370
    private void readMinMaxTemperatureVoltage(final BatteryPack pack, final ByteBuffer data) {
        // Maximum cell temperature (0.1C) - uint_16
        pack.tempMax = data.getShort();
        // Minimum cell temperature (0.1C) - uint_16
        pack.tempMin = data.getShort();
        // Maximum cell voltage (0.1V) - uint_16
        pack.maxCellmV = data.getShort();
        // Minimum cell voltage (0.1V) - uint_16
        pack.minCellmV = data.getShort();
    }


    // 0x371
    private void readTemperatureIds(final BatteryPack pack, final ByteBuffer data) {
        // Maximum cell temperature (0.1C) - uint_16
        // pack.tempMax = data.getShort();
        // Minimum cell temperature (0.1C) - uint_16
        // pack.tempMin = data.getShort();
        // Maximum cell voltage id - uint_16
        pack.maxCellVNum = data.getShort();
        // Minimum cell voltage id - uint_16
        pack.minCellVNum = data.getShort();
    }


    // 0x35E
    private void readManufacturer(final BatteryPack pack, final ByteBuffer data) {
        final char first = (char) data.get();
        final char second = (char) data.get();

        pack.manufacturerCode = "" + first + second;
    }


    // 0x359
    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        // read first 8 bits
        int value = data.getInt();

        // protection alarms
        pack.alarms.levelTwoCellVoltageTooHigh.value = Util.bit(value, 1);
        pack.alarms.levelTwoCellVoltageTooLow.value = Util.bit(value, 2);
        pack.alarms.levelTwoDischargeTempTooHigh.value = Util.bit(value, 3);
        pack.alarms.levelTwoDischargeTempTooLow.value = Util.bit(value, 4);
        pack.alarms.levelTwoDischargeCurrentTooHigh.value = Util.bit(value, 7);
        pack.alarms.levelTwoChargeCurrentTooHigh.value = Util.bit(value, 8);

        // warning alarms
        pack.alarms.levelOneCellVoltageTooHigh.value = Util.bit(value, 17);
        pack.alarms.levelOneCellVoltageTooLow.value = Util.bit(value, 18);
        pack.alarms.levelOneChargeTempTooHigh.value = Util.bit(value, 19);
        pack.alarms.levelOneChargeTempTooLow.value = Util.bit(value, 20);
        pack.alarms.levelOneDischargeCurrentTooHigh.value = Util.bit(value, 23);
        pack.alarms.levelOneChargeCurrentTooHigh.value = Util.bit(value, 24);
        pack.alarms.failureOfIntranetCommunicationModule.value = Util.bit(value, 27);
        pack.alarms.levelTwoCellVoltageDifferenceTooHigh.value = Util.bit(value, 28);

        pack.numberOfCells = data.get();

        // skip two bytes
        data.getShort();

        // dip switch
        value = data.get();
        final int packNo = value >> 4;
        final int cellNo = value & 0x0F;

    }

}
