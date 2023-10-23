package com.airepublic.bmstoinverter.core.service;

import java.io.IOException;

import jakarta.annotation.Nullable;

/**
 * Service interface for the MQTT consumer service.
 */
public interface IMQTTConsumerService extends AutoCloseable {
    /**
     * Creates the MQTT consumer connecting to the specified topic on the broker at the locator url.
     *
     * @param locator the locator url
     * @param topic the topic name
     * @throws Exception if an error occurs
     */
    IMQTTConsumerService create(String locator, String topic) throws Exception;


    /**
     * Returns whether the MQTT consumer service is running or not.
     * 
     * @return flag whether the MQTT consumer service is running or not
     */
    boolean isRunning();


    /**
     * Try to consume a message from the MQTT broker within the specified timeout.
     *
     * @param timeoutMs the timeout in ms
     * @return the message or null
     * @throws IOException if an exception occurs
     */
    @Nullable
    String consume(long timeoutMs) throws IOException;


    /**
     * Stops the MQTT consumer service.
     */
    void stop();

}
