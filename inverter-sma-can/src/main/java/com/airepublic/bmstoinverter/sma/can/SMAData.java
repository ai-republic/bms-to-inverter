package com.airepublic.bmstoinverter.sma.can;

public class SMAData {
    public char chargeVoltageSetpoint; // 0.1V
    public short dcChargeCurrentLimit; // 0.1A
    public short dcDischargeCurrentLimit; // 0.1A
    public char dischargeVoltageLimit; // 0.1V
    public char soc; // 1%
    public char soh; // 1%
    public short batteryVoltage; // 0.01V
    public short batteryCurrent; // 0.1A
    public short batteryTemperature; // 0.1degC
    public int alarms;
    public int warnings;

}
