package com.airepublic.bmstoinverter.growatt.can;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
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

/**
 * The {@link PortProcessor} to handle CAN messages for a Growatt low voltage (12V/24V/48V)
 * inverter.
 */
@Inverter
public class GrowattCANProcessor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(GrowattCANProcessor.class);
    @Inject
    @CAN
    @Portname("inverter.portname")
    private Port port;
    @Inject
    private EnergyStorage energyStorage;

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
                final List<ByteBuffer> canData = updateCANMessages();

                for (final ByteBuffer frame : canData) {
                    LOG.debug("CAN send: {}", Port.printBuffer(frame));
                    port.sendFrame(frame);
                }

            } catch (final Throwable e) {
                LOG.error("Failed to send CAN frame", e);
            }
        }
    }


    private List<ByteBuffer> updateCANMessages() {
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

        // 0x312
        for (int battery = 0; battery < energyStorage.getBatteryPackCount(); battery++) {
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

        // 0x313
        frame = ByteBuffer.allocate(16);
        frame.putInt(0x0313)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes
        // total voltage average (0.01V) - s_int_16 default 50.00V
        frame.putShort((short) (Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.packVoltage).average().orElse(500) * 10));
        // total current (0.1A) - s_int_16
        frame.putShort((short) Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.packCurrent).sum());
        // maximum temperature (0.1C) - s_int_16 default 30.0C
        frame.putShort((short) (Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.tempMax).max().orElse(30) * 10));
        // SOC average (1%) - u_int_8 default 50%
        frame.put((byte) (int) (Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.packSOC).average().orElse(500) % 10));
        // SOH bits 0-6 SOH counters, bit 7 SOH flag
        frame.put((byte) 0);
        frames.add(frame);

        // 0x314
        frame = ByteBuffer.allocate(16);
        frame.putInt(0x0314)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes
        // current capacity (10mAh)
        frame.putShort((short) (Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.remainingCapacitymAh).sum() % 10));
        // full capacity (10mAh)
        frame.putShort((short) (Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.ratedCapacitymAh).sum() % 10));
        // maximum cell difference (1mV)
        frame.putShort((short) Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.cellDiffmV).max().orElse(0));
        // maximum cycles
        frame.putShort((short) Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.bmsCycles).max().orElse(0));
        frames.add(frame);

        // 0x319
        frame = ByteBuffer.allocate(16);
        frame.putInt(0x0319)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes
        // charge state and battery status
        frame.put(getChargeStates());

        // find max and min cell voltages and their index
        int idx = 0;
        int minmV = 0;
        int minIdx = 0;
        int maxmV = 0;
        int maxIdx = 0;

        for (final BatteryPack pack : energyStorage.getBatteryPacks()) {
            for (int i = 0; i < pack.numberOfCells; i++) {
                idx++;

                if (pack.cellVmV[i] < minmV) {
                    minmV = pack.cellVmV[i];
                    minIdx = idx;
                } else if (pack.cellVmV[i] > maxmV) {
                    maxmV = pack.cellVmV[i];
                    maxIdx = idx;
                }
            }
        }
        // maximum cell voltage of all cells
        frame.putChar((char) maxmV);
        // minimum cell voltage of all cells
        frame.putChar((char) minmV);
        // index of cell with maximum cell voltage of all cells
        frame.put((byte) maxIdx);
        // index of cell with minimum cell voltage of all cells
        frame.put((byte) minIdx);
        // pack id of faulty battery
        frame.put((byte) 0);
        frames.add(frame);

        // 0x320
        frame = ByteBuffer.allocate(16);
        frame.putInt(0x0320)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes
        // manufacturer
        frame.put((byte) 0x00);
        frame.put((byte) 0x01);
        // hardware version
        frame.put((byte) 1);
        // software version
        frame.put((byte) 1);
        final byte[] dateTime = getDateTimeBits();
        frame.put(dateTime[0]);
        frame.put(dateTime[1]);
        frame.put(dateTime[2]);
        frame.put(dateTime[3]);
        frames.add(frame);

        // 0x321
        frame = ByteBuffer.allocate(16);
        frame.putInt(0x0321)
                .put(length)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes
        // update status
        frame.put((byte) 0);
        // update schedule
        frame.put((byte) 0);
        // progress programming Id of pack upgrade
        frame.put((byte) 0);
        // update successful count
        frame.put((byte) 0);
        frame.putInt((int) 0L);

        frames.add(frame);
        return frames;
    }


    private boolean bitRead(final int value, final int index) {
        return (value >> index & 1) == 1;
    }


    private byte[] getDateTimeBits() {
        final BitSet bits = new BitSet(32);
        final LocalDateTime time = LocalDateTime.now();
        // seconds
        int value = time.getSecond();
        bits.set(0, bitRead(value, 0));
        bits.set(1, bitRead(value, 1));
        bits.set(2, bitRead(value, 2));
        bits.set(3, bitRead(value, 3));
        bits.set(4, bitRead(value, 4));
        bits.set(5, bitRead(value, 5));

        // minutes
        value = time.getMinute();
        bits.set(6, bitRead(value, 0));
        bits.set(7, bitRead(value, 1));
        bits.set(8, bitRead(value, 2));
        bits.set(9, bitRead(value, 3));
        bits.set(10, bitRead(value, 4));
        bits.set(11, bitRead(value, 5));

        // hours
        value = time.getHour();
        bits.set(12, bitRead(value, 0));
        bits.set(13, bitRead(value, 1));
        bits.set(14, bitRead(value, 2));
        bits.set(15, bitRead(value, 3));
        bits.set(16, bitRead(value, 4));

        // day
        value = time.getDayOfMonth();
        bits.set(17, bitRead(value, 0));
        bits.set(18, bitRead(value, 1));
        bits.set(19, bitRead(value, 2));
        bits.set(20, bitRead(value, 3));
        bits.set(21, bitRead(value, 4));

        // month
        value = time.getMonthValue();
        bits.set(22, bitRead(value, 0));
        bits.set(23, bitRead(value, 1));
        bits.set(24, bitRead(value, 2));
        bits.set(25, bitRead(value, 3));

        // year
        value = time.getYear();
        bits.set(26, bitRead(value, 0));
        bits.set(27, bitRead(value, 1));
        bits.set(28, bitRead(value, 2));
        bits.set(29, bitRead(value, 3));
        bits.set(30, bitRead(value, 4));
        bits.set(31, bitRead(value, 5));

        return bits.toByteArray();
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
        final byte[] bytes = new byte[4];
        final BitSet bits = new BitSet(8);
        // 0x312 table 1
        bits.set(7, energyStorage.getBatteryPack(battery).alarms.levelTwoDischargeCurrentTooHigh.value);
        bits.set(6, energyStorage.getBatteryPack(battery).alarms.levelTwoChargeCurrentTooHigh.value);
        bits.set(5, energyStorage.getBatteryPack(battery).alarms.failureOfShortCircuitProtection.value);
        bits.set(4, energyStorage.getBatteryPack(battery).alarms.levelTwoCellVoltageTooHigh.value);
        bits.set(3, energyStorage.getBatteryPack(battery).alarms.levelTwoCellVoltageTooLow.value);
        bits.set(2, energyStorage.getBatteryPack(battery).alarms.levelTwoPackVoltageTooHigh.value);
        bits.set(1, energyStorage.getBatteryPack(battery).alarms.levelTwoPackVoltageTooLow.value);
        bits.set(0, false);

        bytes[0] = bits.toByteArray()[0];

        // 0x312 table 2
        bits.set(7, energyStorage.getBatteryPack(battery).alarms.levelTwoDischargeTempTooHigh.value);
        bits.set(6, energyStorage.getBatteryPack(battery).alarms.levelTwoChargeTempTooHigh.value);
        bits.set(5, energyStorage.getBatteryPack(battery).alarms.levelTwoDischargeTempTooLow.value);
        bits.set(4, energyStorage.getBatteryPack(battery).alarms.levelTwoChargeTempTooLow.value);
        bits.set(3, energyStorage.getBatteryPack(battery).alarms.failureOfAFEAcquisitionModule.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfChargeFETAdhesion.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfChargeFETTBreaker.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfChargeFETTemperatureSensor.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfCurrentSensorModule.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfDischargeFETAdhesion.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfDischargeFETBreaker.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfDischargeFETTemperatureSensor.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfEEPROMStorageModule.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfIntranetCommunicationModule.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfLowVoltageNoCharging.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfMainVoltageSensorModule.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfPrechargeModule.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfRealtimeClockModule.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfTemperatureSensorModule.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfVehicleCommunicationModule.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfVoltageSensorModule.value);
        bits.set(2, energyStorage.getBatteryPack(battery).alarms.levelTwoCellVoltageDifferenceTooHigh.value);
        bits.set(1, false);
        bits.set(0, false);

        bytes[1] = bits.toByteArray()[0];

        // 0x312 table 3
        bits.set(7, energyStorage.getBatteryPack(battery).alarms.levelOneDischargeCurrentTooHigh.value);
        bits.set(6, energyStorage.getBatteryPack(battery).alarms.levelOneChargeCurrentTooHigh.value);
        bits.set(5, false);
        bits.set(4, energyStorage.getBatteryPack(battery).alarms.levelOneCellVoltageTooHigh.value);
        bits.set(3, energyStorage.getBatteryPack(battery).alarms.levelOneCellVoltageTooLow.value);
        bits.set(2, energyStorage.getBatteryPack(battery).alarms.levelOnePackVoltageTooHigh.value);
        bits.set(1, energyStorage.getBatteryPack(battery).alarms.levelOnePackVoltageTooLow.value);
        bits.set(0, false);

        bytes[2] = bits.toByteArray()[0];

        // 0x312 table 4
        bits.set(7, energyStorage.getBatteryPack(battery).alarms.levelOneDischargeTempTooHigh.value);
        bits.set(6, energyStorage.getBatteryPack(battery).alarms.levelOneChargeTempTooHigh.value);
        bits.set(5, energyStorage.getBatteryPack(battery).alarms.levelOneDischargeTempTooLow.value);
        bits.set(4, energyStorage.getBatteryPack(battery).alarms.levelOneChargeTempTooLow.value);
        bits.set(3, false);
        bits.set(2, energyStorage.getBatteryPack(battery).alarms.levelOneCellVoltageDifferenceTooHigh.value);
        bits.set(1, false);
        bits.set(0, energyStorage.getBatteryPack(battery).alarms.failureOfIntranetCommunicationModule.value
                || energyStorage.getBatteryPack(battery).alarms.failureOfVehicleCommunicationModule.value);

        bytes[3] = bits.toByteArray()[0];

        return bits.toByteArray();
    }


    /**
     * See documentation Table 5.
     *
     * @return the bitset
     */
    private byte getChargeStates() {
        final BitSet bits = new BitSet(8);

        // 0x319 table 5
        bits.set(7, Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.chargeMOSState).anyMatch(b -> b == true));
        bits.set(6, Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.disChargeMOSState).anyMatch(b -> b == true));
        bits.set(5, false);
        bits.set(4, false);
        bits.set(3, false);
        bits.set(2, false);
        switch (energyStorage.getBatteryPack(0).type) {
            case 0:
                bits.set(1, 0);
                bits.set(0, 0);
            break;
            case 1:
                bits.set(1, 0);
                bits.set(0, 1);
            break;
            case 2:
                bits.set(1, 1);
                bits.set(0, 0);
            break;
        }

        return bits.toByteArray()[0];
    }

}
