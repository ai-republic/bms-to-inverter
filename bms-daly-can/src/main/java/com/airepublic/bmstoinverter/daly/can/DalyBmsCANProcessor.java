package com.airepublic.bmstoinverter.daly.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.ServiceLoader;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Bms;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortProcessor;
import com.airepublic.bmstoinverter.core.Portname;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CAN;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.core.service.IMQTTProducerService;
import com.airepublic.bmstoinverter.daly.common.DalyCommand;
import com.airepublic.bmstoinverter.daly.common.DalyMessage;
import com.airepublic.bmstoinverter.daly.common.DalyMessageHandler;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

@Bms
public class DalyBmsCANProcessor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(DalyBmsCANProcessor.class);
    @Inject
    @CAN
    @Portname("bms.portname")
    private CANPort port;
    @Inject
    private EnergyStorage energyStorage;
    private DalyMessageHandler messageHandler;
    private final Predicate<byte[]> frameValidator = bytes -> true;
    private final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16);
    private final IMQTTProducerService mqttProducer = ServiceLoader.load(IMQTTProducerService.class).findFirst().orElse(null);;

    @Override
    @PostConstruct
    public void init() {
        super.init();

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


    @Override
    public void process() {
        if (!port.isOpen()) {
            // open CAN port on Daly BMSes/interfaceboards(WNT)
            try {
                messageHandler = new DalyMessageHandler(energyStorage);
                LOG.info("Opening " + port.getPortname() + ", number of battery packs = " + energyStorage.getBatteryPackCount() + " ...");
                port.open();
                LOG.info("Opening CAN port SUCCESSFUL");

            } catch (final Throwable e) {
                LOG.error("Opening port FAILED!", e);
            }
        }

        if (port.isOpen()) {
            try {
                // LOG.info("SEND:");
                // port.sendExtendedFrame(prepareFrameBuffer(1, 0x90));
                // LOG.info("RECEIVED: {}", Port.printBuffer(port.receiveFrame(frameValidator)));

                for (int bmsAddress = 1; bmsAddress <= energyStorage.getBatteryPackCount(); bmsAddress++) {
                    sendMessage(bmsAddress, DalyCommand.MIN_MAX_PACK_VOLTAGE); // 0x5A
                    sendMessage(bmsAddress, DalyCommand.MAX_PACK_DISCHARGE_CHARGE_CURRENT); // 0x5B
                    sendMessage(bmsAddress, DalyCommand.VOUT_IOUT_SOC); // 0x90
                    sendMessage(bmsAddress, DalyCommand.MIN_MAX_CELL_VOLTAGE); // 0x91
                    sendMessage(bmsAddress, DalyCommand.MIN_MAX_TEMPERATURE); // 0x92
                    sendMessage(bmsAddress, DalyCommand.DISCHARGE_CHARGE_MOS_STATUS); // 0x93
                    sendMessage(bmsAddress, DalyCommand.STATUS_INFO); // 0x94
                    sendMessage(bmsAddress, DalyCommand.CELL_VOLTAGES); // 0x95
                    sendMessage(bmsAddress, DalyCommand.CELL_TEMPERATURE); // 0x96
                    sendMessage(bmsAddress, DalyCommand.CELL_BALANCE_STATE); // 0x97
                    sendMessage(bmsAddress, DalyCommand.FAILURE_CODES); // 0x98
                }

                if (mqttProducer != null) {
                    // send energystorage data to MQTT broker
                    mqttProducer.sendMessage(energyStorage.toJson());
                }
            } catch (final Throwable e) {
                LOG.error("Error requesting data!", e);
            }
        }
    }


    void sendMessage(final int address, final int cmdId) throws IOException {
        prepareFrameBuffer(address, cmdId);
        int framesToBeReceived = getResponseFrameCount(cmdId);
        final int frameCount = framesToBeReceived;
        int skip = 20;

        LOG.debug("SENDING: {}", Port.printBuffer(sendFrame));
        port.sendExtendedFrame(sendFrame);

        // read frames until the requested frame is read
        LOG.debug("Reading address 0x{}, command 0x{}", Integer.toHexString(address), Integer.toHexString(cmdId));
        do {
            skip--;
            for (int i = 0; i < frameCount; i++) {
                final ByteBuffer receiveFrame = port.receiveFrame(frameValidator);

                LOG.debug("RECEIVED: {}", Port.printBuffer(receiveFrame));

                final byte receiver = (byte) (receiveFrame.getInt(0) >> 8 & 0x000000FF);
                final byte command = (byte) (receiveFrame.getInt(0) >> 16 & 0x000000FF);

                if (receiver == (byte) 0x40 && command == (byte) cmdId) {
                    framesToBeReceived--;
                }

                messageHandler.handleMessage(convertCANByteBufferToDalyMessage(receiveFrame));
            }
        } while (framesToBeReceived > 0 & skip > 0);
        LOG.debug("Command 0x{} to BMS {} successfully sent and received!", HexFormat.of().toHexDigits(cmdId), address);
    }


    int getResponseFrameCount(final int cmdId) {
        if (cmdId == 0x95) {
            return Math.round(energyStorage.getBatteryPack(0).numberOfCells / 3f + 0.5f);
        }

        return 1;
    }


    ByteBuffer prepareFrameBuffer(final int address, final int cmdId) {

        // frame id
        sendFrame.put((byte) 0x18)
                .put((byte) cmdId)
                .put((byte) address)
                .put((byte) 0x40);

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // data
        for (int i = 0; i < 8; i++) {
            sendFrame.put((byte) 0);
        }

        sendFrame.rewind();

        return sendFrame;
    }


    public DalyMessage convertCANByteBufferToDalyMessage(final ByteBuffer buffer) {
        buffer.rewind();

        final DalyMessage msg = new DalyMessage();
        final int frameId = buffer.getInt(0);
        msg.address = (byte) (frameId & 0x000000FF);
        msg.dataId = (byte) (frameId >> 16 & 0x000000FF);
        // msg.length = buffer.get(4);

        buffer.get(); // flags
        buffer.getShort(); // skip the next 2 bytes

        final byte[] dataBytes = new byte[buffer.get(4)];
        buffer.get(8, dataBytes);
        msg.data = ByteBuffer.wrap(dataBytes);

        if (LOG.isDebugEnabled()) {
            LOG.debug("DALY Message: frameId= " + Integer.toHexString(frameId)
                    + ", address=" + HexFormat.of().toHexDigits(msg.address)
                    + ", dataId=" + HexFormat.of().toHexDigits(msg.dataId)
                    + ", data=" + HexFormat.of().formatHex(dataBytes));
        }

        return msg;
    }

}
