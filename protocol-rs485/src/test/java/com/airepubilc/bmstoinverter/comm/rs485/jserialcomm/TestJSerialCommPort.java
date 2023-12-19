package com.airepubilc.bmstoinverter.comm.rs485.jserialcomm;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;

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
}
