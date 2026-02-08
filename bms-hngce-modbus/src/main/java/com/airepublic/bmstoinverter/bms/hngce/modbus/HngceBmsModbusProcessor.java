package com.airepublic.bmstoinverter.bms.hngce.modbus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.NoDataAvailableException;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.TooManyInvalidFramesException;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

public class HngceBmsModbusProcessor extends BMS {
    private static final Logger LOG = LoggerFactory.getLogger(HngceBmsModbusProcessor.class);

    private static final int FUNCTION_READ_HOLDING_REGISTERS = 3;
    private static final int START_REGISTER = 0x0000;
    private static final int NUM_REGISTERS = 39;

    @Override
    protected void collectData(final Port port) throws IOException, TooManyInvalidFramesException, NoDataAvailableException {
        final int address = getBmsId();

        // Buffer format: [functionCode, startRegister, numRegisters, address]
        final ByteBuffer requestFrame = ByteBuffer.allocate(16);
        requestFrame.order(ByteOrder.LITTLE_ENDIAN);
        requestFrame.putInt(FUNCTION_READ_HOLDING_REGISTERS);
        requestFrame.putInt(START_REGISTER);
        requestFrame.putInt(NUM_REGISTERS);
        requestFrame.putInt(address);
        requestFrame.rewind();

        port.sendFrame(requestFrame);

        final ByteBuffer responseFrame = port.receiveFrame();

        if (responseFrame != null && responseFrame.remaining() >= 12) {
            parseResponse(responseFrame);
        } else {
            throw new NoDataAvailableException();
        }
    }


    private void parseResponse(final ByteBuffer data) {
        final BatteryPack pack = getBatteryPack(0);
        if (pack == null) {
            LOG.error("BMS: BatteryPack not found");
            return;
        }

        // Response header: [functionCode(int), numRegisters(int), unitId(int)]
        data.getInt(); // function code
        data.getInt(); // number of registers
        final int unitId = data.getInt();

        // Read register values (each as int, but only lower 16 bits are valid)
        final int[] regs = new int[NUM_REGISTERS];
        for (int i = 0; i < NUM_REGISTERS && data.remaining() >= 4; i++) {
            regs[i] = data.getInt() & 0xFFFF;
        }

        // HNGCE raw values: voltage/100 = V, current/100 = A
        // Framework displays: packVoltage/10, packCurrent/10
        // So we divide raw by 10 to get correct display

        // Reg 0: Total Voltage (raw/100 = V) -> divide by 10 for framework display
        pack.packVoltage = regs[0] / 10; // 4908 -> 490 -> displays as 49.0V

        // Reg 1: Current (signed, raw/100 = A) -> divide by 10 for framework display
        final short currentRaw = (short) regs[1];
        pack.packCurrent = currentRaw / 10; // -437 -> -43 -> displays as -4.3A

        // Reg 2-17: Cell voltages (mV) - keep as-is, framework divides by 1000
        int cellCount = 0;
        int minV = Integer.MAX_VALUE, maxV = 0;
        int minCell = 0, maxCell = 0;

        for (int i = 0; i < 16; i++) {
            final int cellV = regs[2 + i];
            pack.cellVmV[i] = cellV;
            if (cellV > 0) {
                cellCount++;
                if (cellV < minV) {
                    minV = cellV;
                    minCell = i + 1;
                }
                if (cellV > maxV) {
                    maxV = cellV;
                    maxCell = i + 1;
                }
            }
        }

        pack.numberOfCells = cellCount;
        pack.minCellmV = minV != Integer.MAX_VALUE ? minV : 0;
        pack.maxCellmV = maxV;
        pack.minCellVNum = minCell;
        pack.maxCellVNum = maxCell;
        pack.cellDiffmV = pack.maxCellmV - pack.minCellmV;

        // Reg 18-20: Temperatures (C) -> framework expects 0.1C
        pack.cellTemperature[0] = regs[18] * 10;
        pack.cellTemperature[1] = regs[19] * 10;
        pack.cellTemperature[2] = regs[20] * 10;
        pack.numOfTempSensors = 3;
        pack.tempAverage = (pack.cellTemperature[0] + pack.cellTemperature[1] + pack.cellTemperature[2]) / 3;
        pack.tempMax = Math.max(Math.max(pack.cellTemperature[0], pack.cellTemperature[1]), pack.cellTemperature[2]);
        pack.tempMin = Math.min(Math.min(pack.cellTemperature[0], pack.cellTemperature[1]), pack.cellTemperature[2]);

        // Reg 21: Remaining capacity (Ah) -> store as mAh
        pack.remainingCapacitymAh = regs[21] * 1000;

        // Reg 22: SOH (%)
        pack.packSOH = regs[22] * 10;

        // Reg 23: Max Current (A) -> framework expects 0.1A units
        pack.maxPackDischargeCurrent = regs[23];
        pack.maxPackChargeCurrent = regs[23];

        // Reg 24: SOC (%) -> framework expects 0.1% units
        pack.packSOC = regs[24] * 10;

        // Reg 25: Status (2=discharge, 3=charge)
        pack.chargeDischargeStatus = regs[25] == 3 ? 1 : regs[25] == 2 ? 2 : 0;

        // Reg 26-27: Alarms
        parseAlarms(pack, regs[26], regs[27]);

        // Reg 30: Cycle count
        pack.bmsCycles = regs[30];

        // Reg 37: Total capacity
        final int capRaw = regs[37];
        pack.ratedCapacitymAh = capRaw > 1000 ? capRaw * 100 : capRaw * 1000;

        pack.manufacturerCode = "HNGCE";

        LOG.info("BMS #{}: {}V, {}A, SOC={}%, Cells={}, Temp={}C, Cycles={}",
                unitId,
                String.format("%.2f", regs[0] / 100.0), // Use raw for accurate LOG
                String.format("%.2f", (short) regs[1] / 100.0),
                regs[24],
                pack.numberOfCells,
                regs[18],
                pack.bmsCycles);
    }


