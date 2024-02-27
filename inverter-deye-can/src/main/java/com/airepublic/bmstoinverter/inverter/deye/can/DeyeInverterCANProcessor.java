package com.airepublic.bmstoinverter.inverter.deye.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.core.util.Util;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The class to handle CAN messages for Deye {@link Inverter}.
 */
@ApplicationScoped
public class DeyeInverterCANProcessor extends Inverter {

    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        final List<ByteBuffer> frames = new ArrayList<>();

        frames.add(createChargeDischargeLimits(aggregatedPack)); // 0x351
        frames.add(createSOC(aggregatedPack)); // 0x355
        frames.add(createBatteryVoltage(aggregatedPack)); // 0x356
        frames.add(createChargeDischargeFlags(aggregatedPack)); // 0x35C
        frames.add(createManufacturer(aggregatedPack)); // 0x35E
        frames.add(createAlarms(aggregatedPack)); // 0x359

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


    protected ByteBuffer prepareSendFrame(final int frameId) {
        final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);
        sendFrame.putInt(frameId);

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        return sendFrame;
    }


    // 0x351
    private ByteBuffer createChargeDischargeLimits(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x0351);

        // Battery charge voltage (0.1V) - uint_16
        frame.putChar((char) pack.maxPackVoltageLimit);
        // Charge current limit (0.1A) - sint_16
        frame.putShort((short) pack.maxPackChargeCurrent);
        // Discharge current limit (0.1A) - sint_16
        frame.putShort((short) pack.maxPackDischargeCurrent);
        // Battery discharge voltage (0.1V) - uint_16
        frame.putChar((char) pack.minPackVoltageLimit);

        return frame;

    }


    // 0x355
    private ByteBuffer createSOC(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x0355);

        // SOC (1%) - uint_16
        frame.putChar((char) (pack.packSOC / 10));
        // SOH (1%) - uint_16
        frame.putChar((char) (pack.packSOH / 10));

        return frame;
    }


    // 0x356
    private ByteBuffer createBatteryVoltage(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x0356);

        // Battery voltage (0.01V) - uint_16
        frame.putShort((short) (pack.packVoltage * 10));
        // Battery current (0.1A) - uint_16
        frame.putShort((short) pack.packCurrent);
        // Battery temperature (0.1C) - uint_16
        frame.putShort((short) pack.tempAverage);

        return frame;
    }


    // 0x35C
    private ByteBuffer createChargeDischargeFlags(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x035E);
        final byte flags = 0x00;

        // request full charge
        Util.setBit(flags, 3, false);
        // request force charge
        Util.setBit(flags, 4, pack.forceCharge);
        Util.setBit(flags, 5, pack.forceCharge);
        Util.setBit(flags, 6, pack.dischargeMOSState);
        Util.setBit(flags, 7, pack.chargeMOSState);

        frame.put(flags);

        return frame;
    }


    // 0x35E
    private ByteBuffer createManufacturer(final BatteryPack pack) {
        final ByteBuffer frame = prepareSendFrame(0x035E);
        int idx = 0;

        while (idx < pack.manufacturerCode.length() && idx < 8) {
            frame.putChar(pack.manufacturerCode.charAt(idx));
            idx++;
        }

        return frame;
    }


    // 0x359
    private ByteBuffer createAlarms(final BatteryPack pack) {

        final BitSet bits = new BitSet(32);
        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.putInt(0x0359)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // protection alarms
        bits.set(1, pack.alarms.levelTwoCellVoltageTooHigh.value);
        bits.set(2, pack.alarms.levelTwoCellVoltageTooLow.value);
        bits.set(3, pack.alarms.levelTwoDischargeTempTooHigh.value);
        bits.set(4, pack.alarms.levelTwoDischargeTempTooLow.value);
        bits.set(7, pack.alarms.levelTwoDischargeCurrentTooHigh.value);
        bits.set(8, pack.alarms.levelTwoChargeCurrentTooHigh.value);

        // warning alarms
        bits.set(17, pack.alarms.levelOneCellVoltageTooHigh.value);
        bits.set(18, pack.alarms.levelOneCellVoltageTooLow.value);
        bits.set(19, pack.alarms.levelOneChargeTempTooHigh.value);
        bits.set(20, pack.alarms.levelOneChargeTempTooLow.value);
        bits.set(23, pack.alarms.levelOneDischargeCurrentTooHigh.value);
        bits.set(24, pack.alarms.levelOneChargeCurrentTooHigh.value);
        bits.set(27, pack.alarms.failureOfIntranetCommunicationModule.value);
        bits.set(28, pack.alarms.levelTwoCellVoltageDifferenceTooHigh.value);

        return frame;
    }

}
