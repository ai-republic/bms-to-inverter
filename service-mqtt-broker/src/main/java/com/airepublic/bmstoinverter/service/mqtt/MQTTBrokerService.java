package com.airepublic.bmstoinverter.service.mqtt;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
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

@ApplicationScoped
public class MQTTBrokerService implements IMQTTBrokerService {
    private final static Logger LOG = LoggerFactory.getLogger(MQTTBrokerService.class);
    private EmbeddedActiveMQ embedded = null;
    private boolean running = false;

    @Override
    public void start(final String locator) {
        try {
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

        final MQTTBrokerService mqtt = new MQTTBrokerService();
        try {

            mqtt.start("tcp://127.0.0.1:61616");

            final ServerLocator serverLocator = ActiveMQClient.createServerLocator("tcp://127.0.0.1:61616");
            final ClientSessionFactory factory = serverLocator.createSessionFactory();
            final ClientSession session = factory.createSession();

            session.createQueue(new QueueConfiguration("example"));

            final ClientProducer producer = session.createProducer("example");
            final ClientMessage message = session.createMessage(true);
            message.getBodyBuffer().writeString("Hello");
            producer.send(message);

            session.start();
            final ClientConsumer consumer = session.createConsumer("example");
            final ClientMessage msgReceived = consumer.receive();
            System.out.println("message = " + msgReceived.getBodyBuffer().readString());
            session.close();
        } catch (final ActiveMQException e) {
            e.printStackTrace();
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            try {
                mqtt.close();
            } catch (final Exception e) {
            }
        }

    }
}
