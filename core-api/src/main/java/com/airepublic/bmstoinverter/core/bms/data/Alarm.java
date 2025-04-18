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
package com.airepublic.bmstoinverter.core.bms.data;

import com.airepublic.bmstoinverter.core.BMS;

/**
 * Alarms holds alarm and warning flags for all possible errors/warnings from a {@link BMS}.
 */
public enum Alarm {
    CELL_VOLTAGE_HIGH,
    CELL_VOLTAGE_LOW,
    CELL_VOLTAGE_DIFFERENCE_HIGH,
    CELL_TEMPERATURE_HIGH,
    CELL_TEMPERATURE_LOW,

    PACK_VOLTAGE_HIGH,
    PACK_VOLTAGE_LOW,
    PACK_CURRENT_HIGH,
    PACK_CURRENT_LOW,
    PACK_TEMPERATURE_HIGH,
    PACK_TEMPERATURE_LOW,

    CHARGE_CURRENT_HIGH,
    CHARGE_VOLTAGE_HIGH,
    CHARGE_VOLTAGE_LOW,
    CHARGE_TEMPERATURE_HIGH,
    CHARGE_TEMPERATURE_LOW,
    CHARGE_MODULE_TEMPERATURE_HIGH,

    DISCHARGE_CURRENT_HIGH,
    DISCHARGE_VOLTAGE_HIGH,
    DISCHARGE_VOLTAGE_LOW,
    DISCHARGE_TEMPERATURE_HIGH,
    DISCHARGE_TEMPERATURE_LOW,
    DISCHARGE_MODULE_TEMPERATURE_HIGH,

    SOC_HIGH,
    SOC_LOW,

    ENCASING_TEMPERATURE_HIGH,
    ENCASING_TEMPERATURE_LOW,

    TEMPERATURE_SENSOR_DIFFERENCE_HIGH,

    FAILURE_SENSOR_CELL_TEMPERATURE,
    FAILURE_SENSOR_PACK_TEMPERATURE,
    FAILURE_SENSOR_CHARGE_MODULE_TEMPERATURE,
    FAILURE_SENSOR_DISCHARGE_MODULE_TEMPERATURE,
    FAILURE_SENSOR_PACK_VOLTAGE,
    FAILURE_SENSOR_PACK_CURRENT,

    FAILURE_COMMUNICATION_INTERNAL,
    FAILURE_COMMUNICATION_EXTERNAL,
    FAILURE_CLOCK_MODULE,
    FAILURE_CHARGE_BREAKER,
    FAILURE_DISCHARGE_BREAKER,
    FAILURE_SHORT_CIRCUIT_PROTECTION,
    FAILURE_EEPROM_MODULE,
    FAILURE_PRECHARGE_MODULE,
    FAILURE_NOT_CHARGING_DUE_TO_LOW_VOLTAGE,

    FAILURE_OTHER;
}
