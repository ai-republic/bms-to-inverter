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
package com.airepublic.bmstoinverter.bms.jk.can;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * The class to handle CAN messages from a JK {@link BMS}.
 */
public class TestJKBmsCANProcessor {
    private static JKBmsCANProcessor p;
    private static BatteryPack pack;
    private static ByteBuffer data;
    private final static byte[] CLEAR_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };

    @BeforeAll
    public static void init() {
        p = new JKBmsCANProcessor();
        pack = new BatteryPack();
        data = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    }


    @BeforeEach
    public void cleanByteBuffer() {
        data.clear();
        data.put(CLEAR_BYTES).rewind();
    }


    @Test
    public void testBatteryStatus() {
        data.put(new byte[] { (byte) 0x34, (byte) 0x02, (byte) 0x1B, (byte) 0x10, (byte) 0x63, (byte) 0x00, (byte) 0x00, (byte) 0x00 }).rewind();

        p.readBatteryStatus(pack, data);

        Assertions.assertEquals(564, pack.packVoltage);
        Assertions.assertEquals(123, pack.packCurrent);
        Assertions.assertEquals(990, pack.packSOC);
    }

}
