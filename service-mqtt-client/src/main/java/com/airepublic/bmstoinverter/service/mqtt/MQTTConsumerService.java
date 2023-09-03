package com.airepublic.bmstoinverter.service.mqtt;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.service.IMQTTConsumerService;

public class MQTTConsumerService implements IMQTTConsumerService {
    private final static Logger LOG = LoggerFactory.getLogger(MQTTConsumerService.class);
    private boolean running = false;
    private ClientSession session;
    private ClientConsumer consumer;

    private String topic;

    @Override
    public MQTTConsumerService create(final String locator, final String topic) throws IOException {
        try {
            final ServerLocator serverLocator = ActiveMQClient.createServerLocator(locator);
            final ClientSessionFactory factory = serverLocator.createSessionFactory();
            session = factory.createSession();
            session.start();
            consumer = session.createConsumer(topic);

            running = true;
            return this;
        } catch (final Exception e) {
            LOG.error("Error starting MQTT service!", e);
            try {
                close();
            } catch (final Exception e1) {
            }

            throw new IOException("Could not create MQTT producer client at " + locator + " on topic " + topic, e);
        }
    }


    @Override
    public boolean isRunning() {
        return running;
    }


    @Override
    public ByteBuffer consume() throws IOException {
        try {
            final ClientMessage message = consumer.receive();
            final ByteBuffer content = ByteBuffer.allocate(message.getBodyBufferSize());
            message.getBodyBuffer().getBytes(0, content);
            return content;
        } catch (final Exception e) {
            throw new IOException("Could not send MQTT message on topic " + topic, e);
        }

    }


    @Override
    public void stop() {
        try {
            session.close();
            running = false;
        } catch (final Exception e) {
        }
    }


    @Override
    public void close() throws Exception {
        stop();
    }
}
