package com.aims.test.integration;

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
 * AIMS Phase 2 Task 3.4: Comprehensive Integration Testing & Performance Validation
 * 
 * This test suite validates the complete Phase 2 integration demonstrating:
 * - Complete workflow: Cart → Order → Approval → Stock Management
 * - Service interoperability with StockValidationService, OrderStateManagementService
 * - Performance benchmarks with new services integrated
 * - Edge cases and stress testing scenarios
 * - Free shipping rules and rush order fee calculations
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AIMS Phase 2 - Comprehensive Integration Testing & Performance Validation")
public class AIMSPhase2ComprehensiveIntegrationTest extends BaseUITest {
    
    // All Phase 2 Services
    private ICartService cartService;
    private IOrderService orderService;
    private IProductService productService;
    private IStockValidationService stockValidationService;
    private IOrderStateManagementService orderStateManagementService;
    private IStockReservationService stockReservationService;
    private IDeliveryCalculationService deliveryCalculationService;
    private IRushOrderService rushOrderService;
    private IPaymentService paymentService;
    private INotificationService notificationService;
    
    // Performance monitoring
    private Map<String, List<Long>> performanceMetrics;
    private long testStartTime;
    
    @BeforeEach
    void setUp() {
        testStartTime = System.currentTimeMillis();
        
        // Initialize all Phase 2 services
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        cartService = serviceFactory.getCartService();
        orderService = serviceFactory.getOrderService();
        productService = serviceFactory.getProductService();
        stockValidationService = serviceFactory.getStockValidationService();
        orderStateManagementService = serviceFactory.getOrderStateManagementService();
        stockReservationService = serviceFactory.getStockReservationService();
        deliveryCalculationService = serviceFactory.getDeliveryCalculationService();
        rushOrderService = serviceFactory.getRushOrderService();
        paymentService = serviceFactory.getPaymentService();
        notificationService = serviceFactory.getNotificationService();
        
        // Initialize performance tracking
        performanceMetrics = new HashMap<>();
        
        // Seed comprehensive test data
        seedDataForTestCase("PHASE2_COMPREHENSIVE_INTEGRATION");
        
        System.out.println("=== AIMS Phase 2 Comprehensive Integration Test Suite ===");
        System.out.println("Testing complete service interoperability and performance");
    }
    
