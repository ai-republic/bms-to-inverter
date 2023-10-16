package com.airepublic.bmstoinverter.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Predicate;

public abstract class Port implements AutoCloseable {
    private String portname;

    public Port() {
    }


    public Port(final String portname) {
        this.portname = portname;
    }


    public String getPortname() {
        return portname;
    }


    public void setPortname(final String portname) {
        this.portname = portname;
    }


    public abstract void open() throws IOException;


    public abstract boolean isOpen();


    public abstract ByteBuffer receiveFrame(Predicate<byte[]> validator) throws IOException;


    public abstract void sendFrame(ByteBuffer frame) throws IOException;


    @Override
    public abstract void close() throws IOException;


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
