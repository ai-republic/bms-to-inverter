package com.airepublic.bmstoinverter.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

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

        energyStorage = new EnergyStorage(numBatteryPacks, getPorts());
    }


    /**
     * Search the implementing class for the {@link PortType} annotation and whether it resembles a
     * {@link Bms} or {@link Inverter}. Corresponding to both annotations it will check the system
     * properties for the port names configuration defined in the <code>config.properties</code>
     * file. Port names can be defined, e.g. like<br>
     * <br>
     * <code>bms.portLocator=can0 <br>bms.portProtocol=CAN</code><br>
     * (simple single port - all BMSes use this port) <br>
     * <br>
     * or <br>
     * <br>
     * <code>bms.0.portLocator=/dev/ttyS0 <br>bms.0.portProtocol=RS485<br>
     * <code>bms.1.portLocator=can1<br>bms.1.portProtocol=CAN</code><br>
     * <br>
     * (multiple ports - each BMS has its own different port) where the each segment is incremented
     * for each port.
     */
    private List<Port> getPorts() {
        // check if the simple single portname is defined
        String portLocator = System.getProperty("bms.portLocator");
        String portProtocol = System.getProperty("bms.portProtocol");

        if (portLocator != null && portProtocol != null) {
            final Protocol protocol = Protocol.valueOf(portProtocol);

            if (protocol == null) {
                throw new RuntimeException("Port configuration is not correct for '" + portLocator + "' and protocol '" + portProtocol + "'!");
            }

            return Arrays.asList(ServiceLoader.load(protocol.portClass).findFirst().get().create(portLocator));
        }

        // otherwise use the multiple incremental numbering
        final List<Port> ports = new ArrayList<>();
        final String configPortPrefix = "bms";

        // otherwise use the multiple incremental numbering
        final int i = 0;

        do {
            portLocator = System.getProperty(configPortPrefix + "." + i + ".portLocator");
            portProtocol = System.getProperty(configPortPrefix + "." + i + ".portProtocol");

            if (portLocator != null && portProtocol != null) {
                final Protocol protocol = Protocol.valueOf(portProtocol);
                ports.add(ServiceLoader.load(protocol.portClass).findFirst().get().create(portLocator));
            }
        } while (portLocator != null);

        if (ports.isEmpty()) {
            LOG.error("Ports are not correctly defined in config.properties!");
            throw new IllegalArgumentException("Ports are not correctly defined in config.properties!");
        }

        return ports;
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
