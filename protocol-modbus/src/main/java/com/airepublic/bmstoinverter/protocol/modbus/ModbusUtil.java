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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.Stream;

import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

/**
 * Utility for handling ModBus communication.
 */
public class ModbusUtil {
    /**
     * The register types definition;
     */
    public enum RegisterCode {
        READ_HOLDING_REGISTERS(0x03),
        READ_INPUT_REGISTERS(0x04),
        WRITE_SINGLE_REGISTER(0x06),
        WRITE_MULTIPLE_REGISTERS(0x10);

        private final int functionCode;

        RegisterCode(final int functionCode) {
            this.functionCode = functionCode;
        }


        /**
         * Gets the function code for the {@link RegisterCode}.
         *
         * @return the function code
         */
        public int getFunctionCode() {
            return functionCode;
        }
    }

    /**
     * Creates a {@link ByteBuffer} that stores the header as integers.
     *
     * @param register the {@link RegisterCode}
     * @param startAddress the start address
     * @param numRegisters the number of registers
     * @param unitId the unit id
     * @return the created {@link ByteBuffer}
     */
    public static ByteBuffer createRequestBuffer(final RegisterCode register, final int startAddress, final int numRegisters, final int unitId) {
        final ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(register.getFunctionCode());
        buffer.putInt(startAddress);
        buffer.putInt(numRegisters);
        buffer.putInt(unitId);

        buffer.rewind();

        return buffer;
    }


    /**
     * Transforms the response into a {@link ByteBuffer} that stores the register values as
     * integers.
     *
     * @param response the ModBus register response
     * @return the ByteBuffer of int value registers
     */
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


    /**
     * Transforms the response into a {@link ByteBuffer} that stores the register values as
     * integers.
     *
     * @param response the ModBus register response
     * @return the ByteBuffer of int value registers
     */
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


    /**
     * Transforms the response into a {@link ByteBuffer} that stores the register reference as an
     * int.
     *
     * @param response the ModBus register response
     * @return the ByteBuffer of int value registers
     */
    public static ByteBuffer toBuffer(final WriteMultipleRegistersResponse response) {
        final ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putInt(response.getFunctionCode());
        buffer.putInt(1);
        buffer.putInt(response.getUnitID());
        buffer.putInt(response.getReference());
        buffer.flip();
        return buffer;
    }


    /**
     * Transforms the response into a {@link ByteBuffer} that stores the register value as an int.
     *
     * @param response the ModBus register response
     * @return the ByteBuffer of int value registers
     */
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
