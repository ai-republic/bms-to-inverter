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
package com.airepublic.bmstoinverter.bms.pace.can;

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
 * The class to handle CAN messages from a PACE {@link BMS}.
 */
public class PaceBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(PaceBmsCANProcessor.class);
    private final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);
    private final byte[] requestData = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };

    enum PaceCommand {
        READ_CELL_VOLTAGES(0x42, 4),
        READ_CELL_TEMPERATURES(0x43, 4),
        READ_SYSTEM_INFORMATION(0x44, 2),
        READ_WARNINGS(0x45, 1),
        READ_SOC_SOH_CAPACITY(0x46, 2),
        READ_BALANCING_STATES(0x47, 5),
        SEND_CONTROL_COMMAND(0x99, 1);

        private final int cmd;
        private final int priority;
        private final static Map<Integer, PaceCommand> commandMap = new HashMap<>();
        private static boolean initializedCommandMapping;

        PaceCommand(final int cmd, final int priority) {
            this.cmd = cmd;
            this.priority = priority;
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
         * Gets the priority.
         *
         * @return the priority
         */
        public int getPriority() {
            return priority;
        }


        /**
         * Gets the {@link PaceCommand} for the specified command or null if it does not exist.
         *
         * @param command the command
         * @return the {@link PaceCommand}
         */
        public static PaceCommand forCommand(final int command) {
            if (!initializedCommandMapping) {
                for (final PaceCommand cmd : values()) {
                    commandMap.put(cmd.getCommand(), cmd);
                }

                initializedCommandMapping = true;
            }

            return commandMap.get(command);
        }
    }

    @Override
    protected void collectData(final Port port) throws IOException, TooManyInvalidFramesException, NoDataAvailableException {
        sendMessage(port, PaceCommand.READ_CELL_VOLTAGES, requestData); // 0x42
        sendMessage(port, PaceCommand.READ_CELL_TEMPERATURES, requestData); // 0x43
        sendMessage(port, PaceCommand.READ_SYSTEM_INFORMATION, requestData); // 0x44
        sendMessage(port, PaceCommand.READ_WARNINGS, requestData); // 0x45
        sendMessage(port, PaceCommand.READ_SOC_SOH_CAPACITY, requestData); // 0x46
        sendMessage(port, PaceCommand.READ_BALANCING_STATES, requestData); // 0x47
    }


    protected List<ByteBuffer> sendMessage(final Port port, final PaceCommand cmd, final byte[] data) throws IOException, NoDataAvailableException {
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
                final byte bmsId = (byte) (receiveFrame.get(0) - 0x10);

                // check that the frame is meant for this BMS
                if (getBmsId() != bmsId) {
                    LOG.debug("Got frame for wrong BMS id: {} instead of {}", bmsId, getBmsId());
                } else {
                    frameReceived = true;
                    final PaceCommand command = PaceCommand.forCommand(receiveFrame.get(2));
                    // move position to the data part
                    receiveFrame.getLong();

                    // one batterypack per BMS
                    final BatteryPack pack = getBatteryPack(0);

                    readBuffers.add(receiveFrame);

                    switch (command) {
                        case READ_CELL_VOLTAGES: {
                            readCellVoltages(pack, receiveFrame);
                        }
                        break;
                        case READ_CELL_TEMPERATURES: {
                            readCellTemperatures(pack, receiveFrame);
                        }
                        break;
                        case READ_SYSTEM_INFORMATION: {
                            readSystemInformation(pack, receiveFrame);
                        }
                        break;
                        case READ_WARNINGS: {
                            readAlarms(pack, receiveFrame);
                        }
                        break;
                        case READ_SOC_SOH_CAPACITY: {
                            readSocSohCapacity(pack, receiveFrame);
                        }
                        break;
                        case READ_BALANCING_STATES: {
                            readBalancingStates(pack, receiveFrame);
                        }
                        break;
                        case SEND_CONTROL_COMMAND: {
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


    protected ByteBuffer prepareSendFrame(final int bmsId, final PaceCommand cmd, final byte[] data) {
        sendFrame.rewind();

        sendFrame.put((byte) 0x01);
        sendFrame.put((byte) (0x10 + bmsId));
        sendFrame.put((byte) cmd.getCommand());
        sendFrame.put((byte) (cmd.getPriority() << 2));

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // data
        sendFrame.put(data);

        sendFrame.rewind();

        return sendFrame;
    }


    private void readCellVoltages(final BatteryPack pack, final ByteBuffer data) {
        // frame no holding the cell voltages (3 per frame)
        final int frameNo = data.getShort();
        // Next 3 cell voltages
        pack.cellVmV[frameNo * 3] = data.getShort();
        pack.cellVmV[frameNo * 3 + 1] = data.getShort();
        pack.cellVmV[frameNo * 3 + 2] = data.getShort();
    }


    private void readCellTemperatures(final BatteryPack pack, final ByteBuffer data) {
        // frame no holding the cell temperatures (3 per frame)
        final int frameNo = data.getShort();
        // Next 3 cell temperatures
        pack.cellTemperature[frameNo * 3] = data.getShort();
        pack.cellTemperature[frameNo * 3 + 1] = data.getShort();
        pack.cellTemperature[frameNo * 3 + 2] = data.getShort();
    }


    private void readSystemInformation(final BatteryPack pack, final ByteBuffer data) {
        // number of cells in the pack
        pack.numberOfCells = data.getShort();
        // Battery voltage (0.01V)
        pack.packVoltage = data.getShort() * 10;
        // Battery current (0.1A)
        pack.packCurrent = data.getShort();
        // Battery cycles
        pack.bmsCycles = data.getShort();
    }


    private void readSocSohCapacity(final BatteryPack pack, final ByteBuffer data) {
        // rated capacity (10mAH)
        pack.ratedCapacitymAh = data.getShort() * 10;
        // remaining capacity (10mAH)
        pack.remainingCapacitymAh = data.getShort() * 10;
        // full capacity (10mAH)
        data.getShort();
        // Battery SOC (1%)
        pack.packSOC = data.get() * 10;
        // Battery SOH (1%)
        pack.packSOH = data.get() * 10;
    }


    private void readBalancingStates(final BatteryPack pack, final ByteBuffer data) {
        // frame no holding the cell balance states (48 per frame)
        final int frameNo = data.getShort();

        // read the next 6 bytes
        for (int byteNo = 0; byteNo < 6; byteNo++) {
            final byte value = data.get();

            // each bit in the byte is the balance state of a cell
            for (int bitNo = 0; bitNo < 8; bitNo++) {
                final int cellNo = frameNo * 48 * byteNo * 8 + bitNo;

                // check if we've read the state for all cells
                if (cellNo > pack.numberOfCells) {
                    return;
                }

                pack.cellBalanceState[cellNo] = BitUtil.bit(value, bitNo);
            }
        }
    }


    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        pack.setAlarm(Alarm.FAILURE_OTHER, AlarmLevel.NONE);

        // read first 8 bits (protect states 1)
        byte value = data.get();

        // cell overvoltage
        pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(value, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // cell undervoltage
        pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(value, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // total voltage overvoltage
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(value, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // total voltage undervoltage
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(value, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // charge overcurrent
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(value, 4) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // discharge overcurrent
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(value, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // short circut
        pack.setAlarm(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, BitUtil.bit(value, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // read next 8 bits (protect states 2)
        value = data.get();

        // charge temperature too high
        pack.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(value, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // Large pressure difference in cell
        pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(value, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // charge temperature too low
        pack.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(value, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // discharge temperature too low
        pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_LOW, BitUtil.bit(value, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // MOS temperature too high
        pack.setAlarm(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(value, 4) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // environment temperature too high
        pack.setAlarm(Alarm.PACK_TEMPERATURE_HIGH, BitUtil.bit(value, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // environment temperature too low
        pack.setAlarm(Alarm.PACK_TEMPERATURE_LOW, BitUtil.bit(value, 6) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // read next 16 bits (indicate and control states)
        data.getShort();

        // read next 8 bits (fault state)
        value = data.get();

        // charge MOS fault
        pack.setAlarm(Alarm.FAILURE_CHARGE_BREAKER, BitUtil.bit(value, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // discharge MOS fault
        pack.setAlarm(Alarm.FAILURE_DISCHARGE_BREAKER, BitUtil.bit(value, 1) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // NTC fault
        pack.setAlarm(Alarm.FAILURE_CLOCK_MODULE, BitUtil.bit(value, 2) ? AlarmLevel.ALARM : AlarmLevel.NONE);

        // read next 8 bits (reserved)
        data.get();
        // read next 8 bits (warn state 1)
        value = data.get();

        // cell overvoltage
        pack.setAlarm(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(value, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // cell undervoltage
        pack.setAlarm(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(value, 1) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // total voltage overvoltage
        pack.setAlarm(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(value, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // total voltage undervoltage
        pack.setAlarm(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(value, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // charge overcurrent
        pack.setAlarm(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(value, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // discharge overcurrent
        pack.setAlarm(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(value, 5) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // read next 8 bits (warn states 2)
        value = data.get();

        // charge temperature too high
        pack.setAlarm(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(value, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // Large pressure difference in cell
        pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(value, 1) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // charge temperature too low
        pack.setAlarm(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(value, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // discharge temperature too low
        pack.setAlarm(Alarm.DISCHARGE_TEMPERATURE_LOW, BitUtil.bit(value, 3) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // environment temperature too high
        pack.setAlarm(Alarm.PACK_TEMPERATURE_HIGH, BitUtil.bit(value, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // environment temperature too low
        pack.setAlarm(Alarm.PACK_TEMPERATURE_LOW, BitUtil.bit(value, 5) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // MOS temperature too high
        pack.setAlarm(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH, BitUtil.bit(value, 6) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // SOC low
        pack.setAlarm(Alarm.SOC_LOW, BitUtil.bit(value, 7) ? AlarmLevel.WARNING : AlarmLevel.NONE);
    }
}
