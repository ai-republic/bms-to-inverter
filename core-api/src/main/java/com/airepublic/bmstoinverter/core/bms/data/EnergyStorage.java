package com.airepublic.bmstoinverter.core.bms.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

/**
 * This class holds the data of the all battery storage modules ({@link BatteryPack} of the system.
 */
public class EnergyStorage {
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

}
