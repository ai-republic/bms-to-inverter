package com.airepublic.bmstoinverter.bms.pylon.hv.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.core.util.Util;

import jakarta.inject.Inject;

/**
 * The class to handle CAN messages from a JK {@link BMS}.
 */
public class PylonHVBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(PylonHVBmsCANProcessor.class);
    @Inject
    private EnergyStorage energyStorage;
    private final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);

    @Override
    protected void collectData(final Port port) {
        try {
            // broadcast ensemble information
            sendMessage(port, 0x00004200, (byte) 0);
            // broadcast system equipment information
            sendMessage(port, 0x00004200, (byte) 2);
        } catch (final Throwable t) {

        }
    }


    private List<ByteBuffer> sendMessage(final Port port, final int frameId, final byte cmd) throws IOException {
        final ByteBuffer sendFrame = prepareSendFrame(frameId, cmd);
        int framesToBeReceived = getResponseFrameCount(sendFrame);
        final int frameCount = framesToBeReceived;
        int skip = 20;
        final List<ByteBuffer> readBuffers = new ArrayList<>();

        LOG.debug("SEND: {}", Port.printBuffer(sendFrame));
        ((CANPort) port).sendExtendedFrame(sendFrame);

        // read frames until the requested frame is read
        do {
            skip--;

            for (int i = 0; i < frameCount; i++) {
                final ByteBuffer receiveFrame = port.receiveFrame();

                if (receiveFrame != null) {
                    LOG.debug("RECEIVED: {}", Port.printBuffer(receiveFrame));
                    handleMessage(receiveFrame);
                    framesToBeReceived--;
                } else {
                    LOG.warn("Message could not be interpreted " + Port.printBuffer(receiveFrame));
                    return readBuffers;
                }
            }
        } while (framesToBeReceived > 0 & skip > 0);

        LOG.debug("Command 0x{} successfully sent and received!", HexFormat.of().toHexDigits(frameId));
        return readBuffers;
    }


    private void handleMessage(final ByteBuffer receiveFrame) {
        try {
            final int frameId = receiveFrame.getInt();
            final int bmsNo = frameId & 0x0000000F;
            final BatteryPack pack = energyStorage.getBatteryPack(bmsNo);
            final byte[] dataBytes = new byte[receiveFrame.get(4)];
            receiveFrame.get(8, dataBytes);

            final ByteBuffer data = ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN);

            switch (frameId) {
                case 0x4210:
                    readBatteryStatus(pack, data);
                break;
                case 0x4220:
                    readChargeDischargeValues(pack, data);
                break;
                case 0x4230:
                    readCellVoltage(pack, data);
                break;
                case 0x4240:
                    readCellTemperature(pack, data);
                break;
                case 0x4250:
                    readAlarms(pack, data);
                break;
                case 0x4260:
                    readModuleVoltage(pack, data);
                break;
                case 0x4270:
                    readModuleTemperature(pack, data);
                break;
                case 0x4280:
                    readChargeForbiddenMarks(pack, data);
                break;
                case 0x7310:
                    readHardwareSoftwareVersion(pack, data);
                break;
                case 0x7320:
                    readBatteryCabinetInfo(pack, data);
                break;
                case 0x7330:
                case 0x7340:
                    readManufacturer(pack, data);
                break;

            }

        } catch (final Throwable e) {
            LOG.error("Error interpreting received frame: {}" + Port.printBuffer(receiveFrame), e);
        }
    }


    private int getResponseFrameCount(final ByteBuffer sendFrame2) {
        return 1;
    }


    protected ByteBuffer prepareSendFrame(final int frameId, final byte cmd) {
        sendFrame.rewind();
        sendFrame.putInt(frameId);

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // data
        sendFrame.put(new byte[] { cmd, 0, 0, 0, 0, 0, 0, 0 });
        sendFrame.rewind();

        return sendFrame;
    }


    private void readBatteryStatus(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Battery voltage (0.1V)
        pack.packVoltage = data.getShort();
        // Battery current (0.1A) offset -3000A
        pack.packCurrent = data.getShort() - 30000;
        // second level temperature (0.1 Celcius) offset -100
        pack.tempAverage = data.getShort() - 1000;
        // Battery SOC (1%)
        pack.packSOC = data.get() * 10;
        // Battery SOH (1%)
        pack.packSOH = data.get() * 10;
    }


    private void readChargeDischargeValues(final BatteryPack pack, final ByteBuffer data) {
        // Charge cutoff voltage (0.1V)
        pack.maxPackVoltageLimit = data.getShort();
        // Discharge cutoff voltage (0.1V)
        pack.minPackVoltageLimit = data.getShort();
        // Max charge current (0.1A) offset -3000A
        pack.maxPackChargeCurrent = data.getShort() - 30000;
        // Max discharge current (0.1A) offset -3000A
        pack.maxPackDischargeCurrent = data.getShort() - 30000;
    }


    private void readCellVoltage(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell voltage (1mV)
        pack.maxCellmV = data.getShort();
        // Minimum cell voltage (1mV)
        pack.minCellmV = data.getShort();
        // Maximum cell voltage cell number
        pack.maxCellVNum = data.getShort();
        // Minimum cell voltage cell number
        pack.minCellVNum = data.getShort();
    }


    private void readCellTemperature(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell temperature (C) offset -100
        pack.tempMax = data.getShort() - 100;
        // Minimum cell temperature (C) offset -100
        pack.tempMin = data.getShort() - 100;
        // Maximum cell temperature cell number
        pack.tempMaxCellNum = data.getShort();
        // Minimum cell temperature cell number
        pack.tempMinCellNum = data.getShort();
    }


    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        // Basic status
        final byte status = data.get();

        switch (Util.bits(status, 0, 3)) {
            case 0:
                pack.chargeDischargeStatus = "Sleep";
            break;
            case 1:
                pack.chargeDischargeStatus = "Charge";
            break;
            case 2:
                pack.chargeDischargeStatus = "Discharge";
            break;
            case 3:
                pack.chargeDischargeStatus = "Idle";
            break;
            default:
                pack.chargeDischargeStatus = null;
        }

        pack.forceCharge = Util.bit(status, 3);
        pack.cellBalanceActive = Util.bit(status, 4);

        // Cycle period
        data.getShort();

        // Error
        final byte fault = data.get();
        pack.alarms.failureOfVoltageSensorModule.value = Util.bit(fault, 0);
        pack.alarms.failureOfTemperatureSensorModule.value = Util.bit(fault, 1);
        pack.alarms.failureOfInternalCommunicationModule.value = Util.bit(fault, 2);
        pack.alarms.failureOfMainVoltageSensorModule.value = Util.bit(fault, 3);
        pack.alarms.failureOfAFEAcquisitionModule.value = Util.bit(fault, 4);
        pack.alarms.failureOfChargeFETTBreaker.value = Util.bit(fault, 5);

        // Alarm
        final short alarms = data.getShort();
        pack.alarms.levelOneCellVoltageTooLow.value = Util.bit(alarms, 0);
        pack.alarms.levelOneCellVoltageTooHigh.value = Util.bit(alarms, 1);
        pack.alarms.levelOneStateOfChargeTooLow.value = Util.bit(alarms, 2);
        pack.alarms.levelOneStateOfChargeTooHigh.value = Util.bit(alarms, 3);
        pack.alarms.levelOneChargeTempTooLow.value = Util.bit(alarms, 4);
        pack.alarms.levelOneChargeTempTooHigh.value = Util.bit(alarms, 5);
        pack.alarms.levelOneDischargeTempTooLow.value = Util.bit(alarms, 6);
        pack.alarms.levelOneDischargeTempTooHigh.value = Util.bit(alarms, 7);
        pack.alarms.levelOneChargeCurrentTooHigh.value = Util.bit(alarms, 8);
        pack.alarms.levelOneDischargeCurrentTooHigh.value = Util.bit(alarms, 9);
        pack.alarms.levelOnePackVoltageTooLow.value = Util.bit(alarms, 10);
        pack.alarms.levelOnePackVoltageTooHigh.value = Util.bit(alarms, 11);

        // Protection
        final short protection = data.getShort();
        pack.alarms.levelTwoCellVoltageTooLow.value = Util.bit(protection, 0);
        pack.alarms.levelTwoCellVoltageTooHigh.value = Util.bit(protection, 1);
        pack.alarms.levelTwoStateOfChargeTooLow.value = Util.bit(protection, 2);
        pack.alarms.levelTwoStateOfChargeTooHigh.value = Util.bit(protection, 3);
        pack.alarms.levelTwoChargeTempTooLow.value = Util.bit(protection, 4);
        pack.alarms.levelTwoChargeTempTooHigh.value = Util.bit(protection, 5);
        pack.alarms.levelTwoDischargeTempTooLow.value = Util.bit(protection, 6);
        pack.alarms.levelTwoDischargeTempTooHigh.value = Util.bit(protection, 7);
        pack.alarms.levelTwoChargeCurrentTooHigh.value = Util.bit(protection, 8);
        pack.alarms.levelTwoDischargeCurrentTooHigh.value = Util.bit(protection, 9);
        pack.alarms.levelTwoPackVoltageTooLow.value = Util.bit(protection, 10);
        pack.alarms.levelTwoPackVoltageTooHigh.value = Util.bit(protection, 11);
    }


    private void readModuleVoltage(final BatteryPack pack, final ByteBuffer data) {
        // maximum module voltage (0.001V)
        pack.maxModulemV = data.getShort();
        // minimum module voltage (0.001V)
        pack.minModulemV = data.getShort();
        // pack number with maximum module voltage
        pack.maxModulemVNum = data.getShort();
        // pack number with minimum module voltage
        pack.minModulemVNum = data.getShort();
    }


    private void readModuleTemperature(final BatteryPack pack, final ByteBuffer data) {
        // maximum module temperature (0.1C)
        pack.maxModuleTemp = data.getShort();
        // minimum module temperature (0.1C)
        pack.minModuleTemp = data.getShort();
        // pack number with maximum module temperature
        pack.maxModuleTempNum = data.getShort();
        // pack number with minimum module temperature
        pack.minModuleTempNum = data.getShort();
    }


    private void readChargeForbiddenMarks(final BatteryPack pack, final ByteBuffer data) {
        // flag if charging is forbidden
        pack.chargeForbidden = data.get() == 0xAA;
        // flag if discharging is forbidden
        pack.dischargeForbidden = data.get() == 0xAA;
    }


    private void readHardwareSoftwareVersion(final BatteryPack pack, final ByteBuffer data) {
        // hardware version
        final byte hwType = data.get();
        data.get(); // skip
        // hardware version V 0x02
        final byte hwV = data.get();
        // hardware version R 0x01
        final byte hwR = data.get();
        // software version V major 0x01
        final byte swV = data.get();
        // software version R minor 0x02
        final byte swR = data.get();
        // software version
        final byte swX = data.get();
        // software version
        final byte swY = data.get();

        switch (hwType) {
            case 0:
                pack.hardwareVersion = "";
            break;
            case 1:
                pack.hardwareVersion = "A";
            break;
            case 2:
                pack.hardwareVersion = "B";
            break;
            default:
                pack.hardwareVersion = "";
        }

        pack.hardwareVersion += Byte.toUnsignedInt(hwV) + "." + Byte.toUnsignedInt(hwR);
        pack.softwareVersion = Byte.toUnsignedInt(swV) + "." + Byte.toUnsignedInt(swR) + "." + Byte.toUnsignedInt(swX) + "." + Byte.toUnsignedInt(swY);
    }


    private void readBatteryCabinetInfo(final BatteryPack pack, final ByteBuffer data) {
        // battery module quantity
        data.getShort();
        // battery modules in series
        data.get();
        // cell quantity in battery module
        pack.numberOfCells = data.get(3);
        // battery cabinet voltage level (1A)
        data.getShort();
        // battery cabinet AH (1AH)
        data.getShort();
    }


    private void readManufacturer(final BatteryPack pack, final ByteBuffer data) {
        pack.manufacturerCode = "PYLONTECH";
    }

}
