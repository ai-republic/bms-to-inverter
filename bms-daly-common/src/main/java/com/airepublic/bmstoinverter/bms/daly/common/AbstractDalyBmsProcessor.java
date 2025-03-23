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
package com.airepublic.bmstoinverter.bms.daly.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.NoDataAvailableException;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.TooManyInvalidFramesException;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.HexUtil;

import jakarta.inject.Inject;

/**
 * An abstraction for the Daly {@link BMS} since the RS485 and CAN communication is very similar.
 */
public abstract class AbstractDalyBmsProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractDalyBmsProcessor.class);
    private final static int BATTERY_ID = 0;
    @Inject
    private DalyMessageHandler messageHandler;
    private final byte[] requestData = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
    private final int calibrationCounter = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean initialRound = true;

    /**
     * Gets the {@link DalyMessageHandler} to process the {@link DalyMessage} converted by the
     * individual {@link PortProcessor}.
     *
     * @return the {@link DalyMessageHandler}
     */
    public DalyMessageHandler getMessageHandler() {
        return messageHandler;
    }


    @Override
    protected void collectData(final Port port) throws IOException, TooManyInvalidFramesException, NoDataAvailableException {

        if (initialRound) {
            try {
                sendMessage(port, DalyCommand.READ_RATED_CAPACITY_CELL_VOLTAGE, requestData); // 0x50
                sendMessage(port, DalyCommand.READ_BATTERY_TYPE_INFO, requestData); // 0x53
                sendMessage(port, DalyCommand.READ_MIN_MAX_PACK_VOLTAGE, requestData); // 0x5A
                sendMessage(port, DalyCommand.READ_MAX_PACK_DISCHARGE_CHARGE_CURRENT, requestData); // 0x5B

                initialRound = false;
            } catch (final Throwable t) {
                LOG.error("Failed to initialize BMS!", t);
            }
        }

        sendMessage(port, DalyCommand.READ_VOUT_IOUT_SOC, requestData); // 0x90
        sendMessage(port, DalyCommand.READ_MIN_MAX_CELL_VOLTAGE, requestData); // 0x91
        sendMessage(port, DalyCommand.READ_MIN_MAX_TEMPERATURE, requestData); // 0x92
        sendMessage(port, DalyCommand.READ_DISCHARGE_CHARGE_MOS_STATUS, requestData); // 0x93
        sendMessage(port, DalyCommand.READ_STATUS_INFO, requestData); // 0x94
        sendMessage(port, DalyCommand.READ_CELL_VOLTAGES, requestData); // 0x95
        sendMessage(port, DalyCommand.READ_CELL_TEMPERATURE, requestData); // 0x96
        sendMessage(port, DalyCommand.READ_CELL_BALANCE_STATE, requestData); // 0x97
        sendMessage(port, DalyCommand.READ_FAILURE_CODES, requestData); // 0x98
    }


    /**
     * Calibrate the SOC of all {@link BatteryPack} according to their maximum and minimum voltage
     * compared to the actual voltage.
     * 
     * @param port the {@link Port} of the {@link BMS}
     */
    protected void autoCalibrateSOC(final Port port) {
        final BatteryPack battery = getBatteryPack(BATTERY_ID);
        final int calculatedSOC = (int) (((float) battery.packVoltage - battery.minPackVoltageLimit) * 100 / (battery.maxPackVoltageLimit - battery.minPackVoltageLimit) * 10);
        final byte[] data = new byte[8];
        final LocalDateTime date = LocalDateTime.now();
        final String yearStr = String.valueOf(date.getYear());
        data[0] = Integer.valueOf(yearStr.substring(yearStr.length() - 2)).byteValue();
        data[1] = (byte) date.getMonthValue();
        data[2] = (byte) date.getDayOfMonth();
        data[3] = (byte) date.getHour();
        data[4] = (byte) date.getMinute();
        data[5] = (byte) date.getSecond();
        data[6] = (byte) (calculatedSOC >>> 8);
        data[7] = (byte) calculatedSOC;

        boolean retry = false;
        int retries = 0;

        do {
            try {
                final Future<List<ByteBuffer>> future = executor.submit(() -> {

                    LOG.info("calibrate request (SOC " + calculatedSOC + "): " + HexUtil.formatHex(data));
                    final List<ByteBuffer> result = sendMessage(port, DalyCommand.WRITE_RTC_AND_SOC, data);
                    LOG.info("calibrate result: " + Port.printBuffer(result.get(0)));
                    return result;
                });

                future.get(500, TimeUnit.MILLISECONDS);
            } catch (final Exception e) {
                LOG.warn("Auto-calibration timed out");
                retries++;
                retry = true;
            }
        } while (retry && retries <= 3);

    }


    /**
     * Sends the specified {@link DalyCommand} and frame data to the specified BMS.
     *
     * @param port the {@link Port} to use
     * @param cmd the {@link DalyCommand}
     * @param data the frame data
     * @return
     * @throws IOException
     */
    protected abstract List<ByteBuffer> sendMessage(Port port, final DalyCommand cmd, final byte[] data) throws IOException, NoDataAvailableException, TooManyInvalidFramesException;


    /**
     * Gets the expected number of response frames for the specified {@link DalyCommand}.
     *
     * @param cmd the {@link DalyCommand}
     * @return the expected number of response frames
     */
    protected int getResponseFrameCount(final DalyCommand cmd) {
        switch (cmd.id) {
            case 0x21:
                return 2;
            case 0x95:
                return Math.round(getBatteryPack(BATTERY_ID).numberOfCells / 3f + 0.5f);
        }

        return 1;
    }


    /**
     * Prepared the send frame according to the specified bms address, {@link DalyCommand} and frame
     * data.
     *
     * @param address the bms address
     * @param cmd the {@link DalyCommand}
     * @param data the frame data
     * @return the frame {@link ByteBuffer}
     */
    protected abstract ByteBuffer prepareSendFrame(final int address, final DalyCommand cmd, final byte[] data);


    /**
     * Converts the received frame {@link ByteBuffer} to a {@link DalyMessage}.
     *
     * @param buffer the frame {@link ByteBuffer}
     * @return the {@link DalyMessage}
     */
    protected abstract DalyMessage convertReceiveFrameToDalyMessage(final ByteBuffer buffer);

}
