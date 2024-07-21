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
import java.util.ServiceLoader;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.airepublic.bmstoinverter.core.InverterDescriptor;

public class InverterPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Vector<MenuItem<InverterDescriptor>> inverterItems = createInverterItems();
    private final JComboBox<MenuItem<InverterDescriptor>> inverterField;
    private final JTextField inverterPortLocatorField;
    private final JTextField inverterBaudRateField;
    private final JTextField inverterSendIntervalField;
    private final NumberInputVerifier numberInputVerifier = new NumberInputVerifier();

    public InverterPanel() {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        final GridBagLayout gbl_inverterPanel = new GridBagLayout();
        gbl_inverterPanel.columnWidths = new int[] { 100, 250, 70, 70 };
        gbl_inverterPanel.rowHeights = new int[] { 30, 30, 30, 30, 30, 30 };
        gbl_inverterPanel.columnWeights = new double[] { 0.0, 1.0 };
        gbl_inverterPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        setLayout(gbl_inverterPanel);

        final JLabel inverterLabel = new JLabel("Inverter");
        final GridBagConstraints gbc_inverterLabel = new GridBagConstraints();
        gbc_inverterLabel.anchor = GridBagConstraints.EAST;
        gbc_inverterLabel.insets = new Insets(0, 0, 5, 5);
        gbc_inverterLabel.gridx = 0;
        gbc_inverterLabel.gridy = 0;
        add(inverterLabel, gbc_inverterLabel);

        inverterField = new JComboBox<>(inverterItems);
        final GridBagConstraints gbc_inverters = new GridBagConstraints();
        gbc_inverters.insets = new Insets(0, 0, 5, 0);
        gbc_inverters.fill = GridBagConstraints.BOTH;
        gbc_inverters.gridx = 1;
        gbc_inverters.gridy = 0;
        add(inverterField, gbc_inverters);

        final JLabel inverterPortLocatorLabel = new JLabel("Port");
        final GridBagConstraints gbc_inverterPortLocatorLabel = new GridBagConstraints();
        gbc_inverterPortLocatorLabel.anchor = GridBagConstraints.EAST;
        gbc_inverterPortLocatorLabel.insets = new Insets(0, 0, 5, 5);
        gbc_inverterPortLocatorLabel.gridx = 0;
        gbc_inverterPortLocatorLabel.gridy = 1;
        add(inverterPortLocatorLabel, gbc_inverterPortLocatorLabel);

        inverterPortLocatorField = new JTextField();
        final GridBagConstraints gbc_inverterPortField = new GridBagConstraints();
        gbc_inverterPortField.insets = new Insets(0, 0, 5, 0);
        gbc_inverterPortField.fill = GridBagConstraints.BOTH;
        gbc_inverterPortField.gridx = 1;
        gbc_inverterPortField.gridy = 1;
        add(inverterPortLocatorField, gbc_inverterPortField);
        inverterPortLocatorField.setColumns(10);

        final JLabel inverterBaudRateLabel = new JLabel("Baud rate");
        final GridBagConstraints gbc_inverterBaudRateLabel = new GridBagConstraints();
        gbc_inverterBaudRateLabel.anchor = GridBagConstraints.EAST;
        gbc_inverterBaudRateLabel.insets = new Insets(0, 0, 5, 5);
        gbc_inverterBaudRateLabel.gridx = 0;
        gbc_inverterBaudRateLabel.gridy = 2;
        add(inverterBaudRateLabel, gbc_inverterBaudRateLabel);

        inverterBaudRateField = new JTextField();
        final GridBagConstraints gbc_inverterBaudRateField = new GridBagConstraints();
        gbc_inverterBaudRateField.insets = new Insets(0, 0, 5, 0);
        gbc_inverterBaudRateField.fill = GridBagConstraints.BOTH;
        gbc_inverterBaudRateField.gridx = 1;
        gbc_inverterBaudRateField.gridy = 2;
        add(inverterBaudRateField, gbc_inverterBaudRateField);
        inverterBaudRateField.setColumns(10);

        final JLabel inverterSendIntervalLabel = new JLabel("Send interval");
        final GridBagConstraints gbc_inverterSendIntervalLabel = new GridBagConstraints();
        gbc_inverterSendIntervalLabel.insets = new Insets(0, 0, 5, 5);
        gbc_inverterSendIntervalLabel.anchor = GridBagConstraints.EAST;
        gbc_inverterSendIntervalLabel.gridx = 0;
        gbc_inverterSendIntervalLabel.gridy = 3;
        add(inverterSendIntervalLabel, gbc_inverterSendIntervalLabel);

        inverterSendIntervalField = new JTextField("1");
        inverterSendIntervalField.setToolTipText("Time in seconds to send data to the inverter");
        inverterSendIntervalField.setColumns(10);
        inverterSendIntervalField.setInputVerifier(numberInputVerifier);
        final GridBagConstraints gbc_inverterPushInvervalField = new GridBagConstraints();
        gbc_inverterPushInvervalField.insets = new Insets(0, 0, 5, 0);
        gbc_inverterPushInvervalField.fill = GridBagConstraints.BOTH;
        gbc_inverterPushInvervalField.gridx = 1;
        gbc_inverterPushInvervalField.gridy = 3;
        add(inverterSendIntervalField, gbc_inverterPushInvervalField);

        inverterField.addActionListener(e -> inverterPortLocatorField.requestFocus());
        inverterPortLocatorField.addActionListener(e -> inverterBaudRateField.requestFocus());
        inverterBaudRateField.addActionListener(e -> inverterSendIntervalField.requestFocus());
    }


    Vector<MenuItem<InverterDescriptor>> createInverterItems() {
        final Vector<MenuItem<InverterDescriptor>> items = new Vector<>();
        ServiceLoader.load(InverterDescriptor.class).forEach(descriptor -> items.add(new MenuItem<>(descriptor.getName(), descriptor)));

        return items;
    }


    public InverterDescriptor getInverterType() {
        return inverterField.getModel().getElementAt(inverterField.getSelectedIndex()).getValue();
    }


    public String getPortLocator() {
        return inverterPortLocatorField.getText();
    }


    public int getBaudRate() {
        return Integer.valueOf(inverterBaudRateField.getText());
    }


    public int getSendInterval() {
        return Integer.valueOf(inverterSendIntervalField.getText());
    }


    public boolean verify(final StringBuffer errors) {
        boolean fail = false;

        if (inverterField.getSelectedIndex() < 0) {
            errors.append("Missing inverter type\n");
            fail = true;
        }

        if (inverterPortLocatorField.getText().isBlank()) {
            errors.append("Missing inverter port locator!\n");
            fail = true;
        }

        if (inverterBaudRateField.getText().isBlank()) {
            errors.append("Missing inverter send interval!\n");
            fail = true;
        } else if (!numberInputVerifier.verify(inverterBaudRateField)) {
            errors.append("Non-numeric inverter baud rate!\n");
            fail = true;
        }

        if (inverterSendIntervalField.getText().isBlank()) {
            errors.append("Missing inverter send interval!\n");
            fail = true;
        } else if (!numberInputVerifier.verify(inverterSendIntervalField)) {
            errors.append("Non-numeric inverter send interval!\n");
            fail = true;
        }

        return !fail;
    }


    protected void generateConfiguration(final StringBuffer config) {
        config.append("###################################################################\n"
                + "###                    Inverter settings                        ###\n"
                + "###################################################################\n"
                + "# The inverter type can be NONE, DEYE_CAN, GROWATT_CAN, SMA_SI_CAN, SOLARK_CAN\n");
        config.append("inverter.type=" + getInverterType().getName() + "\n");
        config.append("# The port name/device to use to communicate to the  inverter  \n");
        config.append("inverter.portLocator=" + getPortLocator() + "\n");
        config.append("# The port baud rate to use to communicate to the  inverter  \n");
        config.append("inverter.baudRate=" + getBaudRate() + "\n");
        config.append("# Interval to send data to the inverter (in seconds)\n");
        config.append("inverter.sendInterval=" + getSendInterval() + "\n");
        config.append("\n");

    }


    void setConfiguration(final Properties config) {
        final String inverterType = config.getProperty("inverter.type");
        final String portLocator = config.getProperty("inverter.portLocator");
        final int baudRate = Integer.parseInt(config.getProperty("inverter.baudRate"));
        final int sendInterval = Integer.parseInt(config.getProperty("inverter.sendInterval"));

        for (int i = 0; i < inverterItems.size(); i++) {
            if (inverterItems.get(i).getDisplayName().equals(inverterType)) {
                inverterField.setSelectedIndex(i);
                break;
            }
        }

        inverterPortLocatorField.setText(portLocator);
        inverterBaudRateField.setText("" + baudRate);
        inverterSendIntervalField.setText("" + sendInterval);
    }
}
