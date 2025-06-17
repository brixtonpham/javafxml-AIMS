package com.aims.test.performance;

import com.aims.core.application.services.*;
import com.aims.core.entities.*;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.*;
import com.aims.core.monitoring.IDataFlowMonitoringService;
import com.aims.core.monitoring.DataFlowMonitoringServiceImpl;
import com.aims.test.base.BaseUITest;
import com.aims.test.data.CartToOrderTestDataFactory;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Cart-to-Order Performance Test Suite
 * 
 * Comprehensive performance testing for cart-to-order data flow operations
 * including conversion speed, validation performance, and system behavior under load.
 */
public class CartToOrderPerformanceTest extends BaseUITest {
    
    private ICartService cartService;
    private IOrderService orderService;
    private ICartDataValidationService cartDataValidationService;
    private IOrderDataValidationService orderDataValidationService;
    private IOrderDataLoaderService orderDataLoaderService;
    private IDataFlowMonitoringService dataFlowMonitoringService;
    
    // Performance thresholds (in milliseconds)
    private static final long CART_LOADING_THRESHOLD = 2000; // 2 seconds
    private static final long ORDER_CREATION_THRESHOLD = 5000; // 5 seconds
    private static final long ORDER_SUMMARY_THRESHOLD = 3000; // 3 seconds
    private static final long VALIDATION_THRESHOLD = 2000; // 2 seconds
    private static final long LARGE_CART_THRESHOLD = 10000; // 10 seconds
    
    // Memory thresholds (in MB)
    private static final long MEMORY_USAGE_THRESHOLD = 100; // 100 MB
    
    @BeforeEach
    void setUp() {
        // Initialize services
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        cartService = serviceFactory.getCartService();
        orderService = serviceFactory.getOrderService();
        cartDataValidationService = serviceFactory.getCartDataValidationService();
        orderDataValidationService = serviceFactory.getOrderDataValidationService();
        orderDataLoaderService = serviceFactory.getOrderDataLoaderService();
        dataFlowMonitoringService = new DataFlowMonitoringServiceImpl();
        
        // Seed performance test data
        seedDataForTestCase("PERFORMANCE_TEST");
        
        // Warm up JVM
        performJVMWarmup();
    }
    
    @Test
    @DisplayName("Cart Loading with Enhanced Services - Performance Acceptable")
    void testCartLoading_WithEnhancedServices_PerformanceAcceptable() throws Exception {
        System.out.println("=== Testing Cart Loading Performance ===");
        
        // Create test cart with moderate number of items
        String cartSessionId = "perf-cart-loading-" + System.currentTimeMillis();
        Cart testCart = CartToOrderTestDataFactory.createLargeTestCart(cartSessionId, 20);
        
        // Measure cart loading performance
        long startTime = System.currentTimeMillis();
        
        // Load cart through service (simulating UI operation)
        Cart loadedCart = cartService.getCart(cartSessionId);
        
        // Validate cart with enhanced services
        var validationResult = cartDataValidationService.validateCartForOrderCreation(loadedCart);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Assert performance is within acceptable limits
        assertTrue(duration < CART_LOADING_THRESHOLD, 
                  "Cart loading should complete within " + CART_LOADING_THRESHOLD + "ms, actual: " + duration + "ms");
        
        // Assert functionality is preserved
        assertNotNull(loadedCart, "Cart should be loaded successfully");
        assertTrue(validationResult.isValid(), "Cart validation should pass");
        
        // Monitor performance
        dataFlowMonitoringService.trackPerformanceMetric("cart_loading_enhanced", duration, true);
        
        System.out.println("✓ Cart loading performance test passed - " + duration + "ms");
    }
    
