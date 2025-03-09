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
package com.airepublic.bmstoinverter.bms.mppsolar.modbus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.BitUtil;
import com.airepublic.bmstoinverter.protocol.modbus.ModbusUtil;
import com.airepublic.bmstoinverter.protocol.modbus.ModbusUtil.RegisterCode;

/**
 * The class to handle Modbus messages from a MPP Solar {@link BMS}.
 */
public class MppSolarBmsModbusProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(MppSolarBmsModbusProcessor.class);
    private final static int BATTERY_NO = 0;

    @Override
    protected void collectData(final Port port) {
        final int startRange = 0;

        try {

            // address 0x0010 - 0x0034
            sendMessage(port, RegisterCode.READ_HOLDING_REGISTERS, startRange + 0x0010, 36, getBmsId(), this::readBatteryStatus);
            // address 0x0036 - 0x003E
            sendMessage(port, RegisterCode.READ_HOLDING_REGISTERS, startRange + 0x0036, 9, getBmsId(), this::readAlarms);
            // address 0x0070 - 0x0076
            sendMessage(port, RegisterCode.READ_HOLDING_REGISTERS, startRange + 0x0070, 7, getBmsId(), this::readChargeDischargeLimits);
        } catch (final IOException e) {
            LOG.error("Error reading from modbus!", e);
        }
    }


    protected void sendMessage(final Port port, final RegisterCode functionCode, final int startAddress, final int numRegisters, final int unitId, final Consumer<ByteBuffer> handler) throws IOException {
        port.sendFrame(ModbusUtil.createRequestBuffer(functionCode, startAddress, numRegisters, unitId));
        handler.accept(port.receiveFrame());
    }


    // read battery status
    protected void readBatteryStatus(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        frame.getInt(); // unitId
        final BatteryPack pack = getBatteryPack(BATTERY_NO);

        // no of cells
        pack.numberOfCells = frame.getChar();

        // cell voltages (0.1V)
        for (int i = 0; i < 20; i++) {
            pack.cellVmV[i] = frame.getChar() * 100;
        }

        pack.numOfTempSensors = frame.getChar();

        for (int i = 0; i < 20; i++) {
            pack.cellTemperature[i] = (int) (frame.getChar() / 10f - 273.15f);
        }

        // curernt (0.1A)
        pack.packCurrent = frame.getChar();

        if (pack.packCurrent == 0) {
            pack.packCurrent = frame.getChar();
        } else {
            frame.getChar();
        }

        // voltage (0.1V)
        pack.packVoltage = frame.getChar();

        // SOC (1%)
        pack.packSOC = frame.getChar();

        // rated capacity (1mAh)
        pack.ratedCapacitymAh = frame.getChar();

    }


    // read alarms
    protected void readAlarms(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        frame.getInt(); // unitId
        final BatteryPack pack = getBatteryPack(BATTERY_NO);
        pack.alarms.clear();

        pack.modulesInSeries = frame.getChar();

        final char chargeAlarms = frame.getChar();

        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(chargeAlarms, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(chargeAlarms, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(chargeAlarms, 1) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(chargeAlarms, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        final char dischargeAlarms = frame.getChar();
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(dischargeAlarms, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(dischargeAlarms, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_LOW, BitUtil.bit(dischargeAlarms, 1) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(dischargeAlarms, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        final char chargeProtection1 = frame.getChar();

        if (BitUtil.bit(chargeProtection1, 15)) {
            pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(chargeProtection1, 12)) {
            pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(chargeProtection1, 11)) {
            pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(chargeProtection1, 10)) {
            pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(chargeProtection1, 9)) {
            pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(chargeProtection1, 8)) {
            pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(chargeProtection1, 7)) {
            pack.alarms.put(Alarm.CHARGE_VOLTAGE_LOW, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(chargeProtection1, 6)) {
            pack.alarms.put(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(chargeProtection1, 5)) {
            pack.alarms.put(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(chargeProtection1, 4)) {
            pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(chargeProtection1, 3)) {
            pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(chargeProtection1, 2)) {
            pack.alarms.put(Alarm.FAILURE_COMMUNICATION_INTERNAL, AlarmLevel.ALARM);
        }

        final char chargeProtection2 = frame.getChar();
        if (BitUtil.bit(chargeProtection2, 2)) {
            pack.alarms.put(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, AlarmLevel.ALARM);
        }

        final char dischargeProtection1 = frame.getChar();
        if (BitUtil.bit(dischargeProtection1, 15)) {
            pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection1, 14)) {
            pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection1, 13)) {
            pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_LOW, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection1, 12)) {
            pack.alarms.put(Alarm.DISCHARGE_VOLTAGE_LOW, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection1, 11)) {
            pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection1, 10)) {
            pack.alarms.put(Alarm.DISCHARGE_MODULE_TEMPERATURE_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection1, 4)) {
            pack.alarms.put(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection1, 3)) {
            pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection1, 2)) {
            pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, AlarmLevel.ALARM);
        }

        final char dischargeProtection2 = frame.getChar();
        if (BitUtil.bit(dischargeProtection2, 5)) {
            pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection2, 4)) {
            pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection2, 2)) {
            pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection2, 1)) {
            pack.alarms.put(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, AlarmLevel.ALARM);
        }
        if (BitUtil.bit(dischargeProtection2, 1)) {
            pack.alarms.put(Alarm.FAILURE_COMMUNICATION_INTERNAL, AlarmLevel.ALARM);
        }

        final char bmsState = frame.getChar();
        pack.moduleRatedCapacityAh = frame.getInt() / 1000;
    }


    // read charge cischarge limits
    protected void readChargeDischargeLimits(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        frame.getInt(); // unitId
        final BatteryPack pack = getBatteryPack(BATTERY_NO);

        pack.maxPackVoltageLimit = frame.getChar();
        pack.minPackVoltageLimit = frame.getChar();
        pack.maxPackChargeCurrent = frame.getChar();
        pack.maxPackDischargeCurrent = frame.getChar();
        final char chargeStatus = frame.getChar();
        pack.chargeDischargeStatus = BitUtil.bit(chargeStatus, 7) ? 1 : BitUtil.bit(chargeStatus, 6) ? 2 : 0;
        frame.getChar();
        pack.remainingCapacitymAh = frame.getInt();
    }
}