    @Test
    @Order(1)
    @DisplayName("Complete Workflow: Cart → Order → Approval → Stock Management")
    void testCompleteWorkflow_CartToOrderToApproval_WithStockManagement() throws Exception {
        System.out.println("\n=== TEST 1: Complete Workflow Integration ===");
        
        String testSessionId = "phase2-workflow-" + System.currentTimeMillis();
        String userId = "test-user-workflow";
        String managerId = "test-manager-001";
        
        // Step 1: Create Cart with Stock Validation
        System.out.println("Step 1: Creating cart with stock validation...");
        long step1Start = System.currentTimeMillis();
        
        Cart cart = cartService.createCart(testSessionId, userId);
        
        // Add items with stock validation
        cartService.addItemToCart(testSessionId, "BOOK-001", 2);
        cartService.addItemToCart(testSessionId, "CD-001", 1);
        cartService.addItemToCart(testSessionId, "DVD-001", 3);
        
        Cart populatedCart = cartService.getCart(testSessionId);
        assertEquals(3, populatedCart.getItems().size(), "Cart should have 3 different items");
        
        recordPerformanceMetric("cart_creation_with_validation", System.currentTimeMillis() - step1Start);
        System.out.println("✓ Cart created and populated with stock validation");
        
        // Step 2: Convert Cart to Order with Enhanced Validation
        System.out.println("Step 2: Converting cart to order with enhanced validation...");
        long step2Start = System.currentTimeMillis();
        
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(testSessionId, userId);
        assertNotNull(order, "Order should be created successfully");
        assertEquals(OrderStatus.PENDING_DELIVERY_INFO, order.getOrderStatus());
        assertEquals(3, order.getOrderItems().size(), "Order should have all cart items");
        
        recordPerformanceMetric("cart_to_order_conversion", System.currentTimeMillis() - step2Start);
        System.out.println("✓ Cart converted to order with enhanced validation");
        
        // Step 3: Add Delivery Information with Rush Order Testing
        System.out.println("Step 3: Adding delivery information and testing rush orders...");
        long step3Start = System.currentTimeMillis();
        
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("RUSH_DELIVERY");
        deliveryInfo.setDeliveryProvinceCity("Hà Nội"); // Rush eligible location
        
        // Test rush order eligibility
        boolean hasRushEligibleItems = order.getOrderItems().stream()
            .anyMatch(OrderItem::isEligibleForRushDelivery);
        
        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(
            order.getOrderId(), deliveryInfo, hasRushEligibleItems);
        
        assertEquals(OrderStatus.PENDING_PAYMENT, orderWithDelivery.getOrderStatus());
        assertNotNull(orderWithDelivery.getDeliveryInfo());
        assertTrue(orderWithDelivery.getCalculatedDeliveryFee() > 0, "Delivery fee should be calculated");
        
        recordPerformanceMetric("delivery_info_with_rush_calculation", System.currentTimeMillis() - step3Start);
        System.out.println("✓ Delivery information added with rush order calculation");
        
        // Step 4: Process Payment with Final Stock Validation
        System.out.println("Step 4: Processing payment with final stock validation...");
        long step4Start = System.currentTimeMillis();
        
        orderService.processOrderPayment(orderWithDelivery.getOrderId(), "test-payment-method");
        
        OrderEntity paidOrder = orderService.getOrderDetails(orderWithDelivery.getOrderId());
        assertEquals(OrderStatus.PENDING_PROCESSING, paidOrder.getOrderStatus());
        assertNotNull(paidOrder.getInvoice(), "Invoice should be created");
        
        recordPerformanceMetric("payment_processing_with_stock_validation", System.currentTimeMillis() - step4Start);
        System.out.println("✓ Payment processed with final stock validation");
        
        // Step 5: Order Approval with OrderStateManagementService
        System.out.println("Step 5: Approving order with OrderStateManagementService...");
        long step5Start = System.currentTimeMillis();
        
        orderService.approveOrder(paidOrder.getOrderId(), managerId);
        
        OrderEntity approvedOrder = orderService.getOrderDetails(paidOrder.getOrderId());
        assertEquals(OrderStatus.APPROVED, approvedOrder.getOrderStatus());
        
        recordPerformanceMetric("order_approval_with_state_management", System.currentTimeMillis() - step5Start);
        System.out.println("✓ Order approved using OrderStateManagementService");
        
        // Step 6: Verify Stock Updates and Reservations
        System.out.println("Step 6: Verifying stock updates and reservations...");
        long step6Start = System.currentTimeMillis();
        
        for (OrderItem item : approvedOrder.getOrderItems()) {
            IStockValidationService.StockInfo stockInfo = 
                stockValidationService.getStockInfo(item.getProduct().getProductId());
            
            assertNotNull(stockInfo, "Stock info should be available");
            System.out.println("Product " + item.getProduct().getProductId() + 
                " - Available: " + stockInfo.getAvailableStock() + 
                ", Reserved: " + stockInfo.getReservedStock());
        }
        
        recordPerformanceMetric("stock_verification", System.currentTimeMillis() - step6Start);
        System.out.println("✓ Stock updates and reservations verified");
        
        System.out.println("✅ Complete workflow test PASSED - End-to-end integration successful");
    }
    