    @Test
    @DisplayName("Order Creation from Large Cart - Under Performance Threshold")
    void testOrderCreation_LargeCart_UnderPerformanceThreshold() throws Exception {
        System.out.println("=== Testing Large Cart Order Creation Performance ===");
        
        // Create large cart (100 items)
        String cartSessionId = "perf-large-cart-" + System.currentTimeMillis();
        Cart largeCart = CartToOrderTestDataFactory.createLargeTestCart(cartSessionId, 100);
        
        // Measure order creation performance
        long startTime = System.currentTimeMillis();
        
        OrderEntity createdOrder = orderService.initiateOrderFromCartEnhanced(cartSessionId, null);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Assert performance is within acceptable limits
        assertTrue(duration < LARGE_CART_THRESHOLD,
                  "Large cart order creation should complete within " + LARGE_CART_THRESHOLD + "ms, actual: " + duration + "ms");
        
        // Assert data integrity
        assertNotNull(createdOrder, "Order should be created successfully");
        assertEquals(100, createdOrder.getOrderItems().size(), "All cart items should be converted");
        
        // Monitor performance
        dataFlowMonitoringService.trackPerformanceMetric("large_cart_order_creation", duration, true);
        
        System.out.println("✓ Large cart order creation test passed - " + duration + "ms");
    }
    
    @Test
    @DisplayName("Order Summary Population with Complete Metadata - Fast Response")
    void testOrderSummaryPopulation_CompleteMetadata_FastResponse() throws Exception {
        System.out.println("=== Testing Order Summary Population Performance ===");
        
        // Create order with complete data
        OrderEntity testOrder = CartToOrderTestDataFactory.createCompleteTestOrder("perf-summary-" + System.currentTimeMillis());
        
        // Add delivery information
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        testOrder.setDeliveryInfo(deliveryInfo);
        
        // Measure order summary population performance
        long startTime = System.currentTimeMillis();
        
        // Load complete order data
        OrderEntity completeOrder = orderDataLoaderService.loadCompleteOrderData(testOrder.getOrderId());
        
        // Create order summary DTO
        var orderSummaryDTO = orderDataLoaderService.createOrderSummaryDTO(completeOrder);
        
        // Validate order for display
        var validationResult = orderDataValidationService.validateOrderForDisplay(completeOrder);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Assert performance is within acceptable limits
        assertTrue(duration < ORDER_SUMMARY_THRESHOLD,
                  "Order summary population should complete within " + ORDER_SUMMARY_THRESHOLD + "ms, actual: " + duration + "ms");
        
        // Assert data completeness
        assertNotNull(orderSummaryDTO, "Order summary DTO should be created");
        assertTrue(validationResult.isValid(), "Order should be valid for display");
        assertTrue(orderDataLoaderService.validateOrderDataCompleteness(completeOrder), "Order should have complete data");
        
        // Monitor performance
        dataFlowMonitoringService.trackPerformanceMetric("order_summary_population", duration, true);
        
        System.out.println("✓ Order summary population test passed - " + duration + "ms");
    }
    
    @Test
    @DisplayName("Validation Services Under Load - Consistent Performance")
    void testValidationServices_UnderLoad_ConsistentPerformance() throws Exception {
        System.out.println("=== Testing Validation Services Performance Under Load ===");
        
        List<Long> validationTimes = new ArrayList<>();
        int numberOfTests = 50;
        
        for (int i = 0; i < numberOfTests; i++) {
            // Create test data for each iteration
            String cartSessionId = "perf-validation-" + i + "-" + System.currentTimeMillis();
            Cart testCart = CartToOrderTestDataFactory.createTestCartWithAllProductTypes(cartSessionId);
            
            // Measure validation performance
            long startTime = System.currentTimeMillis();
            
            // Perform multiple validations
            var cartValidation = cartDataValidationService.validateCartForOrderCreation(testCart);
            cartDataValidationService.validateStockAvailability(testCart);
            cartDataValidationService.validateProductMetadataCompleteness(testCart);
            
            // Create order and validate
            OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartSessionId, null);
            var orderValidation = orderDataValidationService.validateOrderComprehensive(order);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            validationTimes.add(duration);
            
            // Assert individual validation passes
            assertTrue(cartValidation.isValid(), "Cart validation should pass for iteration " + i);
            assertTrue(orderValidation.isValid(), "Order validation should pass for iteration " + i);
        }
        
        // Calculate performance statistics
        long totalTime = validationTimes.stream().mapToLong(Long::longValue).sum();
        double averageTime = (double) totalTime / numberOfTests;
        long maxTime = validationTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long minTime = validationTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        
        // Assert performance consistency
        assertTrue(averageTime < VALIDATION_THRESHOLD,
                  "Average validation time should be under " + VALIDATION_THRESHOLD + "ms, actual: " + averageTime + "ms");
        
