package com.aims.core.utils;

import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayAdapterImpl;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayConfig;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.CardDetails;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;

/**
 * VNPay Integration Test Utility
 * 
 * This utility helps test VNPay integration with the test environment credentials:
 * - Terminal ID: YFW5M6GN
 * - Hash Secret: 3RCPI4281FRSY2W6P3E9QD3JZJICJB5M
 * - Test Card: 9704198526191432198 (NCB Bank)
 * - OTP: 123456
 * 
 * Use this class to:
 * 1. Test payment URL generation
 * 2. Validate signature generation/verification
 * 3. Test IPN handling
 * 4. Query transaction status
 */
public class VNPayTestUtility {
    
    private static final VNPayAdapterImpl vnPayAdapter = new VNPayAdapterImpl();
    
    public static void main(String[] args) {
        System.out.println("=== AIMS VNPay Integration Test Utility ===");
        System.out.println("VNPay Sandbox Environment Configuration:");
        System.out.println("Terminal ID: " + VNPayConfig.VNP_TMN_CODE);
        System.out.println("Payment URL: " + VNPayConfig.VNP_PAY_URL);
        System.out.println("API URL: " + VNPayConfig.VNP_API_URL);
        System.out.println("Return URL: " + VNPayConfig.VNP_RETURN_URL);
        System.out.println();
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            showMenu();
            System.out.print("Choose an option (1-6): ");
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    testPaymentUrlGeneration(scanner);
                    break;
                case "2":
                    testSignatureValidation(scanner);
                    break;
                case "3":
                    testIPNHandling(scanner);
                    break;
                case "4":
                    testTransactionQuery(scanner);
                    break;
                case "5":
                    showTestCardInfo();
                    break;
                case "6":
                    System.out.println("Exiting VNPay Test Utility. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
            
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }
    
    private static void showMenu() {
        System.out.println("\n=== VNPay Test Menu ===");
        System.out.println("1. Test Payment URL Generation");
        System.out.println("2. Test Signature Validation");
        System.out.println("3. Test IPN Handling");
        System.out.println("4. Test Transaction Query");
        System.out.println("5. Show Test Card Information");
        System.out.println("6. Exit");
        System.out.println("========================");
    }
    
    /**
     * Test 1: Generate VNPay payment URL
     */
    private static void testPaymentUrlGeneration(Scanner scanner) {
        System.out.println("\n=== Testing Payment URL Generation ===");
        
        try {
            // Create test order
            OrderEntity testOrder = createTestOrder(scanner);
            
            // Create payment method
            PaymentMethod paymentMethod = new PaymentMethod();
            paymentMethod.setMethodType(PaymentMethodType.DOMESTIC_DEBIT_CARD);
            
            // Generate payment parameters
            Map<String, Object> paymentParams = vnPayAdapter.preparePaymentParameters(testOrder, paymentMethod, null);
            
            // Add bank code for domestic card
            System.out.print("Enter Bank Code (e.g., NCB, VCB, TCB) or press Enter for NCB: ");
            String bankCode = scanner.nextLine().trim();
            if (bankCode.isEmpty()) {
                bankCode = "NCB";
            }
            paymentParams.put("vnp_BankCode", bankCode);
            
            // Process payment to get URL
            Map<String, String> result = vnPayAdapter.processPayment(paymentParams);
            
            System.out.println("\n✓ Payment URL generated successfully!");
            System.out.println("Payment URL: " + result.get("paymentUrl"));
            System.out.println("Transaction Reference: " + result.get("vnp_TxnRef"));
            System.out.println("\nYou can copy this URL and open it in a browser to test the payment flow.");
            System.out.println("Use test card: 9704198526191432198 (NCB) with OTP: 123456");
            
        } catch (Exception e) {
            System.err.println("✗ Error generating payment URL: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test 2: Validate VNPay signature
     */
    private static void testSignatureValidation(Scanner scanner) {
        System.out.println("\n=== Testing Signature Validation ===");
        
        // Create sample response parameters
        Map<String, String> responseParams = new HashMap<>();
        responseParams.put("vnp_Amount", "100000");
        responseParams.put("vnp_BankCode", "NCB");
        responseParams.put("vnp_CardType", "ATM");
        responseParams.put("vnp_OrderInfo", "Test payment");
        responseParams.put("vnp_PayDate", "20250604140000");
        responseParams.put("vnp_ResponseCode", "00");
        responseParams.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        responseParams.put("vnp_TransactionNo", "14400996");
        responseParams.put("vnp_TxnRef", "TEST_ORDER_123");
        
        // Generate signature
        String hashData = VNPayConfig.hashAllFields(responseParams);
        String signature = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData);
        responseParams.put("vnp_SecureHash", signature);
        
        System.out.println("Sample response parameters:");
        responseParams.forEach((key, value) -> {
            if (!"vnp_SecureHash".equals(key)) {
                System.out.println("  " + key + ": " + value);
            }
        });
        System.out.println("  vnp_SecureHash: " + signature);
        
        // Validate signature
        boolean isValid = vnPayAdapter.validateResponseSignature(responseParams);
        
        if (isValid) {
            System.out.println("\n✓ Signature validation successful!");
        } else {
            System.out.println("\n✗ Signature validation failed!");
        }
    }
    
    /**
     * Test 3: Test IPN handling
     */
    private static void testIPNHandling(Scanner scanner) {
        System.out.println("\n=== Testing IPN Handling ===");
        
        // This would typically be called by VNPay's server
        // For testing, we simulate the IPN parameters
        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_Amount", "100000");
        ipnParams.put("vnp_BankCode", "NCB");
        ipnParams.put("vnp_CardType", "ATM");
        ipnParams.put("vnp_OrderInfo", "Test IPN");
        ipnParams.put("vnp_PayDate", "20250604140000");
        ipnParams.put("vnp_ResponseCode", "00");
        ipnParams.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        ipnParams.put("vnp_TransactionNo", "14400996");
        ipnParams.put("vnp_TxnRef", "TEST_ORDER_123_1733328000000");
        
        // Generate signature for IPN
        String hashData = VNPayConfig.hashAllFields(ipnParams);
        String signature = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData);
        ipnParams.put("vnp_SecureHash", signature);
        
        System.out.println("Simulated IPN parameters:");
        ipnParams.forEach((key, value) -> System.out.println("  " + key + ": " + value));
        
        // Note: In real implementation, you would call VNPayIPNController here
        System.out.println("\n✓ IPN parameters prepared successfully!");
        System.out.println("In real implementation, these parameters would be sent to VNPayIPNController.handleIPN()");
    }
    
    /**
     * Test 4: Test transaction query
     */
    private static void testTransactionQuery(Scanner scanner) {
        System.out.println("\n=== Testing Transaction Query ===");
        
        System.out.print("Enter transaction reference to query (or press Enter for test): ");
        String txnRef = scanner.nextLine().trim();
        if (txnRef.isEmpty()) {
            txnRef = "TEST_ORDER_123_1733328000000";
        }
        
        System.out.print("Enter order ID (or press Enter for test): ");
        String orderId = scanner.nextLine().trim();
        if (orderId.isEmpty()) {
            orderId = "TEST_ORDER_123";
        }
        
        try {
            LocalDateTime transactionDate = LocalDateTime.now().minusHours(1);
            Map<String, String> queryResult = vnPayAdapter.queryTransactionStatus(txnRef, orderId, transactionDate);
            
            System.out.println("\n✓ Transaction query completed:");
            queryResult.forEach((key, value) -> System.out.println("  " + key + ": " + value));
            
        } catch (Exception e) {
            System.err.println("✗ Error querying transaction: " + e.getMessage());
        }
    }
    
    /**
     * Show test card information
     */
    private static void showTestCardInfo() {
        System.out.println("\n=== VNPay Test Environment Information ===");
        System.out.println();
        System.out.println("🏦 Test Card Information:");
        System.out.println("  Bank: NCB (National Citizen Bank)");
        System.out.println("  Card Number: 9704198526191432198");
        System.out.println("  Cardholder Name: NGUYEN VAN A");
        System.out.println("  Issue Date: 07/15");
        System.out.println("  OTP: 123456");
        System.out.println();
        System.out.println("🌐 VNPay Demo Site:");
        System.out.println("  URL: https://sandbox.vnpayment.vn/apis/vnpay-demo/");
        System.out.println();
        System.out.println("🔧 Merchant Admin Access:");
        System.out.println("  URL: https://sandbox.vnpayment.vn/merchantv2/");
        System.out.println("  Username: nam.pt225989@sis.hust.edu.vn");
        System.out.println("  Password: xCMT##UQGJeB52z");
        System.out.println();
        System.out.println("🧪 System Integration Testing:");
        System.out.println("  URL: https://sandbox.vnpayment.vn/vnpaygw-sit-testing/user/login");
        System.out.println("  Username: nam.pt225989@sis.hust.edu.vn");
        System.out.println("  Password: xCMT##UQGJeB52z");
        System.out.println();
        System.out.println("📚 Documentation:");
        System.out.println("  Payment API: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html");
        System.out.println("  Query & Refund: https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr&refund.html");
        System.out.println("  Code Demo: https://sandbox.vnpayment.vn/apis/vnpay-demo/code-demo-tích-hợp");
    }
    
    /**
     * Create a test order for testing
     */
    private static OrderEntity createTestOrder(Scanner scanner) {
        System.out.print("Enter order ID (or press Enter for auto-generated): ");
        String orderId = scanner.nextLine().trim();
        if (orderId.isEmpty()) {
            orderId = "TEST_ORDER_" + System.currentTimeMillis();
        }
        
        System.out.print("Enter order amount in VND (or press Enter for 1000): ");
        String amountStr = scanner.nextLine().trim();
        float amount = 1000;
        if (!amountStr.isEmpty()) {
            try {
                amount = Float.parseFloat(amountStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount, using default: 1000 VND");
            }
        }
        
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        order.setTotalAmountPaid(amount);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        
        System.out.println("Created test order: " + orderId + " with amount: " + amount + " VND");
        return order;
    }
}
