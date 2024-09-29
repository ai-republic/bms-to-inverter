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
package com.airepublic.bmstoinverter.bms.dummy;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

import jakarta.annotation.PostConstruct;

/**
 * The class to to use for a dummy {@link BMS}.
 */
public class DummyBmsProcessor extends BMS {

    /**
     * Initializes the dummy {@link BMS} with one battery pack.
     */
    @PostConstruct
    public void init() {
        getBatteryPacks().add(new BatteryPack());
    }


    @Override
    public void collectData(final Port port) {
    }

}