        assertTrue(maxTime < VALIDATION_THRESHOLD * 2,
                  "Maximum validation time should be under " + (VALIDATION_THRESHOLD * 2) + "ms, actual: " + maxTime + "ms");
        
        // Monitor performance statistics
        dataFlowMonitoringService.trackPerformanceMetric("validation_average", (long) averageTime, true);
        dataFlowMonitoringService.trackPerformanceMetric("validation_max", maxTime, true);
        
        System.out.println("✓ Validation services load test passed");
        System.out.println("  - Average: " + String.format("%.2f", averageTime) + "ms");
        System.out.println("  - Min: " + minTime + "ms, Max: " + maxTime + "ms");
    }
    
    @Test
    @DisplayName("Memory Usage During Cart-to-Order Conversion - Within Limits")
    void testMemoryUsage_CartToOrderConversion_WithinLimits() throws Exception {
        System.out.println("=== Testing Memory Usage During Cart-to-Order Conversion ===");
        
        // Force garbage collection before test
        System.gc();
        Thread.sleep(100); // Allow GC to complete
        
        // Measure initial memory usage
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create large cart and convert to order
        String cartSessionId = "perf-memory-" + System.currentTimeMillis();
        Cart largeCart = CartToOrderTestDataFactory.createLargeTestCart(cartSessionId, 200);
        
        // Perform cart-to-order conversion
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartSessionId, null);
        
        // Load complete order data
        OrderEntity completeOrder = orderDataLoaderService.loadCompleteOrderData(order.getOrderId());
        
        // Validate order
        var validationResult = orderDataValidationService.validateOrderComprehensive(completeOrder);
        
        // Measure memory usage after operations
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = (finalMemory - initialMemory) / (1024 * 1024); // Convert to MB
        
        // Assert memory usage is within acceptable limits
        assertTrue(memoryUsed < MEMORY_USAGE_THRESHOLD,
                  "Memory usage should be under " + MEMORY_USAGE_THRESHOLD + "MB, actual: " + memoryUsed + "MB");
        
        // Assert functionality is preserved
        assertNotNull(completeOrder, "Order should be created successfully");
        assertTrue(validationResult.isValid(), "Order should be valid");
        assertEquals(200, completeOrder.getOrderItems().size(), "All items should be converted");
        
        System.out.println("✓ Memory usage test passed - " + memoryUsed + "MB used");
    }
    
    @Test
    @DisplayName("Concurrent Cart-to-Order Operations - Performance Stability")
    void testConcurrentOperations_PerformanceStability() throws Exception {
        System.out.println("=== Testing Concurrent Cart-to-Order Operations ===");
        
        int numberOfThreads = 10;
        int operationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<Long>> futures = new ArrayList<>();
        
        // Submit concurrent cart-to-order operations
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            Future<Long> future = executor.submit(() -> {
                try {
                    List<Long> threadTimes = new ArrayList<>();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        String cartSessionId = "perf-concurrent-" + threadId + "-" + j + "-" + System.currentTimeMillis();
                        Cart testCart = CartToOrderTestDataFactory.createTestCartWithAllProductTypes(cartSessionId);
                        
                        long startTime = System.currentTimeMillis();
                        
                        // Perform cart-to-order conversion
                        OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartSessionId, null);
                        
                        // Add delivery info
                        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
                        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
                        
                        // Validate order
                        var validationResult = orderDataValidationService.validateOrderForPayment(orderWithDelivery);
                        
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;
                        threadTimes.add(duration);
                        
                        // Assert operation succeeded
                        assertTrue(validationResult.isValid(), "Concurrent operation should succeed");
                    }
                    
                    // Return average time for this thread
                    return threadTimes.stream().mapToLong(Long::longValue).sum() / operationsPerThread;
                    
                } catch (Exception e) {
                    throw new RuntimeException("Concurrent operation failed", e);
                }
            });
            
            futures.add(future);
        }
        
        // Collect results
        List<Long> averageTimes = new ArrayList<>();
        for (Future<Long> future : futures) {
            try {
                Long averageTime = future.get(30, TimeUnit.SECONDS);
                averageTimes.add(averageTime);
            } catch (TimeoutException e) {
                fail("Concurrent operation timed out");
            }
        }
        
        executor.shutdown();
        
        // Calculate overall performance statistics
        double overallAverage = averageTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxAverage = averageTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        
        // Assert concurrent performance is acceptable
        assertTrue(overallAverage < ORDER_CREATION_THRESHOLD,
                  "Concurrent operations average should be under " + ORDER_CREATION_THRESHOLD + "ms, actual: " + overallAverage + "ms");
        
        assertTrue(maxAverage < ORDER_CREATION_THRESHOLD * 1.5,
                  "Maximum concurrent operation time should be reasonable, actual: " + maxAverage + "ms");
        
        System.out.println("✓ Concurrent operations test passed");
        System.out.println("  - Overall average: " + String.format("%.2f", overallAverage) + "ms");
        System.out.println("  - Max average: " + maxAverage + "ms");
    }
    
    @Test
    @DisplayName("Performance Degradation Detection - Early Warning System")
    void testPerformanceDegradation_EarlyWarningSystem() throws Exception {
        System.out.println("=== Testing Performance Degradation Detection ===");
        
        List<Long> baselineTimes = new ArrayList<>();
        List<Long> loadedTimes = new ArrayList<>();
        
        // Establish baseline performance
        for (int i = 0; i < 10; i++) {
            String cartSessionId = "perf-baseline-" + i + "-" + System.currentTimeMillis();
            Cart testCart = CartToOrderTestDataFactory.createTestCartWithAllProductTypes(cartSessionId);
            
            long startTime = System.currentTimeMillis();
            OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartSessionId, null);
            long endTime = System.currentTimeMillis();
            
            baselineTimes.add(endTime - startTime);
            assertNotNull(order, "Baseline operation should succeed");
        }
        
        // Simulate system under load
        for (int i = 0; i < 20; i++) {
            String cartSessionId = "perf-loaded-" + i + "-" + System.currentTimeMillis();
            Cart testCart = CartToOrderTestDataFactory.createLargeTestCart(cartSessionId, 50);
            
            long startTime = System.currentTimeMillis();
            OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartSessionId, null);
            
            // Add additional validation load
            var validationResult = orderDataValidationService.validateOrderComprehensive(order);
            
            long endTime = System.currentTimeMillis();
            loadedTimes.add(endTime - startTime);
            
            assertNotNull(order, "Loaded operation should succeed");
            assertTrue(validationResult.isValid(), "Loaded validation should pass");
        }
        
        // Calculate performance metrics
        double baselineAverage = baselineTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double loadedAverage = loadedTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double degradationRatio = loadedAverage / baselineAverage;
        
        // Monitor performance degradation
        dataFlowMonitoringService.trackPerformanceMetric("baseline_average", (long) baselineAverage, true);
        dataFlowMonitoringService.trackPerformanceMetric("loaded_average", (long) loadedAverage, true);
        
        // Alert if significant degradation is detected
        if (degradationRatio > 2.0) {
            dataFlowMonitoringService.triggerAlert("PERFORMANCE_DEGRADATION", 
                                                  "system_load_test", 
                                                  "Performance degraded by " + String.format("%.1f", (degradationRatio - 1) * 100) + "%", 
                                                  "HIGH");
        }
        
        // Assert degradation is within acceptable limits
        assertTrue(degradationRatio < 3.0, 
                  "Performance degradation should be under 300%, actual: " + String.format("%.1f", degradationRatio * 100) + "%");
        
        System.out.println("✓ Performance degradation test passed");
        System.out.println("  - Baseline: " + String.format("%.2f", baselineAverage) + "ms");
        System.out.println("  - Under load: " + String.format("%.2f", loadedAverage) + "ms");
        System.out.println("  - Degradation ratio: " + String.format("%.2f", degradationRatio));
    }
    
    private void performJVMWarmup() {
        try {
            // Perform a few operations to warm up the JVM
            for (int i = 0; i < 5; i++) {
                String warmupCartId = "warmup-" + i;
                Cart warmupCart = CartToOrderTestDataFactory.createTestCartWithAllProductTypes(warmupCartId);
                // Don't measure these operations - they're just for warmup
            }
            
            // Allow JVM to settle
            Thread.sleep(100);
        } catch (Exception e) {
            // Ignore warmup errors
        }
    }
    
    @AfterEach
    void tearDown() {
        // Force garbage collection after performance tests
        System.gc();
        
        // Cleanup performance test data
        System.out.println("Performance test cleanup completed");
    }
}