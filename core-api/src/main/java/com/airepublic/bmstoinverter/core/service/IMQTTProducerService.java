package com.airepublic.bmstoinverter.core.service;

import java.io.IOException;

/**
 * Service interface for the MQTT producer service.
 */
public interface IMQTTProducerService extends AutoCloseable {
    /**
     * Creates the MQTT producer connecting to the specified topic on the broker at the locator url.
     *
     * @param locator the locator url
     * @param topic the topic name
     * @throws Exception if an error occurs
     */
    IMQTTProducerService connect(String locator, String topic) throws Exception;


    /**
     * Returns whether the MQTT producer service is running or not.
     * 
     * @return flag whether the MQTT producer service is running or not
     */
    boolean isRunning();


    /**
     * Sends the specified message content to the connected topic.
     * 
     * @param content the message content
     * @throws IOException if an error occurs
     */
    void sendMessage(String content) throws IOException;


    /**
     * Stops the MQTT producer service.
     */
    void stop();

}
