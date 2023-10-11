package com.airepublic.bmstoinverter.core;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;

public abstract class PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(PortProcessor.class);

    /**
     * Search the implementing class for a {@link Port} field with a {@link Portname} annotation an
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


    public abstract Port getPort();


    public abstract void process();
}
