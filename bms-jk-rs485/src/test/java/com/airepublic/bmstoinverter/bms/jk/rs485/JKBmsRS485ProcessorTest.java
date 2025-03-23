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
package com.airepublic.bmstoinverter.bms.jk.rs485;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.airepublic.bmstoinverter.bms.jk.rs485.JKBmsRS485Processor.DataEntry;
import com.airepublic.bmstoinverter.core.util.ByteReaderWriter;
import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

/**
 * Tests for the JK BMS RS485 binding.
 */
@ExtendWith(MockitoExtension.class)
public class JKBmsRS485ProcessorTest {
    private JKBmsRS485Processor processor;
    @Mock
    private JSerialCommPort port;
    private final ByteBuffer frame = ByteBuffer.wrap(new byte[] {
            (byte) 0x01,
            (byte) 0x79, (byte) 0x30,
            (byte) 0x01, (byte) 0x0B, (byte) 0xBA,
            (byte) 0x02, (byte) 0x0B, (byte) 0xAB,
            (byte) 0x03, (byte) 0x0B, (byte) 0xBA,
            (byte) 0x04, (byte) 0x0B, (byte) 0xBA,
            (byte) 0x05, (byte) 0x0B, (byte) 0xAB,
            (byte) 0x06, (byte) 0x0B, (byte) 0xAA,
            (byte) 0x07, (byte) 0x0B, (byte) 0xBA,
            (byte) 0x08, (byte) 0x0B, (byte) 0xAA,
            (byte) 0x09, (byte) 0x0B, (byte) 0xBC,
            (byte) 0x0A, (byte) 0x0B, (byte) 0xAA,
            (byte) 0x0B, (byte) 0x0B, (byte) 0xBC,
            (byte) 0x0C, (byte) 0x0B, (byte) 0xBC,
            (byte) 0x0D, (byte) 0x0B, (byte) 0xA9,
            (byte) 0x0E, (byte) 0x0B, (byte) 0xA9,
            (byte) 0x0F, (byte) 0x0B, (byte) 0xBC,
            (byte) 0x10, (byte) 0x0B, (byte) 0xAA,
            (byte) 0x80,
            (byte) 0x00, (byte) 0x1B,
            (byte) 0x81,
            (byte) 0x00, (byte) 0x17,
            (byte) 0x82,
            (byte) 0x00, (byte) 0x17,
            (byte) 0x83,
            (byte) 0x12, (byte) 0xB7,
            (byte) 0x84,
            (byte) 0x00, (byte) 0x00,
            (byte) 0x85,
            (byte) 0x02,
            (byte) 0x86,
            (byte) 0x02,
            (byte) 0x87,
            (byte) 0x00, (byte) 0x07,
            (byte) 0x89,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7B,
            (byte) 0x8A,
            (byte) 0x00, (byte) 0x10,
            (byte) 0x8B,
            (byte) 0x00, (byte) 0x00,
            (byte) 0x8C,
            (byte) 0x00, (byte) 0x07,
            (byte) 0x8E,
            (byte) 0x16, (byte) 0x80,
            (byte) 0x8F,
            (byte) 0x10, (byte) 0x40,
            (byte) 0x90,
            (byte) 0x0E, (byte) 0x10,
            (byte) 0x91,
            (byte) 0x0D, (byte) 0xDE,
            (byte) 0x92,
            (byte) 0x00, (byte) 0x05,
            (byte) 0x93,
            (byte) 0x0A, (byte) 0x28,
            (byte) 0x94,
            (byte) 0x0A, (byte) 0x5A,
            (byte) 0x95,
            (byte) 0x00, (byte) 0x05,
            (byte) 0x96,
            (byte) 0x01, (byte) 0x2C,
            (byte) 0x97,
            (byte) 0x00, (byte) 0x1E,
            (byte) 0x98,
            (byte) 0x01, (byte) 0x2C,
            (byte) 0x99,
            (byte) 0x00, (byte) 0x1E,
            (byte) 0x9A,
            (byte) 0x00, (byte) 0x1E,
            (byte) 0x9B,
            (byte) 0x0B, (byte) 0xB8,
            (byte) 0x9C,
            (byte) 0x00, (byte) 0x0A,
            (byte) 0x9D,
            (byte) 0x01,
            (byte) 0x9E,
            (byte) 0x00, (byte) 0x64,
            (byte) 0x9F,
            (byte) 0x00, (byte) 0x50,
            (byte) 0xA0,
            (byte) 0x00, (byte) 0x64,
            (byte) 0xA1,
            (byte) 0x00, (byte) 0x64,
            (byte) 0xA2,
            (byte) 0x00, (byte) 0x14,
            (byte) 0xA3,
            (byte) 0x00, (byte) 0x46,
            (byte) 0xA4,
            (byte) 0x00, (byte) 0x46,
            (byte) 0xA5,
            (byte) 0xFF, (byte) 0xEC,
            (byte) 0xA6,
            (byte) 0xFF, (byte) 0xF6,
            (byte) 0xA7,
            (byte) 0xFF, (byte) 0xEC,
            (byte) 0xA8,
            (byte) 0xFF, (byte) 0xF6,
            (byte) 0xA9,
            (byte) 0x10,
            (byte) 0xAA,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1E,
            (byte) 0xAB,
            (byte) 0x01,
            (byte) 0xAC,
            (byte) 0x01,
            (byte) 0xAD,
            (byte) 0x03, (byte) 0xB1,
            (byte) 0xAE,
            (byte) 0x01,
            (byte) 0xAF,
            (byte) 0x00,
            (byte) 0xB0,
            (byte) 0x00, (byte) 0x0A,
            (byte) 0xB1,
            (byte) 0x14,
            (byte) 0xB2,
            (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0xB3,
            (byte) 0x00,
            (byte) 0xB4,
            (byte) 0x49, (byte) 0x6E, (byte) 0x70, (byte) 0x75, (byte) 0x74, (byte) 0x20, (byte) 0x55, (byte) 0x73,
            (byte) 0xB5,
            (byte) 0x32, (byte) 0x33, (byte) 0x31, (byte) 0x32,
            (byte) 0xB6,
            (byte) 0x00, (byte) 0x00, (byte) 0xFA, (byte) 0xD5,
            (byte) 0xB7,
            (byte) 0x31, (byte) 0x31, (byte) 0x2E, (byte) 0x58, (byte) 0x57, (byte) 0x5F, (byte) 0x53, (byte) 0x31, (byte) 0x31, (byte) 0x2E, (byte) 0x32, (byte) 0x38, (byte) 0x38, (byte) 0x5F, (byte) 0x5F,
            (byte) 0xB8,
            (byte) 0x00,
            (byte) 0xB9,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x11,
            (byte) 0xBA,
            (byte) 0x49, (byte) 0x6E, (byte) 0x70, (byte) 0x75, (byte) 0x74, (byte) 0x20, (byte) 0x55, (byte) 0x73, (byte) 0x65, (byte) 0x72, (byte) 0x64, (byte) 0x61, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0xC0,
            (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x68,
            (byte) 0x00, (byte) 0x00, (byte) 0x52, (byte) 0x93
    }).order(ByteOrder.LITTLE_ENDIAN);

    @BeforeEach
    public void setup() {
        processor = new JKBmsRS485Processor();
    }


    /**
     * Test the checksum calculation when preparing the send frame.
     */
    @Test
    public void testPrepareSendFrameChecksum() {

        // GIVEN
        // - the expected output frame bytes for command 0x85
        final byte[] frameBytes = new byte[] { (byte) 0x4E, (byte) 0x57, (byte) 0x00, (byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x68, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x29 };

        // WHEN
        // - preparing a send frame for command 0x85
        final ByteBuffer sendFrame = processor.prepareSendFrame();

        // THEN
        // - the created send frame bytes should match
        Assertions.assertArrayEquals(frameBytes, sendFrame.array());
    }


    @Test
    public void testReadFrame() throws IOException {
        fillPortByteQueue();

        doCallRealMethod().when(port).readBytes(any(), anyLong());

        final List<DataEntry> entries = processor.readFrame(port);

        assertNotNull(entries);
        assertEquals(52, entries.size());
    }


    private void fillPortByteQueue() {
        doCallRealMethod().when(port).setQueue(any());
        doCallRealMethod().when(port).serialEvent(any(SerialPortEvent.class));

        port.setQueue(new ByteReaderWriter());

        do {
            final int length = (int) Math.round(Math.random() * frame.remaining());
            final byte[] bytes = new byte[length];
            frame.get(bytes);
            port.serialEvent(new SerialPortEvent(mock(SerialPort.class), SerialPort.LISTENING_EVENT_DATA_RECEIVED, bytes));
        } while (frame.remaining() != 0);
    }

}
