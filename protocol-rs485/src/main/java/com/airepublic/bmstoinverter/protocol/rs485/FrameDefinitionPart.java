package com.airepublic.bmstoinverter.protocol.rs485;

/**
 * Defines a part of a communication protocol frame.
 */
public class FrameDefinitionPart {
    private final FrameDefinitionPartType type;
    private int byteCount;

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

}
