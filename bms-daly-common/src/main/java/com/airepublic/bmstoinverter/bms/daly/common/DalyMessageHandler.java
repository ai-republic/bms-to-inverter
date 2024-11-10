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
package com.airepublic.bmstoinverter.bms.daly.common;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.util.BitUtil;

/**
 * The handler to interpret the {@link DalyMessage} and update the application wide
 * {@link EnergyStorage} object.
 */
public class DalyMessageHandler {
    private final static Logger LOG = LoggerFactory.getLogger(DalyMessageHandler.class);
    private final static int MIN_NUMBER_CELLS = 1;
    private final static int MAX_NUMBER_CELLS = 48;
    private final static int MIN_NUMBER_TEMP_SENSORS = 1;
    private final static int MAX_NUMBER_TEMP_SENSORS = 16;
    private final static int BATTERY_ID = 0;

    /**
     * Constructor.
     */
    public DalyMessageHandler() {
    }


    /**
     * Handles the {@link DalyMessage} and updates the {@link EnergyStorage} object.
     *
     * @param msg the {@link DalyMessage}
     */
    public void handleMessage(final BMS bms, final DalyMessage msg) {
        if (msg.bmsId != bms.getBmsId()) {
            LOG.error("Found invalid battery identifier: #{}", msg.bmsId);
            return;
        }

        try {
            switch (msg.cmd.id) {
                case 0x50:
                    getRatedCapacityAndCellVoltage(msg, bms);
                break;
                case 0x53:
                    getBatteryTypeInfo(msg, bms);
                break;
                case 0x5A:
                    getPackVoltageLimits(msg, bms);
                break;
                case 0x5B:
                    getPackDischargeChargeLimits(msg, bms);
                break;
                case 0x90:
                    getPackMeasurements(msg, bms);
                break;
                case 0x91:
                    getMinMaxCellVoltage(msg, bms);
                break;
                case 0x92:
                    getPackTemp(msg, bms);
                break;
                case 0x93:
                    getDischargeChargeMosStatus(msg, bms);
                break;
                case 0x94:
                    getStatusInfo(msg, bms);
                break;
                case 0x95:
                    getCellVoltages(msg, bms);
                break;
                case 0x96:
                    getCellTemperature(msg, bms);
                break;
                case 0x97:
                    getCellBalanceState(msg, bms);
                break;
                case 0x98:
                    getFailureCodes(msg, bms);
                break;
                default:
                break;
            }

        } catch (final Exception e) {
            LOG.error("Error reading BMS data for command: " + msg, e);
        }
    }


    private void getBatteryTypeInfo(final DalyMessage msg, final BMS bms) {
        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        battery.type = msg.data.get();

        LOG.debug("battery type={}=" + battery.type);
    }


    private void getRatedCapacityAndCellVoltage(final DalyMessage msg, final BMS bms) {
        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        battery.ratedCellmV = msg.data.getInt(); // in mV
        battery.ratedCapacitymAh = msg.data.getInt(); // in mAh

        LOG.debug("ratedCellmV={}, ratedCapacitymAh=" + battery.ratedCellmV, battery.ratedCapacitymAh);
    }


    private void getPackDischargeChargeLimits(final DalyMessage msg, final BMS bms) {
        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        battery.maxPackDischargeCurrent = 30000 - msg.data.getShort(); // 30000 offset
        // skip the next 2 bytes because only reading level 1
        msg.data.getShort();

        battery.maxPackChargeCurrent = 30000 - msg.data.getShort(); // 30000 offset

        LOG.debug("maxPackChargeCurrent={}, maxPackDischargeCurrent=" + battery.maxPackChargeCurrent, battery.maxPackDischargeCurrent);
    }


    private void getPackVoltageLimits(final DalyMessage msg, final BMS bms) {
        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        battery.maxPackVoltageLimit = msg.data.getShort();
        // skip the next 2 bytes because only reading level 1
        msg.data.getShort();
        battery.minPackVoltageLimit = msg.data.getShort();
    }


    private void getPackMeasurements(final DalyMessage msg, final BMS bms) throws IOException { // 0x90
        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        // data bytes 0-1 pack voltage
        battery.packVoltage = msg.data.getShort();
        // skip the next 2 bytes
        msg.data.getShort();
        // data bytes 4-5 pack current with a 30000 mA offset
        battery.packCurrent = msg.data.getShort() - 30000;
        // data bytes 6-7 pack SOC
        battery.packSOC = msg.data.getShort();

        // set the data buffer position back to 0
        msg.data.rewind();

        if (LOG.isDebugEnabled()) {
            LOG.info("BMS #{}: {}V, {}A, {}SOC", msg.bmsId, battery.packVoltage / 10f, battery.packCurrent / 10f, battery.packSOC / 10f);
        }
    }


