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
package com.airepublic.bmstoinverter.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestUtil {
    @Test
    public void testReadBitAllZero() {
        // GIVEN a zero bits integer value
        final int value = 0x00000000;

        // WHEN reading each bit
        for (int i = 0; i < Integer.BYTES * 8; i++) {

            // THEN each bit must be 0 (false)
            assertFalse(BitUtil.bit(value, i));
        }
    }


    @Test
    public void testReadBitAllOne() {
        // GIVEN a zero bits integer value
        final int value = 0xFFFFFFFF;

        // WHEN reading each bit
        for (int i = 0; i < Integer.BYTES * 8; i++) {

            // THEN each bit must be 1 (true)
            assertTrue(BitUtil.bit(value, i));
        }
    }


    @Test
    public void testReadBitFirstOne() {
        // GIVEN a zero bits integer value
        final int value = 0x00000001;

        // WHEN reading first bit
        // THEN the first bit must be 1 (true)
        assertTrue(BitUtil.bit(value, 0));
    }


    @Test
    public void testReadBitSecondOne() {
        // GIVEN a zero bits integer value
        final int value = 0x00000002;

        // WHEN reading first bit
        // THEN
        // - the first bit must be 0 (false)
        // - the second bit must be 1 (true)
        assertFalse(BitUtil.bit(value, 0));
        assertTrue(BitUtil.bit(value, 1));
    }


    @Test
    public void testReadBitLastOne() {
        // GIVEN a zero bits integer value
        final int value = 0x80000000;

        // WHEN reading first bit
        // THEN the first bit must be 1 (true)
        assertTrue(BitUtil.bit(value, 31));
    }


    @Test
    public void testReadBitLastOfByteOne() {
        // GIVEN a zero bits integer value
        final int value = 0x80;

        // WHEN reading first bit
        // THEN the first bit must be 1 (true)
        assertTrue(BitUtil.bit(value, 7));
    }


    @Test
    public void testBits() {
        // GIVEN the bits 1000 0000
        final int value = 0x80;

        // WHEN
        // -reading the first 3 bits must be 0
        // -reading the bits 5 - 8 must be 8
        // -reading the last bit must be 1
        assertEquals(0, BitUtil.bits(value, 0, 3));
        assertEquals(8, BitUtil.bits(value, 4, 4));
        assertEquals(1, BitUtil.bits(value, 7, 1));
    }

}
