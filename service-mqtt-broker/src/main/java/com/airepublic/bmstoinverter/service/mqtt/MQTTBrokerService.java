/**
 * This software is free to use and to distribute in its unchanged form for private use.
 * Commercial use is prohibited without an explicit license agreement of the copyright holder.
 * Any changes to this software must be made solely in the project repository at https://github.com/ai-republic/bms-to-inverter.
 * The copyright holder is not liable for any damages in whatever form that may occur by using this software.
 *
 * (c) Copyright 2022 and onwards - Torsten Oltmanns
 *
 * @author Torsten Oltmanns - bms-to-inverter''AT''gmail.com
 */
package com.airepublic.bmstoinverter.service.mqtt;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.service.IMQTTBrokerService;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The implementation of the {@link IMQTTBrokerService} using the ActiveMQ Artemis implementation.
 */
@ApplicationScoped
public class MQTTBrokerService implements IMQTTBrokerService {
    private final static Logger LOG = LoggerFactory.getLogger(MQTTBrokerService.class);
    private EmbeddedActiveMQ embedded = null;
    private boolean running = false;
    private String locator;

    @Override
    public void start(final String locator) {
        if (isRunning()) {
            return;
        }

        try {
            this.locator = locator;

            final Configuration config = new ConfigurationImpl();
            config.setSecurityEnabled(false);
            config.setPersistenceEnabled(false);
            config.addAcceptorConfiguration("tcp", locator + "?protocols=CORE,MQTT");

            embedded = new EmbeddedActiveMQ();
            embedded.setConfiguration(config);
            embedded.start();

            running = true;
        } catch (final Exception e) {
            LOG.error("Error starting MQTT service on {}!", locator, e);
            try {
                close();
            } catch (final Exception e1) {
            }
        }
    }


    @Override
    public void createAddress(final String address, final boolean isMulticast) {
        try (final ServerLocator serverLocator = ActiveMQClient.createServerLocator(locator)) {
            final ClientSessionFactory factory = serverLocator.createSessionFactory();
            final ClientSession session = factory.createSession();

            session.createAddress(new SimpleString(address), isMulticast ? RoutingType.MULTICAST : RoutingType.ANYCAST, true);
            session.close();
        } catch (final Exception e) {
            LOG.error("Error creating topic {}", address);
        }
    }


    @Override
    public boolean isRunning() {
        return running;
    }


    @Override
    public void stop() {
        try {
            if (embedded != null) {
                embedded.stop();
            }
            running = false;
        } catch (final Exception e) {
            throw new RuntimeException("Failed to stop MQTT broker!", e);
        }
    }


    @Override
    public void close() throws Exception {
        try {
            stop();
            LOG.info("Shutting down MQTT broker on '{}'...OK", locator);
        } catch (final Exception e) {
            LOG.error("Shutting down MQTT broker on '{}'...FAILED", locator, e);
        }
        embedded = null;
    }


    /**
     * Main method to test the broker.
     *
     * @param args none
     */
    public static void main(final String[] args) {
        final String locator = "tcp://localhost:61616";
        final String address = "energystorage";
        final String queue1 = "1";
        final String queue2 = "2";
        final MQTTBrokerService mqtt = new MQTTBrokerService();

        try {

            mqtt.start(locator);

            mqtt.createAddress(address, true);

            final ServerLocator serverLocator = ActiveMQClient.createServerLocator(locator);
            final ClientSessionFactory factory = serverLocator.createSessionFactory();
            final ClientSession session = factory.createSession();

            session.createQueue(new QueueConfiguration(address + "::" + queue1).setRingSize(1L).setRoutingType(RoutingType.MULTICAST));
            session.createQueue(new QueueConfiguration(address + "::" + queue2).setRingSize(1L).setRoutingType(RoutingType.MULTICAST));

            final ClientConsumer consumer = session.createConsumer(address + "::" + queue1);
            consumer.setMessageHandler(msg -> {
                System.out.println("----------> Consumer#1 Received message: " + msg.getBodyBuffer().readString());
            });

            final ServerLocator serverLocator2 = ActiveMQClient.createServerLocator(locator);
            final ClientSessionFactory factory2 = serverLocator2.createSessionFactory();
            final ClientSession session2 = factory2.createSession();
            final ClientConsumer consumer2 = session2.createConsumer(address + "::" + queue2);
            consumer2.setMessageHandler(msg -> {
                System.out.println("----------> Consumer#2 Received message: " + msg.getBodyBuffer().readString());
            });

            final ServerLocator serverLocator3 = ActiveMQClient.createServerLocator(locator);
            final ClientSessionFactory factory3 = serverLocator3.createSessionFactory();
            final ClientSession session3 = factory3.createSession();
            final ClientProducer producer = session3.createProducer(address);

            session.start();
            session2.start();
            session3.start();

            final ServerLocator serverLocator4 = ActiveMQClient.createServerLocator(locator);
            final ClientSessionFactory factory4 = serverLocator4.createSessionFactory();
            final ClientSession session4 = factory4.createSession();

            ClientMessage message = session4.createMessage(true);
            message.setRoutingType(RoutingType.MULTICAST);
            message.writeBodyBufferString("Hello");
            producer.send(message);

            message = session4.createMessage(true);
            message.setRoutingType(RoutingType.MULTICAST);
            message.writeBodyBufferString("Hello2");
            producer.send(message);

            message = session4.createMessage(true);
            message.setRoutingType(RoutingType.MULTICAST);
            message.writeBodyBufferString("Hello3");
            producer.send(message);

        } catch (final ActiveMQException e) {
            e.printStackTrace();
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
