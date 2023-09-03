package com.airepublic.bmstoinverter.core.service;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface IMQTTConsumerService extends AutoCloseable {
    IMQTTConsumerService create(String locator, String topic) throws Exception;


    boolean isRunning();


    ByteBuffer consume() throws IOException;


    void stop();

}
