package com.airepublic.bmstoinverter.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Predicate;

/**
 * The definition of a communication port, e.g. for RS485, CAN, ModBus, etc.
 */
public abstract class Port implements AutoCloseable {
    private String portname;

    /**
     * Constructor.
     */
    public Port() {
    }


    /**
     * Constructor.
     * 
     * @param portname the name of the system port, e.g. COM4, TtyS0, etc.
     */
    public Port(final String portname) {
        this.portname = portname;
    }


    /**
     * Creates a new instance of this class with the specified portname.
     *
     * @param portname the portname
     * @return the instance
     */
    protected abstract Port create(String portname);


    /**
     * Gets the portname.
     *
     * @return the portname
     */
    public String getPortname() {
        return portname;
    }


    /**
     * Sets the portname.
     *
     * @param portname the portname
     */
    public void setPortname(final String portname) {
        this.portname = portname;
    }


    /**
     * Opens the port for communication.
     * 
     * @throws IOException if port could not be opened
     */
    public abstract void open() throws IOException;


    /**
     * Returns true if port is open, otherwise false.
     *
     * @return true if port is open, otherwise false.
     */
    public abstract boolean isOpen();


    /**
     * Receives a frame from the ports stream.
     * 
     * @param validator an optional validator to check e.g. the checksum
     * @return the {@link ByteBuffer} that was read
     * @throws IOException if an exception occurs
     */
    public abstract ByteBuffer receiveFrame(Predicate<byte[]> validator) throws IOException;


    /**
     * Sends a frame to the ports stream.
     * 
     * @param frame the {@link ByteBuffer} to send
     * @throws IOException if an exception occurs
     */
    public abstract void sendFrame(ByteBuffer frame) throws IOException;


    @Override
    public abstract void close() throws IOException;


    /**
     * Utility method to print the contents of the buffer as HEX and INT string.
     *
     * @param buffer the {@link ByteBuffer} to print
     * @return the buffer as HEX and INT string
     */
    public static String printBuffer(final ByteBuffer buffer) {
        final StringBuffer hex = new StringBuffer("RX Buffer (HEX): [");
        final StringBuffer dec = new StringBuffer("RX Buffer:(INT) [");

        for (int i = 0; i < buffer.capacity(); i++) {
            hex.append("0x" + String.format("%02X", buffer.get(i)));
            dec.append(Byte.toUnsignedInt(buffer.get(i)));

            if (i < buffer.capacity() - 1) {
                hex.append(", ");
                dec.append(", ");
            }
        }

        hex.append("]\n");
        dec.append("]\n");

        return hex.toString();// + dec.toString();
    }


    /**
     * Utility method to print the contents of the byte array as HEX and INT string.
     *
     * @param buffer the byte array to print
     * @return the buffer as HEX and INT string
     */
    public static String printBytes(final byte[] bytes) {
        final StringBuffer hex = new StringBuffer("RX Buffer (HEX): [");
        final StringBuffer dec = new StringBuffer("RX Buffer:(INT) [");

        for (int i = 0; i < bytes.length; i++) {
            hex.append("0x" + String.format("%02X", bytes[i]));
            dec.append(Byte.toUnsignedInt(bytes[i]));

            if (i < bytes.length - 1) {
                hex.append(", ");
                dec.append(", ");
            }
        }

        hex.append("]\n");
        dec.append("]\n");

        return hex.toString();// + dec.toString();
    }


    @Override
    public String toString() {
        return getClass().getName() + "[portname=" + getPortname() + "]";
    }

}
