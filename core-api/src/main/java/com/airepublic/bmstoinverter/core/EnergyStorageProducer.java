package com.airepublic.bmstoinverter.core;

import java.util.List;

import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Producer to create an {@link EnergyStorage} to use application wide.
 */
public class EnergyStorageProducer {
    @Inject
    private List<BMS> bmsList;
    private static EnergyStorage energyStorage;

    /**
     * Provides the application wide {@link EnergyStorage} object.
     *
     * @return the application wide {@link EnergyStorage} object
     */
    @Produces
    public EnergyStorage getEnergyStorage() {
        if (energyStorage == null) {
            final BatteryPack[] batteryPacks = new BatteryPack[bmsList.size()];

            for (int i = 0; i < batteryPacks.length; i++) {
                batteryPacks[i] = bmsList.get(i).getBatteryPack();
            }

            energyStorage = new EnergyStorage(batteryPacks);
        }

        return energyStorage;
    }

}
