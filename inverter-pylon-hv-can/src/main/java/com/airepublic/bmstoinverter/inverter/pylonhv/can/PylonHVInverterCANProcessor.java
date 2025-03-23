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
package com.airepublic.bmstoinverter.inverter.pylonhv.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.core.util.BitUtil;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The class to handle CAN messages for a Pylon HV {@link Inverter}.
 */
@ApplicationScoped
public class PylonHVInverterCANProcessor extends Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(PylonHVInverterCANProcessor.class);

    @Override
    protected ByteBuffer readRequest(final Port port) throws IOException {
        return port.receiveFrame();
    }


    @Override
    protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
        ((CANPort) port).sendExtendedFrame(frame);
    }


    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        List<ByteBuffer> sendFrames = null;
        requestFrame.rewind();
        final int frameId = requestFrame.getInt();
        final int length = requestFrame.get();
        final byte[] data = new byte[length];
        requestFrame.position(8);
        requestFrame.get(data);

        try {
            switch (frameId) {
                case 0x00004200:
                    switch (data[0]) {
                        case 0x00:
                            sendFrames = sendEnsembleInformation(aggregatedPack);
                        break;
                        case 0x02:
                            sendFrames = sendEquipmentInformation(aggregatedPack);
                        break;
                    }
                break;
            }
        } catch (final IOException e) {
            LOG.error("Error creating responses for request: " + Port.printBuffer(requestFrame), e);
        }

        return sendFrames;
    }


    protected ByteBuffer prepareSendFrame(final int frameId) {
        final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);
        sendFrame.putInt(frameId);

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        return sendFrame;
    }


    private List<ByteBuffer> sendEnsembleInformation(final BatteryPack pack) throws IOException {
        final List<ByteBuffer> sendFrames = new ArrayList<>();
        // 0x4210
        sendFrames.add(sendBatteryStatus(pack));
        // 0x4220
        sendFrames.add(sendChargeDischargeLimits(pack));
        // 0x4230
        sendFrames.add(sendMaxMinCellVoltages(pack));
        // 0x4240
        sendFrames.add(sendMaxMinCellTemperatures(pack));
        // 0x4250
        sendFrames.add(sendAlarms(pack));
        // 0x4260
        sendFrames.add(sendMaxMinModuleVoltages(pack));
        // 0x4270
        sendFrames.add(sendMaxMinModuleTemperatures(pack));
        // 0x4280
        sendFrames.add(sendChargeForbiddenMarks(pack));
        // 0x42B0
        sendFrames.add(sendBatteyVoltageSOCSOH(pack));

        return sendFrames;
    }


    private List<ByteBuffer> sendEquipmentInformation(final BatteryPack pack) throws IOException {
        final List<ByteBuffer> sendFrames = new ArrayList<>();
        // 0x7310
        sendFrames.add(sendHardwareSoftwareVersion(pack));
        // 0x7320
        sendFrames.add(sendBatterModuleInfo(pack));
        // 0x7340
        sendFrames.addAll(sendManufacturer(pack));

        return sendFrames;
    }


    // 0x4210
    private ByteBuffer sendBatteryStatus(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00004210);

        // Battery voltage (0.1V)
        frame.putShort((short) pack.packVoltage);
        // Battery current (0.1A) offset -3000A
        frame.putShort((short) (pack.packCurrent + 30000));
        // second level temperature (0.1 Celcius) offset -100C
        frame.putShort((short) (pack.tempAverage + 1000));
        // Battery SOC (1%)
        frame.put((byte) (pack.packSOC / 10));
        // Battery SOH (1%)
        frame.put((byte) (pack.packSOH / 10));

        LOG.debug("Sending battery status: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x4220
    private ByteBuffer sendChargeDischargeLimits(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00004220);

        // Charge cutoff voltage (0.1V)
        frame.putShort((short) pack.maxPackVoltageLimit);
        // Discharge cutoff voltage (0.1V)
        frame.putShort((short) pack.minPackVoltageLimit);

        // TODO check if these should be swapped as described in Growatt_Battery_BMS.pdf
        // Max charge current (0.1A) offset -3000A
        frame.putShort((short) (pack.maxPackChargeCurrent + 30000));
        // Max discharge current (0.1A) offset -3000A
        frame.putShort((short) (pack.maxPackDischargeCurrent + 30000));

        LOG.debug("Sending max/min voltage, current, charge and discharge limits: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x4230
    private ByteBuffer sendMaxMinCellVoltages(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00004230);

        // Maximum cell voltage (0.001V)
        frame.putShort((short) pack.maxCellmV);
        // Minimum cell voltage (0.001V)
        frame.putShort((short) pack.minCellmV);
        // Cell no with maximum voltage
        frame.putShort((short) pack.maxCellVNum);
        // Cell no with minimum voltage
        frame.putShort((short) pack.minCellVNum);

        LOG.debug("Sending max/min cell voltages: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x4240
    private ByteBuffer sendMaxMinCellTemperatures(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00004240);

        // Maximum cell temperature (0.1C) offset -100C
        frame.putShort((short) (pack.tempMax + 1000));
        // Minimum cell temperature (0.1C) offset -100C
        frame.putShort((short) (pack.tempMin + 1000));
        // Maximum cell temperature cell number
        frame.putShort((short) pack.tempMaxCellNum);
        // Minimum cell temperature cell number
        frame.putShort((short) pack.tempMinCellNum);

        LOG.debug("Sending max/min cell temparatures: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x4250
    private ByteBuffer sendAlarms(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00004250);

        // Basic status
        byte status = 0x00;

        switch (pack.chargeDischargeStatus) {
            case 0: // Idle/Stationary
                status = BitUtil.setBit(status, 0, true);
                status = BitUtil.setBit(status, 1, true);
                status = BitUtil.setBit(status, 2, false);
            break;
            case 1: // Charge
                status = BitUtil.setBit(status, 0, true);
                status = BitUtil.setBit(status, 1, false);
                status = BitUtil.setBit(status, 2, false);
            break;
            case 2: // Discharge
                status = BitUtil.setBit(status, 0, false);
                status = BitUtil.setBit(status, 1, true);
                status = BitUtil.setBit(status, 2, false);
            break;
            case 3: // Sleep
                status = BitUtil.setBit(status, 0, false);
                status = BitUtil.setBit(status, 1, false);
                status = BitUtil.setBit(status, 2, false);
            break;
        }

        status = BitUtil.setBit(status, 3, pack.forceCharge);
        status = BitUtil.setBit(status, 4, pack.cellBalanceActive);

        frame.put(status);

        // Cycle period
        frame.putShort((short) 0x0000);

        // Error
        byte error = 0x00;
        error = BitUtil.setBit(error, 0, pack.getAlarmLevel(Alarm.FAILURE_SENSOR_PACK_VOLTAGE) != AlarmLevel.NONE);
        error = BitUtil.setBit(error, 1, pack.getAlarmLevel(Alarm.FAILURE_SENSOR_PACK_TEMPERATURE) != AlarmLevel.NONE);
        error = BitUtil.setBit(error, 2, pack.getAlarmLevel(Alarm.FAILURE_COMMUNICATION_INTERNAL) != AlarmLevel.NONE);
        error = BitUtil.setBit(error, 3, pack.getAlarmLevel(Alarm.FAILURE_CHARGE_BREAKER) != AlarmLevel.NONE);
        error = BitUtil.setBit(error, 7, pack.getAlarmLevel(Alarm.FAILURE_OTHER) != AlarmLevel.NONE);

        frame.put(error);

        // Alarm
        short alarm = (short) 0x0000;
        alarm = BitUtil.setBit(alarm, 0, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 1, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 2, pack.getAlarmLevel(Alarm.DISCHARGE_VOLTAGE_LOW) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 3, pack.getAlarmLevel(Alarm.CHARGE_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 4, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 5, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 6, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_LOW) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 7, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 8, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 9, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 10, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 11, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.WARNING);

        frame.putShort(alarm);

        // Protection
        short protection = (short) 0x0000;
        protection = BitUtil.setBit(protection, 0, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 1, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 2, pack.getAlarmLevel(Alarm.DISCHARGE_VOLTAGE_LOW) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 3, pack.getAlarmLevel(Alarm.CHARGE_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 4, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 5, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 6, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 7, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 8, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 9, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 10, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 11, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.ALARM);

        frame.putShort(protection);

        LOG.debug("Sending alarms: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x4260
    private ByteBuffer sendMaxMinModuleVoltages(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00004260);

        // maximum module voltage (0.001V)
        frame.putChar((char) pack.maxModulemV);
        // minimum module voltage (0.001V)
        frame.putChar((char) pack.minModulemV);
        // pack number with maximum module voltage
        frame.putShort((short) pack.maxModulemVNum);
        // pack number with minimum module voltage
        frame.putShort((short) pack.minModulemVNum);

        LOG.debug("Sending max/min module V: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x4270
    private ByteBuffer sendMaxMinModuleTemperatures(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00004270);

        // maximum module temperature (0.1C) offset -100C
        frame.putShort((short) (pack.maxModuleTemp + 1000));
        // minimum module temperature (0.1C) offset -100C
        frame.putShort((short) (pack.minModuleTemp + 1000));
        // pack number with maximum module temperature
        frame.putShort((short) pack.maxModuleTempNum);
        // pack number with minimum module temperature
        frame.putShort((short) pack.minModuleTempNum);

        LOG.debug("Sending max/min module C: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x4280
    private ByteBuffer sendChargeForbiddenMarks(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00004280);

        // flag if charging is forbidden
        frame.put(pack.chargeMOSState ? (byte) 0x00 : (byte) 0xAA);
        // flag if discharging is forbidden
        frame.put(pack.dischargeMOSState ? (byte) 0x00 : (byte) 0xAA);

        LOG.debug("Sending dis-/charge forbidden marks: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x42B0
    private ByteBuffer sendBatteyVoltageSOCSOH(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00004210);

        // Battery voltage (0.1V)
        frame.putShort((short) pack.packVoltage);

        // fill 4 0x00 bytes
        frame.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);

        // Battery SOC (1%)
        frame.put((byte) (pack.packSOC / 10));
        // Battery SOH (1%)
        frame.put((byte) (pack.packSOH / 10));

        LOG.debug("Sending battery status: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x7310
    private ByteBuffer sendHardwareSoftwareVersion(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00007310);

        if (pack.hardwareVersion != null && pack.hardwareVersion.length() > 2) {
            // hardware version prefix, e.g. B2.1
            switch (pack.hardwareVersion.charAt(0)) {
                case 'A':
                    frame.put((byte) 0x01);
                break;
                case 'B':
                    frame.put((byte) 0x02);
                break;
                default:
                    frame.put((byte) 0x00);
                break;

            }

            frame.put((byte) 0x00); // reserved

            final String cleanHw = pack.hardwareVersion.charAt(0) == 'A' || pack.hardwareVersion.charAt(0) == 'B' ? pack.hardwareVersion.substring(1) : pack.hardwareVersion;
            final String[] parts = cleanHw.split(".");

            if (parts.length > 0) {
                // hardware version
                frame.put((byte) Integer.parseInt(parts[0]));
            }

            if (parts.length > 1) {
                // hardware revision
                frame.put((byte) Integer.parseInt(parts[1]));
            }
        } else {
            // no hardware version - fill up the four hardware bytes with 0x00
            frame.put((byte) 0x00);
            frame.put((byte) 0x00);
            frame.put((byte) 0x00);
            frame.put((byte) 0x00);
        }

        // software version, e.g. 1.2
        if (pack.softwareVersion != null && pack.softwareVersion.length() > 0) {
            // software version, e.g. 1.2
            final String[] parts = pack.softwareVersion.split(".");

            for (final String part : parts) {
                frame.put((byte) Integer.parseInt(part));
            }
        }

        LOG.debug("Sending hard-/software version: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x7320
    private ByteBuffer sendBatterModuleInfo(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00007320);

        // battery module quantity
        frame.putShort((short) pack.numberOfCells);
        // battery modules in series
        frame.put((byte) pack.modulesInSeries);
        // cell quantity in battery module
        frame.put(pack.moduleNumberOfCells);
        // battery cabinet voltage level (1V)
        frame.putShort((short) pack.moduleVoltage);
        // battery cabinet AH (1AH)
        frame.putShort((short) pack.moduleRatedCapacityAh);

        LOG.debug("Sending battery module info: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x7330
    private List<ByteBuffer> sendManufacturer(final BatteryPack pack) throws IOException {
        final List<ByteBuffer> sendFrames = new ArrayList<>();
        ByteBuffer frame = prepareSendFrame(0x00007320);
        final byte[] bytes = pack.manufacturerCode.getBytes();

        if (bytes.length <= 8) {
            frame.put(bytes);

            LOG.debug("Sending manufacturer: {}", Port.printBuffer(frame));
            sendFrames.add(frame);
        } else {
            frame.put(bytes, 0, 8);

            LOG.debug("Sending manufacturer: {}", Port.printBuffer(frame));
            sendFrames.add(frame);

            frame = prepareSendFrame(0x00007330);
            frame.put(bytes, 8, bytes.length > 16 ? 8 : bytes.length - 8);

            LOG.debug("Sending manufacturer: {}", Port.printBuffer(frame));
            sendFrames.add(frame);
        }

        return sendFrames;
    }
}
