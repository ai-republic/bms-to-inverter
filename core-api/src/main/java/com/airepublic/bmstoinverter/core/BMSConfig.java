package com.airepublic.bmstoinverter.core;

/**
 * Configuration read from the confg.properties for each {@link BMS}.
 */
public class BMSConfig {
    private int bmsId;
    private String portLocator;
    private long delayAfterNoBytes;
    private BMSDescriptor descriptor;

    /**
     * Constructor.
     *
     * @param bmsId the id of the BMS
     * @param portLocator the port locator
     * @param delayAfterNoBytes the delay after no bytes were received in milliseconds
     * @param descriptor the {@link BMSDescriptor} of the {@link BMS} to use
     */
    public BMSConfig(final int bmsId, final String portLocator, final long delayAfterNoBytes, final BMSDescriptor descriptor) {
        super();
        this.bmsId = bmsId;
        this.portLocator = portLocator;
        this.delayAfterNoBytes = delayAfterNoBytes;
        this.descriptor = descriptor;
    }


    /**
     * Updates this configuration.
     *
     * @param bmsId the id of the BMS
     * @param portLocator the port locator
     * @param delayAfterNoBytes the delay after no bytes were received in milliseconds
     * @param bmsDescriptor the {@link BMSDescriptor} of the {@link BMS} to use
     */
    public void update(final int bmsId, final String portLocator, final long delayAfterNoBytes, final BMSDescriptor bmsDescriptor) {
        this.bmsId = bmsId;
        this.portLocator = portLocator;
        this.delayAfterNoBytes = delayAfterNoBytes;
        descriptor = bmsDescriptor;
    }


    /**
     * Gets the number of the BMS.
     *
     * @return the bmsNo the number of the BMS
     */
    public int getBmsId() {
        return bmsId;
    }


    /**
     * Sets the number of the BMS.
     *
     * @param bmsNo the bmsNo the number of the BMS
     */
    public void setBmsId(final int bmsNo) {
        bmsId = bmsNo;
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
    public BMSDescriptor getDescriptor() {
        return descriptor;
    }
}
