package com.airepublic.bmstoinverter.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

/**
 * The class to identify an {@link Inverter}.
 */
public abstract class Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(Inverter.class);
    private String portLocator;
    private int sendInterval;

    /**
     * Initializes the {@link Inverter} with the specified {@link InverterConfig}, initializing the
     * port parameters from the system properties.
     */
    public void initialize(final InverterConfig config) {
        if (!PortAllocator.hasPort(config.getPortLocator())) {
            PortAllocator.addPort(config.getPortLocator(), config.getDescriptor().createPort(config));
        }

        portLocator = config.getPortLocator();
        sendInterval = config.getSendInterval();
    }


    /**
     * Gets the interval the data is sent to the inverter.
     *
     * @return the interval the data is sent to the inverter
     */
    public int getSendInterval() {
        return sendInterval;
    }


    /**
     * Gets the assigned {@link Port}s locator.
     *
     * @return the assigned {@link Port}s locator
     */
    public String getPortLocator() {
        return portLocator;
    }


    /**
     * Process sending the data via the {@link Port} to the {@link Inverter}.
     *
     * @param callback the code executed after successful processing
     */
    public void process(final Runnable callback) {
        try {
            final List<ByteBuffer> sendFrames = createSendFrames();
            final Port port = PortAllocator.allocate(getPortLocator());

            for (final ByteBuffer frame : sendFrames) {
                LOG.debug("Inverter send: {}", Port.printBuffer(frame));
                sendFrame(port, frame);
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
     * Implementations must send the frame depending on its protocol.
     *
     * @param port the {@link Port}
     * @param frame the complete frame
     * @throws IOException if the frame could not be sent
     */
    protected abstract void sendFrame(Port port, ByteBuffer frame) throws IOException;


    /**
     * Aggregate all {@link BatteryPack}s of the {@link EnergyStorage} and create CAN messages to be
     * sent to the inverter.
     *
     * @return the CAN messages to be sent to the inverter
     */
    protected abstract List<ByteBuffer> createSendFrames();
}
