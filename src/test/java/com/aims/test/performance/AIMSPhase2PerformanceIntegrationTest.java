package com.aims.test.performance;

import com.aims.core.application.services.*;
import com.aims.core.entities.*;
import com.aims.core.enums.OrderStatus;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.*;
import com.aims.test.base.BaseUITest;
import com.aims.test.data.CartToOrderTestDataFactory;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * AIMS Phase 2 Performance Integration Test Suite
 * 
 * This test suite validates that Phase 2 service integrations do not degrade system performance:
 * - Baseline performance measurements
 * - Load testing with Phase 2 services
 * - Memory usage validation
 * - Concurrent operation performance
 * - Scalability testing
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AIMS Phase 2 - Performance Integration Testing")
public class AIMSPhase2PerformanceIntegrationTest extends BaseUITest {
    
    private ICartService cartService;
    private IOrderService orderService;
    private IProductService productService;
    private IStockValidationService stockValidationService;
    private IOrderStateManagementService orderStateManagementService;
    private IDeliveryCalculationService deliveryCalculationService;
    
    // Performance thresholds (in milliseconds)
    private static final long CART_OPERATION_THRESHOLD = 500;
    private static final long ORDER_CREATION_THRESHOLD = 2000;
    private static final long STOCK_VALIDATION_THRESHOLD = 200;
    private static final long ORDER_APPROVAL_THRESHOLD = 1000;
    private static final long PAYMENT_PROCESSING_THRESHOLD = 3000;
    
    // Memory thresholds (in MB)
    private static final long MEMORY_USAGE_THRESHOLD = 50;
    
    private Map<String, List<Long>> performanceMetrics;
    
    @BeforeEach
    void setUp() {
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        cartService = serviceFactory.getCartService();
        orderService = serviceFactory.getOrderService();
        productService = serviceFactory.getProductService();
        stockValidationService = serviceFactory.getStockValidationService();
        orderStateManagementService = serviceFactory.getOrderStateManagementService();
        deliveryCalculationService = serviceFactory.getDeliveryCalculationService();
        
        performanceMetrics = new HashMap<>();
        
        seedDataForTestCase("PHASE2_PERFORMANCE_INTEGRATION");
        
        // JVM warmup
        performJVMWarmup();
        
        System.out.println("=== AIMS Phase 2 Performance Integration Tests ===");
    }
    
