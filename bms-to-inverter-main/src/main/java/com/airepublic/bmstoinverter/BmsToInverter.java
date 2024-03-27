package com.airepublic.bmstoinverter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.InverterQualifier;
import com.airepublic.bmstoinverter.core.PortAllocator;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.service.IMQTTBrokerService;
import com.airepublic.bmstoinverter.core.service.IMQTTProducerService;
import com.airepublic.bmstoinverter.core.util.Util;
import com.airepublic.email.api.Email;
import com.airepublic.email.api.EmailAccount;
import com.airepublic.email.api.EmailException;
import com.airepublic.email.api.IEmailService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.inject.Inject;

/**
 * The main class to initiate communication between the configured BMS and the inverter. The
 * {@link BMS} values are read and stored in the {@link EnergyStorage}. Once read these values are
 * send to the (optional) MQTT Broker. Alarms and warnings will be analysed and (optionally) sent by
 * email if some occurred. The the data is sent to the {@link Inverter}.
 */
@ApplicationScoped
public class BmsToInverter implements AutoCloseable {
    private final static Logger LOG = LoggerFactory.getLogger(BmsToInverter.class);
    @Inject
    private EnergyStorage energyStorage;
    @Inject
    private List<BMS> bmsList;
    @Inject
    @InverterQualifier
    private Inverter inverter;
    private Thread bmsRunner;
    private Thread inverterRunner;
    private boolean running = true;
    private IMQTTBrokerService mqttBroker;
    private IMQTTProducerService mqttProducer;
    private IEmailService emailService;
    private EmailAccount account;
    private final List<String> emailRecipients = new ArrayList<>();
    private List<String> lastAlarms = new ArrayList<>();

