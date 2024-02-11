package com.airepublic.bmstoinverter.bms.pylon.hv.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import jakarta.inject.Inject;

/**
 * The class to handle CAN messages from a JK {@link BMS}.
 */
public class PylonHVBmsCANProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(PylonHVBmsCANProcessor.class);
    @Inject
    private EnergyStorage energyStorage;
    private final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);

    @Override
    protected void collectData(final Port port) {
        try {
            // first broadcast ensemble information
            sendMessage(port, 0x00004200, (byte) 0);
        } catch (final Throwable t) {

        }
    }


    private List<ByteBuffer> sendMessage(final Port port, final int frameId, final byte cmd) throws IOException {
        final ByteBuffer sendFrame = prepareSendFrame(frameId, cmd);
        int framesToBeReceived = getResponseFrameCount(sendFrame);
        final int frameCount = framesToBeReceived;
        int skip = 20;
        final List<ByteBuffer> readBuffers = new ArrayList<>();

        LOG.debug("SEND: {}", Port.printBuffer(sendFrame));
        ((CANPort) port).sendExtendedFrame(sendFrame);

        // read frames until the requested frame is read
        do {
            skip--;

            for (int i = 0; i < frameCount; i++) {
                final ByteBuffer receiveFrame = port.receiveFrame();

                if (receiveFrame != null) {
                    LOG.debug("RECEIVED: {}", Port.printBuffer(receiveFrame));
                    handleMessage(receiveFrame);
                    framesToBeReceived--;
                } else {
                    LOG.warn("Message could not be interpreted " + Port.printBuffer(receiveFrame));
                    return readBuffers;
                }
            }
        } while (framesToBeReceived > 0 & skip > 0);

        LOG.debug("Command 0x{} successfully sent and received!", HexFormat.of().toHexDigits(frameId));
        return readBuffers;
    }


    private void handleMessage(final ByteBuffer receiveFrame) {
        try {
            final int frameId = receiveFrame.getInt();
            final int bmsNo = frameId & 0x0000000F;
            final BatteryPack pack = energyStorage.getBatteryPack(bmsNo);
            final byte[] dataBytes = new byte[receiveFrame.get(4)];
            receiveFrame.get(8, dataBytes);

            final ByteBuffer data = ByteBuffer.wrap(dataBytes);

            switch (frameId) {
                case 0x4210:
                    readBatteryStatus(pack, data);
                break;
                case 0x4220:
                    readChargeDischargeValues(pack, data);
                break;
                case 0x4230:
                    readCellVoltage(pack, data);
                break;
                case 0x4240:
                    readCellTemperature(pack, data);
                break;
                case 0x4250:
                    readAlarms(pack, data);
                break;
                case 0x4260:
                    readModuleVoltage(pack, data);
                break;
                case 0x4270:
                    readModuleTemperature(pack, data);
                break;
            }

        } catch (final Throwable e) {
            LOG.error("Error interpreting received frame: {}" + Port.printBuffer(receiveFrame), e);
        }
    }


    private int getResponseFrameCount(final ByteBuffer sendFrame2) {
        // TODO Auto-generated method stub
        return 0;
    }


    protected ByteBuffer prepareSendFrame(final int frameId, final byte cmd) {
        sendFrame.rewind();
        sendFrame.putInt(frameId);

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        // data
        sendFrame.put(new byte[] { cmd, 0, 0, 0, 0, 0, 0, 0 });
        sendFrame.rewind();

        return sendFrame;
    }


    private void readBatteryStatus(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Battery voltage (0.1V)
        pack.packVoltage = data.getShort();
        // Battery current (0.1A) offset -3000A
        pack.packCurrent = data.getShort() - 30000;
        // second level temperature (0.1 Celcius) offset -100
        pack.tempAverage = data.getShort() - 1000;
        // Battery SOC (1%)
        pack.packSOC = data.get();
        // Battery SOH (1%)
        pack.packSOH = data.get();
    }


    private void readChargeDischargeValues(final BatteryPack pack, final ByteBuffer data) {
        // Charge cutoff voltage (0.1V)
        pack.maxPackVoltageLimit = data.getShort();
        // Discharge cutoff voltage (0.1V)
        pack.minPackVoltageLimit = data.getShort();
        // Max charge current (0.1A) offset -3000A
        pack.maxPackChargeCurrent = data.getShort() - 30000;
        // Max discharge current (0.1A) offset -3000A
        pack.maxPackDischargeCurrent = data.getShort() - 30000;
    }


    private void readCellVoltage(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell voltage (1mV)
        pack.maxCellmV = data.getShort();
        // Minimum cell voltage (1mV)
        pack.minCellmV = data.getShort();
        // Maximum cell voltage cell number
        pack.maxCellVNum = data.getShort();
        // Minimum cell voltage cell number
        pack.minCellVNum = data.getShort();
    }


    private void readCellTemperature(final BatteryPack pack, final ByteBuffer data) {
        // frame id is already read, so start at the first data byte
        // Maximum cell temperature (C) offset -100
        pack.tempMax = data.getShort() - 100;
        // Minimum cell temperature (C) offset -100
        pack.tempMin = data.getShort() - 100;
        // Maximum cell temperature cell number
        pack.tempMaxCellNum = data.getShort();
        // Minimum cell temperature cell number
        pack.tempMinCellNum = data.getShort();
    }


    private void readAlarms(final BatteryPack pack, final ByteBuffer data) {
        // Basic status
        data.get();
        // Cycle period
        data.getShort();
        // Error
        final byte error = data.get();
        // Alarm
        final short alarms = data.getShort();
        // Protection
        final short protection = data.getShort();

    }


    private void readModuleVoltage(final BatteryPack pack, final ByteBuffer data) {
    }


    private void readModuleTemperature(final BatteryPack pack, final ByteBuffer data) {
    }


    private static int read2Bits(final byte value, final int index) {
        String str = Integer.toBinaryString(value);
        System.out.println("Str to parse: " + str);

        // remove leading bits
        if (str.length() > 8) {
            str = str.substring(str.length() - 8);
        }

        // pad leading 0's
        while (str.length() < 8) {
            str = "0" + str;
        }

        System.out.println("Padded str: " + str);
        final String bits = str.substring(index, 2);
        System.out.println("Read 2bits: " + bits);

        switch (bits) {
            case "00":
                return 0;
            case "01":
                return 1;
            case "10":
                return 2;
            case "11":
                return 3;
        }

        return 0;
    }

}
