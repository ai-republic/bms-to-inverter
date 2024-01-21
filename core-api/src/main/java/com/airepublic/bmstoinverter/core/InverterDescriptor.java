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
