package com.airepublic.bmstoinverter.core;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.annotation.PostConstruct;

/**
 * The processor to read {@link Bms} or write {@link Inverter} from a {@link Port} and update the
 * {@link EnergyStorage} data.
 */
public abstract class PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(PortProcessor.class);
    private List<Port> ports;

    /**
     * Search the implementing class for the {@link PortType} annotation and whether it resembles a
     * {@link Bms} or {@link Inverter}. Corresponding to both annotations it will check the system
     * properties for the port names configuration defined in the <code>config.properties</code>
     * file. Port names can be defined, e.g. like <code>bms.portname=can0</code> (simple single
     * port) or <code>bms.0.portname=can0</code> (multiple ports) where the second segment is
     * incremented for each port, so the 2nd port would be something like
     * <code>bms.1.portname=can1</code>.
     */
    @PostConstruct
    public void init() {
        // first check if its a bms or inverter
        final Bms bms = getClass().getAnnotation(Bms.class);
        final Inverter inverter = getClass().getAnnotation(Inverter.class);
        String configPortPrefix = null;

        // set the prefix used in the config.properties accordingly
        if (bms != null) {
            configPortPrefix = "bms";
        } else if (inverter != null) {
            configPortPrefix = "inverter";
        }

        if (configPortPrefix == null) {
            LOG.error("Bms or Inverter Annotation is missing on PortProcessor " + getClass().getCanonicalName());
            throw new IllegalArgumentException("Bms or Inverter Annotation is missing on PortProcessor " + getClass().getCanonicalName());
        }

        // Next check the protocol to use on the port
        final PortType portType = getClass().getAnnotation(PortType.class);

        if (portType == null) {
            LOG.error(PortType.class.getName() + " Annotation is missing on PortProcessor " + getClass().getCanonicalName());
            throw new IllegalArgumentException(PortType.class.getName() + " Annotation is missing on PortProcessor " + getClass().getCanonicalName());
        }

        // from the protocol get the service class to use
        final Class<? extends Port> portServiceClass = portType.value().portClass;
        final Port port = ServiceLoader.load(portServiceClass).findFirst().orElseThrow();
        ports = new ArrayList<>();

        // check if the simple single portname is defined
        String portname = System.getProperty(configPortPrefix + ".portname");

        if (portname != null) {
            ports.add(port.create(portname));
        } else {
            // otherwise use the multiple incremental numbering
            final int i = 0;
            do {
                portname = System.getProperty(configPortPrefix + "." + i + ".portname");

                if (portname != null) {
                    ports.add(port.create(portname));
                }
            } while (portname != null);
        }

        if (ports.isEmpty()) {
            LOG.error("Portname(s) are not correctly defined in config.properties!");
            throw new IllegalArgumentException("Portname(s) are not correctly defined in config.properties!");
        }

        // TODO old code removal
        // for (final Field field : getClass().getDeclaredFields()) {
        // final Portname portname = field.getAnnotation(Portname.class);
        //
        // if (portname != null && Port.class.isAssignableFrom(field.getType())) {
        // try {
        // field.setAccessible(true);
        // final String value = System.getProperty(portname.value());
        //
        // if (value == null) {
        // LOG.error("Could not find {} property!", portname.value());
        // }
        // ((Port) field.get(this)).setPortname(System.getProperty(portname.value()));
        // } catch (final Exception e) {
        // LOG.error("Failed to set portname on port!", e);
        // }
        // }
        // }
    }


    /**
     * Gets the {@link Port}s.
     *
     * @return the {@link Port}s
     */
    public List<Port> getPorts() {
        return ports;
    }


    /**
     * Process data received by the port and update the {@link EnergyStorage} for a {@link Bms} or
     * sending the data via the {@link Port} to the {@link Inverter}.
     */
    public abstract void process();
}
