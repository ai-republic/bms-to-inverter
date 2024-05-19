/**
 * This software is free to use and to distribute in its unchanged form for private use.
 * Commercial use is prohibited without an explicit license agreement of the copyright holder.
 * Any changes to this software must be made solely in the project repository at https://github.com/ai-republic/bms-to-inverter.
 * The copyright holder is not liable for any damages in whatever form that may occur by using this software.
 *
 * (c) Copyright 2022 and onwards - Torsten Oltmanns
 *
 * @author Torsten Oltmanns - bms-to-inverter''AT''gmail.com
 */
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
     * @param username the username to use to login with
     * @param password the password to use to login with
     * @throws Exception if an error occurs
     */
    IMQTTProducerService connect(String locator, String topic, String username, String password) throws Exception;


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
