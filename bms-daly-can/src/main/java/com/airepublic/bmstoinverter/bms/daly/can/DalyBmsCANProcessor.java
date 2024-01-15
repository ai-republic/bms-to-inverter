package com.airepublic.bmstoinverter.bms.daly.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.bms.daly.common.AbstractDalyBmsProcessor;
import com.airepublic.bmstoinverter.bms.daly.common.DalyCommand;
import com.airepublic.bmstoinverter.bms.daly.common.DalyMessage;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortType;
import com.airepublic.bmstoinverter.core.Protocol;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import jakarta.inject.Inject;

/**
 * The class to handle CAN messages from a Daly BMS.
 */
@PortType(Protocol.CAN)
public class DalyBmsCANProcessor extends AbstractDalyBmsProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(DalyBmsCANProcessor.class);
    private final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);
    @Inject
    private EnergyStorage energyStorage;

    @Override
    protected List<ByteBuffer> sendMessage(final int bmsNo, final DalyCommand cmd, final byte[] data) throws IOException {
        final ByteBuffer sendFrame = prepareSendFrame(bmsNo + 1, cmd, data);
        int framesToBeReceived = getResponseFrameCount(cmd);
        final int frameCount = framesToBeReceived;
        int skip = 20;
        final List<ByteBuffer> readBuffers = new ArrayList<>();
        @SuppressWarnings("resource")
        final CANPort port = (CANPort) energyStorage.getBatteryPack(bmsNo).port;

        LOG.debug("SEND: {}", Port.printBuffer(sendFrame));
        port.sendExtendedFrame(sendFrame);

        // read frames until the requested frame is read
        do {
            skip--;

            for (int i = 0; i < frameCount; i++) {
                final ByteBuffer receiveFrame = port.receiveFrame(t -> true);

                LOG.debug("RECEIVED: {}", Port.printBuffer(receiveFrame));

                final byte receiver = (byte) (receiveFrame.getInt(0) >> 8 & 0x000000FF);
                final byte command = (byte) (receiveFrame.getInt(0) >> 16 & 0x000000FF);

                if (receiver == (byte) 0x40 && command == (byte) cmd.id) {
                    readBuffers.add(receiveFrame);
                    framesToBeReceived--;
                }

                final DalyMessage dalyMsg = convertReceiveFrameToDalyMessage(receiveFrame);

                if (dalyMsg != null) {
                    getMessageHandler().handleMessage(dalyMsg);
                } else {
                    LOG.warn("Message could not be interpreted " + Port.printBuffer(receiveFrame));
                    return readBuffers;
                }
            }
        } while (framesToBeReceived > 0 & skip > 0);

        LOG.debug("Command 0x{} to BMS {} successfully sent and received!", HexFormat.of().toHexDigits(cmd.id), bmsNo + 1);
        return readBuffers;
    }


    @Override
    protected ByteBuffer prepareSendFrame(final int address, final DalyCommand cmd, final byte[] data) {
        sendFrame.rewind();

        // frame id
        long frameId = 0L;
        frameId = 0x18;
        frameId = frameId << 8;
        frameId += (byte) cmd.id;
        frameId = frameId << 8;
        frameId += (byte) address;
        frameId = frameId << 8;
        frameId += 0x40;
        sendFrame.putInt((int) frameId);

        // sendFrame.put((byte) 0x40);
        // sendFrame.put((byte) address);
        // sendFrame.put((byte) cmd.id);
        // sendFrame.put((byte) 0x18);

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // data
        for (final byte element : data) {
            sendFrame.put(element);
        }

        sendFrame.rewind();

        return sendFrame;
    }


    @Override
    public DalyMessage convertReceiveFrameToDalyMessage(final ByteBuffer buffer) {
        buffer.rewind();

        final DalyMessage msg = new DalyMessage();
        final int frameId = buffer.getInt(0);
        msg.address = (byte) (frameId & 0x000000FF);
        msg.cmd = DalyCommand.valueOf(frameId >> 16 & 0x000000FF);

        if (msg.cmd == null) {
            LOG.error("Received unknown command: " + Byte.toUnsignedInt(buffer.get(2)));
            return null;
        }

        final byte[] dataBytes = new byte[buffer.get(4)];
        buffer.get(8, dataBytes);
        msg.data = ByteBuffer.wrap(dataBytes);

        if (LOG.isDebugEnabled()) {
            LOG.info("DALY Message: frameId= " + Integer.toHexString(frameId)
                    + ", address=" + HexFormat.of().toHexDigits(msg.address)
                    + ", dataId=" + HexFormat.of().toHexDigits(msg.cmd.id)
                    + ", data=" + HexFormat.of().formatHex(dataBytes));
        }

        return msg;
    }
}
