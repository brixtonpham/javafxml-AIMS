package com.aims.core.application.impl;

import org.springframework.stereotype.Service;
import com.aims.core.application.services.IPaymentService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.CardDetails;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.enums.TransactionType;
import com.aims.core.infrastructure.database.dao.IPaymentTransactionDAO;
import com.aims.core.infrastructure.database.dao.IPaymentMethodDAO;
import com.aims.core.infrastructure.database.dao.ICardDetailsDAO;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IPaymentGatewayAdapter;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.infrastructure.database.dao.IOrderEntityDAO;
import com.aims.core.application.services.IOrderValidationService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class PaymentServiceImpl implements IPaymentService {

    private static final Logger logger = Logger.getLogger(PaymentServiceImpl.class.getName());
    
    private final IPaymentTransactionDAO paymentTransactionDAO;
    private final IPaymentMethodDAO paymentMethodDAO;
    private final ICardDetailsDAO cardDetailsDAO;
    private final IPaymentGatewayAdapter paymentGatewayAdapter;
    private final IOrderValidationService orderValidationService;

    public PaymentServiceImpl(IPaymentTransactionDAO paymentTransactionDAO,
                              IPaymentMethodDAO paymentMethodDAO,
                              ICardDetailsDAO cardDetailsDAO,
                              IPaymentGatewayAdapter paymentGatewayAdapter,
                              IOrderValidationService orderValidationService) {
        this.paymentTransactionDAO = paymentTransactionDAO;
        this.paymentMethodDAO = paymentMethodDAO;
        this.cardDetailsDAO = cardDetailsDAO;
        this.paymentGatewayAdapter = paymentGatewayAdapter;
        this.orderValidationService = orderValidationService;
    }

    @Override
    public PaymentTransaction processPayment(OrderEntity order, String paymentMethodId)
            throws PaymentException, ValidationException, ResourceNotFoundException {
        
        logger.log(Level.INFO, "Starting payment processing for order: " + (order != null ? order.getOrderId() : "null"));
        
        // Input validation
        validatePaymentInputs(order, paymentMethodId);
        
        try {
            // Validate order exists and is ready for payment
            OrderEntity validatedOrder = validateOrderForPayment(order.getOrderId());
            
            // Validate payment method
            PaymentMethod validatedPaymentMethod = validatePaymentMethod(paymentMethodId);
            
            // Validate card details if needed
            CardDetails card = null;
            if (validatedPaymentMethod.getMethodType() == PaymentMethodType.CREDIT_CARD || 
                validatedPaymentMethod.getMethodType() == PaymentMethodType.DOMESTIC_DEBIT_CARD) {
                
                if (!paymentMethodId.startsWith("VNPAY_TEMP_") && !paymentMethodId.startsWith("VNPAY_TEST_")) {
                    try {
                        card = cardDetailsDAO.getByPaymentMethodId(paymentMethodId);
                    } catch (SQLException e) {
                        throw new ValidationException("Unable to retrieve card details: " + e.getMessage());
                    }
                    if (card == null) {
                        throw new ValidationException("Card details are required for " + validatedPaymentMethod.getMethodType());
                    }
                }
            }

            // Create payment transaction
            PaymentTransaction transaction = createPaymentTransaction(validatedOrder, validatedPaymentMethod);
            
            try {
                // Persist transaction before gateway call
                paymentTransactionDAO.add(transaction);

                // Prepare gateway parameters and process payment
                Map<String, Object> gatewayParams = paymentGatewayAdapter.preparePaymentParameters(validatedOrder, validatedPaymentMethod, card);
                Map<String, String> gatewayResponse = paymentGatewayAdapter.processPayment(gatewayParams);

                // Process gateway response
                processGatewayResponse(transaction, gatewayResponse);
                
                // Update transaction in database
                paymentTransactionDAO.updateStatusAndGatewayData(
                    transaction.getTransactionId(), 
                    transaction.getTransactionStatus(), 
                    transaction.getExternalTransactionId(), 
                    transaction.getGatewayResponseData()
                );

            } catch (SQLException e) {
                handleTransactionFailure(transaction, "Database error: " + e.getMessage());
                throw new PaymentException("Payment processing failed due to system error", "DATABASE_ERROR", e);
            } catch (Exception e) {
                handleTransactionFailure(transaction, "Gateway error: " + e.getMessage());
                throw new PaymentException("Payment gateway error: " + e.getMessage(), "GATEWAY_ERROR", e);
            }
            
            return transaction;
            
        } catch (SQLException e) {
            throw new PaymentException("Database error during payment validation: " + e.getMessage(), "DATABASE_ERROR", e);
        }
    }

    @Override
    public PaymentTransaction processPaymentWithParams(OrderEntity order, String paymentMethodId, Map<String, Object> additionalParams) 
            throws PaymentException, ValidationException, ResourceNotFoundException {
        
        logger.log(Level.INFO, "Processing payment with additional parameters");
        
        if (additionalParams == null || additionalParams.isEmpty()) {
            return processPayment(order, paymentMethodId);
        }
        
        // For now, delegate to standard payment processing
        // Additional parameters would be handled in a more sophisticated implementation
        return processPayment(order, paymentMethodId);
    }

    @Override
    public PaymentTransaction processRefund(String orderIdToRefund, String originalGatewayTransactionId, float refundAmount, String reason)
            throws PaymentException, ValidationException, ResourceNotFoundException {

        logger.log(Level.INFO, "Processing refund for order: " + orderIdToRefund);
        
        if (refundAmount <= 0) {
            throw new ValidationException("Refund amount must be positive");
        }
        if (originalGatewayTransactionId == null || originalGatewayTransactionId.trim().isEmpty()) {
            throw new ValidationException("Original gateway transaction ID is required");
        }

        try {
            // Get order for refund context
            OrderEntity order = orderValidationService.getValidatedOrderForPayment(orderIdToRefund);

            // Create refund transaction
            PaymentTransaction refundTransaction = new PaymentTransaction();
            refundTransaction.setTransactionId("REF-" + UUID.randomUUID().toString());
            refundTransaction.setOrder(order);
            refundTransaction.setTransactionType(TransactionType.REFUND);
            refundTransaction.setAmount(refundAmount);
            refundTransaction.setTransactionDateTime(LocalDateTime.now());
            refundTransaction.setTransactionStatus("PENDING_GATEWAY");
            refundTransaction.setTransactionContent("Refund Reason: " + reason);

            // Process refund through gateway
            Map<String, Object> refundParams = paymentGatewayAdapter.prepareRefundParameters(order, originalGatewayTransactionId, refundAmount, reason);
            Map<String, String> gatewayResponse = paymentGatewayAdapter.processRefund(refundParams);

            // Update transaction based on gateway response
            String responseCode = gatewayResponse.get("vnp_ResponseCode");
            if ("00".equals(responseCode)) {
                refundTransaction.setTransactionStatus("SUCCESS");
            } else {
                refundTransaction.setTransactionStatus("FAILED");
                throw new PaymentException("Refund failed at gateway. Code: " + responseCode);
            }

            paymentTransactionDAO.add(refundTransaction);
            return refundTransaction;
            
        } catch (SQLException e) {
            throw new PaymentException("Database error during refund processing: " + e.getMessage(), "DATABASE_ERROR", e);
        }
    }

    @Override
    public PaymentMethod savePaymentMethod(PaymentMethod method) throws ValidationException {
        logger.log(Level.INFO, "Saving payment method");
        
        if (method == null) {
            throw new ValidationException("Payment method cannot be null");
        }
        if (method.getMethodType() == null) {
            throw new ValidationException("Payment method type is required");
        }
        
        try {
            if (method.getPaymentMethodId() == null || method.getPaymentMethodId().trim().isEmpty()) {
                method.setPaymentMethodId("PM-" + UUID.randomUUID().toString());
                paymentMethodDAO.add(method);
            } else {
                // Try to update existing, create if not found
                try {
                    PaymentMethod existing = paymentMethodDAO.getById(method.getPaymentMethodId());
                    if (existing != null) {
                        paymentMethodDAO.update(method);
                    } else {
                        paymentMethodDAO.add(method);
                    }
                } catch (SQLException e) {
                    // If getById fails, try to add as new
                    paymentMethodDAO.add(method);
                }
            }
            return method;
        } catch (SQLException e) {
            throw new ValidationException("Unable to save payment method: " + e.getMessage());
        }
    }

    @Override
    public void deletePaymentMethod(String paymentMethodId) throws ValidationException, ResourceNotFoundException {
        logger.log(Level.INFO, "Deleting payment method: " + paymentMethodId);
        
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            throw new ValidationException("Payment method ID is required");
        }
        
        try {
            PaymentMethod method = paymentMethodDAO.getById(paymentMethodId);
            if (method == null) {
                throw new ResourceNotFoundException("Payment method not found: " + paymentMethodId);
            }
            
            // Check for existing transactions - use simple query since getByPaymentMethodId doesn't exist
            List<PaymentTransaction> allTransactions = paymentTransactionDAO.getAll();
            boolean hasTransactions = allTransactions.stream()
                .anyMatch(t -> t.getPaymentMethod() != null && 
                         paymentMethodId.equals(t.getPaymentMethod().getPaymentMethodId()));
            
            if (hasTransactions) {
                throw new ValidationException("Cannot delete payment method with transaction history");
            }
            
            paymentMethodDAO.delete(paymentMethodId);
        } catch (SQLException e) {
            throw new ValidationException("Unable to delete payment method: " + e.getMessage());
        }
    }

    @Override
    public PaymentMethod getPaymentMethodById(String paymentMethodId) throws ResourceNotFoundException {
        try {
            PaymentMethod method = paymentMethodDAO.getById(paymentMethodId);
            if (method == null) {
                throw new ResourceNotFoundException("Payment method not found: " + paymentMethodId);
            }
            return method;
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Unable to retrieve payment method: " + e.getMessage());
        }
    }

    @Override
    public PaymentTransaction findTransactionById(String transactionId) throws ResourceNotFoundException {
        try {
            PaymentTransaction transaction = paymentTransactionDAO.getById(transactionId);
            if (transaction == null) {
                throw new ResourceNotFoundException("Transaction not found: " + transactionId);
            }
            return transaction;
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Unable to find transaction: " + e.getMessage());
        }
    }

    @Override
    public PaymentTransaction findTransactionByExternalId(String externalTransactionId) throws ResourceNotFoundException {
        try {
            // Since getByExternalId doesn't exist, search through all transactions
            List<PaymentTransaction> allTransactions = paymentTransactionDAO.getAll();
            return allTransactions.stream()
                .filter(t -> externalTransactionId.equals(t.getExternalTransactionId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with external ID: " + externalTransactionId));
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Unable to find transaction by external ID: " + e.getMessage());
        }
    }

    @Override
    public PaymentTransaction updateTransactionStatusFromCallback(String gatewayTransactionRef, String responseCode,
                                                               String gatewayMessage, String secureHash) 
            throws PaymentException, ValidationException {
        
        if (gatewayTransactionRef == null || gatewayTransactionRef.trim().isEmpty()) {
            throw new ValidationException("Gateway transaction reference is required");
        }
        if (responseCode == null || responseCode.trim().isEmpty()) {
            throw new ValidationException("Response code is required");
        }
        
        try {
            // Extract order ID from transaction ref (format: orderId_timestamp)
            String orderId = gatewayTransactionRef.split("_")[0];
            
            // Find transaction by order ID
            List<PaymentTransaction> transactions = paymentTransactionDAO.getByOrderId(orderId);
            if (transactions.isEmpty()) {
                throw new PaymentException("No transaction found for order: " + orderId, "TRANSACTION_NOT_FOUND");
            }
            
            PaymentTransaction transaction = transactions.get(0); // Most recent
            
            // Update status based on response code
            String newStatus = mapResponseCodeToStatus(responseCode);
            transaction.setTransactionStatus(newStatus);
            transaction.setExternalTransactionId(gatewayTransactionRef);
            
            // Update in database
            paymentTransactionDAO.updateStatus(transaction.getTransactionId(), newStatus, gatewayTransactionRef);
            
            return transaction;
            
        } catch (SQLException e) {
            throw new PaymentException("Database error updating transaction status: " + e.getMessage(), "DATABASE_ERROR", e);
        } catch (Exception e) {
            throw new PaymentException("Error processing callback: " + e.getMessage(), "CALLBACK_ERROR", e);
        }
    }

    @Override
    public PaymentTransaction checkPaymentStatus(String transactionId, String externalTransactionId)
            throws PaymentException, ResourceNotFoundException {
        
        try {
            PaymentTransaction transaction = paymentTransactionDAO.getById(transactionId);
            if (transaction == null) {
                throw new ResourceNotFoundException("Transaction not found: " + transactionId);
            }
            
            // Query gateway for current status
            String queryId = (externalTransactionId != null && !externalTransactionId.isEmpty()) ? 
                           externalTransactionId : transaction.getExternalTransactionId();
            
            if (queryId == null || queryId.trim().isEmpty()) {
                throw new PaymentException("Cannot check status - no external transaction ID", "MISSING_EXTERNAL_ID");
            }
            
            Map<String, String> gatewayStatus = paymentGatewayAdapter.queryTransactionStatus(
                queryId, transaction.getOrder().getOrderId(), transaction.getTransactionDateTime());
            
            // Update local status based on gateway response
            String responseCode = gatewayStatus.get("vnp_ResponseCode");
            if ("00".equals(responseCode)) {
                transaction.setTransactionStatus("SUCCESS");
            } else {
                transaction.setTransactionStatus("FAILED");
            }
            
            paymentTransactionDAO.updateStatus(transaction.getTransactionId(), 
                                             transaction.getTransactionStatus(), 
                                             transaction.getExternalTransactionId());
            
            return transaction;
            
        } catch (SQLException e) {
            throw new PaymentException("Database error checking payment status: " + e.getMessage(), "DATABASE_ERROR", e);
        } catch (Exception e) {
            throw new PaymentException("Error checking payment status: " + e.getMessage(), "STATUS_CHECK_ERROR", e);
        }
    }

    @Override
    public void updateTransactionStatus(String transactionId, String status) throws ResourceNotFoundException {
        try {
            PaymentTransaction transaction = paymentTransactionDAO.getById(transactionId);
            if (transaction == null) {
                throw new ResourceNotFoundException("Transaction not found: " + transactionId);
            }
            
            paymentTransactionDAO.updateStatus(transactionId, status, transaction.getExternalTransactionId());
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Unable to update transaction status: " + e.getMessage());
        }
    }

    @Override
    public PaymentTransaction getLatestTransactionForOrder(String orderId) throws ResourceNotFoundException {
        try {
            List<PaymentTransaction> transactions = paymentTransactionDAO.getByOrderId(orderId);
            if (transactions.isEmpty()) {
                throw new ResourceNotFoundException("No transactions found for order: " + orderId);
            }
            // Return first transaction (assumed to be ordered by date DESC)
            return transactions.get(0);
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Unable to get latest transaction: " + e.getMessage());
        }
    }

    // Helper methods
    private void validatePaymentInputs(OrderEntity order, String paymentMethodId) throws ValidationException {
        if (order == null) {
            throw new ValidationException("Order is required");
        }
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            throw new ValidationException("Order ID is required");
        }
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            throw new ValidationException("Payment method ID is required");
        }
        if (order.getTotalAmountPaid() <= 0) {
            throw new ValidationException("Payment amount must be greater than zero");
        }
    }

    private OrderEntity validateOrderForPayment(String orderId) throws SQLException, ResourceNotFoundException, ValidationException {
        return orderValidationService.getValidatedOrderForPayment(orderId);
    }

    private PaymentMethod validatePaymentMethod(String paymentMethodId) throws SQLException, ResourceNotFoundException {
        if (paymentMethodId.startsWith("VNPAY_TEMP_") || paymentMethodId.startsWith("VNPAY_TEST_")) {
            // Create temporary payment method for VNPAY
            PaymentMethod tempMethod = new PaymentMethod();
            tempMethod.setPaymentMethodId(paymentMethodId);
            tempMethod.setMethodType(PaymentMethodType.CREDIT_CARD);
            tempMethod.setDefault(false);
            return tempMethod;
        } else {
            PaymentMethod method = paymentMethodDAO.getById(paymentMethodId);
            if (method == null) {
                throw new ResourceNotFoundException("Payment method not found: " + paymentMethodId);
            }
            return method;
        }
    }

    private PaymentTransaction createPaymentTransaction(OrderEntity order, PaymentMethod method) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("PAY-" + UUID.randomUUID().toString());
        transaction.setOrder(order);
        transaction.setPaymentMethod(method);
        transaction.setTransactionType(TransactionType.PAYMENT);
        transaction.setAmount(order.getTotalAmountPaid());
        transaction.setTransactionDateTime(LocalDateTime.now());
        transaction.setTransactionStatus("PENDING_GATEWAY");
        return transaction;
    }

    private void processGatewayResponse(PaymentTransaction transaction, Map<String, String> gatewayResponse) throws PaymentException {
        if (gatewayResponse == null || gatewayResponse.isEmpty()) {
            throw new PaymentException("Invalid gateway response", "INVALID_RESPONSE");
        }

        String paymentUrl = gatewayResponse.get("paymentUrl");
        String vnpTxnRef = gatewayResponse.get("vnp_TxnRef");
        
        if (paymentUrl != null && !paymentUrl.trim().isEmpty()) {
            transaction.setGatewayResponseData("{\"paymentUrl\":\"" + paymentUrl + "\",\"vnp_TxnRef\":\"" + vnpTxnRef + "\"}");
            transaction.setTransactionStatus("PENDING_USER_ACTION");
            transaction.setTransactionContent("Payment URL generated successfully");
            transaction.setExternalTransactionId(vnpTxnRef);
        } else {
            transaction.setTransactionStatus("FAILED");
            transaction.setTransactionContent("Failed to generate payment URL");
            throw new PaymentException("Unable to generate payment URL", "URL_GENERATION_FAILED");
        }
    }

    private void handleTransactionFailure(PaymentTransaction transaction, String errorMessage) {
        transaction.setTransactionStatus("FAILED_INTERNAL");
        transaction.setTransactionContent(errorMessage);
        
        try {
            PaymentTransaction existing = paymentTransactionDAO.getById(transaction.getTransactionId());
            if (existing != null) {
                paymentTransactionDAO.updateStatus(transaction.getTransactionId(), 
                                                 transaction.getTransactionStatus(), 
                                                 transaction.getExternalTransactionId());
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update transaction failure status: " + e.getMessage());
        }
    }

    private String mapResponseCodeToStatus(String responseCode) {
        switch (responseCode) {
            case "00": return "SUCCESS";
            case "24": return "CANCELLED";
            case "07": return "PENDING";
            default: return "FAILED";
        }
    }

    // Order DAO injection for legacy compatibility
    private IOrderEntityDAO orderDAO;
    public void setOrderDAO(IOrderEntityDAO orderDAO) {
        this.orderDAO = orderDAO;
    }
}