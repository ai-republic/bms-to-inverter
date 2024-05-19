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
    IMQTTConsumerService createQueueOnAddress(String locator, String topic, Consumer<String> messageHandler) throws Exception;


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
