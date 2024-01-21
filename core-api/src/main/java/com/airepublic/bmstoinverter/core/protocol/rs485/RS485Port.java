package com.airepublic.bmstoinverter.core.protocol.rs485;

import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.core.Port;

/**
 * A {@link Port} that is used for RS485 messages. Expecting <code>config.properties</code> or
 * System properties <code>RS485.baudrate</code>, <code>RS485.startFlag</code> and
 * <code>RS485.frameLength</code> to be set.
 */
public abstract class RS485Port extends Port {
    private int dataBits;
    private int stopBits;
    private int parity;
    private int startFlag = 0;
    private int frameLength = 0;
    private ByteBuffer frameBuffer;

    /**
     * Constructor.
     */
    public RS485Port() {
    }


    /**
     * Constructor.
     * 
     * @param portname the portname
     * @param baudrate the baudrate
     */
    public RS485Port(final String portname, final int baudrate, final int dataBits, final int stopBits, final int parity, final int startFlag, final int frameLength) {
        super(portname, baudrate);
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.startFlag = startFlag;
        this.frameLength = frameLength;
        frameBuffer = ByteBuffer.allocate(frameLength);

    }


    /**
     * @return the dataBits
     */
    public int getDataBits() {
        return dataBits;
    }


    /**
     * @param dataBits the dataBits to set
     */
    public void setDataBits(final int dataBits) {
        this.dataBits = dataBits;
    }


    /**
     * @return the stopBits
     */
    public int getStopBits() {
        return stopBits;
    }


    /**
     * @param stopBits the stopBits to set
     */
    public void setStopBits(final int stopBits) {
        this.stopBits = stopBits;
    }


    /**
     * @return the parity
     */
    public int getParity() {
        return parity;
    }


    /**
     * @param parity the parity to set
     */
    public void setParity(final int parity) {
        this.parity = parity;
    }


    /**
     * Gets the start flag.
     * 
     * @return the start flag
     */
    public int getStartFlag() {
        return startFlag;
    }


    /**
     * Sets the start flag.
     * 
     * @param startFlag the start flag
     */
    public void setStartFlag(final int startFlag) {
        this.startFlag = startFlag;
    }


    /**
     * Gets the frame length.
     *
     * @return the frame length
     */
    public int getFrameLength() {
        return frameLength;
    }


    /**
     * Sets the frame length.
     *
     * @param frameLength the frame length
     */
    public void setFrameLength(final int frameLength) {
        this.frameLength = frameLength;
        frameBuffer = ByteBuffer.allocate(frameLength);
    }


    /**
     * Gets the frame buffer used to store the bytes of a received frame.
     *
     * @return the frameBuffer
     */
    public ByteBuffer getFrameBuffer() {
        return frameBuffer;
    }
}