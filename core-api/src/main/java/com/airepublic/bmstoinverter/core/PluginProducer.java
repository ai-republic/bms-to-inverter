package com.airepublic.bmstoinverter.core;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginProducer {
    private final static Logger LOG = LoggerFactory.getLogger(PluginProducer.class);

    protected <T extends AbstractPlugin<?>> Set<T> loadPlugins(final Class<T> pluginClass) {
        int pluginIndex = 1;
        final String type = BmsPlugin.class.isAssignableFrom(pluginClass) ? "bms" : "inverter";
        final Set<T> plugins = new LinkedHashSet<>();
        String className;

        do {
            className = System.getProperty(type + ".plugin." + pluginIndex + ".class");

            if (className != null) {
                try {
                    final AbstractPlugin<?> plugin = (AbstractPlugin<?>) Class.forName(className).getConstructor().newInstance();
                    LOG.info("Registering " + type + " plugin '" + plugin.getName() + "'...");
                    int propertyIndex = 1;
                    PluginProperty property = null;

                    do {
                        final String name = System.getProperty(type + ".plugin." + propertyIndex + ".name");

                        if (name != null) {
                            final String value = System.getProperty(type + ".plugin." + propertyIndex + ".value");
                            final String description = System.getProperty(type + ".plugin." + propertyIndex + ".description");
                            property = new PluginProperty(name, value, description);
                            LOG.info("Registering plugin-property for plugin '{}':\n\t{}=\"{}\" - {}", plugin.getName(), name, description);
                            plugin.addProperty(property);
                        }

                        propertyIndex++;
                    } while (property == null);
                } catch (final Throwable t) {
                    LOG.error("Failed to find plugin class for type: " + className);
                }
            }

            pluginIndex++;
        } while (className != null);

        return plugins;
    }
}
