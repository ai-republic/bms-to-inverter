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
package com.airepublic.bmstoinverter.bms.seplos.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.NoDataAvailableException;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.TooManyInvalidFramesException;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.BitUtil;
import com.airepublic.bmstoinverter.core.util.ByteAsciiConverter;

/**
 * The class to handle RS485 messages from a SacredSun (TIAN) BMS.
 */
public class SeplosBmsRS485Processor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(SeplosBmsRS485Processor.class);

    @Override
    protected void collectData(final Port port) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
        sendMessage(port, "42", this::readBatteryInformation); // battery information
        sendMessage(port, "44", this::readAlarms); // battery alarms
    }


    private List<ByteBuffer> sendMessage(final Port port, final String cid2, final BiConsumer<BatteryPack, ByteBuffer> handler) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
        // first convert the bmsId to ascii bytes
        final String bmsId = new String(ByteAsciiConverter.convertByteToAsciiBytes((byte) getBmsId()));
        // insert the ascii byte bmsId into the request bytes
        final String command = "20" + bmsId + "46" + cid2 + "00021";

        final String frame = "~" + command + createChecksum(command) + "\r";
        final ByteBuffer sendBuffer = ByteBuffer.wrap(frame.getBytes()).order(ByteOrder.LITTLE_ENDIAN);
        final List<ByteBuffer> readBuffers = new ArrayList<>();
        int failureCount = 0;
        int noDataReceived = 0;
        boolean done = false;
        final BatteryPack pack = getBatteryPack(getBmsId());

        // read frames until the requested frame is read
        do {

            // send the request command frame
            port.sendFrame(sendBuffer);
            LOG.debug("SEND: {}", Port.printBuffer(sendBuffer));

            try {
                Thread.sleep(92);
            } catch (final InterruptedException e) {
            }

            // read the expected response frame(s)
            boolean valid = false;
            ByteBuffer receiveBuffer = null;

            try {
                receiveBuffer = port.receiveFrame();

                if (receiveBuffer != null) {
                    // check start and end flag
                    valid = receiveBuffer.get(0) == 0x7E && receiveBuffer.get(receiveBuffer.capacity() - 1) == 0x0D;

                    if (valid) {
                        LOG.debug("RECEIVED: {}", Port.printBuffer(receiveBuffer));
                        receiveBuffer.rewind();

                        // extract address
                        final byte[] addressAscii = new byte[2];
                        receiveBuffer.position(3);
                        receiveBuffer.get(addressAscii);
                        final byte address = ByteAsciiConverter.convertAsciiBytesToByte(addressAscii[0], addressAscii[1]);

                        // extract length
                        final byte[] lengthAscii = new byte[4];
                        receiveBuffer.position(9);
                        receiveBuffer.get(lengthAscii);
                        final short length = ByteAsciiConverter.convertAsciiBytesToShort(lengthAscii);

                        receiveBuffer.position(13);
                        receiveBuffer.limit(13 + length);
                        final ByteBuffer data = receiveBuffer.slice();

                        handler.accept(pack, data);
                        done = true;
                    } else {
                        LOG.warn("Frame is not valid: " + Port.printBuffer(receiveBuffer));
                    }
                } else if (receiveBuffer == null) { // received nothing
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

                    // try to receive the response again
                    valid = false;
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                valid = false;
            }

            if (!valid) { // we received an invalid frame
                // keep track of how often invalid frames were received
                failureCount++;
                LOG.debug("Invalid frame received! {}", Port.printBuffer(receiveBuffer));

                if (failureCount >= 10) {
                    // try and wait for the bus to get quiet
                    try {
                        LOG.debug("Waiting for bus to idle....");
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                    }

                    port.clearBuffers();

                    throw new TooManyInvalidFramesException();
                }
            }
        } while (!done);

        LOG.warn("Command {} to sent to BMS successfully and received!", command);

        return readBuffers;
    }


    private String createChecksum(final String command) {
        short sum = 0;

        for (final byte b : command.getBytes()) {
            sum += b;
        }

        short checksum = (short) (sum % 65536);
        checksum = (short) ~checksum;
        checksum++;

        return String.format("%04X", checksum);
    }


    // 0x42
    private void readBatteryInformation(final BatteryPack pack, final ByteBuffer data) {
        final byte[] shortAsciiBuffer = new byte[4];

        // read the first 4 bytes (data flag and command group)
        data.getInt();

        // cell quantity
        pack.numberOfCells = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        // cell voltages 1mV
        int cellNo = 0;

        for (; cellNo < pack.numberOfCells; cellNo++) {
            data.get(shortAsciiBuffer);
            pack.cellVmV[cellNo] = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);

            if (pack.cellVmV[cellNo] < pack.minCellmV) {
                pack.minCellmV = pack.cellVmV[cellNo];
                pack.minCellVNum = cellNo;
            }

            if (pack.cellVmV[cellNo] > pack.maxCellmV) {
                pack.maxCellmV = pack.cellVmV[cellNo];
                pack.maxCellVNum = cellNo;
            }
        }

        while (cellNo < 16) {
            data.get(shortAsciiBuffer);
            cellNo++;
        }

        // temp sensor quantity
        pack.numOfTempSensors = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        // temperature sensors 0.1K
        for (int tempSensorNo = 0; tempSensorNo < 4; tempSensorNo++) {
            data.get(shortAsciiBuffer);
            pack.cellTemperature[tempSensorNo] = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) - 2731;
        }

        // ambient temperature 0.1C
        data.get(shortAsciiBuffer);
        pack.tempAverage = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get()) - 2731;

        // component temperature 0.1C
        data.get(shortAsciiBuffer);

        // charge and discharge current 0.01A
        data.get(shortAsciiBuffer);
        pack.maxPackChargeCurrent = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) / 10;
        pack.maxPackDischargeCurrent = pack.maxPackChargeCurrent;

        // pack voltage 0.01V
        data.get(shortAsciiBuffer);
        pack.packVoltage = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) / 10;

        // remaining capacity 0.01Ah
        data.get(shortAsciiBuffer);
        pack.remainingCapacitymAh = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) * 10;

        // customize info
        data.getShort();

        // battery capacity 0.01Ah
        data.get(shortAsciiBuffer);
        pack.moduleRatedCapacityAh = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) / 100;

        // SOC 1%
        data.get(shortAsciiBuffer);
        pack.packSOC = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);

        // rated capacity 0.01Ah
        data.get(shortAsciiBuffer);
        pack.ratedCapacitymAh = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer) * 10;

        // cycle life
        data.get(shortAsciiBuffer);
        pack.bmsCycles = ByteAsciiConverter.convertAsciiBytesToShort(shortAsciiBuffer);

        // SOH 0.1%
        data.get(shortAsciiBuffer);
        pack.packSOH = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get()) / 10;
    }


    // 0x42
    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        byte warning = 0x00;
        // read the first 4 bytes (data flag and command group)
        data.getInt();

        // cell quantity
        pack.numberOfCells = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        // cell voltages 1mV
        int cellNo = 0;

        for (; cellNo < pack.numberOfCells; cellNo++) {
            warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
            setAlarm(pack, warning, Alarm.CELL_VOLTAGE_LOW, Alarm.CELL_VOLTAGE_HIGH, AlarmLevel.WARNING);
        }

        while (cellNo < 16) {
            data.getShort();
            cellNo++;
        }

        // temp sensor quantity
        pack.numOfTempSensors = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        // temperature sensors 0.1K
        for (int tempSensorNo = 0; tempSensorNo < 4; tempSensorNo++) {
            warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
            setAlarm(pack, warning, Alarm.CELL_TEMPERATURE_LOW, Alarm.CELL_TEMPERATURE_HIGH, AlarmLevel.WARNING);
        }

        // ambient temperature 0.1C
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        setAlarm(pack, warning, Alarm.ENCASING_TEMPERATURE_LOW, Alarm.ENCASING_TEMPERATURE_HIGH, AlarmLevel.WARNING);

        // component temperature 0.1C
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        setAlarm(pack, warning, Alarm.PACK_TEMPERATURE_LOW, Alarm.PACK_TEMPERATURE_HIGH, AlarmLevel.WARNING);

        // charge and discharge current
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        setAlarm(pack, warning, Alarm.DISCHARGE_CURRENT_HIGH, Alarm.CHARGE_CURRENT_HIGH, AlarmLevel.WARNING);

        // pack voltage 0.01V
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        setAlarm(pack, warning, Alarm.PACK_VOLTAGE_LOW, Alarm.PACK_VOLTAGE_HIGH, AlarmLevel.WARNING);

        // custom warning
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        // warning 1
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        pack.alarms.put(Alarm.FAILURE_SENSOR_PACK_VOLTAGE, BitUtil.bit(warning, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_SENSOR_PACK_TEMPERATURE, BitUtil.bit(warning, 1) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_SENSOR_PACK_CURRENT, BitUtil.bit(warning, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(warning, 3) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_DIFFERENCE_HIGH, BitUtil.bit(warning, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_CHARGE_BREAKER, BitUtil.bit(warning, 5) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_DISCHARGE_BREAKER, BitUtil.bit(warning, 6) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(warning, 7) ? AlarmLevel.WARNING : AlarmLevel.NONE);

        // warning 2
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(warning, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_HIGH, BitUtil.bit(warning, 1) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.CELL_VOLTAGE_HIGH));
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(warning, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CELL_VOLTAGE_LOW, BitUtil.bit(warning, 3) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.CELL_VOLTAGE_LOW));
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(warning, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_HIGH, BitUtil.bit(warning, 5) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.PACK_VOLTAGE_HIGH));
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(warning, 6) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_VOLTAGE_LOW, BitUtil.bit(warning, 7) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.PACK_VOLTAGE_LOW));

        // warning 3
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(warning, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_HIGH, BitUtil.bit(warning, 1) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.CHARGE_TEMPERATURE_HIGH));
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(warning, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_TEMPERATURE_LOW, BitUtil.bit(warning, 3) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.CHARGE_TEMPERATURE_LOW));
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(warning, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_HIGH, BitUtil.bit(warning, 5) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.DISCHARGE_TEMPERATURE_HIGH));
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_LOW, BitUtil.bit(warning, 6) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_TEMPERATURE_LOW, BitUtil.bit(warning, 7) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.DISCHARGE_TEMPERATURE_LOW));

        // warning 4
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        pack.alarms.put(Alarm.ENCASING_TEMPERATURE_HIGH, BitUtil.bit(warning, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.ENCASING_TEMPERATURE_HIGH, BitUtil.bit(warning, 1) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.ENCASING_TEMPERATURE_HIGH));
        pack.alarms.put(Alarm.ENCASING_TEMPERATURE_LOW, BitUtil.bit(warning, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.ENCASING_TEMPERATURE_LOW, BitUtil.bit(warning, 3) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.ENCASING_TEMPERATURE_LOW));
        pack.alarms.put(Alarm.PACK_TEMPERATURE_HIGH, BitUtil.bit(warning, 4) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.PACK_TEMPERATURE_HIGH, BitUtil.bit(warning, 5) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.PACK_TEMPERATURE_HIGH));

        // warning 5
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(warning, 0) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.CHARGE_CURRENT_HIGH, BitUtil.bit(warning, 1) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.CHARGE_CURRENT_HIGH));
        pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(warning, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.DISCHARGE_CURRENT_HIGH, BitUtil.bit(warning, 3) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.DISCHARGE_CURRENT_HIGH));
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(warning, 1) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.FAILURE_OTHER));
        pack.alarms.put(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION, BitUtil.bit(warning, 5) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(warning, 6) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.FAILURE_OTHER));
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(warning, 7) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.FAILURE_OTHER));

        // warning 6
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        pack.alarms.put(Alarm.CHARGE_VOLTAGE_HIGH, BitUtil.bit(warning, 0) ? AlarmLevel.ALARM : AlarmLevel.NONE);
        pack.alarms.put(Alarm.SOC_LOW, BitUtil.bit(warning, 2) ? AlarmLevel.WARNING : AlarmLevel.NONE);
        pack.alarms.put(Alarm.SOC_LOW, BitUtil.bit(warning, 3) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.SOC_LOW));
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(warning, 4) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.FAILURE_OTHER));
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(warning, 5) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.FAILURE_OTHER));
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(warning, 6) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.FAILURE_OTHER));
        pack.alarms.put(Alarm.FAILURE_OTHER, BitUtil.bit(warning, 7) ? AlarmLevel.ALARM : pack.alarms.get(Alarm.FAILURE_OTHER));

        // power status
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        // equalization status
        final byte balance1 = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        for (cellNo = 0; cellNo < 8; cellNo++) {
            pack.cellBalanceState[cellNo] = BitUtil.bit(warning, cellNo);
        }

        final byte balance2 = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        for (cellNo = 8; cellNo < 16; cellNo++) {
            pack.cellBalanceState[cellNo] = BitUtil.bit(warning, cellNo - 8);
        }

        pack.cellBalanceActive = balance1 + balance2 > 0;

        // system status
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());
        pack.dischargeMOSState = BitUtil.bit(warning, 0);
        pack.chargeMOSState = BitUtil.bit(warning, 1);
        final boolean sleep = BitUtil.bit(warning, 4);
        pack.chargeDischargeStatus = pack.chargeMOSState ? 1 : pack.dischargeMOSState ? 2 : sleep ? 3 : 0;

        // warning 7
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        if (warning != 0x00 && pack.alarms.get(Alarm.FAILURE_OTHER) != AlarmLevel.ALARM) {
            pack.alarms.put(Alarm.FAILURE_OTHER, AlarmLevel.WARNING);
        }

        // warning 8
        warning = ByteAsciiConverter.convertAsciiBytesToByte(data.get(), data.get());

        if (warning != 0x00 && pack.alarms.get(Alarm.FAILURE_OTHER) != AlarmLevel.ALARM) {
            pack.alarms.put(Alarm.FAILURE_OTHER, AlarmLevel.WARNING);
        }

    }


    private void setAlarm(final BatteryPack pack, final byte warning, final Alarm alarmLow, final Alarm alarmHigh, final AlarmLevel level) {
        switch (warning) {
            case 0x00:
                pack.alarms.put(alarmLow, AlarmLevel.NONE);
                pack.alarms.put(alarmLow, AlarmLevel.NONE);
            break;
            case 0x01:
                pack.alarms.put(alarmLow, level);
            break;
            case 0x02:
                pack.alarms.put(alarmLow, level);
            break;
            default:
            break;
        }
    }


    public static void main(final String[] args) {
        final String bmsId = new String(ByteAsciiConverter.convertByteToAsciiBytes((byte) 1));
        // insert the ascii byte bmsId into the request bytes
        final String command = "1203400456ABCEFE";
        // final String command = new String(new byte[] { 0x32, 0x30, 0x30, 0x30, 0x34, 0x36, 0x34,
        // 0x32, 0x45, 0x30, 0x30, 0x32, 0x30, 0x31 }, Charset.forName("ASCII"));
        short sum = 0;

        for (final byte b : command.getBytes()) {
            sum += b;
            System.out.print(String.format("%02X ", b));
        }

        System.out.println(String.format("%04X ", sum));
        short checksum = (short) (sum % 65536);
        System.out.println(String.format("%04X ", checksum));
        System.out.println(Integer.toBinaryString(checksum));
        checksum = (short) ~checksum;
        System.out.println(Integer.toBinaryString(checksum));
        System.out.println(String.format("%04X ", checksum));
        checksum++;
        System.out.println(String.format("%04X ", checksum));
    }
}