    private void getMinMaxCellVoltage(final DalyMessage msg, final BMS bms) throws IOException { // 0x91

        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        // data byte 0-1 maximum cell voltage in mV
        battery.maxCellmV = msg.data.getShort();
        // data byte 2 maximum voltage cell number
        battery.maxCellVNum = msg.data.get();
        // data byte 3-4 minimum cell voltage in mV
        battery.minCellmV = msg.data.getShort();
        // data byte 5 minimum voltage cell number
        battery.minCellVNum = msg.data.get();
        battery.cellDiffmV = battery.maxCellmV - battery.minCellmV;

        if (LOG.isDebugEnabled()) {
            LOG.info("Battery {} Min/Max/Diff:\n"
                    + "\tMax Voltage: Cell {}({}mV)\n"
                    + "\tMin Voltage: Cell {}({}mV)\n"
                    + "\tDifference: {}mV",
                    bms.getBmsId() + 1, battery.maxCellVNum, battery.maxCellmV, battery.minCellVNum, battery.minCellmV, battery.cellDiffmV);
        }
    }


    private void getPackTemp(final DalyMessage msg, final BMS bms) throws IOException { // 0x92
        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        // maximum temperature (1C)with offset of 40
        battery.tempMax = (msg.data.get(0) - 40) * 10;
        // minimum temperature (1C)with offset of 40
        battery.tempMin = (msg.data.get(2) - 40) * 10;
        // calculate average temperature
        battery.tempAverage = (battery.tempMax + battery.tempMin) / 2;

        if (LOG.isDebugEnabled()) {
            LOG.info("Battery {} Temperature:\n"
                    + "\tMax: {}C\n"
                    + "\tMin: {}C\n"
                    + "\tAvg: {}C",
                    bms.getBmsId() + 1, battery.tempMax / 10f, battery.tempMin / 10f, battery.tempAverage / 10f);
        }
    }


    private void getDischargeChargeMosStatus(final DalyMessage msg, final BMS bms) throws IOException { // 0x93

        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        // read data byte 0 MOS status
        switch (msg.data.get()) {
            case 0:
                battery.chargeDischargeStatus = 0; // Stationary
            break;
            case 1:
                battery.chargeDischargeStatus = 1; // Charge
            break;
            case 2:
                battery.chargeDischargeStatus = 2; // Discharge
            break;
            default:
                battery.chargeDischargeStatus = 0; // Stationary
        }

        // data byte 1 charge MOS state
        battery.chargeMOSState = msg.data.get() == 1;
        // data byte 2 discharge MOS state
        battery.dischargeMOSState = msg.data.get() == 1;
        // data byte 3 BMS life cycles
        battery.bmsHeartBeat = Byte.toUnsignedInt(msg.data.get());

        // data bytes 4-7 remaining capacity in mAH
        battery.remainingCapacitymAh = msg.data.getInt();

        if (LOG.isDebugEnabled()) {
            LOG.info("Battery {} Dis-/Charge data:\n"
                    + "\tDis-/Charge-State: {}\n"
                    + "\tChargeMOS-State: {}\n"
                    + "\tDisChargeMOS-State: {}\n"
                    + "\tBMSHeartBeat: {}",
                    bms.getBmsId() + 1, battery.chargeDischargeStatus, battery.chargeMOSState, battery.dischargeMOSState, battery.bmsHeartBeat);
        }
    }


    private void getStatusInfo(final DalyMessage msg, final BMS bms) throws IOException { // 0x94

        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        // data byte 0 number of cells
        battery.numberOfCells = msg.data.get();
        // data byte 1 number of temperature sensors
        battery.numOfTempSensors = msg.data.get();
        // data byte 2 charger status
        battery.chargerState = msg.data.get() == 1;
        // data byte 3 load status
        battery.loadState = msg.data.get() == 1;

        // data byte 4 represents the 8 bits as booleans of the states of the Digital IO
        final byte dioBits = msg.data.get();
        for (int i = 0; i < 8; i++) {
            battery.dIO[i] = BitUtil.bit(dioBits, i);
        }

        // data bytes 5-6 BMS cycles
        battery.bmsCycles = msg.data.getShort();
    }


