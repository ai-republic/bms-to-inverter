package com.airepublic.bmstoinverter.daly.can;

import java.nio.ByteBuffer;

public class DalyMessage {
    public byte address;
    public byte dataId;
    public ByteBuffer data = ByteBuffer.allocate(8);

}
