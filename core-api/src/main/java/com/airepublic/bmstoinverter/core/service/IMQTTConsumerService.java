package com.airepublic.bmstoinverter.core.service;

import java.util.function.Consumer;

/**
 * Service interface for the MQTT consumer service.
 */
public interface IMQTTConsumerService extends AutoCloseable {
    /**
     * Creates the MQTT consumer connecting to the specified topic on the broker at the locator url.
     *
     * @param locator the locator url
     * @param topic the topic name
     * @param messageHandler the function to handle the incoming message
     * @throws Exception if an error occurs
     */
    IMQTTConsumerService create(String locator, String topic, Consumer<String> messageHandler) throws Exception;


    /**
     * Returns whether the MQTT consumer service is running or not.
     * 
     * @return flag whether the MQTT consumer service is running or not
     */
    boolean isRunning();


    /**
     * Stops the MQTT consumer service.
     */
    void stop();

}
