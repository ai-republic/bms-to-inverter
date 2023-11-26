package com.airepublic.bmstoinverter.inverter.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.PortType;
import com.airepublic.bmstoinverter.core.Protocol;
import com.airepublic.bmstoinverter.core.protocol.can.CAN;

/**
 * The class to handle {@link CAN} messages for a SMA {@link Inverter}.
 */
@PortType(Protocol.CAN)
public class DummyInverterProcessor extends Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(DummyInverterProcessor.class);

    @Override
    public void process(final Runnable callback) {
        try {
            callback.run();
        } catch (final Exception e) {
            LOG.error("Inverter process callback threw an exception!", e);
        }
    }
}
