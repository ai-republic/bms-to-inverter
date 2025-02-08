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
package com.airepublic.bmstoinverter.service.mqtt;

import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.airepublic.bmstoinverter.core.service.IMQTTProducerService;

/**
 * Wrapper to choose between multiple client types based on the port number.  Artemis (default port=61616) or MQTT (default port=1833, highest default is 16000)
 */
public class MQTTProducerServiceWrapper implements IMQTTProducerService {
    private IMQTTProducerService impl = null;
    private final static Logger LOG = LoggerFactory.getLogger(MQTTProducerServiceWrapper.class);
public MQTTProducerServiceWrapper() {

    }

    @Override
    public MQTTProducerServiceWrapper connect(final String locator, final String address, final String username, final String password) throws IOException {
        boolean isMqtt = false;
        try {
            URI uri = new URI(locator);
            int port = uri.getPort();
            isMqtt = (port<20000);
        } catch (Exception e) {
            LOG.error("Failed to parse locator string.  Will use default Artemis client.", e);
        }

        impl = (isMqtt)?new MQTTHAProducerService():new MQTTProducerService() {
            
        };
        try {
            impl.connect(locator, address, username, password);
        } catch (Exception e) {
            throw new IOException(e);
        }
        return this;
    }


    @Override
    public boolean isRunning() {
        return impl.isRunning();
    }


    @Override
    public void sendMessage(final String content) throws IOException {
        try {
            impl.sendMessage(content);
        } catch (final Exception e) {
            throw new IOException("Could not send MQTT message on topic " + content, e);
        }
    }

    @Override
    public void stop() {
        try {
            impl.stop();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to stop MQTT producer!", e);
        }
    }

    @Override
    public void close() throws Exception {
        stop();
    }



}
