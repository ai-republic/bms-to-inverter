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
package com.airepublic.bmstoinverter.core.protocol.modbus;

import com.airepublic.bmstoinverter.core.Port;

import javax.annotation.PostConstruct;

/**
 * A {@link Port} that is used for ModBus messages. Expecting <code>config.properties</code> or
 * System properties "ModBus.baudrate" to be set.
 */
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
     * @param baudrate the baudrate
     */
    public ModBusPort(final String portname, final int baudrate) {
        super(portname, baudrate);
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
    @Override
    public int getBaudrate() {
        return baudrate;
    }


    /**
     * Sets the baud rate.
     *
     * @param baudrate the baud rate
     */
    @Override
    public void setBaudrate(final int baudrate) {
        this.baudrate = baudrate;
    }

}
