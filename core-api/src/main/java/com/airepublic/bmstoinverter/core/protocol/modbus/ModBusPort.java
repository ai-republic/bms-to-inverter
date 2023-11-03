package com.airepublic.bmstoinverter.core.protocol.modbus;

import com.airepublic.bmstoinverter.core.Port;

import jakarta.annotation.PostConstruct;

/**
 * A {@link Port} that is used for ModBus messages. Expecting <code>config.properties</code> or
 * System properties "ModBus.baudrate" to be set.
 */
@ModBus
public abstract class ModBusPort extends Port {
    private int baudrate = 9600;

    /**
     * Constructor.
     */
    public ModBusPort() {
    }


    /**
     * Constructor.
     * 
     * @param portname the portname
     */
    public ModBusPort(final String portname) {
        super(portname);
    }


    /**
     * Initializes the baud rate from the <code>config.properties</code> or system property
     * <code>ModBus.baudrate</code>.
     */
    @PostConstruct
    public void init() {
        baudrate = Integer.parseInt(System.getProperty("ModBus.baudrate"));
    }


    /**
     * Gets the baud rate.
     *
     * @return the baud rate
     */
    public int getBaudrate() {
        return baudrate;
    }


    /**
     * Sets the baud rate.
     *
     * @param baudrate the baud rate
     */
    public void setBaudrate(final int baudrate) {
        this.baudrate = baudrate;
    }

}