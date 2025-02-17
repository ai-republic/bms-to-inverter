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

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.inject.Inject;

/**
 * The abstract class to identify a BMS.
 */
public abstract class BMS {
    private final static Logger LOG = LoggerFactory.getLogger(BMS.class);
    private final Map<Integer, BatteryPack> batteryPacks = new LinkedHashMap<>();
    private BMSConfig config;
    private Set<BmsPlugin> plugins;
    @Inject
    private transient EnergyStorage energyStorage;

    /**
     * Initializes the BMS with the specified {@link BMSConfig}, initializing the port parameters
     * from the system properties.
     * 
     * @param config the {@link BMSConfig}
     */
    public void initialize(final BMSConfig config) {
        this.config = config;

        if (getPlugins() != null) {
            getPlugins().stream().forEach(p -> {
                LOG.debug("Calling BMS plugin (onInitialize): {}", p.getName());
                p.onInitialize(this);
            });
        }

        if (!PortAllocator.hasPort(config.getPortLocator())) {
            final Port port = config.getDescriptor().createPort(config);
            PortAllocator.addPort(config.getPortLocator(), port);
        }
    }


    /**
     * Gets the name of the {@link BMSDescriptor}.
     *
     * @return the name
     */
    public String getName() {
        return config.getDescriptor().getName();
    }


    /**
     * Gets the id of the BMS.
     * 
     * @return the id of the BMS
     */
    public int getBmsId() {
        return config.getBmsId();
    }


    /**
     * Gets the assigned {@link Port}s locator.
     *
     * @return the assigned {@link Port}s locator
     */
    public String getPortLocator() {
        return config.getPortLocator();
    }


    /**
     * Gets the {@link BatteryPack}s associated with this {@link BMS}.
     *
     * @return the {@link BatteryPack}s associated with this {@link BMS}
     */
    public Collection<BatteryPack> getBatteryPacks() {
        return batteryPacks.values();
    }


    /**
     * Gets the {@link BatteryPack} at the specified index associated with this {@link BMS}. If the
     * number is greater than already known, then the pack size will increase until it has the
     * number specified.
     *
     * @param batteryId the id of the {@link BatteryPack}
     * @return the {@link BatteryPack} with the specified id associated with this {@link BMS}
     */
    public BatteryPack getBatteryPack(final int batteryId) {
        BatteryPack pack = batteryPacks.get(batteryId);

        if (pack == null) {
            pack = new BatteryPack();
            batteryPacks.put(batteryId, pack);
            energyStorage.getBatteryPacks().add(pack);
        }

        return pack;
    }


    /**
     * Gets the delay after no bytes were received in milliseconds.
     *
     * @return the delay after no bytes were received in milliseconds
     */
    public long getDelayAfterNoBytes() {
        return config.getDelayAfterNoBytes();
    }


    /**
     * Gets the {@link BmsPlugin}s.
     *
     * @return the {@link BmsPlugin}s
     */
    public Set<BmsPlugin> getPlugins() {
        return plugins;
    }


    /**
     * Sets the {@link BmsPlugin}s.
     * 
     * @param plugins the {@link BmsPlugin}s to set
     */
    public void setPlugins(final Set<BmsPlugin> plugins) {
        this.plugins = plugins;
    }


    /**
     * Processes the collection of data for each configured {@link BatteryPack}.
     * 
     * @param callback the function will be called after successfully collecting data from all
     *        {@link BatteryPack}s
     */
    public void process(final Runnable callback) {
        try {
            final Port port = PortAllocator.allocate(getPortLocator());

            try {
                port.ensureOpen();
                port.clearBuffers();

                if (getPlugins() != null) {
                    getPlugins().stream().forEach(p -> {
                        LOG.debug("Calling BMS plugin (afterCollectData): {}", p.getName());
                        p.beforeCollectData(this);
                    });
                }

                collectData(port);

                if (getPlugins() != null) {
                    getPlugins().stream().forEach(p -> {
                        LOG.debug("Calling BMS plugin (afterCollectData): {}", p.getName());
                        p.beforeCollectData(this);
                    });
                }
            } catch (final NoDataAvailableException e) {
                LOG.error("Received no bytes too many times - trying to close and re-open port!");
                // try to close and re-open the port
                port.close();
                port.open();
            }
            // autoCalibrateSOC();
        } catch (final TooManyInvalidFramesException e) {
            LOG.error("Received too many invalid frames - start new reading round!");
            return;
        } catch (final Throwable e) {
            LOG.error("Error requesting data!", e);
            return;
        } finally {
            PortAllocator.free(getPortLocator());
        }

        try {
            callback.run();
        } catch (final Throwable e) {
            LOG.error("BMS process callback threw an exception!", e);
        }
    }


    /**
     * Processes the collection of data from the specified {@link BatteryPack}.
     *
     * @param port the allocated {@link Port}
     * @throws IOException if there is a problem with the port
     * @throws TooManyInvalidFramesException when too many invalid frames were received
     * @throws NoDataAvailableException when no data was received too many times
     */
    protected abstract void collectData(Port port) throws IOException, TooManyInvalidFramesException, NoDataAvailableException;


    /**
     * ONLY FOR TESTING: Set the energy storage.
     *
     * @param energyStorage the energy storage
     */
    protected void setEnergyStorage(final EnergyStorage energyStorage) {
        this.energyStorage = energyStorage;
    }

}
