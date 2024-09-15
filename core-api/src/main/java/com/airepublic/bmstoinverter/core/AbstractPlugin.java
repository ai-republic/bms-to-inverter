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

import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The abstract super class for the {@link BmsPlugin} and {@link InverterPlugin} allowing for
 * properties definition used in the Configurator application.
 *
 * @param <DEVICE> the device class, e.g. {@link BMS} or {@link Inverter}
 */
public abstract class AbstractPlugin<DEVICE> {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractPlugin.class);
    private Set<PluginProperty> properties = new LinkedHashSet<>();

    /**
     * Gets the name of the plugin.
     * 
     * @return the name
     */
    public abstract String getName();


    /**
     * Gets the properties.
     * 
     * @return the properties
     */
    public Set<PluginProperty> getProperties() {
        return properties;
    }


    /**
     * Sets the properties.
     * 
     * @param properties the properties
     */
    public void setProperties(final Set<PluginProperty> properties) {
        this.properties = properties;
    }


    /**
     * Adds the specified property to the properties for this plugin.
     *
     * @param property the property
     */
    public void addProperty(final PluginProperty property) {
        properties.add(property);
    }


    /**
     * Removes the specified property from the properties for this plugin.
     *
     * @param property the property
     */
    public void removeProperty(final PluginProperty property) {
        properties.remove(property);
    }


    /**
     * Gets the {@link PluginProperty} for the specified name.
     *
     * @param name the name
     * @return the {@link PluginProperty}
     */
    public PluginProperty getProperty(final String name) {
        return properties.stream().filter(p -> p.getName().equals(name)).findFirst().orElseGet(null);
    }


    /**
     * Gets the {@link PluginProperty}'s value for the specified name.
     *
     * @param name the name
     * @param defaultValue the default value if the property doesn't exist
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public <T> T getPropertyValue(final String name, final T defaultValue) {
        final PluginProperty p = getProperty(name);

        if (p == null) {
            return defaultValue;
        }

        return (T) p.getValue();
    }


    /**
     * Called before the initialization of the {@link BMS} from the specified configuration.
     * 
     * @param device the device that can be changed before initialization
     */
    public void onInitialize(final DEVICE device) {
    }


    /**
     * Called before the the frame is sent to the {@link BMS} and can be used to modify the frame
     * data.
     *
     * @param frame the frame data
     * @return the (optionally) modified frame data
     */
    public ByteBuffer onSend(final ByteBuffer frame) {
        return frame;
    }


    /**
     * Called after a frame is received from the {@link BMS} and can be used to modify the frame.
     *
     * @param frame the frame data
     * @return the (optionally) modified frame
     */
    public ByteBuffer onReceive(final ByteBuffer frame) {
        return frame;
    }


    @Override
    public String toString() {
        return getName();
    }
}
