package com.airepublic.bmstoinverter.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private final List<BatteryPack> batteryPacks = new ArrayList<>();
    private BMSConfig config;
    @Inject
    private transient EnergyStorage energyStorage;

    /**
     * Initializes the BMS with the specified {@link BMSConfig}, initializing the port parameters
     * from the system properties.
     * 
     * @param config the {@link BMSConfig}
     */
    public void initialize(final BMSConfig config) {
        if (!PortAllocator.hasPort(config.getPortLocator())) {
            final Port port = config.getDescriptor().createPort(config);
            PortAllocator.addPort(config.getPortLocator(), port);
        }
        this.config = config;
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
    public List<BatteryPack> getBatteryPacks() {
        return batteryPacks;
    }


    /**
     * Gets the {@link BatteryPack} at the specified index associated with this {@link BMS}. If the
     * number is greater than already known, then the pack size will increase until it has the
     * number specified.
     *
     * @param index the index of the {@link BatteryPack}
     * @return the {@link BatteryPack} at the specified index associated with this {@link BMS}
     */
    public BatteryPack getBatteryPack(final int index) {
        // TODO find a better solution and also change energystorage and webserver
        while (getBatteryPacks().size() <= index) {
            final BatteryPack pack = new BatteryPack();
            batteryPacks.add(pack);
            energyStorage.getBatteryPacks().add(pack);
        }

        return batteryPacks.get(index);
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
     * Processes the collection of data for each configured {@link BatteryPack}.
     * 
     * @param callback the function will be called after successfully collecting data from all
     *        {@link BatteryPack}s
     */
    public final void process(final Runnable callback) {
        try {
            LOG.info("---------------------------------> Thread " + Thread.currentThread().getId());

            final Port port = PortAllocator.allocate(getPortLocator());

            try {
                port.ensureOpen();
                port.clearBuffers();
                collectData(port);
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
     * Sets the {@link EnergyStorage}.
     *
     * @param energyStorage the {@link EnergyStorage}
     */
    protected void setEnergyStorage(final EnergyStorage energyStorage) {
        this.energyStorage = energyStorage;
    }
}
