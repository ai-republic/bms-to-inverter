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
package com.airepublic.bmstoinverter.bms.zte.modbus;

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
 * The class to handle Modbus messages from a Huawei {@link BMS}.
 */
public class ZTEBmsModbusProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(ZTEBmsModbusProcessor.class);

    @Override
    protected void collectData(final Port port) {
        try {
            sendMessage(port, RegisterCode.READ_INPUT_REGISTERS, 0x0080, 31, 230 + getBmsId(), this::readBatteryStatus);
            sendMessage(port, RegisterCode.READ_DESCRETE_INPUTS, 0x0668, 17, 230 + getBmsId(), this::readAlarms);
            sendMessage(port, RegisterCode.READ_INPUT_REGISTERS, 0x0B40, 10, 230 + getBmsId(), this::readFactoryInfo);
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

        pack.packVoltage = frame.getChar() / 100; // 0.01V
        frame.getChar(); // busbar voltage
        pack.tempAverage = frame.getShort() / 10; // 0.1C
        pack.packCurrent = frame.getShort() / 100; // 0.01A

        for (int cellNo = 0; cellNo < 15; cellNo++) {
            pack.cellVmV[cellNo] = frame.getChar(); // 0.001V
        }

        // ensure we have space for 4 temperature sensors
        pack.numOfTempSensors = 4;

        pack.cellTemperature[0] = frame.getShort() / 10; // 0.1C
        pack.cellTemperature[1] = frame.getShort() / 10; // 0.1C
        pack.cellTemperature[2] = frame.getShort() / 10; // 0.1C
        pack.cellTemperature[3] = frame.getShort() / 10; // 0.1C

        frame.getShort(); // busbar current

        pack.packSOC = frame.getChar() * 10; // 1%
        pack.packSOH = frame.getChar() * 10; // 1%

        pack.bmsCycles = frame.getChar();

        final char status = frame.getChar();

        switch (status) {
            case 0:
                pack.chargeDischargeStatus = 1; // charge
            break;
            case 1:
                pack.chargeDischargeStatus = 2; // discharge
            break;
            case 2:
                pack.chargeDischargeStatus = 0; // idle
            break;
            case 3:
                pack.chargeDischargeStatus = 3; // sleep
            break;
            default:
                pack.chargeDischargeStatus = 0; // unknown
        }
    }


    private void readAlarms(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        final int unitId = frame.getInt();
        final BatteryPack pack = getBatteryPack(unitId);

        // alarm status 1
        char status = frame.getChar();

        pack.alarms.put(Alarm.ENCASING_TEMPERATURE_HIGH, BitUtil.bit(status, 15) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(status, 14) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(status, 13) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(status, 12) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        // bit 11 reserved
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(status, 10) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(status, 9) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(status, 8) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // reset cell voltage high alarm, will be set below if any cell is over voltage
        pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, AlarmLevel.NONE);

        for (int i = 7; i > 1; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.WARNING);
                break;
            }
        }

        pack.alarms.put(Alarm.ENCASING_TEMPERATURE_LOW, BitUtil.bit(status, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // alarm status 2
        status = frame.getChar();

        for (int i = 15; i > 8; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.WARNING);
                break;
            }
        }

        for (int i = 1; i > 0; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.WARNING);
                break;
            }
        }

        for (int i = 7; i > 2; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.ALARM);
                break;
            }
        }

        // alarm status 3
        status = frame.getChar();

        for (int i = 15; i > 8; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.ALARM);
                break;
            }
        }

        for (int i = 7; i > 1; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, AlarmLevel.WARNING);
                break;
            }
        }

        if (BitUtil.bit(status, 0)) {
            pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.ALARM);
        }

        // alarm status 4
        status = frame.getChar();

        for (int i = 15; i > 8; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, AlarmLevel.WARNING);
                break;
            }
        }

        for (int i = 7; i > 0; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, AlarmLevel.ALARM);
                break;
            }
        }

        // alarm status 5
        status = frame.getChar();

        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(status, 15) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        for (int i = 14; i > 8; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, AlarmLevel.ALARM);
                break;
            }
        }

        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(status, 7) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        for (int i = 6; i > 3; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, AlarmLevel.ALARM);
                break;
            }
        }

        for (int i = 2; i > 0; i--) {
            if (pack.alarms.get(Alarm.CHARGE_TEMPERATURE_HIGH) != AlarmLevel.ALARM && BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, AlarmLevel.WARNING);
                break;
            }
        }

        // alarm status 6
        status = frame.getChar();

        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(status, 15) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        for (int i = 14; i > 11; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, AlarmLevel.ALARM);
                break;
            }
        }

        for (int i = 10; i > 8; i--) {
            if (pack.alarms.get(Alarm.DISCHARGE_TEMPERATURE_HIGH) != AlarmLevel.ALARM && BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, AlarmLevel.WARNING);
                break;
            }
        }

        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(status, 7) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        for (int i = 6; i > 3; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, AlarmLevel.ALARM);
                break;
            }
        }

        for (int i = 2; i > 0; i--) {
            if (pack.alarms.get(Alarm.CHARGE_TEMPERATURE_LOW) != AlarmLevel.ALARM && BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, AlarmLevel.WARNING);
                break;
            }
        }

        // alarm status 7
        status = frame.getChar();

        for (int i = 14; i > 11; i--) {
            if (BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_LOW, AlarmLevel.ALARM);
                break;
            }
        }

        for (int i = 10; i > 8; i--) {
            if (pack.alarms.get(Alarm.DISCHARGE_TEMPERATURE_LOW) != AlarmLevel.ALARM && BitUtil.bit(status, i)) {
                pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_LOW, AlarmLevel.WARNING);
                break;
            }
        }

        // bit 7 -2 reserved
        pack.alarms.put(Alarm.SOC_LOW, BitUtil.bit(status, 1) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        // bit 0 reserved

        // alarm status 8
        status = frame.getChar();
        // bit 15 - 9 reserved
        pack.alarms.put(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, BitUtil.bit(status, 8) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        // bit 7 - 0 reserved

    }


    private void readFactoryInfo(final ByteBuffer frame) {
        frame.getInt(); // functionCode
        frame.getInt(); // numRegisters
        final int unitId = frame.getInt();
        final BatteryPack pack = getBatteryPack(unitId);

        final char[] systemName = new char[13];

        for (int i = 0; i < 13; i++) {
            systemName[i] = frame.getChar();
        }

        pack.manufacturerCode = new String(systemName).trim();

        final char[] version = new char[13];

        for (int i = 0; i < 13; i++) {
            version[i] = frame.getChar();
        }

        pack.softwareVersion = new String(version).trim();
    }
}
