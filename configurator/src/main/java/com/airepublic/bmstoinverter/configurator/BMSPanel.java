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
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;

import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;

public class BMSPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JList<MenuItem<BMSConfig>> bmsList;
    private final DefaultListModel<MenuItem<BMSConfig>> bmsListModel = new DefaultListModel<>();
    private final JTextField pollIntervalField;
    private final JButton editBMSButton;
    private final NumberInputVerifier numberInputVerifier = new NumberInputVerifier();

    public BMSPanel(final JFrame frame) {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        final GridBagLayout gbl_bmsPanel = new GridBagLayout();
        gbl_bmsPanel.columnWidths = new int[] { 100, 300, 70, 70 };
        gbl_bmsPanel.rowHeights = new int[] { 30, 250, 0 };
        gbl_bmsPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0 };
        gbl_bmsPanel.rowWeights = new double[] { 0.0, 1.0, 0.0 };
        setLayout(gbl_bmsPanel);

        final JLabel bmsesLabel = new JLabel("BMS(s)");
        final GridBagConstraints gbc_bmsesLabel = new GridBagConstraints();
        gbc_bmsesLabel.anchor = GridBagConstraints.EAST;
        gbc_bmsesLabel.insets = new Insets(0, 0, 5, 5);
        gbc_bmsesLabel.gridx = 0;
        gbc_bmsesLabel.gridy = 0;
        add(bmsesLabel, gbc_bmsesLabel);

        final JScrollPane scrollPane = new JScrollPane();
        final GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.gridheight = 2;
        gbc_scrollPane.insets = new Insets(0, 0, 5, 5);
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.gridx = 1;
        gbc_scrollPane.gridy = 0;
        add(scrollPane, gbc_scrollPane);

        bmsList = new JList<>(bmsListModel);
        bmsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bmsList.setVisibleRowCount(8);
        bmsList.addMouseListener(new MouseInputAdapter() {

            @Override
            public void mouseClicked(final MouseEvent evt) {
                final JList<MenuItem<BMSConfig>> list = (JList<MenuItem<BMSConfig>>) evt.getSource();
                if (evt.getClickCount() == 2) {
                    Stream.of(editBMSButton.getActionListeners()).forEach(listener -> listener.actionPerformed(new ActionEvent(editBMSButton, ActionEvent.ACTION_PERFORMED, "edit")));
                }
            }
        });
        scrollPane.setViewportView(bmsList);

        final JButton addBMSButton = new JButton("Add");
        addBMSButton.addActionListener(e -> {
            final BMSDialog dlg = new BMSDialog(frame);
            dlg.setVisible(true);
            final BMSConfig config = dlg.getBMSConfig();

            if (config != null) {
                config.setBmsId(bmsListModel.getSize());
                bmsListModel.addElement(new MenuItem<>(createBMSDisplayName(config), config));
            }
        });
        final GridBagConstraints gbc_addBMSButton = new GridBagConstraints();
        gbc_addBMSButton.insets = new Insets(0, 0, 5, 5);
        gbc_addBMSButton.gridx = 2;
        gbc_addBMSButton.gridy = 0;
        add(addBMSButton, gbc_addBMSButton);

        final JButton removeBMSButton = new JButton("Remove");
        removeBMSButton.addActionListener(e -> {
            if (bmsList.getSelectedIndex() != -1) {
                bmsListModel.remove(bmsList.getSelectedIndex());
            }
        });
        final GridBagConstraints gbc_removeBMSButton = new GridBagConstraints();
        gbc_removeBMSButton.insets = new Insets(0, 0, 5, 0);
        gbc_removeBMSButton.gridx = 3;
        gbc_removeBMSButton.gridy = 0;
        add(removeBMSButton, gbc_removeBMSButton);

        editBMSButton = new JButton("Edit");
        final GridBagConstraints gbc_editBMSButton = new GridBagConstraints();
        gbc_editBMSButton.anchor = GridBagConstraints.NORTH;
        gbc_editBMSButton.insets = new Insets(0, 0, 5, 5);
        gbc_editBMSButton.gridx = 2;
        gbc_editBMSButton.gridy = 1;
        add(editBMSButton, gbc_editBMSButton);
        editBMSButton.addActionListener(e -> {
            final MenuItem<BMSConfig> item = bmsList.getSelectedValue();

            if (item != null) {
                final BMSDialog dlg = new BMSDialog(frame);
                dlg.setBMSConfig(item.getValue());
                dlg.setVisible(true);
                item.setDisplayName(createBMSDisplayName(item.getValue()));
                bmsList.repaint();
            }
        });

        final JButton duplicateBMSButton = new JButton("Duplicate");
        final GridBagConstraints gbc_duplicateBMSButton = new GridBagConstraints();
        gbc_duplicateBMSButton.anchor = GridBagConstraints.NORTH;
        gbc_duplicateBMSButton.insets = new Insets(0, 0, 5, 0);
        gbc_duplicateBMSButton.gridx = 3;
        gbc_duplicateBMSButton.gridy = 1;
        add(duplicateBMSButton, gbc_duplicateBMSButton);

        final JLabel pollIntervalLabel = new JLabel("Poll interval");
        final GridBagConstraints gbc_pollIntervalLabel = new GridBagConstraints();
        gbc_pollIntervalLabel.insets = new Insets(0, 0, 0, 5);
        gbc_pollIntervalLabel.anchor = GridBagConstraints.EAST;
        gbc_pollIntervalLabel.gridx = 0;
        gbc_pollIntervalLabel.gridy = 2;
        add(pollIntervalLabel, gbc_pollIntervalLabel);

        pollIntervalField = new JTextField();
        pollIntervalField.setText("1");
        final GridBagConstraints gbc_pollIntervalField = new GridBagConstraints();
        gbc_pollIntervalField.insets = new Insets(0, 0, 0, 5);
        gbc_pollIntervalField.fill = GridBagConstraints.HORIZONTAL;
        gbc_pollIntervalField.gridx = 1;
        gbc_pollIntervalField.gridy = 2;
        add(pollIntervalField, gbc_pollIntervalField);
        pollIntervalField.setColumns(10);
        duplicateBMSButton.addActionListener(e -> {
            final MenuItem<BMSConfig> item = bmsList.getSelectedValue();

            if (item != null) {
                final BMSConfig config = item.getValue();
                final BMSConfig duplicate = new BMSConfig(config.getBmsId() + 1, config.getPortLocator(), config.getBaudRate(), config.getDelayAfterNoBytes(), config.getDescriptor());

                if (item != null) {
                    bmsListModel.addElement(new MenuItem<>(createBMSDisplayName(duplicate), duplicate));
                }
            }
        });
    }


    private String createBMSDisplayName(final BMSConfig config) {
        return config.getDescriptor().getName() + "(ID: " + config.getBmsId() + ") on " + config.getPortLocator();
    }


    public List<BMSConfig> getBMSConfigList() {
        final List<BMSConfig> bmsConfigList = new ArrayList<>();

        for (int i = 0; i < bmsListModel.getSize(); i++) {
            final BMSConfig bmsConfig = bmsListModel.get(i).getValue();
            bmsConfigList.add(bmsConfig);
        }

        return bmsConfigList;
    }


    public boolean verify(final StringBuffer errors) {
        if (bmsListModel.getSize() == 0) {
            errors.append("Missing BMS\n");
            return false;
        }

        if (pollIntervalField.getText() == null || pollIntervalField.getText().isBlank()) {
            errors.append("Missing BMS poll interval!\r\n");
            return false;
        } else if (!numberInputVerifier.verify(pollIntervalField.getText())) {
            errors.append("Non-numeric BMS poll interval!\r\n");
            return false;
        }
        return true;
    }


    protected void generateConfiguration(final StringBuffer config) {
        config.append("###################################################################\n"
                + "###                       BMS settings                          ###\n"
                + "###################################################################\n"
                + "\n"
                + "####  Simple single port configuration ####\n"
                + "# bms.pollIntervall - is the interval to request BMS data (in seconds)\n"
                + "# bms.x.type - can be (DALY_CAN, DALY_RS485, JK_CAN, PYLON_CAN or SEPLOS_CAN \n"
                + "# bms.x.portLocator - is the locator/device to use to communicate to the BMS, eg. can0, /dev/ttyUSB0, com3, etc.  \n"
                + "# bms.x.baudRate - is the locator/device baudrate to use to communicate to the BMS, eg. 9600, 500000, etc.  \n"
                + "# bms.x.delayAfterNoBytes - is the delay after receiving no data (in ms)\n");
        config.append("bms.pollInterval=" + pollIntervalField.getText() + "\n\n");

        for (int index = 1; index <= getBMSConfigList().size(); index++) {
            final BMSConfig bmsConfig = getBMSConfigList().get(index - 1);
            config.append("bms." + index + ".type=" + bmsConfig.getDescriptor().getName() + "\n");
            config.append("bms." + index + ".id=" + bmsConfig.getBmsId() + "\n");
            config.append("bms." + index + ".portLocator=" + bmsConfig.getPortLocator() + "\n");
            config.append("bms." + index + ".baudRate=" + bmsConfig.getBaudRate() + "\n");
            config.append("bms." + index + ".delayAfterNoBytes=" + bmsConfig.getDelayAfterNoBytes() + "\n");
            config.append("\n");
        }
    }


    public void setConfiguration(final Properties config) {
        int index = 1;
        String bmsType;
        final Map<String, BMSDescriptor> descriptors = new HashMap<>();
        ServiceLoader.load(BMSDescriptor.class).forEach(descriptor -> descriptors.put(descriptor.getName(), descriptor));
        bmsListModel.clear();

        while ((bmsType = config.getProperty("bms." + index + ".type")) != null) {
            final String portLocator = config.getProperty("bms." + index + ".portLocator");
            final int bmsId = Integer.parseInt(config.getProperty("bms." + index + ".id", "" + index));
            final int baudRate = Integer.parseInt(config.getProperty("bms." + index + ".baudRate", "" + index));
            final long delayAfterNoBytes = Long.parseLong(config.getProperty("bms." + index + ".delayAfterNoBytes"));
            final BMSConfig bmsConfig = new BMSConfig(bmsId, portLocator, baudRate, delayAfterNoBytes, descriptors.get(bmsType));
            bmsListModel.add(index - 1, new MenuItem<>(createBMSDisplayName(bmsConfig), bmsConfig));

            index++;
        }
    }
}
