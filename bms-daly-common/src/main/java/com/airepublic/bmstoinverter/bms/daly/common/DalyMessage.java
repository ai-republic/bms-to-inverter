package com.airepublic.bmstoinverter.bms.daly.common;

import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.core.Port;

/**
 * The message that contains the frame data, bms address and {@link DalyCommand}.
 */
public class DalyMessage {
    public byte address;
    public DalyCommand cmd;
    public ByteBuffer data = ByteBuffer.allocate(8);

    @Override
    public String toString() {
        return "DalyMessage [address=" + address + ", cmd=" + cmd + ", data=" + Port.printBuffer(data) + "]";
    }

}
