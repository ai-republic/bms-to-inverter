package com.airepublic.bmstoinverter.core.bms.data;

import com.airepublic.bmstoinverter.core.Port;

/**
 * Holds all the data of a set of battery cells - a battery pack - collected from the BMS.
 *
 * Comments specify the precision and units of the value.
 */
public class BatteryPack {
    /** The port via which the BMS communicates with this battery pack. */
    public transient Port port;
    // data from 0x53
    /** Battery type: 0=lithium iron, 1=ternary lithium, 2=lithium titanate */
    public int type;
    // data from 0x50
    /** Capacity of the each cell (1mAh) */
    public int ratedCapacitymAh;
    /** Nominal cell voltage (1mV) */
    public int ratedCellmV;

    // data from 0x5A
    /** Maximum total voltage (0.1V) */
    public int maxPackVoltageLimit;
    /** Minimum total voltage (0.1V) */
    public int minPackVoltageLimit;

    // data from 0x5B
    /** Maximum total charge current (30000 - x 0.1A) */
    public int maxPackChargeCurrent;
    /** Maximum total discharge current (30000 - x 0.1A) */
    public int maxPackDischargeCurrent;

    // data from 0x90
    /** Total pack voltage (0.1 V) */
    public int packVoltage;
    /** Current in (+) or out (-) of pack (0.1 A) */
    public int packCurrent;
    /** State Of Charge (0.1%) */
    public int packSOC;
    /** State of Health (0.1%) */
    public int packSOH;

    // data from 0x91
    /** Maximum cell voltage (mV) */
    public int maxCellmV;
    /** Number of cell with highest voltage */
    public int maxCellVNum;
    /** Minimum cell voltage (mV) */
    public int minCellmV;
    /** Number of cell with lowest voltage */
    public int minCellVNum;
    /** Difference between min and max cell voltages */
    public int cellDiffmV;

    // data from 0x92
    /** Maximum temperature sensor reading (1C) */
    public int tempMax;
    /** Minimum temperature sensor reading (1C) */
    public int tempMin;
    /** Average of temp sensors */
    public int tempAverage;

    // data from 0x93
    /** charge/discharge status (0 stationary, 1 charge, 2 discharge) */
    public String chargeDischargeStatus;
    /** charging MOSFET status */
    public boolean chargeMOSState;
    /** discharge MOSFET state */
    public boolean disChargeMOSState;
    /** BMS life (0~255 cycles)? */
    public int bmsHeartBeat;
    /** residual capacity mAH */
    public int remainingCapacitymAh;

    // data from 0x94
    /** Cell count */
    public int numberOfCells;
    /** Temp sensor count */
    public int numOfTempSensors;
    /** charger status 0 = disconnected 1 = connected */
    public boolean chargeState;
    /** Load Status 0=disconnected 1=connected */
    public boolean loadState;
    /** No information about this */
    public boolean dIO[] = new boolean[8];
    /** charge / discharge cycles */
    public int bmsCycles;

    // data from 0x95
    /** Store Cell Voltages (mV) */
    public int cellVmV[] = new int[48];

    // data from 0x96
    /** array of cell Temperature sensors */
    public int cellTemperature[] = new int[16];

    // data from 0x97
    /** boolean array of cell balance states */
    public boolean[] cellBalanceState = new boolean[48];
    /** boolean is cell balance active */
    public boolean cellBalanceActive;

    // data from 0x98
    /** alarm states */
    public Alarms alarms = new Alarms();
    //
    /** The manufacturer code */
    public String manufacturerCode;

    /**
     * The {@link Port} to use to communicate with this {@link BatteryPack}.
     *
     * @param port the {@link Port}
     */
    public BatteryPack(final Port port) {
        this.port = port;
    }

}
