package com.airepublic.bmstoinverter.core.bms.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

/**
 * This class holds the data of the all battery storage modules ({@link BatteryPack} of the system.
 */
public class EnergyStorage {
    private transient final static Gson gson = new Gson();
    private final List<BatteryPack> batteryPacks = new ArrayList<>();

    /**
     * Constructor.
     */
    public EnergyStorage() {
    }


    /**
     * Gets all {@link BatteryPack}s.
     *
     * @return the battery packs
     */
    public List<BatteryPack> getBatteryPacks() {
        return batteryPacks;
    }


    /**
     * Gets the {@link BatteryPack} at the specified index.
     * 
     * @param index the index of the {@link BatteryPack}
     * @return the {@link BatteryPack}
     */
    public BatteryPack getBatteryPack(final int index) {
        return batteryPacks.get(index);
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
