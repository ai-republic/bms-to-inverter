package com.airepublic.bmstoinverter.core;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.inject.Inject;

public abstract class AbstractBMSProcessor implements BMS {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractBMSProcessor.class);
    @Inject
    private EnergyStorage energyStorage;

    @Override
    public void process(final Runnable callback) {
        try {
            LOG.info("---------------------------------> Thread " + Thread.currentThread().getId());
            clearBuffers();

            for (int bmsNo = 0; bmsNo < energyStorage.getBatteryPackCount(); bmsNo++) {
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
