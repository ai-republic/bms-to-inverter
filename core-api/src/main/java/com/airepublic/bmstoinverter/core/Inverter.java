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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.bms.data.Alarm;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.bms.data.EnergyStorage;

import javax.inject.Inject;

/**
 * The class to identify an {@link Inverter}.
 */
public abstract class Inverter {
    private final static Logger LOG = LoggerFactory.getLogger(Inverter.class);
    private InverterConfig config;
    @Inject
    private EnergyStorage energyStorage;
    private BatteryPack aggregatedPack = new BatteryPack();
    private Set<InverterPlugin> plugins;

    /**
     * Constructor.
     */
    public Inverter() {
    }


    /**
     * Initializes the {@link Inverter} with the specified {@link InverterConfig}, initializing the
     * port parameters from the system properties.
     */
    public void initialize(final InverterConfig config) {
        this.config = config;

        LOG.info("Using plugins: " + plugins);
        if (getPlugins() != null) {
            getPlugins().stream().forEach(p -> {
                LOG.debug("Calling inverter plugin (onInitialize): {}", p.getName());
                p.onInitialize(this);
            });
        }

        if (!PortAllocator.hasPort(config.getPortLocator())) {
            PortAllocator.addPort(config.getPortLocator(), config.getDescriptor().createPort(config));
        }
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
        if (energyStorage == null) {
            energyStorage = new EnergyStorage();
        }

        return energyStorage;
    }


    /**
     * Gets the {@link InverterPlugin}s.
     *
     * @return the {@link InverterPlugin}s
     */
    public Set<InverterPlugin> getPlugins() {
        return plugins;
    }


    /**
     * Sets the {@link InverterPlugin}s.
     * 
     * @param plugins the {@link InverterPlugin}s to set
     */
    public void setPlugins(final Set<InverterPlugin> plugins) {
        this.plugins = plugins;

        if (plugins != null) {
            plugins.forEach(p -> LOG.info("Inverter using plugin: " + p.getName()));
        }
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
                // read any request from the inverter
                final Port port = PortAllocator.allocate(getPortLocator());
                final ByteBuffer requestFrame = readRequest(port);

                if (requestFrame != null) {
                    LOG.debug("Inverter " + config.getDescriptor().getName() + " received: " + Port.printBuffer(requestFrame));

                    // if a plugin is set
                    if (getPlugins() != null) {
                        // call the plugin to manipulate the frame
                        getPlugins().stream().forEach(p -> {
                            LOG.debug("Calling inverter plugin (onReceive): {}", p.getName());
                            p.onReceive(requestFrame);
                        });

                        LOG.debug("Inverter " + config.getDescriptor().getName() + " received (after running plugins): " + Port.printBuffer(requestFrame));
                    }
                }

                // aggregate all battery packs into one
                aggregatedPack = aggregatedBatteryInfo();

                // if a plugin is set
                if (getPlugins() != null) {
                    // call the plugin to manipulate the frame
                    getPlugins().stream().forEach(p -> {
                        LOG.debug("Calling inverter plugin (onBatteryAggregation): {}", p.getName());
                        p.onBatteryAggregation(aggregatedPack);
                    });
                }

                // create send frames based on the aggregated data
                final List<ByteBuffer> sendFrames = createSendFrames(requestFrame, aggregatedPack);

                if (sendFrames != null && !sendFrames.isEmpty()) {
                    for (final ByteBuffer frame : sendFrames) {
                        // keep a reference on the frame being processed for the error log
                        currentFrame = frame;

                        // if a plugin is set
                        if (getPlugins() != null) {
                            // call the plugin to manipulate the frame
                            getPlugins().stream().forEach(p -> {
                                LOG.debug("Calling inverter plugin (onSend): {}", p.getName());
                                p.onSend(frame);
                            });
                        }

                        LOG.debug("Inverter " + config.getDescriptor().getName() + " send: {}", Port.printBuffer(frame));
                        sendFrame(port, frame);
                    }
                }
            } catch (final Throwable e) {
                LOG.error("Failed to send frame to inverter " + config.getDescriptor().getName() + " :" + Port.printBuffer(currentFrame), e);
            }

