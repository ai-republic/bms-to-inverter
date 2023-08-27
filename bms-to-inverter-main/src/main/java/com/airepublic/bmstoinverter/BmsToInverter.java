package com.airepublic.bmstoinverter;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.bms.data.BatteryPack;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.inject.Inject;

public class BmsToInverter {
    private final static Logger LOG = LoggerFactory.getLogger(BmsToInverter.class);
    @Inject
    private BatteryPack[] batteryPacks;
    @Inject
    @Bms
    private PortProcessor bms;
    @Inject
    @Inverter
    private PortProcessor inverter;

    public static void main(final String[] args) throws IOException {
        // update all non-specified system parameters from "pi.properties"
        updateSystemProperties();

        final SeContainerInitializer initializer = SeContainerInitializer.newInstance();
        final SeContainer container = initializer.initialize();
        final BmsToInverter app = container.select(BmsToInverter.class).get();
        app.start();
    }


    public void start() {
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            Future<?> result = executorService.submit(() -> bms.process());
            result.get();

            while (true) {

                // receive BMS data
                try {
                    result = executorService.submit(() -> bms.process());
                    result.get();
                } catch (final Exception e) {
                    LOG.error("Error receiving BMS data!", e);
                }

                synchronized (System.out) {
                    LOG.info(createBatteryOverview());
                }

                // send data to inverter
                try {
                    result = executorService.submit(() -> inverter.process());
                    result.get();
                } catch (final Exception e) {
                    LOG.error("Error sending inverter data!", e);
                }
            }
        } catch (final Throwable e) {
            LOG.error("Failed to perform initial reading of BMS values!", e);
            executorService.shutdownNow();
        }
    }


    private String createBatteryOverview() {
        final StringBuffer log = new StringBuffer();
        // header
        log.append("\nBMS\tSOC\t  V  \t  A  \t CellMinV \t CellMaxV\tCellDiff\n");

        for (final BatteryPack b : batteryPacks) {
            log.append(b.packNumber + 1
                    + "\t " + b.packSOC / 10f
                    + "\t" + b.packVoltage / 10f
                    + "\t" + b.packCurrent / 10f
                    + "\t" + b.minCellmV / 1000f + "(#" + b.minCellVNum + ") "
                    + "\t" + b.maxCellmV / 1000f + "(#" + b.maxCellVNum / 1000f + ")"
                    + "\t" + (b.maxCellmV - b.minCellmV) / 1000f + " \n");
        }

        return log.toString();
    }


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