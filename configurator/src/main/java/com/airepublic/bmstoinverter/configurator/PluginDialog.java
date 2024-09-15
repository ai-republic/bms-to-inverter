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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.airepublic.bmstoinverter.core.AbstractPlugin;
import com.airepublic.bmstoinverter.core.BmsPlugin;
import com.airepublic.bmstoinverter.core.InverterPlugin;
import com.airepublic.bmstoinverter.core.PluginConfig;
import com.airepublic.bmstoinverter.core.PluginProperty;

public class PluginDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final JPanel propertiesPanel = new JPanel();
    final JComboBox<PluginConfig> pluginComboBox = new JComboBox<>();

    /**
     * Constructor.
     *
     * @param configurator the {@link Configurator} frame
     * @param typeClass the type of plugin, e.g. {@link BmsPlugin} or {@link InverterPlugin}
     */
    public PluginDialog(final Configurator configurator, final Class<? extends AbstractPlugin<?>> typeClass) {
        super(configurator, "Plugin configuration...", true);

        setLocation(configurator.getBounds().width / 2 - 250, configurator.getBounds().height / 2 - 60);
        setSize(new Dimension(500, 450));
        setResizable(false);
        getContentPane().setLayout(new BorderLayout(5, 5));

        final JPanel pluginPanel = new JPanel();
        pluginPanel.setBorder(new EmptyBorder(5, 10, 0, 5));
        getContentPane().add(pluginPanel, BorderLayout.CENTER);
        final GridBagLayout gbl_pluginPanel = new GridBagLayout();
        gbl_pluginPanel.columnWidths = new int[] { 321 };
        gbl_pluginPanel.rowHeights = new int[] { 30, 250, 0 };
        gbl_pluginPanel.columnWeights = new double[] { 1.0 };
        gbl_pluginPanel.rowWeights = new double[] { 1.0, 1.0, 1.0 };
        pluginPanel.setLayout(gbl_pluginPanel);

        final GridBagConstraints gbc_pluginComboBox = new GridBagConstraints();
        gbc_pluginComboBox.fill = GridBagConstraints.HORIZONTAL;
        gbc_pluginComboBox.insets = new Insets(0, 0, 5, 0);
        gbc_pluginComboBox.gridx = 0;
        gbc_pluginComboBox.gridy = 0;
        pluginPanel.add(pluginComboBox, gbc_pluginComboBox);

        pluginComboBox.setModel(initializePluginList(typeClass));
        pluginComboBox.setSelectedIndex(-1);

        final JScrollPane propertiesScrollPane = new JScrollPane();
        propertiesScrollPane.setAutoscrolls(true);

        final GridBagConstraints gbc_propertiesScrollPane = new GridBagConstraints();
        gbc_propertiesScrollPane.insets = new Insets(0, 0, 5, 0);
        gbc_propertiesScrollPane.fill = GridBagConstraints.BOTH;
        gbc_propertiesScrollPane.gridx = 0;
        gbc_propertiesScrollPane.gridy = 1;
        pluginPanel.add(propertiesScrollPane, gbc_propertiesScrollPane);

        propertiesScrollPane.setViewportView(propertiesPanel);

        final JPanel buttonPanel = new JPanel();
        final GridBagConstraints gbc_buttonPanel = new GridBagConstraints();
        gbc_buttonPanel.fill = GridBagConstraints.BOTH;
        gbc_buttonPanel.gridx = 0;
        gbc_buttonPanel.gridy = 2;
        pluginPanel.add(buttonPanel, gbc_buttonPanel);

        final JButton okButton = new JButton("Ok");
        buttonPanel.add(okButton);
        okButton.addActionListener(e -> {
            dispose();
        });

        final JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(e -> {
            dispose();
        });

        pluginComboBox.addActionListener(event -> {
            buildPropertiesPanel(((PluginConfig) pluginComboBox.getSelectedItem()).getProperties());
            propertiesScrollPane.validate();
        });

        setLocationRelativeTo(null);
    }


    /**
     * Builds the properties panel after a plugin selection.
     *
     * @param properties the plugin properties
     */
    private void buildPropertiesPanel(final Collection<PluginProperty> properties) {
        propertiesPanel.removeAll();

        final GridBagLayout gbl_propertiesPanel = new GridBagLayout();
        gbl_propertiesPanel.columnWidths = new int[] { 150, 200 };
        gbl_propertiesPanel.rowHeights = new int[properties.size()];
        Arrays.fill(gbl_propertiesPanel.rowHeights, 30);
        gbl_propertiesPanel.columnWeights = new double[] { 0.0, 1.0 };
        gbl_propertiesPanel.rowWeights = new double[properties.size()];
        Arrays.fill(gbl_propertiesPanel.rowWeights, 0.0d);
        propertiesPanel.setLayout(gbl_propertiesPanel);

        final Iterator<PluginProperty> it = properties.iterator();

        for (int i = 0; it.hasNext(); i++) {
            final PluginProperty p = it.next();
            final JLabel label = new JLabel();
            label.setText(p.getName());
            label.setToolTipText(p.getDescription());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 5, 5);
            propertiesPanel.add(label, gbc);

            final JTextField field = new JTextField();
            field.setText(p.getValue() != null ? p.getValue().toString() : "");
            field.setToolTipText(p.getDescription());
            gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = i;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 5, 5);
            propertiesPanel.add(field, gbc);

            label.setLabelFor(field);
        }

        propertiesPanel.validate();
    }


    /**
     * Initializes the plugin combobox with available plugins of the plugin type class.
     *
     * @param typeClass the plugin type class
     * @return the combobox model
     */
    private DefaultComboBoxModel<PluginConfig> initializePluginList(final Class<? extends AbstractPlugin<?>> typeClass) {
        final ServiceLoader<? extends AbstractPlugin<?>> loader = ServiceLoader.load(typeClass);
        final DefaultComboBoxModel<PluginConfig> model = new DefaultComboBoxModel<>();
        loader.forEach(p -> {
            model.addElement(new PluginConfig(p.getName(), p.getClass().getName(), p.getProperties()));
        });

        return model;
    }


    /**
     * Gets the {@link PluginConfig} for the selected plugin.
     *
     * @return the {@link PluginConfig}
     */
    public PluginConfig getPluginConfig() {
        final int idx = pluginComboBox.getSelectedIndex();

        if (idx == -1) {
            return null;
        }

        final PluginConfig pluginConfig = pluginComboBox.getItemAt(idx);
        final Set<PluginProperty> properties = new LinkedHashSet<>();

        for (int i = 0; i < propertiesPanel.getComponentCount(); i++) {
            final Component comp = propertiesPanel.getComponent(i);

            if (JLabel.class.isAssignableFrom(comp.getClass())) {
                final JLabel label = (JLabel) comp;
                final JTextField field = (JTextField) label.getLabelFor();
                final PluginProperty property = pluginConfig.getProperty(label.getText());
                final PluginProperty prop = new PluginProperty(property.getName(), field.getText(), property.getDescription());
                properties.add(prop);
            }
        }

        return new PluginConfig(pluginConfig.getPluginName(), pluginConfig.getPluginClass(), properties);
    }


    /**
     * Sets the {@link PluginConfig} for the selected plugin.
     *
     * @param config the {@link PluginConfig}
     */
    public void setPluginConfig(final PluginConfig config) {
        pluginComboBox.setSelectedItem(config);
        buildPropertiesPanel(config.getProperties());
    }
}
