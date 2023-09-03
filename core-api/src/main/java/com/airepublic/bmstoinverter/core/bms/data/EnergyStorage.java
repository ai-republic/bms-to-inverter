package com.airepublic.bmstoinverter.core.bms.data;

import com.google.gson.Gson;

public class EnergyStorage {
    private final static Gson gson = new Gson();
    private BatteryPack[] batteryPacks;

    public EnergyStorage(final int numBatteryPacks) {
        batteryPacks = new BatteryPack[numBatteryPacks];

        for (int i = 0; i < numBatteryPacks; i++) {
            batteryPacks[i] = new BatteryPack(i);
        }
    }


    /**
     * @return the batteryPacks
     */
    public BatteryPack[] getBatteryPacks() {
        return batteryPacks;
    }


    /**
     * @param batteryPacks the batteryPacks to set
     */
    public void setBatteryPacks(final BatteryPack[] batteryPacks) {
        this.batteryPacks = batteryPacks;
    }


    public BatteryPack getBatteryPack(final int no) {
        return batteryPacks[no];
    }


    public int getBatteryPackCount() {
        return batteryPacks.length;
    }


    public String toJson() {
        return gson.toJson(this);
    }


    public BatteryPack fromJson(final String json) {
        return gson.fromJson(json, BatteryPack.class);
    }

}
