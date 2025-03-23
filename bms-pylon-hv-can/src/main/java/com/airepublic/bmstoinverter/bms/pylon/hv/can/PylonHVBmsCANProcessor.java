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
package com.airepublic.bmstoinverter.bms.pylon.hv.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.core.util.BitUtil;
import com.airepublic.bmstoinverter.core.util.HexUtil;

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
                    LOG.debug("BMS RECEIVED: {}", Port.printBuffer(receiveFrame));
                    handleMessage(receiveFrame);
                    framesToBeReceived--;
                } else {
                    LOG.warn("Message could not be interpreted " + Port.printBuffer(receiveFrame));
                    return readBuffers;
                }
            }
        } while (framesToBeReceived > 0 & skip > 0);

        LOG.debug("Command 0x{} successfully sent and received!", HexUtil.toHexDigits(frameId));
        return readBuffers;
    }


    private void handleMessage(final ByteBuffer receiveFrame) {
        try {
            final int frameId = receiveFrame.getInt();
            final int batteryId = frameId & 0x0000000F;
            final BatteryPack pack = getBatteryPack(batteryId);
            final byte[] dataBytes = new byte[receiveFrame.get(4)];
            receiveFrame.get(dataBytes, 0, dataBytes.length);

            final ByteBuffer data = ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN);

            switch (frameId & 0xFFFFFFF0) {
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
                case 0x42B0: // ???
                case 0x42C0:
                case 0x42D0:
                case 0x42E0:
                break;
                case 0x7310:
                    readHardwareSoftwareVersion(pack, data);
                break;
                case 0x7320:
                    readBatterModuleInfo(pack, data);
                break;
                case 0x7330:
                    readManufacturerPart1(pack, data);
                break;
                case 0x7340:
                    readManufacturerPart2(pack, data);
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
        // second level temperature (0.1 Celcius) offset -100C
        pack.tempAverage = data.getShort() - 1000;
        // Battery SOC (1%)
        pack.packSOC = data.get() * 10;
        // Battery SOH (1%)
        pack.packSOH = data.get() * 10;

        LOG.debug("\nPack V\tPack A\tTempAvg\tPack SOC\tPack SOH\n{}V\t{}A\t{}C\t\t{}%\t{}%", pack.packVoltage / 10f, pack.packCurrent / 10f, pack.tempAverage / 10f, pack.packSOC / 10f, pack.packSOH / 10f);
    }


    // 0x4220
    private void readChargeDischargeValues(final BatteryPack pack, final ByteBuffer data) {
        // Charge cutoff voltage (0.1V)
        pack.maxPackVoltageLimit = data.getShort();
        // Discharge cutoff voltage (0.1V)
        pack.minPackVoltageLimit = data.getShort();

        // TODO check if these should be swapped as described in Growatt_Battery_BMS.pdf
        // Max charge current (0.1A) offset -3000A
        pack.maxPackChargeCurrent = data.getShort() - 30000;
        // Max discharge current (0.1A) offset -3000A
        pack.maxPackDischargeCurrent = data.getShort() - 30000;

        LOG.debug("\nMaxLimit V\tMinLimit V\tMaxCharge A\tMaxDischarge\n{}V\t\t{}V\t\t{}A\t\t\t{}A", pack.maxPackVoltageLimit / 10f, pack.minPackVoltageLimit / 10f, pack.maxPackChargeCurrent / 10f, pack.maxPackDischargeCurrent / 10);
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

        LOG.debug("\nMaxCell V\tMinCell V\n{} (#{})\t{} (#{})", pack.maxCellmV / 1000f, pack.maxCellVNum, pack.minCellmV / 1000f, pack.minCellVNum);
    }


    // 0x4240
    private void readCellTemperature(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell temperature (0.1C) offset -100C
        pack.tempMax = data.getShort() - 1000;
        // Minimum cell temperature (0.1C) offset -100C
        pack.tempMin = data.getShort() - 1000;
        // Maximum cell temperature cell number
        pack.tempMaxCellNum = data.getShort();
        // Minimum cell temperature cell number
        pack.tempMinCellNum = data.getShort();

        LOG.debug("\nMaxCell C\tMinCell C\n{}C (#{})\t{}C (#{})", pack.tempMax / 10f, pack.tempMaxCellNum, pack.tempMin / 10f, pack.tempMinCellNum);
    }


    private AlarmLevel getAlarmLevel(final boolean warning, final boolean alarm) {
        return alarm ? AlarmLevel.ALARM : warning ? AlarmLevel.WARNING : AlarmLevel.NONE;
    }


    // 0x4250
    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        // Basic status
        final byte status = data.get();

        switch (BitUtil.bits(status, 0, 3)) {
            case 0:
                pack.chargeDischargeStatus = 3; // Sleep
            break;
            case 1:
                pack.chargeDischargeStatus = 1; // Charge
            break;
            case 2:
                pack.chargeDischargeStatus = 2; // Discharge
            break;
            case 3:
                pack.chargeDischargeStatus = 0; // Idle/Stationary
            break;
            default:
                pack.chargeDischargeStatus = 0;
        }

        pack.forceCharge = BitUtil.bit(status, 3);
        pack.cellBalanceActive = BitUtil.bit(status, 4);

        // Cycle period
        data.getShort();

        // Error
        final byte error = data.get();
        pack.setAlarm(Alarm.FAILURE_SENSOR_PACK_VOLTAGE, BitUtil.bit(error, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_SENSOR_PACK_TEMPERATURE, BitUtil.bit(error, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_INTERNAL, BitUtil.bit(error, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_CHARGE_BREAKER, BitUtil.bit(error, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_OTHER, BitUtil.bits(error, 3, 4) > 0 ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // Alarm
        final int alarm = data.getShort();
        // Protection
        final int protection = data.getShort();

        pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, getAlarmLevel(BitUtil.bit(protection, 0), BitUtil.bit(alarm, 0)));
        pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, getAlarmLevel(BitUtil.bit(protection, 1), BitUtil.bit(alarm, 1)));
        pack.setAlarm(Alarm.DISCHARGE_VOLTAGE_LOW, getAlarmLevel(BitUtil.bit(protection, 2), BitUtil.bit(alarm, 2)));
        pack.setAlarm(Alarm.CHARGE_VOLTAGE_HIGH, getAlarmLevel(BitUtil.bit(protection, 3), BitUtil.bit(alarm, 3)));
        pack.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, getAlarmLevel(BitUtil.bit(protection, 4), BitUtil.bit(alarm, 4)));
        pack.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, getAlarmLevel(BitUtil.bit(protection, 5), BitUtil.bit(alarm, 5)));
        pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_LOW, getAlarmLevel(BitUtil.bit(protection, 6), BitUtil.bit(alarm, 6)));
        pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_HIGH, getAlarmLevel(BitUtil.bit(protection, 7), BitUtil.bit(alarm, 7)));
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, getAlarmLevel(BitUtil.bit(protection, 8), BitUtil.bit(alarm, 8)));
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, getAlarmLevel(BitUtil.bit(protection, 9), BitUtil.bit(alarm, 9)));
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, getAlarmLevel(BitUtil.bit(protection, 10), BitUtil.bit(alarm, 10)));
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, getAlarmLevel(BitUtil.bit(protection, 11), BitUtil.bit(alarm, 11)));

    }


    // 0x4260
    private void readModuleVoltage(final BatteryPack pack, final ByteBuffer data) {
        // maximum module voltage (0.001V)
        pack.maxModulemV = data.getChar();
        // minimum module voltage (0.001V)
        pack.minModulemV = data.getChar();
        // pack number with maximum module voltage
        pack.maxModulemVNum = data.getShort();
        // pack number with minimum module voltage
        pack.minModulemVNum = data.getShort();

        LOG.debug("\nMaxModule V\tMinModule V\n{}V (#{})\t{}V (#{})", pack.maxModulemV / 1000f, pack.maxModulemVNum, pack.minModulemV / 1000f, pack.minModulemVNum);
    }


    // 0x4270
    private void readModuleTemperature(final BatteryPack pack, final ByteBuffer data) {
        // maximum module temperature (0.1C) offset -100C
        pack.maxModuleTemp = data.getShort() - 1000;
        // minimum module temperature (0.1C) offset -100C
        pack.minModuleTemp = data.getShort() - 1000;
        // pack number with maximum module temperature
        pack.maxModuleTempNum = data.getShort();
        // pack number with minimum module temperature
        pack.minModuleTempNum = data.getShort();

        LOG.debug("\nMaxModule C\tMinModule C\n{}C (#{})\t{}C (#{})", pack.maxModuleTemp / 10f, pack.maxModuleTempNum, pack.minModuleTemp / 10f, pack.minModuleTempNum);
    }


    // 0x4280
    private void readChargeForbiddenMarks(final BatteryPack pack, final ByteBuffer data) {
        // flag if charging is forbidden
        pack.chargeMOSState = data.get() != (byte) 0xAA;
        // flag if discharging is forbidden
        pack.dischargeMOSState = data.get() != (byte) 0xAA;

        LOG.debug("\nCharge Forbidden\tDischarge Forbidden\n\t{}\t\t{}", !pack.chargeMOSState, !pack.dischargeMOSState);
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


    // 0x7320
    private void readBatterModuleInfo(final BatteryPack pack, final ByteBuffer data) {
        // battery module quantity
        pack.numberOfCells = data.getShort();
        // battery modules in series
        pack.modulesInSeries = data.get();
        // cell quantity in battery module
        pack.moduleNumberOfCells = data.get();
        // battery cabinet voltage level (1V)
        pack.moduleVoltage = data.getShort();
        // battery cabinet AH (1AH)
        pack.moduleRatedCapacityAh = data.getShort();

        LOG.debug("\nNo of cells\tModules in Series\tModule No of Cells\tModule V\tModule Capacity Ah\n{}\t\t\t\t{}\t\t\t\t{}\t\t\t\t{}\t\t{}", pack.numberOfCells, pack.modulesInSeries, pack.moduleNumberOfCells, pack.moduleVoltage, pack.moduleRatedCapacityAh);
    }


    // 0x7330
    private void readManufacturerPart1(final BatteryPack pack, final ByteBuffer data) {
        pack.manufacturerCode = "";
        byte chr;

        do {
            chr = data.get();

            if (chr != 0x00) {
                pack.manufacturerCode += (char) chr;
            }
        } while (chr != 0x00 && data.position() < data.capacity());

        LOG.debug("\nManufacturer\n{}", pack.manufacturerCode);
    }


    // 0x7340
    private void readManufacturerPart2(final BatteryPack pack, final ByteBuffer data) {
        byte chr;

        do {
            chr = data.get();

            if (chr != 0x00) {
                pack.manufacturerCode += (char) chr;
            }
        } while (chr != 0x00 && data.position() < data.capacity());

        LOG.debug("\nManufacturer\n{}", pack.manufacturerCode);
    }


    public static void main(final String[] args) {
        final PylonHVBmsCANProcessor p = new PylonHVBmsCANProcessor();
        final BatteryPack pack = new BatteryPack();
        final ByteBuffer data = ByteBuffer.allocate(8);
        data.put(new byte[] { 0x1E, (byte) 0xC3, 0x1B, (byte) 0xC3, 0x02, 0x00, 0x00, 0x00 }).order(ByteOrder.LITTLE_ENDIAN).rewind();
        System.out.println(data.getShort());
        data.rewind();

        p.readModuleVoltage(pack, data);

        System.out.println(pack.maxModulemV + ", " + pack.minModulemV);

    }
}
