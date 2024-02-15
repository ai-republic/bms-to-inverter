package com.airepublic.bmstoinverter.core;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The definition of a communication port, e.g. for RS485, CAN, ModBus, etc.
 */
public abstract class Port implements AutoCloseable {
    private final static Logger LOG = LoggerFactory.getLogger(Port.class);
    private String portname;
    private int baudrate;

    /**
     * Constructor.
     */
    public Port() {
    }


    /**
     * Constructor.
     * 
     * @param portname the name of the system port, e.g. COM4, TtyS0, etc.
     * @param baudrate the baudrate
     */
    public Port(final String portname, final int baudrate) {
        this.portname = portname;
        this.baudrate = baudrate;
    }


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
     * Gets the baudrate of the port.
     *
     * @return the baudrate of the port
     */
    public int getBaudrate() {
        return baudrate;
    }


    /**
     * Sets the baudrate of the port.
     *
     * @param baudrate the baudrate of the port
     */
    public void setBaudrate(final int baudrate) {
        this.baudrate = baudrate;
    }


    /**
     * Opens the port for communication.
     * 
     * @throws Exception if port could not be opened
     */
    public abstract void open() throws Exception;


    /**
     * Returns true if port is open, otherwise false.
     *
     * @return true if port is open, otherwise false.
     */
    public abstract boolean isOpen();


    /**
     * Ensures the port is open. If it is closed it will reopen the port.
     *
     * @return true if the port is or could be opened otherwise false
     */
    public synchronized boolean ensureOpen() {
        if (!isOpen()) {
            // open port
            try {
                LOG.info("Opening " + getPortname() + " ...");
                open();
                LOG.info("Opening port {} SUCCESSFUL", getPortname());

            } catch (final Throwable e) {
                LOG.error("Opening port {} FAILED!", getPortname(), e);
            }
        }

        return isOpen();
    }


    /**
     * Receives a frame from the ports stream.
     * 
     * @return the {@link ByteBuffer} that was read
     * @throws IOException if an exception occurs
     */
    public abstract ByteBuffer receiveFrame() throws IOException;


    /**
     * Sends a frame to the ports stream.
     * 
     * @param frame the {@link ByteBuffer} to send
     * @throws IOException if an exception occurs
     */
    public abstract void sendFrame(ByteBuffer frame) throws IOException;


    /**
     * Clears any buffers or queues to restart communication.
     */
    public abstract void clearBuffers();


    @Override
    public abstract void close();


    /**
     * Utility method to print the contents of the buffer as HEX and INT string.
     *
     * @param buffer the {@link ByteBuffer} to print
     * @return the buffer as HEX and INT string
     */
    public static String printBuffer(final ByteBuffer buffer) {
        final StringBuffer hex = new StringBuffer("Buffer (HEX): [");
        // final StringBuffer dec = new StringBuffer("Buffer:(INT) [");

        if (buffer == null) {
            hex.append("null");
        } else {
            for (int i = 0; i < buffer.capacity(); i++) {
                hex.append("0x" + String.format("%02X", buffer.get(i)));
                // dec.append(Byte.toUnsignedInt(buffer.get(i)));

                if (i < buffer.capacity() - 1) {
                    hex.append(", ");
                    // dec.append(", ");
                }
            }
        }

        hex.append("]\n");
        // dec.append("]\n");

        return hex.toString();// + dec.toString();
    }


    /**
     * Utility method to print the contents of the byte array as HEX and INT string.
     *
     * @param buffer the byte array to print
     * @return the buffer as HEX and INT string
     */
    public static String printBytes(final byte[] bytes) {
        final StringBuffer hex = new StringBuffer("Buffer (HEX): [");
        // final StringBuffer dec = new StringBuffer("RX Buffer:(INT) [");

        if (bytes == null) {
            hex.append("null");
        } else {
            for (int i = 0; i < bytes.length; i++) {
                hex.append("0x" + String.format("%02X", bytes[i]));
                // dec.append(Byte.toUnsignedInt(bytes[i]));

                if (i < bytes.length - 1) {
                    hex.append(", ");
                    // dec.append(", ");
                }
            }
        }

        hex.append("]\n");
        // dec.append("]\n");

        return hex.toString();// + dec.toString();
    }


    @Override
    public String toString() {
        return getClass().getName() + "[portname=" + getPortname() + "]";
    }

}
