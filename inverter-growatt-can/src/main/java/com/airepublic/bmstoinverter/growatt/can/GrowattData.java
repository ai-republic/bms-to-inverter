package com.airepublic.bmstoinverter.growatt.can;

public class GrowattData {
    public char chargeVoltageSetpoint;
    public short dcChargeCurrentLimit;
    public short dcDischargeCurrentLimit;
    public char dischargeVoltageLimit;
    public char soc;
    public char soh;
    public short batteryVoltage;
    public short batteryCurrent;
    public short batteryTemperature;
    public int alarms;
    public int warnings;
    public boolean charging;
    /** charging MOSFET status */
    public boolean chargeMOSState;
    /** discharge MOSFET state */
    public boolean disChargeMOSState;
    /** boolean is cell balance active */
    public boolean cellBalanceActive;

}
