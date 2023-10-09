package com.airepublic.bmstoinverter.daly.common;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

public class DalyMessageHandler {
    private final static Logger LOG = LoggerFactory.getLogger(DalyMessageHandler.class);
    private final static int MIN_NUMBER_CELLS = 1;
    private final static int MAX_NUMBER_CELLS = 48;
    private final static int MIN_NUMBER_TEMP_SENSORS = 1;
    private final static int MAX_NUMBER_TEMP_SENSORS = 16;

    private final EnergyStorage energyStorage;

    public DalyMessageHandler(final EnergyStorage energyStorage) {
        this.energyStorage = energyStorage;
    }


    public void handleMessage(final DalyMessage msg) {
        try {
            switch (msg.dataId) {
                case (byte) 0x5A:
                    getPackVoltageLimits(msg);
                case (byte) 0x5B:
                    getPackDischargeChargeLimits(msg);
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


    protected void getPackDischargeChargeLimits(final DalyMessage msg) {
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.error("getPackMeasurements -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        battery.maxPackDischargeCurrent = msg.data.getShort();
        // skip the next 2 bytes because only reading level 1
        msg.data.getShort();
        battery.maxPackChargeCurrent = msg.data.getShort();
    }


    protected void getPackVoltageLimits(final DalyMessage msg) {
        final int batteryNo = msg.address - 1;

        if (batteryNo < 0 || batteryNo >= energyStorage.getBatteryPackCount()) {
            LOG.error("getPackMeasurements -> Found invalid battery identifier: #{}", msg.address);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);

        battery.maxPackVoltageLimit = msg.data.getShort();
        // skip the next 2 bytes because only reading level 1
        msg.data.getShort();
        battery.minPackVoltageLimit = msg.data.getShort();
    }


    void getPackMeasurements(final DalyMessage msg) throws IOException { // 0x90
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


    void getMinMaxCellVoltage(final DalyMessage msg) throws IOException { // 0x91
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
        battery.cellDiff = battery.maxCellmV - battery.minCellmV;

        if (LOG.isDebugEnabled()) {
            LOG.info("Battery {} Min/Max/Diff:\n"
                    + "\tMax Voltage: Cell {}({}mV)\n"
                    + "\tMin Voltage: Cell {}({}mV)\n"
                    + "\tDifference: {}mV",
                    batteryNo + 1, battery.maxCellVNum, battery.maxCellmV, battery.minCellVNum, battery.minCellmV, battery.cellDiff);
        }
    }


    void getPackTemp(final DalyMessage msg) throws IOException { // 0x92
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


    void getDischargeChargeMosStatus(final DalyMessage msg) throws IOException { // 0x93
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


    void getStatusInfo(final DalyMessage msg) throws IOException { // 0x94
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


    void getCellVoltages(final DalyMessage msg) throws IOException { // 0x95
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


    void getCellTemperature(final DalyMessage msg) throws IOException { // 0x96
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


    void getCellBalanceState(final DalyMessage msg) throws IOException { // 0x97
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


    void getFailureCodes(final DalyMessage msg) throws IOException // 0x98
    {
        final int batteryNo = msg.address - 1;

        if (batteryNo + 1 > energyStorage.getBatteryPackCount()) {
            LOG.debug("getFailureCodes -> Found invalid battery identifier: #{}", batteryNo + 1);
            return;
        }

        final BatteryPack battery = energyStorage.getBatteryPack(batteryNo);
        byte byteValue = msg.data.get();
        /* 0x00 */
        battery.alarms.put("levelOneCellVoltageTooHigh", bitRead(byteValue, 0));
        battery.alarms.put("levelTwoCellVoltageTooHigh", bitRead(byteValue, 1));
        battery.alarms.put("levelOneCellVoltageTooLow", bitRead(byteValue, 2));
        battery.alarms.put("levelTwoCellVoltageTooLow", bitRead(byteValue, 3));
        battery.alarms.put("levelOnePackVoltageTooHigh", bitRead(byteValue, 4));
        battery.alarms.put("levelTwoPackVoltageTooHigh", bitRead(byteValue, 5));
        battery.alarms.put("levelOnePackVoltageTooLow", bitRead(byteValue, 6));
        battery.alarms.put("levelTwoPackVoltageTooLow", bitRead(byteValue, 7));

        /* 0x01 */
        byteValue = msg.data.get(1);
        battery.alarms.put("levelOneChargeTempTooHigh", bitRead(byteValue, 1));
        battery.alarms.put("levelTwoChargeTempTooHigh", bitRead(byteValue, 1));
        battery.alarms.put("levelOneChargeTempTooLow", bitRead(byteValue, 1));
        battery.alarms.put("levelTwoChargeTempTooLow", bitRead(byteValue, 1));
        battery.alarms.put("levelOneDischargeTempTooHigh", bitRead(byteValue, 1));
        battery.alarms.put("levelTwoDischargeTempTooHigh", bitRead(byteValue, 1));
        battery.alarms.put("levelOneDischargeTempTooLow", bitRead(byteValue, 1));
        battery.alarms.put("levelTwoDischargeTempTooLow", bitRead(byteValue, 1));

        /* 0x02 */
        byteValue = msg.data.get(2);
        battery.alarms.put("levelOneChargeCurrentTooHigh", bitRead(byteValue, 0));
        battery.alarms.put("levelTwoChargeCurrentTooHigh", bitRead(byteValue, 1));
        battery.alarms.put("levelOneDischargeCurrentTooHigh", bitRead(byteValue, 2));
        battery.alarms.put("levelTwoDischargeCurrentTooHigh", bitRead(byteValue, 3));
        battery.alarms.put("levelOneStateOfChargeTooHigh", bitRead(byteValue, 4));
        battery.alarms.put("levelTwoStateOfChargeTooHigh", bitRead(byteValue, 5));
        battery.alarms.put("levelOneStateOfChargeTooLow", bitRead(byteValue, 6));
        battery.alarms.put("levelTwoStateOfChargeTooLow", bitRead(byteValue, 7));

        /* 0x03 */
        byteValue = msg.data.get(3);
        battery.alarms.put("levelOneCellVoltageDifferenceTooHigh", bitRead(byteValue, 0));
        battery.alarms.put("levelTwoCellVoltageDifferenceTooHigh", bitRead(byteValue, 1));
        battery.alarms.put("levelOneTempSensorDifferenceTooHigh", bitRead(byteValue, 2));
        battery.alarms.put("levelTwoTempSensorDifferenceTooHigh", bitRead(byteValue, 3));

        /* 0x04 */
        byteValue = msg.data.get(4);
        battery.alarms.put("chargeFETTemperatureTooHigh", bitRead(byteValue, 0));
        battery.alarms.put("dischargeFETTemperatureTooHigh", bitRead(byteValue, 1));
        battery.alarms.put("failureOfChargeFETTemperatureSensor", bitRead(byteValue, 2));
        battery.alarms.put("failureOfDischargeFETTemperatureSensor", bitRead(byteValue, 3));
        battery.alarms.put("failureOfChargeFETAdhesion", bitRead(byteValue, 4));
        battery.alarms.put("failureOfDischargeFETAdhesion", bitRead(byteValue, 5));
        battery.alarms.put("failureOfChargeFETTBreaker", bitRead(byteValue, 6));
        battery.alarms.put("failureOfDischargeFETBreaker", bitRead(byteValue, 7));

        /* 0x05 */
        byteValue = msg.data.get(5);
        battery.alarms.put("failureOfAFEAcquisitionModule", bitRead(byteValue, 0));
        battery.alarms.put("failureOfVoltageSensorModule", bitRead(byteValue, 1));
        battery.alarms.put("failureOfTemperatureSensorModule", bitRead(byteValue, 2));
        battery.alarms.put("failureOfEEPROMStorageModule", bitRead(byteValue, 3));
        battery.alarms.put("failureOfRealtimeClockModule", bitRead(byteValue, 4));
        battery.alarms.put("failureOfPrechargeModule", bitRead(byteValue, 5));
        battery.alarms.put("failureOfVehicleCommunicationModule", bitRead(byteValue, 6));
        battery.alarms.put("failureOfIntranetCommunicationModule", bitRead(byteValue, 7));

        /* 0x06 */
        byteValue = msg.data.get(6);
        battery.alarms.put("failureOfCurrentSensorModule", bitRead(byteValue, 0));
        battery.alarms.put("failureOfMainVoltageSensorModule", bitRead(byteValue, 1));
        battery.alarms.put("failureOfShortCircuitProtection", bitRead(byteValue, 2));
        battery.alarms.put("failureOfLowVoltageNoCharging", bitRead(byteValue, 3));
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
