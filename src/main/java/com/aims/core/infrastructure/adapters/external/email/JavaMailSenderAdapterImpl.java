package com.aims.core.infrastructure.adapters.external.email;

// import com.aims.common.custom_exceptions.EmailSendingException; // If you create this
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.util.Properties;

public class JavaMailSenderAdapterImpl implements IEmailSenderAdapter {

    private static final String CONFIG_FILE = "email_config.properties";
    private String smtpHost;
    private int smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private boolean authEnabled;
    private boolean startTlsEnabled;

    public JavaMailSenderAdapterImpl() {
        loadConfig();
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = JavaMailSenderAdapterImpl.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("CRITICAL: Unable to find " + CONFIG_FILE + ". Email sending will likely fail.");
                // Set some defaults or throw an error
                this.smtpHost = "localhost"; // Fallback
                this.smtpPort = 25; // Fallback
                this.authEnabled = false;
                return;
            }
            props.load(input);
            this.smtpHost = props.getProperty("mail.smtp.host");
            this.smtpPort = Integer.parseInt(props.getProperty("mail.smtp.port", "25"));
            this.authEnabled = Boolean.parseBoolean(props.getProperty("mail.smtp.auth", "false"));
            this.smtpUsername = props.getProperty("mail.smtp.username");
            this.smtpPassword = props.getProperty("mail.smtp.password");
            this.startTlsEnabled = Boolean.parseBoolean(props.getProperty("mail.smtp.starttls.enable", "false"));

            if (this.smtpHost == null) {
                 System.err.println("CRITICAL: mail.smtp.host is not configured in " + CONFIG_FILE);
            }
            if (this.authEnabled && (this.smtpUsername == null || this.smtpPassword == null)) {
                System.err.println("CRITICAL: SMTP auth is enabled but username or password is not configured in " + CONFIG_FILE);
            }

        } catch (Exception e) {
            System.err.println("Error loading email configuration from " + CONFIG_FILE + ": " + e.getMessage());
            // Set safe defaults in case of error
            this.smtpHost = "localhost";
            this.smtpPort = 25;
            this.authEnabled = false;
        }
    }

    @Override
    public void sendEmail(String fromAddress, String toAddress, String subject, String body, boolean isHtml) throws Exception {
        if (smtpHost == null) {
            throw new Exception("SMTP host not configured. Cannot send email.");
            // throw new EmailSendingException("SMTP host not configured. Cannot send email.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));

        if (authEnabled) {
            props.put("mail.smtp.auth", "true");
        }
        if (startTlsEnabled) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        // Additional properties like SSL/TLS might be needed depending on your SMTP server
        // props.put("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
        // props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // For SSL


        Session session;
        if (authEnabled && smtpUsername != null && smtpPassword != null) {
            session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        // session.setDebug(true); // Enable for debugging SMTP communication

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
            message.setSubject(subject);

            if (isHtml) {
                message.setContent(body, "text/html; charset=utf-8");
            } else {
                message.setText(body);
            }

            Transport.send(message);
            System.out.println("Email sent successfully from " + fromAddress + " to " + toAddress + " with subject: " + subject);

        } catch (MessagingException e) {
            System.err.println("Failed to send email. From: " + fromAddress + ", To: " + toAddress + ", Subject: " + subject);
            e.printStackTrace();
            throw new Exception("Failed to send email: " + e.getMessage(), e);
            // throw new EmailSendingException("Failed to send email: " + e.getMessage(), e);
        }
    }
}