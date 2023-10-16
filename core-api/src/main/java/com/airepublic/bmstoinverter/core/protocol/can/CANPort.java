package com.airepublic.bmstoinverter.core.protocol.can;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.core.Port;

import jakarta.annotation.PostConstruct;

public abstract class CANPort extends Port {
    public CANPort() {
    }


    public CANPort(final String portname) {
        super(portname);
    }


    @PostConstruct
    public void init() {
        if (System.getProperties().containsKey("CAN.portname")) {
            setPortname(System.getProperty("CAN.portname"));
        }
    }


    public abstract void sendExtendedFrame(ByteBuffer frame) throws IOException;
}