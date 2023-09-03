package com.airepublic.bmstoinverter.core.service;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface IMQTTProducerService extends AutoCloseable {
    IMQTTProducerService connect(String locator, String topic) throws Exception;


    boolean isRunning();


    void sendMessage(ByteBuffer content) throws IOException;


    void stop();

}