    private void getCellVoltages(final DalyMessage msg, final BMS bms) throws IOException { // 0x95
        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        // Check to make sure we have a valid number of cells
        if (battery.numberOfCells < MIN_NUMBER_CELLS && battery.numberOfCells >= MAX_NUMBER_CELLS) {
            throw new IOException("Can't get cell voltages yet. Please call getStatusInfo() first!");
        }

        // data byte 0 frame number of battery voltages
        final int frameNo = Byte.toUnsignedInt(msg.data.get()) - 1;
        int cellNo = frameNo * 3;

        for (int i = 0; i < 3; i++) {
            final int volt = msg.data.getShort();
            LOG.debug("BMS #{}, Frame No.: {}, Cell No: {}. {}mV", msg.bmsId, frameNo, cellNo + 1, volt);

            if (cellNo + 1 < MIN_NUMBER_CELLS || cellNo + 1 > MAX_NUMBER_CELLS) {
                LOG.debug("Invalid cell number " + (cellNo + 1) + " for battery pack #" + (bms.getBmsId() + 1) + "(" + battery.numberOfCells + "cells)");
                break;
            }

            battery.cellVmV[cellNo] = volt;
            cellNo++;
        }

        if (LOG.isDebugEnabled() && frameNo * 3 + cellNo >= battery.numberOfCells) {
            final StringBuilder buf = new StringBuilder("Battery #" + (bms.getBmsId() + 1) + " voltages:\n");

            for (int i = 0; i < battery.numberOfCells; i++) {
                buf.append("\t#" + (i + 1) + ": " + battery.cellVmV[i] / 1000f + "V\n");
            }

            LOG.info(buf.toString());
        }
    }


    private void getCellTemperature(final DalyMessage msg, final BMS bms) throws IOException { // 0x96
        int sensorNo = 0;

        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        // Check to make sure we have a valid number of temp sensors
        if (battery.numOfTempSensors < MIN_NUMBER_TEMP_SENSORS && battery.numOfTempSensors >= MAX_NUMBER_TEMP_SENSORS) {
            throw new IOException("Can't get cell temperatures yet. Please call getStatusInfo() first!");
        }

        for (int x = 0; x <= Math.ceil(battery.numOfTempSensors / 7); x++) {

            for (int i = 0; i < 7; i++) {

                if (LOG.isDebugEnabled()) {
                    LOG.info("BMS #" + msg.bmsId + ", Frame No.: " + msg.data.get(0) + ", Sensor No: " + (sensorNo + 1) + ". " + (msg.data.get(1 + i) - 40) + "�C");
                }

                battery.cellTemperature[sensorNo] = msg.data.get(1 + i) - 40;
                sensorNo++;
                if (sensorNo + 1 >= battery.numOfTempSensors) {
                    break;
                }
            }
        }
    }


    private void getCellBalanceState(final DalyMessage msg, final BMS bms) throws IOException { // 0x97
        boolean cellBalanceActive = false;
        int cellNo = 0;

        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);

        // Check to make sure we have a valid number of cells
        if (battery.numberOfCells < MIN_NUMBER_CELLS && battery.numberOfCells >= MAX_NUMBER_CELLS) {
            throw new IOException("Can't get cell balance states yet. Please call getStatusInfo() first!");
        }

        // data bytes 0-5 are balance states for up to 48 cells in the pack
        for (int i = 0; i < 6; i++) {
            final byte byteValue = msg.data.get();

            // read the cell balance state of the next 8 cells
            for (int j = 0; j < 8; j++) {
                final boolean state = BitUtil.bit(byteValue, j);
                battery.cellBalanceState[cellNo] = state;
                cellNo++;

                if (state) {
                    cellBalanceActive = true;
                }
            }
        }

        battery.cellBalanceActive = cellBalanceActive;

