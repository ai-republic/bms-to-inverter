package com.airepublic.bmstoinverter.core.service;

import java.io.IOException;

public interface IMQTTProducerService extends AutoCloseable {
    IMQTTProducerService connect(String locator, String topic) throws Exception;


    boolean isRunning();


    void sendMessage(String content) throws IOException;


    void stop();

}
