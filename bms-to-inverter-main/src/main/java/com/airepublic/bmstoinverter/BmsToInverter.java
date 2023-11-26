package com.airepublic.bmstoinverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Bms;
import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.bms.data.Alarms;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.service.IMQTTBrokerService;
import com.airepublic.bmstoinverter.core.service.IMQTTProducerService;
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
 * {@link Bms} values are read and stored in the {@link EnergyStorage}. Once read these values are
 * send to the (optional) MQTT Broker. Alarms and warnings will be analysed and (optionally) sent by
 * email if some occurred. The the data is sent to the {@link Inverter}.
 */
@ApplicationScoped
public class BmsToInverter implements AutoCloseable {
    private final static Logger LOG = LoggerFactory.getLogger(BmsToInverter.class);
    @Inject
    private EnergyStorage energyStorage;
    @Inject
    private Bms bms;
    @Inject
    private Inverter inverter;
    private final IMQTTBrokerService mqttBroker = ServiceLoader.load(IMQTTBrokerService.class).findFirst().orElse(null);
    private final IMQTTProducerService mqttProducer = ServiceLoader.load(IMQTTProducerService.class).findFirst().orElse(null);
    private final IEmailService emailService = ServiceLoader.load(IEmailService.class).findFirst().orElse(null);
    private EmailAccount account;
    private final List<String> emailRecipients = new ArrayList<>();
    private List<String> lastAlarms = new ArrayList<>();
    final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    /**
     * The main method to start the application.
     *
     * @param args none
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {
        // update all non-specified system parameters from "config.properties"
        updateSystemProperties();

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

        // check for MQTT broker service module
        if (mqttBroker != null) {
            final String locator = System.getProperty("mqtt.locator");
            final String topic = System.getProperty("mqtt.topic");

            mqttBroker.start(locator);
            mqttBroker.createTopic(topic, 1L);
        }

        // check for MQTT producer service module
        if (mqttProducer != null) {
            final String locator = System.getProperty("mqtt.locator");
            final String topic = System.getProperty("mqtt.topic");

            try {
                mqttProducer.connect(locator, topic);
            } catch (final Exception e) {
                LOG.error("Could not connect MQTT producer client at {} on topic {}", locator, topic, e);
            }
        }

        // check for EmailService service module
        if (emailService != null) {
            account = new EmailAccount(System.getProperties());
            final String commaSeparatedList = System.getProperty("mail.recipients");

            if (commaSeparatedList == null) {
                throw new IllegalArgumentException("EmailService is activated but the property 'mail.recipients' could not be found!");
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
        final int bmsPollInterval = Integer.parseInt(System.getProperty("bms.pollInterval", "0"));
        final int inverterSendInterval = Integer.parseInt(System.getProperty("inverter.sendInterval", "0"));

        try {

            LOG.info("Starting BMS receiver...");
            // receive BMS data
            executorService.scheduleWithFixedDelay(() -> bms.process(() -> receivedData()), 3, bmsPollInterval, TimeUnit.SECONDS);

            // send data to inverter
            if (inverter != null) {

                LOG.info("Starting inverter sender...");
                executorService.scheduleWithFixedDelay(() -> inverter.process(() -> sentData()), 3, inverterSendInterval, TimeUnit.SECONDS);
            }
        } catch (final Throwable e) {
            LOG.error("Error occured during processing!", e);
        }
    }


    /**
     * Called after the BMS received data.
     */
    private void receivedData() {
        LOG.info(createBatteryOverview());

        if (mqttProducer != null) {
            // send energystorage data to MQTT broker
            try {
                mqttProducer.sendMessage(energyStorage.toJson());
            } catch (final IOException e) {
                LOG.error("Failed to send MQTT message!", e);
            }
        }

        analyseBMSFaults();

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

        for (int i = 0; i < energyStorage.getBatteryPacks().length; i++) {
            for (final String key : getAlarmState(energyStorage.getBatteryPack(i))) {
                currentAlarms.add("BMS #" + (i + 1) + ":" + key);
                alarmContent.append("\tBMS #" + (i + 1) + ":\t" + key + "\r\n");
            }
        }

        // check if alarms have changed
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


    private List<String> getAlarmState(final BatteryPack pack) {
        final List<String> activeAlarms = new ArrayList<>();
        final Alarms alarms = pack.alarms;

        if (alarms.levelOneCellVoltageTooHigh.value == true) {
            activeAlarms.add(alarms.levelOneCellVoltageTooHigh.key);
        }
        if (alarms.levelTwoCellVoltageTooHigh.value == true) {
            activeAlarms.add(alarms.levelTwoCellVoltageTooHigh.key);
        }
        if (alarms.levelOneCellVoltageTooLow.value == true) {
            activeAlarms.add(alarms.levelOneCellVoltageTooLow.key);
        }
        if (alarms.levelTwoCellVoltageTooLow.value == true) {
            activeAlarms.add(alarms.levelTwoCellVoltageTooLow.key);
        }
        if (alarms.levelOnePackVoltageTooHigh.value == true) {
            activeAlarms.add(alarms.levelOnePackVoltageTooHigh.key);
        }
        if (alarms.levelTwoPackVoltageTooHigh.value == true) {
            activeAlarms.add(alarms.levelTwoPackVoltageTooHigh.key);
        }
        if (alarms.levelOnePackVoltageTooLow.value == true) {
            activeAlarms.add(alarms.levelOnePackVoltageTooLow.key);
        }
        if (alarms.levelTwoPackVoltageTooLow.value == true) {
            activeAlarms.add(alarms.levelTwoPackVoltageTooLow.key);
        }
        if (alarms.levelOneChargeTempTooHigh.value == true) {
            activeAlarms.add(alarms.levelOneChargeTempTooHigh.key);
        }
        if (alarms.levelTwoChargeTempTooHigh.value == true) {
            activeAlarms.add(alarms.levelTwoChargeTempTooHigh.key);
        }
        if (alarms.levelOneChargeTempTooLow.value == true) {
            activeAlarms.add(alarms.levelOneChargeTempTooLow.key);
        }
        if (alarms.levelTwoChargeTempTooLow.value == true) {
            activeAlarms.add(alarms.levelTwoChargeTempTooLow.key);
        }
        if (alarms.levelOneDischargeTempTooHigh.value == true) {
            activeAlarms.add(alarms.levelOneDischargeTempTooHigh.key);
        }
        if (alarms.levelTwoDischargeTempTooHigh.value == true) {
            activeAlarms.add(alarms.levelTwoDischargeTempTooHigh.key);
        }
        if (alarms.levelOneDischargeTempTooLow.value == true) {
            activeAlarms.add(alarms.levelOneDischargeTempTooLow.key);
        }
        if (alarms.levelTwoDischargeTempTooLow.value == true) {
            activeAlarms.add(alarms.levelTwoDischargeTempTooLow.key);
        }
        if (alarms.levelOneChargeCurrentTooHigh.value == true) {
            activeAlarms.add(alarms.levelOneChargeCurrentTooHigh.key);
        }
        if (alarms.levelTwoChargeCurrentTooHigh.value == true) {
            activeAlarms.add(alarms.levelTwoChargeCurrentTooHigh.key);
        }
        if (alarms.levelOneDischargeCurrentTooHigh.value == true) {
            activeAlarms.add(alarms.levelOneDischargeCurrentTooHigh.key);
        }
        if (alarms.levelTwoDischargeCurrentTooHigh.value == true) {
            activeAlarms.add(alarms.levelTwoDischargeCurrentTooHigh.key);
        }
        if (alarms.levelOneStateOfChargeTooHigh.value == true) {
            activeAlarms.add(alarms.levelOneStateOfChargeTooHigh.key);
        }
        if (alarms.levelTwoStateOfChargeTooHigh.value == true) {
            activeAlarms.add(alarms.levelTwoStateOfChargeTooHigh.key);
        }
        if (alarms.levelOneStateOfChargeTooLow.value == true) {
            activeAlarms.add(alarms.levelOneStateOfChargeTooLow.key);
        }
        if (alarms.levelTwoStateOfChargeTooLow.value == true) {
            activeAlarms.add(alarms.levelTwoStateOfChargeTooLow.key);
        }
        if (alarms.levelOneCellVoltageDifferenceTooHigh.value == true) {
            activeAlarms.add(alarms.levelOneCellVoltageDifferenceTooHigh.key);
        }
        if (alarms.levelTwoCellVoltageDifferenceTooHigh.value == true) {
            activeAlarms.add(alarms.levelTwoCellVoltageDifferenceTooHigh.key);
        }
        if (alarms.levelOneTempSensorDifferenceTooHigh.value == true) {
            activeAlarms.add(alarms.levelOneTempSensorDifferenceTooHigh.key);
        }
        if (alarms.levelTwoTempSensorDifferenceTooHigh.value == true) {
            activeAlarms.add(alarms.levelTwoTempSensorDifferenceTooHigh.key);
        }
        if (alarms.chargeFETTemperatureTooHigh.value == true) {
            activeAlarms.add(alarms.chargeFETTemperatureTooHigh.key);
        }
        if (alarms.dischargeFETTemperatureTooHigh.value == true) {
            activeAlarms.add(alarms.dischargeFETTemperatureTooHigh.key);
        }
        if (alarms.failureOfChargeFETTemperatureSensor.value == true) {
            activeAlarms.add(alarms.failureOfChargeFETTemperatureSensor.key);
        }
        if (alarms.failureOfDischargeFETTemperatureSensor.value == true) {
            activeAlarms.add(alarms.failureOfDischargeFETTemperatureSensor.key);
        }
        if (alarms.failureOfChargeFETAdhesion.value == true) {
            activeAlarms.add(alarms.failureOfChargeFETAdhesion.key);
        }
        if (alarms.failureOfDischargeFETAdhesion.value == true) {
            activeAlarms.add(alarms.failureOfDischargeFETAdhesion.key);
        }
        if (alarms.failureOfChargeFETTBreaker.value == true) {
            activeAlarms.add(alarms.failureOfChargeFETTBreaker.key);
        }
        if (alarms.failureOfDischargeFETBreaker.value == true) {
            activeAlarms.add(alarms.failureOfDischargeFETBreaker.key);
        }
        if (alarms.failureOfAFEAcquisitionModule.value == true) {
            activeAlarms.add(alarms.failureOfAFEAcquisitionModule.key);
        }
        if (alarms.failureOfVoltageSensorModule.value == true) {
            activeAlarms.add(alarms.failureOfVoltageSensorModule.key);
        }
        if (alarms.failureOfTemperatureSensorModule.value == true) {
            activeAlarms.add(alarms.failureOfTemperatureSensorModule.key);
        }
        if (alarms.failureOfEEPROMStorageModule.value == true) {
            activeAlarms.add(alarms.failureOfEEPROMStorageModule.key);
        }
        if (alarms.failureOfRealtimeClockModule.value == true) {
            activeAlarms.add(alarms.failureOfRealtimeClockModule.key);
        }
        if (alarms.failureOfPrechargeModule.value == true) {
            activeAlarms.add(alarms.failureOfPrechargeModule.key);
        }
        if (alarms.failureOfVehicleCommunicationModule.value == true) {
            activeAlarms.add(alarms.failureOfVehicleCommunicationModule.key);
        }
        if (alarms.failureOfIntranetCommunicationModule.value == true) {
            activeAlarms.add(alarms.failureOfIntranetCommunicationModule.key);
        }
        if (alarms.failureOfCurrentSensorModule.value == true) {
            activeAlarms.add(alarms.failureOfCurrentSensorModule.key);
        }
        if (alarms.failureOfMainVoltageSensorModule.value == true) {
            activeAlarms.add(alarms.failureOfMainVoltageSensorModule.key);
        }
        if (alarms.failureOfShortCircuitProtection.value == true) {
            activeAlarms.add(alarms.failureOfShortCircuitProtection.key);
        }
        if (alarms.failureOfLowVoltageNoCharging.value == true) {
            activeAlarms.add(alarms.failureOfLowVoltageNoCharging.key);
        }

        return activeAlarms;
    }


    @Override
    public void close() {
        try {
            executorService.shutdownNow();
            LOG.info("Shutting down BMS and inverter threads...OK");
        } catch (final Exception e) {
            LOG.info("Shutting down BMS and inverter threads...FAILED");
        }

        if (mqttProducer != null) {
            try {
                mqttProducer.close();
                LOG.info("Shutting down MQTT producer threads...OK");
            } catch (final Exception e) {
                LOG.info("Shutting down MQTT producer threads...FAILED");
            }
        }

        if (mqttBroker != null) {
            try {
                mqttBroker.close();
                LOG.info("Shutting down MQTT broker threads...OK");
            } catch (final Exception e) {
                LOG.info("Shutting down MQTT broker threads...FAILED");
            }
        }

        try {
            energyStorage.close();
        } catch (final Exception e) {
        }
    }


    private String createBatteryOverview() {
        final StringBuffer log = new StringBuffer();
        // header
        log.append("\nBMS\tSOC\t  V  \t  A  \t CellMinV \t CellMaxV\tCellDiff\n");

        for (int bmsNo = 0; bmsNo < energyStorage.getBatteryPackCount(); bmsNo++) {
            final BatteryPack b = energyStorage.getBatteryPack(bmsNo);

            log.append(bmsNo + 1
                    + "\t " + b.packSOC / 10f
                    + "\t" + b.packVoltage / 10f
                    + "\t" + b.packCurrent / 10f
                    + "\t" + b.minCellmV / 1000f + "(#" + b.minCellVNum + ") "
                    + "\t" + b.maxCellmV / 1000f + "(#" + b.maxCellVNum / 1000f + ")"
                    + "\t" + (b.maxCellmV - b.minCellmV) / 1000f + " \n");
        }

        return log.toString();
    }


    /**
     * Reads the <code>config.properties</code> and adds them to the system properties.
     */
    private static void updateSystemProperties() {
        final Properties props = new Properties();
        try {
            props.load(BmsToInverter.class.getClassLoader().getResourceAsStream("config.properties"));

            for (final Object name : props.keySet()) {
                final String key = name.toString();
                if (System.getProperty(key) == null) {
                    System.setProperty(key, props.getProperty(key));
                }

            }
        } catch (final IOException e) {
            LOG.warn("No properties file \"config.properties\" found - should then all be set via command line -D parameters");
        }
    }

}
