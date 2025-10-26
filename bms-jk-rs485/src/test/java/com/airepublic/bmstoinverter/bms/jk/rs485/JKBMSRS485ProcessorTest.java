package com.airepublic.bmstoinverter.bms.jk.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.protocol.rs485.FrameDefinition;
import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

public class JKBMSRS485ProcessorTest {
    private final static JKBmsRS485Processor processor = new JKBmsRS485Processor();
    private final static JKBmsRS485Descriptor descriptor = new JKBmsRS485Descriptor() {
        @Override
        public Port createPort(final BMSConfig config) {
            return port;
        }
    };

    private final static BMSConfig bmsConfig = new BMSConfig(1, "port0", 9600, 200, descriptor);
    private final static JSerialCommPort port = new JSerialCommPort(bmsConfig.getPortLocator(), bmsConfig.getBaudRate(), 8, 1, SerialPort.NO_PARITY, new byte[] { 0x4E, 0x57 }, FrameDefinition.create("SSLL(-18)AAAACOODOOOOOVVVV")) {
        @Override
        public boolean isOpen() {
            return true;
        }


        @Override
        public void sendFrame(final ByteBuffer frame) throws IOException {
            // do nothing
        }
    };
    private final SerialPort serialPort = Mockito.mock(SerialPort.class);

    @BeforeAll
    public static void setupOnce() {
        processor.initialize(bmsConfig);
    }


    @Test
    public void testReadFrame() {
        final byte[] rawData = new byte[] { (byte) 0x4E, (byte) 0x57, (byte) 0x01, (byte) 0x09, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x79, (byte) 0x18, (byte) 0x01, (byte) 0x0D, (byte) 0x04, (byte) 0x02, (byte) 0x0D, (byte) 0x05, (byte) 0x03, (byte) 0x0D, (byte) 0x04, (byte) 0x04, (byte) 0x0D, (byte) 0x04, (byte) 0x05, (byte) 0x0D, (byte) 0x04, (byte) 0x06, (byte) 0x0D, (byte) 0x04,
                (byte) 0x07, (byte) 0x0D, (byte) 0x05, (byte) 0x08, (byte) 0x0D, (byte) 0x04, (byte) 0x80, (byte) 0x00, (byte) 0x15, (byte) 0x81, (byte) 0x00, (byte) 0x12, (byte) 0x82, (byte) 0x00, (byte) 0x12, (byte) 0x83, (byte) 0x0A, (byte) 0x69, (byte) 0x84, (byte) 0x00, (byte) 0x00, (byte) 0x85, (byte) 0x64, (byte) 0x86, (byte) 0x02, (byte) 0x87, (byte) 0x00, (byte) 0x00, (byte) 0x89, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10,
                (byte) 0x8A, (byte) 0x00, (byte) 0x08, (byte) 0x8B, (byte) 0x00, (byte) 0x00, (byte) 0x8C, (byte) 0x00, (byte) 0x03, (byte) 0x8E, (byte) 0x0B, (byte) 0x40, (byte) 0x8F, (byte) 0x08, (byte) 0x20, (byte) 0x90, (byte) 0x0E, (byte) 0x10, (byte) 0x91, (byte) 0x0D, (byte) 0xDE, (byte) 0x92, (byte) 0x00, (byte) 0x03, (byte) 0x93, (byte) 0x0A, (byte) 0x28, (byte) 0x94, (byte) 0x0A, (byte) 0x5A, (byte) 0x95, (byte) 0x00, (byte) 0x03,
                (byte) 0x96, (byte) 0x01, (byte) 0x2C, (byte) 0x97, (byte) 0x00, (byte) 0xC8, (byte) 0x98, (byte) 0x01, (byte) 0x2C, (byte) 0x99, (byte) 0x00, (byte) 0x64, (byte) 0x9A, (byte) 0x00, (byte) 0x1E, (byte) 0x9B, (byte) 0x0B, (byte) 0xB8, (byte) 0x9C, (byte) 0x00, (byte) 0x0A, (byte) 0x9D, (byte) 0x01, (byte) 0x9E, (byte) 0x00, (byte) 0x64, (byte) 0x9F, (byte) 0x00, (byte) 0x50, (byte) 0xA0, (byte) 0x00, (byte) 0x46, (byte) 0xA1,
                (byte) 0x00, (byte) 0x3C, (byte) 0xA2, (byte) 0x00, (byte) 0x14, (byte) 0xA3, (byte) 0x00, (byte) 0x46, (byte) 0xA4, (byte) 0x00, (byte) 0x46, (byte) 0xA5, (byte) 0xFF, (byte) 0xEC, (byte) 0xA6, (byte) 0xFF, (byte) 0xF6, (byte) 0xA7, (byte) 0xFF, (byte) 0xEC, (byte) 0xA8, (byte) 0xFF, (byte) 0xF6, (byte) 0xA9, (byte) 0x08, (byte) 0xAA, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x18, (byte) 0xAB, (byte) 0x01, (byte) 0xAC,
                (byte) 0x01, (byte) 0xAD, (byte) 0x03, (byte) 0xAE, (byte) 0xAE, (byte) 0x01, (byte) 0xAF, (byte) 0x00, (byte) 0xB0, (byte) 0x00, (byte) 0x0A, (byte) 0xB1, (byte) 0x14, (byte) 0xB2, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB3, (byte) 0x00, (byte) 0xB4, (byte) 0x49, (byte) 0x6E, (byte) 0x70, (byte) 0x75, (byte) 0x74, (byte) 0x20,
                (byte) 0x55, (byte) 0x73, (byte) 0xB5, (byte) 0x32, (byte) 0x35, (byte) 0x30, (byte) 0x38, (byte) 0xB6, (byte) 0x00, (byte) 0x00, (byte) 0x2A, (byte) 0x32, (byte) 0xB7, (byte) 0x31, (byte) 0x31, (byte) 0x41, (byte) 0x5F, (byte) 0x5F, (byte) 0x5F, (byte) 0x5F, (byte) 0x5F, (byte) 0x53, (byte) 0x31, (byte) 0x31, (byte) 0x2E, (byte) 0x35, (byte) 0x34, (byte) 0x5F, (byte) 0xB8, (byte) 0x00, (byte) 0xB9, (byte) 0x00, (byte) 0x00,
                (byte) 0x01, (byte) 0x18, (byte) 0xBA, (byte) 0x49, (byte) 0x6E, (byte) 0x70, (byte) 0x75, (byte) 0x74, (byte) 0x20, (byte) 0x55, (byte) 0x73, (byte) 0x65, (byte) 0x72, (byte) 0x64, (byte) 0x61, (byte) 0x34, (byte) 0x30, (byte) 0x37, (byte) 0x32, (byte) 0x37, (byte) 0x32, (byte) 0x43, (byte) 0x32, (byte) 0x37, (byte) 0x32, (byte) 0x37, (byte) 0x00, (byte) 0xC0, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x68, (byte) 0x00, (byte) 0x00, (byte) 0x46, (byte) 0xF1 };

        final SerialPortEvent event = new SerialPortEvent(serialPort, SerialPort.LISTENING_EVENT_DATA_RECEIVED, rawData);
        port.serialEvent(event);

        processor.collectData(port);

        System.out.println(processor.getEnergyStorage().toJson());
    }

}
