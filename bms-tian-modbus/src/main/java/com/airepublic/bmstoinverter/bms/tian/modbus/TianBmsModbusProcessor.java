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
package com.airepublic.bmstoinverter.bms.tian.modbus;

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
import com.airepublic.bmstoinverter.core.util.Util;
import com.airepublic.bmstoinverter.protocol.modbus.ModbusUtil;
import com.airepublic.bmstoinverter.protocol.modbus.ModbusUtil.RegisterCode;

/**
 * The class to handle Modbus messages from a Tian {@link BMS}.
 */
public class TianBmsModbusProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(TianBmsModbusProcessor.class);

    @Override
    protected void collectData(final Port port) {
        try {
            sendMessage(port, RegisterCode.READ_INPUT_REGISTERS, 0x1000, 0x17, getBmsId(), this::readBatteryData);
        } catch (final IOException e) {
            LOG.error("Error reading from modbus!", e);
        }
    }


    protected void sendMessage(final Port port, final RegisterCode functionCode, final int startAddress, final int numRegisters, final int unitId, final Consumer<ByteBuffer> handler) throws IOException {
        port.sendFrame(ModbusUtil.createRequestBuffer(functionCode, startAddress, numRegisters, unitId));
        handler.accept(port.receiveFrame());
    }


    protected void readBatteryData(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        final int unitId = frame.getInt();
        final BatteryPack pack = getBatteryPack(unitId);

        // pack voltage 0.01V
        pack.packVoltage = frame.getInt() / 10;
        // pack current 0.01A
        pack.packCurrent = ((short) frame.getInt()) / 10;
        // remaining capacity 0.01AH
        pack.remainingCapacitymAh = frame.getInt() * 10;
        // average temperature 0.1C
        pack.tempAverage = frame.getInt();
        // environment temperature 0.1C
        frame.getInt();
        final int warningFlag = frame.getInt();
        final int protectionFlag = frame.getInt();
        final int faultStatus = frame.getInt();

        readAlarms(pack, warningFlag, protectionFlag, faultStatus);

        // SOC 0.1%
        pack.packSOC = frame.getInt();
        // SOH 0.1%
        pack.packSOH = frame.getInt();
        // rated capacity 0.01Ah
        pack.ratedCapacitymAh = frame.getInt() * 10;
        pack.bmsCycles = frame.getInt();
        // max charge current 0.01A
        pack.maxPackChargeCurrent = frame.getInt() / 10;
        // max cell voltage 1mV
        pack.maxCellmV = frame.getInt();
        // min cell voltage 1mV
        pack.minCellmV = frame.getInt();
    }


    private void readAlarms(final BatteryPack pack, final int warningFlag, final int protectionFlag, final int faultStatus) {
        // warnings
        pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, Util.bit(warningFlag, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, Util.bit(warningFlag, 1) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, Util.bit(warningFlag, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, Util.bit(warningFlag, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, Util.bit(warningFlag, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, Util.bit(warningFlag, 5) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        pack.alarms.put(Alarm.PACK_TEMPERATURE_HIGH, Util.bit(warningFlag, 6) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_TEMPERATURE_LOW, Util.bit(warningFlag, 7) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.ENCASING_TEMPERATURE_HIGH, Util.bit(warningFlag, 8) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        pack.alarms.put(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, Util.bit(warningFlag, 10) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, Util.bit(warningFlag, 11) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // alarms
        pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, Util.bit(protectionFlag, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, Util.bit(protectionFlag, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, Util.bit(protectionFlag, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, Util.bit(protectionFlag, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, Util.bit(protectionFlag, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, Util.bit(protectionFlag, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, Util.bit(protectionFlag, 7) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, Util.bit(protectionFlag, 8) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_LOW, Util.bit(protectionFlag, 9) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // fault
        pack.alarms.put(Alarm.FAILURE_COMMUNICATION_INTERNAL, Util.bit(faultStatus, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_SENSOR_PACK_TEMPERATURE, Util.bit(faultStatus, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        pack.chargeDischargeStatus = Util.bit(faultStatus, 8) ? 1 : Util.bit(faultStatus, 9) ? 2 : 0;
        pack.chargeMOSState = Util.bit(faultStatus, 9);
        pack.dischargeMOSState = Util.bit(faultStatus, 10);

    }
}