    private void parseAlarms(final BatteryPack pack, final int warnings, final int protections) {
        if ((warnings & 0x02) != 0) {
            pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.WARNING);
        }
        if ((warnings & 0x08) != 0) {
            pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, AlarmLevel.WARNING);
        }
        if ((warnings & 0x10) != 0) {
            pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, AlarmLevel.WARNING);
        }
        if ((warnings & 0x20) != 0) {
            pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, AlarmLevel.WARNING);
        }
        if ((warnings & 0x100) != 0) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, AlarmLevel.WARNING);
        }
        if ((warnings & 0x200) != 0) {
            pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_HIGH, AlarmLevel.WARNING);
        }
        if ((warnings & 0x400) != 0) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, AlarmLevel.WARNING);
        }
        if ((warnings & 0x800) != 0) {
            pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_LOW, AlarmLevel.WARNING);
        }
        if ((warnings & 0x1000) != 0) {
            pack.setAlarm(Alarm.SOC_LOW, AlarmLevel.WARNING);
        }

        if ((protections & 0x02) != 0) {
            pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.ALARM);
        }
        if ((protections & 0x08) != 0) {
            pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, AlarmLevel.ALARM);
        }
        if ((protections & 0x10) != 0) {
            pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, AlarmLevel.ALARM);
        }
        if ((protections & 0x20) != 0) {
            pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, AlarmLevel.ALARM);
        }
        if ((protections & 0x100) != 0) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, AlarmLevel.ALARM);
        }
        if ((protections & 0x200) != 0) {
            pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_HIGH, AlarmLevel.ALARM);
        }
        if ((protections & 0x400) != 0) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, AlarmLevel.ALARM);
        }
        if ((protections & 0x800) != 0) {
            pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_LOW, AlarmLevel.ALARM);
        }
    }
}
