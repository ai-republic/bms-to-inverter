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
package com.airepublic.bmstoinverter.bms.shoto.modbus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;
import com.airepublic.bmstoinverter.protocol.modbus.J2ModMasterPort;

/**
 * The unit test for the Shoto BMS {@link BMSDescriptor}.
 */
public class ShotoBmsModbusDescriptorTest {
    private final ShotoBmsModbusDescriptor descriptor = new ShotoBmsModbusDescriptor();

    @Test
    public void getName() {
        Assertions.assertEquals("SHOTO_MODBUS", descriptor.getName());
    }


    @Test
    public void getDefaultBaudRate() {
        Assertions.assertEquals(9600, descriptor.getDefaultBaudRate());
    }


    @Test
    public void getBMSClass() {
        Assertions.assertEquals(ShotoBmsModbusProcessor.class, descriptor.getBMSClass());
    }


    @Test
    public void createPort() {
        final BMSConfig config = Mockito.mock(BMSConfig.class);
        Assertions.assertInstanceOf(J2ModMasterPort.class, descriptor.createPort(config));
    }

}
