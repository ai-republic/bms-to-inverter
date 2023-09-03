package com.airepublic.bmstoinverter.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.enterprise.inject.Produces;

public class EnergyStorageProducer {
    private final static Logger LOG = LoggerFactory.getLogger(EnergyStorageProducer.class);
    private static EnergyStorage energyStorage;

    public void init() {
        if (!System.getProperties().containsKey("numBatteryPacks")) {
            LOG.error("No system property \"numBatteryPacks\" defined via -DnumBatteryPacks=<value> or in 'pi.properties'");
            return;
        }

        final int numBatteryPacks = Integer.parseInt(System.getProperty("numBatteryPacks"));

        energyStorage = new EnergyStorage(numBatteryPacks);
    }


    @Produces
    public EnergyStorage getEnergyStorage() {
        if (energyStorage == null) {
            init();
        }

        return energyStorage;
    }

}
