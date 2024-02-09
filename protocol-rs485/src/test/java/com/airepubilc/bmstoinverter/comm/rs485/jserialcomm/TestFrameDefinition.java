package com.airepubilc.bmstoinverter.comm.rs485.jserialcomm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.airepublic.bmstoinverter.protocol.rs485.FrameDefinition;
import com.airepublic.bmstoinverter.protocol.rs485.FrameDefinitionPartType;
import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

public class TestFrameDefinition {
    SerialPort getPort() {
        final SerialPort[] ports = SerialPort.getCommPorts();
        if (ports != null && ports.length > 0) {
            return ports[0];
        }

        throw new IllegalArgumentException("No port available on this machine!");
    }


    @Test
    public void testCreateWithCorrectDefinition() {
        final FrameDefinition def = FrameDefinition.create("SSACOOLD");
        assertEquals(6, def.size());
        assertEquals(2, def.get(0).getByteCount());
        assertEquals(FrameDefinitionPartType.START_FLAG, def.get(0).getType());
        assertEquals(1, def.get(1).getByteCount());
        assertEquals(FrameDefinitionPartType.ADDRESS, def.get(1).getType());
        assertEquals(1, def.get(2).getByteCount());
        assertEquals(FrameDefinitionPartType.COMMAND, def.get(2).getType());
        assertEquals(2, def.get(3).getByteCount());
        assertEquals(FrameDefinitionPartType.OTHER, def.get(3).getType());
        assertEquals(1, def.get(4).getByteCount());
        assertEquals(FrameDefinitionPartType.LENGTH, def.get(4).getType());
        assertEquals(1, def.get(5).getByteCount());
        assertEquals(FrameDefinitionPartType.DATA, def.get(5).getType());
    }


    @Test
    public void testCreateWithWrongDefinition() {
        assertThrows(IllegalArgumentException.class, () -> FrameDefinition.create("SSAOBD"));
    }


    @Test
    public void testParseExactFit() {
        final byte[] testBytes = new byte[] { 1, 2, 3, 4, 5, 6, 2, 8, 9 };
        final ByteBuffer buf = FrameDefinition.create("SSACOOLD").parse(testBytes);

        assertEquals(9, buf.capacity());
    }


    @Test
    public void testParseTooSmallFit() {
        final byte[] testBytes = new byte[] { 1, 2, 3, 4, 5, 6, 2, 8 };

        assertThrows(IndexOutOfBoundsException.class, () -> FrameDefinition.create("SSACOOLD").parse(testBytes));
    }


    @Test
    public void testParseTooManyFit() {
        final byte[] testBytes = new byte[] { 1, 2, 3, 4, 5, 6, 2, 8, 9, 10, 11 };
        final ByteBuffer buf = FrameDefinition.create("SSACOOLD").parse(testBytes);

        assertEquals(9, buf.capacity());
    }


    @Test
    public void testCorrectSingleFrame() throws IOException {
        final JSerialCommPort port = new JSerialCommPort("", 9600, 8, 1, SerialPort.NO_PARITY, new byte[] { (byte) 0xA5 }, FrameDefinition.create("SACLOODVV"));
        port.serialEvent(new SerialPortEvent(getPort(), SerialPort.LISTENING_EVENT_DATA_RECEIVED, new byte[] { Integer.valueOf(0xA5).byteValue(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A }));

        final ByteBuffer frame = port.getNextFrame();
        assertNotNull(frame);
        assertArrayEquals(new byte[] { Integer.valueOf(0xA5).byteValue(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A }, frame.array());
    }


    @Test
    public void testCorrectMultipleFrames() throws IOException {
        final JSerialCommPort port = new JSerialCommPort("", 9600, 8, 1, SerialPort.NO_PARITY, new byte[] { (byte) 0xA5 }, FrameDefinition.create("SACLOODVV"));
        port.serialEvent(new SerialPortEvent(getPort(), SerialPort.LISTENING_EVENT_DATA_RECEIVED, new byte[] { Integer.valueOf(0xA5).byteValue(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, Integer.valueOf(0xA5).byteValue(), 0x0C, 0x0D, 0x02, 0x00, 0x00, (byte) 0xAA, (byte) 0xFF, 0x00, 0x00 }));

        ByteBuffer frame = port.getNextFrame();
        assertNotNull(frame);
        assertArrayEquals(new byte[] { Integer.valueOf(0xA5).byteValue(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A }, frame.array());
        frame = port.getNextFrame();
        assertNotNull(frame);
        assertArrayEquals(new byte[] { Integer.valueOf(0xA5).byteValue(), 0x0C, 0x0D, 0x02, 0x00, 0x00, (byte) 0xAA, (byte) 0xFF, 0x00, 0x00 }, frame.array());
    }


    @Test
    public void testCorrectMultipleFramesWithRubbishInBetween() throws IOException {
        final JSerialCommPort port = new JSerialCommPort("", 9600, 8, 1, SerialPort.NO_PARITY, new byte[] { (byte) 0xA5 }, FrameDefinition.create("SACLOODVV"));
        port.serialEvent(new SerialPortEvent(getPort(), SerialPort.LISTENING_EVENT_DATA_RECEIVED, new byte[] { Integer.valueOf(0xA5).byteValue(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x01, 0x02, 0x03, Integer.valueOf(0xA5).byteValue(), 0x0C, 0x0D, 0x02, 0x00, 0x00, (byte) 0xAA, (byte) 0xFF, 0x00, 0x00 }));

        ByteBuffer frame = port.getNextFrame();
        assertNotNull(frame);
        assertArrayEquals(new byte[] { Integer.valueOf(0xA5).byteValue(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A }, frame.array());
        frame = port.getNextFrame();
        assertNotNull(frame);
        assertArrayEquals(new byte[] { Integer.valueOf(0xA5).byteValue(), 0x0C, 0x0D, 0x02, 0x00, 0x00, (byte) 0xAA, (byte) 0xFF, 0x00, 0x00 }, frame.array());
    }


    @Test
    public void testCorrectMultipleFramesWithRubbishInBetweenAndStart() throws IOException {
        final JSerialCommPort port = new JSerialCommPort("", 9600, 8, 1, SerialPort.NO_PARITY, new byte[] { (byte) 0xA5 }, FrameDefinition.create("SACLOODVV"));
        port.serialEvent(new SerialPortEvent(getPort(), SerialPort.LISTENING_EVENT_DATA_RECEIVED, new byte[] { 0x01, 0x02, 0x03, Integer.valueOf(0xA5).byteValue(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x01, 0x02, 0x03, Integer.valueOf(0xA5).byteValue(), 0x0C, 0x0D, 0x02, 0x00, 0x00, (byte) 0xAA, (byte) 0xFF, 0x00, 0x00 }));

        ByteBuffer frame = port.getNextFrame();
        assertNotNull(frame);
        assertArrayEquals(new byte[] { Integer.valueOf(0xA5).byteValue(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A }, frame.array());
        frame = port.getNextFrame();
        assertNotNull(frame);
        assertArrayEquals(new byte[] { Integer.valueOf(0xA5).byteValue(), 0x0C, 0x0D, 0x02, 0x00, 0x00, (byte) 0xAA, (byte) 0xFF, 0x00, 0x00 }, frame.array());
    }

}
