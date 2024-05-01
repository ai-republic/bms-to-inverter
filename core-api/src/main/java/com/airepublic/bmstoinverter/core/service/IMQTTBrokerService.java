package com.airepublic.bmstoinverter.core.service;

/**
 * Service interface for the MQTT broker service.
 */
public interface IMQTTBrokerService extends AutoCloseable {

    /**
     * Creates the specified address where queues can be added.
     *
     * @param address the topic name
     * @param isMulticast true if the topic is a multi-cast topic (each message delivered to all
     *        consumers)
     */
    void createAddress(String address, boolean isMulticast);


    /**
     * Starts the MQTT broker on the specified locator url.
     *
     * @param locator the locator url
     */
    void start(String locator);


    /**
     * Returns whether the MQTT broker is running or not.
     * 
     * @return flag whether the MQTT broker is running or not
     */
    boolean isRunning();


    /**
     * Stops the MQTT broker.
     */
    void stop();

}
