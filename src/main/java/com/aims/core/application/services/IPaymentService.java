package com.aims.core.application.services;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentTransaction;
// import com.aims.core.dtos.PaymentRequestDTO; // DTO to hold details needed for VNPay
// import com.aims.core.dtos.RefundRequestDTO; // DTO for refund details
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import java.sql.SQLException;

/**
 * Service interface for handling payment processing and refunds,
 * primarily interacting with a payment gateway like VNPay.
 */
public interface IPaymentService {

    /**
     * Processes a payment for a given order using the specified payment method.
     * This method will interact with the VNPay gateway adapter.
     * It records the transaction details regardless of success or failure.
     *
     * @param order The OrderEntity for which the payment is being made.
     * @param paymentMethodId The ID of the payment method to use (e.g., a saved card).
     * @param cardDetails DTO or entity containing card details if not already saved or for one-time use (specific to VNPay requirements).
     * This might include card number, expiry, CVV for credit cards, or specific details for domestic cards.
     * @return PaymentTransaction object containing the outcome and details of the payment attempt.
     * @throws SQLException If a database error occurs while recording the transaction.
     * @throws PaymentException If the payment gateway returns an error or payment fails.
     * @throws ValidationException If payment information is invalid.
     * @throws ResourceNotFoundException If the order or payment method is not found.
     */
    PaymentTransaction processPayment(OrderEntity order, String paymentMethodId /*, CardDetailsDTO cardDetails */) throws SQLException, PaymentException, ValidationException, ResourceNotFoundException;

    /**
     * Processes a refund for a previously completed payment transaction, typically for a cancelled or returned order.
     * Interacts with the VNPay gateway adapter.
     *
     * @param orderId The ID of the order for which the refund is requested.
     * @param originalTransactionId The ID of the original successful payment transaction to be refunded.
     * @param refundAmount The amount to be refunded.
     * @param reason The reason for the refund.
     * @return PaymentTransaction object representing the refund transaction.
     * @throws SQLException If a database error occurs while recording the refund transaction.
     * @throws PaymentException If the refund processing via the gateway fails.
     * @throws ValidationException If refund details are invalid or the original transaction cannot be refunded.
     * @throws ResourceNotFoundException If the original transaction or order is not found.
     */
    PaymentTransaction processRefund(String orderId, String originalTransactionId, float refundAmount, String reason) throws SQLException, PaymentException, ValidationException, ResourceNotFoundException;

    /**
     * Retrieves the status of a payment transaction from the payment gateway.
     * This can be used to check on pending transactions or verify completed ones.
     *
     * @param transactionId The internal ID of the payment transaction.
     * @param externalTransactionId The transaction ID from the payment gateway (e.g., VNPay).
     * @return An updated PaymentTransaction object with the latest status from the gateway.
     * @throws PaymentException If there's an error communicating with the gateway.
     * @throws SQLException If there's an error updating the local transaction record.
     * @throws ResourceNotFoundException If the local transaction record is not found.
     * @throws ValidationException If the transaction ID or other parameters are invalid.
     */
    PaymentTransaction checkPaymentStatus(String transactionId, String externalTransactionId) throws PaymentException, SQLException, ResourceNotFoundException, ValidationException;
}