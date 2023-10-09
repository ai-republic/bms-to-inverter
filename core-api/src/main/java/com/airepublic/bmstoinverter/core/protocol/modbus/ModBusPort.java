package com.airepublic.bmstoinverter.core.protocol.modbus;

import com.airepublic.bmstoinverter.core.Port;

import jakarta.annotation.PostConstruct;

/**
 * Expecting System properties "ModBus.baudrate" to be set.
 */
@ModBus
public abstract class ModBusPort extends Port {
    private int baudrate = 9600;

    @PostConstruct
    public void init() {
        baudrate = Integer.parseInt(System.getProperty("ModBus.baudrate"));
    }


    public int getBaudrate() {
        return baudrate;
    }


    public void setBaudrate(final int baudrate) {
        this.baudrate = baudrate;
    }

}