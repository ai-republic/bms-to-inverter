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
package com.airepublic.bmstoinverter.core.plugin.inverter;

import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.InverterConfig;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * Enables adjusting of the {@link Inverter} settings after initialization and manipulation of send-
 * or receive-frames.
 */
public interface InverterPlugin {

    /**
     * Called before the initialization of the {@link Inverter} from the specified configuration.
     * 
     * @param inverter the {@link Inverter}
     * @param config the {@link InverterConfig} that can be changed before initialization
     */
    void onInitialize(Inverter inverter, final InverterConfig config);


    /**
     * Called before the the frame is sent to the {@link Inverter} and can be used to modify the
     * frame data.
     *
     * @param frame the frame data
     * @return the (optionally) modified frame data
     */
    ByteBuffer onSend(ByteBuffer frame);


    /**
     * Called after a frame is received from the {@link Inverter} and can be used to modify the
     * frame.
     *
     * @param frame the frame data
     * @return the (optionally) modified frame
     */
    ByteBuffer onReceive(ByteBuffer frame);


    /**
     * Called after the battery information has been aggregated but before creating send frames to
     * manipulate the aggregated battery information.
     *
     * @param aggregatedPack the aggregated {@link BatteryPack} information
     */
    void onBatteryAggregation(BatteryPack aggregatedPack);
}
