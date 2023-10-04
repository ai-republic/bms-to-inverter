package com.airepublic.bmstoinverter.webserver;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.airepublic.bmstoinverter.core.service.IMQTTConsumerService;
import com.google.gson.Gson;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@SpringBootApplication
@RestController
public class WebserverApplication {
    private final static Logger LOG = LoggerFactory.getLogger(WebserverApplication.class);
    @Value("${mqtt.locator}")
    private String locator;
    @Value("${mqtt.topic}")
    private String topic;
    private String data = "";
    private final IMQTTConsumerService mqtt = ServiceLoader.load(IMQTTConsumerService.class).findFirst().orElse(null);
    private String alarmMessages;

    public static void main(final String[] args) {
        SpringApplication.run(WebserverApplication.class, args);
    }


    @PostConstruct
    public void init() throws Exception {
        final ResourceBundle bundle = ResourceBundle.getBundle("alarms");
        final Map<String, String> map = new LinkedHashMap<>();
        for (final String key : bundle.keySet()) {
            map.put(key, bundle.getString(key));
        }

        alarmMessages = new Gson().toJson(map);

        startMQTTClient();
    }


    @SuppressWarnings("resource")
    private void startMQTTClient() throws Exception {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        LOG.info("Connecting to MQTT at {}/{}", locator, topic);
        mqtt.create(locator, topic);

        try {
            LOG.info("Connected to MQTT at {}/{}", locator, topic);

            executor.scheduleAtFixedRate(() -> {
                try {
                    synchronized (data) {
                        data = mqtt.consume(1000);
                    }
                    LOG.info("Successfully received data");

                } catch (final IOException e) {
                    LOG.error("Error receiving MQTT data", e);
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);

        } catch (final Exception e) {
            LOG.error("Error initializing MQTT consumer", e);
        }
    }


    @GetMapping("/data")
    public String data() {
        synchronized (data) {
            return data;
        }
    }


    @GetMapping("/alarmMessages")
    public String alarmMessages() {
        return alarmMessages;
    }


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
