package com.airepublic.bmstoinverter.core.protocol.rs485;

import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.core.Port;

import jakarta.annotation.PostConstruct;

/**
 * A {@link Port} that is used for RS485 messages. Expecting <code>config.properties</code> or
 * System properties <code>RS485.baudrate</code>, <code>RS485.startFlag</code> and
 * <code>RS485.frameLength</code> to be set.
 */
@RS485
public abstract class RS485Port extends Port {
    private int baudrate = 9600;
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
     */
    public RS485Port(final String portname) {
        super(portname);
    }


    /**
     * Initializes the baud rate, start flag and frame length from the
     * <code>config.properties</code> or system properties <code>RS485.baudrate</code>,
     * <code>RS485.startFlag</code> and <code>RS485.frameLength</code>.
     */
    @PostConstruct
    public void init() {
        setBaudrate(Integer.parseInt(System.getProperty("RS485.baudrate")));
        setStartFlag(Integer.parseInt(System.getProperty("RS485.startFlag")));
        setFrameLength(Integer.parseInt(System.getProperty("RS485.frameLength")));
    }


    /**
     * Gets the baud rate.
     *
     * @return the baud rate
     */
    public int getBaudrate() {
        return baudrate;
    }


    /**
     * Sets the baud rate.
     *
     * @param baudrate the baud rate
     */
    public void setBaudrate(final int baudrate) {
        this.baudrate = baudrate;
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