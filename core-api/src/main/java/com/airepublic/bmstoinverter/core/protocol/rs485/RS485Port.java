package com.airepublic.bmstoinverter.core.protocol.rs485;

import com.airepublic.bmstoinverter.core.Port;

/**
 * A {@link Port} that is used for RS485 messages. Expecting <code>config.properties</code> or
 * System properties <code>RS485.baudrate</code>, <code>RS485.startFlag</code> to be set.
 */
public abstract class RS485Port extends Port {
    private int dataBits;
    private int stopBits;
    private int parity;
    private FrameDefinition frameDefinition;
    private byte[] startFlag;

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
    public RS485Port(final String portname, final int baudrate, final int dataBits, final int stopBits, final int parity, final byte[] startFlag, final FrameDefinition frameDefinition) {
        super(portname, baudrate);
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.startFlag = startFlag;
        this.frameDefinition = frameDefinition;

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
    public byte[] getStartFlag() {
        return startFlag;
    }


    /**
     * Sets the start flag.
     * 
     * @param startFlag the start flag
     */
    public void setStartFlag(final byte[] startFlag) {
        this.startFlag = startFlag;
    }


    /**
     * Gets the {@link FrameDefinition} for this {@link Port}.
     *
     * @return the {@link FrameDefinition}
     */
    public FrameDefinition getFrameDefinition() {
        return frameDefinition;
    }

}