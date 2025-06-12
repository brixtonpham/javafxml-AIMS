package com.aims.core.presentation.controllers;

import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayAdapterImpl;
import com.aims.core.infrastructure.database.dao.IPaymentTransactionDAO;
import com.aims.core.infrastructure.database.dao.PaymentTransactionDAOImpl;
import com.aims.core.infrastructure.database.dao.IOrderEntityDAO;
import com.aims.core.infrastructure.database.dao.OrderEntityDAOImpl;
import com.aims.core.infrastructure.database.dao.IPaymentMethodDAO;
import com.aims.core.infrastructure.database.dao.PaymentMethodDAOImpl;
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import com.aims.core.infrastructure.database.dao.UserAccountDAOImpl;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.infrastructure.database.dao.ProductDAOImpl;
import com.aims.core.infrastructure.database.dao.IOrderItemDAO;
import com.aims.core.infrastructure.database.dao.OrderItemDAOImpl;
import com.aims.core.infrastructure.database.dao.ICardDetailsDAO;
import com.aims.core.infrastructure.database.dao.CardDetailsDAOImpl;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.entities.OrderEntity;
import com.aims.core.enums.OrderStatus;
import com.aims.core.presentation.utils.PaymentErrorHandler;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * VNPay IPN (Instant Payment Notification) Controller
 * This handles server-to-server notifications from VNPay about payment status changes
 * 
 * Important: This endpoint must be accessible from the internet for VNPay to send notifications
 * URL should be configured in VNPay merchant dashboard
 */
public class VNPayIPNController {
    
    private static final Logger logger = Logger.getLogger(VNPayIPNController.class.getName());
    
    private final IVNPayAdapter vnPayAdapter;
    private final IPaymentTransactionDAO paymentTransactionDAO;
    private final IOrderEntityDAO orderDAO;
     public VNPayIPNController() {
        this.vnPayAdapter = new VNPayAdapterImpl();
        
        // Create required DAO dependencies
        IUserAccountDAO userAccountDAO = new UserAccountDAOImpl();
        IProductDAO productDAO = new ProductDAOImpl();
        IOrderItemDAO orderItemDAO = new OrderItemDAOImpl(productDAO);
        ICardDetailsDAO cardDetailsDAO = new CardDetailsDAOImpl();
        
        // Create DAOs with proper dependencies
        this.orderDAO = new OrderEntityDAOImpl(orderItemDAO, userAccountDAO);
        IPaymentMethodDAO paymentMethodDAO = new PaymentMethodDAOImpl(userAccountDAO, cardDetailsDAO);
        this.paymentTransactionDAO = new PaymentTransactionDAOImpl(
            this.orderDAO,
            paymentMethodDAO
        );
    }
    
