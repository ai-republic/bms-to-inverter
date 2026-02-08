package com.airepublic.bmstoinverter.bms.hngce.modbus;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.protocol.modbus.J2ModMasterPort;

public class HngceBmsModbusDescriptor implements BMSDescriptor {

    @Override
    public String getName() {
        return "HNGCE_MODBUS";
    }


    @Override
    public int getDefaultBaudRate() {
        return 9600;
    }


    @Override
    public Class<? extends BMS> getBMSClass() {
        return HngceBmsModbusProcessor.class;
    }


    @Override
    public Port createPort(final BMSConfig config) {
        return new J2ModMasterPort(config.getPortLocator(), config.getBaudRate());
    }
}
