package com.aims.core.application.services;

import java.util.Map;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

public interface IPaymentService {
    /**
     * Process a payment for an order using the specified payment method
     */
    PaymentTransaction processPayment(OrderEntity order, String paymentMethodId) 
        throws PaymentException, ValidationException, ResourceNotFoundException;
    
    /**
     * Process a payment with additional parameters (like gateway-specific data)
     */
    PaymentTransaction processPaymentWithParams(OrderEntity order, String paymentMethodId, Map<String, Object> additionalParams) 
        throws PaymentException, ValidationException, ResourceNotFoundException;
    
    /**
     * Process a refund for a transaction
     */
    PaymentTransaction processRefund(String orderIdToRefund, String originalGatewayTransactionId, float refundAmount, String reason) 
        throws PaymentException, ValidationException, ResourceNotFoundException;
    
    /**
     * Save or update a payment method
     */
    PaymentMethod savePaymentMethod(PaymentMethod method) throws ValidationException;
    
    /**
     * Delete a payment method
     */
    void deletePaymentMethod(String paymentMethodId) throws ValidationException, ResourceNotFoundException;
    
    /**
     * Get a payment method by ID
     */
    PaymentMethod getPaymentMethodById(String paymentMethodId) throws ResourceNotFoundException;
    
    /**
     * Find a transaction by internal ID
     */
    PaymentTransaction findTransactionById(String transactionId) throws ResourceNotFoundException;
    
    /**
     * Find a transaction by external (gateway) ID
     */
    PaymentTransaction findTransactionByExternalId(String externalTransactionId) throws ResourceNotFoundException;
    
    /**
     * Update transaction status from gateway callback
     */
    PaymentTransaction updateTransactionStatusFromCallback(String gatewayTransactionRef, String responseCode,
                                                         String gatewayMessage, String secureHash) 
        throws PaymentException, ValidationException;
    
    /**
     * Check payment status with gateway
     */
    PaymentTransaction checkPaymentStatus(String transactionId, String externalTransactionId)
        throws PaymentException, ResourceNotFoundException;
    
    /**
     * Update transaction status
     */
    void updateTransactionStatus(String transactionId, String status) throws ResourceNotFoundException;
    
    /**
     * Get the latest transaction for an order
     */
    PaymentTransaction getLatestTransactionForOrder(String orderId) throws ResourceNotFoundException;
}