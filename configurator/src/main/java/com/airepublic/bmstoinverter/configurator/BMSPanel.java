package com.airepublic.bmstoinverter.configurator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;

public class BMSPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JList<MenuItem<BMSConfig>> bmsList;
    private final DefaultListModel<MenuItem<BMSConfig>> bmsListModel = new DefaultListModel<>();

    public BMSPanel(final JFrame frame) {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        final GridBagLayout gbl_bmsPanel = new GridBagLayout();
        gbl_bmsPanel.columnWidths = new int[] { 100, 300, 70, 70 };
        gbl_bmsPanel.rowHeights = new int[] { 30, 250 };
        gbl_bmsPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0 };
        gbl_bmsPanel.rowWeights = new double[] { 0.0, 1.0 };
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
        scrollPane.setViewportView(bmsList);

        final JButton addBMSButton = new JButton("Add");
        addBMSButton.addActionListener(e -> {
            final BMSDialog dlg = new BMSDialog(frame);
            dlg.setVisible(true);
            final BMSConfig config = dlg.getBMSConfig();

            if (config != null) {
                config.setBmsNo(bmsListModel.getSize());
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
                reindexBMSList();
            }
        });
        final GridBagConstraints gbc_removeBMSButton = new GridBagConstraints();
        gbc_removeBMSButton.insets = new Insets(0, 0, 5, 0);
        gbc_removeBMSButton.gridx = 3;
        gbc_removeBMSButton.gridy = 0;
        add(removeBMSButton, gbc_removeBMSButton);

        final JButton editBMSButton = new JButton("Edit");
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
        duplicateBMSButton.addActionListener(e -> {
            final MenuItem<BMSConfig> item = bmsList.getSelectedValue();

            if (item != null) {
                final BMSConfig config = item.getValue();
                final BMSConfig duplicate = new BMSConfig(bmsListModel.getSize(), config.getPortLocator(), config.getPollInterval(), config.getDelayAfterNoBytes(), config.getDescriptor());

                if (item != null) {
                    bmsListModel.addElement(new MenuItem<>(createBMSDisplayName(duplicate), duplicate));
                }
            }
        });
    }


    private String createBMSDisplayName(final BMSConfig config) {
        return "#" + (config.getBmsNo() + 1) + ": " + config.getDescriptor().getName() + " on " + config.getPortLocator();
    }


    private void reindexBMSList() {
        for (int i = 0; i < bmsListModel.getSize(); i++) {
            final MenuItem<BMSConfig> item = bmsListModel.get(i);
            item.getValue().setBmsNo(i);
            item.setDisplayName(createBMSDisplayName(item.getValue()));
        }

        bmsList.repaint();
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

        return true;
    }


    protected void generateConfiguration(final StringBuffer config) {
        config.append("###################################################################\n"
                + "###                       BMS settings                          ###\n"
                + "###################################################################\n"
                + "\n"
                + "####  Simple single port configuration ####\n"
                + "# bms.x.type - can be (DALY_CAN, DALY_RS485, JK_CAN, PYLON_CAN or SEPLOS_CAN \n"
                + "# bms.x.portLocator - is the locator/device to use to communicate to the BMS, eg. can0, /dev/ttyUSB0, com3, etc.  \n"
                + "# bms.x.pollIntervall - is the interval to request BMS data (in seconds)\n"
                + "# bms.x.delayAfterNoBytes - is the delay after receiving no data (in ms)\n");
        for (final BMSConfig bmsConfig : getBMSConfigList()) {
            config.append("bms." + bmsConfig.getBmsNo() + ".type=" + bmsConfig.getDescriptor().getName() + "\n");
            config.append("bms." + bmsConfig.getBmsNo() + ".portLocator=" + bmsConfig.getPortLocator() + "\n");
            config.append("bms." + bmsConfig.getBmsNo() + ".pollInterval=" + bmsConfig.getPollInterval() + "\n");
            config.append("bms." + bmsConfig.getBmsNo() + ".delayAfterNoBytes=" + bmsConfig.getDelayAfterNoBytes() + "\n");
            config.append("\n");
        }
    }


    public void setConfiguration(final Properties config) {

        int bmsNo = 0;
        String bmsType;
        final Map<String, BMSDescriptor> descriptors = new HashMap<>();
        ServiceLoader.load(BMSDescriptor.class).forEach(descriptor -> descriptors.put(descriptor.getName(), descriptor));
        bmsListModel.clear();

        while ((bmsType = config.getProperty("bms." + bmsNo + ".type")) != null) {
            final String portLocator = config.getProperty("bms." + bmsNo + ".portLocator");
            final int pollInterval = Integer.parseInt(config.getProperty("bms." + bmsNo + ".pollInterval"));
            final long delayAfterNoBytes = Long.parseLong(config.getProperty("bms." + bmsNo + ".delayAfterNoBytes"));
            final BMSConfig bmsConfig = new BMSConfig(bmsNo, portLocator, pollInterval, delayAfterNoBytes, descriptors.get(bmsType));
            bmsListModel.add(bmsNo, new MenuItem<>(createBMSDisplayName(bmsConfig), bmsConfig));

            bmsNo++;
        }
    }
}
