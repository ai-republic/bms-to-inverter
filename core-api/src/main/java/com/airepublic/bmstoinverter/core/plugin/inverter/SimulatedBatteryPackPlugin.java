package com.airepublic.bmstoinverter.core.plugin.inverter;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.InverterPlugin;
import com.airepublic.bmstoinverter.core.PluginProperty;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * Checks the aggregated battery information of an {@link Inverter} if its SOC has already been read
 * (positive value). If its negative (not yet read), then the preset battery information will be
 * written to the aggregated battery information.
 */
public class SimulatedBatteryPackPlugin extends InverterPlugin {
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
        return "SimulatedBatteryPackPlugin";
    }


    /**
     * Constructor.
     */
    public SimulatedBatteryPackPlugin() {
        addProperty(new PluginProperty(PROPERTY_PRESET_SOC, "500", "The configured preset batterypack SOC (unit 0.1%)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_SOH, "990", "The configured preset batterypack SOH (unit 0.1%)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_CURRENT, "0", "The configured preset batterypack current (unit 0.1A)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_VOLTAGE, "520", "The configured preset batterypack voltage (unit 0.1V)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_MAX_CHARGE_CURRENT, "200", "The configured preset batterypack maximum charge current (unit 0.1A)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_MAX_DISCHARGE_CURRENT, "200", "The configured preset batterypack maximum discharge current (unit 0.1A)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_MAX_VOLTAGE, "540", "The configured preset batterypack maximum voltage limit (unit 0.1V)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_MIN_VOLTAGE, "480", "The configured preset batterypack minimum voltage limit (unit 0.1V)"));
        addProperty(new PluginProperty(PROPERTY_PRESET_AVG_TEMPERATURE, "250", "The configured preset batterypack average temperature (unit 0.1C)"));
    }


    @Override
    public void onBatteryAggregation(final BatteryPack aggregatedPack) {
        // set configured or default values
        aggregatedPack.packSOC = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_SOC, "500"));
        aggregatedPack.packSOH = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_SOH, "990"));
        aggregatedPack.packCurrent = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_CURRENT, "0"));
        aggregatedPack.packVoltage = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_VOLTAGE, "520"));
        aggregatedPack.maxPackChargeCurrent = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_MAX_CHARGE_CURRENT, "200"));
        aggregatedPack.maxPackDischargeCurrent = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_MAX_DISCHARGE_CURRENT, "200"));
        aggregatedPack.maxPackVoltageLimit = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_MAX_VOLTAGE, "540"));
        aggregatedPack.minPackVoltageLimit = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_MIN_VOLTAGE, "480"));
        aggregatedPack.tempAverage = Integer.parseInt(getPropertyValue(PROPERTY_PRESET_AVG_TEMPERATURE, "250"));
    }
}
