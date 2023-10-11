package com.airepublic.bmstoinverter.daly.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Bms;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.Portname;
import com.airepublic.bmstoinverter.core.protocol.can.CAN;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.daly.common.AbstractDalyBmsProcessor;
import com.airepublic.bmstoinverter.daly.common.DalyMessage;

import jakarta.inject.Inject;

@Bms
public class DalyBmsCANProcessor extends AbstractDalyBmsProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(DalyBmsCANProcessor.class);
    @Inject
    @CAN
    @Portname("bms.portname")
    private CANPort port;
    private final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16);

    @Override
    public Port getPort() {
        return port;
    }


    @Override
    protected void sendMessage(final int bmsNo, final int cmdId, final byte[] data) throws IOException {
        final ByteBuffer sendFrame = prepareSendFrame(bmsNo, cmdId, data);
        int framesToBeReceived = getResponseFrameCount(cmdId);
        final int frameCount = framesToBeReceived;
        int skip = 20;

        LOG.debug("SEND: {}", Port.printBuffer(sendFrame));
        send(sendFrame);

        // read frames until the requested frame is read
        LOG.debug("Reading address 0x{}, command 0x{}", Integer.toHexString(bmsNo), Integer.toHexString(cmdId));
        do {
            skip--;

            for (int i = 0; i < frameCount; i++) {
                final ByteBuffer receiveFrame = port.receiveFrame(getValidator());

                LOG.debug("RECEIVED: {}", Port.printBuffer(receiveFrame));

                final byte receiver = (byte) (receiveFrame.getInt(0) >> 8 & 0x000000FF);
                final byte command = (byte) (receiveFrame.getInt(0) >> 16 & 0x000000FF);

                if (receiver == (byte) 0x40 && command == (byte) cmdId) {
                    framesToBeReceived--;
                }

                getMessageHandler().handleMessage(convertReceiveFrameToDalyMessage(receiveFrame));

                if (LOG.isDebugEnabled()) {
                    LOG.debug("RECEIVED: {}", Port.printBuffer(receiveFrame));
                }
            }
        } while (framesToBeReceived > 0 & skip > 0);

        LOG.debug("Command 0x{} to BMS {} successfully sent and received!", HexFormat.of().toHexDigits(cmdId), bmsNo);
    }


    @Override
    protected ByteBuffer prepareSendFrame(final int address, final int cmdId, final byte[] data) {
        sendFrame.rewind();

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
        msg.dataId = (byte) (frameId >> 16 & 0x000000FF);
        // msg.length = buffer.get(4);

        buffer.get(); // flags
        buffer.getShort(); // skip the next 2 bytes

        final byte[] dataBytes = new byte[buffer.get(4)];
        buffer.get(8, dataBytes);
        msg.data = ByteBuffer.wrap(dataBytes);

        if (LOG.isDebugEnabled()) {
            LOG.info("DALY Message: frameId= " + Integer.toHexString(frameId)
                    + ", address=" + HexFormat.of().toHexDigits(msg.address)
                    + ", dataId=" + HexFormat.of().toHexDigits(msg.dataId)
                    + ", data=" + HexFormat.of().formatHex(dataBytes));
        }

        return msg;
    }


    @Override
    protected Predicate<byte[]> getValidator() {
        return t -> true;
    }


    @Override
    protected void send(final ByteBuffer frame) throws IOException {
        port.sendExtendedFrame(frame);
    }
}
