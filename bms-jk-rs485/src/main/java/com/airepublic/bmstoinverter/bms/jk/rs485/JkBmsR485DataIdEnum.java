package com.airepublic.bmstoinverter.bms.jk.rs485;

import java.nio.ByteBuffer;

public enum JkBmsR485DataIdEnum {
    READ_CELL_VOLTAGES((byte) 0x79),
    READ_TUBE_TEMPERATURE((byte) 0x80),
    READ_BOX_TEMPERATURE((byte) 0x81),
    READ_BATTERY_TEMPERATURE((byte) 0x82),
    READ_TOTAL_VOLTAGE((byte) 0x83),
    READ_TOTAL_CURRENT((byte) 0x84),
    READ_BATTERY_SOC((byte) 0x85),
    READ_NUMBER_OF_TEMPERATURE_SENSORS((byte) 0x86),
    READ_CYCLE_TIMES((byte) 0x87),
    READ_TOTAL_CAPACITY((byte) 0x89),
    READ_NUMBER_OF_BATTERY_STRINGS((byte) 0x8A),
    READ_ALARMS((byte) 0x8B),
    READ_BATTERY_STATUS((byte) 0x8C),
    READ_BATTERY_OVER_VOLTAGE_LIMIT((byte) 0x8E),
    READ_BATTERY_UNDER_VOLTAGE_LIMIT((byte) 0x8F),
    READ_CELL_OVER_VOLTAGE_LIMIT((byte) 0x90),
    READ_CELL_UNDER_VOLTAGE_LIMIT((byte) 0x93),
    READ_DISCHARGE_CURRENT_LIMIT((byte) 0x97),
    READ_CHARGE_CURRENT_LIMIT((byte) 0x99),
    READ_RATED_CAPACITY((byte) 0xAA),
    READ_BATTERY_TYPE((byte) 0xAF);

    private final byte dataId;

    JkBmsR485DataIdEnum(byte dataId) {
        this.dataId = dataId;
    }

    public ByteBuffer getCommandWord() {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put(dataId);
        buffer.flip();
        return buffer;
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
