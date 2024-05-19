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
package com.airepublic.bmstoinverter.core.protocol.rs485;

/**
 * Defines a part of a communication protocol frame.
 */
public class FrameDefinitionPart {
    private final FrameDefinitionPartType type;
    private int byteCount;
    private int valueAdjustment = 0;

    public FrameDefinitionPart(final FrameDefinitionPartType type, final int byteCount) {
        this.type = type;
        this.byteCount = byteCount;
    }


    /**
     * Gets the {@link FrameDefinitionPartType} associated with this part.
     *
     * @return the {@link FrameDefinitionPartType} associated with this part
     */
    public FrameDefinitionPartType getType() {
        return type;
    }


    /**
     * Gets the number of bytes associated with this part.
     *
     * @return the number of bytes associated with this part
     */
    public int getByteCount() {
        return byteCount;
    }


    /**
     * Sets the number of bytes associated with this part.
     * 
     * @param byteCount the number of bytes associated with this part
     * @throws IllegalArgumentException if the byte count is less than 1
     */
    void setByteCount(final int byteCount) {
        if (byteCount < 1) {
            throw new IllegalArgumentException("Frame definition part byte length must be greater 0");
        }

        this.byteCount = byteCount;
    }


    /**
     * Gets the value adjustment value to modify the value when parsed.
     *
     * @return the valueAdjustment the value adjustment
     */
    public int getValueAdjustment() {
        return valueAdjustment;
    }


    /**
     * Sets the value adjustment value to modify the value when parsed
     *
     * @param valueAdjustment the value adjustment
     */
    public void setValueAdjustment(final int valueAdjustment) {
        this.valueAdjustment = valueAdjustment;
    }

}
