/**
 * This software is free to use and to distribute in its unchanged form for private use.
 * Commercial use is prohibited without an explicit license agreement of the copyright holder.
 * Any changes to this software must be made solely in the project repository at https://github.com/ai-republic/bms-to-inverter.
 * The copyright holder is not liable for any damages in whatever form that may occur by using this software.
 *
 * (c) Copyright 2022 and onwards - Torsten Oltmanns
 *
 * @author Torsten Oltmanns - bms-to-inverter''AT''gmail.com
 */
package com.airepublic.bmstoinverter.bms.daly.common;

import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.core.Port;

/**
 * The message that contains the frame data, bms address and {@link DalyCommand}.
 */
public class DalyMessage {
    public byte bmsId;
    public DalyCommand cmd;
    public ByteBuffer data = ByteBuffer.allocate(8);

    @Override
    public String toString() {
        return "DalyMessage [address=" + bmsId + ", cmd=" + cmd + ", data=" + Port.printBuffer(data) + "]";
    }

}
