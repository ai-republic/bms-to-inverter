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
 * The class to handle Modbus messages for a Growatt {@link Inverter}.
 */
@ApplicationScoped
public class GrowattInverterModbusProcessor extends Inverter {

    @Override
    protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        final J2ModSlavePort port = (J2ModSlavePort) PortAllocator.allocate(getPortLocator());
        final SimpleProcessImage spi = port.getProcessingImage();

        // set battery info
        spi.setInputRegister(1083, new SimpleInputRegister(getBMSStatus(aggregatedPack)));
        spi.setInputRegister(1085, new SimpleInputRegister(getAlarms(aggregatedPack)));
        spi.setInputRegister(1086, new SimpleInputRegister(aggregatedPack.packSOC / 10));
        spi.setInputRegister(1087, new SimpleInputRegister(aggregatedPack.packVoltage * 10));
        spi.setInputRegister(1088, new SimpleInputRegister(aggregatedPack.packCurrent * 10));
        spi.setInputRegister(1089, new SimpleInputRegister(aggregatedPack.tempAverage / 10));
        spi.setInputRegister(1090, new SimpleInputRegister(aggregatedPack.maxPackChargeCurrent / 10));
        spi.setInputRegister(1091, new SimpleInputRegister(aggregatedPack.remainingCapacitymAh / 10));
        spi.setInputRegister(1092, new SimpleInputRegister(aggregatedPack.ratedCapacitymAh / 10));
        spi.setInputRegister(1094, new SimpleInputRegister(0)); // delta voltage
        spi.setInputRegister(1095, new SimpleInputRegister(aggregatedPack.bmsCycles));
        spi.setInputRegister(1096, new SimpleInputRegister(aggregatedPack.packSOH));
        return null;
    }


    private int getBMSStatus(final BatteryPack aggregatedPack) {
        int status = 0;

        // dis-/charging status
        switch (aggregatedPack.chargeDischargeStatus) {
            case 0: { // idle/stationary
                status = Util.setBit(status, 0, true);
                status = Util.setBit(status, 1, false);
            }
            break;
            case 1: { // charging
                status = Util.setBit(status, 0, false);
                status = Util.setBit(status, 1, true);
            }
            break;
            case 2: { // discharging
                status = Util.setBit(status, 0, true);
                status = Util.setBit(status, 1, true);
            }
            break;
            case 3: { // sleep
                status = Util.setBit(status, 0, false);
                status = Util.setBit(status, 1, false);
            }
            break;
        }

        // error bit
        final boolean hasErrors = aggregatedPack.alarms.values().stream().anyMatch(level -> level == AlarmLevel.ALARM);
        status = Util.setBit(status, 2, hasErrors);

        // balancing
        status = Util.setBit(status, 3, aggregatedPack.cellBalanceActive);

        // sleep status enable/disable
        status = Util.setBit(status, 4, false);

        // discharge enable/disable
        status = Util.setBit(status, 5, false);

        // charge enable/disable
        status = Util.setBit(status, 6, false);

        // battery terminal connected
        status = Util.setBit(status, 7, true);

        // master machine operation
        status = Util.setBit(status, 8, false);
        status = Util.setBit(status, 9, false);

        // SP status
        switch (aggregatedPack.chargeDischargeStatus) {
            case 0: { // none
                status = Util.setBit(status, 10, false);
                status = Util.setBit(status, 11, false);
            }
            break;
            case 1: { // charging
                status = Util.setBit(status, 10, false);
                status = Util.setBit(status, 11, true);
            }
            break;
            case 2: { // discharging
                status = Util.setBit(status, 10, true);
                status = Util.setBit(status, 11, true);
            }
            break;
            case 3: { // stand-by
                status = Util.setBit(status, 10, true);
                status = Util.setBit(status, 11, false);
            }
            break;
        }
        return status;
    }


    private int getAlarms(final BatteryPack aggregatedPack) {
        int alarms = 0;
        boolean permanentFault = aggregatedPack.alarms.get(Alarm.FAILURE_EEPROM_MODULE) == AlarmLevel.ALARM;
        permanentFault |= aggregatedPack.alarms.get(Alarm.FAILURE_OTHER) == AlarmLevel.ALARM;

        alarms = Util.setBit(alarms, 0, aggregatedPack.alarms.get(Alarm.DISCHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        alarms = Util.setBit(alarms, 1, aggregatedPack.alarms.get(Alarm.FAILURE_DISCHARGE_BREAKER) == AlarmLevel.ALARM);
        alarms = Util.setBit(alarms, 2, aggregatedPack.alarms.get(Alarm.PACK_VOLTAGE_HIGH) == AlarmLevel.ALARM);
        alarms = Util.setBit(alarms, 3, aggregatedPack.alarms.get(Alarm.PACK_VOLTAGE_LOW) == AlarmLevel.ALARM);
        alarms = Util.setBit(alarms, 4, aggregatedPack.alarms.get(Alarm.DISCHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        alarms = Util.setBit(alarms, 5, aggregatedPack.alarms.get(Alarm.CHARGE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        alarms = Util.setBit(alarms, 6, aggregatedPack.alarms.get(Alarm.DISCHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        alarms = Util.setBit(alarms, 7, aggregatedPack.alarms.get(Alarm.CHARGE_TEMPERATURE_LOW) == AlarmLevel.ALARM);
        alarms = Util.setBit(alarms, 8, false);
        alarms = Util.setBit(alarms, 9, permanentFault);
        alarms = Util.setBit(alarms, 10, false);
        alarms = Util.setBit(alarms, 11, aggregatedPack.alarms.get(Alarm.CHARGE_CURRENT_HIGH) == AlarmLevel.ALARM);
        alarms = Util.setBit(alarms, 12, aggregatedPack.alarms.get(Alarm.CHARGE_MODULE_TEMPERATURE_HIGH) == AlarmLevel.ALARM || aggregatedPack.alarms.get(Alarm.DISCHARGE_MODULE_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        alarms = Util.setBit(alarms, 13, aggregatedPack.alarms.get(Alarm.ENCASING_TEMPERATURE_HIGH) == AlarmLevel.ALARM || aggregatedPack.alarms.get(Alarm.PACK_TEMPERATURE_HIGH) == AlarmLevel.ALARM);
        alarms = Util.setBit(alarms, 14, aggregatedPack.alarms.get(Alarm.PACK_TEMPERATURE_LOW) == AlarmLevel.ALARM);

        return alarms;
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
