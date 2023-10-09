package com.airepublic.bmstoinverter.growatt.can;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortProcessor;
import com.airepublic.bmstoinverter.core.Portname;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CAN;

import jakarta.inject.Inject;

@Inverter
public class GrowattCANProcessor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(GrowattCANProcessor.class);
    @Inject
    @CAN
    @Portname("inverter.portname")
    private Port port;
    @Inject
    private EnergyStorage energyStorage;
    private final Map<Integer, ByteBuffer> canData = new HashMap<>();

    public GrowattCANProcessor() {
    }


    @Override
    public void process() {
        if (!port.isOpen()) {
            try {
                port.open();
                LOG.debug("Opening port {} SUCCESSFUL", port);
            } catch (final Throwable e) {
                LOG.error("Opening port {} FAILED!", port, e);
            }
        }

        if (port.isOpen()) {
            try {
                updateCANMessages(getGrowattData());

                for (final ByteBuffer frame : canData.values()) {
                    LOG.debug("CAN send: {}", Port.printBuffer(frame));
                    port.sendFrame(frame);
                }

            } catch (final Throwable e) {
                LOG.error("Failed to send CAN frame", e);
            }
        }
    }


    private GrowattData getGrowattData() {
        final GrowattData data = new GrowattData();

        data.chargeVoltageSetpoint = (char) energyStorage.getBatteryPack(0).maxPackVoltageLimit; // 57.6V
        data.dcChargeCurrentLimit = (short) energyStorage.getBatteryPack(0).maxPackChargeCurrent; // 100A
        data.dcDischargeCurrentLimit = (short) energyStorage.getBatteryPack(0).maxPackDischargeCurrent; // 100A
        data.dischargeVoltageLimit = (char) energyStorage.getBatteryPack(0).minPackVoltageLimit; // 48V

        Optional<BatteryPack> opt = Stream.of(energyStorage.getBatteryPacks()).filter(b -> b.packSOC != 0).min((o1, o2) -> ((Integer) o1.packSOC).compareTo(o2.packSOC));
        if (opt.isPresent()) {
            data.soc = (char) (opt.get().packSOC / 10); // 100%
        } else {
            data.soc = (char) 50;
        }

        data.soh = 100; // 100%

        opt = Stream.of(energyStorage.getBatteryPacks()).filter(b -> b.packVoltage != 0).min((o1, o2) -> ((Integer) o1.packVoltage).compareTo(o2.packVoltage));

        if (opt.isPresent()) {
            data.batteryVoltage = (short) (opt.get().packVoltage * 10);
        } else {
            data.batteryVoltage = 5200;
        }

        data.batteryCurrent = (short) Stream.of(energyStorage.getBatteryPacks()).mapToInt(b -> b.packCurrent).sum();
        data.batteryTemperature = 350; // 35degC

        LOG.info("Sending SMA frame: Batt(V)={}, Batt(A)={}, SOC={}", data.batteryVoltage / 100f, data.batteryCurrent / 10f, (int) data.soc);
        return data;
    }


    private void updateCANMessages(final GrowattData data) {
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