    /**
     * Handles IPN notifications from VNPay
     * This method should be called when VNPay sends payment status updates
     * 
     * @param ipnParams Parameters received from VNPay IPN
     * @return Response code to send back to VNPay ({"RspCode":"00","Message":"Confirm Success"} for success)
     */
    public Map<String, String> handleIPN(Map<String, String> ipnParams) {
        Map<String, String> response = new HashMap<>();
        String vnpTxnRef = null;
        
        try {
            // Enhanced input validation
            if (ipnParams == null || ipnParams.isEmpty()) {
                logger.log(Level.SEVERE, "VNPay IPN: Received null or empty parameters");
                response.put("RspCode", "01");
                response.put("Message", "Missing required parameters");
                return response;
            }
            
            logger.log(Level.INFO, "VNPay IPN: Processing IPN with " + ipnParams.size() + " parameters");
            
            // Step 1: Validate signature with enhanced error handling
            try {
                if (!vnPayAdapter.validateResponseSignature(ipnParams)) {
                    vnpTxnRef = ipnParams.get("vnp_TxnRef");
                    logger.log(Level.SEVERE, "VNPay IPN: Invalid signature received for transaction: " + vnpTxnRef);
                    
                    // Handle signature validation failure with centralized error handling
                    PaymentErrorHandler.handleSignatureValidationFailure(vnpTxnRef, ipnParams);
                    
                    response.put("RspCode", "97");
                    response.put("Message", "Invalid signature");
                    return response;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "VNPay IPN: Error during signature validation", e);
                response.put("RspCode", "99");
                response.put("Message", "Signature validation error");
                return response;
            }
            
            // Step 2: Extract and validate transaction information
            vnpTxnRef = ipnParams.get("vnp_TxnRef");
            String vnpResponseCode = ipnParams.get("vnp_ResponseCode");
            String vnpTransactionNo = ipnParams.get("vnp_TransactionNo");
            String vnpAmount = ipnParams.get("vnp_Amount");
            String vnpBankCode = ipnParams.get("vnp_BankCode");
            String vnpPayDate = ipnParams.get("vnp_PayDate");
            
            // Enhanced parameter validation with detailed logging
            if (vnpTxnRef == null || vnpTxnRef.trim().isEmpty()) {
                logger.log(Level.SEVERE, "VNPay IPN: Missing vnp_TxnRef parameter");
                response.put("RspCode", "01");
                response.put("Message", "Missing transaction reference");
                return response;
            }
            
            if (vnpResponseCode == null || vnpResponseCode.trim().isEmpty()) {
                logger.log(Level.SEVERE, "VNPay IPN: Missing vnp_ResponseCode parameter for transaction: " + vnpTxnRef);
                response.put("RspCode", "01");
                response.put("Message", "Missing response code");
                return response;
            }
            
            if (vnpAmount == null || vnpAmount.trim().isEmpty()) {
                logger.log(Level.SEVERE, "VNPay IPN: Missing vnp_Amount parameter for transaction: " + vnpTxnRef);
                response.put("RspCode", "01");
                response.put("Message", "Missing amount information");
                return response;
            }
            
            logger.log(Level.INFO, "VNPay IPN: Processing transaction: " + vnpTxnRef + " with response code: " + vnpResponseCode);
            
            // Step 3: Find the corresponding payment transaction with enhanced error handling
            PaymentTransaction transaction;
            try {
                transaction = findTransactionByReference(vnpTxnRef);
                if (transaction == null) {
                    logger.log(Level.WARNING, "VNPay IPN: Transaction not found for reference: " + vnpTxnRef);
                    response.put("RspCode", "01");
                    response.put("Message", "Order not found");
                    return response;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "VNPay IPN: Error finding transaction for reference: " + vnpTxnRef, e);
                response.put("RspCode", "99");
                response.put("Message", "Database error");
                return response;
            }
            
            // Step 4: Check if transaction was already processed
            if (isTransactionAlreadyProcessed(transaction)) {
                logger.log(Level.INFO, "VNPay IPN: Transaction already processed: " + vnpTxnRef);
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }
            
            // Step 5: Validate amount with enhanced error handling
            try {
                int expectedAmount = (int)(transaction.getAmount() * 100); // Convert to VNPay format
                int receivedAmount = Integer.parseInt(vnpAmount);
                if (expectedAmount != receivedAmount) {
                    logger.log(Level.SEVERE, "VNPay IPN: Amount mismatch for transaction: " + vnpTxnRef +
                              ". Expected: " + expectedAmount + ", Received: " + receivedAmount);
                    response.put("RspCode", "04");
                    response.put("Message", "Invalid amount");
                    return response;
                }
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE, "VNPay IPN: Invalid amount format for transaction: " + vnpTxnRef +
                          ". Amount: " + vnpAmount, e);
                response.put("RspCode", "04");
                response.put("Message", "Invalid amount format");
                return response;
            }
            
            // Step 6: Update transaction status based on VNPay response with enhanced error handling
            try {
                updateTransactionStatus(transaction, vnpResponseCode, vnpTransactionNo, vnpBankCode, vnpPayDate);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "VNPay IPN: Error updating transaction status for: " + vnpTxnRef, e);
                response.put("RspCode", "99");
                response.put("Message", "Database error");
                return response;
            }
            
            // Step 7: Update order status if payment successful with enhanced error handling
            try {
                if ("00".equals(vnpResponseCode)) {
                    updateOrderStatus(transaction.getOrder(), OrderStatus.APPROVED);
                    logger.log(Level.INFO, "VNPay IPN: Payment successful for order: " + transaction.getOrder().getOrderId());
                } else {
                    updateOrderStatus(transaction.getOrder(), OrderStatus.PAYMENT_FAILED);
                    logger.log(Level.WARNING, "VNPay IPN: Payment failed for order: " + transaction.getOrder().getOrderId() +
                                             " with code: " + vnpResponseCode);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "VNPay IPN: Error updating order status for: " + vnpTxnRef, e);
                // Continue processing even if order status update fails
                // The transaction status update is more critical
            }
            
