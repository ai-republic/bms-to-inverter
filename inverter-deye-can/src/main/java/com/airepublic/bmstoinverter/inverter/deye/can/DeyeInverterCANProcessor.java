package com.airepublic.bmstoinverter.inverter.deye.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The class to handle CAN messages for Deye {@link Inverter}.
 */
@ApplicationScoped
public class DeyeInverterCANProcessor extends Inverter {
    @Inject
    private EnergyStorage energyStorage;

    @Override
    protected List<ByteBuffer> createSendFrames() {
        final List<ByteBuffer> frames = new ArrayList<>();

        frames.add(createChargeDischargeInfo()); // 0x351
        frames.add(createSOC()); // 0x355
        frames.add(createBatteryVoltage()); // 0x356
        frames.add(createManufacturer()); // 0x35E
        frames.add(createAlarms()); // 0x359

        return frames;
    }


    @Override
    protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
        ((CANPort) port).sendExtendedFrame(frame);
    }


    // 0x351
    private ByteBuffer createChargeDischargeInfo() {
        final int bmsNo = 0; // read the limits from the first BMS
        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.putInt(0x0351)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // Battery charge voltage (0.1V) - uint_16
        frame.putChar((char) energyStorage.getBatteryPack(bmsNo).maxPackVoltageLimit);
        // Charge current limit (0.1A) - sint_16
        frame.putShort((short) energyStorage.getBatteryPack(bmsNo).maxPackChargeCurrent);
        // Discharge current limit (0.1A) - sint_16
        frame.putShort((short) energyStorage.getBatteryPack(bmsNo).maxPackDischargeCurrent);
        // Battery discharge voltage (0.1V) - uint_16
        frame.putChar((char) energyStorage.getBatteryPack(bmsNo).minPackVoltageLimit);

        return frame;

    }


    // 0x355
    private ByteBuffer createSOC() {
        final int aggregatedSOC = (int) energyStorage.getBatteryPacks().stream().mapToInt(pack -> pack.packSOC).average().orElse(50);
        final int aggregatedSOH = (int) energyStorage.getBatteryPacks().stream().mapToInt(pack -> pack.packSOH).average().orElse(50);

        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.putInt(0x0355)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // SOC (1%) - uint_16
        frame.putChar((char) aggregatedSOC);
        // SOH (1%) - uint_16
        frame.putChar((char) aggregatedSOH);

        return frame;
    }


    // 0x356
    private ByteBuffer createBatteryVoltage() {
        final int aggregatedPackVoltage = (int) energyStorage.getBatteryPacks().stream().mapToInt(pack -> pack.packVoltage).average().orElse(500) * 10;
        final int aggregatedPackCurrent = energyStorage.getBatteryPacks().stream().mapToInt(pack -> pack.packCurrent).sum();
        final int aggregatedPackTemperature = (int) energyStorage.getBatteryPacks().stream().mapToInt(pack -> pack.tempMax).average().orElse(300);

        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.putInt(0x0356)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // Battery voltage (0.01V) - uint_16
        frame.putShort((short) aggregatedPackVoltage);
        // Battery current (0.1A) - uint_16
        frame.putShort((short) aggregatedPackCurrent);
        // Battery temperature (0.1C) - uint_16
        frame.putShort((short) aggregatedPackTemperature);

        return frame;
    }


    // 0x35E
    private ByteBuffer createManufacturer() {
        final int bmsNo = 0; // take the manufacturer from the first BMS
        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.putInt(0x035E)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        frame.putChar(energyStorage.getBatteryPack(bmsNo).manufacturerCode.charAt(0));
        frame.putChar(energyStorage.getBatteryPack(bmsNo).manufacturerCode.charAt(1));

        return frame;
    }


    // 0x359
    private ByteBuffer createAlarms() {
        final boolean aggregatedLevelTwoCellVoltageTooHigh = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelTwoCellVoltageTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelTwoCellVoltageTooLow = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelTwoCellVoltageTooLow).anyMatch(b -> true);
        final boolean aggregatedLevelTwoDischargeTempTooHigh = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelTwoDischargeTempTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelTwoDischargeTempTooLow = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelTwoDischargeTempTooLow).anyMatch(b -> true);
        final boolean aggregatedLevelTwoDischargeCurrentTooHigh = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelTwoDischargeCurrentTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelTwoChargeCurrentTooHigh = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelTwoChargeCurrentTooHigh).anyMatch(b -> true);

        final boolean aggregatedLevelOneCellVoltageTooHigh = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelOneCellVoltageTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelOneCellVoltageTooLow = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelOneCellVoltageTooLow).anyMatch(b -> true);
        final boolean aggregatedLevelOneChargeTempTooHigh = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelOneChargeTempTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelOneChargeTempTooLow = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelOneChargeTempTooLow).anyMatch(b -> true);
        final boolean aggregatedLevelOneDischargeCurrentTooHigh = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelOneDischargeCurrentTooHigh).anyMatch(b -> true);
        final boolean aggregatedLevelOneChargeCurrentTooHigh = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelOneChargeCurrentTooHigh).anyMatch(b -> true);
        final boolean aggregatedFailureOfIntranetCommunicationModule = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.failureOfIntranetCommunicationModule).anyMatch(b -> true);
        final boolean aggregatedLevelTwoCellVoltageDifferenceTooHigh = energyStorage.getBatteryPacks().stream().map(pack -> pack.alarms.levelTwoCellVoltageDifferenceTooHigh).anyMatch(b -> true);

        final BitSet bits = new BitSet(32);
        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.putInt(0x0359)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // protection alarms
        bits.set(1, aggregatedLevelTwoCellVoltageTooHigh);
        bits.set(2, aggregatedLevelTwoCellVoltageTooLow);
        bits.set(3, aggregatedLevelTwoDischargeTempTooHigh);
        bits.set(4, aggregatedLevelTwoDischargeTempTooLow);
        bits.set(7, aggregatedLevelTwoDischargeCurrentTooHigh);
        bits.set(8, aggregatedLevelTwoChargeCurrentTooHigh);

        // warning alarms
        bits.set(17, aggregatedLevelOneCellVoltageTooHigh);
        bits.set(18, aggregatedLevelOneCellVoltageTooLow);
        bits.set(19, aggregatedLevelOneChargeTempTooHigh);
        bits.set(20, aggregatedLevelOneChargeTempTooLow);
        bits.set(23, aggregatedLevelOneDischargeCurrentTooHigh);
        bits.set(24, aggregatedLevelOneChargeCurrentTooHigh);
        bits.set(27, aggregatedFailureOfIntranetCommunicationModule);
        bits.set(28, aggregatedLevelTwoCellVoltageDifferenceTooHigh);

        return frame;
    }

}