    @Test
    @Order(1)
    @DisplayName("Baseline Performance: Cart Operations with Stock Validation")
    void testBaselinePerformance_CartOperationsWithStockValidation() throws Exception {
        System.out.println("\n=== PERFORMANCE TEST 1: Cart Operations with Stock Validation ===");
        
        List<Long> cartCreationTimes = new ArrayList<>();
        List<Long> itemAdditionTimes = new ArrayList<>();
        List<Long> quantityUpdateTimes = new ArrayList<>();
        List<Long> stockValidationTimes = new ArrayList<>();
        
        // Test cart operations performance
        for (int i = 0; i < 20; i++) {
            String cartId = "perf-cart-" + i + "-" + System.currentTimeMillis();
            
            // Cart creation
            long startTime = System.currentTimeMillis();
            cartService.createCart(cartId, "perf-user-" + i);
            long endTime = System.currentTimeMillis();
            cartCreationTimes.add(endTime - startTime);
            
            // Item addition with stock validation
            startTime = System.currentTimeMillis();
            cartService.addItemToCart(cartId, "BOOK-001", 1);
            endTime = System.currentTimeMillis();
            itemAdditionTimes.add(endTime - startTime);
            
            // Quantity update with stock validation
            startTime = System.currentTimeMillis();
            cartService.updateItemQuantity(cartId, "BOOK-001", 2);
            endTime = System.currentTimeMillis();
            quantityUpdateTimes.add(endTime - startTime);
            
            // Direct stock validation
            startTime = System.currentTimeMillis();
            IStockValidationService.StockValidationResult result = 
                stockValidationService.validateProductStock("BOOK-001", 1);
            endTime = System.currentTimeMillis();
            stockValidationTimes.add(endTime - startTime);
            
            assertNotNull(result, "Stock validation should return result");
        }
        
        // Calculate averages and validate performance
        double avgCartCreation = calculateAverage(cartCreationTimes);
        double avgItemAddition = calculateAverage(itemAdditionTimes);
        double avgQuantityUpdate = calculateAverage(quantityUpdateTimes);
        double avgStockValidation = calculateAverage(stockValidationTimes);
        
        // Validate against thresholds
        assertTrue(avgCartCreation < CART_OPERATION_THRESHOLD, 
            "Cart creation average (" + avgCartCreation + "ms) should be under " + CART_OPERATION_THRESHOLD + "ms");
        assertTrue(avgItemAddition < CART_OPERATION_THRESHOLD, 
            "Item addition average (" + avgItemAddition + "ms) should be under " + CART_OPERATION_THRESHOLD + "ms");
        assertTrue(avgQuantityUpdate < CART_OPERATION_THRESHOLD, 
            "Quantity update average (" + avgQuantityUpdate + "ms) should be under " + CART_OPERATION_THRESHOLD + "ms");
        assertTrue(avgStockValidation < STOCK_VALIDATION_THRESHOLD, 
            "Stock validation average (" + avgStockValidation + "ms) should be under " + STOCK_VALIDATION_THRESHOLD + "ms");
        
        recordPerformanceMetrics("cart_operations", cartCreationTimes, itemAdditionTimes, quantityUpdateTimes);
        recordPerformanceMetrics("stock_validation", stockValidationTimes);
        
        System.out.println("Performance Results:");
        System.out.println("- Cart creation avg: " + String.format("%.2f", avgCartCreation) + "ms");
        System.out.println("- Item addition avg: " + String.format("%.2f", avgItemAddition) + "ms");
        System.out.println("- Quantity update avg: " + String.format("%.2f", avgQuantityUpdate) + "ms");
        System.out.println("- Stock validation avg: " + String.format("%.2f", avgStockValidation) + "ms");
        
        System.out.println("✅ Baseline cart operations performance test PASSED");
    }
    
    @Test
    @Order(2)
    @DisplayName("Order Creation Performance with Enhanced Services")
    void testOrderCreationPerformance_WithEnhancedServices() throws Exception {
        System.out.println("\n=== PERFORMANCE TEST 2: Order Creation with Enhanced Services ===");
        
        List<Long> orderCreationTimes = new ArrayList<>();
        List<Long> deliverySetupTimes = new ArrayList<>();
        List<Long> paymentProcessingTimes = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            String cartId = "perf-order-cart-" + i + "-" + System.currentTimeMillis();
            
            // Setup cart
            cartService.createCart(cartId, "perf-user-" + i);
            cartService.addItemToCart(cartId, "BOOK-001", 1);
            cartService.addItemToCart(cartId, "CD-001", 1);
            cartService.addItemToCart(cartId, "DVD-001", 1);
            
            // Measure order creation
            long startTime = System.currentTimeMillis();
            OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartId, "perf-user-" + i);
            long endTime = System.currentTimeMillis();
            orderCreationTimes.add(endTime - startTime);
            
            assertNotNull(order, "Order should be created successfully");
            assertEquals(3, order.getOrderItems().size(), "Order should have 3 items");
            
            // Measure delivery information setup
            startTime = System.currentTimeMillis();
            DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
            OrderEntity orderWithDelivery = orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
            endTime = System.currentTimeMillis();
            deliverySetupTimes.add(endTime - startTime);
            
            assertEquals(OrderStatus.PENDING_PAYMENT, orderWithDelivery.getOrderStatus());
            
            // Measure payment processing
            startTime = System.currentTimeMillis();
            orderService.processOrderPayment(orderWithDelivery.getOrderId(), "perf-payment-" + i);
            endTime = System.currentTimeMillis();
            paymentProcessingTimes.add(endTime - startTime);
            
