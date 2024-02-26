package com.airepublic.bmstoinverter.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.inject.Inject;

/**
 * The class to identify an {@link Inverter}.
 */
public abstract class Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(Inverter.class);
    private InverterConfig config;
    @Inject
    private EnergyStorage energyStorage;

    /**
     * Initializes the {@link Inverter} with the specified {@link InverterConfig}, initializing the
     * port parameters from the system properties.
     */
    public void initialize(final InverterConfig config) {
        if (!PortAllocator.hasPort(config.getPortLocator())) {
            PortAllocator.addPort(config.getPortLocator(), config.getDescriptor().createPort(config));
        }
        this.config = config;
    }


    /**
     * Gets the name of the {@link InverterDescriptor}.
     *
     * @return the name
     */
    public String getName() {
        return config.getDescriptor().getName();
    }


    /**
     * Gets the interval the data is sent to the inverter.
     *
     * @return the interval the data is sent to the inverter
     */
    public int getSendInterval() {
        return config.getSendInterval();
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
     * Process sending the data via the {@link Port} to the {@link Inverter}.
     *
     * @param callback the code executed after successful processing
     */
    public void process(final Runnable callback) {
        try {
            final BatteryPack pack = energyStorage.getAggregatedBatteryInfo();
            final Port port = PortAllocator.allocate(getPortLocator());
            final ByteBuffer requestFrame = readRequest(port);
            LOG.debug("Inverter received: " + Port.printBuffer(requestFrame));
            final List<ByteBuffer> sendFrames = createSendFrames(requestFrame, pack);

            if (sendFrames != null) {
                for (final ByteBuffer frame : sendFrames) {
                    LOG.debug("Inverter send: {}", Port.printBuffer(frame));
                    sendFrame(port, frame);
                }
            }
        } catch (final Throwable e) {
            LOG.error("Failed to send CAN frame", e);
        }

        try {
            callback.run();
        } catch (final Exception e) {
            LOG.error("Inverter process callback threw an exception!", e);
        }
    }


    /**
     * Read the next request (if any) to be responded to the inverter.
     *
     * @param port the {@link Port}
     * @return the received frame or null if no frames need to be read
     * @throws IOException if the frame could not be read
     */
    protected abstract ByteBuffer readRequest(Port port) throws IOException;


    /**
     * Implementations must send the frame depending on its protocol.
     *
     * @param port the {@link Port}
     * @param frame the complete frame
     * @throws IOException if the frame could not be sent
     */
    protected abstract void sendFrame(Port port, ByteBuffer frame) throws IOException;


    /**
     * Create CAN messages for the specified request frame (if any) using the aggregated
     * {@link BatteryPack}s of the {@link EnergyStorage} which will be sent to the inverter.
     *
     * @param requestFrame the request frame if any
     * @param aggregatedPack the {@link BatteryPack} resembling and aggregation of all
     *        {@link EnergyStorage}'s {@link BatteryPack}s.
     * @return the CAN messages to be sent to the inverter
     */
    protected abstract List<ByteBuffer> createSendFrames(ByteBuffer requestFrame, BatteryPack aggregatedPack);
}
