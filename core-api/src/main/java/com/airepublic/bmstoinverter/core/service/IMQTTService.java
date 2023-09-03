package com.airepublic.bmstoinverter.core.service;

public interface IMQTTService extends AutoCloseable {
    void start(String topic, int port);


    boolean isRunning();


    void stop();

}
