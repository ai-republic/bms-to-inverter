package com.airepublic.bmstoinverter.growatt.can;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
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

    public GrowattCANProcessor() {
    }


    @Override
    public Port getPort() {
        return port;
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
                final List<ByteBuffer> canData = updateCANMessages(getGrowattData());

                for (final ByteBuffer frame : canData) {
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
        data.batteryTemperature = (short) (Stream.of(energyStorage.getBatteryPacks()).mapToInt(b -> b.tempAverage).average().orElseGet(() -> 35d) * 10); // 35degC

        LOG.info("Sending Growatt frame: Batt(V)={}, Batt(A)={}, SOC={}", data.batteryVoltage / 100f, data.batteryCurrent / 10f, (int) data.soc);
        return data;
    }


    private List<ByteBuffer> updateCANMessages(final GrowattData data) {
        final List<ByteBuffer> frames = new ArrayList<>();
        final byte length = (byte) 8;
        // 0x0351 charge voltage, charge amp limit, discharge amp limit, discharge voltage limit
        ByteBuffer frame = ByteBuffer.allocate(16);

        frame.putInt(0x0311)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes
        // charge voltage setpoint (0.1V) - u_int_16
        frame.asCharBuffer().put((char) energyStorage.getBatteryPack(0).maxPackVoltageLimit);
        // max charge amps (0.1A) - u_int_16
        frame.asCharBuffer().put((char) energyStorage.getBatteryPack(0).maxPackChargeCurrent);
        // max discharge amps 0.1A)- u_int_16
        frame.asCharBuffer().put((char) energyStorage.getBatteryPack(0).maxPackDischargeCurrent);
        // status bits (see documentation)
        frame.put(get311Status());
        frames.add(frame);

        for (int battery = 0; battery < energyStorage.getBatteryPackCount(); battery++) {
            // 0x312
            frame = ByteBuffer.allocate(16);
            frame.putInt(0x0312)
                    .put(length)
                    .put((byte) 0) // flags
                    .putShort((short) 0); // skip 2 bytes

            // error and warning bits
            frame.put(getErrorBits(battery));
            frame.put((byte) (battery + 1));
            frame.putChar((char) 0); // skip 2 manufacturer codes
            frame.put((byte) energyStorage.getBatteryPack(battery).numberOfCells);
            frames.add(frame);
        }

        return frames;
    }


    private byte[] get311Status() {
        final BitSet bits = new BitSet(12);
        // charging status
        final boolean charging = Stream.of(energyStorage.getBatteryPacks()).anyMatch(pack -> pack.chargeMOSState == true);
        bits.set(0, charging);
        bits.set(1, false);

        // error bit flag
        bits.set(2, false);

        // balancing status
        bits.set(3, Stream.of(energyStorage.getBatteryPacks()).anyMatch(pack -> pack.cellBalanceActive == true));

        // sleep status
        bits.set(4, false);

        // output discharge status
        bits.set(5, false);

        // output charge status
        bits.set(6, false);

        // battery terminal status
        bits.set(7, false);

        // master box operation mode 00-standalone, 01-parallel, 10-parallel ready
        bits.set(8, false);
        bits.set(9, false);

        // SP status 00-none, 01-standby, 10-charging, 11-discharging
        bits.set(10, true);
        bits.set(11, !charging);

        return bits.toByteArray();
    }


    private byte[] getErrorBits(final int battery) {
        final BitSet bits = new BitSet(32);
        bits.set(0, energyStorage.getBatteryPack(battery).alarms.levelTwoDischargeCurrentTooHigh.value);

        return bits.toByteArray();
    }

}
