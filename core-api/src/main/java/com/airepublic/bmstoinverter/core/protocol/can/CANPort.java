package com.airepublic.bmstoinverter.core.protocol.can;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.core.Port;

/**
 * A {@link Port} that is used for CAN messages.
 */
public abstract class CANPort extends Port {
    /**
     * Constructor.
     */
    public CANPort() {
    }


    /**
     * Constructor.
     * 
     * @param portname the portname
     */
    public CANPort(final String portname) {
        super(portname);
    }


    /**
     * Sends a extended CAN frame.
     *
     * @param frame the frame {@link ByteBuffer}
     * @throws IOException if an exception occurs
     */
    public abstract void sendExtendedFrame(ByteBuffer frame) throws IOException;
}