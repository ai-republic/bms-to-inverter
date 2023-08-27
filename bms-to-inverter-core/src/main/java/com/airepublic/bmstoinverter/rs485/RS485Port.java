package com.airepublic.bmstoinverter.rs485;

import com.airepublic.bmstoinverter.Port;

import jakarta.annotation.PostConstruct;

/**
 * Expecting System properties "rs485.baudrate", "rs485.startFlag" and "rs485.frameLength" to be
 * set.
 */
@RS485
public abstract class RS485Port extends Port {
    private int baudrate = 9600;
    private int startFlag = 0;
    private int frameLength = 0;

    @PostConstruct
    public void init() {
        baudrate = Integer.parseInt(System.getProperty("RS485.baudrate"));
        startFlag = Integer.parseInt(System.getProperty("RS485.startFlag"));
        frameLength = Integer.parseInt(System.getProperty("RS485.frameLength"));
    }


    public int getBaudrate() {
        return baudrate;
    }


    public void setBaudrate(final int baudrate) {
        this.baudrate = baudrate;
    }


    public int getStartFlag() {
        return startFlag;
    }


    public void setStartFlag(final int startFlag) {
        this.startFlag = startFlag;
    }


    public int getFrameLength() {
        return frameLength;
    }


    public void setFrameLength(final int frameLength) {
        this.frameLength = frameLength;
    }

}