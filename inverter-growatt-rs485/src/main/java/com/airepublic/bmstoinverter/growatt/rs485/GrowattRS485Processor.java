package com.airepublic.bmstoinverter.growatt.rs485;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortProcessor;
import com.airepublic.bmstoinverter.core.Portname;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.modbus.ModBus;

import jakarta.inject.Inject;

@Inverter
public class GrowattRS485Processor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(GrowattRS485Processor.class);
    @Inject
    @ModBus
    @Portname("inverter.portname")
    private Port port;
    @Inject
    private EnergyStorage energyStorage;

    public GrowattRS485Processor() {
    }


    @Override
    public Port getPort() {
        return port;
    }


    @Override
    public void process() {
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
