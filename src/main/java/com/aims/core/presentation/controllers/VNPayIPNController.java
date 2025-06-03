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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.sql.SQLException;

/**
 * VNPay IPN (Instant Payment Notification) Controller
 * This handles server-to-server notifications from VNPay about payment status changes
 * 
 * Important: This endpoint must be accessible from the internet for VNPay to send notifications
 * URL should be configured in VNPay merchant dashboard
 */
public class VNPayIPNController {
    
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
        
        try {
            // Step 1: Validate signature
            if (!vnPayAdapter.validateResponseSignature(ipnParams)) {
                System.err.println("VNPay IPN: Invalid signature received");
                response.put("RspCode", "97");
                response.put("Message", "Invalid signature");
                return response;
            }
            
            // Step 2: Extract transaction information
            String vnpTxnRef = ipnParams.get("vnp_TxnRef");
            String vnpResponseCode = ipnParams.get("vnp_ResponseCode");
            String vnpTransactionNo = ipnParams.get("vnp_TransactionNo");
            String vnpAmount = ipnParams.get("vnp_Amount");
            String vnpBankCode = ipnParams.get("vnp_BankCode");
            String vnpPayDate = ipnParams.get("vnp_PayDate");
            
            if (vnpTxnRef == null || vnpResponseCode == null) {
                System.err.println("VNPay IPN: Missing required parameters");
                response.put("RspCode", "01");
                response.put("Message", "Missing required parameters");
                return response;
            }
            
            // Step 3: Find the corresponding payment transaction
            PaymentTransaction transaction = findTransactionByReference(vnpTxnRef);
            if (transaction == null) {
                System.err.println("VNPay IPN: Transaction not found for reference: " + vnpTxnRef);
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return response;
            }
            
            // Step 4: Check if transaction was already processed
            if (isTransactionAlreadyProcessed(transaction)) {
                System.out.println("VNPay IPN: Transaction already processed: " + vnpTxnRef);
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }
            
            // Step 5: Validate amount
            int expectedAmount = (int)(transaction.getAmount() * 100); // Convert to VNPay format
            int receivedAmount = Integer.parseInt(vnpAmount);
            if (expectedAmount != receivedAmount) {
                System.err.println("VNPay IPN: Amount mismatch. Expected: " + expectedAmount + ", Received: " + receivedAmount);
                response.put("RspCode", "04");
                response.put("Message", "Invalid amount");
                return response;
            }
            
            // Step 6: Update transaction status based on VNPay response
            updateTransactionStatus(transaction, vnpResponseCode, vnpTransactionNo, vnpBankCode, vnpPayDate);
            
            // Step 7: Update order status if payment successful
            if ("00".equals(vnpResponseCode)) {
                updateOrderStatus(transaction.getOrder(), OrderStatus.APPROVED);
                System.out.println("VNPay IPN: Payment successful for order: " + transaction.getOrder().getOrderId());
            } else {
                updateOrderStatus(transaction.getOrder(), OrderStatus.PAYMENT_FAILED);
                System.out.println("VNPay IPN: Payment failed for order: " + transaction.getOrder().getOrderId() + 
                                 " with code: " + vnpResponseCode);
            }
            
            // Step 8: Send success response to VNPay
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            
        } catch (Exception e) {
            System.err.println("VNPay IPN: Error processing IPN: " + e.getMessage());
            e.printStackTrace();
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }
        
        return response;
    }
    
    /**
     * Find payment transaction by VNPay transaction reference
     */
    private PaymentTransaction findTransactionByReference(String vnpTxnRef) {
        try {
            // Extract order ID from vnpTxnRef (format: orderId_timestamp)
            String orderId = vnpTxnRef.split("_")[0];
            List<PaymentTransaction> transactions = paymentTransactionDAO.getByOrderId(orderId);
            // Return the first transaction for this order (assuming one payment per order)
            return transactions.isEmpty() ? null : transactions.get(0);
        } catch (SQLException e) {
            System.err.println("Error finding transaction: " + e.getMessage());
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
                                       String vnpTransactionNo, String bankCode, String payDate) {
        try {
            if ("00".equals(responseCode)) {
                transaction.setTransactionStatus("SUCCESS");
            } else {
                transaction.setTransactionStatus("FAILED");
            }
            
            transaction.setExternalTransactionId(vnpTransactionNo);
            transaction.setTransactionContent("BankCode: " + bankCode + ", PayDate: " + payDate);
            
            paymentTransactionDAO.updateStatus(transaction.getTransactionId(), transaction.getTransactionStatus(), transaction.getExternalTransactionId());
            
        } catch (SQLException e) {
            System.err.println("Error updating transaction status: " + e.getMessage());
            throw new RuntimeException("Failed to update transaction", e);
        }
    }
    
    /**
     * Update order status based on payment result
     */
    private void updateOrderStatus(OrderEntity order, OrderStatus status) {
        try {
            order.setOrderStatus(status);
            orderDAO.updateStatus(order.getOrderId(), status);
        } catch (SQLException e) {
            System.err.println("Error updating order status: " + e.getMessage());
            throw new RuntimeException("Failed to update order", e);
        }
    }
}
