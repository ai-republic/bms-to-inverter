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
package com.airepublic.bmstoinverter.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;
import com.airepublic.bmstoinverter.core.plugin.inverter.InverterPlugin;

import jakarta.inject.Inject;

/**
 * The class to identify an {@link Inverter}.
 */
public abstract class Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(Inverter.class);
    private InverterConfig config;
    @Inject
    private EnergyStorage energyStorage;
    private final BatteryPack presetPack = new BatteryPack();
    private InverterPlugin plugin;

    /**
     * Constructor.
     */
    public Inverter() {
    }


    /**
     * Constructor.
     *
     * @param plugin the {@link InverterPlugin} to use
     */
    public Inverter(final InverterPlugin plugin) {
        setPlugin(plugin);
    }


    /**
     * Initializes the {@link Inverter} with the specified {@link InverterConfig}, initializing the
     * port parameters from the system properties.
     */
    public void initialize(final InverterConfig config) {
        if (getPlugin() != null) {
            getPlugin().onInitialize(this, config);
        }

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
     * Gets the {@link EnergyStorage} associated with this {@link Inverter}.
     *
     * @return the {@link EnergyStorage}
     */
    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }


    /**
     * Gets the configured {@link InverterPlugin} or null.
     *
     * @return the plugin the {@link InverterPlugin}
     */
    public InverterPlugin getPlugin() {
        return plugin;
    }


    /**
     * Sets the {@link InverterPlugin}.
     *
     * @param plugin the plugin to set
     */
    public void setPlugin(final InverterPlugin plugin) {
        this.plugin = plugin;
    }


    /**
     * Process sending the data via the {@link Port} to the {@link Inverter}.
     *
     * @param callback the code executed after successful processing
     */
    public void process(final Runnable callback) {
        ByteBuffer currentFrame = null;

        if (energyStorage.getBatteryPacks().size() > 0) {
            try {
                updateAggregatedBatteryInfo();
                final Port port = PortAllocator.allocate(getPortLocator());
                ByteBuffer requestFrame = readRequest(port);

                // if a plugin is set
                if (getPlugin() != null) {
                    // call the the plugin to manipulate the frame
                    requestFrame = getPlugin().onReceive(requestFrame);
                }

                LOG.debug("Inverter received: " + Port.printBuffer(requestFrame));

                // if a plugin is set
                if (getPlugin() != null) {
                    // call the the plugin to manipulate the frame
                    getPlugin().onBatteryAggregation(presetPack);
                }

                final List<ByteBuffer> sendFrames = createSendFrames(requestFrame, presetPack);

                if (sendFrames != null && !sendFrames.isEmpty()) {
                    for (ByteBuffer frame : sendFrames) {
                        // keep a reference on the frame being processed for the error log
                        currentFrame = frame;

                        // if a plugin is set
                        if (getPlugin() != null) {
                            // call the the plugin to manipulate the frame
                            frame = getPlugin().onSend(frame);
                        }

                        LOG.debug("Inverter send: {}", Port.printBuffer(frame));
                        sendFrame(port, frame);
                    }
                }
            } catch (final Throwable e) {
                LOG.error("Failed to send frame to inverter:" + Port.printBuffer(currentFrame), e);
            }

            try {
                callback.run();
            } catch (final Exception e) {
                LOG.error("Inverter process callback threw an exception!", e);
            }
        } else {
            LOG.debug("No battery data yet received to send to inverter!");
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
     */
    protected void updateAggregatedBatteryInfo() {
        presetPack.maxPackChargeCurrent = Integer.MAX_VALUE;
        presetPack.maxPackDischargeCurrent = Integer.MAX_VALUE;

        // sum all values
        for (final BatteryPack pack : energyStorage.getBatteryPacks()) {
            presetPack.ratedCapacitymAh += pack.ratedCapacitymAh;
            presetPack.ratedCellmV += pack.ratedCellmV;
            presetPack.maxPackVoltageLimit += pack.maxPackVoltageLimit;
            presetPack.minPackVoltageLimit += pack.minPackVoltageLimit;
            presetPack.maxPackChargeCurrent = Math.min(presetPack.maxPackChargeCurrent, pack.maxPackChargeCurrent);
            presetPack.maxPackDischargeCurrent = Math.max(presetPack.maxPackDischargeCurrent, pack.maxPackDischargeCurrent);
            presetPack.packVoltage += pack.packVoltage;
            presetPack.packCurrent += pack.packCurrent;
            presetPack.packSOC += pack.packSOC;
            presetPack.packSOH += pack.packSOH;
            presetPack.maxCellmV = Math.max(presetPack.maxCellmV, pack.maxCellmV);
            presetPack.maxCellVNum = pack.maxCellmV == presetPack.maxCellmV ? pack.maxCellVNum : presetPack.maxCellVNum;
            presetPack.minCellmV = Math.min(presetPack.minCellmV, pack.minCellmV);
            presetPack.minCellVNum = pack.minCellmV == presetPack.minCellmV ? pack.minCellVNum : presetPack.minCellVNum;
            presetPack.tempMax = Math.max(presetPack.tempMax, pack.tempMax);
            presetPack.tempMin = Math.min(presetPack.tempMin, pack.tempMin);

            // result.chargeDischargeStatus = pack.chargeDischargeStatus;
            presetPack.chargeMOSState |= pack.chargeMOSState;
            presetPack.dischargeMOSState |= pack.dischargeMOSState;
            presetPack.forceCharge |= pack.forceCharge;
            presetPack.remainingCapacitymAh += pack.remainingCapacitymAh;
            presetPack.numberOfCells += pack.numberOfCells;
            presetPack.chargerState |= pack.chargerState;
            presetPack.loadState |= pack.loadState;
            presetPack.bmsCycles = Math.max(presetPack.bmsCycles, pack.bmsCycles);
            // cellVmV
            // cellTemperature
            // cellBalanceState
            presetPack.cellBalanceActive |= pack.cellBalanceActive;

            aggregateAlarms(presetPack, pack.getAlarms(AlarmLevel.WARNING, AlarmLevel.ALARM));

            presetPack.tempMaxCellNum = Math.max(presetPack.tempMaxCellNum, pack.tempMaxCellNum);
            presetPack.tempMinCellNum = Math.min(presetPack.tempMinCellNum, pack.tempMinCellNum);
            presetPack.maxModulemV = Math.max(presetPack.maxModulemV, pack.maxModulemV);
            presetPack.minModulemV = Math.min(presetPack.minModulemV, pack.minModulemV);
            presetPack.maxModulemVNum = pack.maxModulemV == presetPack.maxModulemV ? pack.maxModulemVNum : presetPack.maxModulemVNum;
            presetPack.minModulemVNum = pack.minModulemV == presetPack.minModulemV ? pack.minModulemVNum : presetPack.minModulemVNum;
            presetPack.maxModuleTemp = Math.max(presetPack.maxModuleTemp, pack.maxModuleTemp);
            presetPack.minModuleTemp = Math.min(presetPack.minModuleTemp, pack.minModuleTemp);
            presetPack.maxModuleTempNum = pack.maxModuleTemp == presetPack.maxModuleTemp ? pack.maxModuleTempNum : presetPack.maxModuleTempNum;
            presetPack.minModuleTempNum = pack.minModuleTemp == presetPack.minModuleTemp ? pack.minModuleTempNum : presetPack.minModuleTempNum;
            presetPack.modulesInSeries += pack.modulesInSeries;
            presetPack.moduleNumberOfCells += pack.moduleNumberOfCells;
            presetPack.moduleVoltage += pack.moduleVoltage;
            presetPack.moduleRatedCapacityAh += pack.moduleRatedCapacityAh;
        }

        // calculate averages
        final int count = energyStorage.getBatteryPacks().size();

        if (count > 0) {
            presetPack.ratedCapacitymAh = presetPack.ratedCapacitymAh / count;
            presetPack.ratedCellmV = presetPack.ratedCellmV / count;
            presetPack.maxPackVoltageLimit = presetPack.maxPackVoltageLimit / count;
            presetPack.minPackVoltageLimit = presetPack.minPackVoltageLimit / count;
            presetPack.packVoltage = presetPack.packVoltage / count;
            presetPack.packSOC = presetPack.packSOC / count;
            presetPack.packSOH = presetPack.packSOH / count;
            presetPack.tempAverage = presetPack.tempAverage / count;
            presetPack.bmsCycles = presetPack.bmsCycles / count;
            presetPack.moduleVoltage = presetPack.moduleVoltage / count;
            presetPack.moduleRatedCapacityAh = presetPack.moduleRatedCapacityAh / count;

            // other calculations
            presetPack.cellDiffmV = presetPack.maxCellmV - presetPack.minCellmV;
            presetPack.type = energyStorage.getBatteryPack(0).type;
            presetPack.manufacturerCode = energyStorage.getBatteryPack(0).manufacturerCode;
            presetPack.hardwareVersion = energyStorage.getBatteryPack(0).hardwareVersion;
            presetPack.softwareVersion = energyStorage.getBatteryPack(0).softwareVersion;
        }
    }


    private void aggregateAlarms(final BatteryPack result, final Map<Alarm, AlarmLevel> alarms) {
        try {

            for (final Map.Entry<Alarm, AlarmLevel> entry : alarms.entrySet()) {
                final AlarmLevel level = result.getAlarmLevel(entry.getKey());

                if (level == null || level == AlarmLevel.WARNING && entry.getValue() == AlarmLevel.ALARM) {
                    result.setAlarm(entry.getKey(), entry.getValue());
                }
            }
        } catch (final Exception e) {
            LOG.error("Error aggregating alarms!", e);
        }
    }


    public static void main(final String[] args) {
        final EnergyStorage storage = new EnergyStorage();

        final BatteryPack pack1 = new BatteryPack();
        pack1.chargeMOSState = false;
        storage.getBatteryPacks().add(pack1);

        final BatteryPack pack2 = new BatteryPack();
        pack2.chargeMOSState = true;
        storage.getBatteryPacks().add(pack2);

        final BatteryPack pack3 = new BatteryPack();
        pack3.chargeMOSState = false;
        storage.getBatteryPacks().add(pack3);

        final Inverter inverter = new Inverter() {

            @Override
            protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
                // TODO Auto-generated method stub

            }


            @Override
            protected ByteBuffer readRequest(final Port port) throws IOException {
                // TODO Auto-generated method stub
                return null;
            }


            @Override
            protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
                // TODO Auto-generated method stub
                return null;
            }
        };
        inverter.energyStorage = storage;

        inverter.updateAggregatedBatteryInfo();
        System.out.println(inverter.presetPack.chargeMOSState);

    }
}
