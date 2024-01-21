package com.airepublic.bmstoinverter.core;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.util.Util;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;

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
                Util.updateSystemProperties();
                type = System.getProperty("inverter.type");

                if (type == null) {
                    LOG.error("No config.properties found or no BMSes are configured!");
                    System.exit(0);
                }
            }

            final InverterDescriptor descriptor = descriptors.get(System.getProperty("inverter.type"));
            inverter = CDI.current().select(descriptor.getInverterClass()).get();
            final String portLocator = System.getProperty("inverter.portLocator");
            final int sendInterval = Integer.valueOf(System.getProperty("inverter.sendInterval"));
            final InverterConfig config = new InverterConfig(portLocator, sendInterval, descriptor);
            inverter.initialize(config);
        }

        return inverter;
    }
}