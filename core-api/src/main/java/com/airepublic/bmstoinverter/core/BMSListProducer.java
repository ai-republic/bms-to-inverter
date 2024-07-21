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
package com.airepublic.bmstoinverter.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.util.Util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;

@ApplicationScoped
public class BMSListProducer {
    private final static Logger LOG = LoggerFactory.getLogger(BMSListProducer.class);
    private static EnergyStorage energyStorage = new EnergyStorage();
    private static List<BMS> bmsList = null;
    private final Map<String, BMSDescriptor> bmsDescriptors = new HashMap<>();

    /**
     * Constructor.
     */
    public BMSListProducer() {
        ServiceLoader.load(BMSDescriptor.class).forEach(descriptor -> bmsDescriptors.put(descriptor.getName(), descriptor));
    }


    /**
     * Provides the application wide {@link EnergyStorage} object.
     *
     * @return the application wide {@link EnergyStorage} object
     */
    @Produces
    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }


    @Produces
    public synchronized List<BMS> produceBMSList() {
        if (bmsList == null) {
            bmsList = new ArrayList<>();
            String type = System.getProperty("bms.1.type");

            // if no bms is found, probably the config.properties have not been read
            if (type == null) {
                Util.updateSystemProperties(Path.of(System.getProperty("configFile", "config.properties")));
                type = System.getProperty("bms.1.type");

                if (type == null) {
                    LOG.error("No config.properties found or no BMSes are configured!");
                    System.exit(0);
                }
            }

            int index = 1;

            while (type != null) {
                bmsList.add(createBMS(index, type));

                index++;
                type = System.getProperty("bms." + index + ".type");
            }
        }

        return bmsList;
    }


    private BMS createBMS(final int index, final String name) {
        final BMSDescriptor bmsDescriptor = getBMSDescriptor(name);
        final BMS bms = CDI.current().select(bmsDescriptor.getBMSClass()).get();
        energyStorage.getBatteryPacks().addAll(bms.getBatteryPacks());
        final int bmsId = Integer.valueOf(System.getProperty("bms." + index + ".id"));
        final String portLocator = System.getProperty("bms." + index + ".portLocator");
        final int baudRate = Integer.valueOf(System.getProperty("bms." + index + ".baudRate"));
        final int delayAfterNoBytes = Integer.valueOf(System.getProperty("bms." + index + ".delayAfterNoBytes"));
        final BMSConfig config = new BMSConfig(bmsId, portLocator, baudRate, delayAfterNoBytes, bmsDescriptor);
        bms.initialize(config);

        LOG.info("Intialized BMS #" + config.getBmsId() + "[" + config.getDescriptor().getName() + "] on port " + portLocator);

        return bms;
    }


    /**
     * Gets the {@link BMSDescriptor} for the specified name.
     *
     * @param name the name for the {@link BMSDescriptor}
     * @return the {@link BMSDescriptor}
     */
    public BMSDescriptor getBMSDescriptor(final String name) {
        return bmsDescriptors.get(name);
    }
}
