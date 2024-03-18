package com.airepublic.bmstoinverter.bms.jk.rs485;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the JK BMS RS485 binding.
 */
public class TestJKBmsRS485Processor {

    /**
     * Test the checksum calculation when preparing the send frame.
     */
    @Test
    public void testPrepareSendFrameChecksum() {
        final JKBmsRS485Processor processor = new JKBmsRS485Processor();
        // GIVEN
        // - the expected output frame bytes for command 0x85
        final byte[] frameBytes = new byte[] { (byte) 0x4E, (byte) 0x57, (byte) 0x00, (byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x03, (byte) 0x00, (byte) 0x85, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x68, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xAB };

        // WHEN
        // - preparing a send frame for command 0x85
        final ByteBuffer sendFrame = processor.prepareSendFrame((byte) 0x85);

        // THEN
        // - the created send frame bytes should match
        Assertions.assertArrayEquals(frameBytes, sendFrame.array());
    }

}