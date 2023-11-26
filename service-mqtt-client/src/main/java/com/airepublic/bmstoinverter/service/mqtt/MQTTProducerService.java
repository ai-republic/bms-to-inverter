package com.airepublic.bmstoinverter.service.mqtt;

import java.io.IOException;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.service.IMQTTProducerService;

/**
 * The implementation of the {@link IMQTTProducerService} using the ActiveMQ Artemis implementation.
 */
public class MQTTProducerService implements IMQTTProducerService {
    private final static Logger LOG = LoggerFactory.getLogger(MQTTProducerService.class);
    private boolean running = false;
    private ClientSession session;
    private String topic;
    private ClientProducer producer;
    private String locator;

    @Override
    public MQTTProducerService connect(final String locator, final String topic) throws IOException {
        this.locator = locator;
        this.topic = topic;

        try {

            final ServerLocator serverLocator = ActiveMQClient.createServerLocator(locator);
            final ClientSessionFactory factory = serverLocator.createSessionFactory();
            session = factory.createSession();
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
    public void sendMessage(final String content) throws IOException {
        try {
            final ClientMessage message = session.createMessage(true);
            message.getBodyBuffer().writeString(content);
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
            throw new RuntimeException("Failed to stop MQTT producer!", e);
        }
    }


    @Override
    public void close() throws Exception {
        try {
            stop();
            LOG.info("Shutting down MQTT producer on '{}'...OK", locator);
        } catch (final Exception e) {
            LOG.error("Shutting down MQTT producer on '{}'...FAILED", locator, e);
        }
    }


    /**
     * Main method to test the producer.
     *
     * @param args none
     */
    public static void main(final String[] args) {

        try {

            final ServerLocator serverLocator = ActiveMQClient.createServerLocator("tcp://127.0.0.1:61616");
            final ClientSessionFactory factory = serverLocator.createSessionFactory();
            final ClientSession session = factory.createSession();

            final ClientProducer producer = session.createProducer("energystorage");
            final ClientMessage message = session.createMessage(true);
            message.getBodyBuffer().writeString("Hello from producer");
            producer.send(message);

            session.start();
            // final ClientConsumer consumer = session.createConsumer("example");
            // final ClientMessage msgReceived = consumer.receive();
            // System.out.println("message = " + msgReceived.getBodyBuffer().readString());
            // session.close();
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // mqtt.close();
            } catch (final Exception e) {
            }
        }

    }

}
