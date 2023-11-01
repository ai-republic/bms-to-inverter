package com.airepublic.bmstoinverter.deye.can;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortProcessor;
import com.airepublic.bmstoinverter.core.Portname;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CAN;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import jakarta.inject.Inject;

/**
 * The {@link PortProcessor} to handle CAN messages from a JK BMS.
 */
@Inverter
public class DeyeInverterCANProcessor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(DeyeInverterCANProcessor.class);
    @Inject
    @CAN
    @Portname("inverter.portname")
    private CANPort port;
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
        final int bmsNo = 0; // Deye only gets aggregated battery info of 1 pack

        frames.add(createChargeDischargeInfo(bmsNo)); // 0x351
        frames.add(createSOC(bmsNo)); // 0x355
        frames.add(createBatteryVoltage(bmsNo)); // 0x356
        frames.add(createManufacturer(bmsNo)); // 0x35E
        frames.add(createAlarms(bmsNo)); // 0x359

        return frames;
    }


    private ByteBuffer createChargeDischargeInfo(final int bmsNo) {
        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.putInt(0x0351)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // Battery charge voltage (0.1V) - uint_16
        // frame.putChar((char) energyStorage.getBatteryPack(bmsNo).maxPackVoltageLimit);
        // Charge current limit (0.1A) - sint_16
        frame.putShort((short) energyStorage.getBatteryPack(bmsNo).maxPackChargeCurrent);
        // Discharge current limit (0.1A) - sint_16
        frame.putShort((short) energyStorage.getBatteryPack(bmsNo).maxPackDischargeCurrent);
        // Battery discharge voltage (0.1V) - uint_16
        // frame.putChar((char) energyStorage.getBatteryPack(bmsNo).packVoltage);

        return frame;

    }


    private ByteBuffer createSOC(final int bmsNo) {
        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.putInt(0x0355)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // SOC (1%) - uint_16
        frame.putChar((char) energyStorage.getBatteryPack(bmsNo).maxPackDischargeCurrent);
        // SOH (1%) - uint_16
        frame.putChar((char) energyStorage.getBatteryPack(bmsNo).packVoltage);

        return frame;
    }


    private ByteBuffer createBatteryVoltage(final int bmsNo) {
        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.putInt(0x0356)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // Battery voltage (0.01V) - uint_16
        frame.putShort((short) energyStorage.getBatteryPack(bmsNo).packVoltage);
        // Battery current (0.1A) - uint_16
        frame.putShort((short) energyStorage.getBatteryPack(bmsNo).packCurrent);
        // Battery temperature (0.1C) - uint_16
        frame.putShort((short) energyStorage.getBatteryPack(bmsNo).tempAverage);

        return frame;
    }


    private ByteBuffer createManufacturer(final int bmsNo) {
        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.putInt(0x035E)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        frame.putChar(energyStorage.getBatteryPack(bmsNo).manufacturerCode.charAt(0));
        frame.putChar(energyStorage.getBatteryPack(bmsNo).manufacturerCode.charAt(1));

        return frame;
    }


    private ByteBuffer createAlarms(final int bmsNo) {
        final BitSet bits = new BitSet(32);
        final ByteBuffer frame = ByteBuffer.allocateDirect(16);
        frame.putInt(0x0359)
                .put((byte) 8)
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // protection alarms
        bits.set(1, energyStorage.getBatteryPack(bmsNo).alarms.levelTwoCellVoltageTooHigh.value);
        bits.set(2, energyStorage.getBatteryPack(bmsNo).alarms.levelTwoCellVoltageTooLow.value);
        bits.set(3, energyStorage.getBatteryPack(bmsNo).alarms.levelTwoDischargeTempTooHigh.value);
        bits.set(4, energyStorage.getBatteryPack(bmsNo).alarms.levelTwoDischargeTempTooLow.value);
        bits.set(7, energyStorage.getBatteryPack(bmsNo).alarms.levelTwoDischargeCurrentTooHigh.value);
        bits.set(8, energyStorage.getBatteryPack(bmsNo).alarms.levelTwoChargeCurrentTooHigh.value);

        // warning alarms
        bits.set(17, energyStorage.getBatteryPack(bmsNo).alarms.levelOneCellVoltageTooHigh.value);
        bits.set(18, energyStorage.getBatteryPack(bmsNo).alarms.levelOneCellVoltageTooLow.value);
        bits.set(19, energyStorage.getBatteryPack(bmsNo).alarms.levelOneChargeTempTooHigh.value);
        bits.set(20, energyStorage.getBatteryPack(bmsNo).alarms.levelOneChargeTempTooLow.value);
        bits.set(23, energyStorage.getBatteryPack(bmsNo).alarms.levelOneDischargeCurrentTooHigh.value);
        bits.set(24, energyStorage.getBatteryPack(bmsNo).alarms.levelOneChargeCurrentTooHigh.value);
        bits.set(27, energyStorage.getBatteryPack(bmsNo).alarms.failureOfIntranetCommunicationModule.value);
        bits.set(28, energyStorage.getBatteryPack(bmsNo).alarms.levelTwoCellVoltageDifferenceTooHigh.value);

        return frame;
    }

}
