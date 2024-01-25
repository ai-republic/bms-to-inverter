package com.airepublic.bmstoinverter.configurator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * Dialog to enter a port locator.
 */
public class AddBMSDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final Vector<MenuItem> bmsItems;
    private String portLocator = "";

    /**
     * Constructor.
     *
     * @param owner the owning frame
     */
    public AddBMSDialog(final JFrame owner) {
        super(owner, "Add port...", true);

        bmsItems = createBMSItems();

        setLocation(owner.getBounds().width / 2 - 175, owner.getBounds().height / 2 - 60);
        setSize(new Dimension(350, 359));
        setResizable(false);
        getContentPane().setLayout(new BorderLayout(0, 0));

        final JPanel bmsPanel = new JPanel();
        bmsPanel.setAlignmentY(0.75f);
        bmsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        getContentPane().add(bmsPanel, BorderLayout.NORTH);
        final GridBagLayout gbl_bmsPanel = new GridBagLayout();
        gbl_bmsPanel.columnWeights = new double[] { 0.0, 1.0 };
        gbl_bmsPanel.columnWidths = new int[] { 100, 150 };
        gbl_bmsPanel.rowHeights = new int[] { 30, 30, 30, 0, 30 };
        bmsPanel.setLayout(gbl_bmsPanel);

        final JLabel bmsLabel = new JLabel("BMS");
        final GridBagConstraints gbc_bmsLabel = new GridBagConstraints();
        gbc_bmsLabel.anchor = GridBagConstraints.WEST;
        gbc_bmsLabel.insets = new Insets(0, 0, 5, 5);
        gbc_bmsLabel.gridx = 0;
        gbc_bmsLabel.gridy = 0;
        bmsPanel.add(bmsLabel, gbc_bmsLabel);

        // BMS dropdown
        final JComboBox<MenuItem> bmses = new JComboBox<>(bmsItems);
        final GridBagConstraints gbc_bmses = new GridBagConstraints();
        gbc_bmses.insets = new Insets(0, 0, 5, 0);
        gbc_bmses.fill = GridBagConstraints.HORIZONTAL;
        gbc_bmses.gridx = 1;
        gbc_bmses.gridy = 0;
        bmsPanel.add(bmses, gbc_bmses);

        final JLabel portLocatorLabel = new JLabel("Port locator");
        portLocatorLabel.setToolTipText("The qualified name of the port, e.g. /dev/ttyS0 or can0 or com3");
        final GridBagConstraints gbc_portLocatorLabel = new GridBagConstraints();
        gbc_portLocatorLabel.insets = new Insets(0, 0, 5, 5);
        gbc_portLocatorLabel.anchor = GridBagConstraints.WEST;
        gbc_portLocatorLabel.gridx = 0;
        gbc_portLocatorLabel.gridy = 1;
        bmsPanel.add(portLocatorLabel, gbc_portLocatorLabel);

        final JTextField bmsPollIntervalField = new JTextField();
        bmsPollIntervalField.setToolTipText("Time in seconds to read data from the BMSes");
        final GridBagConstraints gbc_bmsPollIntervalField = new GridBagConstraints();
        gbc_bmsPollIntervalField.insets = new Insets(0, 0, 5, 0);
        gbc_bmsPollIntervalField.fill = GridBagConstraints.HORIZONTAL;
        gbc_bmsPollIntervalField.gridx = 1;
        gbc_bmsPollIntervalField.gridy = 1;
        bmsPanel.add(bmsPollIntervalField, gbc_bmsPollIntervalField);
        bmsPollIntervalField.setColumns(10);

        final JLabel bmsPollIntervalLabel = new JLabel("BMS poll interval (secs)");
        final GridBagConstraints gbc_bmsPollIntervalLabel = new GridBagConstraints();
        gbc_bmsPollIntervalLabel.anchor = GridBagConstraints.BELOW_BASELINE_LEADING;
        gbc_bmsPollIntervalLabel.insets = new Insets(0, 0, 5, 5);
        gbc_bmsPollIntervalLabel.gridx = 0;
        gbc_bmsPollIntervalLabel.gridy = 2;
        bmsPanel.add(bmsPollIntervalLabel, gbc_bmsPollIntervalLabel);

        final JTextField portLocatorField = new JTextField();
        final GridBagConstraints gbc_portLocatorField = new GridBagConstraints();
        gbc_portLocatorField.insets = new Insets(0, 0, 5, 0);
        gbc_portLocatorField.fill = GridBagConstraints.HORIZONTAL;
        gbc_portLocatorField.gridx = 1;
        gbc_portLocatorField.gridy = 2;
        bmsPanel.add(portLocatorField, gbc_portLocatorField);
        portLocatorField.addActionListener(e -> {
            portLocator = portLocatorField.getText();
            dispose();
        });

        final JLabel delayAfterNoBytesLabel = new JLabel("Delay after no bytes received (ms)");
        final GridBagConstraints gbc_delayAfterNoBytesLabel = new GridBagConstraints();
        gbc_delayAfterNoBytesLabel.fill = GridBagConstraints.VERTICAL;
        gbc_delayAfterNoBytesLabel.anchor = GridBagConstraints.WEST;
        gbc_delayAfterNoBytesLabel.insets = new Insets(0, 0, 0, 5);
        gbc_delayAfterNoBytesLabel.gridx = 0;
        gbc_delayAfterNoBytesLabel.gridy = 3;
        bmsPanel.add(delayAfterNoBytesLabel, gbc_delayAfterNoBytesLabel);

        final JTextField delayAfterNoBytesField = new JTextField();
        final GridBagConstraints gbc_delayAfterNoBytesField = new GridBagConstraints();
        gbc_delayAfterNoBytesField.fill = GridBagConstraints.HORIZONTAL;
        gbc_delayAfterNoBytesField.gridx = 1;
        gbc_delayAfterNoBytesField.gridy = 3;
        bmsPanel.add(delayAfterNoBytesField, gbc_delayAfterNoBytesField);
        delayAfterNoBytesField.setColumns(10);

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setPreferredSize(new Dimension(120, 40));
        buttonPanel.setSize(new Dimension(120, 40));
        buttonPanel.setMaximumSize(new Dimension(32767, 40));
        buttonPanel.setAlignmentY(1.0f);
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        final GridBagLayout gbl_buttonPanel = new GridBagLayout();
        gbl_buttonPanel.columnWidths = new int[] { 170, 170 };
        gbl_buttonPanel.rowHeights = new int[] { 20, 0 };
        gbl_buttonPanel.columnWeights = new double[] { 0.0, 0.0 };
        gbl_buttonPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        buttonPanel.setLayout(gbl_buttonPanel);

        final JButton okButton = new JButton("Ok");
        okButton.addActionListener(e -> {
            portLocator = portLocatorField.getText();
            dispose();
        });
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        final GridBagConstraints gbc_okButton = new GridBagConstraints();
        gbc_okButton.anchor = GridBagConstraints.EAST;
        gbc_okButton.insets = new Insets(0, 0, 0, 5);
        gbc_okButton.gridx = 0;
        gbc_okButton.gridy = 0;
        buttonPanel.add(okButton, gbc_okButton);

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton.addActionListener(e -> dispose());
        final GridBagConstraints gbc_cancelButton = new GridBagConstraints();
        gbc_cancelButton.anchor = GridBagConstraints.WEST;
        gbc_cancelButton.gridx = 1;
        gbc_cancelButton.gridy = 0;
        buttonPanel.add(cancelButton, gbc_cancelButton);

        setLocationRelativeTo(null);
    }


    /**
     * Gets the entered port locator string.
     *
     * @return the entered port locator string.
     */
    public String getPortLocator() {
        return portLocator;
    }


    Vector<MenuItem> createBMSItems() {
        final Vector<MenuItem> items = new Vector<>();

        items.add(new MenuItem("DALY (CAN)", ""));
        items.add(new MenuItem("DALY (RS485)", ""));
        items.add(new MenuItem("JK (CAN)", ""));
        items.add(new MenuItem("PYLONTECH (CAN)", ""));
        items.add(new MenuItem("SEPLOS (CAN)", ""));

        return items;
    }

}
