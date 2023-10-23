package com.airepublic.bmstoinverter.core.service;

/**
 * Service interface for the MQTT broker service.
 */
public interface IMQTTBrokerService extends AutoCloseable {

    /**
     * Creates the specified topic with the specified amount of message held in the ring.
     *
     * @param topic the topic name
     * @param ringSize the amount of message kept in the ring
     */
    void createTopic(String topic, long ringSize);


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
