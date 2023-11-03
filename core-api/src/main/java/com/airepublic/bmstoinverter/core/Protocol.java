package com.airepublic.bmstoinverter.core;

import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.core.protocol.modbus.ModBusPort;
import com.airepublic.bmstoinverter.core.protocol.rs485.RS485Port;

public enum Protocol {
    CAN(CANPort.class),
    RS485(RS485Port.class),
    MODBUS(ModBusPort.class);

    final Class<? extends Port> portClass;

    Protocol(final Class<? extends Port> portClass) {
        this.portClass = portClass;
    }
}
