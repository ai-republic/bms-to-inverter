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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * The unit-test for a Shoto {@link BMS}.
 */
public class ShotoBmsModbusProcessorTest {
    private final static int UNIT_ID = 0;
    private ShotoBmsModbusProcessor processor;
    private BatteryPack batteryPack;
    private final Port port = mock(Port.class);

    @BeforeEach
    public void setUp() {
        processor = new ShotoBmsModbusProcessor();
        processor.initialize(new BMSConfig(2, "com3", 9600, 1, new ShotoBmsModbusDescriptor()));
        batteryPack = processor.getBatteryPack(UNIT_ID);
    }


    private ByteBuffer createFromString(final String str) {
        final String[] byteValues = str.split(" ");
        final ByteBuffer buffer = ByteBuffer.allocate(3 * Integer.BYTES + byteValues.length - 3);

        // Convert the first three byte values to integers
        for (int i = 0; i < 3; i++) {
            buffer.putInt(Integer.parseInt(byteValues[i], 16));
        }

        // Convert the remaining byte values to bytes
        for (int i = 3; i < byteValues.length; i++) {
            buffer.put((byte) Integer.parseInt(byteValues[i], 16));
        }

        buffer.flip();
        return buffer;
    }


    @Test
    public void testCollectData() throws IOException {
        // Mock the response frames for consecutive calls
        final ByteBuffer packVoltageResponse = createFromString("02 03 02 14 AB B2 FB");
        final ByteBuffer minMaxTempResponse = createFromString("02 03 04 00 0F 00 0E 78 F4");
        final ByteBuffer cellVoltageAndTemperatureResponse = createFromString("02 03 40 00 0F 00 0F 00 0E 00 0E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0C E5 0C E8 0C E7 0C E6 0C E9 0C E6 0C E8 0C E8 0C E4 0C E8 0C E7 0C E9 0C EA 0C E9 0C E9 0C E9 2C 2B");

        // Define the return values for consecutive calls
        when(port.receiveFrame()).thenReturn(packVoltageResponse).thenReturn(minMaxTempResponse).thenReturn(cellVoltageAndTemperatureResponse);

        // Create a spy of the processor
        final ShotoBmsModbusProcessor spyProcessor = Mockito.spy(processor);

        try {
            // Call the method under test twice
            spyProcessor.collectData(port);
        } catch (final BufferUnderflowException e) {
            // ignore
        }

        // Verify that the methods were called
        verify(spyProcessor, times(1)).readBatteryVoltage(any());
        verify(spyProcessor, times(1)).readCellMinMaxTemperature(any());
        verify(spyProcessor, times(1)).readCellVoltageAndTemperature(any());

        assertEquals(529, batteryPack.packVoltage);
        assertEquals(14, batteryPack.tempMin);
        assertEquals(15, batteryPack.tempMax);
        assertEquals(3301, batteryPack.cellVmV[0]);
        assertEquals(15, batteryPack.cellTemperature[0]);
    }

}
