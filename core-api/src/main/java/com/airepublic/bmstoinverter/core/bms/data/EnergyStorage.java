package com.airepublic.bmstoinverter.core.bms.data;

import java.util.List;

import com.airepublic.bmstoinverter.core.Port;
import com.google.gson.Gson;

/**
 * This class holds the data of the all battery storage modules ({@link BatteryPack} of the system.
 */
public class EnergyStorage implements AutoCloseable {
    private transient final static Gson gson = new Gson();
    private BatteryPack[] batteryPacks;

    /**
     * Constructor.
     * 
     * @param numBatteryPacks the number of {@link BatteryPack}s in the system.
     * @param ports the port or ports to communicate to the {@link BatteryPack}s (can be one port
     *        for all or one port for each BMS)
     */
    public EnergyStorage(final int numBatteryPacks, final List<Port> ports) {
        batteryPacks = new BatteryPack[numBatteryPacks];

        if (ports == null || !(ports.size() == 1 || ports.size() == numBatteryPacks)) {
            throw new RuntimeException("Ports are not configured correctly in config.properties. Please make sure you have either one port configured for all BMS or for each BMS!");
        }

        for (int i = 0; i < numBatteryPacks; i++) {
            batteryPacks[i] = new BatteryPack(ports.size() == 1 ? ports.get(0) : ports.get(i));
        }
    }


    /**
     * Gets an array of all {@link BatteryPack}s.
     *
     * @return the battery packs
     */
    public BatteryPack[] getBatteryPacks() {
        return batteryPacks;
    }


    /**
     * Sets an array of all {@link BatteryPack}s.
     *
     * @param batteryPacks the battery packs to set
     */
    public void setBatteryPacks(final BatteryPack[] batteryPacks) {
        this.batteryPacks = batteryPacks;
    }


    /**
     * Gets the {@link BatteryPack} for the specified bms number..
     * 
     * @param bmsNo the bms number
     * @return the {@link BatteryPack}
     */
    public BatteryPack getBatteryPack(final int bmsNo) {
        return batteryPacks[bmsNo];
    }


    /**
     * Gets the number of {@link BatteryPack}s in the system.
     *
     * @return the number of {@link BatteryPack}s in the system
     */
    public int getBatteryPackCount() {
        return batteryPacks.length;
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


    @Override
    public void close() {
        for (final BatteryPack pack : batteryPacks) {
            try {
                pack.close();
            } catch (final Exception e) {
            }
        }
    }

}