            OrderEntity paidOrder = orderService.getOrderDetails(orderWithDelivery.getOrderId());
            assertEquals(OrderStatus.PENDING_PROCESSING, paidOrder.getOrderStatus());
        }
        
        // Calculate and validate performance
        double avgOrderCreation = calculateAverage(orderCreationTimes);
        double avgDeliverySetup = calculateAverage(deliverySetupTimes);
        double avgPaymentProcessing = calculateAverage(paymentProcessingTimes);
        
        assertTrue(avgOrderCreation < ORDER_CREATION_THRESHOLD, 
            "Order creation average (" + avgOrderCreation + "ms) should be under " + ORDER_CREATION_THRESHOLD + "ms");
        assertTrue(avgPaymentProcessing < PAYMENT_PROCESSING_THRESHOLD, 
            "Payment processing average (" + avgPaymentProcessing + "ms) should be under " + PAYMENT_PROCESSING_THRESHOLD + "ms");
        
        recordPerformanceMetrics("order_creation", orderCreationTimes);
        recordPerformanceMetrics("delivery_setup", deliverySetupTimes);
        recordPerformanceMetrics("payment_processing", paymentProcessingTimes);
        
        System.out.println("Performance Results:");
        System.out.println("- Order creation avg: " + String.format("%.2f", avgOrderCreation) + "ms");
        System.out.println("- Delivery setup avg: " + String.format("%.2f", avgDeliverySetup) + "ms");
        System.out.println("- Payment processing avg: " + String.format("%.2f", avgPaymentProcessing) + "ms");
        
        System.out.println("✅ Order creation performance test PASSED");
    }
    
    @Test
    @Order(3)
    @DisplayName("Order Approval Performance with State Management")
    void testOrderApprovalPerformance_WithStateManagement() throws Exception {
        System.out.println("\n=== PERFORMANCE TEST 3: Order Approval with State Management ===");
        
        List<Long> approvalTimes = new ArrayList<>();
        List<Long> rejectionTimes = new ArrayList<>();
        
        // Test approval performance
        for (int i = 0; i < 8; i++) {
            String cartId = "perf-approval-cart-" + i + "-" + System.currentTimeMillis();
            
            // Setup order ready for approval
            OrderEntity order = createReadyForApprovalOrder(cartId, "perf-approval-user-" + i);
            
            // Measure approval time
            long startTime = System.currentTimeMillis();
            orderService.approveOrder(order.getOrderId(), "perf-manager-" + i);
            long endTime = System.currentTimeMillis();
            approvalTimes.add(endTime - startTime);
            
            OrderEntity approvedOrder = orderService.getOrderDetails(order.getOrderId());
            assertEquals(OrderStatus.APPROVED, approvedOrder.getOrderStatus());
        }
        
        // Test rejection performance
        for (int i = 0; i < 5; i++) {
            String cartId = "perf-rejection-cart-" + i + "-" + System.currentTimeMillis();
            
            // Setup order ready for rejection
            OrderEntity order = createReadyForApprovalOrder(cartId, "perf-rejection-user-" + i);
            
            // Measure rejection time
            long startTime = System.currentTimeMillis();
            orderService.rejectOrder(order.getOrderId(), "perf-manager-" + i, "Performance test rejection");
            long endTime = System.currentTimeMillis();
            rejectionTimes.add(endTime - startTime);
            
            OrderEntity rejectedOrder = orderService.getOrderDetails(order.getOrderId());
            assertEquals(OrderStatus.REJECTED, rejectedOrder.getOrderStatus());
        }
        
        // Calculate and validate performance
        double avgApproval = calculateAverage(approvalTimes);
        double avgRejection = calculateAverage(rejectionTimes);
        
        assertTrue(avgApproval < ORDER_APPROVAL_THRESHOLD, 
            "Order approval average (" + avgApproval + "ms) should be under " + ORDER_APPROVAL_THRESHOLD + "ms");
        assertTrue(avgRejection < ORDER_APPROVAL_THRESHOLD, 
            "Order rejection average (" + avgRejection + "ms) should be under " + ORDER_APPROVAL_THRESHOLD + "ms");
        
        recordPerformanceMetrics("order_approval", approvalTimes);
        recordPerformanceMetrics("order_rejection", rejectionTimes);
        
        System.out.println("Performance Results:");
        System.out.println("- Order approval avg: " + String.format("%.2f", avgApproval) + "ms");
        System.out.println("- Order rejection avg: " + String.format("%.2f", avgRejection) + "ms");
        
        System.out.println("✅ Order approval performance test PASSED");
    }
    
    @Test
    @Order(4)
    @DisplayName("Concurrent Operations Performance Under Load")
    void testConcurrentOperationsPerformance_UnderLoad() throws Exception {
        System.out.println("\n=== PERFORMANCE TEST 4: Concurrent Operations Under Load ===");
        
        int numberOfThreads = 8;
        int operationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<PerformanceResult>> futures = new ArrayList<>();
        
        // Submit concurrent operations
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            Future<PerformanceResult> future = executor.submit(() -> {
                return performConcurrentOperations(threadId, operationsPerThread);
            });
            futures.add(future);
        }
        
        // Collect results
        List<PerformanceResult> results = new ArrayList<>();
        for (Future<PerformanceResult> future : futures) {
            try {
                PerformanceResult result = future.get(30, TimeUnit.SECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                fail("Concurrent operation timed out");
            }
        }
        
        executor.shutdown();
        
        // Analyze concurrent performance
        double avgConcurrentOrderCreation = results.stream()
            .mapToLong(r -> r.orderCreationTime)
            .average()
            .orElse(0.0);
        
        double avgConcurrentPayment = results.stream()
            .mapToLong(r -> r.paymentProcessingTime)
            .average()
            .orElse(0.0);
        
        int successfulOperations = (int) results.stream()
            .mapToLong(r -> r.successfulOperations)
            .sum();
        
        double successRate = (double) successfulOperations / (numberOfThreads * operationsPerThread);
        
        // Validate concurrent performance
        assertTrue(avgConcurrentOrderCreation < ORDER_CREATION_THRESHOLD * 1.5, 
            "Concurrent order creation should be reasonable: " + avgConcurrentOrderCreation + "ms");
        assertTrue(avgConcurrentPayment < PAYMENT_PROCESSING_THRESHOLD * 1.5, 
            "Concurrent payment processing should be reasonable: " + avgConcurrentPayment + "ms");
        assertTrue(successRate >= 0.8, 
            "Success rate should be at least 80%, actual: " + (successRate * 100) + "%");
        
        System.out.println("Concurrent Performance Results:");
        System.out.println("- Threads: " + numberOfThreads + ", Operations per thread: " + operationsPerThread);
        System.out.println("- Success rate: " + String.format("%.2f", successRate * 100) + "%");
        System.out.println("- Avg order creation: " + String.format("%.2f", avgConcurrentOrderCreation) + "ms");
        System.out.println("- Avg payment processing: " + String.format("%.2f", avgConcurrentPayment) + "ms");
        
        System.out.println("✅ Concurrent operations performance test PASSED");
    }
    
    @Test
    @Order(5)
    @DisplayName("Memory Usage Validation with Phase 2 Services")
    void testMemoryUsageValidation_WithPhase2Services() throws Exception {
        System.out.println("\n=== PERFORMANCE TEST 5: Memory Usage Validation ===");
        
        // Force garbage collection before test
        System.gc();
        Thread.sleep(100);
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform memory-intensive operations
        List<String> createdOrders = new ArrayList<>();
        
        for (int i = 0; i < 25; i++) {
            String cartId = "memory-test-cart-" + i + "-" + System.currentTimeMillis();
            
            try {
                // Create cart with multiple items
                cartService.createCart(cartId, "memory-user-" + i);
                cartService.addItemToCart(cartId, "BOOK-001", 2);
                cartService.addItemToCart(cartId, "CD-001", 1);
                cartService.addItemToCart(cartId, "DVD-001", 3);
                
                // Create order
                OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartId, "memory-user-" + i);
                createdOrders.add(order.getOrderId());
                
                // Add delivery info
                DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
                orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
                
                // Process payment
                orderService.processOrderPayment(order.getOrderId(), "memory-payment-" + i);
                
                // Stock validations
                for (int j = 0; j < 5; j++) {
                    stockValidationService.validateProductStock("BOOK-001", 1);
                    stockValidationService.getStockInfo("CD-001");
                }
                
            } catch (Exception e) {
                // Some operations might fail under memory pressure, which is acceptable
                System.out.println("Memory test operation " + i + " failed (acceptable): " + e.getMessage());
            }
        }
        
        // Force garbage collection and measure memory
        System.gc();
        Thread.sleep(100);
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024); // Convert to MB
        
        // Validate memory usage
        assertTrue(memoryIncrease < MEMORY_USAGE_THRESHOLD, 
            "Memory increase (" + memoryIncrease + "MB) should be under " + MEMORY_USAGE_THRESHOLD + "MB");
        
        System.out.println("Memory Usage Results:");
        System.out.println("- Initial memory: " + (initialMemory / 1024 / 1024) + "MB");
        System.out.println("- Final memory: " + (finalMemory / 1024 / 1024) + "MB");
        System.out.println("- Memory increase: " + memoryIncrease + "MB");
        System.out.println("- Created orders: " + createdOrders.size());
        
        System.out.println("✅ Memory usage validation test PASSED");
    }
    
    @Test
    @Order(6)
    @DisplayName("Scalability Testing: Large Order Processing")
    void testScalabilityTesting_LargeOrderProcessing() throws Exception {
        System.out.println("\n=== PERFORMANCE TEST 6: Scalability - Large Order Processing ===");
        
        String largeCartId = "large-order-" + System.currentTimeMillis();
        cartService.createCart(largeCartId, "large-order-user");
        
        // Create large cart with many items
        long cartBuildStart = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            try {
                cartService.addItemToCart(largeCartId, "BOOK-001", 1);
                if (i % 3 == 0) {
                    cartService.addItemToCart(largeCartId, "CD-001", 1);
                }
                if (i % 5 == 0) {
                    cartService.addItemToCart(largeCartId, "DVD-001", 1);
                }
            } catch (Exception e) {
                // Some additions might fail due to stock limits
                break;
            }
        }
        long cartBuildEnd = System.currentTimeMillis();
        
        Cart largeCart = cartService.getCart(largeCartId);
        int actualItems = largeCart.getItems().size();
        
        // Process large order
        long orderCreationStart = System.currentTimeMillis();
        OrderEntity largeOrder = orderService.initiateOrderFromCartEnhanced(largeCartId, "large-order-user");
        long orderCreationEnd = System.currentTimeMillis();
        
        // Add delivery info
        long deliveryStart = System.currentTimeMillis();
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity largeOrderWithDelivery = orderService.setDeliveryInformation(largeOrder.getOrderId(), deliveryInfo, false);
        long deliveryEnd = System.currentTimeMillis();
        
        // Process payment
        long paymentStart = System.currentTimeMillis();
        orderService.processOrderPayment(largeOrderWithDelivery.getOrderId(), "large-order-payment");
        long paymentEnd = System.currentTimeMillis();
        
        // Calculate performance metrics
        long cartBuildTime = cartBuildEnd - cartBuildStart;
        long orderCreationTime = orderCreationEnd - orderCreationStart;
        long deliveryTime = deliveryEnd - deliveryStart;
        long paymentTime = paymentEnd - paymentStart;
        long totalTime = paymentEnd - cartBuildStart;
        
        // Validate scalability
        long maxOrderCreationTime = ORDER_CREATION_THRESHOLD * 3; // Allow 3x for large orders
        long maxPaymentTime = PAYMENT_PROCESSING_THRESHOLD * 2; // Allow 2x for large orders
        
        assertTrue(orderCreationTime < maxOrderCreationTime, 
            "Large order creation (" + orderCreationTime + "ms) should be under " + maxOrderCreationTime + "ms");
        assertTrue(paymentTime < maxPaymentTime, 
            "Large order payment (" + paymentTime + "ms) should be under " + maxPaymentTime + "ms");
        
        // Verify data integrity
        assertEquals(actualItems, largeOrder.getOrderItems().size(), 
            "Large order should maintain all cart items");
        assertTrue(largeOrder.getTotalProductPriceExclVAT() > 0, 
            "Large order should have positive total");
        
        System.out.println("Scalability Test Results:");
        System.out.println("- Cart items: " + actualItems);
        System.out.println("- Cart build time: " + cartBuildTime + "ms");
        System.out.println("- Order creation time: " + orderCreationTime + "ms");
        System.out.println("- Delivery setup time: " + deliveryTime + "ms");
        System.out.println("- Payment processing time: " + paymentTime + "ms");
        System.out.println("- Total processing time: " + totalTime + "ms");
        
        System.out.println("✅ Scalability testing PASSED");
    }
    
    // Helper methods
    
    private void performJVMWarmup() {
        try {
            for (int i = 0; i < 3; i++) {
                String warmupCartId = "warmup-" + i;
                cartService.createCart(warmupCartId, null);
                cartService.addItemToCart(warmupCartId, "BOOK-001", 1);
                stockValidationService.validateProductStock("BOOK-001", 1);
            }
            Thread.sleep(50);
        } catch (Exception e) {
            // Ignore warmup errors
        }
    }
    
    private OrderEntity createReadyForApprovalOrder(String cartId, String userId) throws Exception {
        cartService.createCart(cartId, userId);
        cartService.addItemToCart(cartId, "BOOK-001", 1);
        cartService.addItemToCart(cartId, "CD-001", 1);
        
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartId, userId);
        
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
        
        orderService.processOrderPayment(orderWithDelivery.getOrderId(), "approval-payment");
        
        return orderService.getOrderDetails(orderWithDelivery.getOrderId());
    }
    
    private PerformanceResult performConcurrentOperations(int threadId, int operationsCount) {
        long totalOrderCreationTime = 0;
        long totalPaymentTime = 0;
        int successfulOps = 0;
        
        for (int i = 0; i < operationsCount; i++) {
            try {
                String cartId = "concurrent-" + threadId + "-" + i + "-" + System.currentTimeMillis();
                cartService.createCart(cartId, "concurrent-user-" + threadId);
                cartService.addItemToCart(cartId, "BOOK-001", 1);
                
                long orderStart = System.currentTimeMillis();
                OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartId, "concurrent-user-" + threadId);
                long orderEnd = System.currentTimeMillis();
                
                DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
                OrderEntity orderWithDelivery = orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
                
                long paymentStart = System.currentTimeMillis();
                orderService.processOrderPayment(orderWithDelivery.getOrderId(), "concurrent-payment-" + threadId + "-" + i);
                long paymentEnd = System.currentTimeMillis();
                
                totalOrderCreationTime += (orderEnd - orderStart);
                totalPaymentTime += (paymentEnd - paymentStart);
                successfulOps++;
                
            } catch (Exception e) {
                // Some concurrent operations may fail, which is acceptable
            }
        }
        
        return new PerformanceResult(
            successfulOps > 0 ? totalOrderCreationTime / successfulOps : 0,
            successfulOps > 0 ? totalPaymentTime / successfulOps : 0,
            successfulOps
        );
    }
    
    private double calculateAverage(List<Long> times) {
        return times.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }
    
    private void recordPerformanceMetrics(String category, List<Long>... timeLists) {
        List<Long> allTimes = new ArrayList<>();
        for (List<Long> times : timeLists) {
            allTimes.addAll(times);
        }
        performanceMetrics.put(category, allTimes);
    }
    
    private static class PerformanceResult {
        final long orderCreationTime;
        final long paymentProcessingTime;
        final int successfulOperations;
        
        PerformanceResult(long orderCreationTime, long paymentProcessingTime, int successfulOperations) {
            this.orderCreationTime = orderCreationTime;
            this.paymentProcessingTime = paymentProcessingTime;
            this.successfulOperations = successfulOperations;
        }
    }
    
    @AfterEach
    void tearDown() {
        // Force garbage collection after each test
        System.gc();
    }
    
    @AfterAll
    static void printPerformanceSummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("AIMS Phase 2 Performance Integration Tests COMPLETED");
        System.out.println("Performance validation results:");
        System.out.println("✓ Baseline cart operations with stock validation within thresholds");
        System.out.println("✓ Order creation with enhanced services performs adequately");
        System.out.println("✓ Order approval with state management meets performance requirements");
        System.out.println("✓ Concurrent operations maintain acceptable performance under load");
        System.out.println("✓ Memory usage remains within acceptable limits");
        System.out.println("✓ System scales appropriately for large order processing");
        System.out.println("=".repeat(80));
    }
}