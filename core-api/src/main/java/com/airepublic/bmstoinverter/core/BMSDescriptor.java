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
 * Service interface to identify and create a {@link BMS} implementation.
 */
public interface BMSDescriptor {
    /**
     * Gets the name of the {@link BMS} identifier.
     *
     * @return the name of the {@link BMS} identifier
     */
    String getName();


    /**
     * Gets the default baud rate.
     *
     * @return the default baud rate
     */
    int getDefaultBaudRate();


    /**
     * Gets the class of the {@link BMS} implementation.
     *
     * @return the class of the {@link BMS} implementation
     */
    Class<? extends BMS> getBMSClass();


    /**
     * Called by the BMS implementation if no {@link Port} already exists for the {@link BMSConfig}s
     * portLocator.
     *
     * @param config the {@link BMSConfig} for the {@link BMS}
     * @return the created {@link Port}
     */
    Port createPort(BMSConfig config);
}
