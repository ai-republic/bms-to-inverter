package com.airepublic.bmstoinverter.inverter.dummy;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.PortType;
import com.airepublic.bmstoinverter.core.Protocol;
import com.airepublic.bmstoinverter.core.protocol.can.CAN;

/**
 * The class to handle {@link CAN} messages for a SMA {@link Inverter}.
 */
@PortType(Protocol.CAN)
public class DummyInverterProcessor extends Inverter {

    @Override
    public void process() {
    }
}
