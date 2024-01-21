package com.airepublic.bmstoinverter.inverter.dummy;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.InverterConfig;
import com.airepublic.bmstoinverter.core.InverterDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.protocol.can.JavaCANPort;

/**
 * The {@link InverterDescriptor} for the Daly BMS using the CAN protocol.
 */
public class DummyInverterDescriptor implements InverterDescriptor {
    @Override
    public String getName() {
        return "NONE";
    }


    @Override
    public Class<? extends Inverter> getInverterClass() {
        return DummyInverterProcessor.class;
    }


    @Override
    public Port createPort(final InverterConfig config) {
        final Port port = new JavaCANPort(config.getPortLocator(), 500000);
        return port;
    }

}
