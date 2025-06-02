package com.aims.core.application.impl;

import com.aims.core.application.services.INotificationService;
import com.aims.core.entities.UserAccount;
import com.aims.core.infrastructure.adapters.external.email.IEmailSenderAdapter;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.Invoice;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.entities.OrderItem; // For email content

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificationServiceImpl implements INotificationService {

    private final IEmailSenderAdapter emailSenderAdapter;
    private static final String FROM_EMAIL = "noreply@aims.com"; // Configuration
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    public NotificationServiceImpl(IEmailSenderAdapter emailSenderAdapter) {
        this.emailSenderAdapter = emailSenderAdapter;
    }

    private String formatOrderItemsForEmail(OrderEntity orderEntity) {
        if (orderEntity == null || orderEntity.getOrderItems() == null || orderEntity.getOrderItems().isEmpty()) {
            return "No items in this order.\n";
        }
        StringBuilder itemsDetails = new StringBuilder();
        itemsDetails.append("Order Items:\n");
        itemsDetails.append("--------------------------------------------------\n");
        itemsDetails.append(String.format("%-30s | %-10s | %-15s | %-15s\n", "Product", "Quantity", "Unit Price", "Total Price"));
        itemsDetails.append("--------------------------------------------------\n");
        for (OrderItem item : orderEntity.getOrderItems()) {
            if (item.getProduct() != null) {
                itemsDetails.append(String.format("%-30s | %-10d | %,15.0f VND | %,15.0f VND\n",
                        item.getProduct().getTitle(),
                        item.getQuantity(),
                        item.getPriceAtTimeOfOrder(), // Price at time of order (excl. VAT)
                        item.getPriceAtTimeOfOrder() * item.getQuantity()));
            }
        }
        itemsDetails.append("--------------------------------------------------\n");
        return itemsDetails.toString();
    }


    @Override
    public void sendUserStatusChangeNotification(UserAccount userAccount, String oldStatus, String newStatus, String adminNotes) {
        if (userAccount == null || userAccount.getEmail() == null) {
            System.err.println("Notification Error: User account or email is null. Cannot send status change email.");
            return;
        }
        String subject = "AIMS Account Status Update";
        String body = String.format(
                "Dear %s,\n\n" +
                "Your AIMS account status has been updated.\n\n" +
                "Previous Status: %s\n" +
                "New Status: %s\n\n" +
                "Notes from Administrator: %s\n\n" +
                "If you have any questions, please contact support.\n\n" +
                "Regards,\nThe AIMS Team",
                userAccount.getUsername(),
                oldStatus,
                newStatus,
                (adminNotes != null && !adminNotes.isEmpty() ? adminNotes : "N/A")
        );
        try {
            emailSenderAdapter.sendEmail(FROM_EMAIL, userAccount.getEmail(), subject, body, false); // Assuming plain text email
            System.out.println("User status change notification sent to: " + userAccount.getEmail());
        } catch (Exception e) {
            // Log email sending failure
            System.err.println("Failed to send user status change email to " + userAccount.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    public void sendPasswordResetNotification(UserAccount userAccount, String temporaryPassword) {
        if (userAccount == null || userAccount.getEmail() == null) {
            System.err.println("Notification Error: User account or email is null. Cannot send password reset email.");
            return;
        }
        String subject = "AIMS Account Password Reset";
        // SECURITY WARNING: Sending plain text passwords in email is highly discouraged.
        // This is just an example. A better approach is a one-time reset link.
        String body = String.format(
                "Dear %s,\n\n" +
                "Your password for your AIMS account has been reset.\n\n" +
                "Your temporary password is: %s\n\n" +
                "Please log in using this temporary password and change it immediately.\n\n" +
                "If you did not request this change, please contact support immediately.\n\n" +
                "Regards,\nThe AIMS Team",
                userAccount.getUsername(),
                temporaryPassword
        );
        try {
            emailSenderAdapter.sendEmail(FROM_EMAIL, userAccount.getEmail(), subject, body, false); // Assuming plain text email
            System.out.println("Password reset notification sent to: " + userAccount.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send password reset email to " + userAccount.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    public void sendPasswordChangedNotification(UserAccount userAccount) {
        if (userAccount == null || userAccount.getEmail() == null) {
            System.err.println("Notification Error: User account or email is null. Cannot send password changed email.");
            return;
        }
        String subject = "AIMS Account Password Changed";
        String body = String.format(
                "Dear %s,\n\n" +
                "This email confirms that the password for your AIMS account was successfully changed on %s.\n\n" +
                "If you did not make this change, please contact AIMS support immediately.\n\n" +
                "Regards,\nThe AIMS Team",
                userAccount.getUsername(),
                LocalDateTime.now().format(DATE_TIME_FORMATTER)
        );
        try {
            emailSenderAdapter.sendEmail(FROM_EMAIL, userAccount.getEmail(), subject, body, false); // Assuming plain text email
            System.out.println("Password changed notification sent to: " + userAccount.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send password changed email to " + userAccount.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    public void sendOrderConfirmationEmail(OrderEntity orderEntity, Invoice invoice, PaymentTransaction paymentTransaction) {
        if (orderEntity == null || orderEntity.getDeliveryInfo() == null || orderEntity.getDeliveryInfo().getEmail() == null || invoice == null || paymentTransaction == null) {
            System.err.println("Notification Error: Missing required information for order confirmation email. OrderID: " + (orderEntity != null ? orderEntity.getOrderId() : "N/A"));
            return;
        }

        String customerEmail = orderEntity.getDeliveryInfo().getEmail();
        String customerName = orderEntity.getDeliveryInfo().getRecipientName();
        String subject = "AIMS Order Confirmation - #" + orderEntity.getOrderId();

        String itemsDetails = formatOrderItemsForEmail(orderEntity);

        String body = String.format(
                "Dear %s,\n\n" +
                "Thank you for your order with AIMS!\n\n" +
                "Order ID: %s\n" +
                "Order Date: %s\n" +
                "Order Status: %s\n\n" +
                "%s\n" + // Order items here
                "Subtotal (excl. VAT):      %,.0f VND\n" +
                "VAT (10%%):                 %,.0f VND\n" +
                "Total Product Price (incl. VAT): %,.0f VND\n" +
                "Delivery Fee:            %,.0f VND\n" +
                "--------------------------------------------------\n" +
                "TOTAL AMOUNT PAID:         %,.0f VND\n" +
                "--------------------------------------------------\n\n" +
                "Shipping Address:\n%s\n%s, %s\n\n" +
                "Payment Transaction ID: %s\n" +
                "Payment Transaction Status: %s\n" +
                "Payment Date: %s\n\n" +
                "Invoice ID: %s\n" +
                "Invoice Date: %s\n\n" +
                "We will notify you once your order has been shipped. You can view your order details [link_to_order_details].\n\n" + // Placeholder for actual link
                "Regards,\nThe AIMS Team",
                customerName,
                orderEntity.getOrderId(),
                orderEntity.getOrderDate().format(DATE_TIME_FORMATTER),
                orderEntity.getOrderStatus().toString(), // More user-friendly status might be needed
                itemsDetails,
                orderEntity.getTotalProductPriceExclVAT(),
                orderEntity.getTotalProductPriceInclVAT() - orderEntity.getTotalProductPriceExclVAT(), // Calculated VAT
                orderEntity.getTotalProductPriceInclVAT(),
                orderEntity.getCalculatedDeliveryFee(),
                orderEntity.getTotalAmountPaid(),
                orderEntity.getDeliveryInfo().getRecipientName(),
                orderEntity.getDeliveryInfo().getDeliveryAddress(),
                orderEntity.getDeliveryInfo().getDeliveryProvinceCity(),
                paymentTransaction.getExternalTransactionId() != null ? paymentTransaction.getExternalTransactionId() : paymentTransaction.getTransactionId(),
                paymentTransaction.getTransactionStatus(),
                paymentTransaction.getTransactionDateTime().format(DATE_TIME_FORMATTER),
                invoice.getInvoiceId(),
                invoice.getInvoiceDate().format(DATE_TIME_FORMATTER)
        );

        try {
            emailSenderAdapter.sendEmail(FROM_EMAIL, customerEmail, subject, body, false); // Assuming plain text email
            System.out.println("Order confirmation email sent to: " + customerEmail + " for order: " + orderEntity.getOrderId());
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email for order " + orderEntity.getOrderId() + ": " + e.getMessage());
        }
    }

    @Override
    public void sendOrderCancellationNotification(OrderEntity orderEntity, PaymentTransaction refundTransaction) {
        if (orderEntity == null || orderEntity.getDeliveryInfo() == null || orderEntity.getDeliveryInfo().getEmail() == null) {
            System.err.println("Notification Error: Missing user/email for order cancellation. OrderID: " + (orderEntity != null ? orderEntity.getOrderId() : "N/A"));
            return;
        }
        String customerEmail = orderEntity.getDeliveryInfo().getEmail();
        String customerName = orderEntity.getDeliveryInfo().getRecipientName();
        String subject = "AIMS Order Cancellation Notice - #" + orderEntity.getOrderId();

        String refundDetails = "";
        if (refundTransaction != null) {
            refundDetails = String.format(
                    "A refund of %,.0f VND has been processed.\n" +
                    "Refund Transaction ID: %s\n" +
                    "Refund Status: %s\n\n",
                    refundTransaction.getAmount(),
                    refundTransaction.getExternalTransactionId() != null ? refundTransaction.getExternalTransactionId() : refundTransaction.getTransactionId(),
                    refundTransaction.getTransactionStatus()
            );
        }

        String body = String.format(
                "Dear %s,\n\n" +
                "This email confirms that your AIMS Order #%s has been cancelled as per your request or due to other reasons.\n\n" +
                "%s" + // Refund details here
                "If you have any questions or did not request this cancellation, please contact AIMS support immediately.\n\n" +
                "Regards,\nThe AIMS Team",
                customerName,
                orderEntity.getOrderId(),
                refundDetails
        );
        try {
            emailSenderAdapter.sendEmail(FROM_EMAIL, customerEmail, subject, body, false); // Assuming plain text email
            System.out.println("Order cancellation notification sent to: " + customerEmail + " for order: " + orderEntity.getOrderId());
        } catch (Exception e) {
            System.err.println("Failed to send order cancellation email for order " + orderEntity.getOrderId() + ": " + e.getMessage());
        }
    }

    @Override
    public void sendOrderStatusUpdateNotification(OrderEntity orderEntity, String oldStatus, String newStatus, String notes) {
         if (orderEntity == null || orderEntity.getDeliveryInfo() == null || orderEntity.getDeliveryInfo().getEmail() == null) {
            System.err.println("Notification Error: Missing user/email for order status update. OrderID: " + (orderEntity != null ? orderEntity.getOrderId() : "N/A"));
            return;
        }
        String customerEmail = orderEntity.getDeliveryInfo().getEmail();
        String customerName = orderEntity.getDeliveryInfo().getRecipientName();
        String subject = "AIMS Order Status Update - #" + orderEntity.getOrderId();

        String body = String.format(
                "Dear %s,\n\n" +
                "The status of your AIMS Order #%s has been updated.\n\n" +
                "Previous Status: %s\n" +
                "New Status: %s\n\n" +
                (notes != null && !notes.trim().isEmpty() ? "Notes: " + notes + "\n\n" : "") +
                "You can view your order details [link_to_order_details].\n\n" + // Placeholder
                "Regards,\nThe AIMS Team",
                customerName,
                orderEntity.getOrderId(),
                oldStatus,
                newStatus
        );
        try {
            emailSenderAdapter.sendEmail(FROM_EMAIL, customerEmail, subject, body, false); // Assuming plain text email
            System.out.println("Order status update notification sent to: " + customerEmail + " for order: " + orderEntity.getOrderId());
        } catch (Exception e) {
            System.err.println("Failed to send order status update email for order " + orderEntity.getOrderId() + ": " + e.getMessage());
        }
    }
}