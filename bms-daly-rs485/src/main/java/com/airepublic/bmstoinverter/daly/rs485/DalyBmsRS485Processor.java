package com.airepublic.bmstoinverter.daly.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Bms;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.Portname;
import com.airepublic.bmstoinverter.core.protocol.rs485.RS485;
import com.airepublic.bmstoinverter.daly.common.AbstractDalyBmsProcessor;
import com.airepublic.bmstoinverter.daly.common.DalyMessage;

import jakarta.inject.Inject;

@Bms
public class DalyBmsRS485Processor extends AbstractDalyBmsProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractDalyBmsProcessor.class);
    @Inject
    @RS485
    @Portname("bms.portname")
    private Port port;
    final ByteBuffer sendFrame = ByteBuffer.allocate(13);

    @Override
    public Port getPort() {
        return port;
    }


    @Override
    protected void sendMessage(final int bmsNo, final int cmdId, final byte[] data) throws IOException {
        final int address = bmsNo + 0x3F;
        final ByteBuffer sendBuffer = prepareSendFrame(address, cmdId, data);
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
                receiveBuffer = port.receiveFrame(getValidator());
                LOG.debug("\t-> {}", Port.printBuffer(receiveBuffer));
                // } while (rxBuffer != null && rxBuffer.get(1) > 0x20);

                if (receiveBuffer.get(1) == (byte) (address - 0x40 + 1) && receiveBuffer.get(2) == (byte) cmdId) {
                    framesToBeReceived--;
                }

                getMessageHandler().handleMessage(convertReceiveFrameToDalyMessage(receiveBuffer));

                if (LOG.isDebugEnabled()) {
                    LOG.debug("RECEIVED: {}", Port.printBuffer(receiveBuffer));
                }
            }
        } while (framesToBeReceived > 0);

        LOG.warn("Command {} to BMS {} successfully sent and received!", HexFormat.of().withPrefix("0x").formatHex(new byte[] { (byte) cmdId }), address - 0x3F);
    }


    @Override
    protected ByteBuffer prepareSendFrame(final int address, final int cmdId, final byte[] data) {
        sendFrame.rewind();

        int checksum = 0;
        sendFrame.put((byte) 0xA5);
        checksum += 0xA5;
        sendFrame.put((byte) address);
        checksum += address;
        sendFrame.put((byte) cmdId);
        checksum += cmdId;
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
        msg.dataId = buffer.get(2);
        final byte[] dataBytes = new byte[8];
        buffer.get(4, dataBytes);
        msg.data = ByteBuffer.wrap(dataBytes);
        msg.data.rewind();

        return msg;
    }


    @Override
    protected Predicate<byte[]> getValidator() {
        return bytes -> {
            int checksum = 0;
            for (int i = 0; i < bytes.length - 1; i++) {
                checksum += (byte) Byte.toUnsignedInt(bytes[i]);
            }

            return bytes[12] == (byte) checksum;
        };
    }


    @Override
    protected void send(final ByteBuffer frame) throws IOException {
        port.sendFrame(frame);
    }
}
