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
package com.airepublic.bmstoinverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import com.airepublic.bmstoinverter.core.service.IWebServerService;
import com.airepublic.bmstoinverter.core.util.SystemProperties;
import com.airepublic.email.api.Email;
import com.airepublic.email.api.EmailAccount;
import com.airepublic.email.api.EmailException;
import com.airepublic.email.api.IEmailService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;

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
    private Thread stopChecker;
    private boolean running = true;
    private IMQTTBrokerService mqttBroker;
    private IMQTTProducerService mqttProducer;
    private IMQTTProducerService mqttExternalProducer;
    private IEmailService emailService;
    private IWebServerService webServerService;
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
        SystemProperties.updateSystemProperties(Paths.get(System.getProperty("configFile", "config.properties")));

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
    }


    /**
     * Initialize all optional configured services.
     */
    @PostConstruct
    protected void initializeServices() {
        // check for MQTT broker service module
        if (System.getProperty("mqtt.broker.enabled") != null && System.getProperty("mqtt.broker.enabled").equals("true")) {
            initializeMQTTBroker();
            initializeInternalMQTTProducer();
        }

        // check for MQTT external producer service module
        if (System.getProperty("mqtt.producer.enabled") != null && System.getProperty("mqtt.producer.enabled").equals("true")) {
            initializeExternalMQTTProducer();
        }

        // check for EmailService service module
        if (System.getProperty("mail.service.enabled") != null && System.getProperty("mail.service.enabled").equals("true")) {
            initializeEmailService();
        }

        // check for Webserver service module
        if (System.getProperty("webserver.service.enabled") != null && System.getProperty("webserver.service.enabled").equals("true")) {
            initializeWebserverService();
        }
    }


    /**
     * Initialize the MQTT broker.
     */
    protected void initializeMQTTBroker() {
        try {
            mqttBroker = ServiceLoader.load(IMQTTBrokerService.class).iterator().next();
        } catch (final Exception e) {
        }

        if (mqttBroker == null) {
            LOG.error("Error in project configuration - no MQTT Broker service implementation found!");
        }

        final String locator = System.getProperty("mqtt.broker.locator");
        final String address = System.getProperty("mqtt.broker.topic");

        try {
            mqttBroker.start(locator);
            mqttBroker.createAddress(address, true);

            try {
                mqttProducer = ServiceLoader.load(IMQTTProducerService.class).iterator().next();
            } catch (final Exception e) {
            }

            if (mqttProducer == null) {
                LOG.error("Error in project configuration - no MQTT producer service implementation found!");
            }

            try {
                mqttProducer.connect(locator, address, null, null);
            } catch (final Exception e) {
                LOG.error("Could not connect MQTT producer client at {} on topic {}", locator, address, e);
            }

        } catch (final Exception e) {
            LOG.error("Could not start MQTT broker at {} on topic {}", locator, address, e);
        }
    }


    /**
     * Initialize the external MQTT producer.
     */
    protected void initializeInternalMQTTProducer() {
        try {
            mqttProducer = ServiceLoader.load(IMQTTProducerService.class).iterator().next();
        } catch (final Exception e) {
        }

        if (mqttProducer == null) {
            LOG.error("Error in project configuration - no MQTT internal producer service implementation found!");
            return;
        }

        final String locator = System.getProperty("mqtt.broker.locator");
        final String address = System.getProperty("mqtt.broker.topic");

        try {
            mqttProducer.connect(locator, address, null, null);
        } catch (final Exception e) {
            LOG.error("Could not connect MQTT internal producer client at {} on topic {}", locator, address, e);
        }
    }


    /**
     * Initialize the external MQTT producer.
     */
    protected void initializeExternalMQTTProducer() {
        try {
            mqttExternalProducer = ServiceLoader.load(IMQTTProducerService.class).iterator().next();
        } catch (final Exception e) {
        }

        if (mqttExternalProducer == null) {
            LOG.error("Error in project configuration - no MQTT external producer service implementation found!");
            return;
        }

        final String locator = System.getProperty("mqtt.producer.locator");
        final String address = System.getProperty("mqtt.producer.topic");
        final String username = System.getProperty("mqtt.producer.username");
        final String password = System.getProperty("mqtt.producer.password");

        try {
            mqttExternalProducer.connect(locator, address, username, password);
        } catch (final Exception e) {
            LOG.error("Could not connect MQTT external producer client at {} on topic {}", locator, address, e);
        }
    }


    /**
     * Initialize the email service.
     */
    protected void initializeEmailService() {
        try {
            emailService = ServiceLoader.load(IEmailService.class).iterator().next();
        } catch (final Exception e) {
        }

        if (emailService == null) {
            LOG.error("Error in project configuration - no email provider was found but email service is activated!");
            return;
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


    /**
     * Initialize webserver service.
     */
    protected void initializeWebserverService() {
        try {
            webServerService = ServiceLoader.load(IWebServerService.class).iterator().next();
        } catch (final Exception e) {
        }

        if (webServerService == null) {
            LOG.error("Error in project configuration - no webserver was found but webserver service is activated!");
            return;
        }

        try {
            webServerService.start(Integer.parseInt(System.getProperty("webserver.http.port", "8080")), Integer.parseInt(System.getProperty("webserver.https.port", "8443")), energyStorage);
        } catch (final Exception e) {
            LOG.error("Could not start webserver service!", e);
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

            Files.deleteIfExists(Paths.get("./stop"));

            stopChecker = new Thread(() -> {
                do {
                    if (Files.exists(Paths.get("./stop"))) {
                        running = false;
                        LOG.warn("Found stop file");
                        close();
                        Runtime.getRuntime().halt(0);
                    }

                    try {
                        Thread.sleep(pollInterval * 1000);
                    } catch (final InterruptedException e) {
                    }
                } while (running);
            });
            stopChecker.start();

            bmsRunner = new Thread(() -> {
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
                // send energystorage data to internal MQTT broker
                try {
                    mqttProducer.sendMessage(energyStorage.toJson());
                } catch (final Throwable e) {
                    LOG.error("Failed to send MQTT message!", e);

                    // try to reconnect and resend message
                    try {
                        mqttProducer.close();
                        initializeInternalMQTTProducer();
                        mqttProducer.sendMessage(energyStorage.toJson());
                    } catch (final Exception e1) {
                    }
                }
            }

            if (mqttExternalProducer != null) {
                // send energystorage data to external MQTT broker
                try {
                    mqttExternalProducer.sendMessage(energyStorage.toJson());
                } catch (final Throwable e) {
                    LOG.error("Failed to send MQTT message!", e);

                    // try to reconnect and resent message
                    try {
                        mqttExternalProducer.close();
                        initializeExternalMQTTProducer();
                        mqttExternalProducer.sendMessage(energyStorage.toJson());
                    } catch (final Exception e1) {
                    }

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
            LOG.info("BMS alarms:\n" + alarmContent.toString());
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
        LOG.warn("Starting shutdown.");
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

        if (webServerService != null) {
            try {
                webServerService.stop();
                LOG.info("Shutting down webserver threads...OK");
            } catch (final Exception e) {
                LOG.info("Shutting down webserver threads...FAILED");
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
