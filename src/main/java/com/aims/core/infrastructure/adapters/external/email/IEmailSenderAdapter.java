package com.aims.core.infrastructure.adapters.external.email;

// You might want to create a custom exception for email sending failures
// import com.aims.common.custom_exceptions.EmailSendingException;

public interface IEmailSenderAdapter {

    /**
     * Sends an email.
     *
     * @param fromAddress The email address of the sender.
     * @param toAddress The email address of the recipient.
     * @param subject The subject of the email.
     * @param body The content of the email (can be plain text or HTML).
     * @param isHtml Whether the body content is HTML.
     * @throws Exception if sending the email fails for any reason (e.g., EmailSendingException).
     */
    void sendEmail(String fromAddress, String toAddress, String subject, String body, boolean isHtml) throws Exception;

    /**
     * Sends a plain text email.
     *
     * @param fromAddress The email address of the sender.
     * @param toAddress The email address of the recipient.
     * @param subject The subject of the email.
     * @param body The plain text content of the email.
     * @throws Exception if sending the email fails.
     */
    default void sendPlainTextEmail(String fromAddress, String toAddress, String subject, String body) throws Exception {
        sendEmail(fromAddress, toAddress, subject, body, false);
    }

    /**
     * Sends an HTML email.
     *
     * @param fromAddress The email address of the sender.
     * @param toAddress The email address of the recipient.
     * @param subject The subject of the email.
     * @param htmlBody The HTML content of the email.
     * @throws Exception if sending the email fails.
     */
    default void sendHtmlEmail(String fromAddress, String toAddress, String subject, String htmlBody) throws Exception {
        sendEmail(fromAddress, toAddress, subject, htmlBody, true);
    }
}