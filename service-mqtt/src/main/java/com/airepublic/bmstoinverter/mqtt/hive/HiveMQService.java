package com.airepublic.bmstoinverter.mqtt.hive;

import java.nio.file.Path;

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.embedded.EmbeddedHiveMQ;
import com.hivemq.embedded.EmbeddedHiveMQBuilder;
import com.hivemq.migration.meta.PersistenceType;

public class HiveMQService {

    public static void main(final String[] args) {

        final EmbeddedHiveMQBuilder embeddedHiveMQBuilder = EmbeddedHiveMQ.builder()
                .withConfigurationFolder(Path.of("hive-config"))
                .withDataFolder(Path.of("data"))
                .withExtensionsFolder(Path.of("extensions"));

        EmbeddedHiveMQ hiveMQ = null;

        try {
            hiveMQ = embeddedHiveMQBuilder.build();
            InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE);
            InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE);
            hiveMQ.start().join();
        } catch (final Exception ex) {
            ex.printStackTrace();
        }

        try {
            if (hiveMQ != null) {
                hiveMQ.close();
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }
}
