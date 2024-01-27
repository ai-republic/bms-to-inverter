package com.airepublic.bmstoinverter.configurator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MQTTServicePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel mqttProducerLocatorLabel;
    private final JTextField mqttProducerLocatorField;
    private final JLabel mqttProducerTopicLabel;
    private final JTextField mqttProducerTopicField;
    private final JCheckBox activateMQTTBrokerCheckBox;
    private final JCheckBox activateMQTTProducerCheckBox;
    private final JLabel mqttBrokerLocatorLabel;
    private final JTextField mqttBrokerLocatorField;
    private final JLabel mqttBrokerTopicLabel;
    private final JTextField mqttBrokerTopicField;

    public MQTTServicePanel() {

        final GridBagLayout gbl_mqttPanel = new GridBagLayout();
        gbl_mqttPanel.columnWidths = new int[] { 80, 0 };
        gbl_mqttPanel.rowHeights = new int[] { 30, 30, 30, 30, 30, 30, 30 };
        gbl_mqttPanel.columnWeights = new double[] { 0.0, 1.0 };
        gbl_mqttPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        setLayout(gbl_mqttPanel);

        activateMQTTProducerCheckBox = new JCheckBox("MQTT producer");
        activateMQTTProducerCheckBox.setToolTipText("Activate this if you want to send BMS data to a MQTT broker, e.g. HomeAssistant");
        final GridBagConstraints gbc_activateMQTTProducerCheckBox = new GridBagConstraints();
        gbc_activateMQTTProducerCheckBox.anchor = GridBagConstraints.WEST;
        gbc_activateMQTTProducerCheckBox.gridwidth = 2;
        gbc_activateMQTTProducerCheckBox.insets = new Insets(0, 0, 5, 0);
        gbc_activateMQTTProducerCheckBox.gridx = 0;
        gbc_activateMQTTProducerCheckBox.gridy = 0;
        add(activateMQTTProducerCheckBox, gbc_activateMQTTProducerCheckBox);

        mqttProducerLocatorLabel = new JLabel("Locator");
        mqttProducerLocatorLabel.setEnabled(false);
        final GridBagConstraints gbc_mqttProducerLocatorLabel = new GridBagConstraints();
        gbc_mqttProducerLocatorLabel.anchor = GridBagConstraints.EAST;
        gbc_mqttProducerLocatorLabel.insets = new Insets(0, 0, 5, 5);
        gbc_mqttProducerLocatorLabel.gridx = 0;
        gbc_mqttProducerLocatorLabel.gridy = 1;
        add(mqttProducerLocatorLabel, gbc_mqttProducerLocatorLabel);

        mqttProducerLocatorField = new JTextField("tcp://127.0.0.1:61616");
        mqttProducerLocatorField.setColumns(10);
        mqttProducerLocatorField.setEnabled(false);
        final GridBagConstraints gbc_mqttProducerLocatorField = new GridBagConstraints();
        gbc_mqttProducerLocatorField.fill = GridBagConstraints.BOTH;
        gbc_mqttProducerLocatorField.insets = new Insets(0, 0, 5, 0);
        gbc_mqttProducerLocatorField.gridx = 1;
        gbc_mqttProducerLocatorField.gridy = 1;
        add(mqttProducerLocatorField, gbc_mqttProducerLocatorField);

        mqttProducerTopicLabel = new JLabel("Topic");
        mqttProducerTopicLabel.setEnabled(false);
        final GridBagConstraints gbc_mqttProducerTopicLabel = new GridBagConstraints();
        gbc_mqttProducerTopicLabel.anchor = GridBagConstraints.EAST;
        gbc_mqttProducerTopicLabel.insets = new Insets(0, 0, 5, 5);
        gbc_mqttProducerTopicLabel.gridx = 0;
        gbc_mqttProducerTopicLabel.gridy = 2;
        add(mqttProducerTopicLabel, gbc_mqttProducerTopicLabel);

        mqttProducerTopicField = new JTextField("energystorage");
        mqttProducerTopicField.setColumns(10);
        mqttProducerTopicField.setEnabled(false);
        final GridBagConstraints gbc_mqttProducerTopicField = new GridBagConstraints();
        gbc_mqttProducerTopicField.insets = new Insets(0, 0, 5, 0);
        gbc_mqttProducerTopicField.fill = GridBagConstraints.BOTH;
        gbc_mqttProducerTopicField.gridx = 1;
        gbc_mqttProducerTopicField.gridy = 2;
        add(mqttProducerTopicField, gbc_mqttProducerTopicField);

        activateMQTTBrokerCheckBox = new JCheckBox("MQTT Broker Server");
        activateMQTTBrokerCheckBox.setToolTipText("Activate only if you want to connect other MQTT clients to your broker");
        final GridBagConstraints gbc_activateMQTTBrokerCheckBox = new GridBagConstraints();
        gbc_activateMQTTBrokerCheckBox.insets = new Insets(0, 0, 5, 0);
        gbc_activateMQTTBrokerCheckBox.gridwidth = 2;
        gbc_activateMQTTBrokerCheckBox.fill = GridBagConstraints.VERTICAL;
        gbc_activateMQTTBrokerCheckBox.anchor = GridBagConstraints.WEST;
        gbc_activateMQTTBrokerCheckBox.gridx = 0;
        gbc_activateMQTTBrokerCheckBox.gridy = 3;
        add(activateMQTTBrokerCheckBox, gbc_activateMQTTBrokerCheckBox);

        mqttBrokerLocatorLabel = new JLabel("Locator");
        mqttBrokerLocatorLabel.setEnabled(false);
        final GridBagConstraints gbc_mqttBrokerLocatorLabel = new GridBagConstraints();
        gbc_mqttBrokerLocatorLabel.anchor = GridBagConstraints.EAST;
        gbc_mqttBrokerLocatorLabel.insets = new Insets(0, 0, 5, 5);
        gbc_mqttBrokerLocatorLabel.gridx = 0;
        gbc_mqttBrokerLocatorLabel.gridy = 4;
        add(mqttBrokerLocatorLabel, gbc_mqttBrokerLocatorLabel);

        mqttBrokerLocatorField = new JTextField("tcp://127.0.0.1:61616");
        mqttBrokerLocatorField.setColumns(10);
        mqttBrokerLocatorField.setEnabled(false);
        final GridBagConstraints gbc_mqttBrokerLocatorField = new GridBagConstraints();
        gbc_mqttBrokerLocatorField.insets = new Insets(0, 0, 5, 0);
        gbc_mqttBrokerLocatorField.fill = GridBagConstraints.BOTH;
        gbc_mqttBrokerLocatorField.gridx = 1;
        gbc_mqttBrokerLocatorField.gridy = 4;
        add(mqttBrokerLocatorField, gbc_mqttBrokerLocatorField);

        mqttBrokerTopicLabel = new JLabel("Topic");
        mqttBrokerTopicLabel.setEnabled(false);
        final GridBagConstraints gbc_mqttBrokerTopicLabel = new GridBagConstraints();
        gbc_mqttBrokerTopicLabel.anchor = GridBagConstraints.EAST;
        gbc_mqttBrokerTopicLabel.insets = new Insets(0, 0, 0, 5);
        gbc_mqttBrokerTopicLabel.gridx = 0;
        gbc_mqttBrokerTopicLabel.gridy = 5;
        add(mqttBrokerTopicLabel, gbc_mqttBrokerTopicLabel);

        mqttBrokerTopicField = new JTextField("energystorage");
        mqttBrokerTopicField.setColumns(10);
        mqttBrokerTopicField.setEnabled(false);
        final GridBagConstraints gbc_mqttBrokerTopicField = new GridBagConstraints();
        gbc_mqttBrokerTopicField.fill = GridBagConstraints.BOTH;
        gbc_mqttBrokerTopicField.gridx = 1;
        gbc_mqttBrokerTopicField.gridy = 5;
        add(mqttBrokerTopicField, gbc_mqttBrokerTopicField);

        activateMQTTBrokerCheckBox.addActionListener(t -> {
            mqttBrokerLocatorLabel.setEnabled(activateMQTTBrokerCheckBox.isSelected());
            mqttBrokerLocatorField.setEnabled(activateMQTTBrokerCheckBox.isSelected());
            mqttBrokerTopicLabel.setEnabled(activateMQTTBrokerCheckBox.isSelected());
            mqttBrokerTopicField.setEnabled(activateMQTTBrokerCheckBox.isSelected());
        });

        activateMQTTProducerCheckBox.addActionListener(t -> {
            mqttProducerLocatorLabel.setEnabled(activateMQTTProducerCheckBox.isSelected());
            mqttProducerLocatorField.setEnabled(activateMQTTProducerCheckBox.isSelected());
            mqttProducerTopicLabel.setEnabled(activateMQTTProducerCheckBox.isSelected());
            mqttProducerTopicField.setEnabled(activateMQTTProducerCheckBox.isSelected());
        });
    }


    public boolean verify(final StringBuffer errors) {
        boolean fail = false;

        if (activateMQTTBrokerCheckBox.isSelected()) {
            if (mqttBrokerLocatorField.getText().isBlank()) {
                errors.append("Missing MQTT broker locator!\n");
                fail = true;
            }

            if (mqttBrokerTopicField.getText().isBlank()) {
                errors.append("Missing MQTT broker topic!\n");
                fail = true;
            }
        }

        if (activateMQTTProducerCheckBox.isSelected()) {
            if (mqttProducerLocatorField.getText().isBlank()) {
                errors.append("Missing MQTT producer locator!\n");
                fail = true;
            }

            if (mqttProducerTopicField.getText().isBlank()) {
                errors.append("Missing MQTT producer topic!\n");
                fail = true;
            }
        }

        return !fail;
    }


    protected void generateConfiguration(final StringBuffer config) {
        if (activateMQTTBrokerCheckBox.isSelected() || activateMQTTProducerCheckBox.isSelected()) {
            config.append("#### MQTT properties ####\n");
        }

        if (activateMQTTBrokerCheckBox.isSelected()) {
            config.append("# Activate the MQTT broker if you have other consumers connecting to your MQTT broker\n");
            config.append("mqtt.broker.enabled=" + activateMQTTBrokerCheckBox.isSelected() + "\n");
            config.append("# The URL of the MQTT broker server for other consumers to connect to\n");
            config.append("mqtt.broker.locator=" + mqttProducerLocatorField.getText() + "\n");
            config.append("# The topic name on the MQTT broker to provide\n");
            config.append("mqtt.broker.topic=" + mqttProducerTopicField.getText() + "\n");
        }

        if (activateMQTTProducerCheckBox.isSelected()) {
            config.append("# Activate the MQTT producer if you want to send your BMS data to a MQTT broker, e.g. HomeAssistant\n");
            config.append("mqtt.producer.enabled=" + activateMQTTProducerCheckBox.isSelected() + "\n");
            config.append("# The URL of the MQTT broker to send to\n");
            config.append("mqtt.producer.locator=" + mqttProducerLocatorField.getText() + "\n");
            config.append("# The topic name on the MQTT broker to send to\n");
            config.append("mqtt.producer.topic=" + mqttProducerTopicField.getText() + "\n");
        }
    }


    public void enableMQTT() {
        activateMQTTBrokerCheckBox.setSelected(true);
        mqttBrokerLocatorLabel.setEnabled(activateMQTTBrokerCheckBox.isSelected());
        mqttBrokerLocatorField.setEnabled(activateMQTTBrokerCheckBox.isSelected());
        mqttBrokerTopicLabel.setEnabled(activateMQTTBrokerCheckBox.isSelected());
        mqttBrokerTopicField.setEnabled(activateMQTTBrokerCheckBox.isSelected());

        activateMQTTProducerCheckBox.setSelected(true);
        mqttProducerLocatorLabel.setEnabled(activateMQTTProducerCheckBox.isSelected());
        mqttProducerLocatorField.setEnabled(activateMQTTProducerCheckBox.isSelected());
        mqttProducerTopicLabel.setEnabled(activateMQTTProducerCheckBox.isSelected());
        mqttProducerTopicField.setEnabled(activateMQTTProducerCheckBox.isSelected());
    }


    public String getMQTTBrokerLocator() {
        return mqttBrokerLocatorField.getText();
    }


    public String getMQTTBrokerTopic() {
        return mqttBrokerTopicField.getText();
    }

}
