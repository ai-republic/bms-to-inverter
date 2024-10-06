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
package com.airepublic.bmstoinverter.bms.jk.modbus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.protocol.modbus.ModbusUtil;
import com.airepublic.bmstoinverter.protocol.modbus.ModbusUtil.RegisterCode;

import jakarta.inject.Inject;

/**
 * The class to handle Modbus messages from a JK {@link BMS}.
 */
public class JKBmsModbusProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(JKBmsModbusProcessor.class);
    @Inject
    private EnergyStorage energyStorage;

    @Override
    protected void collectData(final Port port) {
        try {
            sendMessage(port, RegisterCode.READ_HOLDING_REGISTERS, 0x106C, 5, getBmsId(), this::readBatteryStatus);
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
        final BatteryPack pack = energyStorage.getBatteryPack(unitId);

        pack.numberOfCells = frame.getInt();
        pack.chargeMOSState = frame.getInt() == 1;
        pack.dischargeMOSState = frame.getInt() == 1;
        pack.cellBalanceActive = frame.getInt() == 1;
        pack.ratedCapacitymAh = frame.getInt();
    }


    protected void readSOC(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        final int unitId = frame.getInt();
        final BatteryPack pack = energyStorage.getBatteryPack(unitId);

        pack.packSOC = (frame.getInt() & 0xF0) >> 8;
    }


    private void readAlarms(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        final int unitId = frame.getInt();
        final BatteryPack pack = energyStorage.getBatteryPack(unitId - 1);

        final BitSet bits39014 = BitSet.valueOf(new byte[] { frame.get(), frame.get() });
        final BitSet bits39015 = BitSet.valueOf(new byte[] { frame.get(), frame.get() });
        final BitSet bits39016 = BitSet.valueOf(new byte[] { frame.get(), frame.get() });

        pack.alarms.put(Alarm.FAILURE_COMMUNICATION_INTERNAL, bits39014.get(13) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, bits39014.get(14) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, bits39014.get(15) || bits39016.get(0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, bits39015.get(0) || bits39016.get(15) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_CURRENT_HIGH, bits39016.get(12) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, bits39015.get(1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_TEMPERATURE_HIGH, bits39015.get(6) || bits39015.get(7) || bits39015.get(8) || bits39016.get(13) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_TEMPERATURE_LOW, bits39016.get(14) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH, bits39015.get(15) ? AlarmLevel.ALARM : AlarmLevel.NONE);

    }
}
