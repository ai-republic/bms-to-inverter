package com.airepublic.bmstoinverter.protocol.modbus;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

public class ModbusUtil {

    public static ByteBuffer createRequestBuffer(final int functionCode, final int startAddress, final int numRegisters, final int unitId) {
        final ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putInt(functionCode);
        buffer.putInt(startAddress);
        buffer.putInt(numRegisters);
        buffer.putInt(unitId);

        return buffer;
    }


    public static ByteBuffer toBuffer(final ReadMultipleRegistersResponse response) {
        final Register[] registers = response.getRegisters();
        final ByteBuffer buffer = ByteBuffer.allocate(registers.length * 4 + 12);
        buffer.putInt(response.getFunctionCode());
        buffer.putInt(registers.length);
        buffer.putInt(response.getUnitID());
        Stream.of(registers).map(Register::getValue).forEach(buffer::putInt);
        buffer.flip();
        return buffer;
    }


    public static ByteBuffer toBuffer(final ReadInputRegistersResponse response) {
        final InputRegister[] registers = response.getRegisters();
        final ByteBuffer buffer = ByteBuffer.allocate(registers.length * 4 + 12);
        buffer.putInt(response.getFunctionCode());
        buffer.putInt(registers.length);
        buffer.putInt(response.getUnitID());
        Stream.of(registers).map(InputRegister::getValue).forEach(buffer::putInt);
        buffer.flip();
        return buffer;
    }


    public static ByteBuffer toBuffer(final WriteMultipleRegistersResponse response) {
        final ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putInt(response.getFunctionCode());
        buffer.putInt(1);
        buffer.putInt(response.getUnitID());
        buffer.putInt(response.getReference());
        buffer.flip();
        return buffer;
    }


    public static ByteBuffer toBuffer(final WriteSingleRegisterResponse response) {
        final ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.putInt(response.getFunctionCode());
        buffer.putInt(2);
        buffer.putInt(response.getUnitID());
        buffer.putInt(response.getReference());
        buffer.putInt(response.getRegisterValue());
        buffer.flip();
        return buffer;
    }

}