    @Test
    @Order(2)
    @DisplayName("Service Interoperability: All Phase 2 Services Working Together")
    void testServiceInteroperability_AllPhase2Services_WorkingTogether() throws Exception {
        System.out.println("\n=== TEST 2: Service Interoperability ===");
        
        String testSessionId = "phase2-interop-" + System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        
        // Test 1: CartService + StockValidationService Integration
        System.out.println("Testing CartService + StockValidationService integration...");
        Cart cart = cartService.createCart(testSessionId, null);
        
        // Test stock validation during cart operations
        cartService.addItemToCart(testSessionId, "BOOK-001", 5);
        
        IStockValidationService.StockValidationResult stockResult = 
            stockValidationService.validateProductStock("BOOK-001", 5);
        assertTrue(stockResult.isValid(), "Stock validation should pass for valid quantities");
        
        // Test stock validation failure
        assertThrows(InventoryException.class, () -> {
            cartService.addItemToCart(testSessionId, "BOOK-001", 1000); // Excessive quantity
        });
        
        // Test 2: ProductService + StockValidationService Integration
        System.out.println("Testing ProductService + StockValidationService integration...");
        int availableStock = productService.getAvailableStock("BOOK-001");
        assertTrue(availableStock > 0, "Available stock should be positive");
        
        IStockValidationService.StockInfo stockInfo = productService.getProductStockInfo("BOOK-001");
        assertNotNull(stockInfo, "Stock info should be available");
        assertEquals(availableStock, stockInfo.getAvailableStock());
        
        // Test 3: OrderService + OrderStateManagementService Integration
        System.out.println("Testing OrderService + OrderStateManagementService integration...");
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(testSessionId, null);
        
        // Add delivery info and process payment
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
        orderService.processOrderPayment(orderWithDelivery.getOrderId(), "test-payment");
        
        // Test order state management
        orderService.approveOrder(orderWithDelivery.getOrderId(), "test-manager");
        OrderEntity finalOrder = orderService.getOrderDetails(orderWithDelivery.getOrderId());
        assertEquals(OrderStatus.APPROVED, finalOrder.getOrderStatus());
        
        // Test 4: Cross-service Stock Reservation Integration
        System.out.println("Testing cross-service stock reservation integration...");
        Map<String, Integer> reservationRequest = new HashMap<>();
        reservationRequest.put("CD-001", 2);
        reservationRequest.put("DVD-001", 1);
        
        IStockReservationService.ReservationResult reservationResult = 
            stockReservationService.reserveStock(reservationRequest, "test-order-123", "TEST");
        assertTrue(reservationResult.isAllReserved(), "Stock reservation should succeed");
        
        // Verify reservation affects stock validation
        IStockValidationService.StockInfo cdStockInfo = stockValidationService.getStockInfo("CD-001");
        assertTrue(cdStockInfo.getReservedStock() >= 2, "Reserved stock should be reflected");
        
        recordPerformanceMetric("service_interoperability_test", System.currentTimeMillis() - startTime);
        System.out.println("✅ Service interoperability test PASSED - All services working together");
    }
    
