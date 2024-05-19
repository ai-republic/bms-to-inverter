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
package com.airepublic.bmstoinverter.service.mqtt;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.service.IMQTTConsumerService;

/**
 * The implementation of the {@link IMQTTConsumerService} using the ActiveMQ Artemis implementation.
 */
public class MQTTConsumerService implements IMQTTConsumerService {
    private final static Logger LOG = LoggerFactory.getLogger(MQTTConsumerService.class);
    private boolean running = false;
    private ClientSession session;
    private ClientConsumer consumer;
    private String locator;

    @Override
    public MQTTConsumerService createQueueOnAddress(final String locator, final String address, final Consumer<String> messageHandler) throws IOException {
        this.locator = locator;

        try {
            final ServerLocator serverLocator = ActiveMQClient.createServerLocator(locator);
            final ClientSessionFactory factory = serverLocator.createSessionFactory();
            session = factory.createSession();
            session.start();

            final String name = UUID.randomUUID().toString();
            final QueueConfiguration config = new QueueConfiguration()
                    .setAddress(address)
                    .setName(name)
                    .setRingSize(1L)
                    .setRoutingType(RoutingType.MULTICAST)
                    .setAutoDelete(false);
            session.createQueue(config);
            consumer = session.createConsumer(address + "::" + name);
            consumer.setMessageHandler(msg -> messageHandler.accept(msg.getBodyBuffer().readString()));

            running = true;
            return this;
        } catch (final Exception e) {
            LOG.error("Error starting MQTT service!", e);
            try {
                close();
            } catch (final Exception e1) {
            }

            throw new IOException("Could not create MQTT producer client at " + locator + " on address " + address, e);
        }
    }


    @Override
    public boolean isRunning() {
        if (session != null && session.isClosed() || consumer != null && consumer.isClosed()) {
            stop();
        }

        return running;
    }


