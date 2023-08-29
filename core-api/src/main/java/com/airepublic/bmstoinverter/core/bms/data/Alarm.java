package com.airepublic.bmstoinverter.core.bms.data;

/**
 * alarm struct holds booleaneans corresponding to all the possible alarms (aka errors/warnings) the
 * BMS can report
 */
public class Alarm {
    // data from 0x98
    /* 0x00 */
    public boolean levelOneCellVoltageTooHigh;
    public boolean levelTwoCellVoltageTooHigh;
    public boolean levelOneCellVoltageTooLow;
    public boolean levelTwoCellVoltageTooLow;
    public boolean levelOnePackVoltageTooHigh;
    public boolean levelTwoPackVoltageTooHigh;
    public boolean levelOnePackVoltageTooLow;
    public boolean levelTwoPackVoltageTooLow;

    /* 0x01 */
    public boolean levelOneChargeTempTooHigh;
    public boolean levelTwoChargeTempTooHigh;
    public boolean levelOneChargeTempTooLow;
    public boolean levelTwoChargeTempTooLow;
    public boolean levelOneDischargeTempTooHigh;
    public boolean levelTwoDischargeTempTooHigh;
    public boolean levelOneDischargeTempTooLow;
    public boolean levelTwoDischargeTempTooLow;

    /* 0x02 */
    public boolean levelOneChargeCurrentTooHigh;
    public boolean levelTwoChargeCurrentTooHigh;
    public boolean levelOneDischargeCurrentTooHigh;
    public boolean levelTwoDischargeCurrentTooHigh;
    public boolean levelOneStateOfChargeTooHigh;
    public boolean levelTwoStateOfChargeTooHigh;
    public boolean levelOneStateOfChargeTooLow;
    public boolean levelTwoStateOfChargeTooLow;

    /* 0x03 */
    public boolean levelOneCellVoltageDifferenceTooHigh;
    public boolean levelTwoCellVoltageDifferenceTooHigh;
    public boolean levelOneTempSensorDifferenceTooHigh;
    public boolean levelTwoTempSensorDifferenceTooHigh;

    /* 0x04 */
    public boolean chargeFETTemperatureTooHigh;
    public boolean dischargeFETTemperatureTooHigh;
    public boolean failureOfChargeFETTemperatureSensor;
    public boolean failureOfDischargeFETTemperatureSensor;
    public boolean failureOfChargeFETAdhesion;
    public boolean failureOfDischargeFETAdhesion;
    public boolean failureOfChargeFETTBreaker;
    public boolean failureOfDischargeFETBreaker;

    /* 0x05 */
    public boolean failureOfAFEAcquisitionModule;
    public boolean failureOfVoltageSensorModule;
    public boolean failureOfTemperatureSensorModule;
    public boolean failureOfEEPROMStorageModule;
    public boolean failureOfRealtimeClockModule;
    public boolean failureOfPrechargeModule;
    public boolean failureOfVehicleCommunicationModule;
    public boolean failureOfIntranetCommunicationModule;

    /* 0x06 */
    public boolean failureOfCurrentSensorModule;
    public boolean failureOfMainVoltageSensorModule;
    public boolean failureOfShortCircuitProtection;
    public boolean failureOfLowVoltageNoCharging;
}
