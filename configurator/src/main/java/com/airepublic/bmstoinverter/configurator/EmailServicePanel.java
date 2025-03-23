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
import java.util.Properties;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailServicePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField outgoingServerField;
    private final JTextField outgoingServerPortField;
    private final JComboBox<String> sslTlsField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JTextField senderField;
    private final JTextField recipientsField;
    private final NumberInputVerifier numberInputVerifier = new NumberInputVerifier();

    public EmailServicePanel() {
        setBorder(null);
        final GridBagLayout gbl_emailPanel = new GridBagLayout();
        gbl_emailPanel.columnWidths = new int[] { 100, 180, 60, 60, 60, 60 };
        gbl_emailPanel.rowHeights = new int[] { 30, 30, 30, 30 };
        gbl_emailPanel.columnWeights = new double[] { 0.0, 1.0, 1.0, 1.0, 0.0, 1.0 };
        gbl_emailPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
        setLayout(gbl_emailPanel);

        final JLabel outgoingServerLabel = new JLabel("Outgoing server");
        final GridBagConstraints gbc_outgoingServerLabel = new GridBagConstraints();
        gbc_outgoingServerLabel.anchor = GridBagConstraints.EAST;
        gbc_outgoingServerLabel.insets = new Insets(0, 0, 5, 5);
        gbc_outgoingServerLabel.gridx = 0;
        gbc_outgoingServerLabel.gridy = 0;
        add(outgoingServerLabel, gbc_outgoingServerLabel);

        outgoingServerField = new JTextField();
        outgoingServerField.setColumns(10);
        final GridBagConstraints gbc_outgoingServerField = new GridBagConstraints();
        gbc_outgoingServerField.fill = GridBagConstraints.BOTH;
        gbc_outgoingServerField.insets = new Insets(0, 0, 5, 5);
        gbc_outgoingServerField.gridx = 1;
        gbc_outgoingServerField.gridy = 0;
        add(outgoingServerField, gbc_outgoingServerField);

        final JLabel outgoingServerPortLabel = new JLabel("Port");
        final GridBagConstraints gbc_outgoingServerPortLabel = new GridBagConstraints();
        gbc_outgoingServerPortLabel.anchor = GridBagConstraints.EAST;
        gbc_outgoingServerPortLabel.insets = new Insets(0, 0, 5, 5);
        gbc_outgoingServerPortLabel.gridx = 2;
        gbc_outgoingServerPortLabel.gridy = 0;
        add(outgoingServerPortLabel, gbc_outgoingServerPortLabel);

        outgoingServerPortField = new JTextField();
        outgoingServerPortField.setColumns(10);
        final GridBagConstraints gbc_outgoingPortField = new GridBagConstraints();
        gbc_outgoingPortField.fill = GridBagConstraints.BOTH;
        gbc_outgoingPortField.insets = new Insets(0, 0, 5, 5);
        gbc_outgoingPortField.gridx = 3;
        gbc_outgoingPortField.gridy = 0;
        add(outgoingServerPortField, gbc_outgoingPortField);
        outgoingServerPortField.setInputVerifier(numberInputVerifier);

        final JLabel sslTlsLabel = new JLabel("SSL/TLS");
        final GridBagConstraints gbc_sslTlsLabel = new GridBagConstraints();
        gbc_sslTlsLabel.anchor = GridBagConstraints.EAST;
        gbc_sslTlsLabel.insets = new Insets(0, 0, 5, 5);
        gbc_sslTlsLabel.gridx = 4;
        gbc_sslTlsLabel.gridy = 0;
        add(sslTlsLabel, gbc_sslTlsLabel);

        sslTlsField = new JComboBox<>(new DefaultComboBoxModel<>(createSslTlsItems()));
        final GridBagConstraints gbc_sslTlsField = new GridBagConstraints();
        gbc_sslTlsField.fill = GridBagConstraints.BOTH;
        gbc_sslTlsField.insets = new Insets(0, 0, 5, 0);
        gbc_sslTlsField.gridx = 5;
        gbc_sslTlsField.gridy = 0;
        add(sslTlsField, gbc_sslTlsField);

        final JLabel usernameLabel = new JLabel("Username");
        final GridBagConstraints gbc_usernameLabel = new GridBagConstraints();
        gbc_usernameLabel.anchor = GridBagConstraints.EAST;
        gbc_usernameLabel.insets = new Insets(0, 0, 5, 5);
        gbc_usernameLabel.gridx = 0;
        gbc_usernameLabel.gridy = 1;
        add(usernameLabel, gbc_usernameLabel);

        usernameField = new JTextField();
        usernameField.setColumns(10);
        final GridBagConstraints gbc_usernameField = new GridBagConstraints();
        gbc_usernameField.fill = GridBagConstraints.BOTH;
        gbc_usernameField.insets = new Insets(0, 0, 5, 5);
        gbc_usernameField.gridx = 1;
        gbc_usernameField.gridy = 1;
        add(usernameField, gbc_usernameField);

        final JLabel passwordLabel = new JLabel("Password");
        final GridBagConstraints gbc_passwordLabel = new GridBagConstraints();
        gbc_passwordLabel.anchor = GridBagConstraints.EAST;
        gbc_passwordLabel.insets = new Insets(0, 0, 5, 5);
        gbc_passwordLabel.gridx = 2;
        gbc_passwordLabel.gridy = 1;
        add(passwordLabel, gbc_passwordLabel);

        passwordField = new JPasswordField();
        final GridBagConstraints gbc_passwordField = new GridBagConstraints();
        gbc_passwordField.gridwidth = 3;
        gbc_passwordField.fill = GridBagConstraints.BOTH;
        gbc_passwordField.insets = new Insets(0, 0, 5, 0);
        gbc_passwordField.gridx = 3;
        gbc_passwordField.gridy = 1;
        add(passwordField, gbc_passwordField);

        final JLabel senderLabel = new JLabel("Sender");
        final GridBagConstraints gbc_senderLabel = new GridBagConstraints();
        gbc_senderLabel.anchor = GridBagConstraints.EAST;
        gbc_senderLabel.insets = new Insets(0, 0, 5, 5);
        gbc_senderLabel.gridx = 0;
        gbc_senderLabel.gridy = 2;
        add(senderLabel, gbc_senderLabel);

        senderField = new JTextField();
        senderField.setColumns(10);
        final GridBagConstraints gbc_senderField = new GridBagConstraints();
        gbc_senderField.fill = GridBagConstraints.BOTH;
        gbc_senderField.insets = new Insets(0, 0, 5, 5);
        gbc_senderField.gridx = 1;
        gbc_senderField.gridy = 2;
        add(senderField, gbc_senderField);

        final JButton testButton = new JButton("Test");
        final GridBagConstraints gbc_testButton = new GridBagConstraints();
        gbc_testButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_testButton.insets = new Insets(0, 0, 5, 5);
        gbc_testButton.gridx = 3;
        gbc_testButton.gridy = 2;
        add(testButton, gbc_testButton);
        testButton.addActionListener(e -> testEmailConfiguration());

        final JLabel lblNewLabel = new JLabel("Recipients");
        final GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
        gbc_lblNewLabel.insets = new Insets(0, 0, 0, 5);
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 3;
        add(lblNewLabel, gbc_lblNewLabel);

        recipientsField = new JTextField();
        recipientsField.setToolTipText("Recipient email addresses seperated by comma");
        recipientsField.setColumns(10);
        final GridBagConstraints gbc_recipientField = new GridBagConstraints();
        gbc_recipientField.fill = GridBagConstraints.BOTH;
        gbc_recipientField.gridwidth = 5;
        gbc_recipientField.gridx = 1;
        gbc_recipientField.gridy = 3;
        add(recipientsField, gbc_recipientField);

        outgoingServerField.addActionListener(e -> outgoingServerPortField.requestFocus());
        outgoingServerPortField.addActionListener(e -> sslTlsField.requestFocus());
        sslTlsField.addActionListener(e -> usernameField.requestFocus());
        usernameField.addActionListener(e -> passwordField.requestFocus());
        passwordField.addActionListener(e -> senderField.requestFocus());
        senderField.addActionListener(e -> recipientsField.requestFocus());

    }


    private Object testEmailConfiguration() {
        final StringBuffer errors = new StringBuffer();
        if (!verify(errors)) {
            JOptionPane.showMessageDialog(this, errors, "Error", JOptionPane.ERROR_MESSAGE);
            System.out.println(errors);
            return false;
        }

        final Properties properties = new Properties();
        properties.put("mail.smtp.host", getOutgoingServer());
        properties.put("mail.smtp.port", "" + getOutgoingServerPort());
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", getSslTls().equals("TLS"));
        properties.put("mail.smtp.ssl.enable", getSslTls().equals("SSL"));

        final Session session = Session.getInstance(properties, new jakarta.mail.Authenticator() {
            @Override
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(getUsername(), getPassword());
            }
        });

        try {
            final Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(getSender()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(getRecipients()));
            message.setSubject("BMS-to-Inverter Configurator Test");
            message.setText("This is a test email from the BMS-to-Inverter Configurator.");

            Transport.send(message);
            JOptionPane.showMessageDialog(this, "Email test successful!", "Information", JOptionPane.INFORMATION_MESSAGE);
        } catch (final MessagingException e) {
            JOptionPane.showMessageDialog(this, "Email test failed! " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }


    private Vector<String> createSslTlsItems() {
        final Vector<String> items = new Vector<>();
        items.add("SSL");
        items.add("TLS");
        return items;
    }


    public String getOutgoingServer() {
        return outgoingServerField.getText();
    }


    public int getOutgoingServerPort() {
        return Integer.valueOf(outgoingServerPortField.getText());
    }


    public String getSslTls() {
        return (String) sslTlsField.getSelectedItem();
    }


    public String getUsername() {
        return usernameField.getText();
    }


    public String getPassword() {
        return String.valueOf(passwordField.getPassword());
    }


    public String getSender() {
        return senderField.getText();
    }


    public String getRecipients() {
        return recipientsField.getText();
    }


    public boolean verify(final StringBuffer errors) {
        boolean fail = false;

        if (outgoingServerField.getText().trim().isEmpty()) {
            errors.append("Missing outgoing server!\n");
            fail = true;
        }

        if (outgoingServerPortField.getText().trim().isEmpty()) {
            errors.append("Missing email server port!\n");
        } else if (!numberInputVerifier.verify(outgoingServerPortField.getText())) {
            errors.append("Non-numeric email server port!\n");
        }

        if (sslTlsField.getSelectedIndex() == -1) {
            errors.append("Missing email server SSL/TLS!\n");
        }
        if (usernameField.getText().trim().isEmpty()) {
            errors.append("Missing email server username!\n");
        }
        if (passwordField.getPassword().length == 0) {
            errors.append("Missing email server password!\n");
        }
        if (senderField.getText().trim().isEmpty()) {
            errors.append("Missing email sender address!\n");
        }
        if (recipientsField.getText().trim().isEmpty()) {
            errors.append("Missing email recipient addresses!\n");
        }

        return !fail;
    }


    protected void generateConfiguration(final StringBuffer config) {
        config.append("#### Email properties ####\n");
        config.append("mail.service.enabled=true\n");
        config.append("mail.out.debug=true\n");
        config.append("# SMTP or IMAP address of the outgoing server\n");
        config.append("mail.out.host=" + getOutgoingServer() + "\n");
        config.append("# The port of the outgoing server\n");
        config.append("mail.out.port=" + getOutgoingServerPort() + "\n");
        config.append("# smtp for TLS, smtps for SSL\n");
        config.append("mail.out.type=" + (getSslTls().equals("SSL") ? "smtps" : "smtp") + "\n");
        config.append("# User name to authenticate at the outgoing server\n");
        config.append("mail.out.username=" + getUsername() + "\n");
        config.append("# Password to authenticate at the outgoing server\n");
        config.append("mail.out.password=" + getPassword() + "\n");
        config.append("# Disable if using TLS\n");
        config.append(" mail.out.sslEnable=" + (getSslTls().equals("SSL") ? "true" : "false") + "\n");
        config.append("# Disable if using SSL\n");
        config.append("mail.out.tlsEnable=" + (getSslTls().equals("TLS") ? "true" : "false") + "\n");
        config.append("# The email address to use when sending emails\n");
        config.append("mail.out.defaultEmail=" + getSender() + "\n");
        config.append("# A (comma separated) list of pre-configured email recipients\n");
        config.append("mail.out.recipients=" + getRecipients() + "\n");
    }


    void setConfiguration(final Properties config) {
        if (config.containsKey("mail.service.enabled")) {
            outgoingServerField.setText(config.getProperty("mail.out.host"));
            outgoingServerPortField.setText(config.getProperty("mail.out.port"));
            sslTlsField.setSelectedItem(config.getProperty("mail.out.type").equals("smtps") ? "SSL" : "TLS");
            usernameField.setText(config.getProperty("mail.out.username"));
            passwordField.setText(config.getProperty("mail.out.password"));
            senderField.setText(config.getProperty("mail.out.defaultEmail"));
            recipientsField.setText(config.getProperty("mail.out.recipients"));
        }
    }
}
