package com.airepublic.bmstoinverter.bms.daly.common;

import java.nio.ByteBuffer;

/**
 * The message that contains the frame data, bms address and {@link DalyCommand}.
 */
public class DalyMessage {
    public byte address;
    public DalyCommand cmd;
    public ByteBuffer data = ByteBuffer.allocate(8);

}