            try {
                callback.run();
            } catch (final Exception e) {
                LOG.error("Inverter process callback threw an exception!", e);
            }
        } else {
            LOG.debug("No battery data yet received to send to inverter " + config.getDescriptor().getName() + "!");
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
    protected BatteryPack aggregatedBatteryInfo() {
        final BatteryPack result = new BatteryPack();
        result.packSOC = 0;
        result.maxPackChargeCurrent = Integer.MAX_VALUE;
        result.maxPackDischargeCurrent = Integer.MIN_VALUE;
        result.maxPackVoltageLimit = Integer.MAX_VALUE;
        result.minPackVoltageLimit = Integer.MIN_VALUE;
        result.maxCellmV = Integer.MIN_VALUE;
        result.minCellmV = Integer.MAX_VALUE;
        result.tempMax = Integer.MIN_VALUE;
        result.tempMin = Integer.MAX_VALUE;
        result.maxModulemV = Integer.MIN_VALUE;
        result.minModulemV = Integer.MAX_VALUE;
        result.maxModuleTemp = Integer.MIN_VALUE;
        result.minModuleTemp = Integer.MAX_VALUE;

        // sum all values
        for (final BatteryPack pack : energyStorage.getBatteryPacks()) {
            result.ratedCapacitymAh += pack.ratedCapacitymAh;

            result.ratedCellmV += pack.ratedCellmV;
            result.maxPackVoltageLimit = Math.min(result.maxPackVoltageLimit, pack.maxPackVoltageLimit);
            result.minPackVoltageLimit = Math.max(result.minPackVoltageLimit, pack.minPackVoltageLimit);
            result.maxPackChargeCurrent = Math.min(result.maxPackChargeCurrent, pack.maxPackChargeCurrent);
            result.maxPackDischargeCurrent = Math.max(result.maxPackDischargeCurrent, pack.maxPackDischargeCurrent);
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
            result.dischargeMOSState |= pack.dischargeMOSState;
            result.forceCharge |= pack.forceCharge;
            result.remainingCapacitymAh += pack.remainingCapacitymAh;
            result.numberOfCells += pack.numberOfCells;
            result.chargerState |= pack.chargerState;
            result.loadState |= pack.loadState;
            result.bmsCycles = Math.max(result.bmsCycles, pack.bmsCycles);
            // cellVmV
            // cellTemperature
            // cellBalanceState
            result.cellBalanceActive |= pack.cellBalanceActive;

            aggregateAlarms(result, pack.getAlarms(AlarmLevel.WARNING, AlarmLevel.ALARM));

            result.tempMaxCellNum = result.tempMax == pack.tempMax ? pack.tempMaxCellNum : result.tempMaxCellNum;
            result.tempMinCellNum = result.tempMin == pack.tempMin ? pack.tempMinCellNum : result.tempMinCellNum;
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

        if (count > 0) {
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
        }

        // check if values were not set and set them to some default
        if (result.maxPackChargeCurrent == Integer.MAX_VALUE) {
            result.maxPackChargeCurrent = 0;
        }

        if (result.maxPackDischargeCurrent == Integer.MIN_VALUE) {
            result.maxPackDischargeCurrent = 0;
        }
        if (result.maxPackVoltageLimit == Integer.MAX_VALUE) {
            result.maxPackVoltageLimit = 500;
        }
        if (result.minPackVoltageLimit == Integer.MIN_VALUE) {
            result.minPackVoltageLimit = 500;
        }
        if (result.maxCellmV == Integer.MIN_VALUE) {
            result.maxCellmV = 3000;
        }
        if (result.minCellmV == Integer.MAX_VALUE) {
            result.minCellmV = 3000;
        }
        if (result.tempMax == Integer.MIN_VALUE) {
            result.tempMax = 250;
        }
        if (result.tempMin == Integer.MAX_VALUE) {
            result.tempMin = 250;
        }
        if (result.maxModulemV == Integer.MIN_VALUE) {
            result.maxModulemV = 3000;
        }
        if (result.minModulemV == Integer.MAX_VALUE) {
            result.minModulemV = 3000;
        }
        if (result.maxModuleTemp == Integer.MIN_VALUE) {
            result.maxModuleTemp = 250;
        }
        if (result.minModuleTemp == Integer.MAX_VALUE) {
            result.minModuleTemp = 250;
        }

        return result;
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

    // public static void main(final String[] args) {
    // final EnergyStorage storage = new EnergyStorage();
    //
    // final BatteryPack pack1 = new BatteryPack();
    // pack1.chargeMOSState = false;
    // storage.getBatteryPacks().add(pack1);
    //
    // final BatteryPack pack2 = new BatteryPack();
    // pack2.chargeMOSState = true;
    // storage.getBatteryPacks().add(pack2);
    //
    // final BatteryPack pack3 = new BatteryPack();
    // pack3.chargeMOSState = false;
    // storage.getBatteryPacks().add(pack3);
    //
    // final Inverter inverter = new Inverter() {
    //
    // @Override
    // protected void sendFrame(final Port port, final ByteBuffer frame) throws IOException {
    // // TODO Auto-generated method stub
    //
    // }
    //
    //
    // @Override
    // protected ByteBuffer readRequest(final Port port) throws IOException {
    // // TODO Auto-generated method stub
    // return null;
    // }
    //
    //
    // @Override
    // protected List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack
    // aggregatedPack) {
    // // TODO Auto-generated method stub
    // return null;
    // }
    // };
    // inverter.energyStorage = storage;
    //
    // final BatteryPack aggregatedPack = inverter.aggregatedBatteryInfo();
    // System.out.println(aggregatedPack.chargeMOSState);
    //
    // }
}
