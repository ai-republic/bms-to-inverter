package com.airepublic.bmstoinverter.can;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.Port;

import jakarta.annotation.PostConstruct;

public abstract class CANPort extends Port {

    @PostConstruct
    public void init() {
        setPortname(System.getProperty("CAN.portname"));
    }


    public abstract void sendExtendedFrame(ByteBuffer frame) throws IOException;
}