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
package com.airepublic.bmstoinverter.inverter.huawei.modbus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.airepublic.bmstoinverter.core.AlarmLevel;
import com.airepublic.bmstoinverter.core.Inverter;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.PortAllocator;
import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.Util;
import com.airepublic.bmstoinverter.protocol.modbus.J2ModSlavePort;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The class to handle Modbus messages for a Huawei {@link Inverter}.
 */
@ApplicationScoped
public class HuaweiInverterModbusProcessor extends Inverter {

    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        final J2ModSlavePort port = (J2ModSlavePort) PortAllocator.allocate(getPortLocator());
        final SimpleProcessImage spi = port.getProcessingImage();

        // set battery info
        spi.setInputRegister(100, new SimpleInputRegister(aggregatedPack.numberOfCells));
        spi.setInputRegister(102, new SimpleInputRegister(aggregatedPack.packVoltage));
        spi.setInputRegister(103, new SimpleInputRegister(aggregatedPack.packCurrent));
        spi.setInputRegister(104, new SimpleInputRegister(aggregatedPack.packSOC / 10));
        spi.setInputRegister(105, new SimpleInputRegister(aggregatedPack.packSOH / 10));
        spi.setInputRegister(175, new SimpleInputRegister(aggregatedPack.tempMin * 10));
        spi.setInputRegister(176, new SimpleInputRegister(aggregatedPack.tempMinCellNum));
        spi.setInputRegister(177, new SimpleInputRegister(aggregatedPack.tempMax));
        spi.setInputRegister(178, new SimpleInputRegister(aggregatedPack.tempMaxCellNum));
        spi.setInputRegister(179, new SimpleInputRegister(aggregatedPack.minCellmV * 100));
        spi.setInputRegister(180, new SimpleInputRegister(aggregatedPack.minCellVNum));
        spi.setInputRegister(181, new SimpleInputRegister(aggregatedPack.maxCellmV * 100));
        spi.setInputRegister(182, new SimpleInputRegister(aggregatedPack.maxCellVNum));

        // set alarms
        final int[] alarms = getAlarms(aggregatedPack);
        spi.setInputRegister(9013, new SimpleInputRegister(alarms[0]));
        spi.setInputRegister(9014, new SimpleInputRegister(alarms[1]));
        spi.setInputRegister(9015, new SimpleInputRegister(alarms[2]));

        return null;
    }


    private int[] getAlarms(final BatteryPack aggregatedPack) {
        int bits39014 = 0;
        int bits39015 = 0;
        int bits39016 = 0;

        bits39014 = Util.setBit(bits39014, 13, aggregatedPack.alarms.get(Alarm.FAILURE_COMMUNICATION_INTERNAL) == AlarmLevel.ALARM);
        bits39014 = Util.setBit(bits39014, 15, aggregatedPack.alarms.get(Alarm.FAILURE_OTHER) == AlarmLevel.ALARM);
        bits39016 = Util.setBit(bits39016, 0, aggregatedPack.alarms.get(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        bits39016 = Util.setBit(bits39016, 15, aggregatedPack.alarms.get(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.ALARM);
        bits39016 = Util.setBit(bits39016, 12, aggregatedPack.alarms.get(Alarm.PACK_CURRENT_HIGH) == AlarmLevel.ALARM);
        bits39015 = Util.setBit(bits39015, 1, aggregatedPack.alarms.get(Alarm.FAILURE_SHORT_CIRCUIT_PROTECTION) == AlarmLevel.ALARM);
        bits39016 = Util.setBit(bits39016, 13, aggregatedPack.alarms.get(Alarm.PACK_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        bits39016 = Util.setBit(bits39016, 14, aggregatedPack.alarms.get(Alarm.PACK_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        bits39015 = Util.setBit(bits39015, 15, aggregatedPack.alarms.get(Alarm.TEMPERATURE_SENSOR_DIFFERENCE_HIGH) == AlarmLevel.ALARM);

        return new int[] { bits39014, bits39015, bits39016 };
    }


    @Override
    protected ByteBuffer readRequest(final Port port) throws IOException {
        return null;
    }


    @Override
    protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
        port.sendFrame(frame);
    }

}
