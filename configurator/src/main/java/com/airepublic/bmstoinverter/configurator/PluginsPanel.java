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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.MouseInputAdapter;

import com.airepublic.bmstoinverter.core.AbstractPlugin;
import com.airepublic.bmstoinverter.core.BmsPlugin;
import com.airepublic.bmstoinverter.core.InverterPlugin;
import com.airepublic.bmstoinverter.core.PluginConfig;
import com.airepublic.bmstoinverter.core.PluginProperty;

/**
 * The tab panel to configure {@link BmsPlugin}s and {@link InverterPlugin}s.
 */
public class PluginsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JList<PluginConfig> bmsPluginsList = new JList<>(new DefaultListModel<PluginConfig>());
    private final JList<PluginConfig> inverterPluginsList = new JList<>(new DefaultListModel<PluginConfig>());

    /**
     * Constructor.
     *
     * @param configurator the {@link Configurator} application
     */
    public PluginsPanel(final Configurator configurator) {
        final GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 120, 400, 70 };
        gridBagLayout.rowHeights = new int[] { 200, 200 };
        gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 1.0 };
        gridBagLayout.rowWeights = new double[] { 1.0, 1.0 };
        setLayout(gridBagLayout);

        final JLabel bmsPluginsLabel = new JLabel("BMS Plugins");
        final GridBagConstraints gbc_bmsPluginsLabel = new GridBagConstraints();
        gbc_bmsPluginsLabel.insets = new Insets(0, 0, 5, 5);
        gbc_bmsPluginsLabel.anchor = GridBagConstraints.BELOW_BASELINE;
        gbc_bmsPluginsLabel.gridx = 0;
        gbc_bmsPluginsLabel.gridy = 0;
        add(bmsPluginsLabel, gbc_bmsPluginsLabel);

        final JScrollPane bmsPluginsScrollPane = new JScrollPane();
        final GridBagConstraints gbc_bmsPluginsScrollPane = new GridBagConstraints();
        gbc_bmsPluginsScrollPane.insets = new Insets(0, 0, 5, 5);
        gbc_bmsPluginsScrollPane.fill = GridBagConstraints.BOTH;
        gbc_bmsPluginsScrollPane.gridx = 1;
        gbc_bmsPluginsScrollPane.gridy = 0;
        add(bmsPluginsScrollPane, gbc_bmsPluginsScrollPane);

        bmsPluginsScrollPane.setViewportView(bmsPluginsList);

        final JPanel bmsPluginsButtonPanel = new JPanel();
        final GridBagConstraints gbc_bmsPluginsButtonPanel = new GridBagConstraints();
        gbc_bmsPluginsButtonPanel.insets = new Insets(0, 0, 5, 0);
        gbc_bmsPluginsButtonPanel.fill = GridBagConstraints.BOTH;
        gbc_bmsPluginsButtonPanel.gridx = 2;
        gbc_bmsPluginsButtonPanel.gridy = 0;
        add(bmsPluginsButtonPanel, gbc_bmsPluginsButtonPanel);
        final GridBagLayout gbl_bmsPluginsButtonPanel = new GridBagLayout();
        gbl_bmsPluginsButtonPanel.columnWidths = new int[] { 50 };
        gbl_bmsPluginsButtonPanel.rowHeights = new int[] { 30, 30, 30 };
        gbl_bmsPluginsButtonPanel.columnWeights = new double[] { 0.0 };
        gbl_bmsPluginsButtonPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
        bmsPluginsButtonPanel.setLayout(gbl_bmsPluginsButtonPanel);

        final JButton addBmsPluginButton = new JButton("Add...");
        final GridBagConstraints gbc_addBmsPluginButton = new GridBagConstraints();
        gbc_addBmsPluginButton.insets = new Insets(0, 0, 5, 5);
        gbc_addBmsPluginButton.fill = GridBagConstraints.BOTH;
        gbc_addBmsPluginButton.gridx = 0;
        gbc_addBmsPluginButton.gridy = 0;
        bmsPluginsButtonPanel.add(addBmsPluginButton, gbc_addBmsPluginButton);
        addBmsPluginButton.addActionListener(event -> {
            final PluginDialog dlg = new PluginDialog(configurator, BmsPlugin.class);
            dlg.setVisible(true);

            final PluginConfig config = dlg.getPluginConfig();

            if (config != null) {
                ((DefaultListModel<PluginConfig>) bmsPluginsList.getModel()).addElement(config);
            }
        });

        final JButton editBmsPluginButton = new JButton("Edit...");
        final GridBagConstraints gbc_editBmsPluginButton = new GridBagConstraints();
        gbc_editBmsPluginButton.insets = new Insets(0, 0, 5, 5);
        gbc_editBmsPluginButton.fill = GridBagConstraints.BOTH;
        gbc_editBmsPluginButton.gridx = 0;
        gbc_editBmsPluginButton.gridy = 1;
        bmsPluginsButtonPanel.add(editBmsPluginButton, gbc_editBmsPluginButton);
        editBmsPluginButton.addActionListener(event -> {
            final int idx = bmsPluginsList.getSelectedIndex();

            if (idx != -1) {
                final PluginDialog dlg = new PluginDialog(configurator, BmsPlugin.class);
                dlg.setPluginConfig(bmsPluginsList.getSelectedValue());
                dlg.setVisible(true);

                final PluginConfig config = dlg.getPluginConfig();

                if (config != null) {
                    ((DefaultListModel<PluginConfig>) bmsPluginsList.getModel()).set(idx, config);
                }
            }
        });

        final JButton removeBmsPluginButton = new JButton("Remove");
        final GridBagConstraints gbc_removeBmsPluginButton = new GridBagConstraints();
        gbc_removeBmsPluginButton.insets = new Insets(0, 0, 5, 5);
        gbc_removeBmsPluginButton.fill = GridBagConstraints.BOTH;
        gbc_removeBmsPluginButton.gridx = 0;
        gbc_removeBmsPluginButton.gridy = 2;
        bmsPluginsButtonPanel.add(removeBmsPluginButton, gbc_removeBmsPluginButton);
        removeBmsPluginButton.addActionListener(event -> {
            final int selectedIndices[] = bmsPluginsList.getSelectedIndices();

            if (selectedIndices.length > 0) {
                Arrays.sort(selectedIndices);
                for (int i = selectedIndices.length - 1; i >= 0; i--) {
                    ((DefaultListModel<PluginConfig>) bmsPluginsList.getModel()).remove(i);
                }
            }
        });

        bmsPluginsList.addMouseListener(new MouseInputAdapter() {

            @Override
            public void mouseClicked(final MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    Stream.of(editBmsPluginButton.getActionListeners()).forEach(listener -> listener.actionPerformed(new ActionEvent(editBmsPluginButton, ActionEvent.ACTION_PERFORMED, "edit")));
                }
            }
        });

        final JLabel inverterPluginsLabel = new JLabel("Inverter Plugins");
        final GridBagConstraints gbc_inverterPluginsLabel = new GridBagConstraints();
        gbc_inverterPluginsLabel.insets = new Insets(0, 0, 0, 5);
        gbc_inverterPluginsLabel.gridx = 0;
        gbc_inverterPluginsLabel.gridy = 1;
        add(inverterPluginsLabel, gbc_inverterPluginsLabel);

        final JScrollPane inverterPluginsScrollPane = new JScrollPane();
        final GridBagConstraints gbc_inverterPluginsScrollPane = new GridBagConstraints();
        gbc_inverterPluginsScrollPane.insets = new Insets(0, 0, 0, 5);
        gbc_inverterPluginsScrollPane.fill = GridBagConstraints.BOTH;
        gbc_inverterPluginsScrollPane.gridx = 1;
        gbc_inverterPluginsScrollPane.gridy = 1;
        add(inverterPluginsScrollPane, gbc_inverterPluginsScrollPane);

        inverterPluginsScrollPane.setViewportView(inverterPluginsList);

        final JPanel inverterPluginsButtonPanel = new JPanel();
        final GridBagConstraints gbc_inverterPluginsButtonPanel = new GridBagConstraints();
        gbc_inverterPluginsButtonPanel.fill = GridBagConstraints.BOTH;
        gbc_inverterPluginsButtonPanel.gridx = 2;
        gbc_inverterPluginsButtonPanel.gridy = 1;
        add(inverterPluginsButtonPanel, gbc_inverterPluginsButtonPanel);

        final GridBagLayout gbl_inverterPluginsButtonPanel = new GridBagLayout();
        gbl_inverterPluginsButtonPanel.columnWidths = new int[] { 50 };
        gbl_inverterPluginsButtonPanel.rowHeights = new int[] { 30, 30, 30 };
        gbl_inverterPluginsButtonPanel.columnWeights = new double[] { 0.0 };
        gbl_inverterPluginsButtonPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
        inverterPluginsButtonPanel.setLayout(gbl_inverterPluginsButtonPanel);

        final JButton addInverterPluginButton = new JButton("Add...");
        final GridBagConstraints gbc_addInverterPluginButton = new GridBagConstraints();
        gbc_addInverterPluginButton.insets = new Insets(0, 0, 5, 5);
        gbc_addInverterPluginButton.fill = GridBagConstraints.BOTH;
        gbc_addInverterPluginButton.gridx = 0;
        gbc_addInverterPluginButton.gridy = 0;
        inverterPluginsButtonPanel.add(addInverterPluginButton, gbc_addInverterPluginButton);

        addInverterPluginButton.addActionListener(event -> {
            final PluginDialog dlg = new PluginDialog(configurator, InverterPlugin.class);
            dlg.setVisible(true);

            final PluginConfig config = dlg.getPluginConfig();

            if (config != null) {
                ((DefaultListModel<PluginConfig>) inverterPluginsList.getModel()).addElement(config);
            }
        });

        final JButton editInverterPluginButton = new JButton("Edit...");
        final GridBagConstraints gbc_editInverterPluginButton = new GridBagConstraints();
        gbc_editInverterPluginButton.insets = new Insets(0, 0, 5, 5);
        gbc_editInverterPluginButton.fill = GridBagConstraints.BOTH;
        gbc_editInverterPluginButton.gridx = 0;
        gbc_editInverterPluginButton.gridy = 1;
        inverterPluginsButtonPanel.add(editInverterPluginButton, gbc_editInverterPluginButton);
        editInverterPluginButton.addActionListener(event -> {
            final int idx = inverterPluginsList.getSelectedIndex();

            if (idx != -1) {
                final PluginDialog dlg = new PluginDialog(configurator, InverterPlugin.class);
                dlg.setPluginConfig(inverterPluginsList.getSelectedValue());
                dlg.setVisible(true);

                final PluginConfig config = dlg.getPluginConfig();

                if (config != null) {
                    ((DefaultListModel<PluginConfig>) inverterPluginsList.getModel()).set(idx, config);
                }
            }
        });
        final JButton removeInverterPluginButton = new JButton("Remove");
        final GridBagConstraints gbc_removeInverterPluginButton = new GridBagConstraints();
        gbc_removeInverterPluginButton.insets = new Insets(0, 0, 5, 5);
        gbc_removeInverterPluginButton.fill = GridBagConstraints.BOTH;
        gbc_removeInverterPluginButton.gridx = 0;
        gbc_removeInverterPluginButton.gridy = 2;
        inverterPluginsButtonPanel.add(removeInverterPluginButton, gbc_removeInverterPluginButton);
        removeInverterPluginButton.addActionListener(event -> {
            final int selectedIndices[] = inverterPluginsList.getSelectedIndices();

            if (selectedIndices.length > 0) {
                Arrays.sort(selectedIndices);
                for (int i = selectedIndices.length - 1; i >= 0; i--) {
                    ((DefaultListModel<PluginConfig>) inverterPluginsList.getModel()).remove(i);
                }
            }
        });

        inverterPluginsList.addMouseListener(new MouseInputAdapter() {

            @Override
            public void mouseClicked(final MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    Stream.of(editInverterPluginButton.getActionListeners()).forEach(listener -> listener.actionPerformed(new ActionEvent(editInverterPluginButton, ActionEvent.ACTION_PERFORMED, "edit")));
                }
            }
        });

    }


    /**
     * Creats the plugin configuration and writes it to the global config.
     *
     * @param config the global config
     */
    protected void generateConfiguration(final StringBuffer config) {
        if (bmsPluginsList.getModel().getSize() > 0 || inverterPluginsList.getModel().getSize() > 0) {
            config.append("###################################################################\n"
                    + "###                     Plugin settings                         ###\n"
                    + "###################################################################\n");

            writePluginListConfiguration("plugin.bms", bmsPluginsList.getModel(), config);
            writePluginListConfiguration("plugin.inverter", inverterPluginsList.getModel(), config);
        }
    }


    /**
     * Writes the the plugin configurations to the global configuration.
     *
     * @param prefix the prefix whether its a {@link BmsPlugin} ("bms.plugin") or
     *        {@link InverterPlugin} ("inverter.plugin")
     * @param config the global configuration
     * @param model the list model to process
     * @return the {@link DefaultListModel} for the plugin list
     */
    void writePluginListConfiguration(final String prefix, final ListModel<PluginConfig> model, final StringBuffer config) {
        for (int pluginIdx = 1; pluginIdx <= model.getSize(); pluginIdx++) {
            final PluginConfig pluginConfig = model.getElementAt(pluginIdx - 1);

            config.append(prefix + "." + pluginIdx + ".class=" + pluginConfig.getPluginClass() + "\n");

            int propIdx = 1;

            for (final PluginProperty prop : pluginConfig.getProperties()) {

                config.append(prefix + "." + pluginIdx + ".property." + propIdx + ".name=" + prop.getName() + "\n");
                config.append(prefix + "." + pluginIdx + ".property." + propIdx + ".value=" + prop.getValue() + "\n");
                config.append(prefix + "." + pluginIdx + ".property." + propIdx + ".description=" + prop.getDescription() + "\n");

                propIdx++;
            }
            config.append("\n");
        }
    }


    /**
     * Sets the global configuration to read the plugin configurations from.
     *
     * @param config the global configuration
     * @throws Exception if an error occurs during processing
     */
    void setConfiguration(final Properties config) throws Exception {
        bmsPluginsList.setModel(readPluginListConfiguration("plugin.bms", config));
        inverterPluginsList.setModel(readPluginListConfiguration("plugin.inverter", config));
    }


    /**
     * Reads the the plugin configurations from the global configuration.
     *
     * @param prefix the prefix whether its a {@link BmsPlugin} ("bms.plugin") or
     *        {@link InverterPlugin} ("inverter.plugin")
     * @param config the global configuration
     * @return the {@link DefaultListModel} for the plugin list
     * @throws Exception if the plugin class could not be instantiated
     */
    private DefaultListModel<PluginConfig> readPluginListConfiguration(final String prefix, final Properties config) throws Exception {
        final DefaultListModel<PluginConfig> model = new DefaultListModel<>();
        int pluginIdx = 1;
        boolean found = true;

        do {
            // first check if the next plugin config is available
            final String pluginClassName = config.getProperty(prefix + "." + pluginIdx + ".class");

            if (pluginClassName != null) {
                // create the plugin from its class
                final Class<?> pluginClass = Class.forName(pluginClassName);
                final AbstractPlugin<?> plugin = (AbstractPlugin<?>) pluginClass.getConstructor().newInstance();

                // then read all the plugin properties
                final Set<PluginProperty> pluginProperties = new LinkedHashSet<>();
                boolean propFound = true;
                int propIdx = 1;

                do {
                    final String name = config.getProperty(prefix + "." + pluginIdx + ".property." + propIdx + ".name");

                    // check if the property exists
                    if (name != null) {
                        final String value = config.getProperty(prefix + "." + pluginIdx + ".property." + propIdx + ".value");
                        final String description = config.getProperty(prefix + "." + pluginIdx + ".property." + propIdx + ".description");

                        // add the plugin property
                        pluginProperties.add(new PluginProperty(name, value, description));

                        propIdx++;
                    } else {
                        propFound = false;
                    }
                } while (propFound);

                // add the plugin config
                model.addElement(new PluginConfig(plugin.getName(), plugin.getClass().getName(), pluginProperties));

                pluginIdx++;
            } else {
                found = false;
            }
        } while (found);

        return model;
    }

}
