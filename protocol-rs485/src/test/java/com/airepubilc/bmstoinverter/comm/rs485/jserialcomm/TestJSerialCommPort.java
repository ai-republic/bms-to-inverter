package com.airepubilc.bmstoinverter.comm.rs485.jserialcomm;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Port;
import com.fazecast.jSerialComm.SerialPort;

public class TestJSerialCommPort {
    private final static Logger LOG = LoggerFactory.getLogger(TestJSerialCommPort.class);
    private final static byte[] request = new byte[] { (byte) 0xA5, (byte) 0x40, (byte) 0x91, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7E };
    private static SerialPort port;

    public static void main(final String[] args) throws IOException {
        port = SerialPort.getCommPort("com3");
        port.setComPortParameters(9600, 8, 1, SerialPort.NO_PARITY, true);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
        port.openPort();

        do {
            sendFrame(ByteBuffer.wrap(request));
            receiveFrame();
        } while (port.isOpen());
        port.closePort();
    }


    static ByteBuffer receiveFrame() throws IOException {

        // read frame
        final byte[] bytes = new byte[13];
        final int read = port.getInputStream().read(bytes);

        LOG.debug("initial read: {}", Port.printBytes(bytes));

        if (read != bytes.length) {
            throw new IOException("Wrong number of bytes read!");
        }

        do {
            int startPos = 0;

            // verify it starts with the startflag
            if (bytes[0] != (byte) 0xA5) {
                // otherwise search within the received bytes for a startflag
                do {
                    startPos++;
                } while (startPos < bytes.length && bytes[startPos] != (byte) 0xA5);

                // if a startflag was found
                if (startPos < bytes.length && bytes[startPos] == (byte) 0xA5) {
                    // shift the bytes left from the found startflag
                    System.arraycopy(bytes, startPos, bytes, 0, bytes.length - startPos);
                    LOG.debug("copied startflag to beginning: {}", Port.printBytes(bytes));

                    // fill up the rest with the next bytes from the stream
                    startPos = bytes.length - startPos;
                    port.getInputStream().read(bytes, startPos, bytes.length - startPos);

                    LOG.debug("final filling up:  {}", Port.printBytes(bytes));
                } else {
                    // otherwise continue to read the next frame
                    LOG.debug("ignoring frame: {}", Port.printBytes(bytes));
                    return receiveFrame();
                }
            }

            // now we should have a frame starting with the start flag
            // next is to validate the checksum to see if its a valid frame
            int checksum = 0;
            for (int i = 0; i < bytes.length - 1; i++) {
                checksum += (byte) Byte.toUnsignedInt(bytes[i]);
            }

            if (bytes[12] != (byte) checksum) {
                LOG.debug("ERROR: checksum failed!");

                // read the bytes from pos 1 to find possible other startflag
                // and remove the startflag as first byte
                bytes[0] = 0;
            }
        } while (bytes[0] != (byte) 0xA5);

        return ByteBuffer.wrap(bytes);
    }


    public static void sendFrame(final ByteBuffer frame) throws IOException {
        final byte[] bytes = frame.array();
        LOG.debug("Send: {}", Port.printBytes(bytes));
        port.getOutputStream().write(bytes);
        port.getOutputStream().flush();
    }
}
