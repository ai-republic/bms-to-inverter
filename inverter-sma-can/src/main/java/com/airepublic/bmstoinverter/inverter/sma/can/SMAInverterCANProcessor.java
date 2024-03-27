package com.airepublic.bmstoinverter.inverter.sma.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The class to handle CAN messages for a SMA {@link Inverter}.
 */
@ApplicationScoped
public class SMAInverterCANProcessor extends Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(SMAInverterCANProcessor.class);

    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        final List<ByteBuffer> frames = new ArrayList<>();

        frames.add(createChargeDischargeInfo(aggregatedPack)); // 0x351
        frames.add(createSOC(aggregatedPack)); // 0x355
        frames.add(createBatteryVoltage(aggregatedPack)); // 0x356
        frames.add(createAlarms(aggregatedPack)); // 0x35A

        LOG.info("Sending SMA frame: Batt(V)={}, Batt(A)={}, SOC={}", aggregatedPack.packVoltage / 10f, aggregatedPack.packCurrent / 10f, aggregatedPack.packSOC / 10f);

        return frames;
    }


    @Override
    protected ByteBuffer readRequest(final Port port) throws IOException {
        return null;
    }


    @Override
    protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
        ((CANPort) port).sendExtendedFrame(frame);
    }


    // 0x351
    private ByteBuffer createChargeDischargeInfo(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x351);

        // Battery charge voltage (0.1V) - u_int_16
        frame.putChar((char) pack.maxPackVoltageLimit);
        // Charge current limit (0.1A) - s_int_16
        frame.putShort((short) pack.maxPackChargeCurrent);
        // Discharge current limit (0.1A) - s_int_16
        frame.putShort((short) pack.maxPackDischargeCurrent);
        // Battery discharge voltage (0.1V) - u_int_16
        frame.putChar((char) pack.minPackVoltageLimit);

        return frame;

    }


    // 0x355
    private ByteBuffer createSOC(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x355);

        // SOC (1%) - u_int_16
        frame.putChar((char) (pack.packSOC / 10));
        // SOH (1%) - u_int_16
        frame.putChar((char) (pack.packSOH / 10));

        return frame;
    }


    // 0x356
    private ByteBuffer createBatteryVoltage(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x356);

        // Battery voltage (0.01V) - u_int_16
        frame.putShort((short) (pack.packVoltage * 10));
        // Battery current (0.1A) - u_int_16
        frame.putShort((short) pack.packCurrent);
        // Battery temperature (0.1C) - u_int_16
        frame.putShort((short) pack.tempAverage);

        return frame;
    }


    // 0x35A
    private ByteBuffer createAlarms(final BatteryPack pack) {
        BitSet bits = new BitSet(8);
        final ByteBuffer frame = prepareFrame(0x359);

        // alarms
        bits.set(0, false);
        bits.set(1, false);

        bits.set(2, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        bits.set(3, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.NONE); // pack voltage
                                                                                     // to high
        bits.set(4, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.ALARM); // pack voltage
                                                                                     // to low
        bits.set(5, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.NONE);
        bits.set(6, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_HIGH) == AlarmLevel.ALARM);// pack
                                                                                         // temp to
                                                                                         // high
        bits.set(7, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_HIGH) == AlarmLevel.NONE);

        frame.put(bits.toByteArray()[0]);

        bits = new BitSet(8);
        bits.set(0, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_LOW) == AlarmLevel.ALARM); // pack
                                                                                         // temp to
                                                                                         // low
        bits.set(1, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_LOW) == AlarmLevel.NONE);
        bits.set(2, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);// charge
                                                                                           // temp
                                                                                           // to
                                                                                           // high
        bits.set(3, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.NONE);
        bits.set(4, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM); // charge
                                                                                           // temp
                                                                                           // to low
        bits.set(5, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.NONE);
        bits.set(6, pack.getAlarmLevel(Alarm.PACK_CURRENT_HIGH) == AlarmLevel.ALARM); // pack
                                                                                      // current to
                                                                                      // high
        bits.set(7, pack.getAlarmLevel(Alarm.PACK_CURRENT_HIGH) == AlarmLevel.NONE);

        frame.put(bits.toByteArray()[0]);

        bits = new BitSet(8);
        bits.set(0, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM); // charge
                                                                                        // current
                                                                                        // to high
        bits.set(1, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        bits.set(2, false); // contactor
        bits.set(3, false);
        bits.set(4, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.ALARM); // short
                                                                                                     // circuit
        bits.set(5, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.NONE);
        bits.set(6, false); // other bms internal error
        bits.set(7, false);

        frame.put(bits.toByteArray()[0]);

        bits = new BitSet(8);
        bits.set(0, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.ALARM); // cell
                                                                                                 // difference
                                                                                                 // to
        // high
        bits.set(1, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.NONE);
        frame.put(bits.toByteArray()[0]);

        // warnings
        bits = new BitSet(8);
        bits.set(0, false);
        bits.set(1, false);

        bits.set(2, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        bits.set(3, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.NONE); // pack voltage
                                                                                     // to high
        bits.set(4, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.ALARM); // pack voltage
                                                                                     // to low
        bits.set(5, pack.getAlarmLevel(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.NONE);
        bits.set(6, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_HIGH) == AlarmLevel.ALARM);// pack
                                                                                         // temp to
                                                                                         // high
        bits.set(7, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_HIGH) == AlarmLevel.NONE);

        frame.put(bits.toByteArray()[0]);

        bits = new BitSet(8);
        bits.set(0, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_LOW) == AlarmLevel.ALARM); // pack
                                                                                         // temp to
                                                                                         // low
        bits.set(1, pack.getAlarmLevel(Alarm.PACK_TEMPERATURE_LOW) == AlarmLevel.NONE);
        bits.set(2, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);// charge
                                                                                           // temp
                                                                                           // to
                                                                                           // high
        bits.set(3, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.NONE);
        bits.set(4, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM); // charge
                                                                                           // temp
                                                                                           // to low
        bits.set(5, pack.getAlarmLevel(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.NONE);
        bits.set(6, pack.getAlarmLevel(Alarm.PACK_CURRENT_HIGH) == AlarmLevel.ALARM); // pack
                                                                                      // current to
                                                                                      // high
        bits.set(7, pack.getAlarmLevel(Alarm.PACK_CURRENT_HIGH) == AlarmLevel.NONE);

        frame.put(bits.toByteArray()[0]);

        bits = new BitSet(8);
        bits.set(0, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM); // charge
                                                                                        // current
                                                                                        // to high
        bits.set(1, pack.getAlarmLevel(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        bits.set(2, false); // contactor
        bits.set(3, false);
        bits.set(4, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.ALARM); // short
                                                                                                     // circuit
        bits.set(5, pack.getAlarmLevel(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.NONE);
        bits.set(6, false); // other bms internal error
        bits.set(7, false);

        frame.put(bits.toByteArray()[0]);

        bits = new BitSet(8);
        bits.set(0, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.ALARM); // cell
                                                                                                 // difference
                                                                                                 // to
        // high
        bits.set(1, pack.getAlarmLevel(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH) == AlarmLevel.NONE);
        frame.put(bits.toByteArray()[0]);
        return frame;
    }


    private ByteBuffer prepareFrame(final int cmd) {
        final ByteBuffer frame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);
        frame.putInt(cmd)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        return frame;
    }

}
