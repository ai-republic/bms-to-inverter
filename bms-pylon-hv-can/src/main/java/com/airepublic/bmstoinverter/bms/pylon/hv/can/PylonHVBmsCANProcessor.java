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
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.core.util.Util;

/**
 * The class to handle CAN messages from a JK {@link BMS}.
 */
public class PylonHVBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(PylonHVBmsCANProcessor.class);
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
        int framesToBeReceived = getResponseFrameCount(frameId);
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
            final BatteryPack pack = getBatteryPack(bmsNo);
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
                    readBatterModuleInfo(pack, data);
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


    private int getResponseFrameCount(final int frameId) {
        switch (frameId) {
            case 0x00004200:
                return 10;
        }
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


    // 0x4210
    private void readBatteryStatus(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Battery voltage (0.1V)
        pack.packVoltage = data.getShort();
        // Battery current (0.1A) offset -3000A
        pack.packCurrent = data.getShort() - 30000;
        // second level temperature (0.1 Celcius) offset -100
        pack.tempAverage = data.getShort() / 10 - 100;
        // Battery SOC (1%)
        pack.packSOC = data.get() * 10;
        // Battery SOH (1%)
        pack.packSOH = data.get() * 10;

        LOG.debug("\nPack V\tPack A\tTempAvg\tPack SOC\tPack SOH\n{}V\t{}A\t{}C\t{}%\t{}%", pack.packVoltage / 10f, pack.packCurrent / 10f, pack.tempAverage, pack.packSOC / 10f, pack.packSOH / 10f);
    }


    // 0x4220
    private void readChargeDischargeValues(final BatteryPack pack, final ByteBuffer data) {
        // Charge cutoff voltage (0.1V)
        pack.maxPackVoltageLimit = data.getShort();
        // Discharge cutoff voltage (0.1V)
        pack.minPackVoltageLimit = data.getShort();
        // Max charge current (0.1A) offset -3000A
        pack.maxPackChargeCurrent = 30000 - data.getShort();
        // Max discharge current (0.1A) offset -3000A
        pack.maxPackDischargeCurrent = 30000 - data.getShort();

        LOG.debug("\nMaxLimit V\tMinLimit V\tMaxCharge A\tMaxDischarge\n{}V\t{}V\t{}A\t{}A", pack.maxPackVoltageLimit / 10f, pack.minPackVoltageLimit / 10f, (pack.maxPackChargeCurrent + 3000) / 10f, (pack.maxPackDischargeCurrent + 3000) / 10);
    }


    // 0x4230
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

        LOG.debug("\nMaxCell V\tMinCell V\n{}\t{}", pack.maxCellmV / 1000f + "V (#" + pack.maxCellVNum + ")", pack.minCellmV / 1000f + " V(#" + pack.minCellVNum + ")");
    }


    // 0x4240
    private void readCellTemperature(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell temperature (0.1C) offset -100C
        pack.tempMax = (data.getShort() - 1000) / 10;
        // Minimum cell temperature (0.1C) offset -100C
        pack.tempMin = (data.getShort() - 1000) / 10;
        // Maximum cell temperature cell number
        pack.tempMaxCellNum = data.getShort();
        // Minimum cell temperature cell number
        pack.tempMinCellNum = data.getShort();

        LOG.debug("\nMaxCell C\tMinCell C\n{}\t{}", pack.tempMax + "C (#" + pack.tempMaxCellNum + ")", pack.tempMin + "C (#" + pack.tempMinCellNum + ")");
    }


    // 0x4250
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
        final byte error = data.get();
        pack.alarms.failureOfVoltageSensorModule.value = Util.bit(error, 0);
        pack.alarms.failureOfTemperatureSensorModule.value = Util.bit(error, 1);
        pack.alarms.failureOfInternalCommunicationModule.value = Util.bit(error, 2);
        pack.alarms.failureOfMainVoltageSensorModule.value = Util.bit(error, 3);
        pack.alarms.failureOfAFEAcquisitionModule.value = Util.bit(error, 4);
        pack.alarms.failureOfChargeFETTBreaker.value = Util.bit(error, 5);

        // Alarm
        final short alarm = data.getShort();
        pack.alarms.levelOneCellVoltageTooLow.value = Util.bit(alarm, 0);
        pack.alarms.levelOneCellVoltageTooHigh.value = Util.bit(alarm, 1);
        pack.alarms.levelOneStateOfChargeTooLow.value = Util.bit(alarm, 2);
        pack.alarms.levelOneStateOfChargeTooHigh.value = Util.bit(alarm, 3);
        pack.alarms.levelOneChargeTempTooLow.value = Util.bit(alarm, 4);
        pack.alarms.levelOneChargeTempTooHigh.value = Util.bit(alarm, 5);
        pack.alarms.levelOneDischargeTempTooLow.value = Util.bit(alarm, 6);
        pack.alarms.levelOneDischargeTempTooHigh.value = Util.bit(alarm, 7);
        pack.alarms.levelOneChargeCurrentTooHigh.value = Util.bit(alarm, 8);
        pack.alarms.levelOneDischargeCurrentTooHigh.value = Util.bit(alarm, 9);
        pack.alarms.levelOnePackVoltageTooLow.value = Util.bit(alarm, 10);
        pack.alarms.levelOnePackVoltageTooHigh.value = Util.bit(alarm, 11);

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


    // 0x4260
    private void readModuleVoltage(final BatteryPack pack, final ByteBuffer data) {
        // maximum module voltage (0.001V)
        pack.maxModulemV = data.getShort();
        // minimum module voltage (0.001V)
        pack.minModulemV = data.getShort();
        // pack number with maximum module voltage
        pack.maxModulemVNum = data.getShort();
        // pack number with minimum module voltage
        pack.minModulemVNum = data.getShort();

        LOG.debug("\nMaxModule V\tMinModule V\n{}\t{}", pack.maxModulemV / 1000 + "V (#" + pack.maxModulemVNum + ")", pack.minModulemV / 1000 + "V (#" + pack.minModulemVNum + ")");
    }


    // 0x4270
    private void readModuleTemperature(final BatteryPack pack, final ByteBuffer data) {
        // maximum module temperature (0.1C)
        pack.maxModuleTemp = data.getShort();
        // minimum module temperature (0.1C)
        pack.minModuleTemp = data.getShort();
        // pack number with maximum module temperature
        pack.maxModuleTempNum = data.getShort();
        // pack number with minimum module temperature
        pack.minModuleTempNum = data.getShort();

        LOG.debug("\nMaxModule C\tMinModule C\n{}\t{}", pack.maxModuleTemp / 10 + "C (#" + pack.maxModuleTempNum + ")", pack.minModuleTemp / 10 + "C (#" + pack.minModuleTempNum + ")");
    }


    // 0x4280
    private void readChargeForbiddenMarks(final BatteryPack pack, final ByteBuffer data) {
        // flag if charging is forbidden
        pack.chargeForbidden = data.get() == 0xAA;
        // flag if discharging is forbidden
        pack.dischargeForbidden = data.get() == 0xAA;

        LOG.debug("\nCharge Forbidden\tDischarge Forbidden\n\t{}\t\t{}", pack.chargeForbidden, pack.dischargeForbidden);
    }


    // 0x7310
    private void readHardwareSoftwareVersion(final BatteryPack pack, final ByteBuffer data) {
        // hardware version
        final byte hwType = data.get();
        data.get(); // skip
        // hardware version major
        final byte hwV = data.get();
        // hardware version R revision
        final byte hwR = data.get();
        // software version V major
        final byte swV = data.get();
        // software version R minor
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

        LOG.debug("\nHW version\tSW version\n{}\t{}", pack.hardwareVersion, pack.softwareVersion);
    }


    private void readBatterModuleInfo(final BatteryPack pack, final ByteBuffer data) {
        // battery module quantity
        pack.numberOfCells = data.getShort();
        // battery modules in series
        pack.modulesInSeries = data.get();
        // cell quantity in battery module
        pack.moduleNumberOfCells = data.get(3);
        // battery cabinet voltage level (1V)
        pack.moduleVoltage = data.getShort();
        // battery cabinet AH (1AH)
        pack.moduleRatedCapacityAh = data.getShort();

        LOG.debug("\nNo of cells\n{}", pack.numberOfCells);
    }


    private void readManufacturer(final BatteryPack pack, final ByteBuffer data) {
        pack.manufacturerCode = "";
        byte chr;

        do {
            chr = data.get();

            if (chr != 0x00) {
                pack.manufacturerCode += (char) chr;
            }
        } while (chr != 0x00 && data.position() < data.capacity());

        LOG.debug("Manufacturer", pack.manufacturerCode);
    }

}
