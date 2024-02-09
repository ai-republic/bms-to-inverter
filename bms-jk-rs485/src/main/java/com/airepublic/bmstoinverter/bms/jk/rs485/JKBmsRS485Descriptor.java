package com.airepublic.bmstoinverter.bms.jk.rs485;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.protocol.rs485.FrameDefinition;
import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;
import com.fazecast.jSerialComm.SerialPort;

/**
 * The {@link BMSDescriptor} for the Daly BMS using the CAN protocol.
 */
public class JKBmsRS485Descriptor implements BMSDescriptor {
    @Override
    public String getName() {
        return "JK_CAN";
    }


    @Override
    public Class<? extends BMS> getBMSClass() {
        return JKBmsRS485Processor.class;
    }


    @Override
    public Port createPort(final BMSConfig config) {
        final Port port = new JSerialCommPort(config.getPortLocator(), 115000, 8, 1, SerialPort.NO_PARITY, new byte[] { 78 }, FrameDefinition.create(""));
        return port;
    }

}
