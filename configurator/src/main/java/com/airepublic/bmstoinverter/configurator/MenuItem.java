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
