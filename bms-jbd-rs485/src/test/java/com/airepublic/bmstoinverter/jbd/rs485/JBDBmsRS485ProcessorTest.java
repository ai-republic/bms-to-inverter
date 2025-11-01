package com.airepublic.bmstoinverter.jbd.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.airepublic.bmstoinverter.bms.jbd.rs485.JBDBmsRS485Descriptor;
import com.airepublic.bmstoinverter.bms.jbd.rs485.JBDBmsRS485Processor;
import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.NoDataAvailableException;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.protocol.rs485.FrameDefinition;
import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

public class JBDBmsRS485ProcessorTest {
    private final static JBDBmsRS485Processor processor = new JBDBmsRS485Processor();
    private final static JBDBmsRS485Descriptor descriptor = new JBDBmsRS485Descriptor() {
        @Override
        public Port createPort(final BMSConfig config) {
            return port;
        }
    };

    private final static BMSConfig bmsConfig = new BMSConfig(1, "port0", 9600, 200, descriptor);
    private final static JSerialCommPort port = new JSerialCommPort(bmsConfig.getPortLocator(), bmsConfig.getBaudRate(), 8, 1, SerialPort.NO_PARITY, new byte[] { (byte) 0xDD }, FrameDefinition.create("SCOLDVVO")) {
        @Override
        public boolean isOpen() {
            return true;
        }


        @Override
        public void sendFrame(final ByteBuffer frame) throws IOException {
            // do nothing
        }


        @Override
        public void clearBuffers() {
            // do nothing
        }
    };
    private final SerialPort serialPort = Mockito.mock(SerialPort.class);

    @BeforeAll
    public static void setupOnce() {
        processor.initialize(bmsConfig);
    }


    @Test
    public void testReadFrame03() throws Throwable {
        final byte[] frame = new byte[] {
                (byte) 0xDD, 0x03, 0x00, 0x1B, 0x17, 0x00, 0x00, 0x00, 0x02, (byte) 0xD0, 0x03, (byte) 0xE8,
                0x00, 0x00, 0x20, 0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x48,
                0x03, 0x0F, 0x02, 0x0B, 0x76, 0x0B, (byte) 0x82, (byte) 0xFB, (byte) 0xFF, 0x77
        };
        final SerialPortEvent event = new SerialPortEvent(serialPort, SerialPort.LISTENING_EVENT_DATA_RECEIVED, frame);
        port.serialEvent(event);

        // expect exception collecting data
        Assertions.assertThrowsExactly(NoDataAvailableException.class, () -> {
            processor.collectData(port);
        });

        Assertions.assertEquals(588, processor.getBatteryPack(0).packVoltage);
        Assertions.assertEquals(720, processor.getBatteryPack(0).packSOC);
        Assertions.assertEquals(209, processor.getBatteryPack(0).tempAverage);
    }


    @Test
    public void testReadFrame04() throws Throwable {
        final byte[] frame = new byte[] { (byte) 0xDD, (byte) 0x04, (byte) 0x00, (byte) 0x1E, (byte) 0x0F, (byte) 0x66, (byte) 0x0F, (byte) 0x63, (byte) 0x0F, (byte) 0x63, (byte) 0x0F, (byte) 0x64, (byte) 0x0F, (byte) 0x3E, (byte) 0x0F, (byte) 0x63, (byte) 0x0F, (byte) 0x37, (byte) 0x0F, (byte) 0x5B, (byte) 0x0F, (byte) 0x65, (byte) 0x0F, (byte) 0x3B, (byte) 0x0F, (byte) 0x63, (byte) 0x0F, (byte) 0x63, (byte) 0x0F, (byte) 0x3C, (byte) 0x0F,
                (byte) 0x66, (byte) 0x0F, (byte) 0x3D, (byte) 0xF9, (byte) 0xF9, (byte) 0x77 };
        final SerialPortEvent event = new SerialPortEvent(serialPort, SerialPort.LISTENING_EVENT_DATA_RECEIVED, frame);
        port.serialEvent(event);

        // expect exception collecting data
        Assertions.assertThrowsExactly(NoDataAvailableException.class, () -> {
            processor.collectData(port);
        });

        Assertions.assertEquals(3900, processor.getBatteryPack(0).cellVmV[12]);
    }


    @Test
    public void testReadFrame05() throws Throwable {
        final byte[] frame = new byte[] { (byte) 0xDD, (byte) 0x05, (byte) 0x00, (byte) 0x0A, (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0xFD, (byte) 0xE9, (byte) 0x77 };
        final SerialPortEvent event = new SerialPortEvent(serialPort, SerialPort.LISTENING_EVENT_DATA_RECEIVED, frame);
        port.serialEvent(event);

        // expect exception collecting data
        Assertions.assertThrowsExactly(NoDataAvailableException.class, () -> {
            processor.collectData(port);
        });

        Assertions.assertEquals("0123456789", processor.getBatteryPack(0).hardwareVersion);
    }


    @Test
    public void testChecksum03() {
        final byte[] frame = new byte[] {
                (byte) 0xDD, 0x03, 0x00, 0x1B, 0x17, 0x00, 0x00, 0x00, 0x02, (byte) 0xD0, 0x03, (byte) 0xE8,
                0x00, 0x00, 0x20, 0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x48,
                0x03, 0x0F, 0x02, 0x0B, 0x76, 0x0B, (byte) 0x82, (byte) 0xFB, (byte) 0xFF, 0x77
        };

        final short checksum = JBDBmsRS485Processor.compute(frame);

        System.out.printf("Calculated checksum = 0x%04X%n", checksum);
        Assertions.assertEquals((short) 0xFBFF, checksum);
    }


    @Test
    public void testChecksum04() {
        final byte[] frame = new byte[] { (byte) 0xDD, (byte) 0x04, (byte) 0x00, (byte) 0x1E, (byte) 0x0F, (byte) 0x66, (byte) 0x0F, (byte) 0x63, (byte) 0x0F, (byte) 0x63, (byte) 0x0F, (byte) 0x64, (byte) 0x0F, (byte) 0x3E, (byte) 0x0F, (byte) 0x63, (byte) 0x0F, (byte) 0x37, (byte) 0x0F, (byte) 0x5B, (byte) 0x0F, (byte) 0x65, (byte) 0x0F, (byte) 0x3B, (byte) 0x0F, (byte) 0x63, (byte) 0x0F, (byte) 0x63, (byte) 0x0F, (byte) 0x3C, (byte) 0x0F,
                (byte) 0x66, (byte) 0x0F, (byte) 0x3D, (byte) 0xF9, (byte) 0xF9, (byte) 0x77 };

        final short checksum = JBDBmsRS485Processor.compute(frame);

        System.out.printf("Calculated checksum = 0x%04X%n", checksum);
        Assertions.assertEquals((short) 0xF9F9, checksum);
    }


    @Test
    public void testChecksum05() {
        final byte[] frame = new byte[] { (byte) 0xDD, (byte) 0x05, (byte) 0x00, (byte) 0x0A, (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0xFD, (byte) 0xE9, (byte) 0x77 };

        final short checksum = JBDBmsRS485Processor.compute(frame);

        System.out.printf("Calculated checksum = 0x%04X%n", checksum);
        Assertions.assertEquals((short) 0xFDE9, checksum);
    }

}
