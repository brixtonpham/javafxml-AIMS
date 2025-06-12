package com.aims.core.application.impl; // Or com.aims.core.application.services.impl;

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
import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.infrastructure.database.dao.IOrderEntityDAO;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PaymentServiceImpl implements IPaymentService {

    private static final Logger logger = Logger.getLogger(PaymentServiceImpl.class.getName());
    
    private final IPaymentTransactionDAO paymentTransactionDAO;
    private final IPaymentMethodDAO paymentMethodDAO;
    private final ICardDetailsDAO cardDetailsDAO;
    private final IVNPayAdapter vnPayAdapter;
    // private final PaymentStrategyFactory paymentStrategyFactory; // Alternative approach

    public PaymentServiceImpl(IPaymentTransactionDAO paymentTransactionDAO,
                              IPaymentMethodDAO paymentMethodDAO,
                              ICardDetailsDAO cardDetailsDAO,
                              IVNPayAdapter vnPayAdapter
                              /*, PaymentStrategyFactory paymentStrategyFactory */) {
        this.paymentTransactionDAO = paymentTransactionDAO;
        this.paymentMethodDAO = paymentMethodDAO;
        this.cardDetailsDAO = cardDetailsDAO;
        this.vnPayAdapter = vnPayAdapter;
        // this.paymentStrategyFactory = paymentStrategyFactory;
    }

    @Override
    public PaymentTransaction processPayment(OrderEntity order, String paymentMethodId /*, CardDetailsDTO cardDetailsInput */)
            throws SQLException, PaymentException, ValidationException, ResourceNotFoundException {
        
        // Enhanced input validation with detailed error messages
        if (order == null) {
            logger.log(Level.WARNING, "Payment processing attempted with null order");
            throw new ValidationException("Order information is required for payment processing. Please verify your order details.");
        }
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            logger.log(Level.WARNING, "Payment processing attempted with null/empty payment method ID for order: " + order.getOrderId());
            throw new ValidationException("Payment method selection is required. Please select a payment method and try again.");
        }
        if (order.getTotalAmountPaid() <= 0) {
            logger.log(Level.WARNING, "Payment processing attempted with invalid amount for order: " + order.getOrderId() + ", Amount: " + order.getTotalAmountPaid());
            throw new ValidationException("Payment amount must be greater than zero. Please verify your order total.");
        }
        
        logger.log(Level.INFO, "Processing payment for order: " + order.getOrderId() + " with payment method: " + paymentMethodId);

        PaymentMethod method = null;
        
        // For temporary VNPAY payment methods (starts with "VNPAY_TEMP_" or "VNPAY_TEST_"), create in-memory method
        if (paymentMethodId.startsWith("VNPAY_TEMP_") || paymentMethodId.startsWith("VNPAY_TEST_")) {
            method = new PaymentMethod();
            method.setPaymentMethodId(paymentMethodId);
            method.setMethodType(PaymentMethodType.CREDIT_CARD); // Default for VNPAY
            method.setUserAccount(null); // Temporary method
            method.setDefault(false);
            System.out.println("PaymentServiceImpl: Using temporary PaymentMethod for VNPAY: " + paymentMethodId);
        } else {
            // Try to get from database for persistent payment methods
            method = paymentMethodDAO.getById(paymentMethodId);
            if (method == null) {
                throw new ResourceNotFoundException("PaymentMethod with ID " + paymentMethodId + " not found.");
            }
        }

        CardDetails card = null;
        if (method.getMethodType() == PaymentMethodType.CREDIT_CARD || method.getMethodType() == PaymentMethodType.DOMESTIC_DEBIT_CARD) {
            // For temporary VNPAY payment methods, we don't require card details since user enters them on VNPAY gateway
            if (!paymentMethodId.startsWith("VNPAY_TEMP_") && !paymentMethodId.startsWith("VNPAY_TEST_")) {
                card = cardDetailsDAO.getByPaymentMethodId(paymentMethodId);
                if (card == null /* && cardDetailsInput == null */) { // If cardDetailsInput was a param
                    throw new ValidationException("Card details are required for " + method.getMethodType());
                }
            } else {
                System.out.println("PaymentServiceImpl: Skipping card details validation for temporary VNPAY payment method");
            }
        }

        // Prepare parameters for VNPay adapter
        // This would involve constructing a Map or DTO based on 'order', 'method', 'card'
        // and any 'cardDetailsInput' if it were passed for one-time card use.
        Map<String, Object> vnPayParams = vnPayAdapter.preparePaymentParameters(order, method, card /*, cardDetailsInput */);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("PAY-" + UUID.randomUUID().toString());
        transaction.setOrder(order);
        transaction.setPaymentMethod(method);
        transaction.setTransactionType(TransactionType.PAYMENT);
        transaction.setAmount(order.getTotalAmountPaid());
        transaction.setTransactionDateTime(LocalDateTime.now());
        transaction.setTransactionStatus("PENDING_GATEWAY"); // Initial status before calling gateway

        try {
            // Persist transaction attempt before calling gateway
            logger.log(Level.INFO, "Persisting payment transaction: " + transaction.getTransactionId());
            paymentTransactionDAO.add(transaction);

            // Enhanced gateway call with better error handling
            Map<String, String> gatewayResponse;
            try {
                logger.log(Level.INFO, "Calling VNPay adapter for payment processing");
                gatewayResponse = vnPayAdapter.processPayment(vnPayParams);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "VNPay adapter call failed for transaction: " + transaction.getTransactionId(), e);
                
                // Provide user-friendly error message based on exception type
                if (e.getMessage() != null) {
                    String errorMsg = e.getMessage().toLowerCase();
                    if (errorMsg.contains("network") || errorMsg.contains("connection")) {
                        throw new PaymentException("Unable to connect to payment gateway. Please check your internet connection and try again.", "NETWORK_ERROR", e);
                    } else if (errorMsg.contains("timeout")) {
                        throw new PaymentException("Payment request timed out. Please try again.", "TIMEOUT_ERROR", e);
                    } else if (errorMsg.contains("config") || errorMsg.contains("invalid")) {
                        throw new PaymentException("Payment gateway configuration error. Please try again or contact support.", "CONFIG_ERROR", e);
                    }
                }
                throw new PaymentException("Payment gateway is temporarily unavailable. Please try again in a few minutes.", "GATEWAY_ERROR", e);
            }

            // Validate gateway response
            if (gatewayResponse == null || gatewayResponse.isEmpty()) {
                logger.log(Level.SEVERE, "VNPay adapter returned null or empty response for transaction: " + transaction.getTransactionId());
                throw new PaymentException("Payment gateway returned an invalid response. Please try again.", "INVALID_RESPONSE");
            }

            // For VNPAY, the response contains payment URL for redirect, not transaction result
            String paymentUrl = gatewayResponse.get("paymentUrl");
            String vnpTxnRef = gatewayResponse.get("vnp_TxnRef");
            
            if (paymentUrl != null && !paymentUrl.trim().isEmpty()) {
                // Store payment URL in gatewayResponseData as JSON
                transaction.setGatewayResponseData("{\"paymentUrl\":\"" + paymentUrl + "\",\"vnp_TxnRef\":\"" + vnpTxnRef + "\"}");
                transaction.setTransactionStatus("PENDING_USER_ACTION"); // User needs to complete payment on VNPAY
                transaction.setTransactionContent("Payment URL generated successfully. Waiting for user action on VNPAY gateway.");
                transaction.setExternalTransactionId(vnpTxnRef); // Set the transaction reference
                logger.log(Level.INFO, "Payment URL generated successfully for transaction: " + transaction.getTransactionId());
            } else {
                logger.log(Level.SEVERE, "VNPay adapter failed to generate payment URL for transaction: " + transaction.getTransactionId());
                transaction.setTransactionStatus("FAILED");
                transaction.setTransactionContent("Failed to generate payment URL from VNPAY gateway");
                throw new PaymentException("Unable to generate payment URL. This may be due to gateway maintenance or configuration issues. Please try again later.", "URL_GENERATION_FAILED");
            }
            
            // Persist final status and gateway data
            logger.log(Level.INFO, "Updating transaction status and gateway data for: " + transaction.getTransactionId());
            paymentTransactionDAO.updateStatusAndGatewayData(transaction.getTransactionId(), transaction.getTransactionStatus(), transaction.getExternalTransactionId(), transaction.getGatewayResponseData());

        } catch (PaymentException e) {
            // Payment-specific exceptions with enhanced logging and recovery
            logger.log(Level.WARNING, "Payment processing failed for transaction: " + transaction.getTransactionId() + " - " + e.getMessage(), e);
            handleTransactionFailure(transaction, e.getMessage(), e.getGatewayErrorCode());
            throw e; // Re-throw with original error details
            
        } catch (SQLException e) {
            // Database-related exceptions
            logger.log(Level.SEVERE, "Database error during payment processing for transaction: " + transaction.getTransactionId(), e);
            handleTransactionFailure(transaction, "Database connectivity issue: " + e.getMessage(), "DB_ERROR");
            
            // Provide user-friendly error message
            throw new PaymentException("A temporary system error occurred. Please try again. If the problem persists, contact support.", "DATABASE_ERROR", e);
            
        } catch (Exception e) {
            // Unexpected exceptions
            logger.log(Level.SEVERE, "Unexpected error during payment processing for transaction: " + transaction.getTransactionId(), e);
            handleTransactionFailure(transaction, "Unexpected error: " + e.getMessage(), "SYSTEM_ERROR");
            
            throw new PaymentException("An unexpected system error occurred during payment processing. Please try again or contact support.", "SYSTEM_ERROR", e);
        }
        logger.log(Level.INFO, "Payment processing completed successfully for transaction: " + transaction.getTransactionId());
        return transaction;
    }
    
    /**
     * Handle transaction failure with proper error logging and status updates
     */
    private void handleTransactionFailure(PaymentTransaction transaction, String errorMessage, String errorCode) {
        transaction.setTransactionStatus("FAILED_INTERNAL");
        transaction.setTransactionContent(errorMessage + (errorCode != null ? " (Error: " + errorCode + ")" : ""));
        
        try {
            PaymentTransaction existing = paymentTransactionDAO.getById(transaction.getTransactionId());
            if (existing != null) {
                // Update existing transaction with failure details
                paymentTransactionDAO.updateStatus(transaction.getTransactionId(), transaction.getTransactionStatus(), transaction.getExternalTransactionId());
                logger.log(Level.INFO, "Updated transaction failure status for: " + transaction.getTransactionId());
            } else {
                // Transaction was never saved initially - critical error
                logger.log(Level.SEVERE, "Transaction was never persisted initially: " + transaction.getTransactionId());
                // Don't attempt to save again to avoid duplicate key errors
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Critical error: Failed to update transaction failure status for: " + transaction.getTransactionId(), ex);
            // This scenario needs immediate attention - could indicate database issues
        }
    }

    @Override
    public PaymentTransaction processRefund(String orderIdToRefund, String originalGatewayTransactionId, float refundAmount, String reason)
            throws SQLException, PaymentException, ValidationException, ResourceNotFoundException {

        OrderEntity order = orderDAO.getById(orderIdToRefund); // Assuming orderDAO is injected or accessible
        if (order == null) {
            throw new ResourceNotFoundException("Order with ID " + orderIdToRefund + " not found for refund.");
        }
        if (refundAmount <= 0) {
            throw new ValidationException("Refund amount must be positive.");
        }
        if (originalGatewayTransactionId == null || originalGatewayTransactionId.trim().isEmpty()){
            throw new ValidationException("Original gateway transaction ID is required for refund.");
        }

        PaymentTransaction refundTransaction = new PaymentTransaction();
        refundTransaction.setTransactionId("REF-" + UUID.randomUUID().toString());
        refundTransaction.setOrder(order);
        // PaymentMethod might be null for refund record or fetched from original transaction if needed
        refundTransaction.setTransactionType(TransactionType.REFUND);
        refundTransaction.setAmount(refundAmount);
        refundTransaction.setTransactionDateTime(LocalDateTime.now());
        refundTransaction.setTransactionStatus("PENDING_GATEWAY");
        refundTransaction.setTransactionContent("Refund Reason: " + reason);


        try {
            paymentTransactionDAO.add(refundTransaction);

            Map<String, Object> vnPayRefundParams = vnPayAdapter.prepareRefundParameters(order, originalGatewayTransactionId, refundAmount, reason);
            Map<String, String> gatewayResponse = vnPayAdapter.processRefund(vnPayRefundParams);

            refundTransaction.setExternalTransactionId(gatewayResponse.get("vnp_TransactionNo")); // Or equivalent key
            String responseCode = gatewayResponse.get("vnp_ResponseCode");

            if ("00".equals(responseCode)) { // Assuming "00" is success
                refundTransaction.setTransactionStatus("SUCCESS");
            } else {
                refundTransaction.setTransactionStatus("FAILED");
                refundTransaction.setTransactionContent(refundTransaction.getTransactionContent() +
                        " | Gateway Error: " + responseCode + " - " + gatewayResponse.get("vnp_Message"));
                paymentTransactionDAO.updateStatus(refundTransaction.getTransactionId(), refundTransaction.getTransactionStatus(), refundTransaction.getExternalTransactionId());
                throw new PaymentException("Refund failed at gateway. Code: " + responseCode + ". Message: " + gatewayResponse.get("vnp_Message"));
            }
            paymentTransactionDAO.updateStatus(refundTransaction.getTransactionId(), refundTransaction.getTransactionStatus(), refundTransaction.getExternalTransactionId());

        } catch (PaymentException | SQLException e) {
            refundTransaction.setTransactionStatus("FAILED_INTERNAL");
            refundTransaction.setTransactionContent((refundTransaction.getTransactionContent() != null ? refundTransaction.getTransactionContent() : "") + " | Internal Error: " + e.getMessage());
             try {
                PaymentTransaction existing = paymentTransactionDAO.getById(refundTransaction.getTransactionId());
                if(existing != null) {
                    paymentTransactionDAO.updateStatus(refundTransaction.getTransactionId(), refundTransaction.getTransactionStatus(), refundTransaction.getExternalTransactionId());
                } else {
                     paymentTransactionDAO.add(refundTransaction);
                }
            } catch (SQLException ex) {
                System.err.println("Critical error: Failed to save/update refund transaction failure status: " + ex.getMessage());
            }
            if (e instanceof PaymentException) throw (PaymentException)e;
            if (e instanceof SQLException) throw (SQLException)e;
            throw new PaymentException("Refund processing encountered an internal error: " + e.getMessage(), e);
        }
        return refundTransaction;
    }

    @Override
    public PaymentTransaction checkPaymentStatus(String transactionId, String externalTransactionId)
            throws PaymentException, SQLException, ResourceNotFoundException, ValidationException {
        
        logger.log(Level.INFO, "Checking payment status for transaction: " + transactionId);
        
        // Enhanced validation with user-friendly messages
        if (transactionId == null || transactionId.trim().isEmpty()) {
            logger.log(Level.WARNING, "Payment status check attempted with null/empty transaction ID");
            throw new ValidationException("Transaction ID is required to check payment status.");
        }
        
        PaymentTransaction transaction = paymentTransactionDAO.getById(transactionId);
        if (transaction == null) {
            logger.log(Level.WARNING, "Payment status check attempted for non-existent transaction: " + transactionId);
            throw new ResourceNotFoundException("Payment transaction not found. Please verify your transaction reference.");
        }
        
        // External transaction ID is preferred for querying gateway if available
        String queryId = (externalTransactionId != null && !externalTransactionId.isEmpty()) ? externalTransactionId : transaction.getExternalTransactionId();
        if (queryId == null || queryId.trim().isEmpty()){
             logger.log(Level.WARNING, "Payment status check attempted without external transaction ID for: " + transactionId);
             throw new ValidationException("Unable to check payment status with gateway. Transaction reference is missing.");
        }

        Map<String, String> gatewayStatus;
        try {
            logger.log(Level.INFO, "Querying VNPay for transaction status: " + queryId);
            gatewayStatus = vnPayAdapter.queryTransactionStatus(queryId, transaction.getOrder().getOrderId(), transaction.getTransactionDateTime());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to query payment gateway status for transaction: " + transactionId, e);
            
            // Provide user-friendly error based on exception type
            if (e.getMessage() != null) {
                String errorMsg = e.getMessage().toLowerCase();
                if (errorMsg.contains("network") || errorMsg.contains("connection")) {
                    throw new PaymentException("Unable to connect to payment gateway to check status. Please try again.", "NETWORK_ERROR", e);
                } else if (errorMsg.contains("timeout")) {
                    throw new PaymentException("Request to check payment status timed out. Please try again.", "TIMEOUT_ERROR", e);
                }
            }
            throw new PaymentException("Unable to check payment status at this time. Please try again later.", "GATEWAY_ERROR", e);
        }

        // Update local transaction based on gateway status
        String gatewayResponseCode = gatewayStatus.get("vnp_ResponseCode"); // or "vnp_TransactionStatus"
        String gatewayMessage = gatewayStatus.get("vnp_Message");

        if ("00".equals(gatewayResponseCode)) { // Payment successful
            transaction.setTransactionStatus("SUCCESS");
        } else if ("PENDING".equalsIgnoreCase(gatewayStatus.get("statusCategory"))) { // Example check for a pending category
             transaction.setTransactionStatus("PENDING_GATEWAY");
        }
        else {
            transaction.setTransactionStatus("FAILED"); // Or a more specific status from gateway
        }
        transaction.setTransactionContent("Status checked at " + LocalDateTime.now() + ". Gateway: " + gatewayMessage);
        // Update external ID if it was missing or changed
        transaction.setExternalTransactionId(gatewayStatus.getOrDefault("vnp_TransactionNo", transaction.getExternalTransactionId()));


        try {
            paymentTransactionDAO.updateStatus(transaction.getTransactionId(), transaction.getTransactionStatus(), transaction.getExternalTransactionId());
            logger.log(Level.INFO, "Payment status updated successfully for transaction: " + transactionId + " -> " + transaction.getTransactionStatus());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update payment status in database for transaction: " + transactionId, e);
            throw new PaymentException("Unable to update payment status. Please try again.", "DATABASE_ERROR", e);
        }
        
        return transaction;
    }

    @Override
    public PaymentTransaction updateTransactionStatusFromCallback(String vnpTxnRef, String responseCode,
                                                                String externalTransactionId, String gatewayResponseData)
            throws PaymentException, SQLException, ResourceNotFoundException, ValidationException {
        
        if (vnpTxnRef == null || vnpTxnRef.trim().isEmpty()) {
            throw new ValidationException("VNPay transaction reference is required");
        }
        if (responseCode == null || responseCode.trim().isEmpty()) {
            throw new ValidationException("VNPay response code is required");
        }
        
        try {
            // Extract order ID from vnpTxnRef (format: orderId_timestamp)
            String orderId = vnpTxnRef.split("_")[0];
            
            // Find the transaction by order ID
            java.util.List<PaymentTransaction> transactions = paymentTransactionDAO.getByOrderId(orderId);
            if (transactions.isEmpty()) {
                throw new ResourceNotFoundException("No payment transaction found for order ID: " + orderId);
            }
            
            // Get the most recent transaction for this order
            PaymentTransaction transaction = transactions.get(0); // List is ordered by transactionDateTime DESC
            
            // Map VNPay response code to internal status
            String newStatus = mapVNPayResponseToStatus(responseCode);
            
            // Update transaction status and external ID
            transaction.setTransactionStatus(newStatus);
            if (externalTransactionId != null && !externalTransactionId.trim().isEmpty()) {
                transaction.setExternalTransactionId(externalTransactionId);
            }
            
            // Update gateway response data
            if (gatewayResponseData != null && !gatewayResponseData.trim().isEmpty()) {
                transaction.setGatewayResponseData(gatewayResponseData);
            }
            
            // Update transaction content with callback information
            String callbackInfo = String.format("Callback processed at %s. VNPay code: %s",
                                               LocalDateTime.now().toString(), responseCode);
            if (transaction.getTransactionContent() != null) {
                transaction.setTransactionContent(transaction.getTransactionContent() + " | " + callbackInfo);
            } else {
                transaction.setTransactionContent(callbackInfo);
            }
            
            // Persist changes to database
            if (gatewayResponseData != null) {
                paymentTransactionDAO.updateStatusAndGatewayData(
                    transaction.getTransactionId(),
                    transaction.getTransactionStatus(),
                    transaction.getExternalTransactionId(),
                    transaction.getGatewayResponseData()
                );
            } else {
                paymentTransactionDAO.updateStatus(
                    transaction.getTransactionId(),
                    transaction.getTransactionStatus(),
                    transaction.getExternalTransactionId()
                );
            }
            
            System.out.println("PaymentServiceImpl: Updated transaction " + transaction.getTransactionId() +
                             " status to " + newStatus + " for order " + orderId);
            
            return transaction;
            
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ValidationException("Invalid VNPay transaction reference format: " + vnpTxnRef);
        } catch (Exception e) {
            if (e instanceof PaymentException || e instanceof SQLException ||
                e instanceof ResourceNotFoundException || e instanceof ValidationException) {
                throw e;
            }
            throw new PaymentException("Unexpected error updating transaction status from callback: " + e.getMessage(), e);
        }
    }
    
    /**
     * Map VNPay response codes to internal transaction statuses
     */
    private String mapVNPayResponseToStatus(String responseCode) {
        switch (responseCode) {
            case "00":
                return "SUCCESS";
            case "24":
                return "CANCELLED";
            case "07":
                return "PENDING";
            case "09":
            case "10":
            case "11":
            case "12":
            case "13":
            case "51":
            case "65":
            case "75":
            case "79":
            default:
                return "FAILED";
        }
    }

    // Assume IOrderEntityDAO is available if needed, e.g. for fetching OrderEntity for refund context
    // This would typically be injected if needed by the service.
    private IOrderEntityDAO orderDAO;
    public void setOrderDAO(IOrderEntityDAO orderDAO) { // For setter injection if needed
        this.orderDAO = orderDAO;
    }
}