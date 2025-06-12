package com.aims.test.utils;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.entities.CardDetails;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.enums.TransactionType;
import com.aims.core.enums.ProductType;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayConfig;
import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Factory class for creating test data for VNPay integration tests
 * Provides various test scenarios and data combinations
 */
public class VNPayTestDataFactory {
    
    private static final Gson gson = new Gson();
    private static final String TEST_CARD_NUMBER = "9704198526191432198"; // NCB Bank test card
    private static final String TEST_BANK_CODE = "NCB";
    private static final String TEST_OTP = "123456";
    
    // Test Order Scenarios
    
    /**
     * Create a test order with specified amount
     */
    public static OrderEntity createTestOrder(double amount) {
        return createTestOrder(amount, "TEST_ORDER_" + System.currentTimeMillis());
    }
    
    /**
     * Create a test order with specified amount and order ID
     */
    public static OrderEntity createTestOrder(double amount, String orderId) {
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        order.setTotalAmountPaid((float) amount);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        
        // Add some order items for realistic testing
        List<OrderItem> orderItems = createTestOrderItems(amount);
        order.setOrderItems(orderItems);
        
        return order;
    }
    
    /**
     * Create order for testing minimum amount (VNPay minimum is 5,000 VND)
     */
    public static OrderEntity createMinimumAmountOrder() {
        return createTestOrder(5000.0, "MIN_ORDER_" + System.currentTimeMillis());
    }
    
    /**
     * Create order for testing large amount
     */
    public static OrderEntity createLargeAmountOrder() {
        return createTestOrder(50000000.0, "LARGE_ORDER_" + System.currentTimeMillis()); // 50M VND
    }
    
    /**
     * Create order with rush delivery for testing rush order scenarios
     */
    public static OrderEntity createRushDeliveryOrder() {
        OrderEntity order = createTestOrder(150000.0, "RUSH_ORDER_" + System.currentTimeMillis());
        // Add rush delivery flag or metadata
        return order;
    }
    
    // Test Payment Method Scenarios
    
    /**
     * Create temporary VNPay payment method for testing
     */
    public static PaymentMethod createVNPayTempPaymentMethod() {
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId("VNPAY_TEMP_" + UUID.randomUUID().toString());
        method.setMethodType(PaymentMethodType.CREDIT_CARD);
        method.setUserAccount(null); // Temporary method
        method.setDefault(false);
        return method;
    }
    
    /**
     * Create domestic debit card payment method
     */
    public static PaymentMethod createDomesticDebitCardMethod() {
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId("VNPAY_DOMESTIC_" + UUID.randomUUID().toString());
        method.setMethodType(PaymentMethodType.DOMESTIC_DEBIT_CARD);
        method.setUserAccount(null);
        method.setDefault(false);
        return method;
    }
    
    /**
     * Create credit card payment method
     */
    public static PaymentMethod createCreditCardMethod() {
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId("VNPAY_CREDIT_" + UUID.randomUUID().toString());
        method.setMethodType(PaymentMethodType.CREDIT_CARD);
        method.setUserAccount(null);
        method.setDefault(false);
        return method;
    }
    
    /**
     * Create test card details for NCB bank
     */
    public static CardDetails createTestCardDetails(PaymentMethod paymentMethod) {
        CardDetails cardDetails = new CardDetails();
        cardDetails.setPaymentMethod(paymentMethod);
        cardDetails.setCardNumberMasked("9704********2198"); // Masked test card
        cardDetails.setCardholderName("NGUYEN VAN A");
        cardDetails.setExpiryDateMMYY("07/29");
        cardDetails.setIssuingBank(TEST_BANK_CODE);
        return cardDetails;
    }
    
    // VNPay Callback Test Data
    
    /**
     * Create successful VNPay callback parameters
     */
    public static Map<String, String> createSuccessCallback(PaymentTransaction transaction) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", String.valueOf((int)(transaction.getAmount() * 100)));
        params.put("vnp_BankCode", TEST_BANK_CODE);
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Test payment for order " + transaction.getOrder().getOrderId());
        params.put("vnp_PayDate", "20250604140000");
        params.put("vnp_ResponseCode", "00"); // Success
        params.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        params.put("vnp_TransactionNo", "VNP_SUCCESS_" + System.currentTimeMillis());
        params.put("vnp_TxnRef", transaction.getExternalTransactionId());
        
