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
package com.airepublic.bmstoinverter.inverter.pylon.rs485;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.InverterConfig;
import com.airepublic.bmstoinverter.core.InverterDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.protocol.rs485.FrameDefinition;
import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;
import com.fazecast.jSerialComm.SerialPort;

/**
 * The {@link InverterDescriptor} for the Pylontech {@link Inverter} using the RS485 protocol.
 */
public class PylonInverterRS485Descriptor implements InverterDescriptor {
    @Override
    public String getName() {
        return "PYLON_RS485";
    }


    @Override
    public int getDefaultBaudRate() {
        return 9600;
    }


    @Override
    public Class<? extends Inverter> getInverterClass() {
        return PylonInverterRS485Processor.class;
    }


    @Override
    public Port createPort(final InverterConfig config) {
        final Port port = new JSerialCommPort(config.getPortLocator(), config.getBaudRate(), 8, 1, SerialPort.NO_PARITY, new byte[] { (byte) 165 }, FrameDefinition.create("SOACCLDVO"));
        return port;
    }

}
