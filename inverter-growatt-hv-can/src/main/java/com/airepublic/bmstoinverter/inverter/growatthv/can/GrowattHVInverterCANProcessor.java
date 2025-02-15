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
package com.airepublic.bmstoinverter.inverter.growatthv.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
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
 * The class to handle CAN messages for a Growatt HV {@link Inverter}.
 */
@ApplicationScoped
public class GrowattHVInverterCANProcessor extends Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(GrowattHVInverterCANProcessor.class);

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
        final List<ByteBuffer> sendFrames = new ArrayList<>();

        try {
            // 0x3110
            sendFrames.add(sendChargeDischargeLimits(aggregatedPack));
            // 0x3120
            sendFrames.add(sendAlarms(aggregatedPack));
            // 0x3130
            sendFrames.add(sendBatteryStatus(aggregatedPack));
            // 0x3140
            sendFrames.add(sendBatteryCapacity(aggregatedPack));
            // 0x3150
            sendFrames.add(sendWorkingParams(aggregatedPack));
            // 0x3160
            sendFrames.add(sendFaultAndVoltageNumbers(aggregatedPack));
            // 0x3170
            sendFrames.add(sendMinMaxCellTemperatures(aggregatedPack));
            // 0x3180
            sendFrames.add(sendBatteryCodeAndQuantity(aggregatedPack));
            // 0x3190
            sendFrames.add(sendMinMaxCellVoltages(aggregatedPack));
            // 0x3200
            sendFrames.add(sendManufacturerAndMaxCellVoltage(aggregatedPack));
        } catch (final Throwable e) {
            LOG.error("Error creating send frames: ", e);
        }

        return sendFrames;
    }


    protected ByteBuffer prepareSendFrame(final int frameId) {
        final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.BIG_ENDIAN);
        sendFrame.putInt(frameId);

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        return sendFrame;
    }


    // 0x3110
    private ByteBuffer sendChargeDischargeLimits(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00003110);

        // Charge cutoff voltage (0.1V)
        frame.putChar((char) pack.maxPackVoltageLimit);
        // Max charge current (0.1A) offset 0A
        frame.putChar((char) pack.maxPackChargeCurrent);
        // Max discharge current (0.1A) offset -3000A
        frame.putChar((char) (pack.maxPackDischargeCurrent * -1));

        // Battery status
        short status = 0x0000;
        switch (pack.chargeDischargeStatus) {
            case 0: {
                // standby/idle
                status = BitUtil.setBit(status, 0, false); // Byte 7 bit 0
                status = BitUtil.setBit(status, 1, true); // Byte 7 bit 1
            }
            break;
            case 1: {
                // discharging
                status = BitUtil.setBit(status, 0, true); // Byte 7 bit 0
                status = BitUtil.setBit(status, 1, false); // Byte 7 bit 1
            }
            break;
            case 2: {
                // charging
                status = BitUtil.setBit(status, 0, true); // Byte 7 bit 0
                status = BitUtil.setBit(status, 1, true); // Byte 7 bit 1
            }
            break;
            case 3: {
                // battery sleeping state
                status = BitUtil.setBit(status, 4, true); // Byte 7 bit 4
            }
            break;
        }

        // fault flag
        status = BitUtil.setBit(status, 2, false); // Byte 7 bit 2
        // cell balancing state
        status = BitUtil.setBit(status, 3, pack.cellBalanceActive); // Byte 7 bit 3

        LOG.debug("Sending max/min charge and discharge voltage and current limits: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x3120
    private ByteBuffer sendAlarms(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00003120);

        // Protection
        int protection = 0x00000000;
        protection = BitUtil.setBit(protection, 1, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 2, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 3, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 4, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 5, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 6, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 7, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 8, pack.getAlarmLevel(Alarm.DISCHARGE_VOLTAGE_LOW) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 9, pack.getAlarmLevel(Alarm.CHARGE_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 10, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 11, pack.getAlarmLevel(Alarm.FAILURE_OTHER) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 12, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 13, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 14, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 15, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 16, pack.getAlarmLevel(Alarm.SOC_LOW) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 17, pack.getAlarmLevel(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 18, pack.getAlarmLevel(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        protection = BitUtil.setBit(protection, 19, pack.getAlarmLevel(Alarm.ENCASING_TEMPERATURE_HIGH) == AlarmLevel.ALARM);

        frame.putInt(protection);

        // Alarm
        int alarm = 0x00000000;
        alarm = BitUtil.setBit(alarm, 0, pack.getAlarmLevel(Alarm.FAILURE_COMMUNICATION_INTERNAL) == AlarmLevel.WARNING);

        alarm = BitUtil.setBit(alarm, 2, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.WARNING);

        alarm = BitUtil.setBit(alarm, 4, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 5, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_LOW) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 6, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 7, pack.getAlarmLevel(Alarm.DISCHARGE_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 8, pack.getAlarmLevel(Alarm.DISCHARGE_VOLTAGE_LOW) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 9, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 10, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 11, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 12, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 13, pack.getAlarmLevel(Alarm.CHARGE_VOLTAGE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 14, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 15, pack.getAlarmLevel(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.WARNING);

        alarm = BitUtil.setBit(alarm, 17, pack.getAlarmLevel(Alarm.SOC_LOW) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 18, pack.getAlarmLevel(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 19, pack.getAlarmLevel(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 20, pack.getAlarmLevel(Alarm.ENCASING_TEMPERATURE_HIGH) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 21, pack.getAlarmLevel(Alarm.FAILURE_COMMUNICATION_EXTERNAL) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 22, pack.getAlarmLevel(Alarm.FAILURE_COMMUNICATION_INTERNAL) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 23, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.WARNING);
        alarm = BitUtil.setBit(alarm, 24, pack.getAlarmLevel(Alarm.SOC_LOW) == AlarmLevel.WARNING);

        frame.putInt(alarm);

        LOG.debug("Sending alarms: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x3130
    private ByteBuffer sendBatteryStatus(final BatteryPack pack) throws IOException {
        final ByteBuffer frame = prepareSendFrame(0x00003130);

        // Battery voltage (0.1V)
        frame.putChar((char) pack.packVoltage);
        // Battery current (0.1A) offset -3000A
        frame.putShort((short) pack.packCurrent);
        // second level temperature (0.1 Celcius) offset -100C
        frame.putShort((short) pack.tempAverage);
        // Battery SOC (1%)
        frame.put((byte) (pack.packSOC / 10));
        // Battery SOH (1%)
        frame.put((byte) (pack.packSOH / 10));

        LOG.debug("Sending battery status: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x3140
    private ByteBuffer sendBatteryCapacity(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x00003140);

        // Current battery energy (10mAH)
        frame.putChar((char) (pack.remainingCapacitymAh / 10));
        // Rated battery energy (10mAH)
        frame.putChar((char) (pack.ratedCapacitymAh / 10));
        // manufacturer code
        frame.putChar(pack.manufacturerCode != null && pack.manufacturerCode.length() > 0 ? pack.manufacturerCode.charAt(0) : 0);
        // cycle count
        frame.putChar((char) pack.bmsCycles);

        LOG.debug("Sending battery capacity: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x3150
    private ByteBuffer sendWorkingParams(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x00003150);

        // max discharge voltage (0.1V)
        frame.putChar((char) pack.minPackVoltageLimit);
        // case temperature (0.1C)
        frame.putChar((char) pack.tempAverage);
        // number of cells
        frame.putChar((char) getEnergyStorage().getBatteryPacks().stream().mapToInt(p -> p.numberOfCells).sum());
        // number of packs
        frame.putChar((char) getEnergyStorage().getBatteryPacks().size());

        LOG.debug("Sending working params: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x3160
    private ByteBuffer sendFaultAndVoltageNumbers(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x00003160);
        short faultFlags = 0x0000;

        faultFlags = BitUtil.setBit(faultFlags, 0, pack.getAlarmLevel(Alarm.FAILURE_SENSOR_PACK_VOLTAGE) != AlarmLevel.NONE);
        faultFlags = BitUtil.setBit(faultFlags, 1, pack.getAlarmLevel(Alarm.FAILURE_SENSOR_CELL_TEMPERATURE) != AlarmLevel.NONE);
        faultFlags = BitUtil.setBit(faultFlags, 2, pack.getAlarmLevel(Alarm.FAILURE_COMMUNICATION_INTERNAL) != AlarmLevel.NONE);

        // fault flags
        frame.putShort(faultFlags);

        int minCellmV = Integer.MAX_VALUE;
        int maxCellmV = Integer.MIN_VALUE;
        byte maxVBatteryPackNumber = 0;
        byte minVBatteryPackNumber = 0;

        for (int i = 0; i < getEnergyStorage().getBatteryPacks().size(); i++) {
            final BatteryPack p = getEnergyStorage().getBatteryPacks().get(i);
            final int packMaxCellmV = Arrays.stream(p.cellVmV).max().orElse(Integer.MIN_VALUE);
            final int packMinCellmV = Arrays.stream(p.cellVmV).min().orElse(Integer.MAX_VALUE);

            if (packMinCellmV < minCellmV) {
                minCellmV = packMinCellmV;
                minVBatteryPackNumber = (byte) i;
            }

            if (packMaxCellmV > maxCellmV) {
                maxCellmV = packMaxCellmV;
                maxVBatteryPackNumber = (byte) i;
            }
        }

        // number of module with max cell voltage
        frame.put(maxVBatteryPackNumber);
        // cell number with max voltage
        frame.put((byte) pack.maxCellVNum);
        // number of module with min cell voltage
        frame.put(minVBatteryPackNumber);
        // cell number with min voltage
        frame.put((byte) pack.minCellVNum);
        // min cell temperature
        frame.put((byte) pack.tempMin);

        LOG.debug("Sending fault and voltage numbers: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x3170
    private ByteBuffer sendMinMaxCellTemperatures(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x00003170);

        int minTemp = Integer.MAX_VALUE;
        int maxTemp = Integer.MIN_VALUE;
        byte minCellNo = 0;
        byte maxCellNo = 0;
        byte minTempPackNumber = 0;
        byte maxTempPackNumber = 0;

        for (int i = 0; i < getEnergyStorage().getBatteryPacks().size(); i++) {
            final BatteryPack p = getEnergyStorage().getBatteryPacks().get(i);

            if (p.tempMin < minTemp) {
                minTemp = p.tempMin;
                minCellNo = (byte) p.tempMinCellNum;
                minTempPackNumber = (byte) i;
            }

            if (p.tempMax > maxTemp) {
                maxTemp = p.tempMax;
                maxCellNo = (byte) p.tempMaxCellNum;
                maxTempPackNumber = (byte) i;
            }
        }

        frame.put(maxTempPackNumber);
        frame.put(maxCellNo);
        frame.put(minTempPackNumber);
        frame.put(minCellNo);

        LOG.debug("Sending min/max cell temperaturs: {}", Port.printBuffer(frame));

        return frame;
    }


    // 0x3180
    private ByteBuffer sendBatteryCodeAndQuantity(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x00003180);

        // Manufacturer code
        frame.putChar(pack.manufacturerCode != null && pack.manufacturerCode.length() > 0 ? pack.manufacturerCode.charAt(0) : 0);
        // Number of packs in parallel
        frame.putChar((char) getEnergyStorage().getBatteryPacks().size());
        // Total number of cells
        frame.putChar((char) getEnergyStorage().getBatteryPacks().stream().mapToInt(p -> p.numberOfCells).sum());

        short bics = 0x0000;
        bics = BitUtil.setBit(bics, 0, true); // treat all packs as one aggregated pack
        frame.putShort(bics);

        LOG.debug("Sending battery code and quantity: {}", Port.printBuffer(frame));

        return frame;
    }


    // 0x3190
    private ByteBuffer sendMinMaxCellVoltages(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x00003190);

        // Battery status
        frame.put(getChargeStates(pack));
        // Max cell voltage (1mV)
        frame.putChar((char) pack.maxCellmV);
        // Min cell voltage (1mV)
        frame.putChar((char) pack.minCellmV);

        LOG.debug("Sending cell min/max voltages: {}", Port.printBuffer(frame));
        return frame;
    }


    // 0x3200
    private ByteBuffer sendManufacturerAndMaxCellVoltage(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x00003200);
        frame.put((byte) (pack.manufacturerCode != null && pack.manufacturerCode.length() > 0 ? pack.manufacturerCode.charAt(0) : 0));
        frame.put((byte) (pack.manufacturerCode != null && pack.manufacturerCode.length() > 0 ? pack.manufacturerCode.charAt(1) : 0));
        frame.put((byte) (pack.hardwareVersion.equals("A") ? 1 : 2));

        // reserved
        frame.put((byte) 0);
        frame.put((byte) 0);
        frame.put((byte) 0);

        frame.putChar((char) pack.maxCellVoltageLimit);

        LOG.debug("Sending manufacturer and hardware version: {}", Port.printBuffer(frame));
        return frame;
    }


    /**
     * See documentation Table for 0x3190.
     *
     * @return the charge state bits
     */
    private byte getChargeStates(final BatteryPack pack) {
        byte value = 0;
        value = BitUtil.setBit(value, 7, pack.chargeMOSState);
        value = BitUtil.setBit(value, 6, pack.dischargeMOSState);
        value = BitUtil.setBit(value, 5, false);
        value = BitUtil.setBit(value, 4, false);
        value = BitUtil.setBit(value, 3, false);
        value = BitUtil.setBit(value, 2, false);
        switch (pack.type) {
            case 0: // lithium iron phosphate
                value = BitUtil.setBit(value, 1, false);
                value = BitUtil.setBit(value, 0, false);
            break;
            case 1: // ternary lithium
                value = BitUtil.setBit(value, 1, false);
                value = BitUtil.setBit(value, 0, true);
            break;
            case 2: // lithium titanate
                value = BitUtil.setBit(value, 1, true);
                value = BitUtil.setBit(value, 0, false);
            break;
        }

        return value;
    }

}