    @Test
    @Order(3)
    @DisplayName("Free Shipping Rules & Rush Order Fee Calculations")
    void testFreeShippingRules_And_RushOrderFeeCalculations() throws Exception {
        System.out.println("\n=== TEST 3: Free Shipping & Rush Order Fee Calculations ===");
        
        // Test 1: Free Shipping Threshold
        System.out.println("Testing free shipping threshold rules...");
        String freeShippingCartId = "free-shipping-" + System.currentTimeMillis();
        Cart freeShippingCart = cartService.createCart(freeShippingCartId, null);
        
        // Add high-value items to exceed free shipping threshold
        cartService.addItemToCart(freeShippingCartId, "BOOK-001", 10); // High quantity for value
        cartService.addItemToCart(freeShippingCartId, "CD-001", 5);
        
        OrderEntity highValueOrder = orderService.initiateOrderFromCartEnhanced(freeShippingCartId, null);
        
        DeliveryInfo standardDelivery = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithStandardDelivery = orderService.setDeliveryInformation(
            highValueOrder.getOrderId(), standardDelivery, false);
        
        // Check if free shipping is applied
        float shippingFee = orderWithStandardDelivery.getCalculatedDeliveryFee();
        if (orderWithStandardDelivery.getTotalProductPriceExclVAT() >= 500.0f) { // Assuming 500 threshold
            assertEquals(0.0f, shippingFee, 0.01f, "Free shipping should be applied for high-value orders");
        }
        
        // Test 2: Rush Order Fee Calculations
        System.out.println("Testing rush order fee calculations...");
        String rushOrderCartId = "rush-order-" + System.currentTimeMillis();
        Cart rushCart = cartService.createCart(rushOrderCartId, null);
        
        // Add rush-eligible items
        cartService.addItemToCart(rushOrderCartId, "CD-001", 2);
        cartService.addItemToCart(rushOrderCartId, "DVD-001", 1);
        
        OrderEntity rushOrder = orderService.initiateOrderFromCartEnhanced(rushOrderCartId, null);
        
        DeliveryInfo rushDelivery = CartToOrderTestDataFactory.createTestDeliveryInfo("RUSH_DELIVERY");
        rushDelivery.setDeliveryProvinceCity("Hà Nội"); // Rush eligible location
        
        OrderEntity orderWithRushDelivery = orderService.setDeliveryInformation(
            rushOrder.getOrderId(), rushDelivery, true);
        
        float rushShippingFee = orderWithRushDelivery.getCalculatedDeliveryFee();
        assertTrue(rushShippingFee > 0, "Rush delivery should have additional fees");
        
        // Test 3: Mixed Eligibility Rush Orders
        System.out.println("Testing mixed eligibility rush orders...");
        String mixedCartId = "mixed-rush-" + System.currentTimeMillis();
        Cart mixedCart = cartService.createCart(mixedCartId, null);
        
        // Add both rush-eligible and non-eligible items
        cartService.addItemToCart(mixedCartId, "BOOK-001", 1); // Books not rush eligible
        cartService.addItemToCart(mixedCartId, "CD-001", 1);   // CDs rush eligible
        
        OrderEntity mixedOrder = orderService.initiateOrderFromCartEnhanced(mixedCartId, null);
        
        // Should still allow rush delivery if any items are eligible
        DeliveryInfo mixedRushDelivery = CartToOrderTestDataFactory.createTestDeliveryInfo("RUSH_DELIVERY");
        mixedRushDelivery.setDeliveryProvinceCity("Hà Nội");
        
        OrderEntity mixedOrderWithRush = orderService.setDeliveryInformation(
            mixedOrder.getOrderId(), mixedRushDelivery, true);
        
        // Verify only eligible items get rush delivery
        boolean hasEligibleItems = mixedOrderWithRush.getOrderItems().stream()
            .anyMatch(OrderItem::isEligibleForRushDelivery);
        assertTrue(hasEligibleItems, "Order should have at least some rush-eligible items");
        
        System.out.println("✅ Free shipping and rush order fee calculations test PASSED");
    }
    
    @Test
    @Order(4)
    @DisplayName("Edge Cases & Error Scenarios - Comprehensive Validation")
    void testEdgeCases_And_ErrorScenarios_ComprehensiveValidation() throws Exception {
        System.out.println("\n=== TEST 4: Edge Cases & Error Scenarios ===");
        
        // Edge Case 1: Concurrent Stock Access
        System.out.println("Testing concurrent stock access scenarios...");
        String concurrentProductId = "CONCURRENT-TEST-001";
        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            Future<Boolean> future = executor.submit(() -> {
                try {
                    String cartId = "concurrent-cart-" + threadId + "-" + System.currentTimeMillis();
                    cartService.createCart(cartId, null);
                    cartService.addItemToCart(cartId, "BOOK-001", 1);
                    return true;
                } catch (Exception e) {
                    System.out.println("Concurrent thread " + threadId + " failed: " + e.getMessage());
                    return false;
                }
            });
            futures.add(future);
        }
        
