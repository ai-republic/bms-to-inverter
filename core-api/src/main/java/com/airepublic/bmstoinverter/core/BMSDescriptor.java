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
