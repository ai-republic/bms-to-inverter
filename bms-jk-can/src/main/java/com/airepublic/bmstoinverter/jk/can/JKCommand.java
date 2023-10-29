package com.airepublic.bmstoinverter.jk.can;

public enum JKCommand {
    BATTERY_STATUS(0x02FA);

    public final int id;

    JKCommand(final int id) {
        this.id = id;
    }

}
