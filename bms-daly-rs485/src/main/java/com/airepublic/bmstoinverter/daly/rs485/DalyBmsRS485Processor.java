package com.airepublic.bmstoinverter.daly.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortType;
import com.airepublic.bmstoinverter.core.Protocol;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.daly.common.AbstractDalyBmsProcessor;
import com.airepublic.bmstoinverter.daly.common.DalyCommand;
import com.airepublic.bmstoinverter.daly.common.DalyMessage;

import jakarta.inject.Inject;

/**
 * The class to handle RS485 messages from a Daly BMS.
 */
@PortType(Protocol.RS485)
public class DalyBmsRS485Processor extends AbstractDalyBmsProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractDalyBmsProcessor.class);
    private final ByteBuffer sendFrame = ByteBuffer.allocate(13);
    @Inject
    private EnergyStorage energyStorage;
    private final Predicate<byte[]> validator = bytes -> {
        int checksum = 0;
        for (int i = 0; i < bytes.length - 1; i++) {
            checksum += (byte) Byte.toUnsignedInt(bytes[i]);
        }

        return bytes[12] == (byte) checksum;
    };

    @Override
    protected List<ByteBuffer> sendMessage(final int bmsNo, final DalyCommand cmd, final byte[] data) throws IOException {
        final int address = bmsNo + 0x40;
        final ByteBuffer sendBuffer = prepareSendFrame(address, cmd, data);
        int framesToBeReceived = getResponseFrameCount(cmd);
        final int frameCount = framesToBeReceived;
        final List<ByteBuffer> readBuffers = new ArrayList<>();
        final Port port = energyStorage.getBatteryPack(bmsNo).port;

        // read frames until the requested frame is read
        do {
            port.sendFrame(sendBuffer);
            LOG.debug("SEND: {}", Port.printBuffer(sendBuffer));

            for (int i = 0; i < frameCount; i++) {
                final ByteBuffer receiveBuffer = port.receiveFrame(validator);

                LOG.debug("RECEIVED: {}", Port.printBuffer(receiveBuffer));

                if (receiveBuffer.get(1) == (byte) (address - 0x40 + 1) && receiveBuffer.get(2) == (byte) cmd.id) {
                    framesToBeReceived--;
                    readBuffers.add(receiveBuffer);
                }

                getMessageHandler().handleMessage(convertReceiveFrameToDalyMessage(receiveBuffer));
            }
        } while (framesToBeReceived > 0);

        LOG.warn("Command {} to BMS {} successfully sent and received!", HexFormat.of().withPrefix("0x").formatHex(new byte[] { (byte) cmd.id }), address - 0x3F);

        return readBuffers;
    }


    @Override
    protected ByteBuffer prepareSendFrame(final int address, final DalyCommand cmd, final byte[] data) {
        sendFrame.rewind();

        int checksum = 0;
        sendFrame.put((byte) 0xA5);
        checksum += 0xA5;
        sendFrame.put((byte) address);
        checksum += address;
        sendFrame.put((byte) cmd.id);
        checksum += cmd.id;
        sendFrame.put((byte) 0x08);
        checksum += 0x08;

        for (final byte element : data) {
            sendFrame.put(element);
            checksum += element;
        }

        sendFrame.put((byte) checksum);

        sendFrame.rewind();

        return sendFrame;
    }


    @Override
    protected DalyMessage convertReceiveFrameToDalyMessage(final ByteBuffer buffer) {
        final DalyMessage msg = new DalyMessage();
        msg.address = buffer.get(1);
        msg.cmd = DalyCommand.valueOf(Byte.toUnsignedInt(buffer.get(2)));
        final byte[] dataBytes = new byte[8];
        buffer.get(4, dataBytes);
        msg.data = ByteBuffer.wrap(dataBytes);
        msg.data.rewind();

        return msg;
    }

}
