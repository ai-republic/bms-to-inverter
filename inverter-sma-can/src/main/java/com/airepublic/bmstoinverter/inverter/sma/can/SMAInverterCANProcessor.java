package com.airepublic.bmstoinverter.inverter.sma.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The class to handle CAN messages for a SMA {@link Inverter}.
 */
@ApplicationScoped
public class SMAInverterCANProcessor extends Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(SMAInverterCANProcessor.class);
    @Inject
    private EnergyStorage energyStorage;

    @Override
    protected List<ByteBuffer> createSendFrames() {
        final List<ByteBuffer> frames = new ArrayList<>();
        final byte length = (byte) 8;
        // 0x0351 charge voltage, charge amp limit, discharge amp limit, discharge voltage limit
        ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.order(ByteOrder.LITTLE_ENDIAN);

        frame.putInt(0x0351)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes
        // charge voltage setpoint (0.1V) - u_int_16
        frame.asCharBuffer().put((char) energyStorage.getBatteryPack(0).maxPackVoltageLimit);
        // max charge amps (0.1A) - s_int_16
        frame.asShortBuffer().put((short) energyStorage.getBatteryPack(0).maxPackChargeCurrent);
        // max discharge amps (0.1A) - s_int_16
        frame.asShortBuffer().put((short) energyStorage.getBatteryPack(0).maxPackDischargeCurrent);
        // max discharge voltage (0.1V) - u_int_16
        frame.asCharBuffer().put((char) energyStorage.getBatteryPack(0).minPackVoltageLimit);
        frames.add(frame);

        // 0x0355 SOC, SOH, HiRes SOC
        frame = ByteBuffer.allocateDirect(16);
        frame.order(ByteOrder.LITTLE_ENDIAN);

        frame.putInt(0x0355)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // get the minimim SOC of all the packs
        final int soc = energyStorage.getBatteryPacks().stream().map(pack -> pack.packSOC).filter(packSoc -> packSoc != 0).min((s1, s2) -> s1.compareTo(s2)).orElse(50);
        // SOC (1%) - u_int_16
        frame.asCharBuffer().put((char) soc)
                // SOH (1%) - u_int_16
                .put((char) 80);
        // frame.asShortBuffer().put((short) 50); // HiRes SOC (0.01%) - u_int_16
        frames.add(frame);

        // 0x0356 battery voltage, battery current, battery temperature
        frame = ByteBuffer.allocateDirect(16);
        frame.order(ByteOrder.LITTLE_ENDIAN);

        frame.putInt(0x0356)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        final short batteryVoltage = (short) (energyStorage.getBatteryPacks().stream().map(pack -> pack.packVoltage).filter(volt -> volt != 0).min((v1, v2) -> v1.compareTo(v2)).orElse(520) * 10);
        final short batteryCurrent = (short) energyStorage.getBatteryPacks().stream().mapToInt(b -> b.packCurrent).sum();
        final short batteryTemperature = (short) (energyStorage.getBatteryPacks().stream().mapToInt(b -> b.tempAverage).average().orElseGet(() -> 35d) * 10); // 35degC

        // battery voltage (0.01V) - s_int_16
        frame.putShort(batteryVoltage)
                // battery current (0.1A) - s_int_16
                .putShort(batteryCurrent)
                // battery temperature (0.1C) - s_int_16
                .putShort(batteryTemperature);
        frames.add(frame);

        // 0x035A alarms and warnings
        frame = ByteBuffer.allocateDirect(16);
        frame.order(ByteOrder.LITTLE_ENDIAN);

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
                .putInt(0) // alarms
                .putInt(0); // warnings
        frames.add(frame);

        LOG.info("Sending SMA frame: Batt(V)={}, Batt(A)={}, SOC={}", batteryVoltage / 100f, batteryCurrent / 10f, soc);

        return frames;
    }


    @Override
    protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
        ((CANPort) port).sendExtendedFrame(frame);
    }

}
