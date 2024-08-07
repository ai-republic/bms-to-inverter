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
package com.airepublic.bmstoinverter.configurator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Properties;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class WebserverServicePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField webserverPortField;
    private final NumberInputVerifier numberInputVerifier = new NumberInputVerifier();

    public WebserverServicePanel() {
        setBorder(new EmptyBorder(0, 20, 0, 0));

        final GridBagLayout gbl_webserverPanel = new GridBagLayout();
        gbl_webserverPanel.columnWidths = new int[] { 80, 0 };
        gbl_webserverPanel.rowHeights = new int[] { 30 };
        gbl_webserverPanel.columnWeights = new double[] { 1.0, 1.0 };
        gbl_webserverPanel.rowWeights = new double[] { 1.0 };
        setLayout(gbl_webserverPanel);

        final JLabel webserverPortLabel = new JLabel("Port");
        final GridBagConstraints gbc_webserverPortLabel = new GridBagConstraints();
        gbc_webserverPortLabel.anchor = GridBagConstraints.EAST;
        gbc_webserverPortLabel.insets = new Insets(0, 0, 5, 5);
        gbc_webserverPortLabel.gridx = 0;
        gbc_webserverPortLabel.gridy = 0;
        add(webserverPortLabel, gbc_webserverPortLabel);

        webserverPortField = new JTextField("8080");
        final GridBagConstraints gbc_webserverPortField = new GridBagConstraints();
        gbc_webserverPortField.fill = GridBagConstraints.BOTH;
        gbc_webserverPortField.insets = new Insets(0, 0, 5, 0);
        gbc_webserverPortField.gridx = 1;
        gbc_webserverPortField.gridy = 0;
        add(webserverPortField, gbc_webserverPortField);
        webserverPortField.setColumns(10);
    }


    public int getPort() {
        return Integer.valueOf(webserverPortField.getText());
    }


    public boolean verify(final StringBuffer errors) {
        if (webserverPortField.getText().isBlank()) {
            errors.append("Missing webserver port!\n");
            return false;
        } else if (!numberInputVerifier.verify(webserverPortField.getText())) {
            errors.append("Non-numeric webserver port!\n");
            return false;
        }
        return true;
    }


    protected void generateConfiguration(final String mqttLocator, final String mqttTopic, final StringBuffer config) {
        config.append("#### Webserver properties ####\n");
        config.append("webserver.service.enabled=true\n");
        config.append("# The webserver port\n");
        config.append("server.port=" + getPort() + "\n");
        config.append("# The webserver MQTT server to connect to\n");
        config.append("server.mqtt.locator=" + mqttLocator + "\n");
        config.append("# The webserver MQTT topic to connect to\n");
        config.append("server.mqtt.topic=" + mqttTopic + "\n");
    }


    void setConfiguration(final Properties config) {

    }

}
