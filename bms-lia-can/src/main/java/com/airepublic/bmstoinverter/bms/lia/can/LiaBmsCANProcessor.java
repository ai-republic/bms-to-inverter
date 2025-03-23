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
package com.airepublic.bmstoinverter.bms.lia.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * The class to handle CAN messages from a LIA {@link BMS}.
 */
public class LiaBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(LiaBmsCANProcessor.class);
    private final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);
    private final byte[] requestData = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };

    enum Command {
        READ_PROTOCOL_VERSION(0x0CF10100),
        READ_SOFTWARE_VERSION(0x0CF10110),
        READ_MAX_MIN_CELL_TEMPERATURE_VOLTAGE(0x0CF10200),
        READ_CHARGE_DISCHARGE_CURRENT_VOLTAGE(0x0CF10210),
        READ_SOC_CAPACITY(0x0CF10220),
        READ_WARNINGS(0x0CF10300),
        READ_PACK_CHARGE_DISCHARGE_STATES(0x0CF10310),
        READ_CELL_CHARGE_DISCHARGE_STATES(0x0CF10320),
        READ_PACK_CHARGE_DISCHARGE_LIMITS(0x0CF10400),
        READ_PACK_CHARGE_DISCHARGE_REQUESTS(0x0CF10410);

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
                        case READ_MAX_MIN_CELL_TEMPERATURE_VOLTAGE: {
                            readMinMaxCellTemperatureVoltage(pack, receiveFrame);
                        }
                        break;
                        case READ_WARNINGS: {
                            readAlarms(pack, receiveFrame);
                        }
                        break;
                        case READ_CHARGE_DISCHARGE_CURRENT_VOLTAGE: {
                            readPackChargeDisChargeCurrentVoltage(pack, receiveFrame);
                        }
                        break;
                        case READ_SOC_CAPACITY: {
                            readSoCCapacity(pack, receiveFrame);
                        }
                        break;
                        case READ_PACK_CHARGE_DISCHARGE_STATES: {
                            readPackChargeDisChargeStates(pack, receiveFrame);
                        }
                        break;
                        case READ_CELL_CHARGE_DISCHARGE_STATES: {
                            readCellChargeDisChargeStates(pack, receiveFrame);
                        }
                        break;
                        case READ_PACK_CHARGE_DISCHARGE_LIMITS: {
                            readPackChargeDisChargeLimits(pack, receiveFrame);
                        }
                        break;
                        case READ_PACK_CHARGE_DISCHARGE_REQUESTS: {
                            readPackChargeDisChargeRequests(pack, receiveFrame);
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


    private void readMinMaxCellTemperatureVoltage(final BatteryPack pack, final ByteBuffer receiveFrame) {
        // max cell voltage (0.1V)
        pack.maxCellmV = receiveFrame.getShort() * 100;
        // min cell voltage (0.1V)
        pack.minCellmV = receiveFrame.getShort() * 100;
        // max cell temperature (0.1K)
        pack.tempMax = receiveFrame.getShort() - 273;
        // min cell temperature (0.1K)
        pack.tempMin = receiveFrame.getShort() - 273;
    }


    private void readPackChargeDisChargeCurrentVoltage(final BatteryPack pack, final ByteBuffer receiveFrame) {
        // charge current (0.1A)
        final int chargeCurrent = receiveFrame.getShort();
        // discharge current (0.1A)
        final int dischargeCurrent = receiveFrame.getShort() * -1;

        pack.packCurrent = chargeCurrent != 0 ? chargeCurrent : dischargeCurrent;

        // voltage (0.1V)
        pack.packVoltage = receiveFrame.getShort();

        // rest is reserved
    }


    private void readSoCCapacity(final BatteryPack pack, final ByteBuffer receiveFrame) {
        // SOC (1%)
        pack.packSOC = receiveFrame.getShort() * 10;

        // capacity (mAh)
        pack.ratedCapacitymAh = receiveFrame.getInt();

        // rest is reserved
    }


    private void readAlarms(final BatteryPack pack, final ByteBuffer receiveFrame) {
        short state = receiveFrame.getShort();
        pack.alarms.put(Alarm.CHARGE_VOLTAGE_LOW, state == 0x01 ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_VOLTAGE_HIGH, state == 0x02 ? AlarmLevel.ALARM : AlarmLevel.NONE);

        state = receiveFrame.getShort();
        pack.alarms.put(Alarm.DISCHARGE_VOLTAGE_LOW, state == 0x01 ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_VOLTAGE_HIGH, state == 0x02 ? AlarmLevel.ALARM : AlarmLevel.NONE);

        state = receiveFrame.getShort();
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, state == 0x01 ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, state == 0x02 ? AlarmLevel.ALARM : AlarmLevel.NONE);

        state = receiveFrame.getShort();
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, state == 0x01 && pack.alarms.get(Alarm.CELL_VOLTAGE_LOW) == AlarmLevel.NONE ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, state == 0x02 && pack.alarms.get(Alarm.CELL_VOLTAGE_HIGH) == AlarmLevel.NONE ? AlarmLevel.ALARM : AlarmLevel.NONE);

    }


    private void readPackChargeDisChargeStates(final BatteryPack pack, final ByteBuffer receiveFrame) {
        short state = receiveFrame.getShort();
        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, state == 0x02 ? AlarmLevel.ALARM : AlarmLevel.NONE);

        state = receiveFrame.getShort();
        pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, state == 0x02 ? AlarmLevel.ALARM : AlarmLevel.NONE);

        state = receiveFrame.getShort();
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, state == 0x01 ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, state == 0x02 ? AlarmLevel.ALARM : AlarmLevel.NONE);

        state = receiveFrame.getShort();
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_LOW, state == 0x01 ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, state == 0x02 ? AlarmLevel.ALARM : AlarmLevel.NONE);
    }


    private void readCellChargeDisChargeStates(final BatteryPack pack, final ByteBuffer receiveFrame) {
        short state = receiveFrame.getShort();
        pack.alarms.put(Alarm.CELL_TEMPERATURE_LOW, state == 0x01 ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_TEMPERATURE_HIGH, state == 0x02 ? AlarmLevel.ALARM : AlarmLevel.NONE);

        state = receiveFrame.getShort();
        pack.alarms.put(Alarm.CELL_TEMPERATURE_LOW, state == 0x01 && pack.alarms.get(Alarm.CELL_TEMPERATURE_LOW) == AlarmLevel.NONE ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_TEMPERATURE_HIGH, state == 0x02 && pack.alarms.get(Alarm.CELL_TEMPERATURE_HIGH) == AlarmLevel.NONE ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // rest is reserved
    }


    private void readPackChargeDisChargeLimits(final BatteryPack pack, final ByteBuffer receiveFrame) {
        pack.maxPackVoltageLimit = receiveFrame.getShort();
        pack.minPackVoltageLimit = receiveFrame.getShort();
        pack.maxPackChargeCurrent = receiveFrame.getShort();
        pack.maxPackDischargeCurrent = receiveFrame.getShort();
    }


    private void readPackChargeDisChargeRequests(final BatteryPack pack, final ByteBuffer receiveFrame) {
        receiveFrame.get(); // reserved

        final byte status = receiveFrame.get();

        if (BitUtil.bit(status, 7)) {
            pack.chargeDischargeStatus = 1;
        } else if (BitUtil.bit(status, 6)) {
            pack.chargeDischargeStatus = 2;
        }

        // rest is reserved
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
}
