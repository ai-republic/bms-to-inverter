package com.airepubilc.bmstoinverter.comm.rs485.jserialcomm;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

public class TestJSerialCommPort {
    private final static Logger LOG = LoggerFactory.getLogger(TestJSerialCommPort.class);
    private final static byte[] request = new byte[] { (byte) 0xA5, (byte) 0x40, (byte) 0x96, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x83 };
    // private final static byte[] request = new byte[] { (byte) 0xA5, (byte) 0x40, (byte) 0x90,
    // (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    // (byte) 0x00, (byte) 0x00, (byte) 0x7D };

    public static void main(final String[] args) throws Exception {
        final JSerialCommPort port = new JSerialCommPort();
        port.setStartFlag(0xA5);
        port.setBaudrate(9600);
        port.setPortname("com3");
        port.setFrameLength(13);
        port.open();

        int checksum = 0;
        for (int i = 0; i < request.length - 1; i++) {
            checksum += (byte) Byte.toUnsignedInt(request[i]);
        }
        System.out.println("Checksum: 0x" + String.format("%02X", (byte) checksum));

        do {
            port.sendFrame(ByteBuffer.wrap(request));
            Thread.sleep(100);

            port.receiveFrame(t -> true);
        } while (port.isOpen());
        port.close();
    }


    SerialPort getPort() {
        final SerialPort[] ports = SerialPort.getCommPorts();
        if (ports != null && ports.length > 0) {
            return ports[0];
        }

        throw new IllegalArgumentException("No port available on this machine!");
    }


    @Test
    public void testSerialEventValidFullFrame() {
        // GIVEN: a port configured with startflag and framelength
        final JSerialCommPort port = new JSerialCommPort();
        port.setStartFlag(0xA5);
        port.setFrameLength(13);

        // WHEN:
        // a valid full frame is received
        port.serialEvent(new SerialPortEvent(getPort(), SerialPort.LISTENING_EVENT_DATA_RECEIVED, new byte[] { Integer.valueOf(0xA5).byteValue(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C }));

        // THEN:
        // a frame should be added to the queue
        Assertions.assertEquals(1, port.getQueue().size());
    }


    @Test
    public void testSerialEventInvalidFullFrame() {
        // GIVEN: a port configured with startflag and framelength
        final JSerialCommPort port = new JSerialCommPort();
        port.setStartFlag(0xA5);
        port.setFrameLength(13);

        // WHEN:
        // an invalid full frame is received
        port.serialEvent(new SerialPortEvent(getPort(), SerialPort.LISTENING_EVENT_DATA_RECEIVED, new byte[] { Integer.valueOf(0xA4).byteValue(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C }));

        // THEN:
        // no frame should be added to the queue
        Assertions.assertEquals(0, port.getQueue().size());
    }


    @Test
    public void testSerialEventShiftedFullFrame() {
        // GIVEN: a port configured with startflag and framelength
        final JSerialCommPort port = new JSerialCommPort();
        port.setStartFlag(0xA5);
        port.setFrameLength(13);

        // WHEN:
        // a full frame with startflag in wrong place is received
        port.serialEvent(new SerialPortEvent(getPort(), SerialPort.LISTENING_EVENT_DATA_RECEIVED, new byte[] { 0x00, 0x01, Integer.valueOf(0xA5).byteValue(), 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C }));

        // THEN:
        // no frame should be added to the queue
        Assertions.assertEquals(0, port.getQueue().size());
    }


    @Test
    public void testSerialEventShiftedTooLongFrame() {
        // GIVEN: a port configured with startflag and framelength
        final JSerialCommPort port = new JSerialCommPort();
        port.setStartFlag(0xA5);
        port.setFrameLength(13);

        // WHEN:
        // a full frame with startflag in wrong place is received
        port.serialEvent(new SerialPortEvent(getPort(), SerialPort.LISTENING_EVENT_DATA_RECEIVED, new byte[] { 0x00, 0x01, Integer.valueOf(0xA5).byteValue(), 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F }));

        // THEN:
        // 1 frame should be added to the queue
        Assertions.assertEquals(1, port.getQueue().size());
        // there should be the too long bytes in the framebuffer
        Assertions.assertEquals(0x0F, port.getFrameBuffer().get(0));
    }

}
