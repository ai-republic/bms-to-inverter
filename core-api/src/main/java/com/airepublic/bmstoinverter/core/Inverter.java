package com.airepublic.bmstoinverter.core;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.Alarms;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import jakarta.inject.Inject;

/**
 * The class to identify an {@link Inverter}.
 */
public abstract class Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(Inverter.class);
    private InverterConfig config;
    @Inject
    private EnergyStorage energyStorage;

    /**
     * Initializes the {@link Inverter} with the specified {@link InverterConfig}, initializing the
     * port parameters from the system properties.
     */
    public void initialize(final InverterConfig config) {
        if (!PortAllocator.hasPort(config.getPortLocator())) {
            PortAllocator.addPort(config.getPortLocator(), config.getDescriptor().createPort(config));
        }
        this.config = config;
    }


    /**
     * Gets the name of the {@link InverterDescriptor}.
     *
     * @return the name
     */
    public String getName() {
        return config.getDescriptor().getName();
    }


    /**
     * Gets the interval the data is sent to the inverter.
     *
     * @return the interval the data is sent to the inverter
     */
    public int getSendInterval() {
        return config.getSendInterval();
    }


    /**
     * Gets the assigned {@link Port}s locator.
     *
     * @return the assigned {@link Port}s locator
     */
    public String getPortLocator() {
        return config.getPortLocator();
    }


    /**
     * Process sending the data via the {@link Port} to the {@link Inverter}.
     *
     * @param callback the code executed after successful processing
     */
    public void process(final Runnable callback) {
        try {
            final BatteryPack pack = getAggregatedBatteryInfo();
            final Port port = PortAllocator.allocate(getPortLocator());
            final ByteBuffer requestFrame = readRequest(port);
            LOG.debug("Inverter received: " + Port.printBuffer(requestFrame));
            final List<ByteBuffer> sendFrames = createSendFrames(requestFrame, pack);

            if (sendFrames != null) {
                for (final ByteBuffer frame : sendFrames) {
                    LOG.debug("Inverter send: {}", Port.printBuffer(frame));
                    sendFrame(port, frame);
                }
            }
        } catch (final Throwable e) {
            LOG.error("Failed to send CAN frame", e);
        }

        try {
            callback.run();
        } catch (final Exception e) {
            LOG.error("Inverter process callback threw an exception!", e);
        }
    }


    /**
     * Read the next request (if any) to be responded to the inverter.
     *
     * @param port the {@link Port}
     * @return the received frame or null if no frames need to be read
     * @throws IOException if the frame could not be read
     */
    protected abstract ByteBuffer readRequest(Port port) throws IOException;


    /**
     * Implementations must send the frame depending on its protocol.
     *
     * @param port the {@link Port}
     * @param frame the complete frame
     * @throws IOException if the frame could not be sent
     */
    protected abstract void sendFrame(Port port, ByteBuffer frame) throws IOException;


    /**
     * Create CAN messages for the specified request frame (if any) using the aggregated
     * {@link BatteryPack}s of the {@link EnergyStorage} which will be sent to the inverter.
     *
     * @param requestFrame the request frame if any
     * @param aggregatedPack the {@link BatteryPack} resembling and aggregation of all
     *        {@link EnergyStorage}'s {@link BatteryPack}s.
     * @return the CAN messages to be sent to the inverter
     */
    protected abstract List<ByteBuffer> createSendFrames(ByteBuffer requestFrame, BatteryPack aggregatedPack);


    /**
     * Aggregates all {@link BatteryPack}s listed in the {@link EnergyStorage} into one
     * {@link BatteryPack} which data will be sent to the {@link Inverter}.
     *
     * @return the {@link BatteryPack} with aggregated values
     */
    protected BatteryPack getAggregatedBatteryInfo() {
        final BatteryPack result = new BatteryPack();
        result.maxPackChargeCurrent = Integer.MAX_VALUE;
        result.maxPackDischargeCurrent = Integer.MAX_VALUE;

        // sum all values
        for (final BatteryPack pack : energyStorage.getBatteryPacks()) {
            result.ratedCapacitymAh += pack.ratedCapacitymAh;
            result.ratedCellmV += pack.ratedCellmV;
            result.maxPackVoltageLimit += pack.maxPackVoltageLimit;
            result.minPackVoltageLimit += pack.minPackVoltageLimit;
            result.maxPackChargeCurrent = Math.min(result.maxPackChargeCurrent, pack.maxPackChargeCurrent);
            result.maxPackDischargeCurrent = Math.min(result.maxPackDischargeCurrent, pack.maxPackDischargeCurrent);
            result.packVoltage += pack.packVoltage;
            result.packCurrent += pack.packCurrent;
            result.packSOC += pack.packSOC;
            result.packSOH += pack.packSOH;
            result.maxCellmV = Math.max(result.maxCellmV, pack.maxCellmV);
            result.maxCellVNum = pack.maxCellmV == result.maxCellmV ? pack.maxCellVNum : result.maxCellVNum;
            result.minCellmV = Math.min(result.minCellmV, pack.minCellmV);
            result.minCellVNum = pack.minCellmV == result.minCellmV ? pack.minCellVNum : result.minCellVNum;
            result.tempMax = Math.max(result.tempMax, pack.tempMax);
            result.tempMin = Math.min(result.tempMin, pack.tempMin);

            // result.chargeDischargeStatus = pack.chargeDischargeStatus;
            result.chargeMOSState |= pack.chargeMOSState;
            result.disChargeMOSState |= pack.disChargeMOSState;
            result.forceCharge |= pack.forceCharge;
            result.remainingCapacitymAh += pack.remainingCapacitymAh;
            result.numberOfCells += pack.numberOfCells;
            result.chargeState |= pack.chargeState;
            result.loadState |= pack.loadState;
            result.bmsCycles = Math.max(result.bmsCycles, pack.bmsCycles);
            // cellVmV
            // cellTemperature
            // cellBalanceState
            result.cellBalanceActive |= pack.cellBalanceActive;

            aggregateAlarms(result, pack.alarms);

            result.tempMaxCellNum = Math.max(result.tempMaxCellNum, pack.tempMaxCellNum);
            result.tempMinCellNum = Math.min(result.tempMinCellNum, pack.tempMinCellNum);
            result.maxModulemV = Math.max(result.maxModulemV, pack.maxModulemV);
            result.minModulemV = Math.min(result.minModulemV, pack.minModulemV);
            result.maxModulemVNum = pack.maxModulemV == result.maxModulemV ? pack.maxModulemVNum : result.maxModulemVNum;
            result.minModulemVNum = pack.minModulemV == result.minModulemV ? pack.minModulemVNum : result.minModulemVNum;
            result.maxModuleTemp = Math.max(result.maxModuleTemp, pack.maxModuleTemp);
            result.minModuleTemp = Math.min(result.minModuleTemp, pack.minModuleTemp);
            result.maxModuleTempNum = pack.maxModuleTemp == result.maxModuleTemp ? pack.maxModuleTempNum : result.maxModuleTempNum;
            result.minModuleTempNum = pack.minModuleTemp == result.minModuleTemp ? pack.minModuleTempNum : result.minModuleTempNum;
            result.modulesInSeries += pack.modulesInSeries;
            result.moduleNumberOfCells += pack.moduleNumberOfCells;
            result.moduleVoltage += pack.moduleVoltage;
            result.moduleRatedCapacityAh += pack.moduleRatedCapacityAh;
        }

        // calculate averages
        final int count = energyStorage.getBatteryPacks().size();
        result.ratedCapacitymAh = result.ratedCapacitymAh / count;
        result.ratedCellmV = result.ratedCellmV / count;
        result.maxPackVoltageLimit = result.maxPackVoltageLimit / count;
        result.minPackVoltageLimit = result.minPackVoltageLimit / count;
        result.packVoltage = result.packVoltage / count;
        result.packSOC = result.packSOC / count;
        result.packSOH = result.packSOH / count;
        result.tempAverage = result.tempAverage / count;
        result.bmsCycles = result.bmsCycles / count;
        result.moduleVoltage = result.moduleVoltage / count;
        result.moduleRatedCapacityAh = result.moduleRatedCapacityAh / count;

        // other calculations
        result.cellDiffmV = result.maxCellmV - result.minCellmV;
        result.type = energyStorage.getBatteryPack(0).type;
        result.manufacturerCode = energyStorage.getBatteryPack(0).manufacturerCode;
        result.hardwareVersion = energyStorage.getBatteryPack(0).hardwareVersion;
        result.softwareVersion = energyStorage.getBatteryPack(0).softwareVersion;

        return result;
    }


    private void aggregateAlarms(final BatteryPack result, final Alarms alarms) {
        try {
            for (final Field field : Alarms.class.getFields()) {
                if (Alarm.class.equals(field.getType())) {
                    final Alarm alarm = (Alarm) field.get(alarms);
                    final Alarm alarmResult = (Alarm) field.get(result.alarms);

                    if (alarm.value) {
                        alarmResult.value = true;
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Error aggregating alarms!", e);
        }
    }

}
