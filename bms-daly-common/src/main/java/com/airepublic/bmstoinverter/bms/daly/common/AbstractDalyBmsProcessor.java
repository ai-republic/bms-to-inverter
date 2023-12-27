package com.airepublic.bmstoinverter.bms.daly.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Bms;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CAN;
import com.airepublic.bmstoinverter.core.protocol.rs485.RS485;

import jakarta.inject.Inject;

/**
 * An abstraction for the Daly {@link Bms} since the {@link RS485} and {@link CAN} communication is
 * very similar.
 */
public abstract class AbstractDalyBmsProcessor implements Bms {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractDalyBmsProcessor.class);
    @Inject
    private EnergyStorage energyStorage;
    @Inject
    private DalyMessageHandler messageHandler;
    private final byte[] requestData = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
    private final int calibrationCounter = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
    public void initialize() {
        try {
            clearBuffers();

            for (int bmsNo = 0; bmsNo < energyStorage.getBatteryPackCount(); bmsNo++) {
                sendMessage(bmsNo, DalyCommand.READ_RATED_CAPACITY_CELL_VOLTAGE, requestData); // 0x50
                sendMessage(bmsNo, DalyCommand.READ_BATTERY_TYPE_INFO, requestData); // 0x53
                sendMessage(bmsNo, DalyCommand.READ_MIN_MAX_PACK_VOLTAGE, requestData); // 0x5A
                sendMessage(bmsNo, DalyCommand.READ_MAX_PACK_DISCHARGE_CHARGE_CURRENT, requestData); // 0x5B
            }
        } catch (final Throwable t) {
            LOG.error("Failed to initialize BMS!", t);
        }
    }


    @Override
    public void process(final Runnable callback) {
        try {
            LOG.info("---------------------------------> Thread " + Thread.currentThread().getId());
            clearBuffers();

            for (int bmsNo = 0; bmsNo < energyStorage.getBatteryPackCount(); bmsNo++) {
                sendMessage(bmsNo, DalyCommand.READ_VOUT_IOUT_SOC, requestData); // 0x90
                sendMessage(bmsNo, DalyCommand.READ_MIN_MAX_CELL_VOLTAGE, requestData); // 0x91
                sendMessage(bmsNo, DalyCommand.READ_MIN_MAX_TEMPERATURE, requestData); // 0x92
                sendMessage(bmsNo, DalyCommand.READ_DISCHARGE_CHARGE_MOS_STATUS, requestData); // 0x93
                sendMessage(bmsNo, DalyCommand.READ_STATUS_INFO, requestData); // 0x94
                sendMessage(bmsNo, DalyCommand.READ_CELL_VOLTAGES, requestData); // 0x95
                sendMessage(bmsNo, DalyCommand.READ_CELL_TEMPERATURE, requestData); // 0x96
                sendMessage(bmsNo, DalyCommand.READ_CELL_BALANCE_STATE, requestData); // 0x97
                sendMessage(bmsNo, DalyCommand.READ_FAILURE_CODES, requestData); // 0x98
            }

            // autoCalibrateSOC();
        } catch (final NoDataAvailableException e) {
            return;
        } catch (final Throwable e) {
            LOG.error("Error requesting data!", e);
            return;
        }

        try {
            callback.run();
        } catch (final Throwable e) {
            LOG.error("BMS process callback threw an exception!", e);
        }
    }


    /**
     * Calibrate the SOC of all {@link BatteryPack} according to their maximum and minimum voltage
     * compared to the actual voltage.
     */
    protected void autoCalibrateSOC() {
        for (int bmsNo = 0; bmsNo < energyStorage.getBatteryPackCount(); bmsNo++) {
            final BatteryPack battery = energyStorage.getBatteryPack(bmsNo);
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
            final int bmsNum = bmsNo;
            do {
                try {
                    final Future<List<ByteBuffer>> future = executor.submit(() -> {

                        LOG.info("calibrate request (SOC " + calculatedSOC + "): " + HexFormat.of().withUpperCase().withDelimiter(", 0x").formatHex(data));
                        final List<ByteBuffer> result = sendMessage(bmsNum, DalyCommand.WRITE_RTC_AND_SOC, data);
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

    }


    /**
     * Sends the specified {@link DalyCommand} and frame data to the specified BMS.
     *
     * @param port the {@link Port} to use
     * @param bmsNo the bms to send to
     * @param cmd the {@link DalyCommand}
     * @param data the frame data
     * @return
     * @throws IOException
     */
    protected abstract List<ByteBuffer> sendMessage(final int bmsNo, final DalyCommand cmd, final byte[] data) throws IOException;


    /**
     * Gets the expected number of response frames for the specified {@link DalyCommand}.
     *
     * @param cmd the {@link DalyCommand}
     * @return the expected number of response frames
     */
    @SuppressWarnings("resource")
    protected int getResponseFrameCount(final DalyCommand cmd) {
        switch (cmd.id) {
            case 0x21:
                return 2;
            case 0x95:
                return Math.round(energyStorage.getBatteryPack(0).numberOfCells / 3f + 0.5f);
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


    /**
     * Clears any buffers or queues on all associated ports to restart communication.
     */
    protected void clearBuffers() {
        for (final BatteryPack pack : energyStorage.getBatteryPacks()) {
            pack.port.clearBuffers();
        }
    }
}
