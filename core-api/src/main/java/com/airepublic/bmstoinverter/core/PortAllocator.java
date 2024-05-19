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
package com.airepublic.bmstoinverter.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the {@link Port}s used by the {@link BMS}es to ensure that each BMSes reading round is
 * only for their use.
 */
public class PortAllocator {
    private final static Map<String, Port> ports = new ConcurrentHashMap<>();
    private final static Map<String, Boolean> usage = new ConcurrentHashMap<>();

    /**
     * Adds the {@link Port} to be managed.
     * 
     * @param portLocator the port locator
     * @param port the {@link Port}
     */
    public static void addPort(final String portLocator, final Port port) {
        ports.put(portLocator, port);
        usage.put(portLocator, false);
    }


    /**
     * Returns true if a {@link Port} for the specified port locator is being managed.
     *
     * @param portLocator the port locator
     * @return true if managed otherwise false
     */
    public static boolean hasPort(final String portLocator) {
        return ports.containsKey(portLocator);
    }


    /**
     * Allocates a {@link Port} to be used. If the {@link Port} is being used it will wait until the
     * {@link Port} becomes freed again.
     *
     * @param portLocator the port locator
     * @return the {@link Port}
     */
    public final static Port allocate(final String portLocator) {
        final Port port = ports.get(portLocator);

        synchronized (port) {
            if (usage.get(portLocator)) {
                try {
                    port.wait();
                } catch (final InterruptedException e) {
                }
            }
            return port;
        }
    }


    /**
     * Frees the Port to be used again by the next allocation.
     *
     * @param portLocator the port locator
     */
    public static void free(final String portLocator) {
        final Port port = ports.get(portLocator);

        synchronized (port) {
            usage.put(portLocator, false);
            port.notify();
        }
    }


    /**
     * Closes all managed {@link Port}s.
     *
     * @throws Exception if an exception occurs
     */
    public static void close() throws Exception {
        ports.values().forEach(port -> port.close());
    }

}
