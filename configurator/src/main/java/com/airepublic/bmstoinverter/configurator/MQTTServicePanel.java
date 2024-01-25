package com.airepublic.bmstoinverter.configurator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class MQTTServicePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField mqttLocatorField;
    private final JTextField mqttTopicField;

    public MQTTServicePanel() {
        setBorder(new EmptyBorder(0, 20, 0, 20));

        final GridBagLayout gbl_mqttPanel = new GridBagLayout();
        gbl_mqttPanel.columnWidths = new int[] { 80, 0 };
        gbl_mqttPanel.rowHeights = new int[] { 30, 30 };
        gbl_mqttPanel.columnWeights = new double[] { 0.0, 1.0 };
        gbl_mqttPanel.rowWeights = new double[] { 0.0, 0.0 };
        setLayout(gbl_mqttPanel);

        final JLabel mqttLocatorLabel = new JLabel("Locator");
        final GridBagConstraints gbc_mqttLocatorLabel = new GridBagConstraints();
        gbc_mqttLocatorLabel.anchor = GridBagConstraints.EAST;
        gbc_mqttLocatorLabel.insets = new Insets(0, 0, 5, 5);
        gbc_mqttLocatorLabel.gridx = 0;
        gbc_mqttLocatorLabel.gridy = 0;
        add(mqttLocatorLabel, gbc_mqttLocatorLabel);

        mqttLocatorField = new JTextField("tcp://127.0.0.1:61616");
        mqttLocatorField.setColumns(10);
        final GridBagConstraints gbc_mqttLocatorField = new GridBagConstraints();
        gbc_mqttLocatorField.fill = GridBagConstraints.BOTH;
        gbc_mqttLocatorField.insets = new Insets(0, 0, 5, 0);
        gbc_mqttLocatorField.gridx = 1;
        gbc_mqttLocatorField.gridy = 0;
        add(mqttLocatorField, gbc_mqttLocatorField);

        final JLabel mqttTopicLabel = new JLabel("Topic");
        final GridBagConstraints gbc_mqttTopicLabel = new GridBagConstraints();
        gbc_mqttTopicLabel.anchor = GridBagConstraints.EAST;
        gbc_mqttTopicLabel.insets = new Insets(0, 0, 0, 5);
        gbc_mqttTopicLabel.gridx = 0;
        gbc_mqttTopicLabel.gridy = 1;
        add(mqttTopicLabel, gbc_mqttTopicLabel);

        mqttTopicField = new JTextField("energystorage");
        mqttTopicField.setColumns(10);
        final GridBagConstraints gbc_mqttTopicField = new GridBagConstraints();
        gbc_mqttTopicField.insets = new Insets(0, 0, 5, 0);
        gbc_mqttTopicField.fill = GridBagConstraints.BOTH;
        gbc_mqttTopicField.gridx = 1;
        gbc_mqttTopicField.gridy = 1;
        add(mqttTopicField, gbc_mqttTopicField);
    }


    public boolean verify(final StringBuffer errors) {
        boolean fail = false;

        if (mqttLocatorField.getText().isBlank()) {
            errors.append("Missing MQTT locator!\r\n");
            fail = true;
        }
        if (mqttTopicField.getText().isBlank()) {
            errors.append("Missing MQTT topic!\r\n");
            fail = true;
        }

        return !fail;
    }


    protected void generateConfiguration(final StringBuffer config) {
        config.append("#### MQTT properties ####\r\n"
                + "# The URL to of the MQTT broker\r\n");
        config.append("mqtt.locator=" + mqttLocatorField.getText() + "\r\n");
        config.append("# The topic name on the MQTT broker\r\n");
        config.append("mqtt.topic=" + mqttTopicField.getText() + "\r\n");
    }

}
