package com.airepublic.bmstoinverter.core.service;

import java.io.IOException;

public interface IMQTTConsumerService extends AutoCloseable {
    IMQTTConsumerService create(String locator, String topic) throws Exception;


    boolean isRunning();


    String consume(long timeoutMs) throws IOException;


    void stop();

}
