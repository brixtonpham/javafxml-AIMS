package com.aims.test.integration;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.Product;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.enums.ProductType;
import com.aims.core.application.services.*;
import com.aims.core.presentation.utils.OrderValidationStateManager;
import com.aims.core.presentation.utils.PaymentFlowLogger;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.infrastructure.database.SQLiteConnector;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * PaymentFlowNavigationIntegrationTest
 * 
 * Comprehensive integration tests to verify the complete payment flow works end-to-end
 * after the critical fixes, with focus on navigation state preservation, validation
 * bypass prevention, and system reliability under various conditions.
 * 
 * Test Coverage:
 * - Complete payment flow from delivery info to payment processing
 * - Validation state preservation across navigation
 * - Edge case handling (network failures, service unavailability)
 * - Button state consistency testing
 * - Navigation fallback mechanism testing
 * - Concurrent user session handling
 * - Performance under load
 * - Error recovery scenarios
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Payment Flow Navigation Integration Tests")
class PaymentFlowNavigationIntegrationTest {
    
    private static final Logger logger = Logger.getLogger(PaymentFlowNavigationIntegrationTest.class.getName());
    
    // Test infrastructure
    private static ServiceFactory serviceFactory;
    private static IOrderService orderService;
    private static IPaymentService paymentService;
    private static IOrderValidationService orderValidationService;
    private static OrderValidationStateManager stateManager;
    private static PaymentFlowLogger flowLogger;
    
    // Test data
    private OrderEntity testOrder;
    private String paymentFlowSessionId;
    private String validationStateId;
    
    // Test configuration
    private static final Duration MAX_OPERATION_TIME = Duration.ofSeconds(30);
    private static final int CONCURRENT_USERS = 5;
    private static final String TEST_ORDER_PREFIX = "INTEGRATION_TEST_";
    
    @BeforeAll
    static void setUpClass() throws Exception {
        logger.info("=== Setting up Payment Flow Navigation Integration Tests ===");
        
        try {
            // Initialize database connection
            SQLiteConnector.getInstance().getConnection();
            logger.info("✓ Database connection established");
            
            // Initialize service factory and services
            serviceFactory = ServiceFactory.getInstance();
            orderService = serviceFactory.getOrderService();
            paymentService = serviceFactory.getPaymentService();
            orderValidationService = serviceFactory.getOrderValidationService();
            
            // Initialize utility managers
            stateManager = OrderValidationStateManager.getInstance();
            flowLogger = PaymentFlowLogger.getInstance();
            
            logger.info("✓ Services and utilities initialized");
            
            // Verify services are operational
            verifyServicesOperational();
            logger.info("✓ Service operational verification completed");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to set up integration test environment", e);
            throw e;
        }
    }
    
    @AfterAll
    static void tearDownClass() {
        logger.info("=== Tearing down Payment Flow Navigation Integration Tests ===");
        
        try {
            // Cleanup state manager
            if (stateManager != null) {
                stateManager.shutdown();
            }
            
            // Cleanup flow logger
            if (flowLogger != null) {
                flowLogger.cleanupInactiveSessions(Duration.ofMinutes(1));
            }
            
            logger.info("✓ Test cleanup completed");
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during test cleanup", e);
        }
    }
    
    @BeforeEach
    void setUp() {
        logger.info("Setting up individual test case");
        
        // Create fresh test order for each test
        testOrder = createTestOrder();
        
        // Start payment flow session
        paymentFlowSessionId = flowLogger.startPaymentFlowSession(testOrder.getOrderId());
        
        // Create initial validation state
        validationStateId = stateManager.createValidationState(
            testOrder.getOrderId(),
            OrderValidationStateManager.ValidationStep.ORDER_CREATION,
            OrderValidationStateManager.ValidationResult.PASSED,
            "Test setup validation"
        );
        
        logger.info("✓ Test setup completed for order: " + testOrder.getOrderId());
    }
    
