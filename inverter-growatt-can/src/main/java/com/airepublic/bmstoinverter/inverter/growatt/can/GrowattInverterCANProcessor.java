package com.airepublic.bmstoinverter.inverter.growatt.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The class to handle CAN messages for a Growatt low voltage (12V/24V/48V) {@link Inverter}.
 */
@ApplicationScoped
public class GrowattInverterCANProcessor extends Inverter {

    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        final List<ByteBuffer> frames = new ArrayList<>();

        frames.add(createChargeDischargeInfo(aggregatedPack)); // 0x311
        frames.add(createAlarms(aggregatedPack)); // 0x312
        frames.add(createBatteryVoltage(aggregatedPack)); // 0x313
        frames.add(createBatteryStatus(aggregatedPack)); // 0x314
        frames.add(createMinMaxVoltageCell(aggregatedPack)); // 0x319
        frames.add(createBMSInfo(aggregatedPack)); // 0x320

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


    // 0x311
    private ByteBuffer createChargeDischargeInfo(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x311);

        // Battery charge voltage (0.1V) - uint_16
        frame.putChar((char) pack.maxPackVoltageLimit);
        // Charge current limit (0.1A) - sint_16
        frame.putChar((char) pack.maxPackChargeCurrent);
        // Discharge current limit (0.1A) - sint_16
        frame.putChar((char) pack.maxPackDischargeCurrent);
        // status bits (see documentation)
        frame.put(get311Status(pack));

