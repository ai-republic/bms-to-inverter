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
