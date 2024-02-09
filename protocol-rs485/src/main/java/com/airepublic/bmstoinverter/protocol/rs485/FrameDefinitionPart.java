package com.airepublic.bmstoinverter.protocol.rs485;

public class FrameDefinitionPart {
    private final FrameDefinitionPartType type;
    private int byteCount;

    public FrameDefinitionPart(final FrameDefinitionPartType type, final int byteCount) {
        this.type = type;
        this.byteCount = byteCount;
    }


    /**
     * @return the type
     */
    public FrameDefinitionPartType getType() {
        return type;
    }


    /**
     * @return the byteCount
     */
    public int getByteCount() {
        return byteCount;
    }


    void setByteCount(final int byteCount) {
        this.byteCount = byteCount;
    }

}
