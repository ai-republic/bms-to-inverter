package com.airepublic.bmstoinverter.daly.can;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

public class DalyMessageHandler {
    private final static Logger LOG = LoggerFactory.getLogger(DalyMessageHandler.class);
    private final static int MIN_NUMBER_CELLS = 1;
    private final static int MAX_NUMBER_CELLS = 48;
    private final static int MIN_NUMBER_TEMP_SENSORS = 1;
    private final static int MAX_NUMBER_TEMP_SENSORS = 16;

    private final BatteryPack[] batteryPacks;
    private final Alarm[] alarms;

    public DalyMessageHandler(final BatteryPack[] dalyParams, final Alarm[] alarms) {
        batteryPacks = dalyParams;
        this.alarms = alarms;
    }


    public void handleMessage(final DalyMessage msg) {
        try {
            switch (msg.dataId) {
                case (byte) 0x90:
                    getPackMeasurements(msg);
                break;
                case (byte) 0x91:
                    getMinMaxCellVoltage(msg);
                break;
                case (byte) 0x92:
                    getPackTemp(msg);
                break;
                case (byte) 0x93:
                    getDischargeChargeMosStatus(msg);
                break;
                case (byte) 0x94:
                    getStatusInfo(msg);
                break;
                case (byte) 0x95:
                    getCellVoltages(msg);
                break;
                case (byte) 0x96:
                    getCellTemperature(msg);
                break;
                case (byte) 0x97:
                    getCellBalanceState(msg);
                break;
                case (byte) 0x98:
                    getFailureCodes(msg);
                break;
                default:
                break;
            }

        } catch (final IOException e) {
            LOG.error("Error reading BMS data: ", e);
        }
    }


