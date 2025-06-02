package com.aims.core.application.services.strategies;

import com.aims.core.entities.OrderEntity;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.enums.PaymentMethodType; // For strategy identification

import java.util.Map;

/**
 * Interface defining the strategy for processing a payment through a specific payment method or gateway.
 * Each concrete strategy will handle the specifics of one payment type (e.g., Credit Card via VNPay, Domestic Card via VNPay).
 */
public interface IPaymentStrategy {

    /**
     * Processes a payment for the given order using this specific payment strategy.
     *
     * @param order The order for which payment is being processed.
     * @param paymentParams A map of parameters required by this specific payment strategy.
     * This could include client IP, bank codes for domestic cards, etc.
     * The IVNPayAdapter will be called with more detailed, structured parameters.
     * @return A result map, typically containing details from the payment gateway
     * (e.g., payment URL to redirect to, transaction reference, status codes).
     * @throws PaymentException If the payment processing fails at the gateway or due to configuration issues specific to this strategy.
     * @throws ValidationException If the provided paymentParams are invalid for this strategy.
     * @throws ResourceNotFoundException If essential resources for this strategy (e.g., specific configurations) are not found.
     */
    Map<String, String> processPayment(OrderEntity order, Map<String, Object> paymentParams)
            throws PaymentException, ValidationException, ResourceNotFoundException;

    /**
     * Processes a refund for a given original transaction using this specific payment strategy.
     *
     * @param originalGatewayTransactionId The transaction ID from the payment gateway for the payment to be refunded.
     * @param order The order associated with the refund.
     * @param refundAmount The amount to refund.
     * @param reason The reason for the refund.
     * @return A result map, typically containing details of the refund transaction from the gateway.
     * @throws PaymentException If the refund processing fails at the gateway.
     * @throws ValidationException If the refund parameters are invalid for this strategy.
     * @throws ResourceNotFoundException If the original transaction to refund is not found by the gateway or related resources are missing.
     */
    Map<String, String> processRefund(String originalGatewayTransactionId, OrderEntity order, float refundAmount, String reason)
            throws PaymentException, ValidationException, ResourceNotFoundException;

    /**
     * Optional: Gets the specific type of payment method this strategy handles.
     * This can be used by a factory or the PaymentService to select the appropriate strategy.
     *
     * @return The PaymentMethodType enum value this strategy corresponds to.
     */
    PaymentMethodType getStrategyType();
}