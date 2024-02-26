package com.airepublic.bmstoinverter.core.bms.data;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * This class holds the data of the all battery storage modules ({@link BatteryPack} of the system.
 */
public class EnergyStorage {
    private final static transient Logger LOG = LoggerFactory.getLogger(EnergyStorage.class);
    private transient final static Gson gson = new Gson();
    private List<BatteryPack> batteryPacks = new ArrayList<>();

    /**
     * Constructor.
     */
    public EnergyStorage() {
    }


    /**
     * Gets an array of all {@link BatteryPack}s.
     *
     * @return the battery packs
     */
    public List<BatteryPack> getBatteryPacks() {
        return batteryPacks;
    }


    /**
     * Sets an array of all {@link BatteryPack}s.
     *
     * @param batteryPacks the battery packs to set
     */
    public void setBatteryPacks(final List<BatteryPack> batteryPacks) {
        this.batteryPacks = batteryPacks;
    }


    /**
     * Gets the {@link BatteryPack} for the specified bms number..
     * 
     * @param bmsNo the bms number
     * @return the {@link BatteryPack}
     */
    public BatteryPack getBatteryPack(final int bmsNo) {
        return batteryPacks.get(bmsNo);
    }


    /**
     * Gets the number of {@link BatteryPack}s in the system.
     *
     * @return the number of {@link BatteryPack}s in the system
     */
    public int getBatteryPackCount() {
        return batteryPacks.size();
    }


    /**
     * Creates a JSON string representation of this {@link EnergyStorage} object.
     *
     * @return a JSON string representation of this {@link EnergyStorage} object
     */
    public String toJson() {
        return gson.toJson(this);
    }


    /**
     * Creates a {@link BatteryPack} object from the specified a JSON string.
     *
     * @param json a string representing a {@link BatteryPack}
     * @return the {@link BatteryPack}
     */
    public BatteryPack fromJson(final String json) {
        return gson.fromJson(json, BatteryPack.class);
    }


    public BatteryPack getAggregatedBatteryInfo() {
        final BatteryPack result = new BatteryPack();
        result.maxPackChargeCurrent = Integer.MAX_VALUE;
        result.maxPackDischargeCurrent = Integer.MAX_VALUE;

        // sum all values
        for (final BatteryPack pack : getBatteryPacks()) {
            result.ratedCapacitymAh += pack.ratedCapacitymAh;
            result.ratedCellmV += pack.ratedCellmV;
            result.maxPackVoltageLimit += pack.maxPackVoltageLimit;
            result.minPackVoltageLimit += pack.minPackVoltageLimit;
            result.maxPackChargeCurrent = Math.min(result.maxPackChargeCurrent, pack.maxPackChargeCurrent);
            result.maxPackDischargeCurrent = Math.min(result.maxPackDischargeCurrent, pack.maxPackDischargeCurrent);
            result.packVoltage += pack.packVoltage;
            result.packCurrent += pack.packCurrent;
            result.packSOC += pack.packSOC;
            result.packSOH += pack.packSOH;
            result.maxCellmV = Math.max(result.maxCellmV, pack.maxCellmV);
            result.maxCellVNum = pack.maxCellmV == result.maxCellmV ? pack.maxCellVNum : result.maxCellVNum;
            result.minCellmV = Math.min(result.minCellmV, pack.minCellmV);
            result.minCellVNum = pack.minCellmV == result.minCellmV ? pack.minCellVNum : result.minCellVNum;
            result.tempMax = Math.max(result.tempMax, pack.tempMax);
            result.tempMin = Math.min(result.tempMin, pack.tempMin);

            // result.chargeDischargeStatus = pack.chargeDischargeStatus;
            result.chargeMOSState |= pack.chargeMOSState;
            result.disChargeMOSState |= pack.disChargeMOSState;
            result.forceCharge |= pack.forceCharge;
            result.remainingCapacitymAh += pack.remainingCapacitymAh;
            result.numberOfCells += pack.numberOfCells;
            result.chargeState |= pack.chargeState;
            result.loadState |= pack.loadState;
            result.bmsCycles = Math.max(result.bmsCycles, pack.bmsCycles);
            // cellVmV
            // cellTemperature
            // cellBalanceState
            result.cellBalanceActive |= pack.cellBalanceActive;

            aggregateAlarms(result, pack.alarms);

            result.tempMaxCellNum = Math.max(result.tempMaxCellNum, pack.tempMaxCellNum);
            result.tempMinCellNum = Math.min(result.tempMinCellNum, pack.tempMinCellNum);
            result.maxModulemV = Math.max(result.maxModulemV, pack.maxModulemV);
            result.minModulemV = Math.min(result.minModulemV, pack.minModulemV);
            result.maxModulemVNum = pack.maxModulemV == result.maxModulemV ? pack.maxModulemVNum : result.maxModulemVNum;
            result.minModulemVNum = pack.minModulemV == result.minModulemV ? pack.minModulemVNum : result.minModulemVNum;
            result.maxModuleTemp = Math.max(result.maxModuleTemp, pack.maxModuleTemp);
            result.minModuleTemp = Math.min(result.minModuleTemp, pack.minModuleTemp);
            result.maxModuleTempNum = pack.maxModuleTemp == result.maxModuleTemp ? pack.maxModuleTempNum : result.maxModuleTempNum;
            result.minModuleTempNum = pack.minModuleTemp == result.minModuleTemp ? pack.minModuleTempNum : result.minModuleTempNum;
            result.modulesInSeries += pack.modulesInSeries;
            result.moduleNumberOfCells += pack.moduleNumberOfCells;
            result.moduleVoltage += pack.moduleVoltage;
            result.moduleRatedCapacityAh += pack.moduleRatedCapacityAh;
        }

        // calculate averages
        final int count = getBatteryPackCount();
        result.ratedCapacitymAh = result.ratedCapacitymAh / count;
        result.ratedCellmV = result.ratedCellmV / count;
        result.maxPackVoltageLimit = result.maxPackVoltageLimit / count;
        result.minPackVoltageLimit = result.minPackVoltageLimit / count;
        result.packVoltage = result.packVoltage / count;
        result.packSOC = result.packSOC / count;
        result.packSOH = result.packSOH / count;
        result.tempAverage = result.tempAverage / count;
        result.bmsCycles = result.bmsCycles / count;
        result.moduleVoltage = result.moduleVoltage / count;
        result.moduleRatedCapacityAh = result.moduleRatedCapacityAh / count;

        // other calculations
        result.cellDiffmV = result.maxCellmV - result.minCellmV;
        result.type = getBatteryPack(0).type;
        result.manufacturerCode = getBatteryPack(0).manufacturerCode;
        result.hardwareVersion = getBatteryPack(0).hardwareVersion;
        result.softwareVersion = getBatteryPack(0).softwareVersion;

        return result;
    }


    private void aggregateAlarms(final BatteryPack result, final Alarms alarms) {
        try {
            for (final Field field : Alarms.class.getFields()) {
                if (Alarm.class.equals(field.getType())) {
                    final Alarm alarm = (Alarm) field.get(alarms);
                    final Alarm alarmResult = (Alarm) field.get(result.alarms);

                    if (alarm.value) {
                        alarmResult.value = true;
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Error aggregating alarms!", e);
        }
    }
}
