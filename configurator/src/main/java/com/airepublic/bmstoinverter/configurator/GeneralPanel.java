package com.airepublic.bmstoinverter.configurator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class GeneralPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Vector<String> platformItems = createPlatformItems();
    private final JComboBox<String> platformField;
    private final JTextField installationPathField;

    public GeneralPanel() {
        setBorder(new EmptyBorder(10, 10, 0, 10));
        final GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 100, 150, 100 };
        gridBagLayout.rowHeights = new int[] { 30, 30, 30 };
        gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0 };
        gridBagLayout.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
        setLayout(gridBagLayout);

        final JLabel platformLabel = new JLabel("OS Platform");
        platformLabel.setToolTipText("OS platform");
        final GridBagConstraints gbc_platformLabel = new GridBagConstraints();
        gbc_platformLabel.anchor = GridBagConstraints.WEST;
        gbc_platformLabel.insets = new Insets(0, 0, 0, 5);
        gbc_platformLabel.gridx = 0;
        gbc_platformLabel.gridy = 0;
        add(platformLabel, gbc_platformLabel);

        platformField = new JComboBox<>(platformItems);
        platformField.setToolTipText("OS platform");
        final GridBagConstraints gbc_platformField = new GridBagConstraints();
        gbc_platformField.insets = new Insets(0, 0, 5, 5);
        gbc_platformField.fill = GridBagConstraints.BOTH;
        gbc_platformField.gridx = 1;
        gbc_platformField.gridy = 0;
        add(platformField, gbc_platformField);

        final JLabel installationPathLabel = new JLabel("Installation path");
        installationPathLabel.setToolTipText("Path where the application will be installed");
        final GridBagConstraints gbc_installationPathLabel = new GridBagConstraints();
        gbc_installationPathLabel.anchor = GridBagConstraints.WEST;
        gbc_installationPathLabel.insets = new Insets(0, 0, 0, 5);
        gbc_installationPathLabel.gridx = 0;
        gbc_installationPathLabel.gridy = 1;
        add(installationPathLabel, gbc_installationPathLabel);

        installationPathField = new JTextField();
        final GridBagConstraints gbc_installationPathField = new GridBagConstraints();
        gbc_installationPathField.insets = new Insets(0, 0, 0, 5);
        gbc_installationPathField.fill = GridBagConstraints.BOTH;
        gbc_installationPathField.gridx = 1;
        gbc_installationPathField.gridy = 1;
        add(installationPathField, gbc_installationPathField);
        installationPathField.setColumns(10);

        final JButton chooseInstallationPathButton = new JButton("Choose");
        final GridBagConstraints gbc_chooseInstallationPathButton = new GridBagConstraints();
        gbc_chooseInstallationPathButton.insets = new Insets(0, 0, 0, 0);
        gbc_chooseInstallationPathButton.gridx = 2;
        gbc_chooseInstallationPathButton.gridy = 1;
        add(chooseInstallationPathButton, gbc_chooseInstallationPathButton);
        chooseInstallationPathButton.addActionListener(e -> {
            final JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home", "~"));
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                final File file = fileChooser.getSelectedFile();
                installationPathField.setText(file.getAbsolutePath());
            }
        });
    }


    private Vector<String> createPlatformItems() {
        final Vector<String> items = new Vector<>();

        items.add("AARCH64");
        items.add("ARM v5");
        items.add("ARM v6");
        items.add("ARM v7");
        items.add("ARM v7a");
        items.add("ARM v7l");
        items.add("RISCV v32");
        items.add("RISCV v64");
        items.add("X86 32bit (UNIX)");
        items.add("X86 64bit (UNIX)");

        return items;
    }


    public boolean verify(final StringBuffer errors) {
        if (platformField.getSelectedIndex() == -1) {
            errors.append("Missing OS platform\r\n");
            return false;
        }

        if (installationPathField.getText().isBlank()) {
            errors.append("Missing installation directory\r\n");
            return false;
        }

        return true;
    }


    public String getInstallationPath() {
        return installationPathField.getText();
    }

}
