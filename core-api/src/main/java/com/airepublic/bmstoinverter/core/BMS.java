package com.airepublic.bmstoinverter.core;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.inject.Inject;

/**
 * The abstract class to identify a BMS.
 */
public abstract class BMS implements AutoCloseable {
    private final static Logger LOG = LoggerFactory.getLogger(BMS.class);
    @Inject
    private EnergyStorage energyStorage;

    /**
     * Any on-startup neccessary code should go here.
     */
    public abstract void initialize();


    /**
     * Processes the collection of data for each configured {@link BatteryPack}.
     * 
     * @param callback the function will be called after successfully collecting data from all
     *        {@link BatteryPack}s
     */
    public final void process(final Runnable callback) {
        try {
            LOG.info("---------------------------------> Thread " + Thread.currentThread().getId());
            clearBuffers();

            for (int bmsNo = 0; bmsNo < energyStorage.getBatteryPackCount(); bmsNo++) {
                @SuppressWarnings("resource")
                final Port port = energyStorage.getBatteryPack(bmsNo).port;

                try {
                    port.ensureOpen();
                    collectData(bmsNo);
                } catch (final NoDataAvailableException e) {
                    LOG.error("Received no bytes too many times - trying to close and re-open port!");
                    // try to close and re-open the port
                    port.close();
                    port.open();
                }
            }
            // autoCalibrateSOC();
        } catch (final TooManyInvalidFramesException e) {
            LOG.error("Received too many invalid frames - start new reading round!");
            return;
        } catch (final Throwable e) {
            LOG.error("Error requesting data!", e);
            return;
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
     * @param bmsNo the index of the {@link BatteryPack} in the {@link EnergyStorage}
     * @throws IOException if there is a problem with the port
     * @throws TooManyInvalidFramesException when too many invalid frames were received
     * @throws NoDataAvailableException when no data was received too many times
     */
    protected abstract void collectData(int bmsNo) throws IOException, TooManyInvalidFramesException, NoDataAvailableException;


    /**
     * Clears any buffers or queues on all associated ports to restart communication.
     */
    protected void clearBuffers() {
        for (final BatteryPack pack : energyStorage.getBatteryPacks()) {
            pack.port.clearBuffers();
        }
    }


    @Override
    public void close() throws Exception {
        energyStorage.close();
    }

}