        return frame;
    }


    private byte[] get311Status(final BatteryPack pack) {
        final BitSet bits = new BitSet(12);
        // charging status
        final boolean charging = pack.chargeMOSState;
        bits.set(0, charging);
        bits.set(1, false);

        // error bit flag
        bits.set(2, false);

        // balancing status
        bits.set(3, pack.cellBalanceActive);

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


    // 0x312
    private ByteBuffer createAlarms(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x312);

        final boolean aggregatedSystemError = pack.alarms.failureOfAFEAcquisitionModule.value
                || pack.alarms.failureOfChargeFETAdhesion.value
                || pack.alarms.failureOfChargeFETTBreaker.value
                || pack.alarms.failureOfChargeFETTemperatureSensor.value
                || pack.alarms.failureOfCurrentSensorModule.value
                || pack.alarms.failureOfDischargeFETAdhesion.value
                || pack.alarms.failureOfDischargeFETBreaker.value
                || pack.alarms.failureOfDischargeFETTemperatureSensor.value
                || pack.alarms.failureOfEEPROMStorageModule.value
                || pack.alarms.failureOfIntranetCommunicationModule.value
                || pack.alarms.failureOfLowVoltageNoCharging.value
                || pack.alarms.failureOfMainVoltageSensorModule.value
                || pack.alarms.failureOfPrechargeModule.value
                || pack.alarms.failureOfRealtimeClockModule.value
                || pack.alarms.failureOfTemperatureSensorModule.value
                || pack.alarms.failureOfInternalCommunicationModule.value
                || pack.alarms.failureOfVoltageSensorModule.value;

        // protection alarms
        BitSet bits = new BitSet(8);
        bits.set(0, false);
        bits.set(1, pack.alarms.levelTwoPackVoltageTooLow.value);
        bits.set(2, pack.alarms.levelTwoPackVoltageTooHigh.value);
        bits.set(3, pack.alarms.levelTwoCellVoltageTooLow.value);
        bits.set(4, pack.alarms.levelTwoCellVoltageTooHigh.value);
        bits.set(5, pack.alarms.failureOfShortCircuitProtection.value);
        bits.set(6, pack.alarms.levelTwoChargeCurrentTooHigh.value);
        bits.set(7, pack.alarms.levelTwoDischargeCurrentTooHigh.value);
        frame.put(bits.toByteArray()[0]);

        bits = new BitSet(8);
        bits.set(0, false);
        bits.set(1, false);
        bits.set(2, pack.alarms.levelTwoCellVoltageDifferenceTooHigh.value);
        bits.set(3, aggregatedSystemError);
        bits.set(4, pack.alarms.levelTwoChargeTempTooLow.value);
        bits.set(5, pack.alarms.levelTwoDischargeTempTooLow.value);
        bits.set(6, pack.alarms.levelTwoChargeTempTooHigh.value);
        bits.set(7, pack.alarms.levelTwoDischargeTempTooHigh.value);
        frame.put(bits.toByteArray()[0]);

        // warning alarms
        bits = new BitSet(8);
        bits.set(0, false);
        bits.set(1, pack.alarms.levelOnePackVoltageTooLow.value);
        bits.set(2, pack.alarms.levelOnePackVoltageTooHigh.value);
        bits.set(3, pack.alarms.levelOneCellVoltageTooLow.value);
        bits.set(4, pack.alarms.levelOneCellVoltageTooHigh.value);
        bits.set(5, false);
        bits.set(6, pack.alarms.levelOneChargeCurrentTooHigh.value);
        bits.set(7, pack.alarms.levelOneDischargeCurrentTooHigh.value);
        frame.put(bits.toByteArray()[0]);

        bits = new BitSet(8);
        bits.set(0, pack.alarms.failureOfIntranetCommunicationModule.value);
        bits.set(1, false);
        bits.set(2, pack.alarms.levelOneCellVoltageDifferenceTooHigh.value);
        bits.set(3, false);
        bits.set(4, pack.alarms.levelOneChargeTempTooLow.value);
        bits.set(5, pack.alarms.levelOneDischargeTempTooLow.value);
        bits.set(6, pack.alarms.levelOneChargeTempTooHigh.value);
        bits.set(7, pack.alarms.levelOneDischargeTempTooHigh.value);
        frame.put(bits.toByteArray()[0]);

        frame.put((byte) pack.numberOfCells);
        frame.putChar((char) 0); // skip 2 manufacturer codes
        frame.put((byte) pack.numberOfCells);

        return frame;
    }


    /**
     * See documentation Table 5.
     *
     * @return the bitset
     */
    private byte getChargeStates(final BatteryPack pack) {
        final BitSet bits = new BitSet(8);

        // 0x319 table 5
        bits.set(7, pack.chargeMOSState);
        bits.set(6, pack.disChargeMOSState);
        bits.set(5, false);
        bits.set(4, false);
        bits.set(3, false);
        bits.set(2, false);
        switch (pack.type) {
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


    // 0x313
    private ByteBuffer createBatteryVoltage(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x313);

        // Battery voltage (0.01V) - uint_16
        frame.putShort((short) (pack.packVoltage * 10));
        // Battery current (0.1A) - uint_16
        frame.putShort((short) pack.packCurrent);
        // Battery temperature (0.1C) - uint_16
        frame.putShort((short) pack.tempAverage);
        // Battery SOC
        frame.put((byte) (pack.packSOC / 10));
        // Battery SOH
        frame.put((byte) (pack.packSOH / 10));

        return frame;
    }


    // 0x314
    private ByteBuffer createBatteryStatus(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x314);
        // remaining capacity (10mAh)
        frame.putChar((char) (pack.remainingCapacitymAh / 10f));
        // full capacity (10mAh)
        frame.putChar((char) (pack.ratedCapacitymAh / 10f));
        // cell difference (1mv)
        frame.putChar((char) pack.cellDiffmV);
        // bms cycles
        frame.putChar((char) pack.bmsCycles);

        return frame;
    }


    // 0x319
    private ByteBuffer createMinMaxVoltageCell(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x314);
        // charge state and battery status
        frame.put(getChargeStates(pack));

        frame.putChar((char) pack.maxCellmV);
        frame.putChar((char) pack.minCellmV);
        frame.put((byte) pack.maxCellVNum);
        frame.put((byte) pack.minCellVNum);
        // pack id of faulty battery
        frame.put((byte) 0);

        return frame;
    }


    // 0x320
    private ByteBuffer createBMSInfo(final BatteryPack pack) {
        final ByteBuffer frame = prepareFrame(0x320);

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


    private boolean bitRead(final int value, final int index) {
        return (value >> index & 1) == 1;
    }


    public static void main(final String[] args) {
        final BatteryPack pack = new BatteryPack();
        pack.packVoltage = 535;
        pack.packCurrent = 15;
        pack.packSOC = 940;
        pack.packSOH = 1000;
        pack.tempMax = 220;
        final EnergyStorage es = new EnergyStorage();
        es.getBatteryPacks().add(pack);

        final GrowattInverterCANProcessor processor = new GrowattInverterCANProcessor();
        final ByteBuffer frame = processor.createBatteryVoltage(pack);

        System.out.println(Port.printBuffer(frame));
    }
}