    void getPackMeasurements(final DalyMessage msg) throws IOException { // 0x90
        final int battery = msg.address - 1;

        if (battery < 0 || battery >= batteryPacks.length) {
            LOG.error("getPackMeasurements -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        // data bytes 0-1 pack voltage
        batteryPacks[battery].packVoltage = msg.data.getShort();
        // skip the next 2 bytes
        msg.data.getShort();
        // data bytes 4-5 pack current with a 30000 mA offset
        batteryPacks[battery].packCurrent = msg.data.getShort() - 30000;
        // data bytes 6-7 pack SOC
        batteryPacks[battery].packSOC = msg.data.getShort();

        // set the data buffer position back to 0
        msg.data.rewind();

        if (LOG.isDebugEnabled()) {
            LOG.info("BMS #{}: {}V, {}A, {}SOC", msg.address, batteryPacks[battery].packVoltage / 10f, batteryPacks[battery].packCurrent / 10f, batteryPacks[battery].packSOC / 10f);
        }
    }


    void getMinMaxCellVoltage(final DalyMessage msg) throws IOException { // 0x91
        final int battery = msg.address - 1;

        if (battery < 0 || battery >= batteryPacks.length) {
            LOG.debug("getMinMaxCellVoltage -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        // data byte 0-1 maximum cell voltage in mV
        batteryPacks[battery].maxCellmV = msg.data.getShort();
        // data byte 2 maximum voltage cell number
        batteryPacks[battery].maxCellVNum = msg.data.get();
        // data byte 3-4 minimum cell voltage in mV
        batteryPacks[battery].minCellmV = msg.data.getShort();
        // data byte 5 minimum voltage cell number
        batteryPacks[battery].minCellVNum = msg.data.get();
        batteryPacks[battery].cellDiff = batteryPacks[battery].maxCellmV - batteryPacks[battery].minCellmV;

        if (LOG.isDebugEnabled()) {
            LOG.info("Battery {} Min/Max/Diff:\n"
                    + "\tMax Voltage: Cell {}({}mV)\n"
                    + "\tMin Voltage: Cell {}({}mV)\n"
                    + "\tDifference: {}mV",
                    battery + 1, batteryPacks[battery].maxCellVNum, batteryPacks[battery].maxCellmV, batteryPacks[battery].minCellVNum, batteryPacks[battery].minCellmV, batteryPacks[battery].cellDiff);
        }
    }


    void getPackTemp(final DalyMessage msg) throws IOException { // 0x92
        final int battery = msg.address - 1;

        if (battery < 0 || battery >= batteryPacks.length) {
            LOG.debug("getPackTemp -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        // byte 0 maximum temperature with offset of 40
        batteryPacks[battery].tempMax = msg.data.get(0) - 40;
        // byte 2 minimum temperature with offset of 40
        batteryPacks[battery].tempMin = msg.data.get(2) - 40;
        batteryPacks[battery].tempAverage = (batteryPacks[battery].tempMax + batteryPacks[battery].tempMin) / 2;

        if (LOG.isDebugEnabled()) {
            LOG.info("Battery {} Temperature:\n"
                    + "\tMax: {}C\n"
                    + "\tMin: {}C\n"
                    + "\tAvg: {}C",
                    battery + 1, batteryPacks[battery].tempMax, batteryPacks[battery].tempMin, batteryPacks[battery].tempAverage);
        }
    }


    void getDischargeChargeMosStatus(final DalyMessage msg) throws IOException { // 0x93
        final int battery = msg.address - 1;

        if (battery < 0 || battery >= batteryPacks.length) {
            LOG.debug("getDischargeChargeMosStatus -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        // read data byte 0 MOS status
        switch (msg.data.get()) {
            case 0:
                batteryPacks[battery].chargeDischargeStatus = "Stationary";
            break;
            case 1:
                batteryPacks[battery].chargeDischargeStatus = "Charge";
            break;
            case 2:
                batteryPacks[battery].chargeDischargeStatus = "Discharge";
            break;
        }

        // data byte 1 charge MOS state
        batteryPacks[battery].chargeMOSState = msg.data.get() == 1;
        // data byte 2 discharge MOS state
        batteryPacks[battery].disChargeMOSState = msg.data.get() == 1;
        // data byte 3 BMS life cycles
        batteryPacks[battery].bmsHeartBeat = Byte.toUnsignedInt(msg.data.get());

        // data bytes 4-7 remaining capacity in mAH
        batteryPacks[battery].remainingCapacitymAh = msg.data.getInt();

        if (LOG.isDebugEnabled()) {
            LOG.info("Battery {} Dis-/Charge data:\n"
                    + "\tDis-/Charge-State: {}\n"
                    + "\tChargeMOS-State: {}\n"
                    + "\tDisChargeMOS-State: {}\n"
                    + "\tBMSHeartBeat: {}",
                    battery + 1, batteryPacks[battery].chargeDischargeStatus, batteryPacks[battery].chargeMOSState, batteryPacks[battery].disChargeMOSState, batteryPacks[battery].bmsHeartBeat);
        }
    }


    void getStatusInfo(final DalyMessage msg) throws IOException { // 0x94
        final int battery = msg.address - 1;

        if (battery < 0 || battery >= batteryPacks.length) {
            LOG.debug("getStatusInfo -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        // data byte 0 number of cells
        batteryPacks[battery].numberOfCells = msg.data.get();
        // data byte 1 number of temperature sensors
        batteryPacks[battery].numOfTempSensors = msg.data.get();
        // data byte 2 charger status
        batteryPacks[battery].chargeState = msg.data.get() == 1;
        // data byte 3 load status
        batteryPacks[battery].loadState = msg.data.get() == 1;

        // data byte 4 represents the 8 bits as booleans of the states of the Digital IO
        final byte dioBits = msg.data.get();
        for (int i = 0; i < 8; i++) {
            batteryPacks[battery].dIO[i] = bitRead(dioBits, i);
        }

        // data bytes 5-6 BMS cycles
        batteryPacks[battery].bmsCycles = msg.data.getShort();
    }


    void getCellVoltages(final DalyMessage msg) throws IOException { // 0x95
        final int battery = msg.address - 1;

        if (battery < 0 || battery >= batteryPacks.length) {
            LOG.debug("getCellVoltages -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        // Check to make sure we have a valid number of cells
        if (batteryPacks[battery].numberOfCells < MIN_NUMBER_CELLS && batteryPacks[battery].numberOfCells >= MAX_NUMBER_CELLS) {
            throw new IOException("Can't get cell voltages yet. Please call getStatusInfo() first!");
        }

        // data byte 0 frame number of battery voltages
        final int frameNo = Byte.toUnsignedInt(msg.data.get()) - 1;
        int cellNo = frameNo * 3;

        for (int i = 0; i < 3; i++) {
            final int volt = msg.data.getShort();
            LOG.debug("BMS #{}, Frame No.: {}, Cell No: {}. {}mV", msg.address, frameNo, cellNo + 1, volt);

            if (cellNo + 1 < MIN_NUMBER_CELLS || cellNo + 1 > MAX_NUMBER_CELLS) {
                LOG.debug("Invalid cell number " + (cellNo + 1) + " for battery pack #" + (battery + 1) + "(" + batteryPacks[battery].numberOfCells + "cells)");
                break;
            }

            batteryPacks[battery].cellVmV[cellNo] = volt;
            cellNo++;
        }

        if (LOG.isDebugEnabled() && frameNo * 3 + cellNo >= batteryPacks[battery].numberOfCells) {
            final StringBuilder buf = new StringBuilder("Battery #" + (battery + 1) + " voltages:\n");

            for (int i = 0; i < batteryPacks[battery].numberOfCells; i++) {
                buf.append("\t#" + (i + 1) + ": " + batteryPacks[battery].cellVmV[i] / 1000f + "V\n");
            }

            LOG.info(buf.toString());
        }
    }


    void getCellTemperature(final DalyMessage msg) throws IOException { // 0x96
        final int battery = msg.address - 1;

        if (battery < 0 || battery >= batteryPacks.length) {
            LOG.debug("getCellTemperature -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        int sensorNo = 0;

        // Check to make sure we have a valid number of temp sensors
        if (batteryPacks[battery].numOfTempSensors < MIN_NUMBER_TEMP_SENSORS && batteryPacks[battery].numOfTempSensors >= MAX_NUMBER_TEMP_SENSORS) {
            throw new IOException("Can't get cell temperatures yet. Please call getStatusInfo() first!");
        }

        for (int x = 0; x <= Math.ceil(batteryPacks[battery].numOfTempSensors / 7); x++) {

            for (int i = 0; i < 7; i++) {

                if (LOG.isDebugEnabled()) {
                    LOG.info("BMS #" + msg.address + ", Frame No.: " + msg.data.get(0) + ", Sensor No: " + (sensorNo + 1) + ". " + (msg.data.get(1 + i) - 40) + "�C");
                }

                batteryPacks[battery].cellTemperature[sensorNo] = msg.data.get(1 + i) - 40;
                sensorNo++;
                if (sensorNo + 1 >= batteryPacks[battery].numOfTempSensors) {
                    break;
                }
            }
        }
    }


    void getCellBalanceState(final DalyMessage msg) throws IOException { // 0x97
        final int battery = msg.address - 1;

        if (battery < 0 || battery >= batteryPacks.length) {
            LOG.debug("getCellBalanceState -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        boolean cellBalanceActive = false;
        int cellNo = 0;

        // Check to make sure we have a valid number of cells
        if (batteryPacks[battery].numberOfCells < MIN_NUMBER_CELLS && batteryPacks[battery].numberOfCells >= MAX_NUMBER_CELLS) {
            throw new IOException("Can't get cell balance states yet. Please call getStatusInfo() first!");
        }

        // data bytes 0-5 are balance states for up to 48 cells in the pack
        for (int i = 0; i < 6; i++) {
            final byte byteValue = msg.data.get();

            // read the cell balance state of the next 8 cells
            for (int j = 0; j < 8; j++) {
                final boolean state = bitRead(byteValue, j);
                batteryPacks[battery].cellBalanceState[cellNo] = state;
                cellNo++;

                if (state) {
                    cellBalanceActive = true;
                }
            }
        }

        batteryPacks[battery].cellBalanceActive = cellBalanceActive;

        if (LOG.isDebugEnabled()) {
            final StringBuffer buf = new StringBuffer("BMS #" + msg.address + ", Cell Balance State: \n");

            for (int i = 0; i < batteryPacks[battery].numberOfCells; i++) {
                buf.append("\t#" + (i + 1) + ": " + batteryPacks[battery].cellBalanceState[i] + "\n");
            }

            buf.append("CellBalanceActive: " + cellBalanceActive);

            LOG.info(buf.toString());
        }

    }


    void getFailureCodes(final DalyMessage msg) throws IOException // 0x98
    {
        final int battery = msg.address - 1;

        if (battery + 1 > batteryPacks.length) {
            LOG.debug("getFailureCodes -> Found invalid battery identifier: #{}", battery + 1);
            return;
        }

        byte byteValue = msg.data.get();
        /* 0x00 */
        alarms[battery].levelOneCellVoltageTooHigh = bitRead(byteValue, 0);
        alarms[battery].levelTwoCellVoltageTooHigh = bitRead(byteValue, 1);
        alarms[battery].levelOneCellVoltageTooLow = bitRead(byteValue, 2);
        alarms[battery].levelTwoCellVoltageTooLow = bitRead(byteValue, 3);
        alarms[battery].levelOnePackVoltageTooHigh = bitRead(byteValue, 4);
        alarms[battery].levelTwoPackVoltageTooHigh = bitRead(byteValue, 5);
        alarms[battery].levelOnePackVoltageTooLow = bitRead(byteValue, 6);
        alarms[battery].levelTwoPackVoltageTooLow = bitRead(byteValue, 7);

        /* 0x01 */
        byteValue = msg.data.get(1);
        alarms[battery].levelOneChargeTempTooHigh = bitRead(byteValue, 1);
        alarms[battery].levelTwoChargeTempTooHigh = bitRead(byteValue, 1);
        alarms[battery].levelOneChargeTempTooLow = bitRead(byteValue, 1);
        alarms[battery].levelTwoChargeTempTooLow = bitRead(byteValue, 1);
        alarms[battery].levelOneDischargeTempTooHigh = bitRead(byteValue, 1);
        alarms[battery].levelTwoDischargeTempTooHigh = bitRead(byteValue, 1);
        alarms[battery].levelOneDischargeTempTooLow = bitRead(byteValue, 1);
        alarms[battery].levelTwoDischargeTempTooLow = bitRead(byteValue, 1);

        /* 0x02 */
        byteValue = msg.data.get(2);
        alarms[battery].levelOneChargeCurrentTooHigh = bitRead(byteValue, 0);
        alarms[battery].levelTwoChargeCurrentTooHigh = bitRead(byteValue, 1);
        alarms[battery].levelOneDischargeCurrentTooHigh = bitRead(byteValue, 2);
        alarms[battery].levelTwoDischargeCurrentTooHigh = bitRead(byteValue, 3);
        alarms[battery].levelOneStateOfChargeTooHigh = bitRead(byteValue, 4);
        alarms[battery].levelTwoStateOfChargeTooHigh = bitRead(byteValue, 5);
        alarms[battery].levelOneStateOfChargeTooLow = bitRead(byteValue, 6);
        alarms[battery].levelTwoStateOfChargeTooLow = bitRead(byteValue, 7);

        /* 0x03 */
        byteValue = msg.data.get(3);
        alarms[battery].levelOneCellVoltageDifferenceTooHigh = bitRead(byteValue, 0);
        alarms[battery].levelTwoCellVoltageDifferenceTooHigh = bitRead(byteValue, 1);
        alarms[battery].levelOneTempSensorDifferenceTooHigh = bitRead(byteValue, 2);
        alarms[battery].levelTwoTempSensorDifferenceTooHigh = bitRead(byteValue, 3);

        /* 0x04 */
        byteValue = msg.data.get(4);
        alarms[battery].chargeFETTemperatureTooHigh = bitRead(byteValue, 0);
        alarms[battery].dischargeFETTemperatureTooHigh = bitRead(byteValue, 1);
        alarms[battery].failureOfChargeFETTemperatureSensor = bitRead(byteValue, 2);
        alarms[battery].failureOfDischargeFETTemperatureSensor = bitRead(byteValue, 3);
        alarms[battery].failureOfChargeFETAdhesion = bitRead(byteValue, 4);
        alarms[battery].failureOfDischargeFETAdhesion = bitRead(byteValue, 5);
        alarms[battery].failureOfChargeFETTBreaker = bitRead(byteValue, 6);
        alarms[battery].failureOfDischargeFETBreaker = bitRead(byteValue, 7);

        /* 0x05 */
        byteValue = msg.data.get(5);
        alarms[battery].failureOfAFEAcquisitionModule = bitRead(byteValue, 0);
        alarms[battery].failureOfVoltageSensorModule = bitRead(byteValue, 1);
        alarms[battery].failureOfTemperatureSensorModule = bitRead(byteValue, 2);
        alarms[battery].failureOfEEPROMStorageModule = bitRead(byteValue, 3);
        alarms[battery].failureOfRealtimeClockModule = bitRead(byteValue, 4);
        alarms[battery].failureOfPrechargeModule = bitRead(byteValue, 5);
        alarms[battery].failureOfVehicleCommunicationModule = bitRead(byteValue, 6);
        alarms[battery].failureOfIntranetCommunicationModule = bitRead(byteValue, 7);

        /* 0x06 */
        byteValue = msg.data.get(6);
        alarms[battery].failureOfCurrentSensorModule = bitRead(byteValue, 0);
        alarms[battery].failureOfMainVoltageSensorModule = bitRead(byteValue, 1);
        alarms[battery].failureOfShortCircuitProtection = bitRead(byteValue, 2);
        alarms[battery].failureOfLowVoltageNoCharging = bitRead(byteValue, 3);
    }

    /*
     * void setBmsReset() throws IOException // 0x00 Reset the BMS {
     * dalyPort.sendCommand(prepareTXBuffer(DalyCommand.BMS_RESET));
     * 
     * try { dalyPort.receiveBytes(); } catch (final IOException e) { throw new
     * IOException("<DALY-BMS DEBUG> Send failed, can't verify BMS was reset!\n", e); } }
     */


    boolean bitRead(final int n, final int k) {
        return (n >> k & 1) == 1;
    }
}
