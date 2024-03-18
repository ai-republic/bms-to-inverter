package com.airepublic.bmstoinverter.bms.jk.rs485;

import java.nio.ByteBuffer;

public enum JkBmsR485DataIdEnum {
    READ_CELL_VOLTAGES((byte) 0x79, 0),
    READ_TUBE_TEMPERATURE((byte) 0x80, 2),
    READ_BOX_TEMPERATURE((byte) 0x81, 2),
    READ_BATTERY_TEMPERATURE((byte) 0x82, 2),
    READ_TOTAL_VOLTAGE((byte) 0x83, 2),
    READ_TOTAL_CURRENT((byte) 0x84, 0),
    READ_BATTERY_SOC((byte) 0x85, 1),
    READ_NUMBER_OF_TEMPERATURE_SENSORS((byte) 0x86, 1),
    READ_CYCLE_TIMES((byte) 0x87, 2),
    READ_TOTAL_CAPACITY((byte) 0x89, 4),
    READ_NUMBER_OF_BATTERY_STRINGS((byte) 0x8A, 2),
    READ_ALARMS((byte) 0x8B, 2),
    READ_BATTERY_STATUS((byte) 0x8C, 2),
    READ_BATTERY_OVER_VOLTAGE_LIMIT((byte) 0x8E, 2),
    READ_BATTERY_UNDER_VOLTAGE_LIMIT((byte) 0x8F, 2),
    READ_CELL_OVER_VOLTAGE_LIMIT((byte) 0x90, 2),
    READ_CELL_UNDER_VOLTAGE_LIMIT((byte) 0x93, 2),
    READ_DISCHARGE_CURRENT_LIMIT((byte) 0x97, 2),
    READ_CHARGE_CURRENT_LIMIT((byte) 0x99, 2),
    READ_RATED_CAPACITY((byte) 0xAA, 4),
    READ_BATTERY_TYPE((byte) 0xAF, 1);

    private final byte dataId;
    private final int length;

    JkBmsR485DataIdEnum(byte dataId, int length) {
        this.dataId = dataId;
        this.length = length;
    }

    public ByteBuffer getDataIs() {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put(dataId);
        buffer.flip();
        return buffer;
    }

    public int getlength() {
       return length;
    }

    public static JkBmsR485DataIdEnum fromDataId(byte dataId) {


            for (JkBmsR485DataIdEnum value : values()) {
                if (value.dataId == dataId) {
                    return value;
                }
            }
           return null;
    }
    public static JkBmsR485DataIdEnum fromDataId(ByteBuffer dataId) {
        if (dataId != null && dataId.remaining() >= 1) {
            byte dataIdBye = dataId.get();
            for (JkBmsR485DataIdEnum value : values()) {
                if (value.dataId == dataIdBye) {
                    return value;
                }
            }
            String hexCommand = String.format("%02X", dataIdBye);
            throw new IllegalArgumentException(hexCommand + " is an invalid DataId");
        }
        throw new IllegalArgumentException("DataId ByteBuffer is null or not enough data");
    }

    public static boolean dataId(byte dataId) {
        for (JkBmsR485DataIdEnum value : values()) {
            if (value.dataId == dataId) {
                return true;
            }
        }
        return false;
    }
}
