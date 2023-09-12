package com.airepublic.bmstoinverter.core.service;

public interface IWebServerService extends AutoCloseable {
    void start();


    boolean isRunning();


    void stop();
}
