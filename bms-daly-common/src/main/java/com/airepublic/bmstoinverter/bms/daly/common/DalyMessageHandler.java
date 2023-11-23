package com.airepublic.bmstoinverter.bms.daly.common;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.inject.Inject;

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

    @Inject
    private EnergyStorage energyStorage;

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
    public void handleMessage(final DalyMessage msg) {
        try {
            switch (msg.cmd.id) {
                case 0x50:
                    getRatedCapacityAndCellVoltage(msg);
                break;
                case 0x53:
                    getBatteryTypeInfo(msg);
                break;
                case 0x5A:
                    getPackVoltageLimits(msg);
                break;
                case 0x5B:
                    getPackDischargeChargeLimits(msg);
                break;
                case 0x90:
                    getPackMeasurements(msg);
                break;
                case 0x91:
                    getMinMaxCellVoltage(msg);
                break;
                case 0x92:
                    getPackTemp(msg);
                break;
                case 0x93:
                    getDischargeChargeMosStatus(msg);
                break;
                case 0x94:
                    getStatusInfo(msg);
                break;
                case 0x95:
                    getCellVoltages(msg);
                break;
                case 0x96:
                    getCellTemperature(msg);
                break;
                case 0x97:
                    getCellBalanceState(msg);
                break;
                case 0x98:
                    getFailureCodes(msg);
                break;
                default:
                break;
            }

        } catch (final IOException e) {
            LOG.error("Error reading BMS data: ", e);
        }
    }


    private void getBatteryTypeInfo(final DalyMessage msg) {
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.error("getRatedCapacityAndCellVoltage -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        battery.type = msg.data.get();

        LOG.debug("battery type={}=" + battery.type);
    }


    private void getRatedCapacityAndCellVoltage(final DalyMessage msg) {
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.error("getRatedCapacityAndCellVoltage -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        battery.ratedCellmV = msg.data.getInt(); // in mV
        battery.ratedCapacitymAh = msg.data.getInt(); // in mAh

        LOG.debug("ratedCellmV={}, ratedCapacitymAh=" + battery.ratedCellmV, battery.ratedCapacitymAh);
    }


    private void getPackDischargeChargeLimits(final DalyMessage msg) {
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.error("getPackDischargeChargeLimits -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        battery.maxPackDischargeCurrent = 30000 - msg.data.getShort(); // 30000 offset
        // skip the next 2 bytes because only reading level 1
        msg.data.getShort();

        battery.maxPackChargeCurrent = 30000 - msg.data.getShort(); // 3000 offset

        LOG.debug("maxPackChargeCurrent={}, maxPackDischargeCurrent=" + battery.maxPackChargeCurrent, battery.maxPackDischargeCurrent);
    }


    private void getPackVoltageLimits(final DalyMessage msg) {
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.error("getPackVoltageLimits -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        battery.maxPackVoltageLimit = msg.data.getShort();
        // skip the next 2 bytes because only reading level 1
        msg.data.getShort();
        battery.minPackVoltageLimit = msg.data.getShort();
    }


    private void getPackMeasurements(final DalyMessage msg) throws IOException { // 0x90
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.error("getPackMeasurements -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

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
            LOG.info("BMS #{}: {}V, {}A, {}SOC", msg.address, battery.packVoltage / 10f, battery.packCurrent / 10f, battery.packSOC / 10f);
        }
    }


    private void getMinMaxCellVoltage(final DalyMessage msg) throws IOException { // 0x91
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.debug("getMinMaxCellVoltage -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

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
                    batteryNo + 1, battery.maxCellVNum, battery.maxCellmV, battery.minCellVNum, battery.minCellmV, battery.cellDiffmV);
        }
    }


    private void getPackTemp(final DalyMessage msg) throws IOException { // 0x92
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.debug("getPackTemp -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        // byte 0 maximum temperature with offset of 40
        battery.tempMax = msg.data.get(0) - 40;
        // byte 2 minimum temperature with offset of 40
        battery.tempMin = msg.data.get(2) - 40;
        battery.tempAverage = (battery.tempMax + battery.tempMin) / 2;

        if (LOG.isDebugEnabled()) {
            LOG.info("Battery {} Temperature:\n"
                    + "\tMax: {}C\n"
                    + "\tMin: {}C\n"
                    + "\tAvg: {}C",
                    batteryNo + 1, battery.tempMax, battery.tempMin, battery.tempAverage);
        }
    }


    private void getDischargeChargeMosStatus(final DalyMessage msg) throws IOException { // 0x93
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.debug("getDischargeChargeMosStatus -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        // read data byte 0 MOS status
        switch (msg.data.get()) {
            case 0:
                battery.chargeDischargeStatus = "Stationary";
            break;
            case 1:
                battery.chargeDischargeStatus = "Charge";
            break;
            case 2:
                battery.chargeDischargeStatus = "Discharge";
            break;
        }

        // data byte 1 charge MOS state
        battery.chargeMOSState = msg.data.get() == 1;
        // data byte 2 discharge MOS state
        battery.disChargeMOSState = msg.data.get() == 1;
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
                    batteryNo + 1, battery.chargeDischargeStatus, battery.chargeMOSState, battery.disChargeMOSState, battery.bmsHeartBeat);
        }
    }


    private void getStatusInfo(final DalyMessage msg) throws IOException { // 0x94
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.debug("getStatusInfo -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        // data byte 0 number of cells
        battery.numberOfCells = msg.data.get();
        // data byte 1 number of temperature sensors
        battery.numOfTempSensors = msg.data.get();
        // data byte 2 charger status
        battery.chargeState = msg.data.get() == 1;
        // data byte 3 load status
        battery.loadState = msg.data.get() == 1;

        // data byte 4 represents the 8 bits as booleans of the states of the Digital IO
        final byte dioBits = msg.data.get();
        for (int i = 0; i < 8; i++) {
            battery.dIO[i] = bitRead(dioBits, i);
        }

        // data bytes 5-6 BMS cycles
        battery.bmsCycles = msg.data.getShort();
    }


    private void getCellVoltages(final DalyMessage msg) throws IOException { // 0x95
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.debug("getCellVoltages -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        // Check to make sure we have a valid number of cells
        if (battery.numberOfCells < MIN_NUMBER_CELLS && battery.numberOfCells >= MAX_NUMBER_CELLS) {
            throw new IOException("Can't get cell voltages yet. Please call getStatusInfo() first!");
        }

        // data byte 0 frame number of battery voltages
        final int frameNo = Byte.toUnsignedInt(msg.data.get()) - 1;
        int cellNo = frameNo * 3;

        for (int i = 0; i < 3; i++) {
            final int volt = msg.data.getShort();
            LOG.debug("BMS #{}, Frame No.: {}, Cell No: {}. {}mV", msg.address, frameNo, cellNo + 1, volt);

            if (cellNo + 1 < MIN_NUMBER_CELLS || cellNo + 1 > MAX_NUMBER_CELLS) {
                LOG.debug("Invalid cell number " + (cellNo + 1) + " for battery pack #" + (batteryNo + 1) + "(" + battery.numberOfCells + "cells)");
                break;
            }

            battery.cellVmV[cellNo] = volt;
            cellNo++;
        }

        if (LOG.isDebugEnabled() && frameNo * 3 + cellNo >= battery.numberOfCells) {
            final StringBuilder buf = new StringBuilder("Battery #" + (batteryNo + 1) + " voltages:\n");

            for (int i = 0; i < battery.numberOfCells; i++) {
                buf.append("\t#" + (i + 1) + ": " + battery.cellVmV[i] / 1000f + "V\n");
            }

            LOG.info(buf.toString());
        }
    }


    private void getCellTemperature(final DalyMessage msg) throws IOException { // 0x96
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.debug("getCellTemperature -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        int sensorNo = 0;
        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        // Check to make sure we have a valid number of temp sensors
        if (battery.numOfTempSensors < MIN_NUMBER_TEMP_SENSORS && battery.numOfTempSensors >= MAX_NUMBER_TEMP_SENSORS) {
            throw new IOException("Can't get cell temperatures yet. Please call getStatusInfo() first!");
        }

        for (int x = 0; x <= Math.ceil(battery.numOfTempSensors / 7); x++) {

            for (int i = 0; i < 7; i++) {

                if (LOG.isDebugEnabled()) {
                    LOG.info("BMS #" + msg.address + ", Frame No.: " + msg.data.get(0) + ", Sensor No: " + (sensorNo + 1) + ". " + (msg.data.get(1 + i) - 40) + "�C");
                }

                battery.cellTemperature[sensorNo] = msg.data.get(1 + i) - 40;
                sensorNo++;
                if (sensorNo + 1 >= battery.numOfTempSensors) {
                    break;
                }
            }
        }
    }


    private void getCellBalanceState(final DalyMessage msg) throws IOException { // 0x97
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.debug("getCellBalanceState -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        boolean cellBalanceActive = false;
        int cellNo = 0;
        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        // Check to make sure we have a valid number of cells
        if (battery.numberOfCells < MIN_NUMBER_CELLS && battery.numberOfCells >= MAX_NUMBER_CELLS) {
            throw new IOException("Can't get cell balance states yet. Please call getStatusInfo() first!");
        }

        // data bytes 0-5 are balance states for up to 48 cells in the pack
        for (int i = 0; i < 6; i++) {
            final byte byteValue = msg.data.get();

            // read the cell balance state of the next 8 cells
            for (int j = 0; j < 8; j++) {
                final boolean state = bitRead(byteValue, j);
                battery.cellBalanceState[cellNo] = state;
                cellNo++;

                if (state) {
                    cellBalanceActive = true;
                }
            }
        }

        battery.cellBalanceActive = cellBalanceActive;

        if (LOG.isDebugEnabled()) {
            final StringBuffer buf = new StringBuffer("BMS #" + msg.address + ", Cell Balance State: \n");

            for (int i = 0; i < battery.numberOfCells; i++) {
                buf.append("\t#" + (i + 1) + ": " + battery.cellBalanceState[i] + "\n");
            }

            buf.append("CellBalanceActive: " + cellBalanceActive);

            LOG.info(buf.toString());
        }

    }


    private void getFailureCodes(final DalyMessage msg) throws IOException // 0x98
    {
        final int batteryNo = msg.address - 1;

        if (batteryNo + 1 > energyStorage.getBatteryPackCount()) {
            LOG.debug("getFailureCodes -> Found invalid battery identifier: #{}", batteryNo + 1);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);
        byte byteValue = msg.data.get();
        /* 0x00 */
        battery.alarms.levelOneCellVoltageTooHigh.value = bitRead(byteValue, 0);
        battery.alarms.levelTwoCellVoltageTooHigh.value = bitRead(byteValue, 1);
        battery.alarms.levelOneCellVoltageTooLow.value = bitRead(byteValue, 2);
        battery.alarms.levelTwoCellVoltageTooLow.value = bitRead(byteValue, 3);
        battery.alarms.levelOnePackVoltageTooHigh.value = bitRead(byteValue, 4);
        battery.alarms.levelTwoPackVoltageTooHigh.value = bitRead(byteValue, 5);
        battery.alarms.levelOnePackVoltageTooLow.value = bitRead(byteValue, 6);
        battery.alarms.levelTwoPackVoltageTooLow.value = bitRead(byteValue, 7);

        /* 0x01 */
        byteValue = msg.data.get(1);
        battery.alarms.levelOneChargeTempTooHigh.value = bitRead(byteValue, 0);
        battery.alarms.levelTwoChargeTempTooHigh.value = bitRead(byteValue, 1);
        battery.alarms.levelOneChargeTempTooLow.value = bitRead(byteValue, 2);
        battery.alarms.levelTwoChargeTempTooLow.value = bitRead(byteValue, 3);
        battery.alarms.levelOneDischargeTempTooHigh.value = bitRead(byteValue, 4);
        battery.alarms.levelTwoDischargeTempTooHigh.value = bitRead(byteValue, 5);
        battery.alarms.levelOneDischargeTempTooLow.value = bitRead(byteValue, 6);
        battery.alarms.levelTwoDischargeTempTooLow.value = bitRead(byteValue, 7);

        /* 0x02 */
        byteValue = msg.data.get(2);
        battery.alarms.levelOneChargeCurrentTooHigh.value = bitRead(byteValue, 0);
        battery.alarms.levelTwoChargeCurrentTooHigh.value = bitRead(byteValue, 1);
        battery.alarms.levelOneDischargeCurrentTooHigh.value = bitRead(byteValue, 2);
        battery.alarms.levelTwoDischargeCurrentTooHigh.value = bitRead(byteValue, 3);
        battery.alarms.levelOneStateOfChargeTooHigh.value = bitRead(byteValue, 4);
        battery.alarms.levelTwoStateOfChargeTooHigh.value = bitRead(byteValue, 5);
        battery.alarms.levelOneStateOfChargeTooLow.value = bitRead(byteValue, 6);
        battery.alarms.levelTwoStateOfChargeTooLow.value = bitRead(byteValue, 7);

        /* 0x03 */
        byteValue = msg.data.get(3);
        battery.alarms.levelOneCellVoltageDifferenceTooHigh.value = bitRead(byteValue, 0);
        battery.alarms.levelTwoCellVoltageDifferenceTooHigh.value = bitRead(byteValue, 1);
        battery.alarms.levelOneTempSensorDifferenceTooHigh.value = bitRead(byteValue, 2);
        battery.alarms.levelTwoTempSensorDifferenceTooHigh.value = bitRead(byteValue, 3);

        /* 0x04 */
        byteValue = msg.data.get(4);
        battery.alarms.chargeFETTemperatureTooHigh.value = bitRead(byteValue, 0);
        battery.alarms.dischargeFETTemperatureTooHigh.value = bitRead(byteValue, 1);
        battery.alarms.failureOfChargeFETTemperatureSensor.value = bitRead(byteValue, 2);
        battery.alarms.failureOfDischargeFETTemperatureSensor.value = bitRead(byteValue, 3);
        battery.alarms.failureOfChargeFETAdhesion.value = bitRead(byteValue, 4);
        battery.alarms.failureOfDischargeFETAdhesion.value = bitRead(byteValue, 5);
        battery.alarms.failureOfChargeFETTBreaker.value = bitRead(byteValue, 6);
        battery.alarms.failureOfDischargeFETBreaker.value = bitRead(byteValue, 7);

        /* 0x05 */
        byteValue = msg.data.get(5);
        battery.alarms.failureOfAFEAcquisitionModule.value = bitRead(byteValue, 0);
        battery.alarms.failureOfVoltageSensorModule.value = bitRead(byteValue, 1);
        battery.alarms.failureOfTemperatureSensorModule.value = bitRead(byteValue, 2);
        battery.alarms.failureOfEEPROMStorageModule.value = bitRead(byteValue, 3);
        battery.alarms.failureOfRealtimeClockModule.value = bitRead(byteValue, 4);
        battery.alarms.failureOfPrechargeModule.value = bitRead(byteValue, 5);
        battery.alarms.failureOfVehicleCommunicationModule.value = bitRead(byteValue, 6);
        battery.alarms.failureOfIntranetCommunicationModule.value = bitRead(byteValue, 7);

        /* 0x06 */
        byteValue = msg.data.get(6);
        battery.alarms.failureOfCurrentSensorModule.value = bitRead(byteValue, 0);
        battery.alarms.failureOfMainVoltageSensorModule.value = bitRead(byteValue, 1);
        battery.alarms.failureOfShortCircuitProtection.value = bitRead(byteValue, 2);
        battery.alarms.failureOfLowVoltageNoCharging.value = bitRead(byteValue, 3);
    }

    /*
     * void setBmsReset() throws IOException // 0x00 Reset the BMS {
     * dalyPort.sendCommand(prepareTXBuffer(DalyCommand.BMS_RESET));
     * 
     * try { dalyPort.receiveBytes(); } catch (final IOException e) { throw new
     * IOException("<DALY-BMS DEBUG> Send failed, can't verify BMS was reset!\n", e); } }
     */


    private boolean bitRead(final int value, final int index) {
        return (value >> index & 1) == 1;
    }
}
