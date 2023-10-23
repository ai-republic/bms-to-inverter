package com.airepublic.bmstoinverter.core;

import java.lang.reflect.Field;

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

    /**
     * Search the implementing class for a {@link Port} field with a {@link Portname} annotation and
     * set the configured portname on the field.
     */
    @PostConstruct
    public void init() {
        for (final Field field : getClass().getDeclaredFields()) {
            final Portname portname = field.getAnnotation(Portname.class);

            if (portname != null && Port.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    final String value = System.getProperty(portname.value());

                    if (value == null) {
                        LOG.error("Could not find {} property!", portname.value());
                    }
                    ((Port) field.get(this)).setPortname(System.getProperty(portname.value()));
                } catch (final Exception e) {
                    LOG.error("Failed to set portname on port!", e);
                }
            }
        }
    }


    /**
     * Gets the {@link Port}.
     *
     * @return the {@link Port}
     */
    public abstract Port getPort();


    /**
     * Process data received by the port and update the {@link EnergyStorage} for a {@link Bms} or
     * sending the data via the {@link Port} to the {@link Inverter}.
     */
    public abstract void process();
}
