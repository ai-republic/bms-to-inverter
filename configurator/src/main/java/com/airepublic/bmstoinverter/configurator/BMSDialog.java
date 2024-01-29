package com.airepublic.bmstoinverter.configurator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ServiceLoader;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;

/**
 * Dialog to enter a port locator.
 */
public class BMSDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final Vector<MenuItem<BMSDescriptor>> descriptors;
    private BMSConfig config = null;
    private final JComboBox<MenuItem<BMSDescriptor>> bmses;
    private final JTextField portLocatorField;
    private JTextField pollIntervalField;
    private final JTextField delayAfterNoBytesField;
    private final NumberInputVerifier numberInputVerifier = new NumberInputVerifier();

    /**
     * Constructor.
     *
     * @param owner the owning frame
     */
    public BMSDialog(final JFrame owner) {
        super(owner, "BMS configuration...", true);

        descriptors = createBMSDescriptors();

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
        gbc_bmsLabel.anchor = GridBagConstraints.EAST;
        gbc_bmsLabel.insets = new Insets(0, 0, 5, 5);
        gbc_bmsLabel.gridx = 0;
        gbc_bmsLabel.gridy = 0;
        bmsPanel.add(bmsLabel, gbc_bmsLabel);

        // BMS dropdown
        bmses = new JComboBox<>(descriptors);
        final GridBagConstraints gbc_bmses = new GridBagConstraints();
        gbc_bmses.insets = new Insets(0, 0, 5, 0);
        gbc_bmses.fill = GridBagConstraints.HORIZONTAL;
        gbc_bmses.gridx = 1;
        gbc_bmses.gridy = 0;
        bmsPanel.add(bmses, gbc_bmses);

        final JLabel portLocatorLabel = new JLabel("Port");
        portLocatorLabel.setToolTipText("The qualified name of the port, e.g. /dev/ttyS0 or can0 or com3");
        final GridBagConstraints gbc_portLocatorLabel = new GridBagConstraints();
        gbc_portLocatorLabel.insets = new Insets(0, 0, 5, 5);
        gbc_portLocatorLabel.anchor = GridBagConstraints.EAST;
        gbc_portLocatorLabel.gridx = 0;
        gbc_portLocatorLabel.gridy = 1;
        bmsPanel.add(portLocatorLabel, gbc_portLocatorLabel);

        portLocatorField = new JTextField();
        final GridBagConstraints gbc_portLocatorField = new GridBagConstraints();
        gbc_portLocatorField.insets = new Insets(0, 0, 5, 0);
        gbc_portLocatorField.fill = GridBagConstraints.HORIZONTAL;
        gbc_portLocatorField.gridx = 1;
        gbc_portLocatorField.gridy = 1;
        bmsPanel.add(portLocatorField, gbc_portLocatorField);
        portLocatorField.addActionListener(e -> {
            pollIntervalField.requestFocus();
        });

        final JLabel bmsPollIntervalLabel = new JLabel("BMS poll interval (secs)");
        final GridBagConstraints gbc_bmsPollIntervalLabel = new GridBagConstraints();
        gbc_bmsPollIntervalLabel.anchor = GridBagConstraints.BELOW_BASELINE_TRAILING;
        gbc_bmsPollIntervalLabel.insets = new Insets(0, 0, 5, 5);
        gbc_bmsPollIntervalLabel.gridx = 0;
        gbc_bmsPollIntervalLabel.gridy = 2;
        bmsPanel.add(bmsPollIntervalLabel, gbc_bmsPollIntervalLabel);

        pollIntervalField = new JTextField();
        pollIntervalField.setToolTipText("Time in seconds to read data from the BMSes");
        pollIntervalField.setColumns(10);
        final GridBagConstraints gbc_bmsPollInvervalField = new GridBagConstraints();
        gbc_bmsPollInvervalField.insets = new Insets(0, 0, 5, 0);
        gbc_bmsPollInvervalField.fill = GridBagConstraints.HORIZONTAL;
        gbc_bmsPollInvervalField.gridx = 1;
        gbc_bmsPollInvervalField.gridy = 2;
        bmsPanel.add(pollIntervalField, gbc_bmsPollInvervalField);

        final JLabel delayAfterNoBytesLabel = new JLabel("Delay after no bytes (ms)");
        final GridBagConstraints gbc_delayAfterNoBytesLabel = new GridBagConstraints();
        gbc_delayAfterNoBytesLabel.fill = GridBagConstraints.VERTICAL;
        gbc_delayAfterNoBytesLabel.anchor = GridBagConstraints.EAST;
        gbc_delayAfterNoBytesLabel.insets = new Insets(0, 0, 5, 5);
        gbc_delayAfterNoBytesLabel.gridx = 0;
        gbc_delayAfterNoBytesLabel.gridy = 3;
        bmsPanel.add(delayAfterNoBytesLabel, gbc_delayAfterNoBytesLabel);

        delayAfterNoBytesField = new JTextField();
        final GridBagConstraints gbc_delayAfterNoBytesField = new GridBagConstraints();
        gbc_delayAfterNoBytesField.insets = new Insets(0, 0, 5, 0);
        gbc_delayAfterNoBytesField.fill = GridBagConstraints.HORIZONTAL;
        gbc_delayAfterNoBytesField.gridx = 1;
        gbc_delayAfterNoBytesField.gridy = 3;
        bmsPanel.add(delayAfterNoBytesField, gbc_delayAfterNoBytesField);
        delayAfterNoBytesField.setColumns(10);
        pollIntervalField.addActionListener(e -> {
            delayAfterNoBytesField.requestFocus();
        });

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
            final StringBuffer errors = new StringBuffer();
            if (!verify(errors)) {
                JOptionPane.showInternalMessageDialog(getContentPane(), errors, "Error", JOptionPane.ERROR_MESSAGE);
                System.out.println(errors);
                return;
            }

            final BMSDescriptor descriptor = bmses.getModel().getElementAt(bmses.getSelectedIndex()).getValue();
            final String portLocator = portLocatorField.getText();
            final int pollInterval = Integer.valueOf(pollIntervalField.getText());
            final long delayAfterNoBytes = Long.valueOf(delayAfterNoBytesField.getText());

            if (config == null) {
                config = new BMSConfig(0, portLocator, pollInterval, delayAfterNoBytes, descriptor);
            } else {
                config.update(config.getBmsNo(), portLocator, pollInterval, delayAfterNoBytes, descriptor);
            }
            dispose();
        });
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        final GridBagConstraints gbc_okButton = new GridBagConstraints();
        gbc_okButton.anchor = GridBagConstraints.EAST;
        gbc_okButton.insets = new Insets(0, 0, 0, 5);
        gbc_okButton.gridx = 0;
        gbc_okButton.gridy = 0;
        buttonPanel.add(okButton, gbc_okButton);
        delayAfterNoBytesField.addActionListener(e -> {
            okButton.doClick();
        });

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
    public BMSConfig getBMSConfig() {
        return config;
    }


    /**
     * Sets the entered port locator string.
     *
     * @param config the entered port locator string.
     */
    public void setBMSConfig(final BMSConfig config) {
        this.config = config;

        for (int i = 0; i < bmses.getItemCount(); i++) {
            if (bmses.getItemAt(i).getValue().getName().equals(config.getDescriptor().getName())) {
                bmses.setSelectedIndex(i);
                break;
            }
        }

        portLocatorField.setText(config.getPortLocator());
        delayAfterNoBytesField.setText("" + config.getDelayAfterNoBytes());
        pollIntervalField.setText("" + config.getPollInterval());
    }


    Vector<MenuItem<BMSDescriptor>> createBMSDescriptors() {
        final Vector<MenuItem<BMSDescriptor>> items = new Vector<>();
        ServiceLoader.load(BMSDescriptor.class).forEach(descriptor -> items.add(new MenuItem<>(descriptor.getName(), descriptor)));

        return items;
    }


    public boolean verify(final StringBuffer errors) {
        boolean fail = false;

        if (bmses.getSelectedIndex() == -1) {
            errors.append("Missing BMS type!\r\n");
            fail = true;
        }

        if (portLocatorField.getText().isBlank()) {
            errors.append("Missing BMS port locator!\r\n");
            fail = true;
        }

        if (pollIntervalField.getText().isBlank()) {
            errors.append("Missing BMS poll interval!\r\n");
            fail = true;
        } else if (!numberInputVerifier.verify(pollIntervalField.getText())) {
            errors.append("Non-numeric BMS poll interval!\r\n");
            fail = true;
        }

        if (delayAfterNoBytesField.getText().isBlank()) {
            errors.append("Missing BMS delay after no bytes received!\r\n");
            fail = true;
        } else if (!numberInputVerifier.verify(delayAfterNoBytesField.getText())) {
            errors.append("Non-numeric BMS delay after no bytes!\r\n");
            fail = true;
        }

        return !fail;
    }

}
