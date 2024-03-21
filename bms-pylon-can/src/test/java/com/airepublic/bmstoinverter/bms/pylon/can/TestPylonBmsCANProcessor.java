package com.airepublic.bmstoinverter.bms.pylon.can;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * The class to tests CAN messages from a JK {@link BMS}.
 */
public class TestPylonBmsCANProcessor {
    private static PylonBmsCANProcessor p;
    private static BatteryPack pack;
    private static ByteBuffer data;
    private final static byte[] CLEAR_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };

    @BeforeAll
    public static void init() {
        p = new PylonBmsCANProcessor();
        pack = new BatteryPack();
        data = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    }


    @BeforeEach
    public void cleanByteBuffer() {
        data.clear().put(CLEAR_BYTES).rewind();
    }


    @Test
    public void testReadChargeDischargeInfo() {
        data.put(new byte[] { (byte) 0x34, (byte) 0x02, (byte) 0xD0, (byte) 0x07, (byte) 0xA0, (byte) 0x0F, (byte) 0xB0, (byte) 0x01 }).rewind();

        p.readChargeDischargeInfo(pack, data);

        Assertions.assertEquals(564, pack.maxPackVoltageLimit);
        Assertions.assertEquals(2000, pack.maxPackChargeCurrent);
        Assertions.assertEquals(4000, pack.maxPackDischargeCurrent);
        Assertions.assertEquals(432, pack.minPackVoltageLimit);
    }


    @Test
    public void testReadSOC() {
        data.put(new byte[] { (byte) 0x2A, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }).rewind();

        p.readSOC(pack, data);

        Assertions.assertEquals(420, pack.packSOC);
        Assertions.assertEquals(1000, pack.packSOH);
    }


    @Test
    public void testReadBatteryVoltage() {
        data.put(new byte[] { (byte) 0x63, (byte) 0x14, (byte) 0x0F, (byte) 0xFF, (byte) 0xEB, (byte) 0x00, (byte) 0x00, (byte) 0x00 }).rewind();

        p.readBatteryVoltage(pack, data);

        Assertions.assertEquals(521, pack.packVoltage);
        Assertions.assertEquals(-241, pack.packCurrent);
        Assertions.assertEquals(235, pack.tempAverage);
    }

}