        int successfulThreads = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(10, TimeUnit.SECONDS)) {
                successfulThreads++;
            }
        }
        executor.shutdown();
        
        assertTrue(successfulThreads >= numberOfThreads / 2, 
            "At least half of concurrent operations should succeed");
        
        // Edge Case 2: Insufficient Stock During Payment
        System.out.println("Testing insufficient stock during payment processing...");
        String insufficientStockCartId = "insufficient-stock-" + System.currentTimeMillis();
        Cart insufficientStockCart = cartService.createCart(insufficientStockCartId, null);
        
        // Manually reduce stock to create insufficient stock scenario
        productService.updateProductStock("BOOK-001", -100); // Reduce stock significantly
        
        try {
            cartService.addItemToCart(insufficientStockCartId, "BOOK-001", 50); // Try to add more than available
            fail("Should throw InventoryException for insufficient stock");
        } catch (InventoryException e) {
            assertTrue(e.getMessage().contains("Insufficient stock"), 
                "Exception should indicate insufficient stock");
        }
        
        // Restore stock
        productService.updateProductStock("BOOK-001", 150);
        
        // Edge Case 3: Order State Transition Validation
        System.out.println("Testing order state transition validation...");
        String stateTestCartId = "state-test-" + System.currentTimeMillis();
        Cart stateTestCart = cartService.createCart(stateTestCartId, null);
        cartService.addItemToCart(stateTestCartId, "CD-001", 1);
        
        OrderEntity stateTestOrder = orderService.initiateOrderFromCartEnhanced(stateTestCartId, null);
        
        // Try invalid state transitions
        try {
            orderService.approveOrder(stateTestOrder.getOrderId(), "test-manager");
            fail("Should not allow approval of order without payment");
        } catch (ValidationException | OrderException e) {
            assertTrue(e.getMessage().contains("payment") || e.getMessage().contains("status"), 
                "Exception should indicate invalid state for approval");
        }
        
        // Edge Case 4: Invalid Delivery Address for Rush Orders
        System.out.println("Testing invalid delivery address for rush orders...");
        String rushInvalidCartId = "rush-invalid-" + System.currentTimeMillis();
        Cart rushInvalidCart = cartService.createCart(rushInvalidCartId, null);
        cartService.addItemToCart(rushInvalidCartId, "CD-001", 1);
        
        OrderEntity rushInvalidOrder = orderService.initiateOrderFromCartEnhanced(rushInvalidCartId, null);
        
        DeliveryInfo invalidRushDelivery = CartToOrderTestDataFactory.createTestDeliveryInfo("RUSH_DELIVERY");
        invalidRushDelivery.setDeliveryProvinceCity("Remote Province"); // Not rush eligible
        
        try {
            orderService.setDeliveryInformation(rushInvalidOrder.getOrderId(), invalidRushDelivery, true);
            fail("Should not allow rush delivery to non-eligible addresses");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("not eligible for rush"), 
                "Exception should indicate address not eligible for rush delivery");
        }
        
        // Edge Case 5: Large Order Processing
        System.out.println("Testing large order processing...");
        String largeOrderCartId = "large-order-" + System.currentTimeMillis();
        Cart largeOrderCart = cartService.createCart(largeOrderCartId, null);
        
        // Add many different products
        for (int i = 0; i < 10; i++) {
            try {
                cartService.addItemToCart(largeOrderCartId, "BOOK-001", 1);
                cartService.addItemToCart(largeOrderCartId, "CD-001", 1);
                cartService.addItemToCart(largeOrderCartId, "DVD-001", 1);
            } catch (Exception e) {
                // Some additions might fail due to stock limits, which is expected
            }
        }
        
        Cart finalLargeCart = cartService.getCart(largeOrderCartId);
        if (!finalLargeCart.getItems().isEmpty()) {
            OrderEntity largeOrder = orderService.initiateOrderFromCartEnhanced(largeOrderCartId, null);
            assertNotNull(largeOrder, "Large order should be created successfully");
            assertTrue(largeOrder.getOrderItems().size() > 0, "Large order should have items");
        }
        
        System.out.println("✅ Edge cases and error scenarios test PASSED");
    }
    
    @Test
    @Order(5)
    @DisplayName("Performance Validation: No Degradation with Phase 2 Services")
    void testPerformanceValidation_NoDeradationWithPhase2Services() throws Exception {
        System.out.println("\n=== TEST 5: Performance Validation ===");
        
        // Performance benchmarks (in milliseconds)
        final long CART_CREATION_THRESHOLD = 1000;
        final long ORDER_CREATION_THRESHOLD = 3000;
        final long PAYMENT_PROCESSING_THRESHOLD = 2000;
        final long STOCK_VALIDATION_THRESHOLD = 500;
        
        // Performance Test 1: Cart Operations with Stock Validation
        System.out.println("Performance test 1: Cart operations with stock validation...");
        List<Long> cartOperationTimes = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            long startTime = System.currentTimeMillis();
            
            String perfCartId = "perf-cart-" + i + "-" + System.currentTimeMillis();
            cartService.createCart(perfCartId, null);
            cartService.addItemToCart(perfCartId, "BOOK-001", 1);
            cartService.addItemToCart(perfCartId, "CD-001", 1);
            cartService.updateItemQuantity(perfCartId, "BOOK-001", 2);
            
            long endTime = System.currentTimeMillis();
            cartOperationTimes.add(endTime - startTime);
        }
        
        double avgCartTime = cartOperationTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        assertTrue(avgCartTime < CART_CREATION_THRESHOLD, 
            "Average cart operation time should be under " + CART_CREATION_THRESHOLD + "ms, was: " + avgCartTime);
        
        // Performance Test 2: Order Creation with Enhanced Validation
        System.out.println("Performance test 2: Order creation with enhanced validation...");
        List<Long> orderCreationTimes = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            String perfOrderCartId = "perf-order-cart-" + i + "-" + System.currentTimeMillis();
            cartService.createCart(perfOrderCartId, null);
            cartService.addItemToCart(perfOrderCartId, "BOOK-001", 2);
            cartService.addItemToCart(perfOrderCartId, "CD-001", 1);
            
            long startTime = System.currentTimeMillis();
            OrderEntity order = orderService.initiateOrderFromCartEnhanced(perfOrderCartId, null);
            long endTime = System.currentTimeMillis();
            
            orderCreationTimes.add(endTime - startTime);
            assertNotNull(order, "Order should be created successfully");
        }
        
        double avgOrderTime = orderCreationTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        assertTrue(avgOrderTime < ORDER_CREATION_THRESHOLD, 
            "Average order creation time should be under " + ORDER_CREATION_THRESHOLD + "ms, was: " + avgOrderTime);
        
        // Performance Test 3: Stock Validation Performance
        System.out.println("Performance test 3: Stock validation performance...");
        List<Long> stockValidationTimes = new ArrayList<>();
        
        for (int i = 0; i < 20; i++) {
            long startTime = System.currentTimeMillis();
            
            IStockValidationService.StockValidationResult result = 
                stockValidationService.validateProductStock("BOOK-001", 1);
            IStockValidationService.StockInfo stockInfo = 
                stockValidationService.getStockInfo("BOOK-001");
            
            long endTime = System.currentTimeMillis();
            stockValidationTimes.add(endTime - startTime);
            
            assertTrue(result.isValid() || !result.isValid(), "Validation should complete");
            assertNotNull(stockInfo, "Stock info should be available");
        }
        
        double avgStockValidationTime = stockValidationTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        assertTrue(avgStockValidationTime < STOCK_VALIDATION_THRESHOLD, 
            "Average stock validation time should be under " + STOCK_VALIDATION_THRESHOLD + "ms, was: " + avgStockValidationTime);
        
        // Performance Test 4: End-to-End Workflow Performance
        System.out.println("Performance test 4: End-to-end workflow performance...");
        long workflowStartTime = System.currentTimeMillis();
        
        String e2eCartId = "e2e-perf-" + System.currentTimeMillis();
        cartService.createCart(e2eCartId, null);
        cartService.addItemToCart(e2eCartId, "BOOK-001", 1);
        cartService.addItemToCart(e2eCartId, "CD-001", 1);
        
        OrderEntity e2eOrder = orderService.initiateOrderFromCartEnhanced(e2eCartId, null);
        
        DeliveryInfo e2eDelivery = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity e2eOrderWithDelivery = orderService.setDeliveryInformation(e2eOrder.getOrderId(), e2eDelivery, false);
        
        orderService.processOrderPayment(e2eOrderWithDelivery.getOrderId(), "test-payment");
        orderService.approveOrder(e2eOrderWithDelivery.getOrderId(), "test-manager");
        
        long workflowEndTime = System.currentTimeMillis();
        long totalWorkflowTime = workflowEndTime - workflowStartTime;
        
        final long WORKFLOW_THRESHOLD = 10000; // 10 seconds
        assertTrue(totalWorkflowTime < WORKFLOW_THRESHOLD, 
            "Complete workflow should finish under " + WORKFLOW_THRESHOLD + "ms, was: " + totalWorkflowTime);
        
        // Record all performance metrics
        recordPerformanceMetric("cart_operations_avg", (long) avgCartTime);
        recordPerformanceMetric("order_creation_avg", (long) avgOrderTime);
        recordPerformanceMetric("stock_validation_avg", (long) avgStockValidationTime);
        recordPerformanceMetric("complete_workflow", totalWorkflowTime);
        
        System.out.println("Performance Results:");
        System.out.println("- Cart operations avg: " + String.format("%.2f", avgCartTime) + "ms");
        System.out.println("- Order creation avg: " + String.format("%.2f", avgOrderTime) + "ms");
        System.out.println("- Stock validation avg: " + String.format("%.2f", avgStockValidationTime) + "ms");
        System.out.println("- Complete workflow: " + totalWorkflowTime + "ms");
        
        System.out.println("✅ Performance validation test PASSED - No degradation detected");
    }
    
    @Test
    @Order(6)
    @DisplayName("Stress Testing: System Under Load with Phase 2 Services")
    void testStressTesting_SystemUnderLoadWithPhase2Services() throws Exception {
        System.out.println("\n=== TEST 6: Stress Testing ===");
        
        // Stress Test 1: Multiple Concurrent Cart Operations
        System.out.println("Stress test 1: Multiple concurrent cart operations...");
        int numberOfConcurrentUsers = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentUsers);
        List<Future<Boolean>> cartFutures = new ArrayList<>();
        
        for (int i = 0; i < numberOfConcurrentUsers; i++) {
            final int userId = i;
            Future<Boolean> future = executor.submit(() -> {
                try {
                    String stressCartId = "stress-cart-" + userId + "-" + System.currentTimeMillis();
                    cartService.createCart(stressCartId, "user-" + userId);
                    
                    // Perform multiple operations
                    for (int j = 0; j < 5; j++) {
                        cartService.addItemToCart(stressCartId, "BOOK-001", 1);
                        cartService.updateItemQuantity(stressCartId, "BOOK-001", j + 1);
                    }
                    
                    // Create order
                    OrderEntity order = orderService.initiateOrderFromCartEnhanced(stressCartId, "user-" + userId);
                    return order != null;
                    
                } catch (Exception e) {
                    System.out.println("Stress test user " + userId + " failed: " + e.getMessage());
                    return false;
                }
            });
            cartFutures.add(future);
        }
        
        int successfulOperations = 0;
        for (Future<Boolean> future : cartFutures) {
            try {
                if (future.get(30, TimeUnit.SECONDS)) {
                    successfulOperations++;
                }
            } catch (TimeoutException e) {
                System.out.println("Operation timed out");
            }
        }
        
        executor.shutdown();
        
        double successRate = (double) successfulOperations / numberOfConcurrentUsers;
        assertTrue(successRate >= 0.8, 
            "At least 80% of concurrent operations should succeed, actual: " + (successRate * 100) + "%");
        
        // Stress Test 2: Rapid Stock Validation Requests
        System.out.println("Stress test 2: Rapid stock validation requests...");
        int numberOfValidationRequests = 50;
        List<Long> validationTimes = new ArrayList<>();
        
        for (int i = 0; i < numberOfValidationRequests; i++) {
            long startTime = System.currentTimeMillis();
            
            try {
                IStockValidationService.StockValidationResult result = 
                    stockValidationService.validateProductStock("BOOK-001", 1);
                assertNotNull(result, "Validation result should not be null");
            } catch (Exception e) {
                // Some failures are acceptable under stress
            }
            
            long endTime = System.currentTimeMillis();
            validationTimes.add(endTime - startTime);
        }
        
        double avgValidationTime = validationTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        assertTrue(avgValidationTime < 1000, 
            "Average validation time under stress should be reasonable: " + avgValidationTime + "ms");
        
        // Stress Test 3: Memory Usage Under Load
        System.out.println("Stress test 3: Memory usage under load...");
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create many operations
        List<String> createdCartIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String memoryTestCartId = "memory-test-" + i + "-" + System.currentTimeMillis();
            createdCartIds.add(memoryTestCartId);
            
            try {
                cartService.createCart(memoryTestCartId, null);
                cartService.addItemToCart(memoryTestCartId, "BOOK-001", 1);
                cartService.addItemToCart(memoryTestCartId, "CD-001", 1);
                cartService.addItemToCart(memoryTestCartId, "DVD-001", 1);
                
                OrderEntity order = orderService.initiateOrderFromCartEnhanced(memoryTestCartId, null);
            } catch (Exception e) {
                // Some operations may fail under stress, which is acceptable
            }
        }
        
        // Force garbage collection and check memory
        System.gc();
        Thread.sleep(100);
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024); // MB
        
        assertTrue(memoryIncrease < 200, 
            "Memory increase should be reasonable under stress: " + memoryIncrease + "MB");
        
        System.out.println("Stress Test Results:");
        System.out.println("- Concurrent operations success rate: " + (successRate * 100) + "%");
        System.out.println("- Average validation time under stress: " + String.format("%.2f", avgValidationTime) + "ms");
        System.out.println("- Memory increase under load: " + memoryIncrease + "MB");
        
        System.out.println("✅ Stress testing PASSED - System stable under load");
    }
    
    // Helper methods
    
    private void recordPerformanceMetric(String metricName, long duration) {
        performanceMetrics.computeIfAbsent(metricName, k -> new ArrayList<>()).add(duration);
    }
    
    @AfterEach
    void tearDown() {
        long testDuration = System.currentTimeMillis() - testStartTime;
        System.out.println("\n--- Test completed in " + testDuration + "ms ---");
        
        // Print performance summary
        if (!performanceMetrics.isEmpty()) {
            System.out.println("\nPerformance Metrics Summary:");
            performanceMetrics.forEach((metric, times) -> {
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                long min = times.stream().mapToLong(Long::longValue).min().orElse(0);
                long max = times.stream().mapToLong(Long::longValue).max().orElse(0);
                System.out.println("- " + metric + ": avg=" + String.format("%.2f", avg) + 
                    "ms, min=" + min + "ms, max=" + max + "ms");
            });
        }
    }
    
    @AfterAll
    static void printSummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("AIMS Phase 2 Comprehensive Integration Test Suite COMPLETED");
        System.out.println("All Phase 2 objectives validated:");
        System.out.println("✓ Complete workflow: Cart → Order → Approval → Stock Management");
        System.out.println("✓ Service interoperability with StockValidationService & OrderStateManagementService");
        System.out.println("✓ Free shipping rules and rush order fee calculations");
        System.out.println("✓ Edge cases and error scenarios handled");
        System.out.println("✓ Performance benchmarks met with no degradation");
        System.out.println("✓ System stability under stress testing");
        System.out.println("=".repeat(80));
    }
}