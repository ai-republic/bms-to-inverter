/**
 * This software is free to use and to distribute in its unchanged form for private use.
 * Commercial use is prohibited without an explicit license agreement of the copyright holder.
 * Any changes to this software must be made solely in the project repository at https://github.com/ai-republic/bms-to-inverter.
 * The copyright holder is not liable for any damages in whatever form that may occur by using this software.
 *
 * (c) Copyright 2022 and onwards - Torsten Oltmanns
 *
 * @author Torsten Oltmanns - bms-to-inverter''AT''gmail.com
 */
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
     * @param baudrate the baudrate
     */
    public CANPort(final String portname, final int baudrate) {
        super(portname, baudrate);
    }


    /**
     * Sends a extended CAN frame.
     *
     * @param frame the frame {@link ByteBuffer}
     * @throws IOException if an exception occurs
     */
    public abstract void sendExtendedFrame(ByteBuffer frame) throws IOException;
}
