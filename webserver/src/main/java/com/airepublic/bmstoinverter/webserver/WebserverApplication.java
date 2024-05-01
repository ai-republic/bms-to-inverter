package com.airepublic.bmstoinverter.webserver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.service.IMQTTConsumerService;
import com.google.gson.Gson;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Webserver to display the state of all BMSes and batteries in the system.
 */
@SpringBootApplication
@RestController
public class WebserverApplication {
    private final static Logger LOG = LoggerFactory.getLogger(WebserverApplication.class);
    @Value("${server.mqtt.locator}")
    private String locator;
    @Value("${server.mqtt.topic}")
    private String topic;
    private String data = "";
    private final IMQTTConsumerService mqtt = ServiceLoader.load(IMQTTConsumerService.class).findFirst().orElse(null);
    private String alarmMessages;

    /**
     * Starts the webserver application.
     * 
     * @param args none
     */
    public static void main(final String[] args) {
        SpringApplication.run(WebserverApplication.class, args);
    }


    /**
     * Initializes the alarm messages and starts the MQTT client.
     *
     * @throws Exception if resource bundles could not be found
     */
    @PostConstruct
    public void init() throws Exception {
        final ResourceBundle bundle = ResourceBundle.getBundle("alarms");
        final Map<String, String> map = new LinkedHashMap<>();

        for (final String key : bundle.keySet()) {
            map.put(key, bundle.getString(key));
        }

        alarmMessages = new Gson().toJson(map);

        startMQTTConsumer();
    }


    private void startMQTTConsumer() throws Exception {
        LOG.info("Connecting to MQTT at {}/{}", locator, topic);

        try {
            mqtt.create(locator, topic, msg -> {
                data = msg;
                LOG.info("Successfully received data");

            });
            LOG.info("Connected to MQTT at {}/{}", locator, topic);

        } catch (final Exception e) {
            LOG.error("Error initializing MQTT consumer", e);
        }
    }


    /**
     * Gets the {@link EnergyStorage} JSON string.
     *
     * @return the {@link EnergyStorage} JSON string
     */
    @GetMapping("/data")
    public String data() {
        if (!mqtt.isRunning()) {
            try {
                mqtt.close();
            } catch (final Exception e) {
            }

            try {
                startMQTTConsumer();
                Thread.sleep(1000L);
            } catch (final Exception e) {
            }
        }

        synchronized (data) {
            return data;
        }
    }


    /**
     * Gets the alarm message resource bundle as JSON string.
     *
     * @return the alarm message map as JSON string
     */
    @GetMapping("/alarmMessages")
    public String alarmMessages() {
        return alarmMessages;
    }


    /**
     * Close MQTT client on shutdown.
     */
    @PreDestroy
    public void close() {
        try {
            if (mqtt != null) {
                mqtt.close();
            }
        } catch (final Exception e) {
        }
    }

}
