package com.airepublic.bmstoinverter.service.mqtt;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
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

            config.addAcceptorConfiguration("tcp", locator);

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
    public void createTopic(final String topic, final long ringSize) {
        try (final ServerLocator serverLocator = ActiveMQClient.createServerLocator(locator)) {
            final ClientSessionFactory factory = serverLocator.createSessionFactory();
            final ClientSession session = factory.createSession();

            session.createQueue(new QueueConfiguration(topic).setRingSize(ringSize));
            session.close();
        } catch (final Exception e) {
            LOG.error("Error creating topic {}", topic);
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
        }
    }


    @Override
    public void close() throws Exception {
        stop();
        embedded = null;
    }


    public static void main(final String[] args) {
        final String locator = "tcp://127.0.0.1:61616";
        final String topic = "energystorage";
        final MQTTBrokerService mqtt = new MQTTBrokerService();

        try {

            mqtt.start(locator);

            final ServerLocator serverLocator = ActiveMQClient.createServerLocator(locator);
            final ClientSessionFactory factory = serverLocator.createSessionFactory();
            final ClientSession session = factory.createSession();

            session.createQueue(new QueueConfiguration(topic).setRingSize(1L));

            final ClientProducer producer = session.createProducer(topic);
            final ClientMessage message = session.createMessage(true);
            message.getBodyBuffer().writeString("Hello");
            producer.send(message);

            session.start();
            // final ClientConsumer consumer = session.createConsumer("example");
            // final ClientMessage msgReceived = consumer.receive();
            // System.out.println("message = " + msgReceived.getBodyBuffer().readString());
            // session.close();
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
