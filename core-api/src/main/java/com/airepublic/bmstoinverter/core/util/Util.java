package com.airepublic.bmstoinverter.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
    private final static Logger LOG = LoggerFactory.getLogger(Util.class);

    /**
     * Reads the <code>config.properties</code> and adds them to the system properties.
     */
    public static void updateSystemProperties(final Path config) {
        final Properties props = new Properties();

        // try to load from configured path
        try {
            LOG.info("Loading config.properties from: " + config);
            props.load(Files.newInputStream(config));
        } catch (final IOException e) {
            // try to load from resource path
            try {
                props.load(Util.class.getClassLoader().getResourceAsStream("config.properties"));
            } catch (final IOException e1) {
                LOG.warn("No properties file \"config.properties\" found - should then all be set via command line -D parameters");
            }
        }

        // set all as system properties
        for (final Object name : props.keySet()) {
            final String key = name.toString();
            if (System.getProperty(key) == null) {
                System.setProperty(key, props.getProperty(key));
            }
        }
    }
}
