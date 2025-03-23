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
package com.airepublic.bmstoinverter.bms.daly.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.bms.daly.common.AbstractDalyBmsProcessor;
import com.airepublic.bmstoinverter.bms.daly.common.DalyCommand;
import com.airepublic.bmstoinverter.bms.daly.common.DalyMessage;
import com.airepublic.bmstoinverter.core.NoDataAvailableException;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;
import com.airepublic.bmstoinverter.core.util.HexUtil;

/**
 * The class to handle CAN messages from a Daly BMS.
 */
public class DalyBmsCANProcessor extends AbstractDalyBmsProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(DalyBmsCANProcessor.class);
    private final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);

    @Override
    protected List<ByteBuffer> sendMessage(final Port port, final DalyCommand cmd, final byte[] data) throws IOException, NoDataAvailableException {
        final ByteBuffer sendFrame = prepareSendFrame(getBmsId(), cmd, data);
        int framesToBeReceived = getResponseFrameCount(cmd);
        final int frameCount = framesToBeReceived;
        int skip = 20;
        final List<ByteBuffer> readBuffers = new ArrayList<>();
        int noDataReceived = 0;

        LOG.debug("SEND: {}", Port.printBuffer(sendFrame));
        ((CANPort) port).sendExtendedFrame(sendFrame);

        // read frames until the requested frame is read
        do {
            skip--;

            for (int i = 0; i < frameCount; i++) {
                final ByteBuffer receiveFrame = port.receiveFrame();

                if (receiveFrame == null) {
                    // keep track of how often no bytes could be read
                    noDataReceived++;
                    LOG.debug("No bytes received: " + noDataReceived + " times!");

                    // if we received no bytes more than 10 times we stop and notify the handler
                    // to re-open the port
                    if (noDataReceived >= 10) {
                        throw new NoDataAvailableException();
                    }

                    // try and wait for the next message to arrive
                    try {
                        LOG.debug("Waiting for messages to arrive....");
                        Thread.sleep(getDelayAfterNoBytes());
                    } catch (final InterruptedException e) {
                    }
                } else {

                    LOG.debug("RECEIVED: {}", Port.printBuffer(receiveFrame));

                    final byte receiver = (byte) (receiveFrame.getInt(0) >> 8 & 0x000000FF);
                    final byte command = (byte) (receiveFrame.getInt(0) >> 16 & 0x000000FF);

                    if (receiver == (byte) 0x40) {
                        final DalyMessage dalyMsg = convertReceiveFrameToDalyMessage(receiveFrame);

                        if (dalyMsg != null) {
                            readBuffers.add(receiveFrame);
                            framesToBeReceived--;

                            getMessageHandler().handleMessage(this, dalyMsg);
                        } else {
                            LOG.warn("Message could not be interpreted " + Port.printBuffer(receiveFrame));
                            port.clearBuffers();
                            return readBuffers;
                        }
                    } else {
                        LOG.warn("Message has wrong address and command id: " + Port.printBuffer(receiveFrame));
                        port.clearBuffers();
                        return readBuffers;
                    }
                }
            }
        } while (framesToBeReceived > 0 & skip > 0);

        LOG.debug("Command 0x{} to BMS {} successfully sent and received!", HexUtil.toHexDigits(cmd.id), getBmsId());
        return readBuffers;
    }


    @Override
    protected ByteBuffer prepareSendFrame(final int bmsId, final DalyCommand cmd, final byte[] data) {
        sendFrame.rewind();

        sendFrame.put((byte) 0x40);
        sendFrame.put((byte) bmsId);
        sendFrame.put((byte) cmd.id);
        sendFrame.put((byte) 0x18);

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
        msg.bmsId = (byte) (frameId & 0x000000FF);
        msg.cmd = DalyCommand.valueOf(frameId >> 16 & 0x000000FF);

        if (msg.cmd == null) {
            LOG.error("Received unknown command: " + Byte.toUnsignedInt(buffer.get(2)));
            return null;
        }

        final byte[] dataBytes = new byte[buffer.get(4)];
        buffer.position(8);
        buffer.get(dataBytes);
        msg.data = ByteBuffer.wrap(dataBytes);

        if (LOG.isDebugEnabled()) {
            LOG.info("DALY Message: frameId= " + Integer.toHexString(frameId)
                    + ", address=" + HexUtil.toHexDigits(msg.bmsId)
                    + ", dataId=" + HexUtil.toHexDigits(msg.cmd.id)
                    + ", data=" + HexUtil.formatHex(dataBytes));
        }

        return msg;
    }
}
