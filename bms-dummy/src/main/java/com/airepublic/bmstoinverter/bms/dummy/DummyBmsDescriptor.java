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
import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;
import com.airepublic.bmstoinverter.core.DummyPort;
import com.airepublic.bmstoinverter.core.Port;

/**
 * The {@link BMSDescriptor} for the BYD BMS using the CAN protocol.
 */
public class DummyBmsDescriptor implements BMSDescriptor {
    @Override
    public String getName() {
        return "NONE";
    }


    @Override
    public int getDefaultBaudRate() {
        return 500000;
    }


    @Override
    public Class<? extends BMS> getBMSClass() {
        return DummyBmsProcessor.class;
    }


    @Override
    public Port createPort(final BMSConfig config) {
        final Port port = new DummyPort();
        return port;
    }

}
