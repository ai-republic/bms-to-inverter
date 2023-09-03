package com.airepublic.bmstoinverter.service.mqtt;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.service.IMQTTProducerService;

public class MQTTProducerService implements IMQTTProducerService {
    private final static Logger LOG = LoggerFactory.getLogger(MQTTProducerService.class);
    private boolean running = false;
    private ClientSession session;
    private String topic;
    private ClientProducer producer;

    @Override
    public MQTTProducerService connect(final String locator, final String topic) throws IOException {
        this.topic = topic;

        try {

            final ServerLocator serverLocator = ActiveMQClient.createServerLocator(locator);
            final ClientSessionFactory factory = serverLocator.createSessionFactory();
            session = factory.createSession();
            session.createQueue(new QueueConfiguration(topic));

            producer = session.createProducer(topic);
            session.start();

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
    public void sendMessage(final ByteBuffer content) throws IOException {
        try {
            content.rewind();
            final ClientMessage message = session.createMessage(true);
            message.getBodyBuffer().writeBytes(content);
            producer.send(message);
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
