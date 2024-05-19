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
package com.airepublic.bmstoinverter.configurator;

public class MenuItem<V> {
    private String displayName;
    private V value;

    public MenuItem(final String displayName, final V value) {
        setDisplayName(displayName);
        setValue(value);
    }


    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }


    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }


    /**
     * @return the value
     */
    public V getValue() {
        return value;
    }


    /**
     * @param value the value to set
     */
    public void setValue(final V value) {
        this.value = value;
    }


    @Override
    public int hashCode() {
        return displayName.hashCode();
    }


    @Override
    public String toString() {
        return getDisplayName();
    }

}
