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

import java.util.Objects;
import java.util.Set;

/**
 * Class describing a plugin and its properties.
 */
public class PluginConfig {
    private final String pluginName;
    private final String pluginClass;
    private final Set<PluginProperty> properties;

    /**
     * Constructor.
     * 
     * @param pluginName the name of the plugin
     * @param pluginClass the class of the plugin
     * @param properties the plugin properties
     */
    public PluginConfig(final String pluginName, final String pluginClass, final Set<PluginProperty> properties) {
        this.pluginName = pluginName;
        this.pluginClass = pluginClass;
        this.properties = properties;
    }


    /**
     * Gets the name of the plugin.
     *
     * @return the name
     */
    public String getPluginName() {
        return pluginName;
    }


    /**
     * Gets the plugin class.
     *
     * @return the plugin class
     */
    public String getPluginClass() {
        return pluginClass;
    }


    /**
     * Gets the plugin properties.
     *
     * @return the plugin properties
     */
    public Set<PluginProperty> getProperties() {
        return properties;
    }


    /**
     * Gets the property for the specified name or null if it doesn't exist.
     *
     * @param name the name of the property
     * @return the property or null
     */
    public PluginProperty getProperty(final String name) {
        return properties.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }


    @Override
    public String toString() {
        return getPluginName();
    }


    @Override
    public int hashCode() {
        return Objects.hash(pluginClass, pluginName);
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PluginConfig other = (PluginConfig) obj;
        return Objects.equals(pluginClass, other.pluginClass) && Objects.equals(pluginName, other.pluginName);
    }
}
