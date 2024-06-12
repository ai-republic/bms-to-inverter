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
package com.airepublic.bmstoinverter.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for general purpose methods.
 */
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


    /**
     * Gets the bit at the specified index of the value.
     *
     * @param value the value
     * @param index the index of the bit
     * @return true if the bit is 1, otherwise false
     */
    public static boolean bit(final int value, final int index) {
        return (value >> index & 1) == 1;
    }


    /**
     * Gets the bit at the specified index of the value.
     *
     * @param value the value
     * @param index the index of the bit
     * @return true if the bit is 1, otherwise false
     */
    public static boolean bit(final long value, final int index) {
        return (value >> index & 1) == 1;
    }


    /**
     * Gets the specified number of bits starting at the specified index.
     *
     * @param value the value to read from
     * @param index the index of the first bit to read in the value
     * @param length the number of bits to read
     * @return the value represented by the returned bits
     */
    public static int bits(final int value, final int index, final int length) {
        return value >> index & (1 << length) - 1;
    }


    /**
     * Sets the bit in the value at the specified index to on or off.
     *
     * @param value the byte value
     * @param index the index
     * @param on the flag whether to set the bit to 1 (on) or 0 (off)
     */
    public static void setBit(byte value, final int index, final boolean on) {
        if (on) {
            value |= 1 << index;
        } else {
            value &= ~(1 << index);
        }
    }


    /**
     * Sets the bit in the value at the specified index to on or off.
     *
     * @param value the short value
     * @param index the index
     * @param on the flag whether to set the bit to 1 (on) or 0 (off)
     */
    public static void setBit(short value, final int index, final boolean on) {
        if (on) {
            value |= 1 << index;
        } else {
            value &= ~(1 << index);
        }
    }


    /**
     * Sets the bit in the value at the specified index to on or off.
     *
     * @param value the short value
     * @param index the index
     * @param on the flag whether to set the bit to 1 (on) or 0 (off)
     */
    public static void setBit(char value, final int index, final boolean on) {
        if (on) {
            value |= 1 << index;
        } else {
            value &= ~(1 << index);
        }
    }


    /**
     * Sets the bit in the value at the specified index to on or off.
     *
     * @param value the integer value
     * @param index the index
     * @param on the flag whether to set the bit to 1 (on) or 0 (off)
     */
    public static void setBit(int value, final int index, final boolean on) {
        if (on) {
            value |= 1 << index;
        } else {
            value &= ~(1 << index);
        }
    }


    /**
     * Sets the bit in the value at the specified index to on or off.
     *
     * @param value the long value
     * @param index the index
     * @param on the flag whether to set the bit to 1 (on) or 0 (off)
     */
    public static void setBit(long value, final int index, final boolean on) {
        if (on) {
            value |= 1 << index;
        } else {
            value &= ~(1 << index);
        }
    }

}
