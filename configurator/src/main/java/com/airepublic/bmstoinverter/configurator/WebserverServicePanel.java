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
    private final JTextField webserverHttpPortField;
    private final JTextField webserverHttpsPortField;
    private final NumberInputVerifier numberInputVerifier = new NumberInputVerifier();

    public WebserverServicePanel() {
        setBorder(new EmptyBorder(0, 20, 0, 0));

        final GridBagLayout gbl_webserverPanel = new GridBagLayout();
        gbl_webserverPanel.columnWidths = new int[] { 80, 80, 80, 80 };
        gbl_webserverPanel.rowHeights = new int[] { 30 };
        gbl_webserverPanel.columnWeights = new double[] { 1.0, 1.0, 1.0, 1.0 };
        gbl_webserverPanel.rowWeights = new double[] { 1.0 };
        setLayout(gbl_webserverPanel);

        final JLabel webserverPortLabel = new JLabel("HTTP Port");
        final GridBagConstraints gbc_webserverHttpPortLabel = new GridBagConstraints();
        gbc_webserverHttpPortLabel.anchor = GridBagConstraints.EAST;
        gbc_webserverHttpPortLabel.insets = new Insets(0, 0, 5, 5);
        gbc_webserverHttpPortLabel.gridx = 0;
        gbc_webserverHttpPortLabel.gridy = 0;
        add(webserverPortLabel, gbc_webserverHttpPortLabel);

        webserverHttpPortField = new JTextField("8080");
        final GridBagConstraints gbc_webserverHttpPortField = new GridBagConstraints();
        gbc_webserverHttpPortField.fill = GridBagConstraints.BOTH;
        gbc_webserverHttpPortField.insets = new Insets(0, 0, 5, 0);
        gbc_webserverHttpPortField.gridx = 1;
        gbc_webserverHttpPortField.gridy = 0;
        add(webserverHttpPortField, gbc_webserverHttpPortField);
        webserverHttpPortField.setColumns(10);

        final JLabel webserverHttpsPortLabel = new JLabel("HTTPS Port");
        final GridBagConstraints gbc_webserverHttpsPortLabel = new GridBagConstraints();
        gbc_webserverHttpsPortLabel.anchor = GridBagConstraints.EAST;
        gbc_webserverHttpsPortLabel.insets = new Insets(0, 0, 5, 5);
        gbc_webserverHttpsPortLabel.gridx = 2;
        gbc_webserverHttpsPortLabel.gridy = 0;
        add(webserverHttpsPortLabel, gbc_webserverHttpsPortLabel);

        webserverHttpsPortField = new JTextField("8443");
        final GridBagConstraints gbc_webserverHttpsPortField = new GridBagConstraints();
        gbc_webserverHttpsPortField.fill = GridBagConstraints.BOTH;
        gbc_webserverHttpsPortField.insets = new Insets(0, 0, 5, 0);
        gbc_webserverHttpsPortField.gridx = 3;
        gbc_webserverHttpsPortField.gridy = 0;
        add(webserverHttpsPortField, gbc_webserverHttpsPortField);
        webserverHttpsPortField.setColumns(10);

    }


    public int getHttpPort() {
        return Integer.valueOf(webserverHttpPortField.getText());
    }


    public int getHttpsPort() {
        return Integer.valueOf(webserverHttpsPortField.getText());
    }


    public boolean verify(final StringBuffer errors) {
        if (webserverHttpPortField.getText().isBlank()) {
            errors.append("Missing webserver port!\n");
            return false;
        } else if (!numberInputVerifier.verify(webserverHttpPortField.getText())) {
            errors.append("Non-numeric webserver port!\n");
            return false;
        }
        return true;
    }


    protected void generateConfiguration(final StringBuffer config) {
        config.append("#### Webserver properties ####\n");
        config.append("webserver.service.enabled=true\n");
        config.append("# The webserver port\n");
        config.append("webserver.http.port=" + getHttpPort() + "\n");
        config.append("webserver.https.port=" + getHttpsPort() + "\n");
    }


    void setConfiguration(final Properties config) {
        webserverHttpPortField.setText(config.getProperty("webserver.http.port"));
        webserverHttpsPortField.setText(config.getProperty("webserver.https.port"));
    }

}
