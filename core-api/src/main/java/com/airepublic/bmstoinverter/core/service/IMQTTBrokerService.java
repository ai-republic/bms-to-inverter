package com.airepublic.bmstoinverter.core.service;

public interface IMQTTBrokerService extends AutoCloseable {

    void createTopic(String topic, long ringSize);


    void start(String locator);


    boolean isRunning();


    void stop();

}
