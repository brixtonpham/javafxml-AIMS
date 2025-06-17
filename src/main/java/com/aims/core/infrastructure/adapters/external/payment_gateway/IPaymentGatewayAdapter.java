package com.aims.core.infrastructure.adapters.external.payment_gateway;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.CardDetails;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Universal Payment Gateway Adapter Interface
 * 
 * This interface provides a gateway-agnostic contract for payment processing,
 * replacing the VNPay-specific IVNPayAdapter. It supports multiple payment
 * gateways while maintaining consistent business logic.
 * 
 * Key improvements over VNPay-specific implementation:
 * - Gateway-agnostic parameter names and methods
 * - Standardized response format across different gateways
 * - Enhanced error handling and validation
 * - Support for multiple payment gateway types
 */
public interface IPaymentGatewayAdapter {

    /**
     * Prepare payment parameters for the gateway
     * 
     * @param order The order to process payment for
     * @param paymentMethod The selected payment method
     * @param cardDetails Card details if applicable (optional for wallet/bank transfers)
     * @return Gateway-specific parameters prepared for payment processing
     * @throws ValidationException If input validation fails
     */
    Map<String, Object> preparePaymentParameters(OrderEntity order, PaymentMethod paymentMethod, CardDetails cardDetails) throws ValidationException;

    /**
     * Prepare refund parameters for the gateway
     * 
     * @param order The original order to refund
     * @param originalGatewayTransactionId Original transaction ID from the gateway
     * @param refundAmount Amount to refund
     * @param reason Reason for refund
     * @return Gateway-specific parameters prepared for refund processing
     * @throws ValidationException If input validation fails
     */
    Map<String, Object> prepareRefundParameters(OrderEntity order, String originalGatewayTransactionId, float refundAmount, String reason) throws ValidationException;

    /**
     * Process payment through the gateway
     * 
     * @param paymentParams Parameters prepared by preparePaymentParameters
     * @return Gateway response containing payment URL, transaction reference, or direct result
     * @throws PaymentException If payment processing fails
     */
    Map<String, String> processPayment(Map<String, Object> paymentParams) throws PaymentException;

    /**
     * Process refund through the gateway
     * 
     * @param refundParams Parameters prepared by prepareRefundParameters
     * @return Gateway response containing refund status and transaction details
     * @throws PaymentException If refund processing fails
     */
    Map<String, String> processRefund(Map<String, Object> refundParams) throws PaymentException;

    /**
     * Query transaction status from the gateway
     * 
     * @param gatewayTransactionRef Gateway's transaction reference
     * @param aimsOrderId Internal order ID for reference
     * @param originalTransactionDate Date of original transaction
     * @return Gateway response containing current transaction status
     * @throws PaymentException If status query fails
     */
    Map<String, String> queryTransactionStatus(String gatewayTransactionRef, String aimsOrderId, LocalDateTime originalTransactionDate) throws PaymentException;

    /**
     * Validate response signature from gateway callback
     * 
     * @param responseParams Parameters received from gateway callback/return
     * @return true if signature is valid, false otherwise
     */
    boolean validateResponseSignature(Map<String, String> responseParams);

    /**
     * Get the gateway type identifier
     * 
     * @return Gateway type (e.g., "VNPAY", "STRIPE", "PAYPAL", "MOMO")
     */
    String getGatewayType();

    /**
     * Get gateway-specific configuration status
     * 
     * @return true if gateway is properly configured and ready for use
     */
    boolean isConfigured();

    /**
     * Map gateway-specific response codes to standardized status codes
     * 
     * @param gatewayResponseCode Gateway's response code
     * @return Standardized status code (SUCCESS, FAILED, PENDING, CANCELLED)
     */
    String mapResponseCodeToStatus(String gatewayResponseCode);

    /**
     * Get gateway-specific field mappings for standardized processing
     * 
     * @return Map of standard field names to gateway-specific field names
     */
    Map<String, String> getFieldMappings();
}