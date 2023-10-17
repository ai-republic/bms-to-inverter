package com.airepublic.bmstoinverter.core.bms.data;

public class Alarm {
    public String key;
    public boolean value = false;

    public Alarm(final String key, final boolean value) {
        this.key = key;
        this.value = value;
    }
}
