package com.airepublic.bmstoinverter.bms.jk.can;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.protocol.can.JavaCANPort;

/**
 * The {@link BMSDescriptor} for the JK BMS using the CAN protocol.
 */
public class TestJKBmsCANDescriptor {
    private static final JKBmsCANDescriptor desc = new JKBmsCANDescriptor();

    @Test
    public void testName() {
        Assertions.assertEquals("JK_CAN", desc.getName());
    }


    @Test
    public void testBMSClass() {
        Assertions.assertEquals(JKBmsCANProcessor.class, desc.getBMSClass());
    }


    @Test
    public void testPort() {
        final BMSConfig config = new BMSConfig(1, "", 200, desc);
        final Port port = desc.createPort(config);

        Assertions.assertEquals(JavaCANPort.class, port.getClass());
        Assertions.assertEquals(500000, port.getBaudrate());
    }

}
