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
package com.airepublic.bmstoinverter.service.email;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.service.IEmailService;

public class EmailService implements IEmailService {
    private final static Logger LOG = LoggerFactory.getLogger(EmailService.class);
    private final boolean enabled;
    private final String host;
    private final int port;
    private final String type;
    private final String username;
    private final String password;
    private final boolean sslEnable;
    private final boolean tlsEnable;
    private final boolean debug;
    private final String senderEmail;
    private final List<String> recipients;

    public EmailService() {
        enabled = Boolean.parseBoolean(getEnv("mail.service.enabled", "true"));
        debug = Boolean.parseBoolean(getEnv("mail.out.debug", "false"));
        host = getEnv("mail.out.host");
        port = Integer.parseInt(getEnv("mail.out.port", "465"));
        type = getEnv("mail.out.type", "smtps");
        username = getEnv("mail.out.username");
        password = getEnv("mail.out.password");
        sslEnable = Boolean.parseBoolean(getEnv("mail.out.sslEnable", "true"));
        tlsEnable = Boolean.parseBoolean(getEnv("mail.out.tlsEnable", "false"));
        senderEmail = getEnv("mail.out.defaultEmail");

        final String recipientConfig = getEnv("mail.out.recipients");

        recipients = Arrays.asList(recipientConfig.split("\\s*,\\s*"));
    }


    @Override
    public void sendEmail(final String subject, final String message) {
        if (!enabled) {
            LOG.info("Email service disabled.");
            return;
        }

        try {
            final Properties props = new Properties();
            props.put("mail.transport.protocol", type);
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", String.valueOf(sslEnable));
            props.put("mail.smtp.starttls.enable", String.valueOf(tlsEnable));

            final Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            username,
                            password);
                }
            });

            session.setDebug(debug);

            final MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(senderEmail));

            for (final String recipient : recipients) {
                mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }

            mimeMessage.setSubject(subject);
            mimeMessage.setText(message);

            Transport.send(mimeMessage);

            LOG.info("Email sent successfully.");
        } catch (final Exception e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }


    private String getEnv(final String key) {
        final String value = System.getenv(key);

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }

        return value;
    }


    private String getEnv(final String key, final String defaultValue) {
        final String value = System.getenv(key);

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }
}