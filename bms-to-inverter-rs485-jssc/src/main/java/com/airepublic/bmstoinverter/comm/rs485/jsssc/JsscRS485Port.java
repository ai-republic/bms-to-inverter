package com.airepublic.bmstoinverter.comm.rs485.jsssc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.rs485.RS485;
import com.airepublic.bmstoinverter.rs485.RS485Port;

import jssc.SerialPort;
import jssc.SerialPortException;

@RS485
public class JsscRS485Port extends RS485Port {
    private final static Logger LOG = LoggerFactory.getLogger(JsscRS485Port.class);

    private SerialPort serialPort;

    @Override
    public void open() throws IOException {
        try {
            serialPort = new SerialPort(getPortname());
            // open port
            serialPort.openPort();
            // set port configuration
            serialPort.setParams(getBaudrate(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            // set event mask
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR);
            // serialPort.setDTR(true);
            serialPort.purgePort(SerialPort.PURGE_TXCLEAR & SerialPort.PURGE_RXCLEAR);

        } catch (final SerialPortException e) {
            LOG.error("Could not open port {}!", getPortname(), e);
        }
    }


    @Override
    public boolean isOpen() {
        return serialPort.isOpened();
    }


    @Override
    public void close() throws IOException {
        try {
            serialPort.closePort();
        } catch (final SerialPortException e) {
            throw new IOException("Port could not be closed!", e);
        }
    }


    private byte[] readOverlapping(final byte[] bytes) throws IOException {
        int value = 0;
        final byte[] frame = new byte[getFrameLength()];
        boolean foundStart = false;
        int pos = 0;

        // first check the faulty buffer for a start flag
        for (final byte element : bytes) {
            value = element;

            if (!foundStart && value == getStartFlag()) {
                foundStart = true;
            }

            if (foundStart && pos < getFrameLength()) {
                frame[pos++] = element;
            }
        }

        // otherwise read from the stream until the start flag is found
        if (!foundStart) {
            do {
                try {
                    value = Byte.toUnsignedInt(serialPort.readBytes(1)[0]);
                } catch (final SerialPortException e) {
                    throw new IOException("Error reading bytes from port " + serialPort.getPortName());
                }

            } while (value != getStartFlag());

            frame[pos++] = (byte) getStartFlag();
        }

        // fill up the rest of the frame from bytes read from the stream
        for (int i = pos; i < getFrameLength(); i++) {
            try {
                frame[i] = serialPort.readBytes(1)[0];
            } catch (final SerialPortException e) {
                throw new IOException("Error reading bytes from port " + serialPort.getPortName());
            }
        }

        return frame;
    }


    @Override
    public void sendFrame(final ByteBuffer txBuffer) throws IOException {
        // try {
        // int[] chr = null;
        //
        // do // clear all incoming serial to avoid data collision
        // {
        // chr = my_serialIntf.readIntArray(1);
        // } while (chr != null);
        // } catch (final SerialPortException e) {
        // throw new IOException(e);
        // }

        try {
            // serialPort.setRTS(true);
            serialPort.writeBytes(txBuffer.array());
        } catch (final SerialPortException e) {
            throw new IOException(e);
        }
    }


    @Override
    public ByteBuffer receiveFrame(final Predicate<byte[]> validator) throws IOException {
        // Read bytes from the specified serial interface
        try {
            byte[] bytes = serialPort.readBytes(getFrameLength());

            if (bytes == null || bytes.length != getFrameLength()) {
                LOG.info("Received wrong number of bytes: \n{}", printBytes(bytes));
                bytes = readOverlapping(bytes);
            }

            // check valid start flag
            if (Byte.toUnsignedInt(bytes[0]) != getStartFlag()) {
                // handle wrong message start
                LOG.info("Received wrong startflag: \n{}", printBytes(bytes));
                bytes = readOverlapping(bytes);
            }

            return ByteBuffer.wrap(bytes);

        } catch (final SerialPortException e) {
            throw new IOException(e);
        }
    }

}
