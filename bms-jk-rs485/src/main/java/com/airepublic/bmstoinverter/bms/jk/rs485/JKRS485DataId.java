package com.airepublic.bmstoinverter.bms.jk.rs485;

/**
 * The enumeration of all JK command data identifiers mapped also to their length of their data
 * segment.
 */
public enum JKRS485DataId {
    READ_CELL_VOLTAGES((byte) 0x79, 0),
    READ_TUBE_TEMPERATURE((byte) 0x80, 2),
    READ_BOX_TEMPERATURE((byte) 0x81, 2),
    READ_BATTERY_TEMPERATURE((byte) 0x82, 2),
    READ_TOTAL_VOLTAGE((byte) 0x83, 2),
    READ_TOTAL_CURRENT((byte) 0x84, 2),
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
    X1((byte) 0x9A, 2),
    X2((byte) 0x9B, 2),
    X3((byte) 0x9C, 2),
    X4((byte) 0x9D, 1),
    X5((byte) 0x9E, 2),
    X6((byte) 0x9F, 2),
    X7((byte) 0xA0, 2),
    X8((byte) 0xA1, 2),
    X9((byte) 0xA2, 2),
    X10((byte) 0xA3, 2),
    X11((byte) 0xA4, 2),
    X12((byte) 0xA5, 2),
    X13((byte) 0xA6, 2),
    X14((byte) 0xA7, 2),
    X15((byte) 0xA8, 2),
    X16((byte) 0xA9, 1),
    READ_RATED_CAPACITY((byte) 0xAA, 4),
    X17((byte) 0xAB, 1),
    X18((byte) 0xAC, 1),
    X19((byte) 0xAD, 2),
    X20((byte) 0xAE, 1),
    READ_BATTERY_TYPE((byte) 0xAF, 1),
    X21((byte) 0xB0, 2),
    X22((byte) 0xB1, 1),
    X23((byte) 0xB2, 10),
    X24((byte) 0xB3, 1),
    X25((byte) 0xB4, 8),
    X26((byte) 0xB5, 4),
    X27((byte) 0xB6, 4),
    READ_SOFTWARE_VERSION((byte) 0xB7, 15),
    X29((byte) 0xB8, 2),
    X30((byte) 0xB9, 4),
    READ_MANUFACTURER((byte) 0xBA, 24),
    X33((byte) 0xC0, 5),
    END_FLAG((byte) 0x68, 4);

    private final byte dataId;
    private final int length;

    /**
     * Constructor.
     *
     * @param dataId the data id byte
     * @param length the length of the data segment
     */
    JKRS485DataId(final byte dataId, final int length) {
        this.dataId = dataId;
        this.length = length;
    }


    /**
     * Gets the length of the data segment for this data id.
     *
     * @return the length of the data segment
     */
    public int getLength() {
        return length;
    }


    /**
     * Gets the {@link JKRS485DataId} for the specified data id.
     *
     * @param dataId the byte resembling a valid data id
     * @return the {@link JKRS485DataId}
     */
    public static JKRS485DataId fromDataId(final byte dataId) {

        for (final JKRS485DataId value : values()) {
            if (value.dataId == dataId) {
                return value;
            }
        }
        return null;
    }


    /**
     * Checks if the provided byte resembles a valid data id.
     *
     * @param dataId the byte to check
     * @return true if the provided byte resembles a data id, otherwise false
     */
    public static boolean isDataId(final byte dataId) {
        for (final JKRS485DataId value : values()) {
            if (value.dataId == dataId) {
                return true;
            }
        }
        return false;
    }
}
