package com.aims.core.infrastructure.adapters.external.payment_gateway;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.CardDetails;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Stub implementation of IVNPayAdapter for testing and development purposes.
 * This implementation simulates VNPay payment gateway operations without actual external calls.
 * All methods log their inputs and return mock successful responses.
 */
public class StubVNPayAdapter implements IVNPayAdapter {

    @Override
    public Map<String, Object> preparePaymentParameters(OrderEntity order, PaymentMethod paymentMethod, CardDetails cardDetails) throws ValidationException {
        System.out.println("=== STUB VNPay Payment Preparation ===");
        System.out.println("Order ID: " + (order != null ? order.getOrderId() : "null"));
        System.out.println("Order Total: " + (order != null ? order.getTotalAmountPaid() : "null"));
        System.out.println("Payment Method: " + (paymentMethod != null ? paymentMethod.getPaymentMethodId() : "null"));
        System.out.println("Card Details: " + (cardDetails != null ? "****" + cardDetails.getCardNumberMasked().substring(cardDetails.getCardNumberMasked().length() - 4) : "null"));
        
        // Return mock payment parameters
        Map<String, Object> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "STUBMERCHANT");
        params.put("vnp_Amount", order != null ? (int)(order.getTotalAmountPaid() * 100) : 0); // VNPay uses cents
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", "STUB_" + (order != null ? order.getOrderId() : "TEST"));
        params.put("vnp_OrderInfo", "Stub payment for order " + (order != null ? order.getOrderId() : "TEST"));
        params.put("vnp_CreateDate", LocalDateTime.now().toString());
        params.put("vnp_ExpireDate", LocalDateTime.now().plusMinutes(30).toString());
        
        System.out.println("Payment parameters prepared successfully");
        return params;
    }

    @Override
    public Map<String, Object> prepareRefundParameters(OrderEntity order, String originalGatewayTransactionId, float refundAmount, String reason) throws ValidationException {
        System.out.println("=== STUB VNPay Refund Preparation ===");
        System.out.println("Order ID: " + (order != null ? order.getOrderId() : "null"));
        System.out.println("Original Transaction ID: " + originalGatewayTransactionId);
        System.out.println("Refund Amount: " + refundAmount);
        System.out.println("Reason: " + reason);
        
        // Return mock refund parameters
        Map<String, Object> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "refund");
        params.put("vnp_TmnCode", "STUBMERCHANT");
        params.put("vnp_Amount", (int)(refundAmount * 100)); // VNPay uses cents
        params.put("vnp_TransactionType", "02"); // Full refund
        params.put("vnp_TxnRef", originalGatewayTransactionId);
        params.put("vnp_OrderInfo", "Stub refund: " + reason);
        params.put("vnp_CreateDate", LocalDateTime.now().toString());
        
        System.out.println("Refund parameters prepared successfully");
        return params;
    }

    @Override
    public Map<String, String> processPayment(Map<String, Object> paymentParams) throws PaymentException {
        System.out.println("=== STUB VNPay Payment Processing ===");
        System.out.println("Processing payment with parameters:");
        paymentParams.forEach((key, value) -> System.out.println("  " + key + ": " + value));
        
        // Simulate successful payment response
        Map<String, String> response = new HashMap<>();
        response.put("vnp_ResponseCode", "00"); // Success code
        response.put("vnp_Message", "Successful");
        response.put("vnp_TxnRef", paymentParams.get("vnp_TxnRef").toString());
        response.put("vnp_TransactionNo", "STUB_TXN_" + System.currentTimeMillis());
        response.put("vnp_BankCode", "STUBBANK");
        response.put("vnp_PayDate", LocalDateTime.now().toString());
        response.put("vnp_Amount", paymentParams.get("vnp_Amount").toString());
        
        System.out.println("Payment processed successfully - Transaction ID: " + response.get("vnp_TransactionNo"));
        return response;
    }

    @Override
    public Map<String, String> processRefund(Map<String, Object> refundParams) throws PaymentException {
        System.out.println("=== STUB VNPay Refund Processing ===");
        System.out.println("Processing refund with parameters:");
        refundParams.forEach((key, value) -> System.out.println("  " + key + ": " + value));
        
        // Simulate successful refund response
        Map<String, String> response = new HashMap<>();
        response.put("vnp_ResponseCode", "00"); // Success code
        response.put("vnp_Message", "Successful");
        response.put("vnp_TxnRef", refundParams.get("vnp_TxnRef").toString());
        response.put("vnp_TransactionNo", "STUB_REFUND_" + System.currentTimeMillis());
        response.put("vnp_Amount", refundParams.get("vnp_Amount").toString());
        response.put("vnp_PayDate", LocalDateTime.now().toString());
        
        System.out.println("Refund processed successfully - Transaction ID: " + response.get("vnp_TransactionNo"));
        return response;
    }

    @Override
    public Map<String, String> queryTransactionStatus(String vnpTransactionRef, String aimsOrderId, LocalDateTime originalTransactionDate) throws PaymentException {
        System.out.println("=== STUB VNPay Transaction Status Query ===");
        System.out.println("VNP Transaction Ref: " + vnpTransactionRef);
        System.out.println("AIMS Order ID: " + aimsOrderId);
        System.out.println("Original Transaction Date: " + originalTransactionDate);
        
        // Simulate successful transaction status query
        Map<String, String> response = new HashMap<>();
        response.put("vnp_ResponseCode", "00"); // Success code
        response.put("vnp_Message", "Successful");
        response.put("vnp_TxnRef", vnpTransactionRef);
        response.put("vnp_TransactionNo", "STUB_TXN_" + vnpTransactionRef.hashCode());
        response.put("vnp_TransactionStatus", "00"); // Success status
        response.put("vnp_Amount", "100000"); // Mock amount
        response.put("vnp_PayDate", originalTransactionDate.toString());
        
        System.out.println("Transaction status queried successfully - Status: Successful");
        return response;
    }

    @Override
    public boolean validateResponseSignature(Map<String, String> responseParams) {
        System.out.println("=== STUB VNPay Signature Validation ===");
        System.out.println("Validating response signature for parameters:");
        responseParams.forEach((key, value) -> System.out.println("  " + key + ": " + value));
        
        // In a real implementation, this would validate the HMAC signature
        // For stub purposes, we'll always return true (valid signature)
        System.out.println("Signature validation result: VALID (stub always returns true)");
        return true;
    }
}