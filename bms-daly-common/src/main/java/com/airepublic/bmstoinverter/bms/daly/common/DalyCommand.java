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

/**
 * The CAN/RS-485 commands for the Daly BMS.
 */
public enum DalyCommand {
    READ_RATED_CAPACITY_CELL_VOLTAGE(0x50),
    READ_BATTERY_TYPE_INFO(0x53),
    READ_MIN_MAX_PACK_VOLTAGE(0x5A),
    READ_MAX_PACK_DISCHARGE_CHARGE_CURRENT(0x5B),
    READ_VOUT_IOUT_SOC(0x90),
    READ_MIN_MAX_CELL_VOLTAGE(0x91),
    READ_MIN_MAX_TEMPERATURE(0x92),
    READ_DISCHARGE_CHARGE_MOS_STATUS(0x93),
    READ_STATUS_INFO(0x94),
    READ_CELL_VOLTAGES(0x95),
    READ_CELL_TEMPERATURE(0x96),
    READ_CELL_BALANCE_STATE(0x97),
    READ_FAILURE_CODES(0x98),

    WRITE_RTC_AND_SOC(0x21), // bytes: YY MM DD hh mm ss soc_hi soc_low (0.1%)
    WRITE_DISCHRG_FET(0xD9),
    WRITE_CHRG_FET(0xDA),
    WRITE_BMS_RESET(0x00);

    public int id;

    DalyCommand(final int id) {
        this.id = id;
    }


    /**
     * Trys to find the {@link DalyCommand} for the specified id.
     *
     * @param id the id of the {@link DalyCommand} to find
     * @return the {@link DalyCommand} for the id or <code>null</null>
     */
    public final static DalyCommand valueOf(final int id) {
        for (final DalyCommand cmd : DalyCommand.values()) {
            if (cmd.id == id) {
                return cmd;
            }
        }

        return null;
    }

}
