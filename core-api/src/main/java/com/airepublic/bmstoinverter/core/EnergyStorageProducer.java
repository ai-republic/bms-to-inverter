package com.airepublic.bmstoinverter.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.enterprise.inject.Produces;

/**
 * Producer to create an {@link EnergyStorage} to use application wide.
 */
public class EnergyStorageProducer {
    private final static Logger LOG = LoggerFactory.getLogger(EnergyStorageProducer.class);
    private static EnergyStorage energyStorage;

    /**
     * Reads the <code>numBatteryPacks</code> from the <code>config.properties</code> and
     * initializes the {@link EnergyStorage} accordingly.
     */
    private void init() {
        if (!System.getProperties().containsKey("numBatteryPacks")) {
            LOG.error("No system property \"numBatteryPacks\" defined via -DnumBatteryPacks=<value> or in 'pi.properties'");
            return;
        }

        final int numBatteryPacks = Integer.parseInt(System.getProperty("numBatteryPacks"));

        energyStorage = new EnergyStorage(numBatteryPacks);
    }


    /**
     * Provides the application wide {@link EnergyStorage} object.
     *
     * @return the application wide {@link EnergyStorage} object
     */
    @Produces
    public EnergyStorage getEnergyStorage() {
        if (energyStorage == null) {
            init();
        }

        return energyStorage;
    }

}
