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

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Vetoed;

import com.google.gson.Gson;

@Vetoed
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
     * Fills this {@link EnergyStorage} object from the specified a JSON string.
     *
     * @param json a string representing a {@link EnergyStorage} object
     */
    public void fromJson(final String json) {
        final EnergyStorage temp = gson.fromJson(json, EnergyStorage.class);
        batteryPacks.clear();
        batteryPacks.addAll(temp.getBatteryPacks());
    }

}