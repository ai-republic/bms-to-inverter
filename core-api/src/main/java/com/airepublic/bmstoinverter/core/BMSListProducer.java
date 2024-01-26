package com.airepublic.bmstoinverter.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.util.Util;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;

public class BMSListProducer {
    private final static Logger LOG = LoggerFactory.getLogger(BMSListProducer.class);
    private static List<BMS> bmsList = null;
    private final Map<String, BMSDescriptor> bmsDescriptors = new HashMap<>();

    /**
     * Constructor.
     */
    public BMSListProducer() {
        ServiceLoader.load(BMSDescriptor.class).forEach(descriptor -> bmsDescriptors.put(descriptor.getName(), descriptor));
    }


    /**
     * Gets the {@link BMSDescriptor} for the specified name.
     *
     * @param name the name for the {@link BMSDescriptor}
     * @return the {@link BMSDescriptor}
     */
    public BMSDescriptor getBMSDescriptor(final String name) {
        return bmsDescriptors.get(name);
    }


    @Produces
    public synchronized List<BMS> produceBMSList() {
        if (bmsList == null) {
            bmsList = new ArrayList<>();
            String type = System.getProperty("bms.0.type");

            // if no bms is found, probably the config.properties have not been read
            if (type == null) {
                Util.updateSystemProperties(Path.of(System.getProperty("configFile")));
                type = System.getProperty("bms.0.type");

                if (type == null) {
                    LOG.error("No config.properties found or no BMSes are configured!");
                    System.exit(0);
                }
            }

            int bmsNo = 0;

            while (type != null) {
                bmsList.add(createBMS(bmsNo, type));

                bmsNo++;
                type = System.getProperty("bms." + bmsNo + ".type");
            }
        }

        return bmsList;
    }


    private BMS createBMS(final int bmsNo, final String name) {
        final BMSDescriptor bmsDescriptor = getBMSDescriptor(name);
        final BMS bms = CDI.current().select(bmsDescriptor.getBMSClass()).get();
        final String portLocator = System.getProperty("bms." + bmsNo + ".portLocator");
        final int pollInverval = Integer.valueOf(System.getProperty("bms." + bmsNo + ".pollInterval"));
        final int delayAfterNoBytes = Integer.valueOf(System.getProperty("bms." + bmsNo + ".delayAfterNoBytes"));
        final BMSConfig config = new BMSConfig(bmsNo, portLocator, pollInverval, delayAfterNoBytes, bmsDescriptor);
        bms.initialize(config);

        return bms;
    }

}
