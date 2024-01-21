package com.airepublic.bmstoinverter.inverter.growatt.rs485;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.InverterConfig;
import com.airepublic.bmstoinverter.core.InverterDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.protocol.modbus.J2ModPort;

/**
 * The {@link InverterDescriptor} for the Daly BMS using the CAN protocol.
 */
public class GrowattInverterRS485Descriptor implements InverterDescriptor {
    @Override
    public String getName() {
        return "GROWATT_RS485";
    }


    @Override
    public Class<? extends Inverter> getInverterClass() {
        return GrowattInverterRS485Processor.class;
    }


    @Override
    public Port createPort(final InverterConfig config) {
        final Port port = new J2ModPort(config.getPortLocator(), 500000);
        return port;
    }

}
