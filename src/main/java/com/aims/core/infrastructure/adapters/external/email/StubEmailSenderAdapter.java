package com.aims.core.infrastructure.adapters.external.email;

/**
 * Stub implementation of email sender adapter for testing/development.
 * This implementation just logs email details instead of actually sending emails.
 */
public class StubEmailSenderAdapter implements IEmailSenderAdapter {

    @Override
    public void sendEmail(String fromAddress, String toAddress, String subject, String body, boolean isHtml) throws Exception {
        System.out.println("=== STUB EMAIL SENDING ===");
        System.out.println("From: " + fromAddress);
        System.out.println("To: " + toAddress);
        System.out.println("Subject: " + subject);
        System.out.println("Content Type: " + (isHtml ? "HTML" : "Plain Text"));
        System.out.println("Body:");
        System.out.println(body);
        System.out.println("=========================");
    }

    @Override
    public void sendPlainTextEmail(String fromAddress, String toAddress, String subject, String plainTextBody) throws Exception {
        sendEmail(fromAddress, toAddress, subject, plainTextBody, false);
    }

    @Override
    public void sendHtmlEmail(String fromAddress, String toAddress, String subject, String htmlBody) throws Exception {
        sendEmail(fromAddress, toAddress, subject, htmlBody, true);
    }
}
