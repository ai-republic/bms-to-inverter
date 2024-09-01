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
package com.airepublic.bmstoinverter.protocol.modbus;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.protocol.modbus.ModBusPort;
import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.procimg.SimpleDigitalIn;
import com.ghgande.j2mod.modbus.procimg.SimpleDigitalOut;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import com.ghgande.j2mod.modbus.util.SerialParameters;

/**
 * The implementation of the {@link ModBusPort} using the J2Mod implementation.
 */
public class J2ModSlavePort extends ModBusPort {
    private final static Logger LOG = LoggerFactory.getLogger(J2ModSlavePort.class);
    private ModbusSerialMaster port;
    private final SimpleProcessImage spi = new SimpleProcessImage();

    public J2ModSlavePort() {
    }


    /**
     * Constructor.
     *
     * @param portname the portname
     * @param baudrate the baudrate
     */
    public J2ModSlavePort(final String portname, final int baudrate) {
        super(portname, baudrate);
    }


    @Override
    public void open() throws IOException {

        try {
            // initialize processing image
            for (int i = 0; i < 9999; i++) {
                // coils
                spi.addDigitalOut(new SimpleDigitalOut(false));
                // discretes
                spi.addDigitalIn(new SimpleDigitalIn(false));
                // input registers
                spi.addInputRegister(new SimpleInputRegister(i));
                // holding registers
                spi.addRegister(new SimpleRegister(i));
            }

            // Set up serial parameters
            final SerialParameters params = new SerialParameters();
            params.setPortName(getPortname()); // Adjust this to your serial port
            params.setBaudRate(getBaudrate()); // Adjust this to your baud rate
            params.setDatabits(8);
            params.setParity("None");
            params.setStopbits(1);
            params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
            params.setEcho(false);

            final ModbusSlave slave = ModbusSlaveFactory.createSerialSlave(params);
            slave.addProcessImage(1, spi);
            slave.open();
        } catch (final Exception e) {
            LOG.error("Could not open modbus slave port {}!", getPortname(), e);
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
        return null;
    }


    /**
     * The buffer represents a custom format specified as follows:<br>
     * Byte1-4 (int): start address<br>
     * Byte5-8 (int): start address<br>
     * Byte9-12 (int): number of registers to request<br>
     * Byte13-16 (int): unit id<br>
     */
    @Override
    public void sendFrame(final ByteBuffer frame) throws IOException {
    }


    @Override
    public void clearBuffers() {
    }


    /**
     * Returns the process image storing the modbus registers.
     *
     * @return the process image
     */
    public SimpleProcessImage getProcessingImage() {
        return spi;
    }
}
