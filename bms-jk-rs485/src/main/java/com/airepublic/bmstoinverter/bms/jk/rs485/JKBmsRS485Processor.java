package com.airepublic.bmstoinverter.bms.jk.rs485;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;

/**
 * The class to handle CAN messages from a JK {@link BMS}.
 */
public class JKBmsRS485Processor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(JKBmsRS485Processor.class);
    private final static int BATTERY_ID = 0;

    @Override
    protected void collectData(final Port port) {
        final ByteBuffer sendFrame = prepareSendFrame((byte) 0x85); // SOC

        try {
            port.sendFrame(sendFrame);

            try {
                final ByteBuffer frame = port.receiveFrame();
                final BatteryPack pack = getBatteryPack(BATTERY_ID);
                final int dataLength = frame.getShort(2) - 1; // -1 because of command id byte is
                                                              // first data byte
                final int commandId = frame.get(11);
                final byte[] bytes = new byte[dataLength];
                frame.position(12);
                frame.get(bytes);
                final ByteBuffer data = ByteBuffer.wrap(bytes);

                switch (commandId) {
                    case 0x85:
                        readBatterySOC(pack, data);
                    break;
                }

            } catch (final IOException e) {
                LOG.error("Error receiving frame!", e);
            }
        } catch (final IOException e) {
            LOG.error("Error sending frame: " + Port.printBuffer(sendFrame));
        }
    }


    ByteBuffer prepareSendFrame(final byte commandId) {
        final ByteBuffer sendFrame = ByteBuffer.allocateDirect(21).order(ByteOrder.LITTLE_ENDIAN);
        sendFrame.put((byte) 0x4E); // start flag 2 bytes
        sendFrame.put((byte) 0x57);
        sendFrame.put((byte) 0x00); // frame length including this 2 bytes
        sendFrame.put((byte) 0x13);
        sendFrame.put((byte) 0x00); // terminal number 4 bytes
        sendFrame.put((byte) 0x00);
        sendFrame.put((byte) 0x00);
        sendFrame.put((byte) 0x00);
        sendFrame.put((byte) 0x03); // command id (0x01 - activation instruction, 0x02 - write
                                    // instruction, 0x03 - read identifier data, 0x05 - pair code,
                                    // 0x06 - read all data
        sendFrame.put((byte) 0x03); // frame source id (0x00 - BMS, 0x01- BT, 0x02-GPS, 0x03 - PC)
        sendFrame.put((byte) 0x00); // transport type (0x00 - request, 0x01 - response)
        sendFrame.put(commandId);
        sendFrame.putInt(0x00000000); // record number - 4 bytes (1st random, 2-4 recorde number)
        sendFrame.put((byte) 0x68); // end flag

        int crc = 0;

        for (int i = 2; i < sendFrame.capacity() - 4; i++) {
            crc += sendFrame.get(i);
        }
        sendFrame.putInt(crc); // CRC 4 byts

        return sendFrame;
    }


    private void readBatterySOC(final BatteryPack pack, final ByteBuffer data) {
        // Battery SOC (1%)
        pack.packSOC = data.get();
    }

}
