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
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.util.Util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;

@ApplicationScoped
public class InverterProducer {
    private final static Logger LOG = LoggerFactory.getLogger(InverterProducer.class);
    private static Inverter inverter = null;
    private final Map<String, InverterDescriptor> descriptors = new HashMap<>();

    /**
     * Constructor.
     */
    public InverterProducer() {
        ServiceLoader.load(InverterDescriptor.class).forEach(descriptor -> descriptors.put(descriptor.getName(), descriptor));
    }


    /**
     * Gets the {@link InverterDescriptor} for the specified name.
     *
     * @param name the name for the {@link InverterDescriptor}
     * @return the {@link InverterDescriptor}
     */
    public InverterDescriptor getDescriptor(final String name) {
        return descriptors.get(name);
    }


    @Produces
    @InverterQualifier
    public synchronized Inverter createInverter() {
        if (inverter == null) {
            String type = System.getProperty("inverter.type");

            // if no inverter is found, probably the config.properties have not been read
            if (type == null) {
                Util.updateSystemProperties(Path.of(System.getProperty("configFile", "config.properties")));
                type = System.getProperty("inverter.type");

                if (type == null) {
                    LOG.error("No config.properties found or no BMSes are configured!");
                    System.exit(0);
                }
            }

            final InverterDescriptor descriptor = descriptors.get(System.getProperty("inverter.type"));
            inverter = CDI.current().select(descriptor.getInverterClass()).get();
            final String portLocator = System.getProperty("inverter.portLocator");
            final int baudRate = Integer.valueOf(System.getProperty("inverter.baudRate"));
            final int sendInterval = Integer.valueOf(System.getProperty("inverter.sendInterval"));
            final InverterConfig config = new InverterConfig(portLocator, baudRate, sendInterval, descriptor);
            inverter.initialize(config);
        }

        return inverter;
    }
}
