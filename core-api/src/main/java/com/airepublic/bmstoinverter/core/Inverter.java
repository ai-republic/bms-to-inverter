package com.airepublic.bmstoinverter.core;

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;

/**
 * The class to identify an {@link Inverter}.
 */
public abstract class Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(Inverter.class);
    private Port port;

    /**
     * Search the implementing class for the {@link PortType} annotation defined in the
     * <code>config.properties</code> file. The {@link Inverter} port can be configured like
     * e.g.:<br>
     * <br>
     * <code>inverter.portLocator=can0</code><br>
     */
    @PostConstruct
    public void init() {
        // Check the protocol to use on the port
        final PortType portType = getClass().getAnnotation(PortType.class);

        if (portType == null) {
            LOG.error(PortType.class.getName() + " Annotation is missing on PortProcessor " + getClass().getCanonicalName());
            throw new IllegalArgumentException(PortType.class.getName() + " Annotation is missing on PortProcessor " + getClass().getCanonicalName());
        }

        // from the protocol get the service class to use
        final Class<? extends Port> portServiceClass = portType.value().portClass;
        final Port port = ServiceLoader.load(portServiceClass).findFirst().orElseThrow();

        // check if the simple single portname is defined
        final String portname = System.getProperty("inverter.portLocator");

        if (portname != null) {
            this.port = port.create(portname);
        } else {
            LOG.error("The property 'inverter.portLocator' not correctly defined in config.properties!");
            throw new IllegalArgumentException("The property 'inverter.portLocator' not correctly defined in config.properties!");
        }
    }


    /**
     * Gets the {@link Port} of the {@link Inverter}.
     *
     * @return the {@link Port}
     */
    public Port getPort() {
        return port;
    }


    /**
     * Process sending the data via the {@link Port} to the {@link Inverter}.
     *
     * @param callback the code executed after successful processing
     */
    public abstract void process(Runnable callback);

}
