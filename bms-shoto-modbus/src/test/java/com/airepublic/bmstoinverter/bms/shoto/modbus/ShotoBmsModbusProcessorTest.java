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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * The unit-test for a Shoto {@link BMS}.
 */
public class ShotoBmsModbusProcessorTest {
    private final static int UNIT_ID = 0;
    private ShotoBmsModbusProcessor processor;
    private BatteryPack batteryPack;

    @BeforeEach
    public void setUp() {
        processor = new ShotoBmsModbusProcessor();
        batteryPack = processor.getBatteryPack(UNIT_ID);
    }


    @Test
    public void testReadBatteryVoltage() {
        final ByteBuffer buffer = ByteBuffer.allocate(26);
        buffer.putInt(0x0003); // functionCode
        buffer.putInt(0x0001); // numRegisters
        buffer.putInt(UNIT_ID); // unitId
        buffer.putChar((char) 2500); // packVoltage (25.0V)
        buffer.flip();

        processor.readBatteryVoltage(buffer);

        assertEquals(250, batteryPack.packVoltage);
    }
}
