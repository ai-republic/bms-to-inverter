package com.airepublic.bmstoinverter.sma.can;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.Inverter;
import com.airepublic.bmstoinverter.Port;
import com.airepublic.bmstoinverter.PortProcessor;
import com.airepublic.bmstoinverter.Portname;
import com.airepublic.bmstoinverter.bms.data.Alarm;
import com.airepublic.bmstoinverter.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.can.CAN;

import jakarta.inject.Inject;

@Inverter
public class SmaCANProcessor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(SmaCANProcessor.class);
    @Inject
    @CAN
    @Portname("sma.can.portname")
    private Port canPort;
    @Inject
    private BatteryPack[] batteryPack;
    @Inject
    private Alarm[] alarms;
    private final Map<Integer, ByteBuffer> canData = new HashMap<>();

    public SmaCANProcessor() {
    }


    @Override
    public void process() {
        if (!canPort.isOpen()) {
            try {
                canPort.open();
                LOG.debug("Opening CAN port SUCCESSFUL");
            } catch (final Throwable e) {
                LOG.error("Opening CAN port FAILED!", e);
            }
        }

        if (canPort.isOpen()) {
            try {
                updateCANMessages(getSMAData());

                for (final ByteBuffer frame : canData.values()) {
                    LOG.debug("CAN send: {}", Port.printBuffer(frame));
                    canPort.sendFrame(frame);
                }

            } catch (final Throwable e) {
                LOG.error("Failed to send CAN frame", e);
            }
        }
    }


    private SMAData getSMAData() {
        final SMAData data = new SMAData();

        data.chargeVoltageSetpoint = 576; // 57.6V
        data.dcChargeCurrentLimit = 1000; // 100A
        data.dcDischargeCurrentLimit = 1000; // 100A
        data.dischargeVoltageLimit = 480; // 48V

        Optional<BatteryPack> opt = Stream.of(batteryPack).filter(b -> b.packSOC != 0).min((o1, o2) -> ((Integer) o1.packSOC).compareTo(o2.packSOC));
        if (opt.isPresent()) {
            data.soc = (char) (opt.get().packSOC / 10); // 100%
        } else {
            data.soc = (char) 50;
        }

        data.soh = 100; // 100%

        opt = Stream.of(batteryPack).filter(b -> b.packVoltage != 0).min((o1, o2) -> ((Integer) o1.packVoltage).compareTo(o2.packVoltage));

        if (opt.isPresent()) {
            data.batteryVoltage = (short) (opt.get().packVoltage * 10);
        } else {
            data.batteryVoltage = 5200;
        }

        data.batteryCurrent = (short) Stream.of(batteryPack).mapToInt(b -> b.packCurrent).sum();
        data.batteryTemperature = 350; // 35degC

        LOG.info("Sending SMA frame: Batt(V)={}, Batt(A)={}, SOC={}", data.batteryVoltage / 100f, data.batteryCurrent / 10f, (int) data.soc);
        return data;
    }


    private void updateCANMessages(final SMAData data) {
        final byte length = (byte) 8;
        // 0x0351 charge voltage, charge amp limit, discharge amp limit, discharge voltage limit
        ByteBuffer frame = getCANData(0x0351);

        frame.putInt(0x0351)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes
        frame.asCharBuffer().put(data.chargeVoltageSetpoint); // charge voltage setpoint (0.1V) -
        // u_int_16
        frame.asShortBuffer().put(data.dcChargeCurrentLimit); // max charge amps (0.1A) - s_int_16
        frame.asShortBuffer().put(data.dcDischargeCurrentLimit); // max discharge amps (0.1A) -
                                                                 // s_int_16
        frame.asCharBuffer().put(data.dischargeVoltageLimit); // max discharge voltage (0.1V) -
                                                              // u_int_16

        // 0x0355 SOC, SOH, HiRes SOC
        frame = getCANData(0x0355);
        frame.putInt(0x0355)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes
        frame.asCharBuffer().put(data.soc) // SOC (1%) - u_int_16
                .put(data.soh); // SOH (1%) - u_int_16
        // frame.asShortBuffer().put((short) 50); // HiRes SOC (0.01%) - u_int_16

        // 0x0356 battery voltage, battery current, battery temperature
        frame = getCANData(0x0356);
        frame.putInt(0x0356)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0) // skip 2 bytes
                .putShort(data.batteryVoltage) // battery voltage (0.01V) - s_int_16
                .putShort(data.batteryCurrent) // battery current (0.1A) - s_int_16
                .putShort(data.batteryTemperature); // battery temperature (0.1C) -
                                                    // s_int_16

        // 0x035A alarms and warnings
        frame = getCANData(0x035A);
        frame.putInt(0x035A)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0)
                // general arrive/leave, high voltage arrive/leave, low
                // voltage arrive/leave, high temperature arrive/leave, low temperature
                // arrive/leave, high temp charge
                // arrive/leave, low temp charge arrive/leave, high current arrive/leave
                // high current charge arrive/leave, contactor arrive/leave, short circuit
                // arrive/leave, BMS internal arrive/leave
                // cell imbalance arrive/leave, last 6 bits reserved
                .putInt(data.alarms)
                .putInt(data.warnings);
    }


    private ByteBuffer getCANData(final int id) {
        ByteBuffer frame = canData.get(id);

        if (frame == null) {
            frame = ByteBuffer.allocateDirect(16);
            frame.order(ByteOrder.LITTLE_ENDIAN);
            frame.putInt(id);
            canData.put(id, frame);
        }

        frame.rewind();

        return frame;
    }

}
