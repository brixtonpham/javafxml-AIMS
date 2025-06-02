package com.aims.core.application.services;

import com.aims.core.entities.UserAccount;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.Invoice;
import com.aims.core.entities.PaymentTransaction;
// import com.aims.core.dtos.EmailDetailsDTO; // Could be a DTO to carry email content, recipients etc.

// No direct SQLException expected if this service only orchestrates email sending
// and doesn't interact with the database directly for email content generation beyond passed entities.

/**
 * Service interface for handling various notifications, primarily emails,
 * to users regarding account activities and order confirmations.
 */
public interface INotificationService {

    /**
     * Sends a notification to a user when their account status changes (e.g., blocked, unblocked).
     *
     * @param userAccount The UserAccount whose status has changed.
     * @param oldStatus The previous status of the user account.
     * @param newStatus The new status of the user account.
     * @param adminNotes Optional notes from the administrator regarding the change.
     */
    void sendUserStatusChangeNotification(UserAccount userAccount, String oldStatus, String newStatus, String adminNotes);

    /**
     * Sends a password reset notification to the user.
     * This might contain a temporary password or a link to reset the password.
     *
     * @param userAccount The UserAccount for whom the password was reset.
     * @param temporaryPassword The new temporary password (if applicable).
     */
    void sendPasswordResetNotification(UserAccount userAccount, String temporaryPassword);
    
    /**
     * Sends a notification confirming a successful password change by the user.
     *
     * @param userAccount The UserAccount whose password was changed.
     */
    void sendPasswordChangedNotification(UserAccount userAccount);

    /**
     * Sends an order confirmation email to the customer after a successful order placement and payment.
     * This email should include invoice details and payment transaction information.
     *
     * @param orderEntity The successfully placed OrderEntity.
     * @param invoice The Invoice associated with the order.
     * @param paymentTransaction The successful PaymentTransaction details.
     */
    void sendOrderConfirmationEmail(OrderEntity orderEntity, Invoice invoice, PaymentTransaction paymentTransaction);

    /**
     * Sends a notification to the customer when their order has been cancelled.
     *
     * @param orderEntity The OrderEntity that has been cancelled.
     * @param refundTransaction Optional: The PaymentTransaction details if a refund was processed.
     */
    void sendOrderCancellationNotification(OrderEntity orderEntity, PaymentTransaction refundTransaction);
    
    /**
     * Sends a notification to the customer when their order status has been updated
     * (e.g., approved, rejected, shipped, delivered).
     *
     * @param orderEntity The OrderEntity whose status has changed.
     * @param oldStatus The previous status of the order.
     * @param newStatus The new status of the order.
     * @param notes Optional notes regarding the status change (e.g., rejection reason, tracking number).
     */
    void sendOrderStatusUpdateNotification(OrderEntity orderEntity, String oldStatus, String newStatus, String notes);

}