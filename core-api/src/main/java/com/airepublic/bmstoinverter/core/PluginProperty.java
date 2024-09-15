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

/**
 * A property for usage in an {@link AbstractPlugin}.
 */
public class PluginProperty {
    private String name;
    private String value;
    private String description;

    /**
     * Constructor.
     *
     * @param name the name of the property
     * @param value the value
     * @param description the description
     */
    public PluginProperty(final String name, final String value, final String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }


    /**
     * Gets the name of the property.
     *
     * @return the name of the property
     */
    public String getName() {
        return name;
    }


    /**
     * Sets the name of the property.
     *
     * @param name the name of the property
     */
    public void setName(final String name) {
        this.name = name;
    }


    /**
     * Gets the value of the property.
     *
     * @return the value of the property
     */
    public String getValue() {
        return value;
    }


    /**
     * Sets the value of the property.
     *
     * @param value the value of the property
     */
    public void setValue(final String value) {
        this.value = value;
    }


    /**
     * Gets the description of the property.
     *
     * @return the description of the property
     */
    public String getDescription() {
        return description;
    }


    /**
     * Sets the description of the property.
     *
     * @param description the description of the property
     */
    public void setDescription(final String description) {
        this.description = description;
    }
}