            // Step 8: Send success response to VNPay
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            logger.log(Level.INFO, "VNPay IPN: Successfully processed IPN for transaction: " + vnpTxnRef);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "VNPay IPN: Unexpected error processing IPN for transaction: " + vnpTxnRef, e);
            
            // Handle callback validation error with centralized error handling
            PaymentErrorHandler.handleCallbackValidationError(e, vnpTxnRef, ipnParams);
            
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }
        
        return response;
    }
    
    /**
     * Find payment transaction by VNPay transaction reference
     */
    private PaymentTransaction findTransactionByReference(String vnpTxnRef) throws SQLException {
        try {
            // Enhanced validation of transaction reference format
            if (vnpTxnRef == null || !vnpTxnRef.contains("_")) {
                logger.log(Level.WARNING, "VNPay IPN: Invalid transaction reference format: " + vnpTxnRef);
                return null;
            }
            
            // Extract order ID from vnpTxnRef (format: orderId_timestamp)
            String orderId = vnpTxnRef.split("_")[0];
            if (orderId.trim().isEmpty()) {
                logger.log(Level.WARNING, "VNPay IPN: Empty order ID extracted from reference: " + vnpTxnRef);
                return null;
            }
            
            List<PaymentTransaction> transactions = paymentTransactionDAO.getByOrderId(orderId);
            
            // Return the first transaction for this order (assuming one payment per order)
            PaymentTransaction result = transactions.isEmpty() ? null : transactions.get(0);
            
            if (result != null) {
                logger.log(Level.INFO, "VNPay IPN: Found transaction: " + result.getTransactionId() + " for order: " + orderId);
            } else {
                logger.log(Level.WARNING, "VNPay IPN: No transaction found for order: " + orderId);
            }
            
            return result;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "VNPay IPN: Database error finding transaction for reference: " + vnpTxnRef, e);
            throw e; // Re-throw to be handled by caller
        } catch (Exception e) {
            logger.log(Level.SEVERE, "VNPay IPN: Unexpected error finding transaction for reference: " + vnpTxnRef, e);
            return null;
        }
    }
    
    /**
     * Check if transaction was already processed to avoid duplicate processing
     */
    private boolean isTransactionAlreadyProcessed(PaymentTransaction transaction) {
        return "SUCCESS".equals(transaction.getTransactionStatus()) || 
               "FAILED".equals(transaction.getTransactionStatus());
    }
    
    /**
     * Update payment transaction status with VNPay response data
     */
    private void updateTransactionStatus(PaymentTransaction transaction, String responseCode,
                                       String vnpTransactionNo, String bankCode, String payDate) throws SQLException {
        try {
            // Map VNPay response code to internal status with detailed logging
            String newStatus;
            if ("00".equals(responseCode)) {
                newStatus = "SUCCESS";
                logger.log(Level.INFO, "VNPay IPN: Marking transaction as successful: " + transaction.getTransactionId());
            } else {
                newStatus = "FAILED";
                logger.log(Level.WARNING, "VNPay IPN: Marking transaction as failed: " + transaction.getTransactionId() +
                          " with VNPay code: " + responseCode);
            }
            
            transaction.setTransactionStatus(newStatus);
            transaction.setExternalTransactionId(vnpTransactionNo);
            
            // Enhanced transaction content with more details
            String content = String.format("IPN processed - VNPay Code: %s, Bank: %s, PayDate: %s, ExternalTxnId: %s",
                                          responseCode, bankCode, payDate, vnpTransactionNo);
            transaction.setTransactionContent(content);
            
            paymentTransactionDAO.updateStatus(transaction.getTransactionId(), transaction.getTransactionStatus(), transaction.getExternalTransactionId());
            
            logger.log(Level.INFO, "VNPay IPN: Successfully updated transaction status: " + transaction.getTransactionId() + " -> " + newStatus);
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "VNPay IPN: Database error updating transaction status for: " + transaction.getTransactionId(), e);
            throw e; // Re-throw to be handled by caller
        }
    }
    
    /**
     * Update order status based on payment result
     */
    private void updateOrderStatus(OrderEntity order, OrderStatus status) throws SQLException {
        try {
            OrderStatus previousStatus = order.getOrderStatus();
            order.setOrderStatus(status);
            orderDAO.updateStatus(order.getOrderId(), status);
            
            logger.log(Level.INFO, "VNPay IPN: Successfully updated order status for: " + order.getOrderId() +
                      " from " + previousStatus + " to " + status);
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "VNPay IPN: Database error updating order status for: " + order.getOrderId(), e);
            throw e; // Re-throw to be handled by caller
        }
    }
}
