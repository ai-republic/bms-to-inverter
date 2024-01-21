package com.airepublic.bmstoinverter.bms.pylon.can;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.protocol.can.JavaCANPort;

/**
 * The {@link BMSDescriptor} for the Daly BMS using the CAN protocol.
 */
public class PylonBmsCANDescriptor implements BMSDescriptor {
    @Override
    public String getName() {
        return "PYLON_CAN";
    }


    @Override
    public Class<? extends BMS> getBMSClass() {
        return PylonBmsCANProcessor.class;
    }


    @Override
    public Port createPort(final BMSConfig config) {
        final Port port = new JavaCANPort(config.getPortLocator(), 500000);
        return port;
    }

}
