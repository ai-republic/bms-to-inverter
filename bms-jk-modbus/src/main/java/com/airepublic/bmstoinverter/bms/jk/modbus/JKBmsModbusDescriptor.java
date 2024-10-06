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
package com.airepublic.bmstoinverter.bms.jk.modbus;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.protocol.modbus.J2ModMasterPort;

/**
 * The {@link BMSDescriptor} for the JK BMS using the Modbus protocol.
 */
public class JKBmsModbusDescriptor implements BMSDescriptor {
    @Override
    public String getName() {
        return "JK_MODBUS";
    }


    @Override
    public int getDefaultBaudRate() {
        return 115200;
    }


    @Override
    public Class<? extends BMS> getBMSClass() {
        return JKBmsModbusProcessor.class;
    }


    @Override
    public Port createPort(final BMSConfig config) {
        final Port port = new J2ModMasterPort(config.getPortLocator(), config.getBaudRate());
        return port;
    }

}
