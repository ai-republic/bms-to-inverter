package com.airepublic.bmstoinverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.bms.data.Alarm;
import com.airepublic.bmstoinverter.bms.data.BatteryPack;

import jakarta.enterprise.inject.Produces;

public class BatteryProducer {
    private final static Logger LOG = LoggerFactory.getLogger(BatteryProducer.class);
    private static BatteryPack[] batteryPacks;
    private static Alarm[] alarms;

    public void init() {
        if (!System.getProperties().containsKey("numBatteryPacks")) {
            LOG.error("No system property \"numBatteryPacks\" defined via -DnumBatteryPacks=<value> or in 'pi.properties'");
            return;
        }

        final int numBatteryPacks = Integer.parseInt(System.getProperty("numBatteryPacks"));

        batteryPacks = new BatteryPack[numBatteryPacks];
        alarms = new Alarm[numBatteryPacks];

        for (int i = 0; i < numBatteryPacks; i++) {
            batteryPacks[i] = new BatteryPack(i);
            alarms[i] = new Alarm();
        }
    }


    @Produces
    public BatteryPack[] getBatteryPacks() {
        if (batteryPacks == null) {
            init();
        }

        return batteryPacks;
    }


    @Produces
    public Alarm[] getAlarms() {
        if (alarms == null) {
            init();
        }
        return alarms;
    }

}
