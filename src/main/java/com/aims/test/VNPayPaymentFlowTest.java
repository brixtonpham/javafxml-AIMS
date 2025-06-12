package com.aims.test;

import com.aims.core.application.impl.PaymentServiceImpl;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.enums.TransactionType;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayAdapterImpl;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.shared.ServiceFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test class to verify VNPAY payment flow integration
 */
public class VNPayPaymentFlowTest {

    public static void main(String[] args) {
        System.out.println("=== VNPAY Payment Flow Integration Test ===");
        
        try {
            // Test 1: Create a test order
            OrderEntity testOrder = createTestOrder();
            System.out.println("✓ Test order created: " + testOrder.getOrderId());
            
            // Test 2: Create temporary PaymentMethod for VNPAY
            PaymentMethod vnpayPaymentMethod = createVNPayPaymentMethod();
            System.out.println("✓ VNPAY payment method created: " + vnpayPaymentMethod.getPaymentMethodId());
            
            // Test 3: Initialize PaymentService
            PaymentServiceImpl paymentService = initializePaymentService();
            System.out.println("✓ PaymentService initialized");
            
            // Test 4: Process payment (generate payment URL)
            PaymentTransaction transaction = paymentService.processPayment(testOrder, vnpayPaymentMethod.getPaymentMethodId());
            System.out.println("✓ Payment processing completed");
            
            // Test 5: Verify transaction status and payment URL
            verifyPaymentTransaction(transaction);
            
            System.out.println("\n=== VNPAY Payment Flow Test COMPLETED SUCCESSFULLY ===");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static OrderEntity createTestOrder() {
        OrderEntity order = new OrderEntity();
        order.setOrderId("TEST_ORDER_" + System.currentTimeMillis());
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmountPaid(250000.0f); // 250,000 VND
        return order;
    }

    private static PaymentMethod createVNPayPaymentMethod() {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setPaymentMethodId("VNPAY_TEST_" + UUID.randomUUID().toString());
        paymentMethod.setMethodType(PaymentMethodType.CREDIT_CARD);
        paymentMethod.setUserAccount(null); // Temporary method
        paymentMethod.setDefault(false);
        return paymentMethod;
    }

    private static PaymentServiceImpl initializePaymentService() {
        // Get PaymentService from ServiceFactory (already configured with real VNPayAdapterImpl)
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        return (PaymentServiceImpl) serviceFactory.getPaymentService();
    }

    private static void verifyPaymentTransaction(PaymentTransaction transaction) {
        System.out.println("\n--- Payment Transaction Verification ---");
        System.out.println("Transaction ID: " + transaction.getTransactionId());
        System.out.println("Transaction Status: " + transaction.getTransactionStatus());
        System.out.println("Transaction Type: " + transaction.getTransactionType());
        System.out.println("Amount: " + transaction.getAmount() + " VND");
        System.out.println("External Transaction ID: " + transaction.getExternalTransactionId());
        
        // Verify transaction status
        if (!"PENDING_USER_ACTION".equals(transaction.getTransactionStatus())) {
            throw new RuntimeException("Expected transaction status to be PENDING_USER_ACTION, but was: " + transaction.getTransactionStatus());
        }
        System.out.println("✓ Transaction status is correct: PENDING_USER_ACTION");
        
        // Verify gateway response data contains payment URL
        String gatewayResponseData = transaction.getGatewayResponseData();
        if (gatewayResponseData == null || gatewayResponseData.trim().isEmpty()) {
            throw new RuntimeException("Gateway response data is null or empty");
        }
        System.out.println("✓ Gateway response data is present");
        
        // Parse and verify payment URL
        try {
            Gson gson = new Gson();
            JsonObject responseJson = gson.fromJson(gatewayResponseData, JsonObject.class);
            
            if (!responseJson.has("paymentUrl")) {
                throw new RuntimeException("Gateway response data does not contain paymentUrl");
            }
            
            String paymentUrl = responseJson.get("paymentUrl").getAsString();
            if (paymentUrl == null || paymentUrl.trim().isEmpty()) {
                throw new RuntimeException("Payment URL is null or empty");
            }
            
            // Verify URL contains VNPay domain
            if (!paymentUrl.contains("vnpayment.vn")) {
                throw new RuntimeException("Payment URL does not contain VNPay domain: " + paymentUrl);
            }
            
            System.out.println("✓ Payment URL is valid: " + paymentUrl.substring(0, Math.min(100, paymentUrl.length())) + "...");
            
            // Verify URL contains required parameters
            if (!paymentUrl.contains("vnp_Amount") || !paymentUrl.contains("vnp_TxnRef") || !paymentUrl.contains("vnp_SecureHash")) {
                throw new RuntimeException("Payment URL is missing required VNPay parameters");
            }
            
            System.out.println("✓ Payment URL contains required VNPay parameters");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse gateway response data: " + e.getMessage(), e);
        }
        
        System.out.println("✓ All payment transaction verifications passed");
    }
}