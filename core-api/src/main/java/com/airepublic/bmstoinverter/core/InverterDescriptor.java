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
 * Service interface to identify and create a {@link Inverter} implementation.
 */
public interface InverterDescriptor {
    /**
     * Gets the name of the {@link Inverter} identifier.
     *
     * @return the name of the {@link Inverter} identifier
     */
    String getName();


    /**
     * Gets the default baud rate.
     *
     * @return the default baud rate
     */
    int getDefaultBaudRate();


    /**
     * Gets the class of the {@link Inverter} implementation.
     *
     * @return the class of the {@link Inverter} implementation
     */
    Class<? extends Inverter> getInverterClass();


    /**
     * Called by the BMS implementation if no {@link Port} already exists for the
     * {@link InverterConfig}s portLocator.
     *
     * @param config the {@link InverterConfig} for the {@link Inverter}
     * @return the created {@link Port}
     */
    Port createPort(InverterConfig config);
}
