package com.airepublic.bmstoinverter.core.protocol.can;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.core.Port;

import jakarta.annotation.PostConstruct;

public abstract class CANPort extends Port {

    @PostConstruct
    public void init() {
        setPortname(System.getProperty("CAN.portname"));
    }


    public abstract void sendExtendedFrame(ByteBuffer frame) throws IOException;
}