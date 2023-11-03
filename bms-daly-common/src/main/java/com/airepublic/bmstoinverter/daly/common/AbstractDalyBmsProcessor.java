package com.airepublic.bmstoinverter.daly.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Bms;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortProcessor;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.inject.Inject;

/**
 * An abstraction for the {@link PortProcessor} for the Daly {@link Bms} since the RS485 and CAN
 * communication is very similar.
 */
public abstract class AbstractDalyBmsProcessor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractDalyBmsProcessor.class);
    @Inject
    private EnergyStorage energyStorage;
    @Inject
    private DalyMessageHandler messageHandler;
    private final byte[] requestData = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };

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
    public void process() {
        for (final Port port : getPorts()) {
            if (!port.isOpen()) {
                // open port on Daly BMSes/interfaceboards(WNT)
                try {
                    LOG.info("Opening " + port.getPortname() + ", number of battery packs = " + energyStorage.getBatteryPackCount() + " ...");
                    port.open();
                    LOG.info("Opening port {} SUCCESSFUL", port);

                } catch (final Throwable e) {
                    LOG.error("Opening port {} FAILED!", port, e);
                }
            }

            if (port.isOpen()) {
                try {
                    for (int bmsNo = 1; bmsNo <= energyStorage.getBatteryPackCount(); bmsNo++) {
                        sendMessage(port, bmsNo, DalyCommand.READ_RATED_CAPACITY_CELL_VOLTAGE, requestData); // 0x50
                        sendMessage(port, bmsNo, DalyCommand.READ_BATTERY_TYPE_INFO, requestData); // 0x53
                        sendMessage(port, bmsNo, DalyCommand.READ_MIN_MAX_PACK_VOLTAGE, requestData); // 0x5A
                        sendMessage(port, bmsNo, DalyCommand.READ_MAX_PACK_DISCHARGE_CHARGE_CURRENT, requestData); // 0x5B
                        sendMessage(port, bmsNo, DalyCommand.READ_VOUT_IOUT_SOC, requestData); // 0x90
                        sendMessage(port, bmsNo, DalyCommand.READ_MIN_MAX_CELL_VOLTAGE, requestData); // 0x91
                        sendMessage(port, bmsNo, DalyCommand.READ_MIN_MAX_TEMPERATURE, requestData); // 0x92
                        sendMessage(port, bmsNo, DalyCommand.READ_DISCHARGE_CHARGE_MOS_STATUS, requestData); // 0x93
                        sendMessage(port, bmsNo, DalyCommand.READ_STATUS_INFO, requestData); // 0x94
                        sendMessage(port, bmsNo, DalyCommand.READ_CELL_VOLTAGES, requestData); // 0x95
                        sendMessage(port, bmsNo, DalyCommand.READ_CELL_TEMPERATURE, requestData); // 0x96
                        sendMessage(port, bmsNo, DalyCommand.READ_CELL_BALANCE_STATE, requestData); // 0x97
                        sendMessage(port, bmsNo, DalyCommand.READ_FAILURE_CODES, requestData); // 0x98
                    }

                    autoCalibrateSOC(port);
                } catch (final Throwable e) {
                    LOG.error("Error requesting data!", e);
                }
            }
        }
    }


    /**
     * Calibrate the SOC of all {@link BatteryPack} according to their maximum and minimum voltage
     * compared to the actual voltage.
     *
     * @param port the {@link Port} to use
     * @throws IOException if setting of SOC failed
     */
    protected void autoCalibrateSOC(final Port port) throws IOException {
        for (int bmsAddress = 1; bmsAddress <= energyStorage.getBatteryPackCount(); bmsAddress++) {
            final BatteryPack battery = energyStorage.getBatteryPack(bmsAddress - 1);
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

            LOG.info("calibrate request (SOC " + calculatedSOC + "): " + HexFormat.of().withUpperCase().withDelimiter(", 0x").formatHex(data));
            final List<ByteBuffer> result = sendMessage(port, bmsAddress, DalyCommand.WRITE_RTC_AND_SOC, data);
            LOG.info("calibrate result: " + Port.printBuffer(result.get(0)));

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
    protected abstract List<ByteBuffer> sendMessage(Port port, final int bmsNo, final DalyCommand cmd, final byte[] data) throws IOException;


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
}
