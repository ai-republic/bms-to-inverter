package com.airepublic.bmstoinverter.bms.seplos.can;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Bms;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortType;
import com.airepublic.bmstoinverter.core.Protocol;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CAN;

import jakarta.inject.Inject;

/**
 * The class to handle {@link CAN} messages from a Seplos {@link Bms}.
 */
@PortType(Protocol.CAN)
public class SeplosBmsCANProcessor implements Bms {
    private final static Logger LOG = LoggerFactory.getLogger(SeplosBmsCANProcessor.class);
    @Inject
    private EnergyStorage energyStorage;

    @Override
    public void initialize() {
    }


    @Override
    public void process(final Runnable callback) {
        for (int bmsNo = 0; bmsNo < energyStorage.getBatteryPackCount(); bmsNo++) {
            try {
                final Port port = energyStorage.getBatteryPack(bmsNo).port;
                final ByteBuffer frame = port.receiveFrame(null);
                final int frameId = frame.getInt();
                final byte[] bytes = new byte[8];
                frame.get(bytes);
                final ByteBuffer data = ByteBuffer.wrap(bytes);

                switch (frameId) {
                    case 0x351:
                        readChargeDischargeInfo(bmsNo, data);
                    break;
                    case 0x355:
                        readSOC(bmsNo, data);
                    break;
                    case 0x356:
                        readBatteryVoltage(bmsNo, data);
                    break;
                    case 0x35C:
                        requestChargeDischargeConfigChange(bmsNo, data);
                    break;
                    case 0x370:
                        readMinMaxTemperatureVoltage(bmsNo, data);
                    break;
                    case 0x371:
                        readTemperatureIds(bmsNo, data);
                    break;
                    case 0x35E:
                        readManufacturer(bmsNo, data);
                    break;
                    case 0x359:
                        readAlarms(bmsNo, data);
                    break;
                }
            } catch (final IOException e) {
                LOG.error("Error receiving frame!", e);
            }
        }

        try {
            callback.run();
        } catch (final Exception e) {
            LOG.error("BMS process callback threw an exception!", e);
        }
    }


    // 0x351
    private void readChargeDischargeInfo(final int bmsNo, final ByteBuffer data) {
        // Battery charge voltage (0.1V) - uint_16
        energyStorage.getBatteryPack(bmsNo).maxPackVoltageLimit = data.getChar();
        // Charge current limit (0.1A) - sint_16
        energyStorage.getBatteryPack(bmsNo).maxPackChargeCurrent = data.getShort();
        // Discharge current limit (0.1A) - sint_16
        energyStorage.getBatteryPack(bmsNo).maxPackDischargeCurrent = data.getShort();
        // Battery discharge voltage (0.1V) - uint_16
        energyStorage.getBatteryPack(bmsNo).minPackVoltageLimit = data.getChar();

    }


    // 0x355
    private void readSOC(final int bmsNo, final ByteBuffer data) {
        // SOC (1%) - uint_16
        energyStorage.getBatteryPack(bmsNo).maxPackDischargeCurrent = data.getChar();
        // SOH (1%) - uint_16
        energyStorage.getBatteryPack(bmsNo).packVoltage = data.getChar();
    }


    // 0x356
    private void readBatteryVoltage(final int bmsNo, final ByteBuffer data) {
        // Battery voltage (0.01V) - uint_16
        energyStorage.getBatteryPack(bmsNo).packVoltage = data.getShort();
        // Battery current (0.1A) - uint_16
        energyStorage.getBatteryPack(bmsNo).packCurrent = data.getShort();
        // Battery current (0.1C) - uint_16
        energyStorage.getBatteryPack(bmsNo).tempAverage = data.getShort();
    }


    // 0x35C
    private void requestChargeDischargeConfigChange(final int bmsNo, final ByteBuffer data) {
        final byte bits = data.get();

        if (bitRead(bits, 4)) {
            // request force-charge II
        }

        if (bitRead(bits, 5)) {
            // request force-charge I
        }

        if (bitRead(bits, 6)) {
            // request discharge enable
        }

        if (bitRead(bits, 7)) {
            // request charge enable
        }
    }


    // 0x370
    private void readMinMaxTemperatureVoltage(final int bmsNo, final ByteBuffer data) {
        // Maximum cell temperature (0.1C) - uint_16
        energyStorage.getBatteryPack(bmsNo).tempMax = data.getShort();
        // Minimum cell temperature (0.1C) - uint_16
        energyStorage.getBatteryPack(bmsNo).tempMin = data.getShort();
        // Maximum cell voltage (0.1V) - uint_16
        energyStorage.getBatteryPack(bmsNo).maxCellmV = data.getShort();
        // Minimum cell voltage (0.1V) - uint_16
        energyStorage.getBatteryPack(bmsNo).minCellmV = data.getShort();
    }


    // 0x371
    private void readTemperatureIds(final int bmsNo, final ByteBuffer data) {
        // Maximum cell temperature (0.1C) - uint_16
        // energyStorage.getBatteryPack(bmsNo).tempMax = data.getShort();
        // Minimum cell temperature (0.1C) - uint_16
        // energyStorage.getBatteryPack(bmsNo).tempMin = data.getShort();
        // Maximum cell voltage id - uint_16
        energyStorage.getBatteryPack(bmsNo).maxCellVNum = data.getShort();
        // Minimum cell voltage id - uint_16
        energyStorage.getBatteryPack(bmsNo).minCellVNum = data.getShort();
    }


    // 0x35E
    private void readManufacturer(final int bmsNo, final ByteBuffer data) {
        final char first = (char) data.get();
        final char second = (char) data.get();

        energyStorage.getBatteryPack(bmsNo).manufacturerCode = "" + first + second;
    }


    // 0x359
    private void readAlarms(final int bmsNo, final ByteBuffer data) {
        // read first 8 bits
        int value = data.getInt();

        // protection alarms
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoCellVoltageTooHigh.value = bitRead(value, 1);
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoCellVoltageTooLow.value = bitRead(value, 2);
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoDischargeTempTooHigh.value = bitRead(value, 3);
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoDischargeTempTooLow.value = bitRead(value, 4);
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoDischargeCurrentTooHigh.value = bitRead(value, 7);
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoChargeCurrentTooHigh.value = bitRead(value, 8);

        // warning alarms
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneCellVoltageTooHigh.value = bitRead(value, 17);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneCellVoltageTooLow.value = bitRead(value, 18);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneChargeTempTooHigh.value = bitRead(value, 19);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneChargeTempTooLow.value = bitRead(value, 20);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneDischargeCurrentTooHigh.value = bitRead(value, 23);
        energyStorage.getBatteryPack(bmsNo).alarms.levelOneChargeCurrentTooHigh.value = bitRead(value, 24);
        energyStorage.getBatteryPack(bmsNo).alarms.failureOfIntranetCommunicationModule.value = bitRead(value, 27);
        energyStorage.getBatteryPack(bmsNo).alarms.levelTwoCellVoltageDifferenceTooHigh.value = bitRead(value, 28);

        energyStorage.getBatteryPack(bmsNo).numberOfCells = data.get();

        // skip two bytes
        data.getShort();

        // dip switch
        value = data.get();
        final int packNo = value >> 4;
        final int cellNo = value & 0x0F;

    }


    private boolean bitRead(final int value, final int index) {
        return (value >> index & 1) == 1;
    }

}
