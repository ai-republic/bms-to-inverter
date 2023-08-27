package com.airepublic.bmstoinverter.daly.can;

public interface DalyCommand {
    int VOUT_IOUT_SOC = 0x90;
    int MIN_MAX_CELL_VOLTAGE = 0x91;
    int MIN_MAX_TEMPERATURE = 0x92;
    int DISCHARGE_CHARGE_MOS_STATUS = 0x93;
    int STATUS_INFO = 0x94;
    int CELL_VOLTAGES = 0x95;
    int CELL_TEMPERATURE = 0x96;
    int CELL_BALANCE_STATE = 0x97;
    int FAILURE_CODES = 0x98;
    int DISCHRG_FET = 0xD9;
    int CHRG_FET = 0xDA;
    int BMS_RESET = 0x00;
}