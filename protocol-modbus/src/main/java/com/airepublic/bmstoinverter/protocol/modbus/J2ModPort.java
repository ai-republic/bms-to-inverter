package com.airepublic.bmstoinverter.protocol.modbus;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.protocol.modbus.ModBusPort;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.util.SerialParameters;

/**
 * The implementation of the {@link ModBusPort} using the J2Mod implementation.
 */
public class J2ModPort extends ModBusPort {
    private final static Logger LOG = LoggerFactory.getLogger(J2ModPort.class);
    private ModbusSerialMaster port;

    public J2ModPort() {
    }


    /**
     * Constructor.
     *
     * @param portname the portname
     * @param baudrate the baudrate
     */
    public J2ModPort(final String portname, final int baudrate) {
        super(portname, baudrate);
    }


    @Override
    public void open() throws IOException {
        try {
            final SerialParameters serialParams = new SerialParameters(getPortname(), getBaudrate(), 0, 0, 8, 1, 0, false);
            port = new ModbusSerialMaster(serialParams);
            port.connect();
        } catch (final Exception e) {
            LOG.error("Could not open port {}!", getPortname(), e);
        }
    }


    @Override
    public boolean isOpen() {

        return port != null && port.isConnected();
    }


    @Override
    public void close() {
        try {
            port.disconnect();
            LOG.info("Shutting down port '{}'...OK", getPortname());
        } catch (final Exception e) {
            LOG.error("Shutting down port '{}'...FAILED", getPortname(), e);
        }
    }


    @Override
    public ByteBuffer receiveFrame() throws IOException {
        final byte[] frame = new byte[0];
        return ByteBuffer.wrap(frame);
    }


    @Override
    public void sendFrame(final ByteBuffer frame) throws IOException {
    }


    @Override
    public void clearBuffers() {
    }
}
