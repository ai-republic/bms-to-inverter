package com.airepublic.bmstoinverter.bms.byd.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * The class to handle CAN messages from a BYD {@link BMS}.
 */
public class BYDBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(BYDBmsCANProcessor.class);
    private final static int BATTERY_ID = 0;

    @Override
    public void collectData(final Port port) {
        try {
            final BatteryPack pack = getBatteryPack(BATTERY_ID);
            final ByteBuffer frame = port.receiveFrame();
            final int frameId = frame.getInt();
            final byte[] bytes = new byte[8];
            frame.get(bytes);
            final ByteBuffer data = ByteBuffer.wrap(bytes);

            switch (frameId) {
                case 0x351:
                    readChargeDischargeInfo(pack, data);
                break;
                case 0x355:
                    readSOC(pack, data);
                break;
                case 0x356:
                    readBatteryVoltage(pack, data);
                break;
                case 0x35A:
                    readAlarms(pack, data);
                break;
                case 0x35E:
                    readManufacturer(pack, data);
                break;
            }
        } catch (final IOException e) {
            LOG.error("Error receiving frame!", e);
        }
    }


    // 0x351
    private void readChargeDischargeInfo(final BatteryPack pack, final ByteBuffer data) {

        // Battery charge voltage (0.1V) - u_int_16
        pack.maxPackVoltageLimit = data.getChar();
        // Charge current limit (0.1A) - s_int_16
        pack.maxPackChargeCurrent = data.getShort();
        // Discharge current limit (0.1A) - s_int_16
        pack.maxPackDischargeCurrent = data.getShort();
        // Battery discharge voltage (0.1V) - u_int_16
        pack.minPackVoltageLimit = data.getChar();
    }


    // 0x355
    private void readSOC(final BatteryPack pack, final ByteBuffer data) {
        // SOC (1%) - u_int_16
        pack.packSOC = data.getChar() * 10;
        // SOH (1%) - u_int_16
        pack.packSOH = data.getChar() * 10;
    }


    // 0x356
    private void readBatteryVoltage(final BatteryPack pack, final ByteBuffer data) {
        // Battery voltage (0.01V) - u_int_16
        pack.packVoltage = (int) (data.getShort() / 10f);
        // Battery current (0.1A) - u_int_16
        pack.packCurrent = data.getShort();
        // Battery temperature (0.1C) - u_int_16
        pack.tempAverage = data.getShort();
    }


    // 0x35A
    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.NONE);

        // warnings
        BitSet bits = BitSet.valueOf(new byte[] { data.get(4) });

        if (bits.get(0)) {
            pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.WARNING);
        }

        if (bits.get(1)) {
            pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.NONE);
        }

        if (bits.get(2)) {
            pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, AlarmLevel.WARNING);
        }

        if (bits.get(3)) {
            pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, AlarmLevel.NONE);
        }

        if (bits.get(4)) {
            pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, AlarmLevel.WARNING);
        }

        if (bits.get(5)) {
            pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, AlarmLevel.NONE);
        }

        if (bits.get(6)) {
            pack.setAlarm(Alarm.PACK_TEMPERATURE_HIGH, AlarmLevel.WARNING);
        }

        if (bits.get(7)) {
            pack.setAlarm(Alarm.PACK_TEMPERATURE_HIGH, AlarmLevel.NONE);
        }

        bits = BitSet.valueOf(new byte[] { data.get(5) });

        if (bits.get(0)) {
            pack.setAlarm(Alarm.PACK_TEMPERATURE_LOW, AlarmLevel.WARNING);
        }

        if (bits.get(1)) {
            pack.setAlarm(Alarm.PACK_TEMPERATURE_LOW, AlarmLevel.NONE);
        }

        if (bits.get(2)) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, AlarmLevel.WARNING);
        }

        if (bits.get(3)) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, AlarmLevel.NONE);
        }

        if (bits.get(4)) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, AlarmLevel.WARNING);
        }

        if (bits.get(5)) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, AlarmLevel.NONE);
        }

        if (bits.get(6)) {
            pack.setAlarm(Alarm.PACK_CURRENT_HIGH, AlarmLevel.WARNING);
        }

        if (bits.get(7)) {
            pack.setAlarm(Alarm.PACK_CURRENT_HIGH, AlarmLevel.NONE);
        }

        bits = BitSet.valueOf(new byte[] { data.get(6) });

        if (bits.get(0)) {
            pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, AlarmLevel.WARNING);
        }

        if (bits.get(1)) {
            pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, AlarmLevel.NONE);
        }

        if (bits.get(4)) {
            pack.setAlarm(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, AlarmLevel.WARNING);
        }

        if (bits.get(5)) {
            pack.setAlarm(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, AlarmLevel.NONE);
        }

        if (bits.get(6) && pack.getAlarmLevel(Alarm.FAILURE_OTHER) != AlarmLevel.WARNING) {
            pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.WARNING);
        }

        if (bits.get(7)) {
            pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.NONE);
        }

        bits = BitSet.valueOf(new byte[] { data.get(7) });

        if (bits.get(0)) {
            pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, AlarmLevel.WARNING);
        }

        if (bits.get(1)) {
            pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, AlarmLevel.NONE);
        }

        // alarms
        bits = BitSet.valueOf(new byte[] { data.get(0) });

        if (bits.get(0)) {
            pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.ALARM);
        }

        if (bits.get(1)) {
            pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.NONE);
        }

        if (bits.get(2)) {
            pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, AlarmLevel.ALARM);
        }

        if (bits.get(3)) {
            pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, AlarmLevel.NONE);
        }

        if (bits.get(4)) {
            pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, AlarmLevel.ALARM);
        }

        if (bits.get(5)) {
            pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, AlarmLevel.NONE);
        }

        if (bits.get(6)) {
            pack.setAlarm(Alarm.PACK_TEMPERATURE_HIGH, AlarmLevel.ALARM);
        }

        if (bits.get(7)) {
            pack.setAlarm(Alarm.PACK_TEMPERATURE_HIGH, AlarmLevel.NONE);
        }

        bits = BitSet.valueOf(new byte[] { data.get(1) });

        if (bits.get(0)) {
            pack.setAlarm(Alarm.PACK_TEMPERATURE_LOW, AlarmLevel.ALARM);
        }

        if (bits.get(1)) {
            pack.setAlarm(Alarm.PACK_TEMPERATURE_LOW, AlarmLevel.NONE);
        }

        if (bits.get(2)) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, AlarmLevel.ALARM);
        }

        if (bits.get(3)) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, AlarmLevel.NONE);
        }

        if (bits.get(4)) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, AlarmLevel.ALARM);
        }

        if (bits.get(5)) {
            pack.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, AlarmLevel.NONE);
        }

        if (bits.get(6)) {
            pack.setAlarm(Alarm.PACK_CURRENT_HIGH, AlarmLevel.ALARM);
        }

        if (bits.get(7)) {
            pack.setAlarm(Alarm.PACK_CURRENT_HIGH, AlarmLevel.NONE);
        }

        bits = BitSet.valueOf(new byte[] { data.get(2) });

        if (bits.get(0)) {
            pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, AlarmLevel.ALARM);
        }

        if (bits.get(1)) {
            pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, AlarmLevel.NONE);
        }

        if (bits.get(4)) {
            pack.setAlarm(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, AlarmLevel.ALARM);
        }

        if (bits.get(5)) {
            pack.setAlarm(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, AlarmLevel.NONE);
        }

        if (bits.get(6) && pack.getAlarmLevel(Alarm.FAILURE_OTHER) != AlarmLevel.ALARM) {
            pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.ALARM);
        }

        if (bits.get(7)) {
            pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.NONE);
        }

        bits = BitSet.valueOf(new byte[] { data.get(3) });

        if (bits.get(0)) {
            pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, AlarmLevel.ALARM);
        }

        if (bits.get(1)) {
            pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, AlarmLevel.NONE);
        }

    }


    // 0x35E
    private void readManufacturer(final BatteryPack pack, final ByteBuffer data) {
        pack.manufacturerCode = "";
        byte chr;

        do {
            chr = data.get();

            if (chr != 0x00) {
                pack.manufacturerCode += (char) chr;
            }
        } while (chr != 0x00 && data.position() < data.capacity());

        LOG.debug("\nManufacturer\n{}", pack.manufacturerCode);

    }
}
