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
package com.airepublic.bmstoinverter.core.plugin.bms;

import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.BMSConfig;

/**
 * Enables adjusting of the {@link BMS} settings after initialization and manipulation of send- or
 * receive-frames.
 */
public interface BmsPlugin {

    /**
     * Called before the initialization of the {@link BMS} from the specified configuration.
     * 
     * @param config the {@link BMSConfig} that can be changed before initialization
     */
    void onInitialize(final BMSConfig config);


    /**
     * Called before the the frame is sent to the {@link BMS} and can be used to modify the frame
     * data.
     *
     * @param frame the frame data
     * @return the (optionally) modified frame data
     */
    ByteBuffer onSend(ByteBuffer frame);


    /**
     * Called after a frame is received from the {@link BMS} and can be used to modify the frame.
     *
     * @param frame the frame data
     * @return the (optionally) modified frame
     */
    ByteBuffer onReceive(ByteBuffer frame);
}
