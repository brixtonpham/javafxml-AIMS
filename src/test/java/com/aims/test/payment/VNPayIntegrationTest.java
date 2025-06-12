package com.aims.test.payment;

import com.aims.core.application.impl.PaymentServiceImpl;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.entities.CardDetails;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.TransactionType;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayAdapterImpl;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayConfig;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.infrastructure.database.SQLiteConnector;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Comprehensive VNPAY Integration Test Suite
 * Tests complete payment flow using sandbox credentials and test data
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VNPayIntegrationTest {

    private static final String TEST_CARD_NUMBER = "9704198526191432198"; // NCB Bank test card
    private static final String TEST_OTP = "123456";
    private static final String TEST_BANK_CODE = "NCB";
    
    private PaymentServiceImpl paymentService;
    private IVNPayAdapter vnPayAdapter;
    private IPaymentTransactionDAO paymentTransactionDAO;
    private IPaymentMethodDAO paymentMethodDAO;
    private ICardDetailsDAO cardDetailsDAO;
    private IOrderEntityDAO orderDAO;
    private Gson gson;
    
    @BeforeEach
    void setUp() throws SQLException {
        // Initialize components
        gson = new Gson();
        vnPayAdapter = new VNPayAdapterImpl();
        
        // Initialize DAOs
        IUserAccountDAO userAccountDAO = new UserAccountDAOImpl();
        IProductDAO productDAO = new ProductDAOImpl();
        IOrderItemDAO orderItemDAO = new OrderItemDAOImpl(productDAO);
        cardDetailsDAO = new CardDetailsDAOImpl();
        
        orderDAO = new OrderEntityDAOImpl(orderItemDAO, userAccountDAO);
        paymentMethodDAO = new PaymentMethodDAOImpl(userAccountDAO, cardDetailsDAO);
        paymentTransactionDAO = new PaymentTransactionDAOImpl(orderDAO, paymentMethodDAO);
        
        // Initialize PaymentService
        paymentService = new PaymentServiceImpl(
            paymentTransactionDAO,
            paymentMethodDAO,
            cardDetailsDAO,
            vnPayAdapter
        );
        paymentService.setOrderDAO(orderDAO);
        
        // Verify VNPAY configuration
        verifyVNPayConfiguration();
    }
    
    @Test
    @Order(1)
    @DisplayName("Test VNPAY Configuration Verification")
    void testVNPayConfigurationVerification() {
        System.out.println("=== Testing VNPAY Configuration ===");
        
        // Verify all required configuration values are present
        assertNotNull(VNPayConfig.VNP_TMN_CODE, "VNP_TMN_CODE should not be null");
        assertNotNull(VNPayConfig.VNP_HASH_SECRET, "VNP_HASH_SECRET should not be null");
        assertNotNull(VNPayConfig.VNP_PAY_URL, "VNP_PAY_URL should not be null");
        assertNotNull(VNPayConfig.VNP_API_URL, "VNP_API_URL should not be null");
        assertNotNull(VNPayConfig.VNP_RETURN_URL, "VNP_RETURN_URL should not be null");
        
        // Verify configuration values match expected sandbox values
        assertEquals("YFW5M6GN", VNPayConfig.VNP_TMN_CODE, "TMN Code should match sandbox credentials");
        assertEquals("3RCPI4281FRSY2W6P3E9QD3JZJICJB5M", VNPayConfig.VNP_HASH_SECRET, "Hash secret should match sandbox credentials");
        assertTrue(VNPayConfig.VNP_PAY_URL.contains("sandbox.vnpayment.vn"), "Should use sandbox URL");
        assertTrue(VNPayConfig.VNP_API_URL.contains("sandbox.vnpayment.vn"), "Should use sandbox API URL");
        
        System.out.println("✓ VNPAY configuration verified successfully");
        printConfigurationDetails();
    }
    
    @Test
    @Order(2)
    @DisplayName("Test Payment URL Generation with Valid Order")
    void testPaymentUrlGenerationWithValidOrder() throws Exception {
        System.out.println("=== Testing Payment URL Generation ===");
        
        // Create test order
        OrderEntity testOrder = createTestOrder(250000.0f); // 250,000 VND
        PaymentMethod vnpayMethod = createVNPayPaymentMethod();
        
        // Process payment to generate URL
        PaymentTransaction transaction = paymentService.processPayment(testOrder, vnpayMethod.getPaymentMethodId());
        
        // Verify transaction was created successfully
        assertNotNull(transaction, "Transaction should be created");
        assertEquals("PENDING_USER_ACTION", transaction.getTransactionStatus(), "Transaction should be pending user action");
        assertNotNull(transaction.getExternalTransactionId(), "External transaction ID should be set");
        assertNotNull(transaction.getGatewayResponseData(), "Gateway response data should be present");
        
        // Verify payment URL was generated
        JsonObject responseData = gson.fromJson(transaction.getGatewayResponseData(), JsonObject.class);
        assertTrue(responseData.has("paymentUrl"), "Response should contain payment URL");
        
        String paymentUrl = responseData.get("paymentUrl").getAsString();
        validatePaymentUrl(paymentUrl, testOrder);
        
        System.out.println("✓ Payment URL generated successfully");
        System.out.println("Payment URL: " + paymentUrl.substring(0, Math.min(100, paymentUrl.length())) + "...");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test VNPay Parameter Generation and Signature Validation")
    void testVNPayParameterGenerationAndSignature() throws Exception {
        System.out.println("=== Testing VNPay Parameter Generation and Signature ===");
        
        OrderEntity testOrder = createTestOrder(100000.0f);
        PaymentMethod paymentMethod = createVNPayPaymentMethod();
        paymentMethod.setMethodType(PaymentMethodType.DOMESTIC_DEBIT_CARD);
        
        // Test parameter preparation
        Map<String, Object> paymentParams = vnPayAdapter.preparePaymentParameters(testOrder, paymentMethod, null);
        
        // Verify required parameters
        assertTrue(paymentParams.containsKey("vnp_Version"), "Should contain version");
        assertTrue(paymentParams.containsKey("vnp_Command"), "Should contain command");
        assertTrue(paymentParams.containsKey("vnp_TmnCode"), "Should contain terminal code");
        assertTrue(paymentParams.containsKey("vnp_Amount"), "Should contain amount");
        assertTrue(paymentParams.containsKey("vnp_TxnRef"), "Should contain transaction reference");
        assertTrue(paymentParams.containsKey("vnp_OrderInfo"), "Should contain order info");
        assertTrue(paymentParams.containsKey("vnp_CreateDate"), "Should contain create date");
        assertTrue(paymentParams.containsKey("vnp_ExpireDate"), "Should contain expire date");
        
        // Verify amount calculation (VND amount * 100)
        assertEquals("10000000", paymentParams.get("vnp_Amount"), "Amount should be in cents");
        
        // Test payment processing (URL generation with signature)
        Map<String, String> result = vnPayAdapter.processPayment(paymentParams);
        
        assertNotNull(result.get("paymentUrl"), "Payment URL should be generated");
        assertNotNull(result.get("vnp_TxnRef"), "Transaction reference should be returned");
        
        // Extract and verify signature from URL
        String paymentUrl = result.get("paymentUrl");
        assertTrue(paymentUrl.contains("vnp_SecureHash="), "URL should contain secure hash");
        
        System.out.println("✓ VNPay parameters and signature generated successfully");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test Payment Flow with Domestic Card")
    void testPaymentFlowWithDomesticCard() throws Exception {
        System.out.println("=== Testing Payment Flow with Domestic Card ===");
        
        OrderEntity testOrder = createTestOrder(50000.0f);
        PaymentMethod domesticCard = createVNPayPaymentMethod();
        domesticCard.setMethodType(PaymentMethodType.DOMESTIC_DEBIT_CARD);
        
        // Process payment
        PaymentTransaction transaction = paymentService.processPayment(testOrder, domesticCard.getPaymentMethodId());
        
        // Verify transaction details
        assertEquals(TransactionType.PAYMENT, transaction.getTransactionType());
        assertEquals(50000.0f, transaction.getAmount());
        assertTrue(transaction.getExternalTransactionId().contains(testOrder.getOrderId()));
        
        // Verify gateway response contains payment URL
        JsonObject responseData = gson.fromJson(transaction.getGatewayResponseData(), JsonObject.class);
        String paymentUrl = responseData.get("paymentUrl").getAsString();
        
        // For domestic cards, URL should contain proper bank code or allow bank selection
        assertTrue(paymentUrl.contains("vnpayment.vn"), "Should be VNPay URL");
        assertTrue(paymentUrl.contains("vnp_Amount=5000000"), "Should contain correct amount");
        
        System.out.println("✓ Domestic card payment flow tested successfully");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test Payment Flow with Credit Card")
    void testPaymentFlowWithCreditCard() throws Exception {
        System.out.println("=== Testing Payment Flow with Credit Card ===");
        
        OrderEntity testOrder = createTestOrder(75000.0f);
        PaymentMethod creditCard = createVNPayPaymentMethod();
        creditCard.setMethodType(PaymentMethodType.CREDIT_CARD);
        
        // Process payment
        PaymentTransaction transaction = paymentService.processPayment(testOrder, creditCard.getPaymentMethodId());
        
        // Verify transaction details
        assertEquals("PENDING_USER_ACTION", transaction.getTransactionStatus());
        assertEquals(75000.0f, transaction.getAmount());
        
        // Verify gateway response
        JsonObject responseData = gson.fromJson(transaction.getGatewayResponseData(), JsonObject.class);
        String paymentUrl = responseData.get("paymentUrl").getAsString();
        
        // For credit cards, URL should contain INTCARD bank code
        assertTrue(paymentUrl.contains("vnp_BankCode=INTCARD") || !paymentUrl.contains("vnp_BankCode="), 
                  "Credit card should use INTCARD or no bank code");
        
        System.out.println("✓ Credit card payment flow tested successfully");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test Signature Validation with Valid Response")
    void testSignatureValidationWithValidResponse() {
        System.out.println("=== Testing Signature Validation ===");
        
        // Create test response parameters
        Map<String, String> responseParams = new HashMap<>();
        responseParams.put("vnp_Amount", "10000000");
        responseParams.put("vnp_BankCode", TEST_BANK_CODE);
        responseParams.put("vnp_CardType", "ATM");
        responseParams.put("vnp_OrderInfo", "Test payment validation");
        responseParams.put("vnp_PayDate", "20250604140000");
        responseParams.put("vnp_ResponseCode", "00");
        responseParams.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        responseParams.put("vnp_TransactionNo", "14400996");
        responseParams.put("vnp_TxnRef", "TEST_ORDER_" + System.currentTimeMillis());
        
        // Generate valid signature
        String hashData = VNPayConfig.hashAllFields(responseParams);
        String validSignature = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData);
        responseParams.put("vnp_SecureHash", validSignature);
        
        // Test valid signature
        boolean isValid = vnPayAdapter.validateResponseSignature(responseParams);
        assertTrue(isValid, "Valid signature should be accepted");
        
        // Test invalid signature
        responseParams.put("vnp_SecureHash", "invalid_signature");
        boolean isInvalid = vnPayAdapter.validateResponseSignature(responseParams);
        assertFalse(isInvalid, "Invalid signature should be rejected");
        
        // Test missing signature
        responseParams.remove("vnp_SecureHash");
        boolean isMissing = vnPayAdapter.validateResponseSignature(responseParams);
        assertFalse(isMissing, "Missing signature should be rejected");
        
        System.out.println("✓ Signature validation tested successfully");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test Callback Processing with Success Response")
    void testCallbackProcessingWithSuccessResponse() throws Exception {
        System.out.println("=== Testing Callback Processing ===");
        
        // Create initial transaction
        OrderEntity testOrder = createTestOrder(150000.0f);
        PaymentMethod paymentMethod = createVNPayPaymentMethod();
        PaymentTransaction transaction = paymentService.processPayment(testOrder, paymentMethod.getPaymentMethodId());
        
        // Simulate successful callback from VNPay
        String vnpTxnRef = transaction.getExternalTransactionId();
        String responseCode = "00"; // Success
        String externalTransactionId = "VNP_" + System.currentTimeMillis();
        String gatewayResponseData = createSuccessCallbackData();
        
        // Process callback
        PaymentTransaction updatedTransaction = paymentService.updateTransactionStatusFromCallback(
            vnpTxnRef, responseCode, externalTransactionId, gatewayResponseData
        );
        
        // Verify transaction was updated correctly
        assertEquals("SUCCESS", updatedTransaction.getTransactionStatus());
        assertEquals(externalTransactionId, updatedTransaction.getExternalTransactionId());
        assertNotNull(updatedTransaction.getGatewayResponseData());
        
        System.out.println("✓ Success callback processed successfully");
    }
    
    @Test
    @Order(8)
    @DisplayName("Test Callback Processing with Failure Response")
    void testCallbackProcessingWithFailureResponse() throws Exception {
        System.out.println("=== Testing Callback Processing with Failure ===");
        
        // Create initial transaction
        OrderEntity testOrder = createTestOrder(200000.0f);
        PaymentMethod paymentMethod = createVNPayPaymentMethod();
        PaymentTransaction transaction = paymentService.processPayment(testOrder, paymentMethod.getPaymentMethodId());
        
        // Simulate failed callback from VNPay
        String vnpTxnRef = transaction.getExternalTransactionId();
        String responseCode = "99"; // General failure
        String externalTransactionId = "VNP_FAIL_" + System.currentTimeMillis();
        String gatewayResponseData = createFailureCallbackData();
        
        // Process callback
        PaymentTransaction updatedTransaction = paymentService.updateTransactionStatusFromCallback(
            vnpTxnRef, responseCode, externalTransactionId, gatewayResponseData
        );
        
        // Verify transaction was updated to failed status
        assertEquals("FAILED", updatedTransaction.getTransactionStatus());
        assertEquals(externalTransactionId, updatedTransaction.getExternalTransactionId());
        
        System.out.println("✓ Failure callback processed successfully");
    }
    
    @Test
    @Order(9)
    @DisplayName("Test Payment Status Check")
    void testPaymentStatusCheck() throws Exception {
        System.out.println("=== Testing Payment Status Check ===");
        
        // Create and process payment
        OrderEntity testOrder = createTestOrder(100000.0f);
        PaymentMethod paymentMethod = createVNPayPaymentMethod();
        PaymentTransaction transaction = paymentService.processPayment(testOrder, paymentMethod.getPaymentMethodId());
        
        // Note: In real environment, this would query VNPay API
        // For testing, we expect the method to handle the query gracefully
        try {
            PaymentTransaction statusResult = paymentService.checkPaymentStatus(
                transaction.getTransactionId(), 
                transaction.getExternalTransactionId()
            );
            
            // If no exception thrown, verify the transaction is returned
            assertNotNull(statusResult);
            assertEquals(transaction.getTransactionId(), statusResult.getTransactionId());
            
            System.out.println("✓ Payment status check completed successfully");
            
        } catch (PaymentException e) {
            // Expected for sandbox environment without actual transaction
            assertTrue(e.getMessage().contains("Unable to check payment status") || 
                      e.getMessage().contains("network") || 
                      e.getMessage().contains("gateway"));
            System.out.println("✓ Payment status check handled gracefully (expected for test environment)");
        }
    }
    
    @Test
    @Order(10)
    @DisplayName("Test Error Handling for Invalid Orders")
    void testErrorHandlingForInvalidOrders() {
        System.out.println("=== Testing Error Handling ===");
        
        PaymentMethod validMethod = createVNPayPaymentMethod();
        
        // Test null order
        assertThrows(ValidationException.class, () -> 
            paymentService.processPayment(null, validMethod.getPaymentMethodId()),
            "Should reject null order"
        );
        
        // Test order with zero amount
        OrderEntity zeroAmountOrder = createTestOrder(0.0f);
        assertThrows(ValidationException.class, () -> 
            paymentService.processPayment(zeroAmountOrder, validMethod.getPaymentMethodId()),
            "Should reject zero amount order"
        );
        
        // Test order with negative amount
        OrderEntity negativeAmountOrder = createTestOrder(-100.0f);
        assertThrows(ValidationException.class, () -> 
            paymentService.processPayment(negativeAmountOrder, validMethod.getPaymentMethodId()),
            "Should reject negative amount order"
        );
        
        // Test null payment method ID
        OrderEntity validOrder = createTestOrder(100000.0f);
        assertThrows(ValidationException.class, () -> 
            paymentService.processPayment(validOrder, null),
            "Should reject null payment method ID"
        );
        
        // Test empty payment method ID
        assertThrows(ValidationException.class, () -> 
            paymentService.processPayment(validOrder, ""),
            "Should reject empty payment method ID"
        );
        
        System.out.println("✓ Error handling tested successfully");
    }
    
    // Helper Methods
    
    private void verifyVNPayConfiguration() {
        System.out.println("Verifying VNPAY Configuration...");
        assertNotNull(VNPayConfig.VNP_TMN_CODE, "TMN Code must be configured");
        assertFalse(VNPayConfig.VNP_TMN_CODE.contains("YOUR_TMN_CODE"), "TMN Code must not be placeholder");
        System.out.println("✓ VNPAY Configuration verified");
    }
    
    private void printConfigurationDetails() {
        System.out.println("VNPAY Configuration Details:");
        System.out.println("  TMN Code: " + VNPayConfig.VNP_TMN_CODE);
        System.out.println("  Pay URL: " + VNPayConfig.VNP_PAY_URL);
        System.out.println("  API URL: " + VNPayConfig.VNP_API_URL);
        System.out.println("  Return URL: " + VNPayConfig.VNP_RETURN_URL);
        System.out.println("  Version: " + VNPayConfig.VNP_VERSION);
    }
    
    private OrderEntity createTestOrder(float amount) {
        OrderEntity order = new OrderEntity();
        order.setOrderId("TEST_ORDER_" + System.currentTimeMillis());
        order.setTotalAmountPaid(amount);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        return order;
    }
    
    private PaymentMethod createVNPayPaymentMethod() {
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId("VNPAY_TEST_" + UUID.randomUUID().toString());
        method.setMethodType(PaymentMethodType.CREDIT_CARD);
        method.setUserAccount(null); // Temporary method
        method.setDefault(false);
        return method;
    }
    
    private void validatePaymentUrl(String paymentUrl, OrderEntity order) {
        assertNotNull(paymentUrl, "Payment URL should not be null");
        assertTrue(paymentUrl.startsWith("https://sandbox.vnpayment.vn"), "Should use sandbox URL");
        assertTrue(paymentUrl.contains("vnp_Amount="), "Should contain amount parameter");
        assertTrue(paymentUrl.contains("vnp_TxnRef="), "Should contain transaction reference");
        assertTrue(paymentUrl.contains("vnp_SecureHash="), "Should contain secure hash");
        assertTrue(paymentUrl.contains("vnp_TmnCode=" + VNPayConfig.VNP_TMN_CODE), "Should contain correct TMN code");
        
        // Verify amount is correctly formatted (amount * 100)
        String expectedAmount = String.valueOf((int)(order.getTotalAmountPaid() * 100));
        assertTrue(paymentUrl.contains("vnp_Amount=" + expectedAmount), 
                  "Should contain correct amount: " + expectedAmount);
    }
    
    private String createSuccessCallbackData() {
        Map<String, String> callbackData = new HashMap<>();
        callbackData.put("vnp_ResponseCode", "00");
        callbackData.put("vnp_Message", "Successful");
        callbackData.put("vnp_BankCode", TEST_BANK_CODE);
        callbackData.put("vnp_CardType", "ATM");
        callbackData.put("vnp_PayDate", "20250604140000");
        return gson.toJson(callbackData);
    }
    
    private String createFailureCallbackData() {
        Map<String, String> callbackData = new HashMap<>();
        callbackData.put("vnp_ResponseCode", "99");
        callbackData.put("vnp_Message", "Transaction failed");
        callbackData.put("vnp_BankCode", TEST_BANK_CODE);
        return gson.toJson(callbackData);
    }
}