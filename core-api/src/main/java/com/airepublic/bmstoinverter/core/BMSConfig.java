package com.airepublic.bmstoinverter.core;

/**
 * Configuration read from the confg.properties for each {@link BMS}.
 */
public class BMSConfig {
    private final int bmsNo;
    private final String portLocator;
    private final int pollInterval;
    private final long delayAfterNoBytes;
    private final BMSDescriptor bmsDescriptor;

    /**
     * Constructor.
     *
     * @param bmsNo the number of the BMS
     * @param portLocator the port locator
     * @param pollInterval the polling interval in seconds
     * @param delayAfterNoBytes the delay after no bytes were received in milliseconds
     */
    public BMSConfig(final int bmsNo, final String portLocator, final int pollInterval, final long delayAfterNoBytes, final BMSDescriptor bmsDescriptor) {
        super();
        this.bmsNo = bmsNo;
        this.portLocator = portLocator;
        this.pollInterval = pollInterval;
        this.delayAfterNoBytes = delayAfterNoBytes;
        this.bmsDescriptor = bmsDescriptor;
    }


    /**
     * Gets the number of the BMS.
     *
     * @return the bmsNo the number of the BMS
     */
    public int getBmsNo() {
        return bmsNo;
    }


    /**
     * Gets the port locator like /dev/ttyS0, can0, com3, etc.
     *
     * @return the portLocator the port locator
     */
    public String getPortLocator() {
        return portLocator;
    }


    /**
     * Gets the polling interval in seconds.
     *
     * @return the polling interval in seconds
     */
    public int getPollInterval() {
        return pollInterval;
    }


    /**
     * Gets the delay after no bytes were received in milliseconds.
     *
     * @return the delay after no bytes were received in milliseconds
     */
    public long getDelayAfterNoBytes() {
        return delayAfterNoBytes;
    }


    /**
     * Gets the {@link BMSDescriptor} of the {@link BMS}.
     *
     * @return the {@link BMSDescriptor} of the {@link BMS}
     */
    public BMSDescriptor getBMSDescriptor() {
        return bmsDescriptor;
    }
}
