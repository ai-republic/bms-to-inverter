package com.airepublic.bmstoinverter.core.bms.data;

/**
 * Class to represent a warning or alarm from the BMS.
 */
public class Alarm {
    /** The key identifier to look up the internationalized message. */
    public String key;
    /** The flag if the warning/alarm is active. */
    public boolean value = false;

    /**
     * Constructor.
     * 
     * @param key the key identifier to look up the internationalized message
     * @param value the flag if the warning/alarm is active
     */
    public Alarm(final String key, final boolean value) {
        this.key = key;
        this.value = value;
    }
}
