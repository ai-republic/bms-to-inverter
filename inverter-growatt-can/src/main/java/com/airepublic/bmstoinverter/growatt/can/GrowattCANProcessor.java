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
import com.airepublic.bmstoinverter.core.PortType;
import com.airepublic.bmstoinverter.core.Protocol;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.inject.Inject;

/**
 * The {@link PortProcessor} to handle CAN messages for a Growatt low voltage (12V/24V/48V)
 * inverter.
 */
@Inverter
@PortType(Protocol.CAN)
public class GrowattCANProcessor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(GrowattCANProcessor.class);
    @Inject
    private EnergyStorage energyStorage;

    @Override
    public void process() {
        for (final Port port : getPorts()) {
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
    }


    private List<ByteBuffer> updateCANMessages() {
        final List<ByteBuffer> frames = new ArrayList<>();

        frames.add(createChargeDischargeInfo()); // 0x311
        frames.add(createAlarms()); // 0x312
        frames.add(createBatteryVoltage()); // 0x313
        frames.add(createBatteryStatus()); // 0x314
        frames.add(createMinMaxVoltageCell()); // 0x319
        frames.add(createBMSInfo()); // 0x320

        return frames;
    }


    // 0x311
    private ByteBuffer createChargeDischargeInfo() {
        final int bmsNo = 0; // read the limits from the first BMS
        final ByteBuffer frame = prepareFrame(0x311);

        // Battery charge voltage (0.1V) - uint_16
        frame.putChar((char) energyStorage.getBatteryPack(bmsNo).maxPackVoltageLimit);
        // Charge current limit (0.1A) - sint_16
        frame.putChar((char) energyStorage.getBatteryPack(bmsNo).maxPackChargeCurrent);
        // Discharge current limit (0.1A) - sint_16
        frame.putChar((char) energyStorage.getBatteryPack(bmsNo).maxPackDischargeCurrent);
        // status bits (see documentation)
        frame.put(get311Status());

        return frame;
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


    // 0x312
    private ByteBuffer createAlarms() {
        final ByteBuffer frame = prepareFrame(0x312);

        final boolean aggregatedLevelTwoCellVoltageTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelTwoCellVoltageTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelTwoCellVoltageTooLow = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelTwoCellVoltageTooLow).anyMatch(b -> true);
        final boolean aggregatedLevelTwoCellVoltageDifferenceTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelTwoCellVoltageDifferenceTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelTwoDischargeTempTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelTwoDischargeTempTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelTwoDischargeTempTooLow = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelTwoDischargeTempTooLow).anyMatch(b -> true);
        final boolean aggregatedLevelTwoChargeTempTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelTwoChargeTempTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelTwoChargeTempTooLow = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelTwoChargeTempTooLow).anyMatch(b -> true);
        final boolean aggregatedLevelTwoDischargeCurrentTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelTwoDischargeCurrentTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelTwoChargeCurrentTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelTwoChargeCurrentTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelTwoPackVoltageTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelTwoPackVoltageTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelTwoPackVoltageTooLow = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelTwoPackVoltageTooLow).anyMatch(b -> true);
        final boolean aggregatedFailureOfShortCircuitProtection = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.failureOfShortCircuitProtection).anyMatch(b -> true);

        final boolean aggregatedLevelOneCellVoltageTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelOneCellVoltageTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelOneCellVoltageTooLow = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelOneCellVoltageTooLow).anyMatch(b -> true);
        final boolean aggregatedLevelOneCellVoltageDifferenceTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelOneCellVoltageDifferenceTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelOneDischargeTempTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelOneDischargeTempTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelOneDischargeTempTooLow = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelOneDischargeTempTooLow).anyMatch(b -> true);
        final boolean aggregatedLevelOneChargeTempTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelOneChargeTempTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelOneChargeTempTooLow = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelOneChargeTempTooLow).anyMatch(b -> true);
        final boolean aggregatedLevelOneDischargeCurrentTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelOneDischargeCurrentTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelOneChargeCurrentTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelOneChargeCurrentTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelOnePackVoltageTooHigh = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelOnePackVoltageTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelOnePackVoltageTooLow = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.levelOnePackVoltageTooLow).anyMatch(b -> true);

        final boolean aggregatedFailureOfIntranetCommunicationModule = Stream.of(energyStorage.getBatteryPacks()).map(pack -> pack.alarms.failureOfIntranetCommunicationModule).anyMatch(b -> true);
        final boolean aggregatedSystemError = Stream.of(energyStorage.getBatteryPack(getChargeStates())).map(pack -> pack.alarms).anyMatch(alarms -> alarms.failureOfAFEAcquisitionModule.value
                || alarms.failureOfChargeFETAdhesion.value
                || alarms.failureOfChargeFETTBreaker.value
                || alarms.failureOfChargeFETTemperatureSensor.value
                || alarms.failureOfCurrentSensorModule.value
                || alarms.failureOfDischargeFETAdhesion.value
                || alarms.failureOfDischargeFETBreaker.value
                || alarms.failureOfDischargeFETTemperatureSensor.value
                || alarms.failureOfEEPROMStorageModule.value
                || alarms.failureOfIntranetCommunicationModule.value
                || alarms.failureOfLowVoltageNoCharging.value
                || alarms.failureOfMainVoltageSensorModule.value
                || alarms.failureOfPrechargeModule.value
                || alarms.failureOfRealtimeClockModule.value
                || alarms.failureOfTemperatureSensorModule.value
                || alarms.failureOfVehicleCommunicationModule.value
                || alarms.failureOfVoltageSensorModule.value);

        // protection alarms
        BitSet bits = new BitSet(8);
        bits.set(0, false);
        bits.set(1, aggregatedLevelTwoPackVoltageTooLow);
        bits.set(2, aggregatedLevelTwoPackVoltageTooHigh);
        bits.set(3, aggregatedLevelTwoCellVoltageTooLow);
        bits.set(4, aggregatedLevelTwoCellVoltageTooHigh);
        bits.set(5, aggregatedFailureOfShortCircuitProtection);
        bits.set(6, aggregatedLevelTwoChargeCurrentTooHigh);
        bits.set(7, aggregatedLevelTwoDischargeCurrentTooHigh);
        frame.put(bits.toByteArray()[0]);

        bits = new BitSet(8);
        bits.set(0, false);
        bits.set(1, false);
        bits.set(2, aggregatedLevelTwoCellVoltageDifferenceTooHigh);
        bits.set(3, aggregatedSystemError);
        bits.set(4, aggregatedLevelTwoChargeTempTooLow);
        bits.set(5, aggregatedLevelTwoDischargeTempTooLow);
        bits.set(6, aggregatedLevelTwoChargeTempTooHigh);
        bits.set(7, aggregatedLevelTwoDischargeTempTooHigh);
        frame.put(bits.toByteArray()[0]);

        // warning alarms
        bits = new BitSet(8);
        bits.set(0, false);
        bits.set(1, aggregatedLevelOnePackVoltageTooLow);
        bits.set(2, aggregatedLevelOnePackVoltageTooHigh);
        bits.set(3, aggregatedLevelOneCellVoltageTooLow);
        bits.set(4, aggregatedLevelOneCellVoltageTooHigh);
        bits.set(5, false);
        bits.set(6, aggregatedLevelOneChargeCurrentTooHigh);
        bits.set(7, aggregatedLevelOneDischargeCurrentTooHigh);
        frame.put(bits.toByteArray()[0]);

        bits = new BitSet(8);
        bits.set(0, aggregatedFailureOfIntranetCommunicationModule);
        bits.set(1, false);
        bits.set(2, aggregatedLevelOneCellVoltageDifferenceTooHigh);
        bits.set(3, false);
        bits.set(4, aggregatedLevelOneChargeTempTooLow);
        bits.set(5, aggregatedLevelOneDischargeTempTooLow);
        bits.set(6, aggregatedLevelOneChargeTempTooHigh);
        bits.set(7, aggregatedLevelOneDischargeTempTooHigh);
        frame.put(bits.toByteArray()[0]);

        frame.put((byte) energyStorage.getBatteryPack(0).numberOfCells);
        frame.putChar((char) 0); // skip 2 manufacturer codes
        frame.put((byte) Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.numberOfCells).sum());

        return frame;
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


    // 0x313
    private ByteBuffer createBatteryVoltage() {
        final int aggregatedPackVoltage = (int) Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.packVoltage).average().orElse(500) * 10;
        final int aggregatedPackCurrent = Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.packCurrent).sum();
        final int aggregatedPackTemperature = (int) Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.tempMax).average().orElse(35) * 10;
        final byte aggregatedSOC = (byte) (Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.packSOC).average().orElse(500) / 10);
        final byte aggregatedSOH = (byte) (Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.packSOH).average().orElse(500) / 10);

        final ByteBuffer frame = prepareFrame(0x313);

        // Battery voltage (0.01V) - uint_16
        frame.putShort((short) aggregatedPackVoltage);
        // Battery current (0.1A) - uint_16
        frame.putShort((short) aggregatedPackCurrent);
        // Battery temperature (0.1C) - uint_16
        frame.putShort((short) aggregatedPackTemperature);
        // Battery SOC
        frame.put(aggregatedSOC);
        // Battery SOH
        frame.put(aggregatedSOH);

        return frame;
    }


    // 0x314
    private ByteBuffer createBatteryStatus() {
        final int aggregatedCurrentCapacity = Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.remainingCapacitymAh).sum() / 10;
        final int aggregatedFullyChargedCapacity = Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.ratedCapacitymAh).sum() / 10;
        final int aggregatedHighestVoltageDiff = Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.cellDiffmV).max().orElse(0);
        final int aggregatedCycleCount = Stream.of(energyStorage.getBatteryPacks()).mapToInt(pack -> pack.bmsCycles).max().orElse(0);

        final ByteBuffer frame = prepareFrame(0x314);
        frame.putChar((char) aggregatedCurrentCapacity);
        frame.putChar((char) aggregatedFullyChargedCapacity);
        frame.putChar((char) aggregatedHighestVoltageDiff);
        frame.putChar((char) aggregatedCycleCount);

        return frame;
    }


    // 0x319
    private ByteBuffer createMinMaxVoltageCell() {
        int maxCellVoltage = 0;
        int maxCellVoltageNo = 0;
        int minCellVoltage = 4000;
        int minCellVoltageNo = 0;

        for (int i = 0; i < energyStorage.getBatteryPacks().length; i++) {
            final BatteryPack pack = energyStorage.getBatteryPack(i);

            if (pack.maxCellmV > maxCellVoltage) {
                maxCellVoltage = pack.maxCellmV;
                maxCellVoltageNo = pack.maxCellVNum;
            }

            if (pack.minCellmV < minCellVoltage) {
                minCellVoltage = pack.minCellmV;
                minCellVoltageNo = pack.minCellVNum;
            }
        }

        final ByteBuffer frame = prepareFrame(0x314);
        // charge state and battery status
        frame.put(getChargeStates());

        frame.putChar((char) maxCellVoltage);
        frame.putChar((char) minCellVoltage);
        frame.putChar((char) maxCellVoltageNo);
        frame.putChar((char) minCellVoltageNo);
        // pack id of faulty battery
        frame.put((byte) 0);

        return frame;
    }


    // 0x320
    private ByteBuffer createBMSInfo() {
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
        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
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

}
