package com.airepublic.bmstoinverter.core.bms.data;

/**
 * Alarms holds alarm and warning flags for all possible errors/warnings from the BMS.
 */
public class Alarms {
    // data from 0x98
    /* 0x00 */
    public Alarm levelOneCellVoltageTooHigh = new Alarm("levelOneCellVoltageTooHigh", false);
    public Alarm levelTwoCellVoltageTooHigh = new Alarm("levelTwoCellVoltageTooHigh", false);
    public Alarm levelOneCellVoltageTooLow = new Alarm("levelOneCellVoltageTooLow", false);
    public Alarm levelTwoCellVoltageTooLow = new Alarm("levelTwoCellVoltageTooLow", false);
    public Alarm levelOnePackVoltageTooHigh = new Alarm("levelOnePackVoltageTooHigh", false);
    public Alarm levelTwoPackVoltageTooHigh = new Alarm("levelTwoPackVoltageTooHigh", false);
    public Alarm levelOnePackVoltageTooLow = new Alarm("levelOnePackVoltageTooLow", false);
    public Alarm levelTwoPackVoltageTooLow = new Alarm("levelTwoPackVoltageTooLow", false);

    /* 0x01 */
    public Alarm levelOneChargeTempTooHigh = new Alarm("levelOneChargeTempTooHigh", false);
    public Alarm levelTwoChargeTempTooHigh = new Alarm("levelTwoChargeTempTooHigh", false);
    public Alarm levelOneChargeTempTooLow = new Alarm("levelOneChargeTempTooLow", false);
    public Alarm levelTwoChargeTempTooLow = new Alarm("levelTwoChargeTempTooLow", false);
    public Alarm levelOneDischargeTempTooHigh = new Alarm("levelOneDischargeTempTooHigh", false);
    public Alarm levelTwoDischargeTempTooHigh = new Alarm("levelTwoDischargeTempTooHigh", false);
    public Alarm levelOneDischargeTempTooLow = new Alarm("levelOneDischargeTempTooLow", false);
    public Alarm levelTwoDischargeTempTooLow = new Alarm("levelTwoDischargeTempTooLow", false);

    /* 0x02 */
    public Alarm levelOneChargeCurrentTooHigh = new Alarm("levelOneChargeCurrentTooHigh", false);
    public Alarm levelTwoChargeCurrentTooHigh = new Alarm("levelTwoChargeCurrentTooHigh", false);
    public Alarm levelOneDischargeCurrentTooHigh = new Alarm("levelOneDischargeCurrentTooHigh", false);
    public Alarm levelTwoDischargeCurrentTooHigh = new Alarm("levelTwoDischargeCurrentTooHigh", false);
    public Alarm levelOneStateOfChargeTooHigh = new Alarm("levelOneStateOfChargeTooHigh", false);
    public Alarm levelTwoStateOfChargeTooHigh = new Alarm("levelTwoStateOfChargeTooHigh", false);
    public Alarm levelOneStateOfChargeTooLow = new Alarm("levelOneStateOfChargeTooLow", false);
    public Alarm levelTwoStateOfChargeTooLow = new Alarm("levelTwoStateOfChargeTooLow", false);

    /* 0x03 */
    public Alarm levelOneCellVoltageDifferenceTooHigh = new Alarm("levelOneCellVoltageDifferenceTooHigh", false);
    public Alarm levelTwoCellVoltageDifferenceTooHigh = new Alarm("levelTwoCellVoltageDifferenceTooHigh", false);
    public Alarm levelOneTempSensorDifferenceTooHigh = new Alarm("levelOneTempSensorDifferenceTooHigh", false);
    public Alarm levelTwoTempSensorDifferenceTooHigh = new Alarm("levelTwoTempSensorDifferenceTooHigh", false);

    /* 0x04 */
    public Alarm chargeFETTemperatureTooHigh = new Alarm("chargeFETTemperatureTooHigh", false);
    public Alarm dischargeFETTemperatureTooHigh = new Alarm("dischargeFETTemperatureTooHigh", false);
    public Alarm failureOfChargeFETTemperatureSensor = new Alarm("failureOfChargeFETTemperatureSensor", false);
    public Alarm failureOfDischargeFETTemperatureSensor = new Alarm("failureOfDischargeFETTemperatureSensor", false);
    public Alarm failureOfChargeFETAdhesion = new Alarm("failureOfChargeFETAdhesion", false);
    public Alarm failureOfDischargeFETAdhesion = new Alarm("failureOfDischargeFETAdhesion", false);
    public Alarm failureOfChargeFETTBreaker = new Alarm("failureOfChargeFETTBreaker", false);
    public Alarm failureOfDischargeFETBreaker = new Alarm("failureOfDischargeFETBreaker", false);

    /* 0x05 */
    public Alarm failureOfAFEAcquisitionModule = new Alarm("failureOfAFEAcquisitionModule", false);
    public Alarm failureOfVoltageSensorModule = new Alarm("failureOfVoltageSensorModule", false);
    public Alarm failureOfTemperatureSensorModule = new Alarm("failureOfTemperatureSensorModule", false);
    public Alarm failureOfEEPROMStorageModule = new Alarm("failureOfEEPROMStorageModule", false);
    public Alarm failureOfRealtimeClockModule = new Alarm("failureOfRealtimeClockModule", false);
    public Alarm failureOfPrechargeModule = new Alarm("failureOfPrechargeModule", false);
    public Alarm failureOfInternalCommunicationModule = new Alarm("failureOfInternalCommunicationModule", false);
    public Alarm failureOfIntranetCommunicationModule = new Alarm("failureOfIntranetCommunicationModule", false);

    /* 0x06 */
    public Alarm failureOfCurrentSensorModule = new Alarm("failureOfCurrentSensorModule", false);
    public Alarm failureOfMainVoltageSensorModule = new Alarm("failureOfMainVoltageSensorModule", false);
    public Alarm failureOfShortCircuitProtection = new Alarm("failureOfShortCircuitProtection", false);
    public Alarm failureOfLowVoltageNoCharging = new Alarm("failureOfLowVoltageNoCharging", false);
}