    /**
     * The main method to start the application.
     *
     * @param args none
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {
        // update all non-specified system parameters from "config.properties"
        Util.updateSystemProperties(Path.of(System.getProperty("configFile", "config.properties")));

        final SeContainerInitializer initializer = SeContainerInitializer.newInstance();
        final SeContainer container = initializer.initialize();
        final BmsToInverter app = container.select(BmsToInverter.class).get();
        app.start();
    }


    /**
     * Constructor.
     */
    public BmsToInverter() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> close()));
        initializeServices();
    }


    /**
     * Initialize all optional configured services.
     */
    protected void initializeServices() {
        // check for MQTT broker service module
        if (System.getProperty("mqtt.broker.enabled") != null && System.getProperty("mqtt.broker.enabled").equals("true")) {
            mqttBroker = ServiceLoader.load(IMQTTBrokerService.class).findFirst().orElse(null);

            if (mqttBroker == null) {
                LOG.error("Error in project configuration - no MQTT Broker service implementation found!");
            }

            final String locator = System.getProperty("mqtt.broker.locator");
            final String topic = System.getProperty("mqtt.broker.topic");

            try {
                mqttBroker.start(locator);
                mqttBroker.createTopic(topic, 1L);
            } catch (final Exception e) {
                LOG.error("Could not start MQTT broker at {} on topic {}", locator, topic, e);
            }
        }

        // check for MQTT producer service module
        if (System.getProperty("mqtt.producer.enabled") != null && System.getProperty("mqtt.producer.enabled").equals("true")) {
            mqttProducer = ServiceLoader.load(IMQTTProducerService.class).findFirst().orElse(null);

            if (mqttProducer == null) {
                LOG.error("Error in project configuration - no MQTT producer service implementation found!");
            }

            final String locator = System.getProperty("mqtt.producer.locator");
            final String topic = System.getProperty("mqtt.producer.topic");

            try {
                mqttProducer.connect(locator, topic);
            } catch (final Exception e) {
                LOG.error("Could not connect MQTT producer client at {} on topic {}", locator, topic, e);
            }
        }

        // check for EmailService service module
        if (System.getProperty("mail.service.enabled") != null && System.getProperty("mail.service.enabled").equals("true")) {
            emailService = ServiceLoader.load(IEmailService.class).findFirst().orElse(null);

            if (emailService == null) {
                LOG.error("Error in project configuration - no email provider was found be email service is activated!");
            }

            account = new EmailAccount(System.getProperties());
            final String commaSeparatedList = System.getProperty("mail.out.recipients");

            if (commaSeparatedList == null) {
                throw new IllegalArgumentException("EmailService is activated but the property 'mail.out.recipients' could not be found!");
            }

            final StringTokenizer tokenizer = new StringTokenizer(commaSeparatedList, ",");

            while (tokenizer.hasMoreTokens()) {
                emailRecipients.add(tokenizer.nextToken());
            }
        }
    }


    /**
     * Starts the reading and processing of the BMS data in parallel execution reading and writing
     * synchronized to the {@link EnergyStorage}.
     */
    public void start() {
        try {

            LOG.info("Starting BMS receiver...");
            final int pollInterval = Integer.parseInt(System.getProperty("bms.pollInterval", "1"));

            final Thread bmsRunner = new Thread(() -> {
                do {
                    for (final BMS bms : bmsList) {
                        try {
                            LOG.info("Reading BMS #" + bms.getBmsId() + " " + bms.getName() + " on " + bms.getPortLocator() + "...");
                            bms.process(() -> receivedData());
                        } catch (final Throwable e) {
                        }
                    }

                    try {
                        Thread.sleep(pollInterval * 1000);
                    } catch (final InterruptedException e) {
                    }
                } while (running);
            });
            bmsRunner.start();

            // wait for the first data to be received
            synchronized (this) {
                this.wait();
            }

            // send data to inverter
            if (inverter != null) {

                LOG.info("Starting inverter sender...");
                inverterRunner = new Thread(() -> {
                    do {
                        try {
                            LOG.info("Sending to inverter " + inverter.getName() + " on " + inverter.getPortLocator() + "...");
                            inverter.process(() -> sentData());
                            Thread.sleep(inverter.getSendInterval() * 1000);
                        } catch (final Throwable e) {
                        }
                    } while (running);
                });
                inverterRunner.start();
            }
        } catch (

        final Throwable e) {
            LOG.error("Error occured during processing!", e);
        }
    }


    /**
     * Called after the BMS received data.
     */
    private void receivedData() {
        try {
            synchronized (this) {
                notify();
            }

            LOG.info(createBatteryOverview());

            if (mqttProducer != null) {
                // send energystorage data to MQTT broker
                try {
                    mqttProducer.sendMessage(energyStorage.toJson());
                } catch (final Throwable e) {
                    LOG.error("Failed to send MQTT message!", e);

                    // try to reconnect
                    try {
                        mqttProducer.close();
                    } catch (final Exception e1) {
                    }

                    final String locator = System.getProperty("mqtt.producer.locator");
                    final String topic = System.getProperty("mqtt.producer.topic");

                    mqttProducer.connect(locator, topic);
                    mqttProducer.sendMessage(energyStorage.toJson());
                }
            }

            analyseBMSFaults();
        } catch (final Throwable e) {
            LOG.error("Error after data received!", e);
        }
    }


    /**
     * Called after the data was sent to the inverter.
     */
    private void sentData() {
    }


    /**
     * Analyzes and aggregates the warnings and alarms and sends them to the configured mail
     * account(s).
     */
    private void analyseBMSFaults() {
        final List<String> currentAlarms = new ArrayList<>();
        final StringBuffer alarmContent = new StringBuffer();
        Email email = null;

        for (int index = 0; index < energyStorage.getBatteryPacks().size(); index++) {
            for (final Map.Entry<Alarm, AlarmLevel> entry : energyStorage.getBatteryPack(index).getAlarms(AlarmLevel.WARNING, AlarmLevel.ALARM).entrySet()) {
                currentAlarms.add("BMS #" + (index + 1) + ":" + entry.getValue().name() + " -> " + entry.getKey());
                alarmContent.append("\tBMS #" + (index + 1) + ":\t" + entry.getValue().name() + " -> " + entry.getKey().name() + "\r\n");
            }
        }

        if (!currentAlarms.isEmpty()) {
            LOG.info("BMS alarms:\n" + currentAlarms.toString());
        } else {
            LOG.info("BMS alarms: \n\tNONE");
        }

        // check if alarms have changed
        if (emailService != null) {
            if (!currentAlarms.isEmpty() && !currentAlarms.equals(lastAlarms)) {
                final StringBuffer content = new StringBuffer("This is a generated email - do not reply!\n\n Your BMS has reported the following alarms:\n");
                content.append(alarmContent);

                email = new Email(account.getOutgoingMailServerEmail(), emailRecipients, "BMS Alarms occured", content.toString(), false);
                // otherwise check if alarms have resolved
            } else if (currentAlarms.isEmpty() && !lastAlarms.isEmpty()) {
                final String content = "This is a generated email - do not reply!\n\n Your BMS is back to working normally.";
                email = new Email(account.getOutgoingMailServerEmail(), emailRecipients, "BMS Alarms resolved", content, false);
            }

            if (email != null) {
                try {
                    emailService.sendEmail(email, account);
                    lastAlarms = currentAlarms;
                } catch (final EmailException e) {
                    LOG.error("Email could not be sent!", e);
                }
            }
        }
    }


    @Override
    public void close() {
        try {
            running = false;
            bmsRunner.interrupt();
            inverterRunner.interrupt();
            LOG.info("Shutting down BMS and inverter threads...OK");
        } catch (final Throwable e) {
            LOG.info("Shutting down BMS and inverter threads...FAILED");
        }

        if (mqttProducer != null) {
            try {
                mqttProducer.close();
                LOG.info("Shutting down MQTT producer threads...OK");
            } catch (final Throwable e) {
                LOG.info("Shutting down MQTT producer threads...FAILED");
            }
        }

        if (mqttBroker != null) {
            try {
                mqttBroker.close();
                LOG.info("Shutting down MQTT broker threads...OK");
            } catch (final Throwable e) {
                LOG.info("Shutting down MQTT broker threads...FAILED");
            }
        }

        try {
            PortAllocator.close();
            LOG.info("Shutting down ports...OK");
        } catch (final Throwable e) {
            LOG.info("Shutting down ports...FAILED");
        }
    }


    private String createBatteryOverview() {
        final StringBuffer log = new StringBuffer();
        // header
        log.append("\nBMS\tSOC\t  V  \t  A  \t CellMinV \t CellMaxV\tCellDiff\n");

        for (int index = 0; index < energyStorage.getBatteryPacks().size(); index++) {
            final BatteryPack b = energyStorage.getBatteryPack(index);

            log.append("#" + (index + 1)
                    + "\t " + b.packSOC / 10f
                    + "\t" + b.packVoltage / 10f
                    + "\t" + b.packCurrent / 10f
                    + "\t" + b.minCellmV / 1000f + "(#" + b.minCellVNum + ") "
                    + "\t" + b.maxCellmV / 1000f + "(#" + b.maxCellVNum + ")"
                    + "\t" + (b.maxCellmV - b.minCellmV) / 1000f + " \n");
        }

        return log.toString();
    }

}
