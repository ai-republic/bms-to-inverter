package com.airepublic.bmstoinverter.daly.common;

public interface DalyCommand {
    int READ_MIN_MAX_PACK_VOLTAGE = 0x5A;
    int READ_MAX_PACK_DISCHARGE_CHARGE_CURRENT = 0x5B;
    int READ_VOUT_IOUT_SOC = 0x90;
    int READ_MIN_MAX_CELL_VOLTAGE = 0x91;
    int READ_MIN_MAX_TEMPERATURE = 0x92;
    int READ_DISCHARGE_CHARGE_MOS_STATUS = 0x93;
    int READ_STATUS_INFO = 0x94;
    int READ_CELL_VOLTAGES = 0x95;
    int READ_CELL_TEMPERATURE = 0x96;
    int READ_CELL_BALANCE_STATE = 0x97;
    int READ_FAILURE_CODES = 0x98;

    int WRITE_RTC_AND_SOC = 0x21; // bytes: YY MM DD hh mm ss soc_hi soc_low (0.1%)
    int WRITE_DISCHRG_FET = 0xD9;
    int WRITE_CHRG_FET = 0xDA;
    int WRITE_BMS_RESET = 0x00;
}