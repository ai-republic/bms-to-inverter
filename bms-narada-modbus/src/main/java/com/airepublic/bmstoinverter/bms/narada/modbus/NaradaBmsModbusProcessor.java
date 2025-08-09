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
package com.airepublic.bmstoinverter.bms.narada.modbus;

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
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.util.BitUtil;
import com.airepublic.bmstoinverter.protocol.modbus.ModbusUtil;
import com.airepublic.bmstoinverter.protocol.modbus.ModbusUtil.RegisterCode;

import javax.inject.Inject;

/**
 * The class to handle Modbus messages from a Narada {@link BMS}.
 */
public class NaradaBmsModbusProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(NaradaBmsModbusProcessor.class);
    @Inject
    private EnergyStorage energyStorage;

    @Override
    protected void collectData(final Port port) {
        try {
            sendMessage(port, RegisterCode.READ_INPUT_REGISTERS, 0x0FFF, 50, getBmsId(), this::readBatteryStatus);
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

        pack.packVoltage = frame.getShort() / 10; // 0.01V
        pack.packCurrent = frame.getShort() - 10000; // 0.1A
        pack.remainingCapacitymAh = frame.getShort() * 100; // 0.1A
        pack.tempAverage = frame.getShort() - 400; // 0.1C - 400
        frame.getShort(); // env temp 0.1C - 400
        final int warningFlag = frame.getChar();
        final int protectionFlag = frame.getShort();
        final int statusFlag = frame.getShort();
        pack.packSOC = frame.getShort() / 10; // 0.01%
        pack.bmsCycles = frame.getShort();
        pack.packSOH = frame.getShort() / 10; // 0.01%
        frame.getShort(); // history discharge capacity 10A
        pack.numberOfCells = frame.getShort();

        // determine min/max cell voltages
        for (int i = 0; i < 16; i++) {
            if (i < pack.numberOfCells) {
                pack.cellVmV[i] = frame.getShort();

                if (pack.cellVmV[i] > pack.maxCellmV) {
                    pack.maxCellmV = pack.cellVmV[i];
                    pack.maxCellVNum = i;
                } else if (pack.cellVmV[i] < pack.minCellmV) {
                    pack.minCellmV = pack.cellVmV[i];
                    pack.minCellVNum = i;
                }
            } else {
                frame.getShort();
            }
        }

        // calculate cell voltage difference
        pack.cellDiffmV = pack.maxCellmV - pack.minCellmV;

        pack.numOfTempSensors = frame.getShort();

        for (int i = 0; i < 16; i++) {
            if (i < pack.numOfTempSensors) {
                pack.cellTemperature[i] = frame.getShort() - 400; // 0.1C - 400
            } else {
                frame.getShort();
            }
        }

        pack.ratedCapacitymAh = frame.getShort() * 100; // 0.1A
        frame.getShort(); // remaining charge time 1min
        frame.getShort(); // remaining discharge time 1min
        final int cellUVStatus = frame.getShort(); // Cell undervoltage state

        readAlarms(pack, warningFlag, protectionFlag, statusFlag, cellUVStatus);
    }


    private void readAlarms(final BatteryPack pack, final int warningFlag, final int protectionFlag, final int faultStatus, final int cellUVStatus) {
        // warnings
        pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(warningFlag, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(warningFlag, 1) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(warningFlag, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(warningFlag, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(warningFlag, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(warningFlag, 5) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_TEMPERATURE_HIGH, BitUtil.bit(warningFlag, 6) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_TEMPERATURE_LOW, BitUtil.bit(warningFlag, 7) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.ENCASING_TEMPERATURE_HIGH, BitUtil.bit(warningFlag, 8) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        pack.alarms.put(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(warningFlag, 10) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.SOC_LOW, BitUtil.bit(warningFlag, 11) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, BitUtil.bit(warningFlag, 12) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // alarms
        pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(protectionFlag, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(protectionFlag, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(protectionFlag, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(protectionFlag, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, BitUtil.bit(protectionFlag, 4) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(protectionFlag, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(protectionFlag, 7) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(protectionFlag, 8) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_LOW, BitUtil.bit(protectionFlag, 9) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // fault
        pack.alarms.put(Alarm.FAILURE_COMMUNICATION_INTERNAL, BitUtil.bit(faultStatus, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_SENSOR_PACK_TEMPERATURE, BitUtil.bit(faultStatus, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        pack.chargeDischargeStatus = BitUtil.bit(faultStatus, 8) ? 1 : BitUtil.bit(faultStatus, 9) ? 2 : 0;
        pack.chargeMOSState = BitUtil.bit(faultStatus, 9);
        pack.dischargeMOSState = BitUtil.bit(faultStatus, 10);

    }
}
