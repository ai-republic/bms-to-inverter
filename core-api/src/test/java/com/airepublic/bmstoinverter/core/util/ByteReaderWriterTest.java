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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class ByteReaderWriterTest {
    @SuppressWarnings("resource")
    @Test
    public void testWriteCorrect() throws IOException {
        final ByteReaderWriter test = new ByteReaderWriter();
        test.write(new byte[] { 1, 2, 3 });

        assertEquals(1, test.read());
        assertEquals(2, test.read());
        assertEquals(3, test.read());
    }


    @SuppressWarnings("resource")
    @Test
    public void testWriteNull() throws IOException {
        // GIVEN an empty instance
        final ByteReaderWriter test = new ByteReaderWriter();

        // WHEN an null array is written
        test.write(null);

        // THEN
        // - it should throw an IOException when reading indicating no bytes available
        assertThrows(IOException.class, () -> test.read());
    }


    @SuppressWarnings("resource")
    @Test
    public void testWriteEmptyArray() throws IOException {
        // GIVEN an empty instance
        final ByteReaderWriter test = new ByteReaderWriter();

        // WHEN an empty array is written
        test.write(new byte[0]);

        // THEN
        // - it should throw an IOException when reading indicating no bytes available
        assertThrows(IOException.class, () -> test.read());
    }


    @SuppressWarnings("resource")
    @Test
    public void testReadCorrect() throws IOException {
        // GIVEN an instance with an array of 3 bytes
        final ByteReaderWriter test = new ByteReaderWriter();
        test.write(new byte[] { 1, 2, 3 });

        // WHEN an array of length 3 is read
        final byte[] testArray = new byte[3];
        final int result = test.read(testArray);

        // THEN
        // - the read count should be 3
        // - 3 bytes should be read into the array
        assertEquals(3, result);
        assertArrayEquals(new byte[] { 1, 2, 3 }, testArray);
    }


    @SuppressWarnings("resource")
    @Test
    public void testReadTooLargeArray() throws IOException {
        // GIVEN an instance with 3 bytes
        final ByteReaderWriter test = new ByteReaderWriter();
        test.write(new byte[] { 1, 2, 3 });

        // WHEN an array is read larger than bytes availabe
        // THEN
        // - it should return -1 when reading indicating not enough bytes available
        assertEquals(-1, test.read(new byte[4]));
    }


    @SuppressWarnings("resource")
    @Test
    public void testReadFromMultipleElementsArray() throws IOException {
        // GIVEN an instance with 3 bytes
        final ByteReaderWriter test = new ByteReaderWriter();
        test.write(new byte[] { 1, 2, 3 });
        test.write(new byte[] { 4, 5, 6 });
        test.write(new byte[] { 7, 8, 9 });
        // WHEN an array is read larger than the first element
        final byte[] testArray = new byte[7];
        test.read(testArray);
        // THEN
        // - it should throw an IOException when reading indicating not enough bytes available
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7 }, testArray);
    }

}