    @Override
    public void stop() {
        try {
            if (consumer != null) {
                try {
                    consumer.close();
                    consumer = null;
                } catch (final Exception e) {
                }
            }

            if (session != null) {
                session.stop();
                session.close();
                session = null;
                running = false;
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to stop MQTT consumer!", e);
        }
    }


    @Override
    public void close() throws Exception {
        try {
            stop();
            LOG.info("Shutting down MQTT consumer on '{}'...OK", locator);
        } catch (final Exception e) {
            LOG.error("Shutting down MQTT consumer on '{}'...FAILED", locator, e);
        }
    }


    /**
     * Main method to test the consumer.
     *
     * @param args none
     */
    public static void main(final String[] args) {
        final String locator = "tcp://127.0.0.1:61616";
        final String topic = "energystorage";

        try {
            final MQTTProducerService producer = new MQTTProducerService();
            producer.connect(locator, topic);
            producer.sendMessage(
                    "{\"batteryPacks\":[{\"packNumber\":0,\"packVoltage\":532,\"packCurrent\":0,\"packSOC\":992,\"maxCellmV\":3343,\"maxCellVNum\":4,\"minCellmV\":3323,\"minCellVNum\":10,\"cellDiff\":20,\"tempMax\":35,\"tempMin\":35,\"tempAverage\":35,\"chargeDischargeStatus\":\"Stationary\",\"chargeMOSState\":true,\"disChargeMOSState\":true,\"bmsHeartBeat\":114,\"remainingCapacitymAh\":89280,\"numberOfCells\":16,\"numOfTempSensors\":2,\"chargeState\":false,\"loadState\":false,\"dIO\":[false,false,false,false,false,false,false,false],\"bmsCycles\":33,\"cellVmV\":[3323,3331,3330,3341,3336,3331,3332,3329,3335,3322,3332,3333,3332,3326,3330,3328,3326,3330,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellTemperature\":[35,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellBalanceState\":[false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false],\"cellBalanceActive\":false,\"alarms\":{\"levelOneCellVoltageTooHigh\":false,\"levelTwoCellVoltageTooHigh\":false,\"levelOneCellVoltageTooLow\":false,\"levelTwoCellVoltageTooLow\":false,\"levelOnePackVoltageTooHigh\":false,\"levelTwoPackVoltageTooHigh\":false,\"levelOnePackVoltageTooLow\":false,\"levelTwoPackVoltageTooLow\":false,\"levelOneChargeTempTooHigh\":false,\"levelTwoChargeTempTooHigh\":false,\"levelOneChargeTempTooLow\":false,\"levelTwoChargeTempTooLow\":false,\"levelOneDischargeTempTooHigh\":false,\"levelTwoDischargeTempTooHigh\":false,\"levelOneDischargeTempTooLow\":false,\"levelTwoDischargeTempTooLow\":false,\"levelOneChargeCurrentTooHigh\":false,\"levelTwoChargeCurrentTooHigh\":false,\"levelOneDischargeCurrentTooHigh\":false,\"levelTwoDischargeCurrentTooHigh\":false,\"levelOneStateOfChargeTooHigh\":false,\"levelTwoStateOfChargeTooHigh\":false,\"levelOneStateOfChargeTooLow\":false,\"levelTwoStateOfChargeTooLow\":false,\"levelOneCellVoltageDifferenceTooHigh\":false,\"levelTwoCellVoltageDifferenceTooHigh\":false,\"levelOneTempSensorDifferenceTooHigh\":false,\"levelTwoTempSensorDifferenceTooHigh\":false,\"chargeFETTemperatureTooHigh\":false,\"dischargeFETTemperatureTooHigh\":false,\"failureOfChargeFETTemperatureSensor\":false,\"failureOfDischargeFETTemperatureSensor\":false,\"failureOfChargeFETAdhesion\":false,\"failureOfDischargeFETAdhesion\":false,\"failureOfChargeFETTBreaker\":false,\"failureOfDischargeFETBreaker\":false,\"failureOfAFEAcquisitionModule\":false,\"failureOfVoltageSensorModule\":false,\"failureOfTemperatureSensorModule\":false,\"failureOfEEPROMStorageModule\":false,\"failureOfRealtimeClockModule\":false,\"failureOfPrechargeModule\":false,\"failureOfVehicleCommunicationModule\":false,\"failureOfIntranetCommunicationModule\":false,\"failureOfCurrentSensorModule\":false,\"failureOfMainVoltageSensorModule\":false,\"failureOfShortCircuitProtection\":false,\"failureOfLowVoltageNoCharging\":false}},{\"packNumber\":1,\"packVoltage\":531,\"packCurrent\":0,\"packSOC\":993,\"maxCellmV\":3334,\"maxCellVNum\":14,\"minCellmV\":3190,\"minCellVNum\":1,\"cellDiff\":144,\"tempMax\":35,\"tempMin\":35,\"tempAverage\":35,\"chargeDischargeStatus\":\"Stationary\",\"chargeMOSState\":true,\"disChargeMOSState\":true,\"bmsHeartBeat\":3,\"remainingCapacitymAh\":89370,\"numberOfCells\":16,\"numOfTempSensors\":2,\"chargeState\":false,\"loadState\":false,\"dIO\":[false,false,false,false,false,false,false,false],\"bmsCycles\":35,\"cellVmV\":[3188,3334,3331,3332,3334,3331,3335,3333,3332,3334,3331,3334,3334,3333,3330,3332,3333,3330,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellTemperature\":[35,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellBalanceState\":[false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false],\"cellBalanceActive\":false,\"alarms\":{\"levelOneCellVoltageTooHigh\":false,\"levelTwoCellVoltageTooHigh\":false,\"levelOneCellVoltageTooLow\":false,\"levelTwoCellVoltageTooLow\":false,\"levelOnePackVoltageTooHigh\":false,\"levelTwoPackVoltageTooHigh\":false,\"levelOnePackVoltageTooLow\":false,\"levelTwoPackVoltageTooLow\":false,\"levelOneChargeTempTooHigh\":false,\"levelTwoChargeTempTooHigh\":false,\"levelOneChargeTempTooLow\":false,\"levelTwoChargeTempTooLow\":false,\"levelOneDischargeTempTooHigh\":false,\"levelTwoDischargeTempTooHigh\":false,\"levelOneDischargeTempTooLow\":false,\"levelTwoDischargeTempTooLow\":false,\"levelOneChargeCurrentTooHigh\":false,\"levelTwoChargeCurrentTooHigh\":false,\"levelOneDischargeCurrentTooHigh\":false,\"levelTwoDischargeCurrentTooHigh\":false,\"levelOneStateOfChargeTooHigh\":false,\"levelTwoStateOfChargeTooHigh\":false,\"levelOneStateOfChargeTooLow\":false,\"levelTwoStateOfChargeTooLow\":false,\"levelOneCellVoltageDifferenceTooHigh\":false,\"levelTwoCellVoltageDifferenceTooHigh\":false,\"levelOneTempSensorDifferenceTooHigh\":false,\"levelTwoTempSensorDifferenceTooHigh\":false,\"chargeFETTemperatureTooHigh\":false,\"dischargeFETTemperatureTooHigh\":false,\"failureOfChargeFETTemperatureSensor\":false,\"failureOfDischargeFETTemperatureSensor\":false,\"failureOfChargeFETAdhesion\":false,\"failureOfDischargeFETAdhesion\":false,\"failureOfChargeFETTBreaker\":false,\"failureOfDischargeFETBreaker\":false,\"failureOfAFEAcquisitionModule\":false,\"failureOfVoltageSensorModule\":false,\"failureOfTemperatureSensorModule\":false,\"failureOfEEPROMStorageModule\":false,\"failureOfRealtimeClockModule\":false,\"failureOfPrechargeModule\":false,\"failureOfVehicleCommunicationModule\":false,\"failureOfIntranetCommunicationModule\":false,\"failureOfCurrentSensorModule\":false,\"failureOfMainVoltageSensorModule\":false,\"failureOfShortCircuitProtection\":false,\"failureOfLowVoltageNoCharging\":false}},{\"packNumber\":2,\"packVoltage\":532,\"packCurrent\":0,\"packSOC\":993,\"maxCellmV\":3336,\"maxCellVNum\":8,\"minCellmV\":3312,\"minCellVNum\":11,\"cellDiff\":24,\"tempMax\":37,\"tempMin\":35,\"tempAverage\":36,\"chargeDischargeStatus\":\"Stationary\",\"chargeMOSState\":true,\"disChargeMOSState\":true,\"bmsHeartBeat\":119,\"remainingCapacitymAh\":89370,\"numberOfCells\":16,\"numOfTempSensors\":2,\"chargeState\":false,\"loadState\":false,\"dIO\":[false,false,false,false,false,false,false,false],\"bmsCycles\":32,\"cellVmV\":[3334,3334,3322,3331,3336,3331,3332,3335,3334,3334,3312,3333,3333,3334,3332,3322,3334,3332,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellTemperature\":[37,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellBalanceState\":[false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false],\"cellBalanceActive\":false,\"alarms\":{\"levelOneCellVoltageTooHigh\":false,\"levelTwoCellVoltageTooHigh\":false,\"levelOneCellVoltageTooLow\":false,\"levelTwoCellVoltageTooLow\":false,\"levelOnePackVoltageTooHigh\":false,\"levelTwoPackVoltageTooHigh\":false,\"levelOnePackVoltageTooLow\":false,\"levelTwoPackVoltageTooLow\":false,\"levelOneChargeTempTooHigh\":false,\"levelTwoChargeTempTooHigh\":false,\"levelOneChargeTempTooLow\":false,\"levelTwoChargeTempTooLow\":false,\"levelOneDischargeTempTooHigh\":false,\"levelTwoDischargeTempTooHigh\":false,\"levelOneDischargeTempTooLow\":false,\"levelTwoDischargeTempTooLow\":false,\"levelOneChargeCurrentTooHigh\":false,\"levelTwoChargeCurrentTooHigh\":false,\"levelOneDischargeCurrentTooHigh\":false,\"levelTwoDischargeCurrentTooHigh\":false,\"levelOneStateOfChargeTooHigh\":false,\"levelTwoStateOfChargeTooHigh\":false,\"levelOneStateOfChargeTooLow\":false,\"levelTwoStateOfChargeTooLow\":false,\"levelOneCellVoltageDifferenceTooHigh\":false,\"levelTwoCellVoltageDifferenceTooHigh\":false,\"levelOneTempSensorDifferenceTooHigh\":false,\"levelTwoTempSensorDifferenceTooHigh\":false,\"chargeFETTemperatureTooHigh\":false,\"dischargeFETTemperatureTooHigh\":false,\"failureOfChargeFETTemperatureSensor\":false,\"failureOfDischargeFETTemperatureSensor\":false,\"failureOfChargeFETAdhesion\":false,\"failureOfDischargeFETAdhesion\":false,\"failureOfChargeFETTBreaker\":false,\"failureOfDischargeFETBreaker\":false,\"failureOfAFEAcquisitionModule\":false,\"failureOfVoltageSensorModule\":false,\"failureOfTemperatureSensorModule\":false,\"failureOfEEPROMStorageModule\":false,\"failureOfRealtimeClockModule\":false,\"failureOfPrechargeModule\":false,\"failureOfVehicleCommunicationModule\":false,\"failureOfIntranetCommunicationModule\":false,\"failureOfCurrentSensorModule\":false,\"failureOfMainVoltageSensorModule\":false,\"failureOfShortCircuitProtection\":false,\"failureOfLowVoltageNoCharging\":false}},{\"packNumber\":3,\"packVoltage\":532,\"packCurrent\":0,\"packSOC\":993,\"maxCellmV\":3336,\"maxCellVNum\":13,\"minCellmV\":3303,\"minCellVNum\":14,\"cellDiff\":33,\"tempMax\":33,\"tempMin\":33,\"tempAverage\":33,\"chargeDischargeStatus\":\"Stationary\",\"chargeMOSState\":true,\"disChargeMOSState\":true,\"bmsHeartBeat\":4,\"remainingCapacitymAh\":89370,\"numberOfCells\":16,\"numOfTempSensors\":2,\"chargeState\":false,\"loadState\":false,\"dIO\":[false,false,false,false,false,false,false,false],\"bmsCycles\":32,\"cellVmV\":[3333,3332,3335,3333,3318,3336,3333,3336,3332,3335,3334,3335,3336,3303,3335,3332,3303,3335,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellTemperature\":[33,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellBalanceState\":[false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false],\"cellBalanceActive\":false,\"alarms\":{\"levelOneCellVoltageTooHigh\":false,\"levelTwoCellVoltageTooHigh\":false,\"levelOneCellVoltageTooLow\":false,\"levelTwoCellVoltageTooLow\":false,\"levelOnePackVoltageTooHigh\":false,\"levelTwoPackVoltageTooHigh\":false,\"levelOnePackVoltageTooLow\":false,\"levelTwoPackVoltageTooLow\":false,\"levelOneChargeTempTooHigh\":false,\"levelTwoChargeTempTooHigh\":false,\"levelOneChargeTempTooLow\":false,\"levelTwoChargeTempTooLow\":false,\"levelOneDischargeTempTooHigh\":false,\"levelTwoDischargeTempTooHigh\":false,\"levelOneDischargeTempTooLow\":false,\"levelTwoDischargeTempTooLow\":false,\"levelOneChargeCurrentTooHigh\":false,\"levelTwoChargeCurrentTooHigh\":false,\"levelOneDischargeCurrentTooHigh\":false,\"levelTwoDischargeCurrentTooHigh\":false,\"levelOneStateOfChargeTooHigh\":false,\"levelTwoStateOfChargeTooHigh\":false,\"levelOneStateOfChargeTooLow\":false,\"levelTwoStateOfChargeTooLow\":false,\"levelOneCellVoltageDifferenceTooHigh\":false,\"levelTwoCellVoltageDifferenceTooHigh\":false,\"levelOneTempSensorDifferenceTooHigh\":false,\"levelTwoTempSensorDifferenceTooHigh\":false,\"chargeFETTemperatureTooHigh\":false,\"dischargeFETTemperatureTooHigh\":false,\"failureOfChargeFETTemperatureSensor\":false,\"failureOfDischargeFETTemperatureSensor\":false,\"failureOfChargeFETAdhesion\":false,\"failureOfDischargeFETAdhesion\":false,\"failureOfChargeFETTBreaker\":false,\"failureOfDischargeFETBreaker\":false,\"failureOfAFEAcquisitionModule\":false,\"failureOfVoltageSensorModule\":false,\"failureOfTemperatureSensorModule\":false,\"failureOfEEPROMStorageModule\":false,\"failureOfRealtimeClockModule\":false,\"failureOfPrechargeModule\":false,\"failureOfVehicleCommunicationModule\":false,\"failureOfIntranetCommunicationModule\":false,\"failureOfCurrentSensorModule\":false,\"failureOfMainVoltageSensorModule\":false,\"failureOfShortCircuitProtection\":false,\"failureOfLowVoltageNoCharging\":false}},{\"packNumber\":4,\"packVoltage\":533,\"packCurrent\":0,\"packSOC\":986,\"maxCellmV\":3344,\"maxCellVNum\":5,\"minCellmV\":3317,\"minCellVNum\":10,\"cellDiff\":27,\"tempMax\":35,\"tempMin\":35,\"tempAverage\":35,\"chargeDischargeStatus\":\"Stationary\",\"chargeMOSState\":true,\"disChargeMOSState\":true,\"bmsHeartBeat\":113,\"remainingCapacitymAh\":32219,\"numberOfCells\":16,\"numOfTempSensors\":2,\"chargeState\":false,\"loadState\":false,\"dIO\":[false,false,false,false,false,false,false,false],\"bmsCycles\":94,\"cellVmV\":[3329,3334,3342,3344,3345,3332,3334,3332,3330,3318,3330,3334,3318,3333,3333,3325,3333,3333,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellTemperature\":[35,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellBalanceState\":[false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false],\"cellBalanceActive\":false,\"alarms\":{\"levelOneCellVoltageTooHigh\":false,\"levelTwoCellVoltageTooHigh\":false,\"levelOneCellVoltageTooLow\":false,\"levelTwoCellVoltageTooLow\":false,\"levelOnePackVoltageTooHigh\":false,\"levelTwoPackVoltageTooHigh\":false,\"levelOnePackVoltageTooLow\":false,\"levelTwoPackVoltageTooLow\":false,\"levelOneChargeTempTooHigh\":false,\"levelTwoChargeTempTooHigh\":false,\"levelOneChargeTempTooLow\":false,\"levelTwoChargeTempTooLow\":false,\"levelOneDischargeTempTooHigh\":false,\"levelTwoDischargeTempTooHigh\":false,\"levelOneDischargeTempTooLow\":false,\"levelTwoDischargeTempTooLow\":false,\"levelOneChargeCurrentTooHigh\":false,\"levelTwoChargeCurrentTooHigh\":false,\"levelOneDischargeCurrentTooHigh\":false,\"levelTwoDischargeCurrentTooHigh\":false,\"levelOneStateOfChargeTooHigh\":false,\"levelTwoStateOfChargeTooHigh\":false,\"levelOneStateOfChargeTooLow\":false,\"levelTwoStateOfChargeTooLow\":false,\"levelOneCellVoltageDifferenceTooHigh\":false,\"levelTwoCellVoltageDifferenceTooHigh\":false,\"levelOneTempSensorDifferenceTooHigh\":false,\"levelTwoTempSensorDifferenceTooHigh\":false,\"chargeFETTemperatureTooHigh\":false,\"dischargeFETTemperatureTooHigh\":false,\"failureOfChargeFETTemperatureSensor\":false,\"failureOfDischargeFETTemperatureSensor\":false,\"failureOfChargeFETAdhesion\":false,\"failureOfDischargeFETAdhesion\":false,\"failureOfChargeFETTBreaker\":false,\"failureOfDischargeFETBreaker\":false,\"failureOfAFEAcquisitionModule\":false,\"failureOfVoltageSensorModule\":false,\"failureOfTemperatureSensorModule\":false,\"failureOfEEPROMStorageModule\":false,\"failureOfRealtimeClockModule\":false,\"failureOfPrechargeModule\":false,\"failureOfVehicleCommunicationModule\":false,\"failureOfIntranetCommunicationModule\":false,\"failureOfCurrentSensorModule\":false,\"failureOfMainVoltageSensorModule\":false,\"failureOfShortCircuitProtection\":false,\"failureOfLowVoltageNoCharging\":false}},{\"packNumber\":5,\"packVoltage\":532,\"packCurrent\":0,\"packSOC\":995,\"maxCellmV\":3336,\"maxCellVNum\":1,\"minCellmV\":3305,\"minCellVNum\":7,\"cellDiff\":31,\"tempMax\":35,\"tempMin\":35,\"tempAverage\":35,\"chargeDischargeStatus\":\"Stationary\",\"chargeMOSState\":true,\"disChargeMOSState\":true,\"bmsHeartBeat\":243,\"remainingCapacitymAh\":89550,\"numberOfCells\":16,\"numOfTempSensors\":2,\"chargeState\":false,\"loadState\":false,\"dIO\":[false,false,false,false,false,false,false,false],\"bmsCycles\":23,\"cellVmV\":[3336,3335,3335,3335,3333,3333,3305,3334,3329,3335,3329,3332,3333,3333,3330,3319,3333,3330,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellTemperature\":[35,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellBalanceState\":[false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false],\"cellBalanceActive\":false,\"alarms\":{\"levelOneCellVoltageTooHigh\":false,\"levelTwoCellVoltageTooHigh\":false,\"levelOneCellVoltageTooLow\":false,\"levelTwoCellVoltageTooLow\":false,\"levelOnePackVoltageTooHigh\":false,\"levelTwoPackVoltageTooHigh\":false,\"levelOnePackVoltageTooLow\":false,\"levelTwoPackVoltageTooLow\":false,\"levelOneChargeTempTooHigh\":false,\"levelTwoChargeTempTooHigh\":false,\"levelOneChargeTempTooLow\":false,\"levelTwoChargeTempTooLow\":false,\"levelOneDischargeTempTooHigh\":false,\"levelTwoDischargeTempTooHigh\":false,\"levelOneDischargeTempTooLow\":false,\"levelTwoDischargeTempTooLow\":false,\"levelOneChargeCurrentTooHigh\":false,\"levelTwoChargeCurrentTooHigh\":false,\"levelOneDischargeCurrentTooHigh\":false,\"levelTwoDischargeCurrentTooHigh\":false,\"levelOneStateOfChargeTooHigh\":false,\"levelTwoStateOfChargeTooHigh\":false,\"levelOneStateOfChargeTooLow\":false,\"levelTwoStateOfChargeTooLow\":false,\"levelOneCellVoltageDifferenceTooHigh\":false,\"levelTwoCellVoltageDifferenceTooHigh\":false,\"levelOneTempSensorDifferenceTooHigh\":false,\"levelTwoTempSensorDifferenceTooHigh\":false,\"chargeFETTemperatureTooHigh\":false,\"dischargeFETTemperatureTooHigh\":false,\"failureOfChargeFETTemperatureSensor\":false,\"failureOfDischargeFETTemperatureSensor\":false,\"failureOfChargeFETAdhesion\":false,\"failureOfDischargeFETAdhesion\":false,\"failureOfChargeFETTBreaker\":false,\"failureOfDischargeFETBreaker\":false,\"failureOfAFEAcquisitionModule\":false,\"failureOfVoltageSensorModule\":false,\"failureOfTemperatureSensorModule\":false,\"failureOfEEPROMStorageModule\":false,\"failureOfRealtimeClockModule\":false,\"failureOfPrechargeModule\":false,\"failureOfVehicleCommunicationModule\":false,\"failureOfIntranetCommunicationModule\":false,\"failureOfCurrentSensorModule\":false,\"failureOfMainVoltageSensorModule\":false,\"failureOfShortCircuitProtection\":false,\"failureOfLowVoltageNoCharging\":false}},{\"packNumber\":6,\"packVoltage\":533,\"packCurrent\":0,\"packSOC\":997,\"maxCellmV\":3352,\"maxCellVNum\":5,\"minCellmV\":3309,\"minCellVNum\":16,\"cellDiff\":43,\"tempMax\":35,\"tempMin\":35,\"tempAverage\":35,\"chargeDischargeStatus\":\"Stationary\",\"chargeMOSState\":true,\"disChargeMOSState\":true,\"bmsHeartBeat\":114,\"remainingCapacitymAh\":89730,\"numberOfCells\":16,\"numOfTempSensors\":2,\"chargeState\":false,\"loadState\":false,\"dIO\":[false,false,false,false,false,false,false,false],\"bmsCycles\":25,\"cellVmV\":[3352,3347,3320,3316,3351,3345,3318,3324,3346,3344,3317,3324,3345,3352,3326,3309,3352,3326,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellTemperature\":[35,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellBalanceState\":[false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false],\"cellBalanceActive\":false,\"alarms\":{\"levelOneCellVoltageTooHigh\":false,\"levelTwoCellVoltageTooHigh\":false,\"levelOneCellVoltageTooLow\":false,\"levelTwoCellVoltageTooLow\":false,\"levelOnePackVoltageTooHigh\":false,\"levelTwoPackVoltageTooHigh\":false,\"levelOnePackVoltageTooLow\":false,\"levelTwoPackVoltageTooLow\":false,\"levelOneChargeTempTooHigh\":false,\"levelTwoChargeTempTooHigh\":false,\"levelOneChargeTempTooLow\":false,\"levelTwoChargeTempTooLow\":false,\"levelOneDischargeTempTooHigh\":false,\"levelTwoDischargeTempTooHigh\":false,\"levelOneDischargeTempTooLow\":false,\"levelTwoDischargeTempTooLow\":false,\"levelOneChargeCurrentTooHigh\":false,\"levelTwoChargeCurrentTooHigh\":false,\"levelOneDischargeCurrentTooHigh\":false,\"levelTwoDischargeCurrentTooHigh\":false,\"levelOneStateOfChargeTooHigh\":false,\"levelTwoStateOfChargeTooHigh\":false,\"levelOneStateOfChargeTooLow\":false,\"levelTwoStateOfChargeTooLow\":false,\"levelOneCellVoltageDifferenceTooHigh\":false,\"levelTwoCellVoltageDifferenceTooHigh\":false,\"levelOneTempSensorDifferenceTooHigh\":false,\"levelTwoTempSensorDifferenceTooHigh\":false,\"chargeFETTemperatureTooHigh\":false,\"dischargeFETTemperatureTooHigh\":false,\"failureOfChargeFETTemperatureSensor\":false,\"failureOfDischargeFETTemperatureSensor\":false,\"failureOfChargeFETAdhesion\":false,\"failureOfDischargeFETAdhesion\":false,\"failureOfChargeFETTBreaker\":false,\"failureOfDischargeFETBreaker\":false,\"failureOfAFEAcquisitionModule\":false,\"failureOfVoltageSensorModule\":false,\"failureOfTemperatureSensorModule\":false,\"failureOfEEPROMStorageModule\":false,\"failureOfRealtimeClockModule\":false,\"failureOfPrechargeModule\":false,\"failureOfVehicleCommunicationModule\":false,\"failureOfIntranetCommunicationModule\":false,\"failureOfCurrentSensorModule\":false,\"failureOfMainVoltageSensorModule\":false,\"failureOfShortCircuitProtection\":false,\"failureOfLowVoltageNoCharging\":false}},{\"packNumber\":7,\"packVoltage\":533,\"packCurrent\":0,\"packSOC\":998,\"maxCellmV\":3346,\"maxCellVNum\":6,\"minCellmV\":3313,\"minCellVNum\":14,\"cellDiff\":33,\"tempMax\":37,\"tempMin\":37,\"tempAverage\":37,\"chargeDischargeStatus\":\"Stationary\",\"chargeMOSState\":true,\"disChargeMOSState\":true,\"bmsHeartBeat\":114,\"remainingCapacitymAh\":89820,\"numberOfCells\":16,\"numOfTempSensors\":2,\"chargeState\":false,\"loadState\":false,\"dIO\":[false,false,false,false,false,false,false,false],\"bmsCycles\":21,\"cellVmV\":[3317,3321,3342,3344,3344,3346,3345,3344,3325,3345,3345,3323,3320,3313,3321,3317,3313,3321,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellTemperature\":[37,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"cellBalanceState\":[false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false,false],\"cellBalanceActive\":false,\"alarms\":{\"levelOneCellVoltageTooHigh\":false,\"levelTwoCellVoltageTooHigh\":false,\"levelOneCellVoltageTooLow\":false,\"levelTwoCellVoltageTooLow\":false,\"levelOnePackVoltageTooHigh\":false,\"levelTwoPackVoltageTooHigh\":false,\"levelOnePackVoltageTooLow\":false,\"levelTwoPackVoltageTooLow\":false,\"levelOneChargeTempTooHigh\":false,\"levelTwoChargeTempTooHigh\":false,\"levelOneChargeTempTooLow\":false,\"levelTwoChargeTempTooLow\":false,\"levelOneDischargeTempTooHigh\":false,\"levelTwoDischargeTempTooHigh\":false,\"levelOneDischargeTempTooLow\":false,\"levelTwoDischargeTempTooLow\":false,\"levelOneChargeCurrentTooHigh\":false,\"levelTwoChargeCurrentTooHigh\":false,\"levelOneDischargeCurrentTooHigh\":false,\"levelTwoDischargeCurrentTooHigh\":false,\"levelOneStateOfChargeTooHigh\":false,\"levelTwoStateOfChargeTooHigh\":false,\"levelOneStateOfChargeTooLow\":false,\"levelTwoStateOfChargeTooLow\":false,\"levelOneCellVoltageDifferenceTooHigh\":false,\"levelTwoCellVoltageDifferenceTooHigh\":false,\"levelOneTempSensorDifferenceTooHigh\":false,\"levelTwoTempSensorDifferenceTooHigh\":false,\"chargeFETTemperatureTooHigh\":false,\"dischargeFETTemperatureTooHigh\":false,\"failureOfChargeFETTemperatureSensor\":false,\"failureOfDischargeFETTemperatureSensor\":false,\"failureOfChargeFETAdhesion\":false,\"failureOfDischargeFETAdhesion\":false,\"failureOfChargeFETTBreaker\":false,\"failureOfDischargeFETBreaker\":false,\"failureOfAFEAcquisitionModule\":false,\"failureOfVoltageSensorModule\":false,\"failureOfTemperatureSensorModule\":false,\"failureOfEEPROMStorageModule\":false,\"failureOfRealtimeClockModule\":false,\"failureOfPrechargeModule\":false,\"failureOfVehicleCommunicationModule\":false,\"failureOfIntranetCommunicationModule\":false,\"failureOfCurrentSensorModule\":false,\"failureOfMainVoltageSensorModule\":true,\"failureOfShortCircuitProtection\":true,\"failureOfLowVoltageNoCharging\":true}}]}");
            producer.close();

            final MQTTConsumerService consumer = new MQTTConsumerService();
            consumer.createQueueOnAddress(locator, topic, System.out::println);

            consumer.close();

        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // mqtt.close();
            } catch (final Exception e) {
            }
        }

    }

}
