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
package com.airepublic.bmstoinverter.bms.growatt.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.NoDataAvailableException;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.TooManyInvalidFramesException;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.core.util.BitUtil;
import com.airepublic.bmstoinverter.core.util.HexUtil;

/**
 * The class to handle CAN messages for a Growatt {@link BMS}.
 */
@ApplicationScoped
public class GrowattBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(GrowattBmsCANProcessor.class);
    private final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.BIG_ENDIAN);
    private final byte[] requestData = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };

    enum Command {
        READ_PACK_CHARGE_DISCHARGE_LIMITS(0x311),
        READ_ALARMS(0x312),
        READ_BATTERY_STATUS(0x313),
        READ_BATTERY_CAPACITY(0x314),
        READ_MIN_MAX_CELL_VOLTAGES(0x319),
        READ_MANUFACTURER_AND_MAX_CELL_VOLTAGE(0x320);

        private final int cmd;
        private final static Map<Integer, Command> commandMap = new HashMap<>();
        private static boolean initializedCommandMapping;

        Command(final int cmd) {
            this.cmd = cmd;
        }


        /**
         * Gets the command.
         * 
         * @return the command
         */
        public int getCommand() {
            return cmd;
        }


        /**
         * Gets the {@link PaceCommand} for the specified command or null if it does not exist.
         *
         * @param command the command
         * @return the {@link PaceCommand}
         */
        public static Command forCommand(final int command) {
            if (!initializedCommandMapping) {
                for (final Command cmd : values()) {
                    commandMap.put(cmd.getCommand(), cmd);
                }

                initializedCommandMapping = true;
            }

            return commandMap.get(command);
        }
    }

    @Override
    protected void collectData(final Port port) throws IOException, TooManyInvalidFramesException, NoDataAvailableException {
        // read all values
        for (final Command cmd : Command.values()) {
            sendMessage(port, cmd, requestData);
        }
    }


    protected List<ByteBuffer> sendMessage(final Port port, final Command cmd, final byte[] data) throws IOException, NoDataAvailableException {
        final ByteBuffer sendFrame = prepareSendFrame(getBmsId(), cmd, data);
        final List<ByteBuffer> readBuffers = new ArrayList<>();
        int noDataReceived = 0;
        boolean frameReceived = false;

        LOG.debug("SEND: {}", Port.printBuffer(sendFrame));
        ((CANPort) port).sendExtendedFrame(sendFrame);

        // read frames until the requested frame is read
        do {
            final ByteBuffer receiveFrame = port.receiveFrame();
            LOG.debug("RECEIVED: {}", Port.printBuffer(receiveFrame));

            if (receiveFrame == null) {
                // keep track of how often no bytes could be read
                noDataReceived++;
                LOG.debug("No bytes received: " + noDataReceived + " times!");

                // if we received no bytes more than 10 times we stop and notify the handler
                // to re-open the port
                if (noDataReceived >= 10) {
                    throw new NoDataAvailableException();
                }

                // try and wait for the next message to arrive
                try {
                    LOG.debug("Waiting for messages to arrive....");
                    Thread.sleep(getDelayAfterNoBytes());
                } catch (final InterruptedException e) {
                }
            } else {
                // read the BMS id
                final int canId = receiveFrame.getInt();
                final byte bmsId = (byte) (canId & 0x00000001); // last 4 bits represents bms id

                // check that the frame is meant for this BMS
                if (getBmsId() != bmsId) {
                    LOG.debug("Got frame for wrong BMS id: {} instead of {}", bmsId, getBmsId());
                } else {
                    frameReceived = true;
                    // request has 4th byte 0x10 and response 0x20
                    final Command command = Command.forCommand(canId - 0x00001000);
                    // move position to the data part
                    receiveFrame.getInt();

                    // one batterypack per BMS
                    final BatteryPack pack = getBatteryPack(0);

                    readBuffers.add(receiveFrame);

                    switch (command) {
                        // 0x311
                        case READ_PACK_CHARGE_DISCHARGE_LIMITS: {
                            readChargeDischargeLimits(pack, receiveFrame);
                        }
                        break;
                        // 0x312
                        case READ_ALARMS: {
                            readAlarms(pack, receiveFrame);
                        }
                        break;
                        // 0x313
                        case READ_BATTERY_STATUS: {
                            readBatteryStatus(pack, receiveFrame);
                        }
                        break;
                        // 0x314
                        case READ_BATTERY_CAPACITY: {
                            readBatteryCapacity(pack, receiveFrame);
                        }
                        break;
                        // 0x319
                        case READ_MIN_MAX_CELL_VOLTAGES: {
                            readMinMaxCellVoltages(pack, receiveFrame);
                        }
                        break;
                        // 0x320
                        case READ_MANUFACTURER_AND_MAX_CELL_VOLTAGE: {
                            readManufacturerAndVersion(pack, receiveFrame);
                        }
                        break;
                        default: {
                            LOG.warn("Message could not be interpreted " + Port.printBuffer(receiveFrame));
                            port.clearBuffers();
                            return readBuffers;
                        }
                    }
                }
            }
        } while (!frameReceived);

        LOG.debug("Command 0x{} to BMS {} successfully sent and received!", HexUtil.toHexDigits(cmd.getCommand()), getBmsId());
        return readBuffers;
    }


    protected ByteBuffer prepareSendFrame(final int bmsId, final Command cmd, final byte[] data) {
        sendFrame.rewind();

        sendFrame.putInt(cmd.cmd + getBmsId());

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // data
        sendFrame.put(data);

        sendFrame.rewind();

        return sendFrame;
    }


    // 0x311
    private void readChargeDischargeLimits(final BatteryPack pack, final ByteBuffer frame) throws IOException {
        // Charge cutoff voltage (0.1V)
        pack.maxPackVoltageLimit = frame.getChar();
        // Max charge current (0.1A) offset 0A
        pack.maxPackChargeCurrent = frame.getChar();
        // Max discharge current (0.1A) offset -3000A
        pack.maxPackDischargeCurrent = frame.getChar() * -1;

        // Battery status
        final short status = frame.getShort();
        switch (status & 0x0013) {
            case 1: {
                // standby/idle
                pack.chargeDischargeStatus = 0; // Byte 7 bit 0,1 (01)
            }
            break;
            case 2: {
                // charging
                pack.chargeDischargeStatus = 1; // Byte 7 bit 0,1 (10)
            }
            break;
            case 3: {
                // discharging
                pack.chargeDischargeStatus = 2; // Byte 7 bit 0,1 (11)
            }
            break;
            case 16: {
                // battery sleeping state
                pack.chargeDischargeStatus = 0; // Byte 7 bit 4
            }
            break;
        }

        // cell balancing state
        pack.cellBalanceActive = BitUtil.bit(status, 3); // Byte 7 bit 3

        LOG.debug("Read max/min charge and discharge voltage and current limits: {}", Port.printBuffer(frame));
    }


    // 0x312
    private void readAlarms(final BatteryPack pack, final ByteBuffer frame) throws IOException {

        // Protection
        final int protection = frame.getInt();
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(protection, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(protection, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(protection, 4) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, BitUtil.bit(protection, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(protection, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_VOLTAGE_LOW, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_VOLTAGE_HIGH, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_OTHER, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_LOW, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.SOC_LOW, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.setAlarm(Alarm.ENCASING_TEMPERATURE_HIGH, BitUtil.bit(protection, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // Alarm
        final int alarm = frame.getInt();
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_INTERNAL, BitUtil.bit(alarm, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        pack.setAlarm(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, BitUtil.bit(alarm, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        pack.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(alarm, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_LOW, BitUtil.bit(alarm, 5) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(alarm, 6) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(alarm, 7) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_VOLTAGE_LOW, BitUtil.bit(alarm, 8) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(alarm, 9) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(alarm, 10) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(alarm, 11) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(alarm, 12) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_VOLTAGE_HIGH, BitUtil.bit(alarm, 13) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(alarm, 14) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(alarm, 15) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        pack.setAlarm(Alarm.SOC_LOW, BitUtil.bit(alarm, 17) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH, BitUtil.bit(alarm, 18) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(alarm, 19) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.ENCASING_TEMPERATURE_HIGH, BitUtil.bit(alarm, 20) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_EXTERNAL, BitUtil.bit(alarm, 21) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_COMMUNICATION_INTERNAL, BitUtil.bit(alarm, 22) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, BitUtil.bit(alarm, 23) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.setAlarm(Alarm.SOC_LOW, BitUtil.bit(alarm, 23) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        LOG.debug("Read alarms: {}", Port.printBuffer(frame));
    }


    // 0x313
    private void readBatteryStatus(final BatteryPack pack, final ByteBuffer frame) throws IOException {
        // Battery voltage (0.1V)
        pack.packVoltage = frame.getChar();
        // Battery current (0.1A)
        pack.packCurrent = frame.getShort();
        // second level temperature (0.1 Celcius)
        pack.tempAverage = frame.getShort();
        // Battery SOC (1%)
        pack.packSOC = frame.get() * 10;
        // Battery SOH (1%)
        pack.packSOH = frame.get() * 10;

        LOG.debug("Read battery status: {}", Port.printBuffer(frame));
    }


    // 0x314
    private void readBatteryCapacity(final BatteryPack pack, final ByteBuffer frame) {
        // Current battery energy (10mAH)
        pack.remainingCapacitymAh = frame.getChar() * 10;
        // Rated battery energy (10mAH)
        pack.ratedCapacitymAh = frame.getChar() * 10;
        // manufacturer code
        pack.manufacturerCode = String.valueOf(frame.getChar());
        // cycle count
        pack.bmsCycles = frame.getChar();

        LOG.debug("Read battery capacity: {}", Port.printBuffer(frame));
    }


    // 0x319
    private void readMinMaxCellVoltages(final BatteryPack pack, final ByteBuffer frame) {
        // Battery status
        setChargeStates(pack, frame.get());

        // Max cell voltage (1mV)
        pack.maxCellmV = frame.getChar();
        // Min cell voltage (1mV)
        pack.minCellmV = frame.getChar();

        LOG.debug("Read cell min/max voltages: {}", Port.printBuffer(frame));
    }


    // 0x320
    private void readManufacturerAndVersion(final BatteryPack pack, final ByteBuffer frame) {
        // manufacturer code
        final char chr1 = (char) frame.get();
        final char chr2 = (char) frame.get();
        pack.manufacturerCode = "" + chr1 + chr2;
        pack.hardwareVersion = "" + (char) frame.get();
        final char swLow = (char) frame.get();
        pack.softwareVersion = "" + (char) frame.get() + "." + swLow;

        LOG.debug("Read manufacturer and hard-/software version: {}", Port.printBuffer(frame));
    }


    /**
     * See documentation Table for 0x319.
     *
     * @param pack the {@link BatteryPack}
     * @param value the charge state bits
     */
    private void setChargeStates(final BatteryPack pack, final byte value) {
        pack.chargeMOSState = BitUtil.bit(value, 7);
        pack.dischargeMOSState = BitUtil.bit(value, 6);

        pack.type = value & 0x03;
    }
}
