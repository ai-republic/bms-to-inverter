package com.airepublic.bmstoinverter.core.service;

public interface IMQTTBrokerService extends AutoCloseable {
    void start(String locator);


    boolean isRunning();


    void stop();

}
