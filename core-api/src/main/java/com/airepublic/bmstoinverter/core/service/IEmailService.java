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
package com.airepublic.bmstoinverter.core.service;

/**
 * Service interface for the email service. It will read the config.properties for the email
 * configuration and send emails with the configured settings.
 */
public interface IEmailService {

    /**
     * Sends an email with the specified message.
     *
     * @param subject the subject of the email
     * @param message the message to send
     */
    void sendEmail(String subject, String message);

}