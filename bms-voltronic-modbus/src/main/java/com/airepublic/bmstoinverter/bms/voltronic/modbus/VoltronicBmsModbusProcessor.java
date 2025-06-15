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
package com.airepublic.bmstoinverter.bms.voltronic.modbus;

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
 * The class to handle Modbus messages from a JK {@link BMS}.
 */
public class VoltronicBmsModbusProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(VoltronicBmsModbusProcessor.class);

    @Override
    protected void collectData(final Port port) {
        try {
            sendMessage(port, RegisterCode.READ_HOLDING_REGISTERS, 0x106C, 5, getBmsId(), this::readBatteryStatus);
            sendMessage(port, RegisterCode.READ_HOLDING_REGISTERS, 0x1290, 5, getBmsId(), this::readVoltCurrentTemp);
            sendMessage(port, RegisterCode.READ_HOLDING_REGISTERS, 0x12A0, 4, getBmsId(), this::readAlarms);
            sendMessage(port, RegisterCode.READ_HOLDING_REGISTERS, 0x12A6, 4, getBmsId(), this::readSOC);
        } catch (final IOException e) {
            LOG.error("Error reading from modbus!", e);
        }
    }


    protected void sendMessage(final Port port, final RegisterCode functionCode, final int startAddress, final int numRegisters, final int unitId, final Consumer<ByteBuffer> handler) throws IOException {
        port.sendFrame(ModbusUtil.createRequestBuffer(functionCode, startAddress, numRegisters, unitId));
        handler.accept(port.receiveFrame());
    }


    protected void readBatteryStatus(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        final int unitId = frame.getInt();
        final BatteryPack pack = getBatteryPack(unitId);

        pack.numberOfCells = frame.getInt();
        pack.chargeMOSState = frame.getInt() == 1;
        pack.dischargeMOSState = frame.getInt() == 1;
        pack.cellBalanceActive = frame.getInt() == 1;
        pack.ratedCapacitymAh = frame.getInt();
    }


    private void readVoltCurrentTemp(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        final int unitId = frame.getInt();
        final BatteryPack pack = getBatteryPack(unitId);

        // voltage in mV
        pack.packVoltage = frame.getInt() / 100;
        // pack Watts in mW
        frame.getInt();
        // current in mA
        pack.packCurrent = frame.getInt() / 100;
        // temp in 0.1C
        pack.tempAverage = (frame.getShort() + frame.getShort()) / 2;
    }


    protected void readSOC(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        final int unitId = frame.getInt();
        final BatteryPack pack = getBatteryPack(unitId);

        // SOC in 1%
        pack.packSOC = (frame.getInt() & 0xF0) >> 8;
    }


    private void readAlarms(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        final int unitId = frame.getInt();
        final BatteryPack pack = getBatteryPack(unitId);

        final int bits = frame.getInt();

        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(bits, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_SENSOR_PACK_CURRENT, BitUtil.bit(bits, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(bits, 4) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(bits, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(bits, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(bits, 8) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(bits, 9) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_COMMUNICATION_INTERNAL, BitUtil.bit(bits, 10) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(bits, 11) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(bits, 12) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(bits, 13) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(bits, 15) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_CHARGE_BREAKER, BitUtil.bit(bits, 16) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_DISCHARGE_BREAKER, BitUtil.bit(bits, 17) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_TEMPERATURE_HIGH, BitUtil.bit(bits, 21) ? AlarmLevel.ALARM : AlarmLevel.NONE);
    }
}