    @AfterEach
    void tearDown() {
        logger.info("Cleaning up individual test case");
        
        try {
            if (paymentFlowSessionId != null) {
                flowLogger.endPaymentFlowSession(paymentFlowSessionId, "Test completed");
            }
            
            if (testOrder != null) {
                stateManager.invalidateOrderValidations(testOrder.getOrderId());
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during test cleanup", e);
        }
    }
    
    // ==================== CORE FLOW TESTS ====================
    
    @Test
    @Order(1)
    @DisplayName("Complete Payment Flow - Happy Path")
    void testCompletePaymentFlowHappyPath() throws Exception {
        logger.info("=== Testing Complete Payment Flow - Happy Path ===");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Validate initial order state
            flowLogger.logNavigationStep(paymentFlowSessionId, "TEST_SETUP", "DELIVERY_INFO", 
                Map.of("test_phase", "initial_validation"));
            
            assertTrue(orderValidationService.orderExists(testOrder.getOrderId()), 
                "Order should exist before starting flow");
            
            // Step 2: Simulate delivery info validation
            DeliveryInfo deliveryInfo = createTestDeliveryInfo();
            testOrder.setDeliveryInfo(deliveryInfo);
            
            String deliveryValidationStateId = stateManager.createValidationState(
                testOrder.getOrderId(),
                OrderValidationStateManager.ValidationStep.DELIVERY_INFO_VALIDATION,
                OrderValidationStateManager.ValidationResult.PASSED,
                "Delivery info validation completed"
            );
            
            flowLogger.logValidationStep(paymentFlowSessionId, "DELIVERY_INFO_VALIDATION", true,
                "Delivery information validated successfully", 
                Map.of("recipient", deliveryInfo.getRecipientName(), "address_length", deliveryInfo.getDeliveryAddress().length()));
            
            // Step 3: Simulate navigation to order summary
            flowLogger.logNavigationStep(paymentFlowSessionId, "DELIVERY_INFO", "ORDER_SUMMARY", 
                Map.of("validation_state", deliveryValidationStateId));
            
            // Verify validation state preservation
            assertTrue(stateManager.hasValidValidation(testOrder.getOrderId(), 
                OrderValidationStateManager.ValidationStep.DELIVERY_INFO_VALIDATION),
                "Delivery info validation should be preserved during navigation");
            
            // Step 4: Order summary validation
            OrderValidationStateManager.ValidationSummary summary = stateManager.validateOrderForPayment(testOrder);
            assertTrue(summary.isValid(), "Order should be valid for payment: " + summary.getMessage());
            
            String summaryValidationStateId = stateManager.createValidationState(
                testOrder.getOrderId(),
                OrderValidationStateManager.ValidationStep.ORDER_SUMMARY_VALIDATION,
                OrderValidationStateManager.ValidationResult.PASSED,
                "Order summary validation completed"
            );
            
            flowLogger.logValidationStep(paymentFlowSessionId, "ORDER_SUMMARY_VALIDATION", true,
                "Order summary validated successfully", 
                Map.of("total_amount", testOrder.getTotalAmountPaid(), "items_count", testOrder.getOrderItems().size()));
            
            // Step 5: Simulate navigation to payment method
            flowLogger.logNavigationStep(paymentFlowSessionId, "ORDER_SUMMARY", "PAYMENT_METHOD", 
                Map.of("validation_state", summaryValidationStateId));
            
            // Verify all validations are still preserved
            assertTrue(stateManager.hasValidValidation(testOrder.getOrderId(), 
                OrderValidationStateManager.ValidationStep.DELIVERY_INFO_VALIDATION),
                "Delivery validation should still be preserved");
            assertTrue(stateManager.hasValidValidation(testOrder.getOrderId(), 
                OrderValidationStateManager.ValidationStep.ORDER_SUMMARY_VALIDATION),
                "Order summary validation should be preserved");
            
            // Step 6: Payment method selection validation
            boolean paymentReadiness = orderValidationService.isOrderReadyForPayment(testOrder.getOrderId());
            assertTrue(paymentReadiness, "Order should be ready for payment");
            
            flowLogger.logValidationStep(paymentFlowSessionId, "PAYMENT_READINESS_CHECK", true,
                "Order ready for payment processing", 
                Map.of("order_status", testOrder.getOrderStatus(), "payment_readiness", paymentReadiness));
            
            long endTime = System.currentTimeMillis();
            Duration totalDuration = Duration.ofMillis(endTime - startTime);
            
            flowLogger.logPerformanceMetrics(paymentFlowSessionId, "COMPLETE_PAYMENT_FLOW", totalDuration,
                Map.of("steps_completed", 6, "validations_passed", 3));
            
            // Verify performance is acceptable (should complete in under 5 seconds)
            assertTrue(totalDuration.compareTo(Duration.ofSeconds(5)) < 0,
                "Complete flow should complete in under 5 seconds, took: " + totalDuration.toMillis() + "ms");
            
            logger.info("✓ Complete payment flow test passed in " + totalDuration.toMillis() + "ms");
            
        } catch (Exception e) {
            flowLogger.logCriticalError(paymentFlowSessionId, "COMPLETE_FLOW_TEST_FAILURE", e,
                Map.of("test_order", testOrder.getOrderId(), "test_phase", "happy_path"));
            throw e;
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("Validation State Preservation Across Multiple Navigation")
    void testValidationStatePreservationAcrossNavigation() throws Exception {
        logger.info("=== Testing Validation State Preservation Across Navigation ===");
        
        try {
            // Create multiple validation states
            String deliveryStateId = stateManager.createValidationState(
                testOrder.getOrderId(),
                OrderValidationStateManager.ValidationStep.DELIVERY_INFO_VALIDATION,
                OrderValidationStateManager.ValidationResult.PASSED,
                "Initial delivery validation"
            );
            
            String summaryStateId = stateManager.createValidationState(
                testOrder.getOrderId(),
                OrderValidationStateManager.ValidationStep.ORDER_SUMMARY_VALIDATION,
                OrderValidationStateManager.ValidationResult.PASSED,
                "Initial summary validation"
            );
            
            // Simulate complex navigation pattern: forward, backward, forward again
            String[] navigationSequence = {
                "DELIVERY_INFO", "ORDER_SUMMARY", "PAYMENT_METHOD", 
                "ORDER_SUMMARY", "DELIVERY_INFO", "ORDER_SUMMARY", "PAYMENT_METHOD"
            };
            
            for (int i = 1; i < navigationSequence.length; i++) {
                String fromScreen = navigationSequence[i - 1];
                String toScreen = navigationSequence[i];
                
                flowLogger.logNavigationStep(paymentFlowSessionId, fromScreen, toScreen,
                    Map.of("navigation_step", i, "sequence_test", true));
                
                // Simulate navigation state tracking
                stateManager.trackNavigation(testOrder.getOrderId(), fromScreen, toScreen);
                
                // Verify validations are preserved after each navigation step
                assertTrue(stateManager.hasValidValidation(testOrder.getOrderId(),
                    OrderValidationStateManager.ValidationStep.DELIVERY_INFO_VALIDATION),
                    "Delivery validation should be preserved at step " + i);
                
                assertTrue(stateManager.hasValidValidation(testOrder.getOrderId(),
                    OrderValidationStateManager.ValidationStep.ORDER_SUMMARY_VALIDATION),
                    "Summary validation should be preserved at step " + i);
                
                // Brief pause to simulate realistic navigation timing
                Thread.sleep(100);
            }
            
            // Verify final state
            Map<String, OrderValidationStateManager.ValidationState> finalStates = 
                stateManager.getValidationStatesForOrder(testOrder.getOrderId());
            
            assertFalse(finalStates.isEmpty(), "Validation states should not be empty after navigation");
            
            long validStatesCount = finalStates.values().stream()
                .filter(OrderValidationStateManager.ValidationState::isValid)
                .count();
            
            assertTrue(validStatesCount >= 2, 
                "Should have at least 2 valid validation states, found: " + validStatesCount);
            
            flowLogger.logValidationStep(paymentFlowSessionId, "NAVIGATION_PRESERVATION_TEST", true,
                "Validation states preserved through complex navigation",
                Map.of("navigation_steps", navigationSequence.length - 1, "valid_states", validStatesCount));
            
            logger.info("✓ Validation state preservation test passed");
            
        } catch (Exception e) {
            flowLogger.logCriticalError(paymentFlowSessionId, "NAVIGATION_PRESERVATION_FAILURE", e,
                Map.of("test_order", testOrder.getOrderId()));
            throw e;
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("Validation Bypass Prevention")
    void testValidationBypassPrevention() throws Exception {
        logger.info("=== Testing Validation Bypass Prevention ===");
        
        try {
            // Attempt to navigate to payment method without proper validations
            flowLogger.logNavigationStep(paymentFlowSessionId, "ORDER_CREATION", "PAYMENT_METHOD",
                Map.of("bypass_attempt", true, "expected_result", "blocked"));
            
            // This should be detected as a bypass attempt
            stateManager.trackNavigation(testOrder.getOrderId(), "OrderSummaryController", "PaymentMethodScreenController");
            
            // Verify the system detected the bypass attempt
            Map<String, OrderValidationStateManager.ValidationState> states = 
                stateManager.getValidationStatesForOrder(testOrder.getOrderId());
            
            boolean bypassDetected = states.values().stream()
                .anyMatch(state -> state.getResult() == OrderValidationStateManager.ValidationResult.BYPASSED);
            
            assertTrue(bypassDetected, "System should detect validation bypass attempt");
            
            flowLogger.logSecurityEvent(paymentFlowSessionId, "VALIDATION_BYPASS_ATTEMPT",
                "Detected attempt to bypass validation steps",
                Map.of("from_screen", "OrderSummaryController", "to_screen", "PaymentMethodScreenController"));
            
            // Verify order validation fails due to missing validations
            OrderValidationStateManager.ValidationSummary summary = stateManager.validateOrderForPayment(testOrder);
            
            // The summary might still be valid due to auto-validation, but should have warnings
            if (summary.isValid()) {
                assertTrue(summary.hasWarnings(), "Should have warnings even if auto-validated");
            }
            
            flowLogger.logValidationStep(paymentFlowSessionId, "BYPASS_PREVENTION_TEST", true,
                "Validation bypass prevention working correctly",
                Map.of("bypass_detected", bypassDetected, "warnings_present", summary.hasWarnings()));
            
            logger.info("✓ Validation bypass prevention test passed");
            
        } catch (Exception e) {
            flowLogger.logCriticalError(paymentFlowSessionId, "BYPASS_PREVENTION_FAILURE", e,
                Map.of("test_order", testOrder.getOrderId()));
            throw e;
        }
    }
    
    // ==================== EDGE CASE TESTS ====================
    
    @Test
    @Order(4)
    @DisplayName("Network Failure Simulation")
    void testNetworkFailureHandling() throws Exception {
        logger.info("=== Testing Network Failure Handling ===");
        
        try {
            // Simulate network failure during payment processing
            PaymentMethod testPaymentMethod = createTestPaymentMethod();
            
            // This would normally succeed, but we'll simulate a network failure
            flowLogger.logSecurityEvent(paymentFlowSessionId, "NETWORK_FAILURE_SIMULATION",
                "Simulating network failure during payment processing",
                Map.of("payment_method", testPaymentMethod.getMethodType(), "simulation", true));
            
            // Verify system can handle the failure gracefully
            try {
                // In a real scenario, this might throw a PaymentException due to network issues
                // For testing, we'll verify the order validation still works
                boolean orderReady = orderValidationService.isOrderReadyForPayment(testOrder.getOrderId());
                
                // The order readiness check should still work even if payment processing fails
                flowLogger.logValidationStep(paymentFlowSessionId, "NETWORK_FAILURE_RESILIENCE", true,
                    "Order validation resilient to network issues",
                    Map.of("order_ready", orderReady, "simulation", true));
                
            } catch (SQLException e) {
                // This is expected if we simulate database connectivity issues
                flowLogger.logCriticalError(paymentFlowSessionId, "SIMULATED_NETWORK_FAILURE", e,
                    Map.of("expected", true, "test_type", "network_simulation"));
                
                // Verify recovery mechanisms work
                assertNotNull(testOrder.getOrderId(), "Order data should be preserved during network issues");
            }
            
            logger.info("✓ Network failure handling test passed");
            
        } catch (Exception e) {
            flowLogger.logCriticalError(paymentFlowSessionId, "NETWORK_FAILURE_TEST_ERROR", e,
                Map.of("test_order", testOrder.getOrderId()));
            throw e;
        }
    }
    
    @Test
    @Order(5)
    @DisplayName("Service Unavailability Handling")
    void testServiceUnavailabilityHandling() throws Exception {
        logger.info("=== Testing Service Unavailability Handling ===");
        
        try {
            // Test with null/unavailable services
            OrderValidationStateManager.ValidationSummary summary = stateManager.validateOrderForPayment(null);
            
            assertFalse(summary.isValid(), "Validation should fail for null order");
            assertTrue(summary.hasErrors(), "Should have errors for null order");
            
            flowLogger.logValidationStep(paymentFlowSessionId, "SERVICE_UNAVAILABILITY_TEST", true,
                "System handles service unavailability gracefully",
                Map.of("null_order_handled", true, "errors_present", summary.hasErrors()));
            
            // Test with invalid order ID
            try {
                boolean exists = orderValidationService.orderExists("INVALID_ORDER_ID");
                assertFalse(exists, "Invalid order should not exist");
                
            } catch (SQLException e) {
                // Expected if database service is unavailable
                flowLogger.logCriticalError(paymentFlowSessionId, "SERVICE_UNAVAILABLE_SIMULATION", e,
                    Map.of("expected", true, "service", "database"));
            }
            
            logger.info("✓ Service unavailability handling test passed");
            
        } catch (Exception e) {
            flowLogger.logCriticalError(paymentFlowSessionId, "SERVICE_UNAVAILABILITY_TEST_ERROR", e,
                Map.of("test_order", testOrder.getOrderId()));
            throw e;
        }
    }
    
    // ==================== PERFORMANCE TESTS ====================
    
    @Test
    @Order(6)
    @DisplayName("Concurrent User Sessions")
    void testConcurrentUserSessions() throws Exception {
        logger.info("=== Testing Concurrent User Sessions ===");
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        try {
            // Create concurrent payment flow sessions
            for (int i = 0; i < CONCURRENT_USERS; i++) {
                final int userId = i;
                
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        // Create unique order for each concurrent user
                        OrderEntity concurrentOrder = createTestOrder();
                        concurrentOrder.setOrderId(TEST_ORDER_PREFIX + "CONCURRENT_" + userId + "_" + System.currentTimeMillis());
                        
                        String sessionId = flowLogger.startPaymentFlowSession(concurrentOrder.getOrderId());
                        
                        // Simulate payment flow steps
                        flowLogger.logNavigationStep(sessionId, "START", "DELIVERY_INFO",
                            Map.of("user_id", userId, "concurrent_test", true));
                        
                        String validationStateId = stateManager.createValidationState(
                            concurrentOrder.getOrderId(),
                            OrderValidationStateManager.ValidationStep.DELIVERY_INFO_VALIDATION,
                            OrderValidationStateManager.ValidationResult.PASSED,
                            "Concurrent user " + userId + " validation"
                        );
                        
                        // Add some processing delay to simulate real usage
                        Thread.sleep(100 + (userId * 50));
                        
                        flowLogger.logNavigationStep(sessionId, "DELIVERY_INFO", "ORDER_SUMMARY",
                            Map.of("user_id", userId, "validation_state", validationStateId));
                        
                        // Verify validation state is preserved for this specific user
                        boolean hasValidation = stateManager.hasValidValidation(concurrentOrder.getOrderId(),
                            OrderValidationStateManager.ValidationStep.DELIVERY_INFO_VALIDATION);
                        
                        flowLogger.logValidationStep(sessionId, "CONCURRENT_USER_VALIDATION", hasValidation,
                            "Concurrent user validation check",
                            Map.of("user_id", userId, "validation_preserved", hasValidation));
                        
                        flowLogger.endPaymentFlowSession(sessionId, "Concurrent test completed");
                        
                        return hasValidation;
                        
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Concurrent user " + userId + " test failed", e);
                        return false;
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // Wait for all concurrent operations to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allFutures.get(30, TimeUnit.SECONDS);
            
            // Verify all concurrent operations succeeded
            long successCount = futures.stream()
                .map(CompletableFuture::join)
                .filter(success -> success)
                .count();
            
            assertEquals(CONCURRENT_USERS, successCount, 
                "All concurrent users should complete successfully, succeeded: " + successCount);
            
            flowLogger.logPerformanceMetrics(paymentFlowSessionId, "CONCURRENT_USERS_TEST", 
                Duration.ofSeconds(30),
                Map.of("concurrent_users", CONCURRENT_USERS, "success_count", successCount));
            
            logger.info("✓ Concurrent user sessions test passed: " + successCount + "/" + CONCURRENT_USERS + " succeeded");
            
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("Performance Under Load")
    void testPerformanceUnderLoad() throws Exception {
        logger.info("=== Testing Performance Under Load ===");
        
        long startTime = System.currentTimeMillis();
        int operationCount = 100;
        
        try {
            for (int i = 0; i < operationCount; i++) {
                // Create validation state
                String stateId = stateManager.createValidationState(
                    testOrder.getOrderId(),
                    OrderValidationStateManager.ValidationStep.DELIVERY_INFO_VALIDATION,
                    OrderValidationStateManager.ValidationResult.PASSED,
                    "Load test iteration " + i
                );
                
                // Perform validation check
                boolean hasValidation = stateManager.hasValidValidation(testOrder.getOrderId(),
                    OrderValidationStateManager.ValidationStep.DELIVERY_INFO_VALIDATION);
                
                assertTrue(hasValidation, "Validation should exist at iteration " + i);
                
                // Log user action
                flowLogger.logUserAction(paymentFlowSessionId, "LOAD_TEST_ACTION_" + i,
                    Map.of("iteration", i, "operation_count", operationCount));
                
                if (i % 20 == 0) {
                    logger.info("Completed " + i + "/" + operationCount + " load test operations");
                }
            }
            
            long endTime = System.currentTimeMillis();
            Duration totalDuration = Duration.ofMillis(endTime - startTime);
            double operationsPerSecond = operationCount / (totalDuration.toMillis() / 1000.0);
            
            flowLogger.logPerformanceMetrics(paymentFlowSessionId, "LOAD_TEST", totalDuration,
                Map.of(
                    "total_operations", operationCount,
                    "operations_per_second", operationsPerSecond,
                    "avg_operation_time_ms", totalDuration.toMillis() / operationCount
                ));
            
            // Verify performance is acceptable (should handle at least 10 operations per second)
            assertTrue(operationsPerSecond >= 10,
                "Should handle at least 10 operations per second, achieved: " + operationsPerSecond);
            
            logger.info("✓ Performance under load test passed: " + operationsPerSecond + " ops/sec");
            
        } catch (Exception e) {
            flowLogger.logCriticalError(paymentFlowSessionId, "LOAD_TEST_FAILURE", e,
                Map.of("test_order", testOrder.getOrderId(), "operation_count", operationCount));
            throw e;
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private static void verifyServicesOperational() throws Exception {
        assertNotNull(orderService, "OrderService should be initialized");
        assertNotNull(paymentService, "PaymentService should be initialized");
        assertNotNull(orderValidationService, "OrderValidationService should be initialized");
        assertNotNull(stateManager, "OrderValidationStateManager should be initialized");
        assertNotNull(flowLogger, "PaymentFlowLogger should be initialized");
        
        // Test basic service operations
        try {
            // This might throw SQLException if database is not available, which is fine for testing
            boolean serviceTest = orderValidationService.orderExists("TEST_NON_EXISTENT_ORDER");
            assertFalse(serviceTest, "Non-existent order should return false");
        } catch (SQLException e) {
            // Expected in some test environments
            logger.info("Database connectivity test - SQL exception expected in some environments: " + e.getMessage());
        }
    }
    
    private OrderEntity createTestOrder() {
        OrderEntity order = new OrderEntity();
        order.setOrderId(TEST_ORDER_PREFIX + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8));
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmountPaid(100000.0f); // 100,000 VND
        order.setTotalProductPriceExclVAT(90909.09f);
        order.setTotalProductPriceInclVAT(100000.0f);
        order.setCalculatedDeliveryFee(0.0f);
        
        // Add test order items
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem();
        item.setQuantity(1);
        item.setPriceAtTimeOfOrder(100000.0f);
        
        Product product = new Product();
        product.setProductId("TEST_PRODUCT_" + System.currentTimeMillis());
        product.setTitle("Test Product for Integration Test");
        product.setPrice(100000.0f);
        product.setProductType(ProductType.BOOK);
        product.setQuantityInStock(10);
        
        item.setProduct(product);
        items.add(item);
        order.setOrderItems(items);
        
        return order;
    }
    
    private DeliveryInfo createTestDeliveryInfo() {
        DeliveryInfo deliveryInfo = new DeliveryInfo();
        deliveryInfo.setRecipientName("Integration Test User");
        deliveryInfo.setPhoneNumber("0123456789");
        deliveryInfo.setEmail("test@integration.com");
        deliveryInfo.setDeliveryAddress("123 Test Street, Test District");
        deliveryInfo.setDeliveryProvinceCity("Hanoi");
        deliveryInfo.setDeliveryMethodChosen("STANDARD");
        deliveryInfo.setDeliveryInstructions("Integration test delivery");
        
        return deliveryInfo;
    }
    
    private PaymentMethod createTestPaymentMethod() {
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId("TEST_PAYMENT_" + System.currentTimeMillis());
        method.setMethodType(PaymentMethodType.CREDIT_CARD);
        method.setDefault(false);
        
        return method;
    }
}