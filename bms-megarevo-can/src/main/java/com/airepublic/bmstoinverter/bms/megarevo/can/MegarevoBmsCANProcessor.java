/**
 * This software is free to use and to distribute in its unchanged form for private use.
 * Commercial use is prohibited without an explicit license agreement of the copyright holder.
 * Any changes to this software must be made solely in the project repository at https://github.com/ai-republic/bms-to-inverter.
 * The copyright holder is not liable for any damages in whatever form that may occur by using this software.
 *
 * (c) Copyright 2022 and onwards - Torsten Oltmanns
 *
 * @author Torsten Oltmanns - bms-to-inverter''AT''gmail.com
 */
package com.airepublic.bmstoinverter.bms.megarevo.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.protocol.can.JavaCANPort;

/**
 * The class to handle CAN messages from a Megarevo {@link BMS}.
 */
public class MegarevoBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(MegarevoBmsCANProcessor.class);
    private final static int BATTERY_ID = 0;

    @Override
    public void collectData(final Port port) {
        try {
            final JavaCANPort canPort = (JavaCANPort) port;
            // send request frame to request battery data
            canPort.sendExtendedFrame(prepareSendFrame(0x1F000100, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 }));
            canPort.sendExtendedFrame(prepareSendFrame(0x0001001F, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 }));

            final BatteryPack pack = getBatteryPack(BATTERY_ID);
            final ByteBuffer frame = canPort.receiveFrame();
            final int frameId = frame.getInt();
            final byte[] bytes = new byte[8];
            frame.position(8);
            frame.get(bytes);
            final ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

            switch (frameId) {
                case 0x1F000300:
                    readChargeDischargeInfo(pack, data);
                break;
                case 0x1F000301:
                    readSOCAndVoltage(pack, data);
                break;
                case 0x1F000302:
                    readMinMaxTemperatureVoltage(pack, data);
                break;
                case 0x1F000303:
                    readAlarms(pack, data);
                break;
                case 0x1F000304:
                    readCapacity(pack, data);
                break;
            }
        } catch (final IOException e) {
            LOG.error("Error receiving frame!", e);
        }
    }


    protected ByteBuffer prepareSendFrame(final int id, final byte[] data) {
        final ByteBuffer sendFrame = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        sendFrame.rewind();

        sendFrame.putInt(id);

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // data
        sendFrame.put(data);

        sendFrame.rewind();

        return sendFrame;
    }


    // 0x1F000300
    protected void readChargeDischargeInfo(final BatteryPack pack, final ByteBuffer data) {
        // Battery charge voltage (0.1V) - uint_16
        pack.maxPackVoltageLimit = data.getChar();
        // Battery discharge voltage (0.1V) - uint_16
        pack.minPackVoltageLimit = data.getChar();
        // Charge current limit (0.1A) - uint_16
        pack.maxPackChargeCurrent = data.getChar();
        // Discharge current limit (0.1A) - uint_16
        pack.maxPackDischargeCurrent = data.getChar();

        LOG.debug("\nMax Voltage \tMin Voltage \tMax Charge \tMax Discharge\n  {}\t\t{}\t\t{}\t\t", pack.maxPackVoltageLimit / 10f, pack.minPackVoltageLimit / 10f, pack.maxPackChargeCurrent / 10f, pack.maxPackDischargeCurrent / 10f);
    }


    // 0x1F000301
    protected void readSOCAndVoltage(final BatteryPack pack, final ByteBuffer data) {
        // SOC (1%) - uint_16
        pack.packSOC = data.getChar() * 10;
        // SOH (1%) - uint_16
        pack.packSOH = data.getChar() * 10;
        // Battery voltage (0.1V) - uint_16
        pack.packVoltage = data.getChar();
        // Battery current (0.1A) - sint_16
        pack.packCurrent = data.getShort();

        LOG.debug("\nSOC \tSOH  \tPack V \t Pack A\n{} \t{} \t{} \t{}", pack.packSOC / 10f, pack.packSOH / 10f, pack.packVoltage / 10f, pack.packCurrent / 10f);
    }


    // 0x1F000302
    protected void readMinMaxTemperatureVoltage(final BatteryPack pack, final ByteBuffer data) {
        // Maximum cell voltage (0.001V) - uint_16
        pack.maxCellmV = data.getChar();
        // Minimum cell voltage (0.001V) - uint_16
        pack.minCellmV = data.getChar();
        // Maximum cell temperature (0.1C) - sint_16
        pack.tempMax = data.getShort();
        // Minimum cell temperature (0.1C) - sint_16
        pack.tempMin = data.getShort();

        LOG.debug("\nMax Temp \tMin Temp \tMax Cell mV \tMin Cell mV\n{} \t {}\t\t{}\t\t{}", pack.tempMax / 10f, pack.tempMin / 10f, pack.maxCellmV, pack.minCellmV);
    }


    // 0x1F000303
    protected void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        final int warningLevel = data.getChar();
        final int chargeStatus = data.getChar();
        final int dischargeStatus = data.getChar();

        switch (warningLevel) {
            case 0:
                pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.NONE);
            break;
            case 1:
                pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.WARNING);
            break;
            case 2:
                pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.ALARM);
            break;
            default:
                pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.ALARM);
            break;
        }

        if (chargeStatus == 0x02) {
            pack.setAlarm(Alarm.CHARGE_VOLTAGE_HIGH, AlarmLevel.ALARM);
        } else {
            pack.setAlarm(Alarm.CHARGE_VOLTAGE_HIGH, AlarmLevel.NONE);
        }

        if (dischargeStatus == 0x02) {
            pack.setAlarm(Alarm.DISCHARGE_VOLTAGE_HIGH, AlarmLevel.ALARM);
        } else {
            pack.setAlarm(Alarm.DISCHARGE_VOLTAGE_HIGH, AlarmLevel.NONE);
        }
    }


    // 0x1F000304
    protected void readCapacity(final BatteryPack pack, final ByteBuffer data) {
        // Battery capacity (0.1Ah) - uint_16
        pack.ratedCapacitymAh = data.getChar() * 100;
        // Remaining capacity (0.1Ah) - uint_16
        pack.remainingCapacitymAh = data.getChar() * 100;

        LOG.debug("\nCapacity \tRemaining\n \t{}\t\t{}", pack.ratedCapacitymAh / 1000f, pack.remainingCapacitymAh / 1000f);
    }
}
