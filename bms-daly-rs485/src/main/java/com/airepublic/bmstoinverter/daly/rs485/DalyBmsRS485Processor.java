package com.airepublic.bmstoinverter.daly.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Bms;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortProcessor;
import com.airepublic.bmstoinverter.core.Portname;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.rs485.RS485;
import com.airepublic.bmstoinverter.core.protocol.rs485.RS485Port;
import com.airepublic.bmstoinverter.daly.can.DalyCommand;
import com.airepublic.bmstoinverter.daly.can.DalyMessage;
import com.airepublic.bmstoinverter.daly.can.DalyMessageHandler;

import jakarta.inject.Inject;

@Bms
public class DalyBmsRS485Processor extends PortProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(DalyBmsRS485Processor.class);
    @Inject
    @RS485
    @Portname("daly.rs485.portname")
    private RS485Port port;
    @Inject
    private EnergyStorage energyStorage;
    private DalyMessageHandler messageHandler;
    private final Predicate<byte[]> checksumValidator = bytes -> {
        int checksum = 0;
        for (int i = 0; i < bytes.length - 1; i++) {
            checksum += (byte) Byte.toUnsignedInt(bytes[i]);
        }

        return bytes[12] == (byte) checksum;
    };

    @Override
    public void process() {
        if (!port.isOpen()) {
            // open RS485 port on Daly BMSes/interfaceboards(WNT)
            try {
                LOG.info("Opening " + port.getPortname() + ", number of battery packs = " + energyStorage.getBatteryPackCount() + " ...");
                messageHandler = new DalyMessageHandler(energyStorage);
                port.open();
                LOG.info("Opening RS485 port SUCCESSFUL");

            } catch (final IOException e) {
                LOG.error("Opening port FAILED!", e);
            }
        }

        if (port.isOpen()) {
            try {
                for (int i = 0; i < energyStorage.getBatteryPackCount(); i++) {
                    sendMessage(0x40 + i, DalyCommand.VOUT_IOUT_SOC); // 0x90
                    sendMessage(0x40 + i, DalyCommand.MIN_MAX_CELL_VOLTAGE); // 0x91
                    sendMessage(0x40 + i, DalyCommand.MIN_MAX_TEMPERATURE); // 0x92
                    sendMessage(0x40 + i, DalyCommand.DISCHARGE_CHARGE_MOS_STATUS); // 0x93
                    sendMessage(0x40 + i, DalyCommand.STATUS_INFO); // 0x94
                    sendMessage(0x40 + i, DalyCommand.CELL_VOLTAGES); // 0x95
                    sendMessage(0x40 + i, DalyCommand.CELL_TEMPERATURE); // 0x96
                    sendMessage(0x40 + i, DalyCommand.CELL_BALANCE_STATE); // 0x97
                    sendMessage(0x40 + i, DalyCommand.FAILURE_CODES); // 0x98
                }
            } catch (final IOException e) {
                LOG.error("Error requesting data!", e);
            }
        }
    }


    void sendMessage(final int address, final int cmdId) throws IOException {
        final ByteBuffer sendBuffer = prepareSendBuffer(address, cmdId);
        int framesToBeReceived = getResponseFrameCount(cmdId);
        final int frameCount = framesToBeReceived;

        // read frames until the requested frame is read
        do {
            port.sendFrame(sendBuffer);
            LOG.debug("SEND: {}", Port.printBuffer(sendBuffer));

            for (int i = 0; i < frameCount; i++) {
                ByteBuffer receiveBuffer;

                // repeat receiving as long as receiving command frames
                // do {
                receiveBuffer = port.receiveFrame(checksumValidator);
                LOG.debug("\t-> {}", Port.printBuffer(receiveBuffer));
                // } while (rxBuffer != null && rxBuffer.get(1) > 0x20);

                if (receiveBuffer.get(1) == (byte) (address - 0x40 + 1) && receiveBuffer.get(2) == (byte) cmdId) {
                    framesToBeReceived--;
                }

                messageHandler.handleMessage(convertRS485ByteBufferToDalyMessage(receiveBuffer));

                if (LOG.isDebugEnabled()) {
                    LOG.debug("RECEIVED: {}", Port.printBuffer(receiveBuffer));
                }
            }
        } while (framesToBeReceived > 0);

        LOG.warn("Command {} to BMS {} successfully sent and received!", HexFormat.of().withPrefix("0x").formatHex(new byte[] { (byte) cmdId }), address - 0x3F);
    }


    int getResponseFrameCount(final int cmdId) {
        if (cmdId == 0x95) {
            return Math.round((energyStorage.getBatteryPack(0).numberOfCells + 0.5f) % 3f);
        }

        return 1;
    }


    ByteBuffer prepareSendBuffer(final int address, final int cmdId) {
        final ByteBuffer sendBuffer = ByteBuffer.allocate(13);
        int checksum = 0;
        sendBuffer.put((byte) 0xA5);
        checksum += 0xA5;
        sendBuffer.put((byte) address);
        checksum += address;
        sendBuffer.put((byte) cmdId);
        checksum += cmdId;
        sendBuffer.put((byte) 0x08);
        checksum += 0x08;

        for (int i = 0; i < 8; i++) {
            sendBuffer.put((byte) 0);
        }
        sendBuffer.put((byte) checksum);

        sendBuffer.rewind();

        return sendBuffer;
    }


    public DalyMessage convertRS485ByteBufferToDalyMessage(final ByteBuffer buffer) throws IOException {
        final DalyMessage msg = new DalyMessage();
        msg.address = buffer.get(1);
        msg.dataId = buffer.get(2);
        final byte[] dataBytes = new byte[8];
        buffer.get(4, dataBytes);
        msg.data = ByteBuffer.wrap(dataBytes);

        return msg;
    }

}
