package com.airepublic.bmstoinverter.growatt.rs485;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortProcessor;
import com.airepublic.bmstoinverter.core.PortType;
import com.airepublic.bmstoinverter.core.Protocol;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.inject.Inject;

/**
 * The {@link PortProcessor} to handle RS485 messages for a Growatt low voltage (12V/24V/48V)
 * inverter.
 */
@Inverter
@PortType(Protocol.RS485)
public class GrowattRS485Processor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(GrowattRS485Processor.class);
    @Inject
    private EnergyStorage energyStorage;

    @Override
    public void process() {
        for (final Port port : getPorts()) {
            if (!port.isOpen()) {
                try {
                    port.open();
                    LOG.debug("Opening port {} SUCCESSFUL", port);
                } catch (final Throwable e) {
                    LOG.error("Opening port {} FAILED!", port, e);
                }
            }

            if (port.isOpen()) {
                try {
                    final List<ByteBuffer> sendBuffers = collectBMSData();

                    for (final ByteBuffer frame : sendBuffers) {
                        LOG.debug("Frame send: {}", Port.printBuffer(frame));
                        port.sendFrame(frame);
                    }

                } catch (final Throwable e) {
                    LOG.error("Failed to send frame", e);
                }
            }
        }
    }


    private List<ByteBuffer> collectBMSData() {

        final List<ByteBuffer> frames = new ArrayList<>();
        final int slaveAddress = 11;
        final int function = 6;
        final int regHi = 0;
        final int regLow = 0;
        final int presetHi = 0;
        final int presetLow = 0;
        final int crc = 0;

        return frames;
    }

}
