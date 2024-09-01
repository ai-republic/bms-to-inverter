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
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.protocol.modbus.ModBusPort;
import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.SerialParameters;

/**
 * The implementation of the {@link ModBusPort} using the J2Mod implementation.
 */
public class J2ModMasterPort extends ModBusPort {
    private final static Logger LOG = LoggerFactory.getLogger(J2ModMasterPort.class);
    private ModbusSerialMaster port;
    private final ConcurrentLinkedQueue<ByteBuffer> buffers = new ConcurrentLinkedQueue<>();

    public J2ModMasterPort() {
    }


    /**
     * Constructor.
     *
     * @param portname the portname
     * @param baudrate the baudrate
     */
    public J2ModMasterPort(final String portname, final int baudrate) {
        super(portname, baudrate);
    }


    @Override
    public void open() throws IOException {
        try {
            // Set up serial parameters
            final SerialParameters params = new SerialParameters();
            params.setPortName(getPortname()); // Adjust this to your serial port
            params.setBaudRate(getBaudrate()); // Adjust this to your baud rate
            params.setDatabits(8);
            params.setParity("None");
            params.setStopbits(1);
            params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
            params.setEcho(false);

            port = new ModbusSerialMaster(params);
            port.connect();
        } catch (final Exception e) {
            LOG.error("Could not open modbus master port {}!", getPortname(), e);
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
        return buffers.poll();
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
        final int functionCode = frame.getInt();

        switch (functionCode) {
            case Modbus.READ_INPUT_REGISTERS:
                readInputRegisters(frame);
            break;
            case Modbus.READ_HOLDING_REGISTERS:
                readHoldingRegisters(frame);
            break;
            case Modbus.WRITE_SINGLE_REGISTER:
                writeSingleRegister(frame);
            break;
            case Modbus.WRITE_MULTIPLE_REGISTERS:
                writeMultipleRegisters(frame);
            break;
        }
    }


    private void readInputRegisters(final ByteBuffer frame) throws IOException {
        // Modbus register addresses are 1-based, so subtract 1 for the j2mod library
        final int startAddress = frame.getInt() - 1;
        final int numRegisters = frame.getInt();
        final int unitId = frame.getInt();
        ModbusSerialTransaction transaction = null;
        ReadInputRegistersResponse response = null;
        ReadInputRegistersRequest request = null;

        try {
            request = new ReadInputRegistersRequest(startAddress, numRegisters);
            request.setUnitID(unitId);
            request.setHeadless();

            // Prepare a transaction
            transaction = new ModbusSerialTransaction(request);

            transaction.setTransDelayMS(50);
            transaction.execute();

            response = (ReadInputRegistersResponse) transaction.getResponse();
            buffers.add(ModbusUtil.toBuffer(response));
        } catch (final Exception e) {
            throw new IOException("Error reading from modbus device #" + unitId + "(address: " + startAddress + ", numRegisters: " + numRegisters + ")");
        }
    }


    private void readHoldingRegisters(final ByteBuffer frame) throws IOException {
        // Modbus register addresses are 1-based, so subtract 1 for the j2mod library
        final int startAddress = frame.getInt() - 1;
        final int numRegisters = frame.getInt();
        final int unitId = frame.getInt();
        ModbusSerialTransaction transaction = null;
        ReadMultipleRegistersRequest request = null;
        ReadMultipleRegistersResponse response = null;

        try {
            // Prepare a request
            request = new ReadMultipleRegistersRequest(startAddress, numRegisters);
            request.setUnitID(unitId);
            request.setHeadless();

            // Prepare a transaction
            transaction = new ModbusSerialTransaction(request);

            transaction.setTransDelayMS(50);
            transaction.execute();

            response = (ReadMultipleRegistersResponse) transaction.getResponse();

            buffers.add(ModbusUtil.toBuffer(response));
        } catch (final Exception ex) {
            throw new IOException("Error reading from modbus device #" + unitId + "(address: " + startAddress + ", numRegisters: " + numRegisters + ")");
        }
    }


    private void writeMultipleRegisters(final ByteBuffer frame) throws IOException {
        // Modbus register addresses are 1-based, so subtract 1 for the j2mod library
        final int startAddress = frame.getInt() - 1;
        final int numRegisters = frame.getInt();
        final int unitId = frame.getInt();
        final Register[] registers = new Register[numRegisters];

        for (int i = 0; i < numRegisters; i++) {
            registers[i] = new SimpleRegister(frame.get(), frame.get());
        }

        ModbusSerialTransaction transaction = null;
        WriteMultipleRegistersResponse response = null;
        WriteMultipleRegistersRequest request = null;

        try {
            request = new WriteMultipleRegistersRequest(startAddress, registers);
            request.setUnitID(unitId);
            request.setHeadless();

            // Prepare a transaction
            transaction = new ModbusSerialTransaction(request);

            transaction.setTransDelayMS(50);
            transaction.execute();

            response = (WriteMultipleRegistersResponse) transaction.getResponse();
            buffers.add(ModbusUtil.toBuffer(response));
        } catch (final Exception e) {
            throw new IOException("Error reading from modbus device #" + unitId + "(address: " + startAddress + ", numRegisters: " + numRegisters + ")");
        }
    }


    private void writeSingleRegister(final ByteBuffer frame) throws IOException {
        // Modbus register addresses are 1-based, so subtract 1 for the j2mod library
        final int startAddress = frame.getInt() - 1;
        final int numRegisters = frame.getInt();
        final int unitId = frame.getInt();

        ModbusSerialTransaction transaction = null;
        WriteSingleRegisterResponse response = null;
        WriteSingleRegisterRequest request = null;

        try {
            request = new WriteSingleRegisterRequest(startAddress, new SimpleRegister(frame.get(), frame.get()));
            request.setUnitID(unitId);
            request.setHeadless();

            // Prepare a transaction
            transaction = new ModbusSerialTransaction(request);

            transaction.setTransDelayMS(50);
            transaction.execute();

            response = (WriteSingleRegisterResponse) transaction.getResponse();
            buffers.add(ModbusUtil.toBuffer(response));
        } catch (final Exception e) {
            throw new IOException("Error reading from modbus device #" + unitId + "(address: " + startAddress + ", numRegisters: " + numRegisters + ")");
        }
    }


    @Override
    public void clearBuffers() {
    }
}
