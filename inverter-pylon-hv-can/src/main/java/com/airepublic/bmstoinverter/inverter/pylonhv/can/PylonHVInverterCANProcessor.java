package com.airepublic.bmstoinverter.inverter.pylonhv.can;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortAllocator;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.protocol.can.CANPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The class to handle CAN messages for a Growatt low voltage (12V/24V/48V) {@link Inverter}.
 */
@ApplicationScoped
public class PylonHVInverterCANProcessor extends Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(PylonHVInverterCANProcessor.class);
    @Inject
    private EnergyStorage energyStorage;

    @Override
    public void process(final Runnable callback) {
        final Port port = PortAllocator.allocate(getPortLocator());

        try {
            // listen for inverter requests
            final ByteBuffer requestFrame = port.receiveFrame();
            handleRequest(port, requestFrame);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            callback.run();
        } catch (final Exception e) {
            LOG.error("Inverter process callback threw an exception!", e);
        }

    }


    @Override
    protected List<ByteBuffer> createSendFrames() {
        // TODO let inverters do processing itself
        return null;
    }


    @Override
    protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
        ((CANPort) port).sendExtendedFrame(frame);
    }


    private void handleRequest(final Port port, final ByteBuffer frame) {
        frame.rewind();
        final int frameId = frame.getInt();
        final int length = frame.get();
        final byte[] data = new byte[length];
        frame.get(8, data);
        final int bmsNo = frameId & 0x0000000F;

        try {
            switch (frameId) {
                case 0x00004200:
                    send4200Responses(port, bmsNo);
                break;
            }
        } catch (final IOException e) {
            LOG.error("Error sending responses for request: " + Port.printBuffer(frame));
        }
    }


    protected ByteBuffer prepareSendFrame(final int frameId) {
        final ByteBuffer sendFrame = ByteBuffer.allocateDirect(16).order(ByteOrder.LITTLE_ENDIAN);
        sendFrame.putInt(frameId);

        // header
        sendFrame.put((byte) 0x08) // data length
                .put((byte) 0) // flags
                .putShort((short) 0); // skip 2 bytes

        return sendFrame;
    }


    private void send4200Responses(final Port port, final int bmsNo) throws IOException {
        sendBatteryStatus(port, bmsNo);
        sendChargeDischargeValues(port, bmsNo);
    }


    private void sendBatteryStatus(final Port port, final int bmsNo) throws IOException {
        final BatteryPack pack = energyStorage.getBatteryPack(bmsNo);
        final ByteBuffer frame = prepareSendFrame(0x00004210 | bmsNo);

        // Battery voltage (0.1V)
        frame.putShort((short) pack.packVoltage);
        // Battery current (0.1A) offset -3000A
        frame.putShort((short) (pack.packCurrent + 30000));
        // second level temperature (0.1 Celcius) offset -100
        frame.putShort((short) (pack.tempAverage * 10 + 1000));
        // Battery SOC (1%)
        frame.put((byte) (pack.packSOC / 10));
        // Battery SOH (1%)
        frame.put((byte) (pack.packSOH / 10));

        LOG.debug("Sending battery status: {}", Port.printBuffer(frame));
        sendFrame(port, frame);
    }


    private void sendChargeDischargeValues(final Port port, final int bmsNo) {
        final BatteryPack pack = energyStorage.getBatteryPack(bmsNo);
        final ByteBuffer frame = prepareSendFrame(0x00004220 | bmsNo);

        // Charge cutoff voltage (0.1V)
        frame.putShort((short) pack.maxPackVoltageLimit);
        // Discharge cutoff voltage (0.1V)
        frame.putShort((short) pack.minPackVoltageLimit);
        // Max charge current (0.1A) offset -3000A
        frame.putShort((short) (pack.maxPackChargeCurrent + 30000));
        // Max discharge current (0.1A) offset -3000A
        frame.putShort((short) (pack.maxPackDischargeCurrent + 30000));

        LOG.debug("Sending max/min voltage, current, charge and discharge limits: {}", Port.printBuffer(frame));
    }

}