        if (LOG.isDebugEnabled()) {
            final StringBuffer buf = new StringBuffer("BMS #" + msg.bmsId + ", Cell Balance State: \n");

            for (int i = 0; i < battery.numberOfCells; i++) {
                buf.append("\t#" + (i + 1) + ": " + battery.cellBalanceState[i] + "\n");
            }

            buf.append("CellBalanceActive: " + cellBalanceActive);

            LOG.info(buf.toString());
        }

    }


    private AlarmLevel getAlarmLevel(final int value) {
        return value == 0 ? AlarmLevel.NONE : value == 1 ? AlarmLevel.WARNING : AlarmLevel.ALARM;
    }


    private void getFailureCodes(final DalyMessage msg, final BMS bms) throws IOException // 0x98
    {
        final BatteryPack battery = bms.getBatteryPack(BATTERY_ID);
        battery.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.NONE);

        byte byteValue = msg.data.get(0);
        /* 0x00 */
        battery.setAlarm(Alarm.CELL_VOLTAGE_HIGH, getAlarmLevel(BitUtil.bits(byteValue, 0, 2)));
        battery.setAlarm(Alarm.CELL_VOLTAGE_LOW, getAlarmLevel(BitUtil.bits(byteValue, 2, 2)));
        battery.setAlarm(Alarm.PACK_VOLTAGE_HIGH, getAlarmLevel(BitUtil.bits(byteValue, 4, 2)));
        battery.setAlarm(Alarm.PACK_VOLTAGE_LOW, getAlarmLevel(BitUtil.bits(byteValue, 6, 2)));

        /* 0x01 */
        byteValue = msg.data.get(1);
        battery.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, getAlarmLevel(BitUtil.bits(byteValue, 0, 2)));
        battery.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, getAlarmLevel(BitUtil.bits(byteValue, 2, 2)));
        battery.setAlarm(Alarm.DISCHARGE_TEMPERATURE_HIGH, getAlarmLevel(BitUtil.bits(byteValue, 4, 2)));
        battery.setAlarm(Alarm.DISCHARGE_TEMPERATURE_LOW, getAlarmLevel(BitUtil.bits(byteValue, 6, 2)));

        /* 0x02 */
        byteValue = msg.data.get(2);
        battery.setAlarm(Alarm.CHARGE_CURRENT_HIGH, getAlarmLevel(BitUtil.bits(byteValue, 0, 2)));
        battery.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, getAlarmLevel(BitUtil.bits(byteValue, 2, 2)));
        battery.setAlarm(Alarm.SOC_HIGH, getAlarmLevel(BitUtil.bits(byteValue, 4, 2)));
        battery.setAlarm(Alarm.SOC_LOW, getAlarmLevel(BitUtil.bits(byteValue, 6, 2)));

        /* 0x03 */
        byteValue = msg.data.get(3);
        battery.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, getAlarmLevel(BitUtil.bits(byteValue, 0, 2)));
        battery.setAlarm(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH, getAlarmLevel(BitUtil.bits(byteValue, 2, 2)));

        /* 0x04 */
        byteValue = msg.data.get(4);
        battery.setAlarm(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(byteValue, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.DISCHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(byteValue, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_SENSOR_CHARGE_MODULE_TEMPERATURE, BitUtil.bit(byteValue, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_SENSOR_DISCHARGE_MODULE_TEMPERATURE, BitUtil.bit(byteValue, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_OTHER, BitUtil.bits(byteValue, 4, 2) != 0 ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_CHARGE_BREAKER, BitUtil.bit(byteValue, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_DISCHARGE_BREAKER, BitUtil.bit(byteValue, 7) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        /* 0x05 */
        byteValue = msg.data.get(5);

        if (battery.getAlarmLevel(Alarm.FAILURE_OTHER) != AlarmLevel.ALARM) {
            battery.setAlarm(Alarm.FAILURE_OTHER, BitUtil.bit(byteValue, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        }

        battery.setAlarm(Alarm.FAILURE_SENSOR_PACK_VOLTAGE, BitUtil.bit(byteValue, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_SENSOR_PACK_TEMPERATURE, BitUtil.bit(byteValue, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_EEPROM_MODULE, BitUtil.bit(byteValue, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_CLOCK_MODULE, BitUtil.bit(byteValue, 4) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_PRECHARGE_MODULE, BitUtil.bit(byteValue, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_COMMUNICATION_INTERNAL, BitUtil.bit(byteValue, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_COMMUNICATION_EXTERNAL, BitUtil.bit(byteValue, 7) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        /* 0x06 */
        byteValue = msg.data.get(6);
        battery.setAlarm(Alarm.FAILURE_SENSOR_PACK_CURRENT, BitUtil.bit(byteValue, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        if (battery.getAlarmLevel(Alarm.FAILURE_OTHER) != AlarmLevel.ALARM) {
            battery.setAlarm(Alarm.FAILURE_OTHER, BitUtil.bit(byteValue, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        }

        battery.setAlarm(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, BitUtil.bit(byteValue, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        battery.setAlarm(Alarm.FAILURE_NOT_CHARGING_DUE_TO_LOW_VOLTAGE, BitUtil.bit(byteValue, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
    }

}
