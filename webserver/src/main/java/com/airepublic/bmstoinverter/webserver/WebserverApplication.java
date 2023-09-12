package com.airepublic.bmstoinverter.webserver;

import java.io.IOException;
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

import com.airepublic.bmstoinverter.service.mqtt.MQTTConsumerService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@SpringBootApplication
@RestController
public class WebserverApplication {
    private final static Logger LOG = LoggerFactory.getLogger(WebserverApplication.class);
    @Value("${mqtt.host}")
    private String host;
    @Value("${mqtt.port}")
    private String port;
    @Value("${mqtt.locator}")
    private String locator;
    @Value("${mqtt.topic}")
    private String topic;
    private String data = "";
    private MQTTConsumerService mqtt;

    public static void main(final String[] args) {
        SpringApplication.run(WebserverApplication.class, args);
    }


    @SuppressWarnings("resource")
    @PostConstruct
    public void startMQTTClient() throws IOException {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        LOG.info("Connecting to MQTT at {}/{}", locator, topic);
        mqtt = new MQTTConsumerService().create(locator, topic);

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


    @GetMapping("/config")
    public String host() {
        return "{\"host\":\"" + host + "\",\"port\":" + port + ",\"topic\":\"" + topic + "\"}";
    }


    @GetMapping("/data")

    public String data() {
        synchronized (data) {
            return data;
        }
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
