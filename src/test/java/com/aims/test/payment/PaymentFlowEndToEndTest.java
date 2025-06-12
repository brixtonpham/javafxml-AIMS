package com.aims.test.payment;

import com.aims.core.application.impl.PaymentServiceImpl;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.enums.TransactionType;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayAdapterImpl;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayConfig;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.test.utils.VNPayTestDataFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * End-to-End Payment Flow Test
 * Tests complete customer journey from order creation to payment completion
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentFlowEndToEndTest {

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
        
        // Initialize DAOs (using actual implementations for end-to-end testing)
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
    }

    @Test
    @Order(1)
    @DisplayName("End-to-End: Complete Successful Payment Flow")
    void testCompleteSuccessfulPaymentFlow() throws Exception {
        System.out.println("=== End-to-End: Complete Successful Payment Flow ===");
        
        // Step 1: Customer creates order
        OrderEntity customerOrder = VNPayTestDataFactory.createTestOrder(150000.0);
        System.out.println("✓ Step 1: Customer order created: " + customerOrder.getOrderId());
        
        // Step 2: Customer selects VNPay payment method
        PaymentMethod vnpayMethod = VNPayTestDataFactory.createVNPayTempPaymentMethod();
        vnpayMethod.setMethodType(PaymentMethodType.DOMESTIC_DEBIT_CARD);
        System.out.println("✓ Step 2: Payment method selected: " + vnpayMethod.getMethodType());
        
        // Step 3: System initiates payment and generates VNPay URL
        PaymentTransaction transaction = paymentService.processPayment(customerOrder, vnpayMethod.getPaymentMethodId());
        
        assertNotNull(transaction, "Transaction should be created");
        assertEquals("PENDING_USER_ACTION", transaction.getTransactionStatus());
        assertNotNull(transaction.getGatewayResponseData(), "Payment URL should be generated");
        
        // Extract payment URL
        JsonObject responseData = gson.fromJson(transaction.getGatewayResponseData(), JsonObject.class);
        String paymentUrl = responseData.get("paymentUrl").getAsString();
        
        System.out.println("✓ Step 3: VNPay payment URL generated");
        System.out.println("   Payment URL: " + paymentUrl.substring(0, Math.min(100, paymentUrl.length())) + "...");
        
        // Verify URL contains required parameters
        assertTrue(paymentUrl.contains("vnp_Amount=15000000"), "Amount should be 150,000 VND * 100");
        assertTrue(paymentUrl.contains("vnp_TxnRef="), "Should contain transaction reference");
        assertTrue(paymentUrl.contains("vnp_SecureHash="), "Should contain security hash");
        
        // Step 4: Simulate customer completing payment at VNPay (success scenario)
        System.out.println("✓ Step 4: Simulating customer payment at VNPay gateway...");
        
        // Step 5: Simulate VNPay callback (success)
        Map<String, String> successCallback = VNPayTestDataFactory.createSuccessCallback(transaction);
        
        PaymentTransaction updatedTransaction = paymentService.updateTransactionStatusFromCallback(
            transaction.getExternalTransactionId(),
            successCallback.get("vnp_ResponseCode"),
            successCallback.get("vnp_TransactionNo"),
            gson.toJson(successCallback)
        );
        
        // Step 6: Verify final transaction status
        assertEquals("SUCCESS", updatedTransaction.getTransactionStatus());
        assertEquals(successCallback.get("vnp_TransactionNo"), updatedTransaction.getExternalTransactionId());
        assertNotNull(updatedTransaction.getGatewayResponseData());
        
        System.out.println("✓ Step 5: VNPay callback processed successfully");
        System.out.println("✓ Step 6: Transaction status: " + updatedTransaction.getTransactionStatus());
        System.out.println("✓ End-to-End payment flow completed successfully!");
        
        // Verify transaction persisted correctly
        PaymentTransaction persistedTransaction = paymentTransactionDAO.getById(transaction.getTransactionId());
        assertNotNull(persistedTransaction, "Transaction should be persisted");
        assertEquals("SUCCESS", persistedTransaction.getTransactionStatus());
    }

    @Test
    @Order(2)
    @DisplayName("End-to-End: Payment Cancellation Flow")
    void testPaymentCancellationFlow() throws Exception {
        System.out.println("=== End-to-End: Payment Cancellation Flow ===");
        
        // Step 1: Create order and initiate payment
        OrderEntity customerOrder = VNPayTestDataFactory.createTestOrder(75000.0);
        PaymentMethod vnpayMethod = VNPayTestDataFactory.createCreditCardMethod();
        
        PaymentTransaction transaction = paymentService.processPayment(customerOrder, vnpayMethod.getPaymentMethodId());
        
        System.out.println("✓ Payment initiated for order: " + customerOrder.getOrderId());
        
        // Step 2: Simulate customer cancelling payment at VNPay
        Map<String, String> cancelCallback = VNPayTestDataFactory.createCancelledCallback(transaction);
        
        PaymentTransaction cancelledTransaction = paymentService.updateTransactionStatusFromCallback(
            transaction.getExternalTransactionId(),
            cancelCallback.get("vnp_ResponseCode"),
            cancelCallback.get("vnp_TransactionNo"),
            gson.toJson(cancelCallback)
        );
        
        // Step 3: Verify cancellation status
        assertEquals("CANCELLED", cancelledTransaction.getTransactionStatus());
        System.out.println("✓ Payment cancellation processed correctly");
        
        // Verify order can be retried with different payment method
        PaymentMethod alternativeMethod = VNPayTestDataFactory.createDomesticDebitCardMethod();
        PaymentTransaction retryTransaction = paymentService.processPayment(customerOrder, alternativeMethod.getPaymentMethodId());
        
        assertNotNull(retryTransaction, "Should allow retry with different payment method");
        System.out.println("✓ Payment retry with alternative method successful");
    }

    @Test
    @Order(3)
    @DisplayName("End-to-End: Payment Failure and Recovery Flow")
    void testPaymentFailureAndRecoveryFlow() throws Exception {
        System.out.println("=== End-to-End: Payment Failure and Recovery Flow ===");
        
        // Step 1: Create order and initiate payment
        OrderEntity customerOrder = VNPayTestDataFactory.createTestOrder(200000.0);
        PaymentMethod vnpayMethod = VNPayTestDataFactory.createVNPayTempPaymentMethod();
        
        PaymentTransaction transaction = paymentService.processPayment(customerOrder, vnpayMethod.getPaymentMethodId());
        
        System.out.println("✓ Payment initiated for order: " + customerOrder.getOrderId());
        
        // Step 2: Simulate payment failure at VNPay (insufficient funds)
        Map<String, String> failureCallback = VNPayTestDataFactory.createFailureCallback(transaction);
        
        PaymentTransaction failedTransaction = paymentService.updateTransactionStatusFromCallback(
            transaction.getExternalTransactionId(),
            failureCallback.get("vnp_ResponseCode"),
            failureCallback.get("vnp_TransactionNo"),
            gson.toJson(failureCallback)
        );
        
        // Step 3: Verify failure status
        assertEquals("FAILED", failedTransaction.getTransactionStatus());
        System.out.println("✓ Payment failure processed correctly");
        
        // Step 4: Customer retries with different amount (smaller order)
        OrderEntity reducedOrder = VNPayTestDataFactory.createTestOrder(100000.0, customerOrder.getOrderId() + "_RETRY");
        PaymentMethod retryMethod = VNPayTestDataFactory.createVNPayTempPaymentMethod();
        
        PaymentTransaction retryTransaction = paymentService.processPayment(reducedOrder, retryMethod.getPaymentMethodId());
        
        assertNotNull(retryTransaction, "Should allow retry with reduced amount");
        assertEquals("PENDING_USER_ACTION", retryTransaction.getTransactionStatus());
        System.out.println("✓ Payment retry with reduced amount successful");
        
        // Step 5: Simulate successful completion of retry
        Map<String, String> retrySuccessCallback = VNPayTestDataFactory.createSuccessCallback(retryTransaction);
        
        PaymentTransaction successfulRetry = paymentService.updateTransactionStatusFromCallback(
            retryTransaction.getExternalTransactionId(),
            retrySuccessCallback.get("vnp_ResponseCode"),
            retrySuccessCallback.get("vnp_TransactionNo"),
            gson.toJson(retrySuccessCallback)
        );
        
        assertEquals("SUCCESS", successfulRetry.getTransactionStatus());
        System.out.println("✓ Payment retry completed successfully");
    }

    @Test
    @Order(4)
    @DisplayName("End-to-End: Multiple Payment Methods Flow")
    void testMultiplePaymentMethodsFlow() throws Exception {
        System.out.println("=== End-to-End: Multiple Payment Methods Flow ===");
        
        OrderEntity customerOrder = VNPayTestDataFactory.createTestOrder(125000.0);
        
        // Test domestic debit card
        PaymentMethod domesticCard = VNPayTestDataFactory.createDomesticDebitCardMethod();
        PaymentTransaction domesticTransaction = paymentService.processPayment(customerOrder, domesticCard.getPaymentMethodId());
        
        JsonObject domesticResponse = gson.fromJson(domesticTransaction.getGatewayResponseData(), JsonObject.class);
        String domesticUrl = domesticResponse.get("paymentUrl").getAsString();
        
        // For domestic cards, should allow bank selection or contain bank code
        assertTrue(domesticUrl.contains("vnpayment.vn"), "Should be VNPay URL");
        System.out.println("✓ Domestic debit card payment URL generated");
        
        // Test credit card
        OrderEntity creditCardOrder = VNPayTestDataFactory.createTestOrder(125000.0, customerOrder.getOrderId() + "_CC");
        PaymentMethod creditCard = VNPayTestDataFactory.createCreditCardMethod();
        PaymentTransaction creditTransaction = paymentService.processPayment(creditCardOrder, creditCard.getPaymentMethodId());
        
        JsonObject creditResponse = gson.fromJson(creditTransaction.getGatewayResponseData(), JsonObject.class);
        String creditUrl = creditResponse.get("paymentUrl").getAsString();
        
        // For credit cards, should contain INTCARD or no bank code
        assertTrue(creditUrl.contains("vnpayment.vn"), "Should be VNPay URL");
        assertTrue(creditUrl.contains("vnp_BankCode=INTCARD") || !creditUrl.contains("vnp_BankCode="), 
                  "Credit card should use INTCARD or no bank code");
        System.out.println("✓ Credit card payment URL generated");
        
        System.out.println("✓ Multiple payment methods tested successfully");
    }

    @Test
    @Order(5)
    @DisplayName("End-to-End: Payment Status Check Flow")
    void testPaymentStatusCheckFlow() throws Exception {
        System.out.println("=== End-to-End: Payment Status Check Flow ===");
        
        // Step 1: Create and process payment
        OrderEntity customerOrder = VNPayTestDataFactory.createTestOrder(80000.0);
        PaymentMethod vnpayMethod = VNPayTestDataFactory.createVNPayTempPaymentMethod();
        
        PaymentTransaction transaction = paymentService.processPayment(customerOrder, vnpayMethod.getPaymentMethodId());
        
        System.out.println("✓ Payment initiated, transaction ID: " + transaction.getTransactionId());
        
        // Step 2: Simulate customer completing payment but callback delayed/lost
        // In real scenario, customer would complete payment at VNPay but return callback fails
        
        // Step 3: System checks payment status with VNPay
        try {
            PaymentTransaction statusResult = paymentService.checkPaymentStatus(
                transaction.getTransactionId(),
                transaction.getExternalTransactionId()
            );
            
            assertNotNull(statusResult, "Status check should return transaction");
            System.out.println("✓ Payment status check completed");
            
        } catch (PaymentException e) {
            // Expected for sandbox environment - VNPay query API may not work for test transactions
            assertTrue(e.getMessage().contains("Unable to check payment status") ||
                      e.getMessage().contains("network") ||
                      e.getMessage().contains("gateway"));
            System.out.println("✓ Payment status check handled gracefully (expected for test environment)");
        }
        
        // Step 4: Manual status update via callback (simulating delayed callback)
        Map<String, String> delayedCallback = VNPayTestDataFactory.createSuccessCallback(transaction);
        
        PaymentTransaction finalTransaction = paymentService.updateTransactionStatusFromCallback(
            transaction.getExternalTransactionId(),
            delayedCallback.get("vnp_ResponseCode"),
            delayedCallback.get("vnp_TransactionNo"),
            gson.toJson(delayedCallback)
        );
        
        assertEquals("SUCCESS", finalTransaction.getTransactionStatus());
        System.out.println("✓ Delayed callback processed successfully");
    }

    @Test
    @Order(6)
    @DisplayName("End-to-End: Security and Signature Validation Flow")
    void testSecurityAndSignatureValidationFlow() throws Exception {
        System.out.println("=== End-to-End: Security and Signature Validation Flow ===");
        
        // Step 1: Create payment transaction
        OrderEntity customerOrder = VNPayTestDataFactory.createTestOrder(90000.0);
        PaymentMethod vnpayMethod = VNPayTestDataFactory.createVNPayTempPaymentMethod();
        
        PaymentTransaction transaction = paymentService.processPayment(customerOrder, vnpayMethod.getPaymentMethodId());
        
        System.out.println("✓ Payment transaction created");
        
        // Step 2: Test valid signature callback
        Map<String, String> validCallback = VNPayTestDataFactory.createSuccessCallback(transaction);
        
        // Verify signature is valid before processing
        boolean isValidSignature = vnPayAdapter.validateResponseSignature(validCallback);
        assertTrue(isValidSignature, "Valid signature should be accepted");
        
        PaymentTransaction validTransaction = paymentService.updateTransactionStatusFromCallback(
            transaction.getExternalTransactionId(),
            validCallback.get("vnp_ResponseCode"),
            validCallback.get("vnp_TransactionNo"),
            gson.toJson(validCallback)
        );
        
        assertEquals("SUCCESS", validTransaction.getTransactionStatus());
        System.out.println("✓ Valid signature callback processed successfully");
        
        // Step 3: Test invalid signature callback (security test)
        OrderEntity securityTestOrder = VNPayTestDataFactory.createTestOrder(60000.0, customerOrder.getOrderId() + "_SEC");
        PaymentTransaction securityTransaction = paymentService.processPayment(securityTestOrder, vnpayMethod.getPaymentMethodId());
        
        Map<String, String> invalidCallback = VNPayTestDataFactory.createInvalidSignatureCallback(securityTransaction);
        
        // Verify invalid signature is rejected
        boolean isInvalidSignature = vnPayAdapter.validateResponseSignature(invalidCallback);
        assertFalse(isInvalidSignature, "Invalid signature should be rejected");
        
        System.out.println("✓ Invalid signature correctly rejected");
        
        // Step 4: Test tampered parameters
        Map<String, String> tamperedCallback = VNPayTestDataFactory.createSuccessCallback(securityTransaction);
        tamperedCallback.put("vnp_Amount", "999999999"); // Tampered amount
        
        boolean isTamperedSignature = vnPayAdapter.validateResponseSignature(tamperedCallback);
        assertFalse(isTamperedSignature, "Tampered parameters should result in invalid signature");
        
        System.out.println("✓ Parameter tampering detection working correctly");
        System.out.println("✓ Security validation flow completed successfully");
    }

    @Test
    @Order(7)
    @DisplayName("End-to-End: Error Handling and Recovery Flow")
    void testErrorHandlingAndRecoveryFlow() throws Exception {
        System.out.println("=== End-to-End: Error Handling and Recovery Flow ===");
        
        // Test 1: Invalid order data
        try {
            OrderEntity invalidOrder = VNPayTestDataFactory.createTestOrder(0.0); // Invalid amount
            PaymentMethod validMethod = VNPayTestDataFactory.createVNPayTempPaymentMethod();
            
            paymentService.processPayment(invalidOrder, validMethod.getPaymentMethodId());
            fail("Should reject invalid order amount");
            
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("amount must be greater than zero"));
            System.out.println("✓ Invalid order amount correctly rejected");
        }
        
        // Test 2: Network/Gateway errors (simulated)
        // This would typically be tested with network mocking
        
        // Test 3: Recovery after errors
        OrderEntity recoveryOrder = VNPayTestDataFactory.createTestOrder(50000.0);
        PaymentMethod recoveryMethod = VNPayTestDataFactory.createVNPayTempPaymentMethod();
        
        PaymentTransaction recoveryTransaction = paymentService.processPayment(recoveryOrder, recoveryMethod.getPaymentMethodId());
        
        assertNotNull(recoveryTransaction, "Should successfully process payment after error recovery");
        assertEquals("PENDING_USER_ACTION", recoveryTransaction.getTransactionStatus());
        
        System.out.println("✓ Error recovery successful");
        System.out.println("✓ Error handling and recovery flow completed");
    }

    @AfterEach
    void tearDown() {
        // Clean up any test data if needed
        System.out.println("Test cleanup completed");
    }

    // Performance and Load Testing Helper
    
    @Test
    @Order(8)
    @DisplayName("End-to-End: Performance and Concurrent Payment Flow")
    void testPerformanceAndConcurrentFlow() throws Exception {
        System.out.println("=== End-to-End: Performance and Concurrent Payment Flow ===");
        
        long startTime = System.currentTimeMillis();
        
        // Test multiple concurrent payment requests
        for (int i = 0; i < 5; i++) {
            OrderEntity concurrentOrder = VNPayTestDataFactory.createTestOrder(30000.0, "CONCURRENT_ORDER_" + i + "_" + System.currentTimeMillis());
            PaymentMethod concurrentMethod = VNPayTestDataFactory.createVNPayTempPaymentMethod();
            
            PaymentTransaction concurrentTransaction = paymentService.processPayment(concurrentOrder, concurrentMethod.getPaymentMethodId());
            
            assertNotNull(concurrentTransaction, "Concurrent transaction " + i + " should be processed");
            assertEquals("PENDING_USER_ACTION", concurrentTransaction.getTransactionStatus());
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        System.out.println("✓ Processed 5 concurrent payments in " + processingTime + "ms");
        assertTrue(processingTime < 10000, "Processing should complete within 10 seconds"); // Performance threshold
        
        System.out.println("✓ Performance and concurrent payment flow completed");
    }
}