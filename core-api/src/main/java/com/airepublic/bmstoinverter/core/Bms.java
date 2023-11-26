package com.airepublic.bmstoinverter.core;

import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

/**
 * The interface to identify a BMS.
 */
public interface Bms {

    /**
     * Process data received by the port and update the {@link EnergyStorage} for a {@link Bms}.
     *
     * @param callback the code executed after successful processing
     */
    void process(Runnable callback);

}
