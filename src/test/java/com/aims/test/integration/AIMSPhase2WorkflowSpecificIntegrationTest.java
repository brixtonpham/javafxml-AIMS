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

/**
 * AIMS Phase 2 Workflow-Specific Integration Tests
 * 
 * This test suite focuses on specific workflow scenarios to validate:
 * - Stock validation prevents overselling in various scenarios
 * - Order approval workflow with state management
 * - Delivery calculation integration with order processing
 * - Payment processing with final stock validation
 * - Cross-service data consistency and integrity
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AIMS Phase 2 - Workflow-Specific Integration Tests")
public class AIMSPhase2WorkflowSpecificIntegrationTest extends BaseUITest {
    
    private ICartService cartService;
    private IOrderService orderService;
    private IProductService productService;
    private IStockValidationService stockValidationService;
    private IOrderStateManagementService orderStateManagementService;
    private IDeliveryCalculationService deliveryCalculationService;
    private IPaymentService paymentService;
    
    @BeforeEach
    void setUp() {
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        cartService = serviceFactory.getCartService();
        orderService = serviceFactory.getOrderService();
        productService = serviceFactory.getProductService();
        stockValidationService = serviceFactory.getStockValidationService();
        orderStateManagementService = serviceFactory.getOrderStateManagementService();
        deliveryCalculationService = serviceFactory.getDeliveryCalculationService();
        paymentService = serviceFactory.getPaymentService();
        
        seedDataForTestCase("PHASE2_WORKFLOW_SPECIFIC");
        System.out.println("=== AIMS Phase 2 Workflow-Specific Integration Tests ===");
    }
    
    @Test
    @Order(1)
    @DisplayName("Stock Validation Prevents Overselling - Cart Level")
    void testStockValidationPreventsOverselling_CartLevel() throws Exception {
        System.out.println("\n=== TEST 1: Stock Validation Prevents Overselling at Cart Level ===");
        
        String cartId = "overselling-cart-" + System.currentTimeMillis();
        cartService.createCart(cartId, null);
        
        // First, add a reasonable quantity
        cartService.addItemToCart(cartId, "BOOK-001", 5);
        
        // Get current stock info
        IStockValidationService.StockInfo stockInfo = stockValidationService.getStockInfo("BOOK-001");
        int availableStock = stockInfo.getAvailableStock();
        
        // Try to add more than available stock
        try {
            cartService.addItemToCart(cartId, "BOOK-001", availableStock + 10);
            fail("Should prevent adding more items than available stock");
        } catch (InventoryException e) {
            assertTrue(e.getMessage().contains("Insufficient stock") || 
                      e.getMessage().contains("not enough stock"), 
                      "Exception should indicate insufficient stock");
        }
        
        // Verify cart still has the original valid quantity
        Cart cart = cartService.getCart(cartId);
        CartItem bookItem = cart.getItems().stream()
            .filter(item -> "BOOK-001".equals(item.getProduct().getProductId()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(bookItem, "Cart should still contain the book item");
        assertTrue(bookItem.getQuantity() <= availableStock, 
                  "Cart quantity should not exceed available stock");
        
        System.out.println("✓ Stock validation successfully prevented overselling at cart level");
    }
    
    @Test
    @Order(2)
    @DisplayName("Stock Validation Prevents Overselling - Order Creation Level")
    void testStockValidationPreventsOverselling_OrderCreationLevel() throws Exception {
        System.out.println("\n=== TEST 2: Stock Validation Prevents Overselling at Order Creation Level ===");
        
        String cartId = "order-overselling-" + System.currentTimeMillis();
        cartService.createCart(cartId, null);
        
        // Add valid quantities to cart
        cartService.addItemToCart(cartId, "CD-001", 3);
        cartService.addItemToCart(cartId, "DVD-001", 2);
        
        // Manually reduce stock after cart creation but before order creation
        productService.updateProductStock("CD-001", -10); // Reduce stock significantly
        
        try {
            // This should fail due to insufficient stock
            orderService.initiateOrderFromCartEnhanced(cartId, null);
            fail("Order creation should fail when stock becomes insufficient");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Stock validation failed") ||
                      e.getMessage().contains("Insufficient stock"),
                      "Exception should indicate stock validation failure");
        }
        
        // Restore stock for other tests
        productService.updateProductStock("CD-001", 15);
        
        System.out.println("✓ Stock validation successfully prevented overselling at order creation level");
    }
    
    @Test
    @Order(3)
    @DisplayName("Stock Validation Prevents Overselling - Payment Processing Level")
    void testStockValidationPreventsOverselling_PaymentProcessingLevel() throws Exception {
        System.out.println("\n=== TEST 3: Stock Validation Prevents Overselling at Payment Processing Level ===");
        
        String cartId = "payment-overselling-" + System.currentTimeMillis();
        cartService.createCart(cartId, null);
        cartService.addItemToCart(cartId, "DVD-001", 2);
        
        // Create order successfully
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartId, null);
        
        // Add delivery info
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
        
        // Reduce stock after order creation but before payment
        productService.updateProductStock("DVD-001", -10);
        
        try {
            // Payment processing should fail due to final stock validation
            orderService.processOrderPayment(orderWithDelivery.getOrderId(), "test-payment");
            fail("Payment processing should fail when stock becomes insufficient");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Stock validation failed"),
                      "Exception should indicate stock validation failure during payment");
        }
        
        // Verify order status reverted
        OrderEntity finalOrder = orderService.getOrderDetails(orderWithDelivery.getOrderId());
        assertEquals(OrderStatus.PENDING_DELIVERY_INFO, finalOrder.getOrderStatus(),
                    "Order status should revert when payment fails due to stock issues");
        
        // Restore stock
        productService.updateProductStock("DVD-001", 15);
        
        System.out.println("✓ Stock validation successfully prevented overselling at payment processing level");
    }
    
    @Test
    @Order(4)
    @DisplayName("Order Approval Workflow with State Management")
    void testOrderApprovalWorkflow_WithStateManagement() throws Exception {
        System.out.println("\n=== TEST 4: Order Approval Workflow with State Management ===");
        
        String cartId = "approval-workflow-" + System.currentTimeMillis();
        String managerId = "test-manager-approval";
        
        // Create and process order to PENDING_PROCESSING status
        cartService.createCart(cartId, null);
        cartService.addItemToCart(cartId, "BOOK-001", 1);
        cartService.addItemToCart(cartId, "CD-001", 1);
        
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartId, null);
        
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
        
        orderService.processOrderPayment(orderWithDelivery.getOrderId(), "test-payment");
        
        OrderEntity paidOrder = orderService.getOrderDetails(orderWithDelivery.getOrderId());
        assertEquals(OrderStatus.PENDING_PROCESSING, paidOrder.getOrderStatus());
        
        // Test successful approval
        orderService.approveOrder(paidOrder.getOrderId(), managerId);
        
        OrderEntity approvedOrder = orderService.getOrderDetails(paidOrder.getOrderId());
        assertEquals(OrderStatus.APPROVED, approvedOrder.getOrderStatus());
        
        // Test rejection workflow
        String cartId2 = "rejection-workflow-" + System.currentTimeMillis();
        cartService.createCart(cartId2, null);
        cartService.addItemToCart(cartId2, "BOOK-001", 1);
        
        OrderEntity order2 = orderService.initiateOrderFromCartEnhanced(cartId2, null);
        DeliveryInfo deliveryInfo2 = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithDelivery2 = orderService.setDeliveryInformation(order2.getOrderId(), deliveryInfo2, false);
        orderService.processOrderPayment(orderWithDelivery2.getOrderId(), "test-payment");
        
        OrderEntity paidOrder2 = orderService.getOrderDetails(orderWithDelivery2.getOrderId());
        
        // Test order rejection
        orderService.rejectOrder(paidOrder2.getOrderId(), managerId, "Quality concerns");
        
        OrderEntity rejectedOrder = orderService.getOrderDetails(paidOrder2.getOrderId());
        assertEquals(OrderStatus.REJECTED, rejectedOrder.getOrderStatus());
        
        System.out.println("✓ Order approval workflow with state management completed successfully");
    }
    
    @Test
    @Order(5)
    @DisplayName("Delivery Calculation Integration with Order Processing")
    void testDeliveryCalculationIntegration_WithOrderProcessing() throws Exception {
        System.out.println("\n=== TEST 5: Delivery Calculation Integration with Order Processing ===");
        
        String cartId = "delivery-calc-" + System.currentTimeMillis();
        cartService.createCart(cartId, null);
        cartService.addItemToCart(cartId, "BOOK-001", 2);
        cartService.addItemToCart(cartId, "CD-001", 3);
        
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartId, null);
        
        // Test standard delivery calculation
        DeliveryInfo standardDelivery = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        standardDelivery.setDeliveryProvinceCity("Hồ Chí Minh");
        
        OrderEntity orderWithStandardDelivery = orderService.setDeliveryInformation(
            order.getOrderId(), standardDelivery, false);
        
        assertTrue(orderWithStandardDelivery.getCalculatedDeliveryFee() >= 0,
                  "Standard delivery fee should be calculated");
        
        // Test rush delivery calculation
        String rushCartId = "rush-delivery-calc-" + System.currentTimeMillis();
        cartService.createCart(rushCartId, null);
        cartService.addItemToCart(rushCartId, "CD-001", 1); // Rush eligible
        cartService.addItemToCart(rushCartId, "DVD-001", 1); // Rush eligible
        
        OrderEntity rushOrder = orderService.initiateOrderFromCartEnhanced(rushCartId, null);
        
        DeliveryInfo rushDelivery = CartToOrderTestDataFactory.createTestDeliveryInfo("RUSH_DELIVERY");
        rushDelivery.setDeliveryProvinceCity("Hà Nội"); // Rush eligible location
        
        OrderEntity orderWithRushDelivery = orderService.setDeliveryInformation(
            rushOrder.getOrderId(), rushDelivery, true);
        
        assertTrue(orderWithRushDelivery.getCalculatedDeliveryFee() > orderWithStandardDelivery.getCalculatedDeliveryFee(),
                  "Rush delivery should cost more than standard delivery");
        
        // Test free shipping threshold
        String freeShippingCartId = "free-shipping-" + System.currentTimeMillis();
        cartService.createCart(freeShippingCartId, null);
        cartService.addItemToCart(freeShippingCartId, "BOOK-001", 10); // High value for free shipping
        cartService.addItemToCart(freeShippingCartId, "CD-001", 5);
        
        OrderEntity freeShippingOrder = orderService.initiateOrderFromCartEnhanced(freeShippingCartId, null);
        
        DeliveryInfo freeShippingDelivery = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithFreeShipping = orderService.setDeliveryInformation(
            freeShippingOrder.getOrderId(), freeShippingDelivery, false);
        
        // Check if free shipping is applied based on order value
        if (orderWithFreeShipping.getTotalProductPriceExclVAT() >= 500.0f) {
            assertEquals(0.0f, orderWithFreeShipping.getCalculatedDeliveryFee(), 0.01f,
                        "Free shipping should be applied for high-value orders");
        }
        
        System.out.println("✓ Delivery calculation integration with order processing completed successfully");
    }
    
    @Test
    @Order(6)
    @DisplayName("Payment Processing with Final Stock Validation")
    void testPaymentProcessing_WithFinalStockValidation() throws Exception {
        System.out.println("\n=== TEST 6: Payment Processing with Final Stock Validation ===");
        
        String cartId = "payment-stock-validation-" + System.currentTimeMillis();
        cartService.createCart(cartId, null);
        cartService.addItemToCart(cartId, "BOOK-001", 2);
        cartService.addItemToCart(cartId, "CD-001", 1);
        
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartId, null);
        
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
        
        // Record initial stock levels
        IStockValidationService.StockInfo bookStockBefore = stockValidationService.getStockInfo("BOOK-001");
        IStockValidationService.StockInfo cdStockBefore = stockValidationService.getStockInfo("CD-001");
        
        // Process payment successfully
        orderService.processOrderPayment(orderWithDelivery.getOrderId(), "test-payment-method");
        
        // Verify stock was properly updated
        IStockValidationService.StockInfo bookStockAfter = stockValidationService.getStockInfo("BOOK-001");
        IStockValidationService.StockInfo cdStockAfter = stockValidationService.getStockInfo("CD-001");
        
        assertEquals(bookStockBefore.getActualStock() - 2, bookStockAfter.getActualStock(),
                    "Book stock should be reduced by ordered quantity");
        assertEquals(cdStockBefore.getActualStock() - 1, cdStockAfter.getActualStock(),
                    "CD stock should be reduced by ordered quantity");
        
        // Verify order status and invoice creation
        OrderEntity paidOrder = orderService.getOrderDetails(orderWithDelivery.getOrderId());
        assertEquals(OrderStatus.PENDING_PROCESSING, paidOrder.getOrderStatus());
        assertNotNull(paidOrder.getInvoice(), "Invoice should be created after successful payment");
        
        System.out.println("✓ Payment processing with final stock validation completed successfully");
    }
    
    @Test
    @Order(7)
    @DisplayName("Cross-Service Data Consistency and Integrity")
    void testCrossServiceDataConsistency_AndIntegrity() throws Exception {
        System.out.println("\n=== TEST 7: Cross-Service Data Consistency and Integrity ===");
        
        String cartId = "data-consistency-" + System.currentTimeMillis();
        cartService.createCart(cartId, "test-user-consistency");
        
        // Add items with specific quantities and track data
        cartService.addItemToCart(cartId, "BOOK-001", 3);
        cartService.addItemToCart(cartId, "CD-001", 2);
        cartService.addItemToCart(cartId, "DVD-001", 1);
        
        Cart originalCart = cartService.getCart(cartId);
        
        // Calculate expected totals
        float expectedTotal = 0f;
        Map<String, Integer> originalQuantities = new HashMap<>();
        for (CartItem item : originalCart.getItems()) {
            expectedTotal += item.getProduct().getPrice() * item.getQuantity();
            originalQuantities.put(item.getProduct().getProductId(), item.getQuantity());
        }
        
        // Convert to order and verify data preservation
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartId, "test-user-consistency");
        
        // Verify item count consistency
        assertEquals(originalCart.getItems().size(), order.getOrderItems().size(),
                    "Order should have same number of items as cart");
        
        // Verify quantity consistency
        for (OrderItem orderItem : order.getOrderItems()) {
            String productId = orderItem.getProduct().getProductId();
            Integer originalQuantity = originalQuantities.get(productId);
            assertNotNull(originalQuantity, "Product should exist in original cart");
            assertEquals(originalQuantity.intValue(), orderItem.getQuantity(),
                        "Quantity should be preserved from cart to order");
        }
        
        // Verify price consistency
        assertEquals(expectedTotal, order.getTotalProductPriceExclVAT(), 0.01f,
                    "Total price should be consistent between cart and order");
        
        // Add delivery and verify total calculation consistency
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
        
        float expectedTotalWithVATAndDelivery = (expectedTotal * 1.10f) + orderWithDelivery.getCalculatedDeliveryFee();
        assertEquals(expectedTotalWithVATAndDelivery, orderWithDelivery.getTotalAmountPaid(), 0.01f,
                    "Total amount should include VAT and delivery fee");
        
        // Process payment and verify stock updates are consistent
        Map<String, Integer> stockBefore = new HashMap<>();
        for (OrderItem item : orderWithDelivery.getOrderItems()) {
            IStockValidationService.StockInfo stockInfo = stockValidationService.getStockInfo(item.getProduct().getProductId());
            stockBefore.put(item.getProduct().getProductId(), stockInfo.getActualStock());
        }
        
        orderService.processOrderPayment(orderWithDelivery.getOrderId(), "test-payment");
        
        // Verify stock was updated correctly for each item
        for (OrderItem item : orderWithDelivery.getOrderItems()) {
            IStockValidationService.StockInfo stockInfo = stockValidationService.getStockInfo(item.getProduct().getProductId());
            Integer beforeStock = stockBefore.get(item.getProduct().getProductId());
            assertEquals(beforeStock - item.getQuantity(), stockInfo.getActualStock(),
                        "Stock should be reduced by exact order quantity for " + item.getProduct().getProductId());
        }
        
        System.out.println("✓ Cross-service data consistency and integrity verified successfully");
    }
    
    @Test
    @Order(8)
    @DisplayName("Rush Order Eligibility and Fee Calculation Workflow")
    void testRushOrderEligibility_AndFeeCalculationWorkflow() throws Exception {
        System.out.println("\n=== TEST 8: Rush Order Eligibility and Fee Calculation Workflow ===");
        
        // Test 1: All items rush eligible
        String allRushCartId = "all-rush-" + System.currentTimeMillis();
        cartService.createCart(allRushCartId, null);
        cartService.addItemToCart(allRushCartId, "CD-001", 2);
        cartService.addItemToCart(allRushCartId, "DVD-001", 1);
        
        OrderEntity allRushOrder = orderService.initiateOrderFromCartEnhanced(allRushCartId, null);
        
        // Verify all items are rush eligible
        boolean allItemsRushEligible = allRushOrder.getOrderItems().stream()
            .allMatch(OrderItem::isEligibleForRushDelivery);
        assertTrue(allItemsRushEligible, "All CD/DVD items should be rush eligible");
        
        // Test rush delivery to eligible location
        DeliveryInfo rushDeliveryHanoi = CartToOrderTestDataFactory.createTestDeliveryInfo("RUSH_DELIVERY");
        rushDeliveryHanoi.setDeliveryProvinceCity("Hà Nội");
        
        OrderEntity orderWithRushHanoi = orderService.setDeliveryInformation(
            allRushOrder.getOrderId(), rushDeliveryHanoi, true);
        
        assertTrue(orderWithRushHanoi.getCalculatedDeliveryFee() > 0,
                  "Rush delivery to Hanoi should have delivery fee");
        
        // Test 2: Mixed rush eligibility
        String mixedRushCartId = "mixed-rush-" + System.currentTimeMillis();
        cartService.createCart(mixedRushCartId, null);
        cartService.addItemToCart(mixedRushCartId, "BOOK-001", 1); // Not rush eligible
        cartService.addItemToCart(mixedRushCartId, "CD-001", 1);   // Rush eligible
        
        OrderEntity mixedRushOrder = orderService.initiateOrderFromCartEnhanced(mixedRushCartId, null);
        
        boolean hasRushEligibleItems = mixedRushOrder.getOrderItems().stream()
            .anyMatch(OrderItem::isEligibleForRushDelivery);
        boolean hasNonRushItems = mixedRushOrder.getOrderItems().stream()
            .anyMatch(item -> !item.isEligibleForRushDelivery());
        
        assertTrue(hasRushEligibleItems, "Order should have some rush eligible items");
        assertTrue(hasNonRushItems, "Order should have some non-rush eligible items");
        
        // Should still allow rush delivery if any items are eligible
        DeliveryInfo mixedRushDelivery = CartToOrderTestDataFactory.createTestDeliveryInfo("RUSH_DELIVERY");
        mixedRushDelivery.setDeliveryProvinceCity("Hà Nội");
        
        OrderEntity mixedOrderWithRush = orderService.setDeliveryInformation(
            mixedRushOrder.getOrderId(), mixedRushDelivery, true);
        
        assertTrue(mixedOrderWithRush.getCalculatedDeliveryFee() > 0,
                  "Mixed order should still allow rush delivery with appropriate fee");
        
        // Test 3: No items rush eligible
        String noRushCartId = "no-rush-" + System.currentTimeMillis();
        cartService.createCart(noRushCartId, null);
        cartService.addItemToCart(noRushCartId, "BOOK-001", 3); // Books not rush eligible
        
        OrderEntity noRushOrder = orderService.initiateOrderFromCartEnhanced(noRushCartId, null);
        
        boolean noRushEligibleItems = noRushOrder.getOrderItems().stream()
            .noneMatch(OrderItem::isEligibleForRushDelivery);
        assertTrue(noRushEligibleItems, "Book-only order should have no rush eligible items");
        
        // Should prevent rush delivery
        DeliveryInfo noRushAttempt = CartToOrderTestDataFactory.createTestDeliveryInfo("RUSH_DELIVERY");
        noRushAttempt.setDeliveryProvinceCity("Hà Nội");
        
        try {
            orderService.setDeliveryInformation(noRushOrder.getOrderId(), noRushAttempt, true);
            fail("Should prevent rush delivery when no items are eligible");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("eligible for rush"),
                      "Exception should indicate no items eligible for rush delivery");
        }
        
        System.out.println("✓ Rush order eligibility and fee calculation workflow completed successfully");
    }
    
    @Test
    @Order(9)
    @DisplayName("Stock Reservation and Release Workflow")
    void testStockReservationAndReleaseWorkflow() throws Exception {
        System.out.println("\n=== TEST 9: Stock Reservation and Release Workflow ===");
        
        // Test stock reservation during order processing
        String reservationCartId = "reservation-test-" + System.currentTimeMillis();
        cartService.createCart(reservationCartId, null);
        cartService.addItemToCart(reservationCartId, "BOOK-001", 2);
        cartService.addItemToCart(reservationCartId, "CD-001", 1);
        
        // Check initial stock levels
        IStockValidationService.StockInfo bookStockInitial = stockValidationService.getStockInfo("BOOK-001");
        IStockValidationService.StockInfo cdStockInitial = stockValidationService.getStockInfo("CD-001");
        
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(reservationCartId, null);
        
        // Add delivery info (this might create reservations)
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
        
        // Check if stock is reserved during pending payment state
        IStockValidationService.StockInfo bookStockPending = stockValidationService.getStockInfo("BOOK-001");
        IStockValidationService.StockInfo cdStockPending = stockValidationService.getStockInfo("CD-001");
        
        // Available stock should be reduced even if actual stock isn't yet
        assertTrue(bookStockPending.getAvailableStock() <= bookStockInitial.getAvailableStock(),
                  "Available book stock should be reduced or equal during pending payment");
        assertTrue(cdStockPending.getAvailableStock() <= cdStockInitial.getAvailableStock(),
                  "Available CD stock should be reduced or equal during pending payment");
        
        // Process payment - this should finalize the stock reduction
        orderService.processOrderPayment(orderWithDelivery.getOrderId(), "test-payment");
        
        // Check final stock levels
        IStockValidationService.StockInfo bookStockFinal = stockValidationService.getStockInfo("BOOK-001");
        IStockValidationService.StockInfo cdStockFinal = stockValidationService.getStockInfo("CD-001");
        
        assertEquals(bookStockInitial.getActualStock() - 2, bookStockFinal.getActualStock(),
                    "Book actual stock should be reduced by ordered quantity");
        assertEquals(cdStockInitial.getActualStock() - 1, cdStockFinal.getActualStock(),
                    "CD actual stock should be reduced by ordered quantity");
        
        // Test stock release on order cancellation
        String cancellationCartId = "cancellation-test-" + System.currentTimeMillis();
        cartService.createCart(cancellationCartId, null);
        cartService.addItemToCart(cancellationCartId, "DVD-001", 2);
        
        IStockValidationService.StockInfo dvdStockBeforeOrder = stockValidationService.getStockInfo("DVD-001");
        
        OrderEntity cancellationOrder = orderService.initiateOrderFromCartEnhanced(cancellationCartId, null);
        DeliveryInfo cancellationDelivery = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity cancellationOrderWithDelivery = orderService.setDeliveryInformation(
            cancellationOrder.getOrderId(), cancellationDelivery, false);
        
        orderService.processOrderPayment(cancellationOrderWithDelivery.getOrderId(), "test-payment");
        
        // Verify stock was reduced
        IStockValidationService.StockInfo dvdStockAfterOrder = stockValidationService.getStockInfo("DVD-001");
        assertEquals(dvdStockBeforeOrder.getActualStock() - 2, dvdStockAfterOrder.getActualStock(),
                    "DVD stock should be reduced after order");
        
        // Cancel the order
        orderService.cancelOrder(cancellationOrderWithDelivery.getOrderId());
        
        // Verify stock was restored
        IStockValidationService.StockInfo dvdStockAfterCancellation = stockValidationService.getStockInfo("DVD-001");
        assertEquals(dvdStockBeforeOrder.getActualStock(), dvdStockAfterCancellation.getActualStock(),
                    "DVD stock should be restored after order cancellation");
        
        System.out.println("✓ Stock reservation and release workflow completed successfully");
    }
    
    @Test
    @Order(10)
    @DisplayName("Complex Multi-Product Order Workflow Validation")
    void testComplexMultiProductOrderWorkflow_Validation() throws Exception {
        System.out.println("\n=== TEST 10: Complex Multi-Product Order Workflow Validation ===");
        
        String complexCartId = "complex-order-" + System.currentTimeMillis();
        String userId = "complex-user-" + System.currentTimeMillis();
        cartService.createCart(complexCartId, userId);
        
        // Add multiple types of products with different characteristics
        cartService.addItemToCart(complexCartId, "BOOK-001", 3);   // Not rush eligible
        cartService.addItemToCart(complexCartId, "CD-001", 2);     // Rush eligible
        cartService.addItemToCart(complexCartId, "DVD-001", 1);    // Rush eligible
        
        // Verify cart state before conversion
        Cart complexCart = cartService.getCart(complexCartId);
        assertEquals(3, complexCart.getItems().size(), "Cart should have 3 different product types");
        
        // Convert to order with enhanced validation
        OrderEntity complexOrder = orderService.initiateOrderFromCartEnhanced(complexCartId, userId);
        
        // Verify order item characteristics
        assertEquals(3, complexOrder.getOrderItems().size(), "Order should have 3 order items");
        
        long rushEligibleCount = complexOrder.getOrderItems().stream()
            .filter(OrderItem::isEligibleForRushDelivery)
            .count();
        assertEquals(2, rushEligibleCount, "Should have 2 rush eligible items (CD and DVD)");
        
        long nonRushEligibleCount = complexOrder.getOrderItems().stream()
            .filter(item -> !item.isEligibleForRushDelivery())
            .count();
        assertEquals(1, nonRushEligibleCount, "Should have 1 non-rush eligible item (Book)");
        
        // Test standard delivery
        DeliveryInfo standardDelivery = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        standardDelivery.setDeliveryProvinceCity("Đà Nẵng");
        
        OrderEntity orderWithStandardDelivery = orderService.setDeliveryInformation(
            complexOrder.getOrderId(), standardDelivery, false);
        
        assertTrue(orderWithStandardDelivery.getCalculatedDeliveryFee() >= 0,
                  "Standard delivery fee should be calculated");
        assertEquals(OrderStatus.PENDING_PAYMENT, orderWithStandardDelivery.getOrderStatus());
        
        // Test payment processing with complex order
        orderService.processOrderPayment(orderWithStandardDelivery.getOrderId(), "test-payment-complex");
        
        OrderEntity paidComplexOrder = orderService.getOrderDetails(orderWithStandardDelivery.getOrderId());
        assertEquals(OrderStatus.PENDING_PROCESSING, paidComplexOrder.getOrderStatus());
        assertNotNull(paidComplexOrder.getInvoice(), "Invoice should be created");
        
        // Verify all product stocks were updated correctly
        for (OrderItem item : paidComplexOrder.getOrderItems()) {
            IStockValidationService.StockInfo stockInfo = 
                stockValidationService.getStockInfo(item.getProduct().getProductId());
            assertTrue(stockInfo.getActualStock() >= 0, 
                      "Stock for " + item.getProduct().getProductId() + " should remain non-negative");
        }
        
        // Test order approval
        orderService.approveOrder(paidComplexOrder.getOrderId(), "complex-manager");
        
        OrderEntity approvedComplexOrder = orderService.getOrderDetails(paidComplexOrder.getOrderId());
        assertEquals(OrderStatus.APPROVED, approvedComplexOrder.getOrderStatus());
        
        // Verify data integrity throughout the complex workflow
        assertEquals(3, approvedComplexOrder.getOrderItems().size(), 
                    "Order should maintain all items through the workflow");
        assertTrue(approvedComplexOrder.getTotalAmountPaid() > 0, 
                  "Total amount should be positive");
        assertTrue(approvedComplexOrder.getTotalProductPriceInclVAT() > approvedComplexOrder.getTotalProductPriceExclVAT(),
                  "Price including VAT should be higher than excluding VAT");
        
        System.out.println("✓ Complex multi-product order workflow validation completed successfully");
    }
    
    @AfterEach
    void tearDown() {
        System.out.println("--- Workflow-specific test completed ---");
    }
    
    @AfterAll
    static void printSummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("AIMS Phase 2 Workflow-Specific Integration Tests COMPLETED");
        System.out.println("Validated specific workflow scenarios:");
        System.out.println("✓ Stock validation prevents overselling at all levels");
        System.out.println("✓ Order approval workflow with comprehensive state management");
        System.out.println("✓ Delivery calculation integration with various scenarios");
        System.out.println("✓ Payment processing with robust final stock validation");
        System.out.println("✓ Cross-service data consistency and integrity maintenance");
        System.out.println("✓ Rush order eligibility and fee calculation workflows");
        System.out.println("✓ Stock reservation and release mechanisms");
        System.out.println("✓ Complex multi-product order processing validation");
        System.out.println("=".repeat(80));
    }
}