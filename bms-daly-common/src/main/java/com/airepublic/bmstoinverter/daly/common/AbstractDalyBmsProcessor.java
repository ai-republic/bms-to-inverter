package com.airepublic.bmstoinverter.daly.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortProcessor;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.service.IMQTTProducerService;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public abstract class AbstractDalyBmsProcessor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractDalyBmsProcessor.class);
    @Inject
    private EnergyStorage energyStorage;
    private DalyMessageHandler messageHandler;
    private final IMQTTProducerService mqttProducer = ServiceLoader.load(IMQTTProducerService.class).findFirst().orElse(null);
    private final byte[] requestData = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
    private Port port;

    @Override
    @PostConstruct
    public void init() {
        super.init();
        port = getPort();

        if (mqttProducer != null) {
            final String locator = System.getProperty("mqtt.locator");
            final String topic = System.getProperty("mqtt.topic");

            try {
                mqttProducer.connect(locator, topic);
            } catch (final Exception e) {
                LOG.error("Could not connect MQTT producer client at {} on topic {}", locator, topic, e);
            }
        }

    }


    public DalyMessageHandler getMessageHandler() {
        return messageHandler;
    }


    @Override
    public void process() {
        if (!port.isOpen()) {
            // open port on Daly BMSes/interfaceboards(WNT)
            try {
                LOG.info("Opening " + port.getPortname() + ", number of battery packs = " + energyStorage.getBatteryPackCount() + " ...");
                messageHandler = new DalyMessageHandler(energyStorage);
                port.open();
                LOG.info("Opening port {} SUCCESSFUL", port);

            } catch (final Throwable e) {
                LOG.error("Opening port {} FAILED!", port, e);
            }
        }

        if (port.isOpen()) {
            try {
                for (int bmsNo = 1; bmsNo <= energyStorage.getBatteryPackCount(); bmsNo++) {
                    sendMessage(bmsNo, DalyCommand.READ_MIN_MAX_PACK_VOLTAGE, requestData); // 0x5A
                    sendMessage(bmsNo, DalyCommand.READ_MAX_PACK_DISCHARGE_CHARGE_CURRENT, requestData); // 0x5B
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

                if (mqttProducer != null) {
                    // send energystorage data to MQTT broker
                    mqttProducer.sendMessage(energyStorage.toJson());
                }

                autoCalibrateSOC();
            } catch (final Throwable e) {
                LOG.error("Error requesting data!", e);
            }
        }
    }


    protected void autoCalibrateSOC() throws IOException {
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
            final List<ByteBuffer> result = sendMessage(bmsAddress, DalyCommand.WRITE_RTC_AND_SOC, data);
            LOG.info("calibrate result: " + Port.printBuffer(result.get(0)));

        }
    }


    protected abstract List<ByteBuffer> sendMessage(final int bmsNo, final int cmdId, final byte[] data) throws IOException;


    protected int getResponseFrameCount(final int cmdId) {
        switch (cmdId) {
            case 0x21:
                return 2;
            case 0x95:
                return Math.round(energyStorage.getBatteryPack(0).numberOfCells / 3f + 0.5f);
        }

        return 1;
    }


    protected abstract ByteBuffer prepareSendFrame(final int address, final int cmdId, final byte[] data);


    protected abstract DalyMessage convertReceiveFrameToDalyMessage(final ByteBuffer buffer);
}
