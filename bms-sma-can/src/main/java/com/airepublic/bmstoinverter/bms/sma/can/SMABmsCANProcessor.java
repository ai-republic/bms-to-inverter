package com.airepublic.bmstoinverter.bms.sma.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * The class to handle CAN messages from a SMA {@link BMS}.
 */
public class SMABmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(SMABmsCANProcessor.class);
    private final static int BATTERY_ID = 0;

    @Override
    public void collectData(final Port port) {
        try {
            final BatteryPack pack = getBatteryPack(BATTERY_ID);
            final ByteBuffer frame = port.receiveFrame();
            final int frameId = frame.getInt();
            final byte[] bytes = new byte[8];
            frame.get(bytes);
            final ByteBuffer data = ByteBuffer.wrap(bytes);

            switch (frameId) {
                case 0x351:
                    readChargeDischargeInfo(pack, data);
                break;
                case 0x355:
                    readSOC(pack, data);
                break;
                case 0x356:
                    readBatteryVoltage(pack, data);
                break;
                case 0x35A:
                    readAlarms(pack, data);
                break;
            }
        } catch (final IOException e) {
            LOG.error("Error receiving frame!", e);
        }
    }


    // 0x351
    private void readChargeDischargeInfo(final BatteryPack pack, final ByteBuffer data) {

        // Battery charge voltage (0.1V) - u_int_16
        pack.maxPackVoltageLimit = data.getChar();
        // Charge current limit (0.1A) - s_int_16
        pack.maxPackChargeCurrent = data.getShort();
        // Discharge current limit (0.1A) - s_int_16
        pack.maxPackDischargeCurrent = data.getShort();
        // Battery discharge voltage (0.1V) - u_int_16
        pack.minPackVoltageLimit = data.getChar();
    }


    // 0x355
    private void readSOC(final BatteryPack pack, final ByteBuffer data) {
        // SOC (1%) - u_int_16
        pack.packSOC = data.getChar() * 10;
        // SOH (1%) - u_int_16
        pack.packSOH = data.getChar() * 10;
    }


    // 0x356
    private void readBatteryVoltage(final BatteryPack pack, final ByteBuffer data) {
        // Battery voltage (0.01V) - u_int_16
        pack.packVoltage = (int) (data.getShort() / 10f);
        // Battery current (0.1A) - u_int_16
        pack.packCurrent = data.getShort();
        // Battery temperature (0.1C) - u_int_16
        pack.tempAverage = data.getShort();
    }


    // 0x35A
    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        BitSet bits = BitSet.valueOf(new byte[] { data.get() });

        // alarms
        pack.alarms.levelTwoPackVoltageTooHigh.value = bits.get(2); // pack voltage to high
        pack.alarms.levelTwoPackVoltageTooHigh.value = !bits.get(3);
        pack.alarms.levelTwoPackVoltageTooLow.value = bits.get(4); // pack voltage to low
        pack.alarms.levelTwoPackVoltageTooLow.value = !bits.get(5);
        pack.alarms.levelTwoChargeTempTooHigh.value = bits.get(6);// pack temp to high
        pack.alarms.levelTwoChargeTempTooHigh.value = !bits.get(7);

        bits = BitSet.valueOf(new byte[] { data.get() });

        pack.alarms.levelTwoChargeTempTooLow.value = bits.get(0); // pack temp to low
        pack.alarms.levelTwoChargeTempTooLow.value = !bits.get(1);
        pack.alarms.levelTwoChargeTempTooHigh.value = bits.get(2);// charge temp to high
        pack.alarms.levelTwoChargeTempTooHigh.value = !bits.get(3);
        pack.alarms.levelTwoChargeTempTooLow.value = bits.get(4); // charge temp to low
        pack.alarms.levelTwoChargeTempTooLow.value = !bits.get(5);
        pack.alarms.levelTwoChargeCurrentTooHigh.value = bits.get(6); // pack current to high
        pack.alarms.levelTwoChargeCurrentTooHigh.value = !bits.get(7);

        bits = BitSet.valueOf(new byte[] { data.get() });

        pack.alarms.levelTwoChargeCurrentTooHigh.value = bits.get(0); // charge current to high
        pack.alarms.levelTwoChargeCurrentTooHigh.value = !bits.get(1);
        pack.alarms.failureOfShortCircuitProtection.value = bits.get(4); // short circuit
        pack.alarms.failureOfShortCircuitProtection.value = !bits.get(5);

        bits = BitSet.valueOf(new byte[] { data.get() });

        pack.alarms.levelTwoCellVoltageDifferenceTooHigh.value = bits.get(0); // cell difference to
                                                                              // high
        pack.alarms.levelTwoCellVoltageDifferenceTooHigh.value = !bits.get(1);

        // warnings
        bits = BitSet.valueOf(new byte[] { data.get() });

        pack.alarms.levelOnePackVoltageTooHigh.value = bits.get(2); // pack voltage to high
        pack.alarms.levelOnePackVoltageTooHigh.value = !bits.get(3);
        pack.alarms.levelOnePackVoltageTooLow.value = bits.get(4); // pack voltage to low
        pack.alarms.levelOnePackVoltageTooLow.value = !bits.get(5);
        pack.alarms.levelOneChargeTempTooHigh.value = bits.get(6);// pack temp to high
        pack.alarms.levelOneChargeTempTooHigh.value = !bits.get(7);

        bits = BitSet.valueOf(new byte[] { data.get() });

        pack.alarms.levelOneChargeTempTooLow.value = bits.get(0); // pack temp to low
        pack.alarms.levelOneChargeTempTooLow.value = !bits.get(1);
        pack.alarms.levelOneChargeTempTooHigh.value = bits.get(2);// charge temp to high
        pack.alarms.levelOneChargeTempTooHigh.value = !bits.get(3);
        pack.alarms.levelOneChargeTempTooLow.value = bits.get(4); // charge temp to low
        pack.alarms.levelOneChargeTempTooLow.value = !bits.get(5);
        pack.alarms.levelOneChargeCurrentTooHigh.value = bits.get(6); // pack current to high
        pack.alarms.levelOneChargeCurrentTooHigh.value = !bits.get(7);

        bits = BitSet.valueOf(new byte[] { data.get() });

        pack.alarms.levelOneChargeCurrentTooHigh.value = bits.get(0); // charge current to high
        pack.alarms.levelOneChargeCurrentTooHigh.value = !bits.get(1);
        pack.alarms.failureOfShortCircuitProtection.value = bits.get(4); // short circuit
        pack.alarms.failureOfShortCircuitProtection.value = !bits.get(5);

        bits = BitSet.valueOf(new byte[] { data.get() });

        pack.alarms.levelOneCellVoltageDifferenceTooHigh.value = bits.get(0); // cell difference to
        pack.alarms.levelOneCellVoltageDifferenceTooHigh.value = !bits.get(1);
    }

}
