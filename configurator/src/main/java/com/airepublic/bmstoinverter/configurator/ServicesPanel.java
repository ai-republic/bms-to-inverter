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
import java.util.stream.Stream;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class ServicesPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JCheckBox emailCheckBox;
    private final EmailServicePanel emailPanel;
    private final MQTTServicePanel mqttPanel;
    private final JCheckBox webserverCheckBox;
    private final WebserverServicePanel webserverPanel;

    public ServicesPanel(final Configurator configurator) {
        final GridBagLayout gbl_servicesPanel = new GridBagLayout();
        gbl_servicesPanel.columnWidths = new int[] { 100, 100, 100, 100, 0 };
        gbl_servicesPanel.rowHeights = new int[] { 30, 30, 30, 0, 30, 0, 30 };
        gbl_servicesPanel.columnWeights = new double[] { 1.0, 4.9E-324, 0.0, 0.0, Double.MIN_VALUE };
        gbl_servicesPanel.rowWeights = new double[] { 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
        setLayout(gbl_servicesPanel);

        emailCheckBox = new JCheckBox("Email notification");
        emailCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
        final GridBagConstraints gbc_emailCheckBox = new GridBagConstraints();
        gbc_emailCheckBox.anchor = GridBagConstraints.WEST;
        gbc_emailCheckBox.insets = new Insets(0, 0, 5, 5);
        gbc_emailCheckBox.gridx = 0;
        gbc_emailCheckBox.gridy = 0;
        add(emailCheckBox, gbc_emailCheckBox);

        emailPanel = new EmailServicePanel();
        final GridBagLayout gridBagLayout = (GridBagLayout) emailPanel.getLayout();
        gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0 };
        gridBagLayout.rowHeights = new int[] { 30, 30, 30, 30, 30 };
        gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, 1.0 };
        gridBagLayout.columnWidths = new int[] { 80, 150, 70, 60, 70, 30 };
        emailPanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        final GridBagConstraints gbc_emailPanel = new GridBagConstraints();
        gbc_emailPanel.fill = GridBagConstraints.BOTH;
        gbc_emailPanel.gridwidth = 4;
        gbc_emailPanel.insets = new Insets(0, 0, 5, 0);
        gbc_emailPanel.gridx = 0;
        gbc_emailPanel.gridy = 1;
        add(emailPanel, gbc_emailPanel);

        enableComponent(emailPanel, false);
        emailCheckBox.addActionListener(t -> {
            enableComponent(emailPanel, emailCheckBox.isSelected());
            configurator.disableUpdateConfiguration();
        });

        mqttPanel = new MQTTServicePanel(configurator);
        final GridBagConstraints gbc_mqttPanel = new GridBagConstraints();
        gbc_mqttPanel.fill = GridBagConstraints.BOTH;
        gbc_mqttPanel.gridwidth = 4;
        gbc_mqttPanel.insets = new Insets(0, 0, 5, 0);
        gbc_mqttPanel.gridx = 0;
        gbc_mqttPanel.gridy = 2;
        add(mqttPanel, gbc_mqttPanel);

        webserverCheckBox = new JCheckBox("Webserver");
        webserverCheckBox.setToolTipText("Enabling the webserver");
        final GridBagConstraints gbc_webserverCheckBox = new GridBagConstraints();
        gbc_webserverCheckBox.anchor = GridBagConstraints.WEST;
        gbc_webserverCheckBox.insets = new Insets(0, 0, 5, 5);
        gbc_webserverCheckBox.gridx = 0;
        gbc_webserverCheckBox.gridy = 3;
        add(webserverCheckBox, gbc_webserverCheckBox);

        webserverPanel = new WebserverServicePanel();
        final GridBagConstraints gbc_webserverPanel = new GridBagConstraints();
        gbc_webserverPanel.anchor = GridBagConstraints.WEST;
        gbc_webserverPanel.fill = GridBagConstraints.VERTICAL;
        gbc_webserverPanel.gridwidth = 4;
        gbc_webserverPanel.gridx = 0;
        gbc_webserverPanel.gridy = 4;
        add(webserverPanel, gbc_webserverPanel);

        enableComponent(webserverPanel, false);
        webserverCheckBox.addActionListener(t -> {
            enableComponent(webserverPanel, webserverCheckBox.isSelected());
            configurator.disableUpdateConfiguration();
        });

    }


    private void enableComponent(final JPanel panel, final boolean enable) {
        if (enable) {
            Stream.of(panel.getComponents()).forEach(c -> c.setEnabled(true));
        } else {
            Stream.of(panel.getComponents()).forEach(c -> c.setEnabled(false));
        }
    }


    public boolean verify(final StringBuffer errors) {
        boolean fail = false;
        if (emailCheckBox.isSelected() && !emailPanel.verify(errors)) {
            fail = true;
        }

        if (!mqttPanel.verify(errors)) {
            fail = true;
        }

        if (webserverCheckBox.isSelected() && !webserverPanel.verify(errors)) {
            fail = true;
        }

        return !fail;
    }


    protected void generateConfiguration(final StringBuffer config) {
        config.append("###################################################################\n"
                + "###                 Optional services settings                  ###\n"
                + "###################################################################\n"
                + "\n");
        config.append("\n");

        mqttPanel.generateConfiguration(config);
        config.append("\n");

        if (emailCheckBox.isSelected()) {
            emailPanel.generateConfiguration(config);
            config.append("\n");
        }

        if (webserverCheckBox.isSelected()) {
            webserverPanel.generateConfiguration(config);
        }
    }


    public boolean isEmailEnabled() {
        return emailCheckBox.isSelected();
    }


    public boolean isWebserverEnabled() {
        return webserverCheckBox.isSelected();
    }


    public boolean isMQTTProducerEnabled() {
        return mqttPanel.isMQTTProducerEnabled();
    }


    public boolean isMQTTBrokerEnabled() {
        return mqttPanel.isMQTTBrokerEnabled();
    }


    void setConfiguration(final Properties config) {
        if (config.containsKey("mail.service.enabled")) {
            emailCheckBox.setSelected(true);
            emailPanel.setConfiguration(config);
            enableComponent(emailPanel, true);
        } else {
            enableComponent(emailPanel, false);
        }

        mqttPanel.setConfiguration(config);

        if (config.containsKey("webserver.service.enabled")) {
            webserverCheckBox.setSelected(true);
            webserverPanel.setConfiguration(config);
            enableComponent(webserverPanel, true);
        } else {
            enableComponent(webserverPanel, false);
        }
    }
}