        // Generate valid signature
        String hashData = VNPayConfig.hashAllFields(params);
        String signature = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData);
        params.put("vnp_SecureHash", signature);
        
        return params;
    }
    
    /**
     * Create failed VNPay callback parameters
     */
    public static Map<String, String> createFailureCallback(PaymentTransaction transaction) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", String.valueOf((int)(transaction.getAmount() * 100)));
        params.put("vnp_BankCode", TEST_BANK_CODE);
        params.put("vnp_OrderInfo", "Test payment for order " + transaction.getOrder().getOrderId());
        params.put("vnp_PayDate", "20250604140000");
        params.put("vnp_ResponseCode", "99"); // General failure
        params.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        params.put("vnp_TransactionNo", "VNP_FAIL_" + System.currentTimeMillis());
        params.put("vnp_TxnRef", transaction.getExternalTransactionId());
        
        // Generate valid signature
        String hashData = VNPayConfig.hashAllFields(params);
        String signature = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData);
        params.put("vnp_SecureHash", signature);
        
        return params;
    }
    
    /**
     * Create cancelled VNPay callback parameters
     */
    public static Map<String, String> createCancelledCallback(PaymentTransaction transaction) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", String.valueOf((int)(transaction.getAmount() * 100)));
        params.put("vnp_BankCode", TEST_BANK_CODE);
        params.put("vnp_OrderInfo", "Test payment for order " + transaction.getOrder().getOrderId());
        params.put("vnp_PayDate", "20250604140000");
        params.put("vnp_ResponseCode", "24"); // Transaction cancelled
        params.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        params.put("vnp_TransactionNo", "VNP_CANCEL_" + System.currentTimeMillis());
        params.put("vnp_TxnRef", transaction.getExternalTransactionId());
        
        // Generate valid signature
        String hashData = VNPayConfig.hashAllFields(params);
        String signature = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData);
        params.put("vnp_SecureHash", signature);
        
        return params;
    }
    
    /**
     * Create callback with invalid signature for security testing
     */
    public static Map<String, String> createInvalidSignatureCallback(PaymentTransaction transaction) {
        Map<String, String> params = createSuccessCallback(transaction);
        params.put("vnp_SecureHash", "invalid_signature_for_testing");
        return params;
    }
    
    // Test Scenarios
    
    /**
     * Create test scenario for successful payment flow
     */
    public static TestScenario createSuccessfulPaymentScenario() {
        return new TestScenario(
            "Successful Payment Flow",
            "Test complete successful payment using NCB test card",
            createTestOrder(100000.0),
            createVNPayTempPaymentMethod(),
            createTestExpectedOutcome("SUCCESS", "Payment completed successfully")
        );
    }
    
    /**
     * Create test scenario for payment cancellation
     */
    public static TestScenario createPaymentCancellationScenario() {
        return new TestScenario(
            "Payment Cancellation Flow",
            "Test user cancellation at VNPay gateway",
            createTestOrder(75000.0),
            createVNPayTempPaymentMethod(),
            createTestExpectedOutcome("CANCELLED", "Payment cancelled by user")
        );
    }
    
    /**
     * Create test scenario for payment failure
     */
    public static TestScenario createPaymentFailureScenario() {
        return new TestScenario(
            "Payment Failure Flow", 
            "Test payment failure due to insufficient funds or card issues",
            createTestOrder(200000.0),
            createVNPayTempPaymentMethod(),
            createTestExpectedOutcome("FAILED", "Payment failed at gateway")
        );
    }
    
    /**
     * Create test scenario for minimum amount payment
     */
    public static TestScenario createMinimumAmountScenario() {
        return new TestScenario(
            "Minimum Amount Payment",
            "Test payment with minimum allowed amount",
            createMinimumAmountOrder(),
            createVNPayTempPaymentMethod(),
            createTestExpectedOutcome("SUCCESS", "Minimum amount payment successful")
        );
    }
    
    /**
     * Create test scenario for large amount payment
     */
    public static TestScenario createLargeAmountScenario() {
        return new TestScenario(
            "Large Amount Payment",
            "Test payment with large amount",
            createLargeAmountOrder(),
            createVNPayTempPaymentMethod(),
            createTestExpectedOutcome("SUCCESS", "Large amount payment successful")
        );
    }
    
    // Helper Methods
    
    private static List<OrderItem> createTestOrderItems(double totalAmount) {
        List<OrderItem> items = new ArrayList<>();
        
        // Create a few test products
        Product book = createTestProduct("TEST_BOOK_001", "Test Programming Book", 50000.0, ProductType.BOOK);
        Product cd = createTestProduct("TEST_CD_001", "Test Music CD", 30000.0, ProductType.CD);
        Product dvd = createTestProduct("TEST_DVD_001", "Test Movie DVD", 40000.0, ProductType.DVD);
        
        // Create order items that sum to approximately the total amount
        OrderItem item1 = new OrderItem();
        item1.setProduct(book);
        item1.setQuantity(1);
        item1.setPriceAtTimeOfOrder(book.getPrice());
        item1.setEligibleForRushDelivery(false);
        items.add(item1);
        
        if (totalAmount > 50000) {
            OrderItem item2 = new OrderItem();
            item2.setProduct(cd);
            item2.setQuantity(1);
            item2.setPriceAtTimeOfOrder(cd.getPrice());
            item2.setEligibleForRushDelivery(false);
            items.add(item2);
        }
        
        if (totalAmount > 80000) {
            OrderItem item3 = new OrderItem();
            item3.setProduct(dvd);
            item3.setQuantity(1);
            item3.setPriceAtTimeOfOrder(dvd.getPrice());
            item3.setEligibleForRushDelivery(false);
            items.add(item3);
        }
        
        return items;
    }
    
    private static Product createTestProduct(String productId, String title, double price, ProductType type) {
        Product product = new Product();
        product.setProductId(productId);
        product.setTitle(title);
        product.setPrice((float) price);
        product.setProductType(type);
        product.setQuantityInStock(100); // Sufficient stock
        product.setWeightKg(0.5f); // 500g
        return product;
    }
    
    private static TestExpectedOutcome createTestExpectedOutcome(String expectedStatus, String expectedMessage) {
        return new TestExpectedOutcome(expectedStatus, expectedMessage);
    }
    
    // Test Data Classes
    
    /**
     * Test scenario data structure
     */
    public static class TestScenario {
        private final String name;
        private final String description;
        private final OrderEntity order;
        private final PaymentMethod paymentMethod;
        private final TestExpectedOutcome expectedOutcome;
        
        public TestScenario(String name, String description, OrderEntity order, 
                           PaymentMethod paymentMethod, TestExpectedOutcome expectedOutcome) {
            this.name = name;
            this.description = description;
            this.order = order;
            this.paymentMethod = paymentMethod;
            this.expectedOutcome = expectedOutcome;
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public OrderEntity getOrder() { return order; }
        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        public TestExpectedOutcome getExpectedOutcome() { return expectedOutcome; }
    }
    
    /**
     * Expected test outcome data structure
     */
    public static class TestExpectedOutcome {
        private final String expectedStatus;
        private final String expectedMessage;
        
        public TestExpectedOutcome(String expectedStatus, String expectedMessage) {
            this.expectedStatus = expectedStatus;
            this.expectedMessage = expectedMessage;
        }
        
        // Getters
        public String getExpectedStatus() { return expectedStatus; }
        public String getExpectedMessage() { return expectedMessage; }
    }
    
    // Test Environment Information
    
    /**
     * Get VNPay test environment information
     */
    public static Map<String, String> getTestEnvironmentInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("TEST_CARD_NUMBER", TEST_CARD_NUMBER);
        info.put("TEST_BANK_CODE", TEST_BANK_CODE);
        info.put("TEST_OTP", TEST_OTP);
        info.put("TEST_CARDHOLDER_NAME", "NGUYEN VAN A");
        info.put("TEST_ISSUE_DATE", "07/15");
        info.put("SANDBOX_URL", "https://sandbox.vnpayment.vn");
        info.put("DEMO_URL", "https://sandbox.vnpayment.vn/apis/vnpay-demo/");
        info.put("MERCHANT_ADMIN_URL", "https://sandbox.vnpayment.vn/merchantv2/");
        return info;
    }
    
    /**
     * Print test environment information for manual testing
     */
    public static void printTestEnvironmentInfo() {
        System.out.println("=== VNPay Test Environment Information ===");
        Map<String, String> info = getTestEnvironmentInfo();
        info.forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("===========================================");
    }
    
    /**
     * Create test data cleanup helper
     */
    public static List<String> getTestDataCleanupQueries() {
        List<String> queries = new ArrayList<>();
        queries.add("DELETE FROM PAYMENT_TRANSACTION WHERE transactionID LIKE 'PAY-%' OR transactionID LIKE 'REF-%'");
        queries.add("DELETE FROM CARD_DETAILS WHERE paymentMethodID LIKE 'VNPAY_%'");
        queries.add("DELETE FROM PAYMENT_METHOD WHERE paymentMethodID LIKE 'VNPAY_%'");
        queries.add("DELETE FROM ORDER_ITEM WHERE orderID LIKE 'TEST_ORDER_%'");
        queries.add("DELETE FROM ORDERS WHERE orderID LIKE 'TEST_ORDER_%'");
        return queries;
    }
}