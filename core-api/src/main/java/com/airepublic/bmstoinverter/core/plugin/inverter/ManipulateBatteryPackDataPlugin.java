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
package com.airepublic.bmstoinverter.core.plugin.inverter;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.InverterPlugin;
import com.airepublic.bmstoinverter.core.PluginProperty;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * Manipulates the aggregated battery information for an {@link Inverter}. If its value is set the
 * aggregated battery value will be overwritten with the configured value.
 */
public class ManipulateBatteryPackDataPlugin extends InverterPlugin {
    public final static String PROPERTY_PRESET_SOC = "SOC";
    public final static String PROPERTY_PRESET_SOH = "SOH";
    public final static String PROPERTY_PRESET_CURRENT = "Current";
    public final static String PROPERTY_PRESET_VOLTAGE = "Voltage";
    public final static String PROPERTY_PRESET_MAX_CHARGE_CURRENT = "Max. charge current";
    public final static String PROPERTY_PRESET_MAX_DISCHARGE_CURRENT = "Max. discharge current";
    public final static String PROPERTY_PRESET_MAX_VOLTAGE = "Max. voltage limit";
    public final static String PROPERTY_PRESET_MIN_VOLTAGE = "Min. voltage lime";
    public final static String PROPERTY_PRESET_AVG_TEMPERATURE = "Average Temperature";

    @Override
    public String getName() {
        return "ManipulateBatteryPackDataPlugin";
    }


    /**
     * Constructor.
     */
    public ManipulateBatteryPackDataPlugin() {
        addProperty(new PluginProperty(PROPERTY_PRESET_SOC, "", "The configured preset batterypack SOC (unit 0.1%)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_SOH, "", "The configured preset batterypack SOH (unit 0.1%)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_CURRENT, "", "The configured preset batterypack current (unit 0.1A)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_VOLTAGE, "", "The configured preset batterypack voltage (unit 0.1V)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_MAX_CHARGE_CURRENT, "", "The configured preset batterypack maximum charge current (unit 0.1A)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_MAX_DISCHARGE_CURRENT, "", "The configured preset batterypack maximum discharge current (unit 0.1A)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_MAX_VOLTAGE, "", "The configured preset batterypack maximum voltage limit (unit 0.1V)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_MIN_VOLTAGE, "", "The configured preset batterypack minimum voltage limit (unit 0.1V)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_AVG_TEMPERATURE, "", "The configured preset batterypack average temperature (unit 0.1C)"));
    }


    @Override
    public void onBatteryAggregation(final BatteryPack aggregatedPack) {
        String value = getPropertyValue(PROPERTY_PRESET_SOC, "");

        if (value != null && !value.trim().isEmpty()) {
            aggregatedPack.packSOC = Integer.parseInt(value);
        }

        value = getPropertyValue(PROPERTY_PRESET_SOH, "");

        if (value != null && !value.trim().isEmpty()) {
            aggregatedPack.packSOH = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_SOH, ""));
        }

        value = getPropertyValue(PROPERTY_PRESET_CURRENT, "");

        if (value != null && !value.trim().isEmpty()) {
            aggregatedPack.packCurrent = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_CURRENT, ""));
        }

        value = getPropertyValue(PROPERTY_PRESET_VOLTAGE, "");

        if (value != null && !value.trim().isEmpty()) {
            aggregatedPack.packVoltage = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_VOLTAGE, ""));
        }

        value = getPropertyValue(PROPERTY_PRESET_MAX_CHARGE_CURRENT, "");

        if (value != null && !value.trim().isEmpty()) {
            aggregatedPack.maxPackChargeCurrent = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_MAX_CHARGE_CURRENT, ""));
        }

        value = getPropertyValue(PROPERTY_PRESET_MAX_DISCHARGE_CURRENT, "");

        if (value != null && !value.trim().isEmpty()) {
            aggregatedPack.maxPackDischargeCurrent = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_MAX_DISCHARGE_CURRENT, ""));
        }
        value = getPropertyValue(PROPERTY_PRESET_MAX_VOLTAGE, "");

        if (value != null && !value.trim().isEmpty()) {
            aggregatedPack.maxPackVoltageLimit = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_MAX_VOLTAGE, ""));
        }

        value = getPropertyValue(PROPERTY_PRESET_MIN_VOLTAGE, "");

        if (value != null && !value.trim().isEmpty()) {
            aggregatedPack.minPackVoltageLimit = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_MIN_VOLTAGE, ""));
        }

        value = getPropertyValue(PROPERTY_PRESET_AVG_TEMPERATURE, "");
        if (value != null && !value.trim().isEmpty()) {
            aggregatedPack.tempAverage = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_AVG_TEMPERATURE, ""));
        }
    }
}
