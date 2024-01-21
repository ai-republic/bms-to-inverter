package com.airepublic.bmstoinverter.configurator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class Configurator extends JFrame {
    private static final long serialVersionUID = 1L;
    private final Vector<MenuItem> inverterItems;
    private final Vector<MenuItem> serviceItems;
    private final Vector<MenuItem> platformItems;
    private final JTextField inverterPushInvervalField;
    private final JTextField inverterPortField;

    public Configurator() {
        super("BMS-to-Inverter Configurator");
        setSize(new Dimension(640, 554));
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        inverterItems = createInverterItems();
        serviceItems = createServiceItems();
        platformItems = createPlatformItems();

        getContentPane().setLayout(new BorderLayout(10, 10));

        final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
        getContentPane().add(tabbedPane, BorderLayout.NORTH);

        final JPanel bmsPanel = new JPanel();
        tabbedPane.addTab("BMS", bmsPanel);

        bmsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        final GridBagLayout gbl_bmsPanel = new GridBagLayout();
        gbl_bmsPanel.columnWidths = new int[] { 150, 250, 70, 70 };
        gbl_bmsPanel.rowHeights = new int[] { 100, 30, 30 };
        gbl_bmsPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0 };
        gbl_bmsPanel.rowWeights = new double[] { 0.0, 1.0, 0.0 };
        bmsPanel.setLayout(gbl_bmsPanel);

        final DefaultListModel<String> portListModel = new DefaultListModel<>();

        final JLabel bmsesLabel = new JLabel("BMS(s)");
        final GridBagConstraints gbc_bmsesLabel = new GridBagConstraints();
        gbc_bmsesLabel.anchor = GridBagConstraints.WEST;
        gbc_bmsesLabel.insets = new Insets(0, 0, 5, 5);
        gbc_bmsesLabel.gridx = 0;
        gbc_bmsesLabel.gridy = 0;
        bmsPanel.add(bmsesLabel, gbc_bmsesLabel);
        final JList<String> portList = new JList<>(portListModel);
        final GridBagConstraints gbc_portList = new GridBagConstraints();
        gbc_portList.insets = new Insets(0, 0, 5, 5);
        gbc_portList.fill = GridBagConstraints.BOTH;
        gbc_portList.gridx = 1;
        gbc_portList.gridy = 0;
        bmsPanel.add(portList, gbc_portList);

        final JButton addPortButton = new JButton("Add");
        addPortButton.addActionListener(e -> {
            final AddBMSDialog dlg = new AddBMSDialog(Configurator.this);
            dlg.setVisible(true);
            portListModel.addElement(dlg.getPortLocator());

        });
        final GridBagConstraints gbc_addPortButton = new GridBagConstraints();
        gbc_addPortButton.insets = new Insets(0, 0, 5, 5);
        gbc_addPortButton.gridx = 2;
        gbc_addPortButton.gridy = 0;
        bmsPanel.add(addPortButton, gbc_addPortButton);

        final JButton removePortButton = new JButton("Remove");
        removePortButton.addActionListener(e -> {
            portListModel.remove(portList.getSelectedIndex());
        });
        final GridBagConstraints gbc_removePortButton = new GridBagConstraints();
        gbc_removePortButton.insets = new Insets(0, 0, 5, 0);
        gbc_removePortButton.gridx = 3;
        gbc_removePortButton.gridy = 0;
        bmsPanel.add(removePortButton, gbc_removePortButton);

        final JLabel platformLabel = new JLabel("Platform");
        final GridBagConstraints gbc_platformLabel = new GridBagConstraints();
        gbc_platformLabel.anchor = GridBagConstraints.WEST;
        gbc_platformLabel.insets = new Insets(0, 0, 5, 5);
        gbc_platformLabel.gridx = 0;
        gbc_platformLabel.gridy = 1;
        bmsPanel.add(platformLabel, gbc_platformLabel);

        final JComboBox<MenuItem> platform = new JComboBox<>(platformItems);
        final GridBagConstraints gbc_platform = new GridBagConstraints();
        gbc_platform.insets = new Insets(0, 0, 5, 5);
        gbc_platform.fill = GridBagConstraints.HORIZONTAL;
        gbc_platform.gridx = 1;
        gbc_platform.gridy = 1;
        bmsPanel.add(platform, gbc_platform);

        final JLabel serviceLabel = new JLabel("Services");
        final GridBagConstraints gbc_serviceLabel = new GridBagConstraints();
        gbc_serviceLabel.anchor = GridBagConstraints.WEST;
        gbc_serviceLabel.insets = new Insets(0, 0, 5, 5);
        gbc_serviceLabel.gridx = 0;
        gbc_serviceLabel.gridy = 2;
        bmsPanel.add(serviceLabel, gbc_serviceLabel);

        final JComboBox<MenuItem> services = new JComboBox<>(serviceItems);
        final GridBagConstraints gbc_services = new GridBagConstraints();
        gbc_services.insets = new Insets(0, 0, 5, 5);
        gbc_services.fill = GridBagConstraints.HORIZONTAL;
        gbc_services.gridx = 1;
        gbc_services.gridy = 2;
        bmsPanel.add(services, gbc_services);

        final JPanel inverterPanel = new JPanel();
        inverterPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        tabbedPane.addTab("Inverter", null, inverterPanel, null);
        final GridBagLayout gbl_inverterPanel = new GridBagLayout();
        gbl_inverterPanel.columnWidths = new int[] { 150, 250, 70, 70 };
        gbl_inverterPanel.rowHeights = new int[] { 30, 30, 30, 30, 30 };
        gbl_inverterPanel.columnWeights = new double[] { 0.0, 1.0 };
        gbl_inverterPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        inverterPanel.setLayout(gbl_inverterPanel);

        final JLabel inverterLabel = new JLabel("Inverter");
        final GridBagConstraints gbc_inverterLabel = new GridBagConstraints();
        gbc_inverterLabel.anchor = GridBagConstraints.WEST;
        gbc_inverterLabel.insets = new Insets(0, 0, 5, 5);
        gbc_inverterLabel.gridx = 0;
        gbc_inverterLabel.gridy = 0;
        inverterPanel.add(inverterLabel, gbc_inverterLabel);

        final JComboBox<MenuItem> inverters = new JComboBox<>(inverterItems);
        final GridBagConstraints gbc_inverters = new GridBagConstraints();
        gbc_inverters.insets = new Insets(0, 0, 5, 0);
        gbc_inverters.fill = GridBagConstraints.HORIZONTAL;
        gbc_inverters.gridx = 1;
        gbc_inverters.gridy = 0;
        inverterPanel.add(inverters, gbc_inverters);

        final JLabel inverterPortLabel = new JLabel("Inverter port");
        final GridBagConstraints gbc_inverterPortLabel = new GridBagConstraints();
        gbc_inverterPortLabel.anchor = GridBagConstraints.WEST;
        gbc_inverterPortLabel.insets = new Insets(0, 0, 5, 5);
        gbc_inverterPortLabel.gridx = 0;
        gbc_inverterPortLabel.gridy = 1;
        inverterPanel.add(inverterPortLabel, gbc_inverterPortLabel);

        inverterPortField = new JTextField();
        final GridBagConstraints gbc_inverterPortField = new GridBagConstraints();
        gbc_inverterPortField.insets = new Insets(0, 0, 5, 0);
        gbc_inverterPortField.fill = GridBagConstraints.HORIZONTAL;
        gbc_inverterPortField.gridx = 1;
        gbc_inverterPortField.gridy = 1;
        inverterPanel.add(inverterPortField, gbc_inverterPortField);
        inverterPortField.setColumns(10);

        final JLabel inverterPushInvervalLabel = new JLabel("Inverter push interval");
        final GridBagConstraints gbc_inverterPushInvervalLabel = new GridBagConstraints();
        gbc_inverterPushInvervalLabel.insets = new Insets(0, 0, 5, 5);
        gbc_inverterPushInvervalLabel.anchor = GridBagConstraints.WEST;
        gbc_inverterPushInvervalLabel.gridx = 0;
        gbc_inverterPushInvervalLabel.gridy = 2;
        inverterPanel.add(inverterPushInvervalLabel, gbc_inverterPushInvervalLabel);

        inverterPushInvervalField = new JTextField();
        inverterPushInvervalField.setToolTipText("Time in seconds to send data to the inverter");
        inverterPushInvervalField.setColumns(10);
        final GridBagConstraints gbc_inverterPushInvervalField = new GridBagConstraints();
        gbc_inverterPushInvervalField.insets = new Insets(0, 0, 5, 0);
        gbc_inverterPushInvervalField.fill = GridBagConstraints.HORIZONTAL;
        gbc_inverterPushInvervalField.gridx = 1;
        gbc_inverterPushInvervalField.gridy = 2;
        inverterPanel.add(inverterPushInvervalField, gbc_inverterPushInvervalField);

        final JPanel buttonPanel = new JPanel();
        final GridBagLayout gbl_buttonPanel = new GridBagLayout();
        gbl_buttonPanel.columnWidths = new int[] { 0, 120, 120 };
        gbl_buttonPanel.rowHeights = new int[] { 0, 50 };
        gbl_buttonPanel.columnWeights = new double[] { 0.0, 0.0, 0.0 };
        gbl_buttonPanel.rowWeights = new double[] { 0.0, 0.0 };
        buttonPanel.setLayout(gbl_buttonPanel);

        final JButton createButton = new JButton("Create");
        createButton.addActionListener(event -> generateConfiguration());
        final GridBagConstraints gbc_createButton = new GridBagConstraints();
        gbc_createButton.anchor = GridBagConstraints.EAST;
        gbc_createButton.insets = new Insets(0, 0, 0, 5);
        gbc_createButton.gridx = 1;
        gbc_createButton.gridy = 1;
        buttonPanel.add(createButton, gbc_createButton);

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());
        final GridBagConstraints gbc_cancelButton = new GridBagConstraints();
        gbc_cancelButton.anchor = GridBagConstraints.WEST;
        gbc_cancelButton.gridx = 2;
        gbc_cancelButton.gridy = 1;
        buttonPanel.add(cancelButton, gbc_cancelButton);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }


    private void generateConfiguration() {

    }


    private Vector<MenuItem> createPlatformItems() {
        final Vector<MenuItem> items = new Vector<>();

        items.add(new MenuItem("AARCH64", ""));
        items.add(new MenuItem("ARM v5", ""));
        items.add(new MenuItem("ARM v6", ""));
        items.add(new MenuItem("ARM v7", ""));
        items.add(new MenuItem("ARM v7a", ""));
        items.add(new MenuItem("ARM v7l", ""));
        items.add(new MenuItem("RISCV v32", ""));
        items.add(new MenuItem("RISCV v64", ""));
        items.add(new MenuItem("X86 32bit (UNIX)", ""));
        items.add(new MenuItem("X86 64bit (UNIX)", ""));

        return items;
    }


    Vector<MenuItem> createInverterItems() {
        final Vector<MenuItem> items = new Vector<>();

        items.add(new MenuItem("NONE", ""));
        items.add(new MenuItem("DEYE (CAN)", ""));
        items.add(new MenuItem("GROWATT (CAN)", ""));
        items.add(new MenuItem("PYLONTECH (CAN)", ""));
        items.add(new MenuItem("SMA SI (CAN)", ""));
        items.add(new MenuItem("SOLARK (CAN)", ""));

        return items;
    }


    Vector<MenuItem> createServiceItems() {
        final Vector<MenuItem> items = new Vector<>();

        items.add(new MenuItem("NONE", ""));
        items.add(new MenuItem("ALL", ""));
        items.add(new MenuItem("EMAIL NOTIFICATIONS", ""));
        items.add(new MenuItem("MQTT", ""));

        return items;
    }


    public static void main(final String[] args) {
        new Configurator();
    }

}
