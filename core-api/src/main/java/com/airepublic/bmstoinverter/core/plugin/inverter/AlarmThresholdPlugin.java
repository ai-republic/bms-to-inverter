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

import java.util.HashMap;
import java.util.Map;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.InverterPlugin;
import com.airepublic.bmstoinverter.core.PluginProperty;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * Sets an alarm on the aggregated battery pack if a configured threshold of battery packs
 * from the EnergyStorage have active alarms. This plugin reduces noise by only reporting
 * system-wide alarms when multiple packs are affected. Separate thresholds are maintained
 * for WARNING and ALARM levels.
 */
public class AlarmThresholdPlugin extends InverterPlugin {
    public final static String PROPERTY_PACK_WARNING_THRESHOLD = "Pack warning threshold";
    public final static String PROPERTY_PACK_ALARM_THRESHOLD = "Pack alarm threshold";
    
    private Inverter inverter;
    
    @Override
    public String getName() {
        return "AlarmThresholdPlugin";
    }


    /**
     * Constructor.
     */
    public AlarmThresholdPlugin() {
        addProperty(new PluginProperty(PROPERTY_PACK_WARNING_THRESHOLD, "1", "The number of packs with WARNING alarms required to trigger a warning (integer)"));
        addProperty(new PluginProperty(PROPERTY_PACK_ALARM_THRESHOLD, "1", "The number of packs with ALARM level alarms required to trigger an alarm (integer)"));
    }


    @Override
    public void onInitialize(final Inverter device) {
        this.inverter = device;
    }


    @Override
    public void onBatteryAggregation(final BatteryPack aggregatedPack) {
        if (inverter == null || inverter.getEnergyStorage() == null) {
            return;
        }

        // Get the configured thresholds
        String warningThresholdValue = getPropertyValue(PROPERTY_PACK_WARNING_THRESHOLD, "1");
        String alarmThresholdValue = getPropertyValue(PROPERTY_PACK_ALARM_THRESHOLD, "1");
        
        int warningThreshold = 1;
        int alarmThreshold = 1;
        
        try {
            warningThreshold = Integer.parseInt(warningThresholdValue);
        } catch (final NumberFormatException e) {
            warningThreshold = 1;
        }
        
        try {
            alarmThreshold = Integer.parseInt(alarmThresholdValue);
        } catch (final NumberFormatException e) {
            alarmThreshold = 1;
        }

        // Count alarms separately by level
        int packsWithWarnings = 0;
        int packsWithAlarms = 0;
        final Map<Alarm, Integer> warningAlarmCounts = new HashMap<>();
        final Map<Alarm, Integer> alarmAlarmCounts = new HashMap<>();

        for (final BatteryPack pack : inverter.getEnergyStorage().getBatteryPacks()) {
            if (pack != null && pack.alarms != null && !pack.alarms.isEmpty()) {
                boolean hasWarning = false;
                boolean hasAlarm = false;
                
                for (final Map.Entry<Alarm, AlarmLevel> entry : pack.alarms.entrySet()) {
                    if (entry.getValue() == AlarmLevel.WARNING) {
                        hasWarning = true;
                        // Count this warning alarm
                        warningAlarmCounts.put(entry.getKey(), warningAlarmCounts.getOrDefault(entry.getKey(), 0) + 1);
                    } else if (entry.getValue() == AlarmLevel.ALARM) {
                        hasAlarm = true;
                        // Count this alarm
                        alarmAlarmCounts.put(entry.getKey(), alarmAlarmCounts.getOrDefault(entry.getKey(), 0) + 1);
                    }
                }
                
                if (hasWarning) {
                    packsWithWarnings++;
                }
                if (hasAlarm) {
                    packsWithAlarms++;
                }
            }
        }

        // If alarm threshold is reached, set the most common ALARM on the aggregated pack
        if (packsWithAlarms >= alarmThreshold && !alarmAlarmCounts.isEmpty()) {
            // Find the alarm with the highest count
            Alarm mostCommonAlarm = alarmAlarmCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Alarm.FAILURE_OTHER);

            aggregatedPack.setAlarm(mostCommonAlarm, AlarmLevel.ALARM);
        }
        // Otherwise, if warning threshold is reached, set the most common WARNING on the aggregated pack
        else if (packsWithWarnings >= warningThreshold && !warningAlarmCounts.isEmpty()) {
            // Find the alarm with the highest count
            Alarm mostCommonAlarm = warningAlarmCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Alarm.FAILURE_OTHER);

            aggregatedPack.setAlarm(mostCommonAlarm, AlarmLevel.WARNING);
        }
    }
}
