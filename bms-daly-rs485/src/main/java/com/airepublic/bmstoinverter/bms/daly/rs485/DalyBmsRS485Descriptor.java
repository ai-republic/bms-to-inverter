package com.airepublic.bmstoinverter.bms.daly.rs485;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.protocol.rs485.FrameDefinition;
import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;
import com.fazecast.jSerialComm.SerialPort;

/**
 * The {@link BMSDescriptor} for the Daly BMS using the CAN protocol.
 */
public class DalyBmsRS485Descriptor implements BMSDescriptor {
    @Override
    public String getName() {
        return "DALY_RS485";
    }


    @Override
    public Class<? extends BMS> getBMSClass() {
        return DalyBmsRS485Processor.class;
    }


    @Override
    public Port createPort(final BMSConfig config) {
        final Port port = new JSerialCommPort(config.getPortLocator(), 9600, 8, 1, SerialPort.NO_PARITY, new byte[] { (byte) 165 }, FrameDefinition.create("SACLDV"));
        return port;
    }

}
