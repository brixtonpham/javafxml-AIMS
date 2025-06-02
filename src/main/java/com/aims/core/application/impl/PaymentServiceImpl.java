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

public class PaymentServiceImpl implements IPaymentService {

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
        if (order == null) {
            throw new ValidationException("Order cannot be null for payment processing.");
        }
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            throw new ValidationException("Payment method ID is required.");
        }

        PaymentMethod method = paymentMethodDAO.getById(paymentMethodId);
        if (method == null) {
            throw new ResourceNotFoundException("PaymentMethod with ID " + paymentMethodId + " not found.");
        }

        CardDetails card = null;
        if (method.getMethodType() == PaymentMethodType.CREDIT_CARD || method.getMethodType() == PaymentMethodType.DOMESTIC_DEBIT_CARD) {
            card = cardDetailsDAO.getByPaymentMethodId(paymentMethodId);
            if (card == null /* && cardDetailsInput == null */) { // If cardDetailsInput was a param
                throw new ValidationException("Card details are required for " + method.getMethodType());
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
            paymentTransactionDAO.add(transaction);

            Map<String, String> gatewayResponse = vnPayAdapter.processPayment(vnPayParams);

            // Update transaction based on gateway response
            transaction.setExternalTransactionId(gatewayResponse.get("vnp_TransactionNo")); // Or equivalent key
            String responseCode = gatewayResponse.get("vnp_ResponseCode"); // Or equivalent key

            if ("00".equals(responseCode)) { // "00" is typically success for VNPay
                transaction.setTransactionStatus("SUCCESS");
            } else {
                transaction.setTransactionStatus("FAILED");
                transaction.setTransactionContent("Gateway Error: " + responseCode + " - " + gatewayResponse.get("vnp_Message"));
                 // Persist updated status immediately
                paymentTransactionDAO.updateStatus(transaction.getTransactionId(), transaction.getTransactionStatus(), transaction.getExternalTransactionId());
                throw new PaymentException("Payment failed at gateway. Code: " + responseCode + ". Message: " + gatewayResponse.get("vnp_Message"));
            }
            // Persist final status and external ID
            paymentTransactionDAO.updateStatus(transaction.getTransactionId(), transaction.getTransactionStatus(), transaction.getExternalTransactionId());

        } catch (PaymentException | SQLException e) {
            // If any exception occurs during adapter call or initial DAO save
            transaction.setTransactionStatus("FAILED_INTERNAL");
            transaction.setTransactionContent(e.getMessage());
            // Attempt to update status if transaction was already added, or add if it wasn't
            try {
                PaymentTransaction existing = paymentTransactionDAO.getById(transaction.getTransactionId());
                if(existing != null) { // Only update if it was successfully added before the exception
                    paymentTransactionDAO.updateStatus(transaction.getTransactionId(), transaction.getTransactionStatus(), transaction.getExternalTransactionId());
                } else {
                    // Do not add again if the initial add failed. Log or handle appropriately.
                    // paymentTransactionDAO.add(transaction); // This was causing the double add
                     System.err.println("Critical error: Failed to initially save payment transaction, and then failed again: " + transaction.getTransactionId() + " Error: " + e.getMessage());
                }
            } catch (SQLException ex) {
                System.err.println("Critical error: Failed to save/update payment transaction failure status: " + ex.getMessage());
                // This scenario needs robust logging and possibly alerting
            }
            if (e instanceof PaymentException) throw (PaymentException)e;
            if (e instanceof SQLException) throw (SQLException)e; // If initial add failed, this might be the original SQLException
            throw new PaymentException("Payment processing encountered an internal error: " + e.getMessage(), e);
        }
        return transaction;
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
        PaymentTransaction transaction = paymentTransactionDAO.getById(transactionId);
        if (transaction == null) {
            throw new ResourceNotFoundException("Payment transaction with ID " + transactionId + " not found.");
        }
        // External transaction ID is preferred for querying gateway if available
        String queryId = (externalTransactionId != null && !externalTransactionId.isEmpty()) ? externalTransactionId : transaction.getExternalTransactionId();
        if (queryId == null || queryId.trim().isEmpty()){
             throw new ValidationException("External transaction ID is required to check status with gateway.");
        }


        Map<String, String> gatewayStatus = vnPayAdapter.queryTransactionStatus(queryId, transaction.getOrder().getOrderId(), transaction.getTransactionDateTime());

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


        paymentTransactionDAO.updateStatus(transaction.getTransactionId(), transaction.getTransactionStatus(), transaction.getExternalTransactionId());
        return transaction;
    }

    // Assume IOrderEntityDAO is available if needed, e.g. for fetching OrderEntity for refund context
    // This would typically be injected if needed by the service.
    private IOrderEntityDAO orderDAO;
    public void setOrderDAO(IOrderEntityDAO orderDAO) { // For setter injection if needed
        this.orderDAO = orderDAO;
    }
}