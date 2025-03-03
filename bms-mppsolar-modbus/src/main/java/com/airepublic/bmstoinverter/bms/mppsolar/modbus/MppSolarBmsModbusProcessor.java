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
            sendMessage(port, RegisterCode.READ_HOLDING_REGISTERS, startRange + 0x0010, 36, getBmsId(), this::readBatteryStatus);
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

        pack.packCurrent = frame.getShort() / 10;
        pack.ratedCapacitymAh = frame.getChar() * 10;
        pack.bmsCycles = frame.getChar();
        pack.packSOC = frame.getChar() / 10;
        pack.packSOH = frame.getChar() / 10;
        pack.numOfTempSensors = frame.getChar();

        char alarms = frame.getChar();
        byte highByte = (byte) (alarms >> 8);
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(highByte, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(highByte, 4) || pack.alarms.containsKey(Alarm.FAILURE_OTHER) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_CHARGE_BREAKER, BitUtil.bit(highByte, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_DISCHARGE_BREAKER, BitUtil.bit(highByte, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(highByte, 7) || pack.alarms.containsKey(Alarm.FAILURE_OTHER) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        byte lowByte = (byte) (alarms & 0xFF);
        pack.alarms.put(Alarm.FAILURE_CLOCK_MODULE, BitUtil.bit(lowByte, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(lowByte, 1) || pack.alarms.containsKey(Alarm.FAILURE_OTHER) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(lowByte, 2) || pack.alarms.containsKey(Alarm.FAILURE_OTHER) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(lowByte, 3) || pack.alarms.containsKey(Alarm.FAILURE_OTHER) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(lowByte, 4) || pack.alarms.containsKey(Alarm.FAILURE_OTHER) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        alarms = frame.getChar();
        highByte = (byte) (alarms >> 8);
        pack.alarms.put(Alarm.FAILURE_SENSOR_DISCHARGE_MODULE_TEMPERATURE, BitUtil.bit(highByte, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_SENSOR_DISCHARGE_MODULE_TEMPERATURE, BitUtil.bit(highByte, 1) || pack.alarms.containsKey(Alarm.FAILURE_SENSOR_DISCHARGE_MODULE_TEMPERATURE) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(highByte, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(lowByte, 3) || pack.alarms.containsKey(Alarm.FAILURE_OTHER) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(lowByte, 4) || pack.alarms.containsKey(Alarm.FAILURE_OTHER) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(lowByte, 5) || pack.alarms.containsKey(Alarm.FAILURE_OTHER) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        lowByte = (byte) (alarms & 0xFF);
        pack.chargeDischargeStatus = BitUtil.bit(lowByte, 0) ? 1 : BitUtil.bit(lowByte, 1) ? 2 : 0;
        pack.alarms.put(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, BitUtil.bit(lowByte, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(lowByte, 4) || pack.alarms.containsKey(Alarm.PACK_VOLTAGE_HIGH) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(lowByte, 5) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(lowByte, 6) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(lowByte, 7) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        alarms = frame.getChar();
        highByte = (byte) (alarms >> 8);
        pack.alarms.put(Alarm.PACK_TEMPERATURE_LOW, BitUtil.bit(highByte, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_TEMPERATURE_HIGH, BitUtil.bit(highByte, 1) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        lowByte = (byte) (alarms & 0xFF);
        if (BitUtil.bit(lowByte, 0)) {
            pack.forceCharge = true;
        }

        if (BitUtil.bit(lowByte, 1)) {
            pack.forceCharge = false;
        }

        if (BitUtil.bit(lowByte, 2)) {
            pack.forceDischarge = true;
        }

        if (BitUtil.bit(lowByte, 3)) {
            pack.forceDischarge = false;
        }

        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(lowByte, 5) || pack.alarms.containsKey(Alarm.CHARGE_TEMPERATURE_HIGH) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(lowByte, 6) || pack.alarms.containsKey(Alarm.CHARGE_TEMPERATURE_LOW) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(lowByte, 7) || pack.alarms.containsKey(Alarm.CHARGE_TEMPERATURE_LOW) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        alarms = frame.getChar();
        alarms = frame.getChar();
        highByte = (byte) (alarms >> 8);
        pack.alarms.put(Alarm.ENCASING_TEMPERATURE_HIGH, BitUtil.bit(highByte, 0) || pack.alarms.containsKey(Alarm.ENCASING_TEMPERATURE_HIGH) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.ENCASING_TEMPERATURE_LOW, BitUtil.bit(highByte, 1) || pack.alarms.containsKey(Alarm.ENCASING_TEMPERATURE_LOW) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(highByte, 2) || pack.alarms.containsKey(Alarm.DISCHARGE_TEMPERATURE_HIGH) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.SOC_LOW, BitUtil.bit(highByte, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(highByte, 4) || pack.alarms.containsKey(Alarm.PACK_VOLTAGE_HIGH) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_TEMPERATURE_HIGH, BitUtil.bit(highByte, 5) || pack.alarms.containsKey(Alarm.PACK_TEMPERATURE_HIGH) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_LOW, BitUtil.bit(highByte, 5) || pack.alarms.containsKey(Alarm.DISCHARGE_TEMPERATURE_LOW) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        lowByte = (byte) (alarms & 0xFF);
        pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(lowByte, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(lowByte, 1) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(lowByte, 2) || pack.alarms.containsKey(Alarm.PACK_VOLTAGE_HIGH) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(lowByte, 3) || pack.alarms.containsKey(Alarm.PACK_VOLTAGE_LOW) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.SOC_HIGH, BitUtil.bit(lowByte, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(lowByte, 6) || pack.alarms.containsKey(Alarm.CHARGE_TEMPERATURE_HIGH) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(lowByte, 7) || pack.alarms.containsKey(Alarm.CHARGE_TEMPERATURE_LOW) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        final char protection = frame.getChar();
        highByte = (byte) (protection >> 8);
        pack.alarms.put(Alarm.SOC_HIGH, BitUtil.bit(highByte, 0) || pack.alarms.containsKey(Alarm.SOC_HIGH) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        lowByte = (byte) (protection & 0xFF);
        pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(lowByte, 0) || pack.alarms.containsKey(Alarm.CELL_VOLTAGE_HIGH) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(lowByte, 1) || pack.alarms.containsKey(Alarm.PACK_VOLTAGE_HIGH) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(lowByte, 2) || pack.alarms.containsKey(Alarm.CELL_VOLTAGE_LOW) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(lowByte, 3) || pack.alarms.containsKey(Alarm.PACK_VOLTAGE_LOW) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(lowByte, 4) || pack.alarms.containsKey(Alarm.CHARGE_CURRENT_HIGH) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(lowByte, 5) || pack.alarms.containsKey(Alarm.CHARGE_CURRENT_HIGH) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(lowByte, 6) || pack.alarms.containsKey(Alarm.DISCHARGE_CURRENT_HIGH) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(lowByte, 7) || pack.alarms.containsKey(Alarm.DISCHARGE_CURRENT_HIGH) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        frame.getInt();
        pack.moduleRatedCapacityAh = frame.getInt();
    }

}
