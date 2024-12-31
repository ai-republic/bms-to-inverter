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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.airepublic.bmstoinverter.core.AlarmLevel;

/**
 * Holds all the data of a set of battery cells - a battery pack - collected from the BMS.
 *
 * Comments specify the precision and units of the value.
 */
public class BatteryPack {
    public final Map<Alarm, AlarmLevel> alarms = new HashMap<>();
    /** Battery type: 0=lithium iron, 1=ternary lithium, 2=lithium titanate */
    public int type;
    /** Capacity of each cell (1mAh) */
    public int ratedCapacitymAh;
    /** Nominal cell voltage (1mV) */
    public int ratedCellmV;

    /** Maximum total voltage (0.1V) */
    public int maxPackVoltageLimit;
    /** Minimum total voltage (0.1V) */
    public int minPackVoltageLimit;

    /** Maximum total charge current (0.1A) */
    public int maxPackChargeCurrent;
    /** Maximum total discharge current (-0.1A) */
    public int maxPackDischargeCurrent;
    /** Maximum total charge voltage (0.1V) */
    public int maxChargeVoltage;

    /** Total pack voltage (0.1 V) */
    public int packVoltage;
    /** Current in (+) or out (-) of pack (0.1 A) */
    public int packCurrent;
    /** State Of Charge (0.1%) */
    public int packSOC = -1;
    /** State of Health (0.1%) */
    public int packSOH;

    /** Maximum cell voltage limit (1mV) */
    public int maxCellVoltageLimit;
    /** Minimum cell voltage limit (1mV) */
    public int minCellVoltageLimit;
    /** Maximum cell voltage (1mV) */
    public int maxCellmV;
    /** Number of cell with highest voltage */
    public int maxCellVNum;
    /** Minimum cell voltage (1mV) */
    public int minCellmV;
    /** Number of cell with lowest voltage */
    public int minCellVNum;
    /** Difference between min and max cell voltages */
    public int cellDiffmV;

    // data from 0x92
    /** Maximum temperature sensor reading (0.1C) */
    public int tempMax;
    /** Minimum temperature sensor reading (0.1C) */
    public int tempMin;
    /** Average of temp sensors (0.1C) */
    public int tempAverage;

    // data from 0x93
    /** charge/discharge status (0 idle/stationary, 1 charge, 2 discharge; 3 sleep) */
    public int chargeDischargeStatus = 0;
    /** charging MOSFET status */
    public boolean chargeMOSState;
    /** discharge MOSFET state */
    public boolean dischargeMOSState;
    /** force charging */
    public boolean forceCharge;

    /** BMS life (0~255 cycles)? */
    public int bmsHeartBeat;
    /** residual capacity mAH */
    public int remainingCapacitymAh;

    // data from 0x94
    /** Cell count */
    public int numberOfCells;
    /** Temp sensor count */
    public int numOfTempSensors;
    /** charger status 0 = disconnected 1 = connected */
    public boolean chargerState;
    /** Load Status 0=disconnected 1=connected */
    public boolean loadState;
    /** No information about this */
    public boolean dIO[] = new boolean[8];
    /** charge / discharge cycles */
    public int bmsCycles;

    // data from 0x95
    /** Store Cell Voltages (mV) */
    public int cellVmV[] = new int[1024];

    // data from 0x96
    /** array of cell Temperature sensors */
    public int cellTemperature[] = new int[1024];

    // data from 0x97
    /** boolean array of cell balance states */
    public boolean[] cellBalanceState = new boolean[1024];
    /** boolean is cell balance active */
    public boolean cellBalanceActive;

    //
    /** The manufacturer code */
    public String manufacturerCode = "";
    /** The hardware version */
    public String hardwareVersion = "";
    /** The software version */
    public String softwareVersion = "";

    /** The cell with the maximum temperature */
    public int tempMaxCellNum;
    /** The cell with the minimum temperature */
    public int tempMinCellNum;

    /** The maximum module voltage (0.001V) of a pack */
    public int maxModulemV;
    /** The minimum module voltage (0.001V) of a pack */
    public int minModulemV;
    /** The number of the pack with the maximum voltage */
    public int maxModulemVNum;
    /** The number of the pack with the minimum voltage */
    public int minModulemVNum;
    /** The maximum module temperature (0.1C) */
    public int maxModuleTemp;
    /** The minimum module temperature (0.1C) */
    public int minModuleTemp;
    /** The pack number with maximum module temperature */
    public int maxModuleTempNum;
    /** The pack number with minimum module temperature */
    public int minModuleTempNum;
    /** The number of battery modules in series */
    public int modulesInSeries;
    /** The number of cells in a module */
    public byte moduleNumberOfCells;
    /** The module voltage (1V) */
    public int moduleVoltage;
    /** The rated capacity of the module (1Ah) */
    public int moduleRatedCapacityAh;

    /**
     * Gets all {@link Alarm}s for the given levels.
     * 
     * @param levels the {@link AlarmLevel}
     * @return the matching {@link Alarm}s
     */
    public final Map<Alarm, AlarmLevel> getAlarms(final AlarmLevel... levels) {
        final Map<Alarm, AlarmLevel> result = new HashMap<>();

        for (final AlarmLevel level : levels) {
            result.putAll(alarms.entrySet().stream().filter(entry -> entry.getValue() == level).collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())));
        }

        return result;
    }


    /**
     * Sets the {@link Alarm} with its reported {@link AlarmLevel}.
     *
     * @param alarm the {@link Alarm}
     * @param level the {@link AlarmLevel}
     */
    public final void setAlarm(final Alarm alarm, final AlarmLevel level) {
        alarms.put(alarm, level);
    }


    /**
     * Gets the {@link AlarmLevel} for the specified {@link Alarm}.
     *
     * @param alarm the {@link Alarm} to get the {@link AlarmLevel} for
     * @return the {@link AlarmLevel} or null if not present
     */
    public AlarmLevel getAlarmLevel(final Alarm alarm) {
        final AlarmLevel level = alarms.get(alarm);

        if (level == null) {
            return AlarmLevel.NONE;
        }

        return level;
    }
}
