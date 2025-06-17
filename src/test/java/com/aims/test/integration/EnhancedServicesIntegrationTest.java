package com.aims.test.integration;

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

/**
 * Enhanced Services Integration Test Suite
 * 
 * Tests the integration of all enhanced services (Order Data Loader, 
 * Cart Data Validation, Order Data Validation) with the core system
 * and validates their interaction with UI controllers.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EnhancedServicesIntegrationTest extends BaseUITest {
    
    private IOrderDataLoaderService orderDataLoaderService;
    private ICartDataValidationService cartDataValidationService;
    private IOrderDataValidationService orderDataValidationService;
    private ICartService cartService;
    private IOrderService orderService;
    private IDeliveryCalculationService deliveryCalculationService;
    private IDataFlowMonitoringService dataFlowMonitoringService;
    
    // Test data
    private String testCartSessionId;
    private String testOrderId;
    
    @BeforeEach
    void setUp() {
        // Initialize services from ServiceFactory
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        orderDataLoaderService = serviceFactory.getOrderDataLoaderService();
        cartDataValidationService = serviceFactory.getCartDataValidationService();
        orderDataValidationService = serviceFactory.getOrderDataValidationService();
        cartService = serviceFactory.getCartService();
        orderService = serviceFactory.getOrderService();
        deliveryCalculationService = serviceFactory.getDeliveryCalculationService();
        
        // Initialize monitoring service
        dataFlowMonitoringService = new DataFlowMonitoringServiceImpl();
        
        // Generate test identifiers
        testCartSessionId = "enhanced-test-cart-" + System.currentTimeMillis();
        testOrderId = "enhanced-test-order-" + System.currentTimeMillis();
        
        // Seed test data
        seedDataForTestCase("ENHANCED_SERVICES_INTEGRATION");
    }
    
    @Test
    @Order(1)
    @DisplayName("Order Data Loader Service Integration - With All Related Services")
    void testOrderDataLoaderService_Integration_WithAllRelatedServices() throws Exception {
        System.out.println("=== Testing Order Data Loader Service Integration ===");
        
        // Phase 1: Create test order with complete data
        OrderEntity testOrder = CartToOrderTestDataFactory.createCompleteTestOrder(testOrderId);
        
        // Phase 2: Test loading complete order data
        OrderEntity loadedOrder = orderDataLoaderService.loadCompleteOrderData(testOrder.getOrderId());
        
        // Validate order data completeness
        assertNotNull(loadedOrder, "Loaded order should not be null");
        assertTrue(orderDataLoaderService.validateOrderDataCompleteness(loadedOrder), 
                  "Order should have complete data");
        
        // Phase 3: Test integration with validation services
        var validationResult = orderDataValidationService.validateOrderForDisplay(loadedOrder);
        assertTrue(validationResult.isValid(), "Order should be valid for display");
        
        // Phase 4: Test lazy loading initialization validation
        assertTrue(orderDataLoaderService.validateLazyLoadingInitialization(loadedOrder),
                  "All lazy loading relationships should be initialized");
        
        // Phase 5: Test order summary DTO creation
        var orderSummaryDTO = orderDataLoaderService.createOrderSummaryDTO(loadedOrder);
        assertNotNull(orderSummaryDTO, "Order summary DTO should be created");
        assertNotNull(orderSummaryDTO.orderId(), "DTO should have order ID");
        
        System.out.println("✓ Order Data Loader Service integration test passed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Cart Data Validation Service Integration - With Order Creation")
    void testCartDataValidationService_Integration_WithOrderCreation() throws Exception {
        System.out.println("=== Testing Cart Data Validation Service Integration ===");
        
        // Phase 1: Create test cart with diverse products
        Cart testCart = CartToOrderTestDataFactory.createTestCartWithAllProductTypes(testCartSessionId);
        
        // Phase 2: Test cart validation before order creation
        var cartValidationResult = cartDataValidationService.validateCartForOrderCreation(testCart);
        assertTrue(cartValidationResult.isValid(), "Cart should be valid for order creation");
        
        // Phase 3: Test stock availability validation
        assertTrue(cartDataValidationService.validateStockAvailability(testCart),
                  "All cart items should have sufficient stock");
        
        // Phase 4: Test product metadata completeness
        assertTrue(cartDataValidationService.validateProductMetadataCompleteness(testCart),
                  "All products should have complete metadata");
        
        // Phase 5: Test cart enrichment with product metadata
        Cart enrichedCart = cartDataValidationService.enrichCartWithProductMetadata(testCart);
        assertNotNull(enrichedCart, "Enriched cart should not be null");
        assertEquals(testCart.getItems().size(), enrichedCart.getItems().size(),
                    "Enriched cart should have same number of items");
        
        // Phase 6: Test integration with order creation
        OrderEntity createdOrder = orderService.initiateOrderFromCartEnhanced(testCartSessionId, null);
        assertNotNull(createdOrder, "Order should be created successfully");
        
        // Phase 7: Validate data integrity after order creation
        assertTrue(cartDataValidationService.validateCartDataIntegrity(testCart),
                  "Cart data integrity should be maintained");
        
        System.out.println("✓ Cart Data Validation Service integration test passed");
    }
    
    @Test
    @Order(3)
    @DisplayName("Order Data Validation Service Integration - With UI Controllers")
    void testOrderDataValidationService_Integration_WithUIControllers() throws Exception {
        System.out.println("=== Testing Order Data Validation Service Integration ===");
        
        // Phase 1: Create test order for validation scenarios
        OrderEntity testOrder = CartToOrderTestDataFactory.createCompleteTestOrder(testOrderId + "_validation");
        
        // Phase 2: Test comprehensive order validation
        var comprehensiveResult = orderDataValidationService.validateOrderComprehensive(testOrder);
        assertTrue(comprehensiveResult.isValid(), "Order should pass comprehensive validation");
        
        // Phase 3: Test validation for different UI scenarios
        var displayResult = orderDataValidationService.validateOrderForDisplay(testOrder);
        assertTrue(displayResult.isValid(), "Order should be valid for display");
        
        var paymentResult = orderDataValidationService.validateOrderForPayment(testOrder);
        assertTrue(paymentResult.isValid(), "Order should be valid for payment");
        
        var navigationResult = orderDataValidationService.validateOrderForNavigation(testOrder, "OrderSummaryScreen");
        assertTrue(navigationResult.isValid(), "Order should be valid for navigation");
        
        // Phase 4: Test order items validation
        var itemsResult = orderDataValidationService.validateOrderItems(testOrder.getOrderItems());
        assertTrue(itemsResult.isValid(), "Order items should be valid");
        
        // Phase 5: Test pricing validation
        var pricingResult = orderDataValidationService.validateOrderPricing(testOrder);
        assertTrue(pricingResult.isValid(), "Order pricing should be valid");
        
        // Phase 6: Test delivery info validation
        DeliveryInfo testDeliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        testOrder.setDeliveryInfo(testDeliveryInfo);
        
        var deliveryResult = orderDataValidationService.validateDeliveryInfo(testDeliveryInfo);
        assertTrue(deliveryResult.isValid(), "Delivery info should be valid");
        
        // Phase 7: Test detailed validation report
        var detailedReport = orderDataValidationService.getDetailedValidationReport(testOrder);
        assertNotNull(detailedReport, "Detailed validation report should be generated");
        
        System.out.println("✓ Order Data Validation Service integration test passed");
    }
    
    @Test
    @Order(4)
    @DisplayName("Enhanced Controllers Integration - With All Services")
    void testEnhancedControllers_Integration_WithAllServices() throws Exception {
        System.out.println("=== Testing Enhanced Controllers Integration ===");
        
        // Phase 1: Test cart screen controller integration
        testCartScreenControllerIntegration();
        
        // Phase 2: Test delivery info controller integration
        testDeliveryInfoControllerIntegration();
        
        // Phase 3: Test order summary controller integration
        testOrderSummaryControllerIntegration();
        
        System.out.println("✓ Enhanced Controllers integration test passed");
    }
    
    @Test
    @Order(5)
    @DisplayName("Service Factory Dependency Injection - All Services Available")
    void testServiceFactory_DependencyInjection_AllServicesAvailable() throws Exception {
        System.out.println("=== Testing Service Factory Dependency Injection ===");
        
        // Test all enhanced services are properly injected
        assertNotNull(orderDataLoaderService, "Order Data Loader Service should be available");
        assertNotNull(cartDataValidationService, "Cart Data Validation Service should be available");
        assertNotNull(orderDataValidationService, "Order Data Validation Service should be available");
        
        // Test core services are available
        assertNotNull(cartService, "Cart Service should be available");
        assertNotNull(orderService, "Order Service should be available");
        assertNotNull(deliveryCalculationService, "Delivery Calculation Service should be available");
        
        // Test service factory static methods
        assertNotNull(ServiceFactory.getOrderDataLoaderService(), "Static access to Order Data Loader should work");
        assertNotNull(ServiceFactory.getCartDataValidationService(), "Static access to Cart Data Validation should work");
        assertNotNull(ServiceFactory.getOrderDataValidationService(), "Static access to Order Data Validation should work");
        
        // Test service interdependencies
        testServiceInterdependencies();
        
        System.out.println("✓ Service Factory dependency injection test passed");
    }
    
    @Test
    @Order(6)
    @DisplayName("Data Consistency Across All Services - No Data Loss")
    void testDataConsistency_AcrossAllServices_NoDataLoss() throws Exception {
        System.out.println("=== Testing Data Consistency Across All Services ===");
        
        // Phase 1: Create comprehensive test scenario
        Cart originalCart = CartToOrderTestDataFactory.createTestCartWithAllProductTypes(testCartSessionId + "_consistency");
        
        // Phase 2: Validate cart through validation service
        var cartValidation = cartDataValidationService.validateCartForOrderCreation(originalCart);
        assertTrue(cartValidation.isValid(), "Cart should be valid");
        
        // Phase 3: Convert to order
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(originalCart.getCartSessionId(), null);
        
        // Phase 4: Load complete order data
        OrderEntity completeOrder = orderDataLoaderService.loadCompleteOrderData(order.getOrderId());
        
        // Phase 5: Validate order through validation service
        var orderValidation = orderDataValidationService.validateOrderComprehensive(completeOrder);
        assertTrue(orderValidation.isValid(), "Order should be valid");
        
        // Phase 6: Validate data consistency
        validateDataConsistencyAcrossServices(originalCart, completeOrder);
        
        // Phase 7: Test with delivery information
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(completeOrder.getOrderId(), deliveryInfo, false);
        
        // Phase 8: Final validation after delivery info
        var finalValidation = orderDataValidationService.validateOrderForPayment(orderWithDelivery);
        assertTrue(finalValidation.isValid(), "Order with delivery should be valid for payment");
        
        System.out.println("✓ Data consistency across all services test passed");
    }
    
    // Helper methods for controller integration testing
    
    private void testCartScreenControllerIntegration() throws Exception {
        // Simulate cart screen controller operations
        Cart testCart = CartToOrderTestDataFactory.createTestCartWithAllProductTypes(testCartSessionId + "_cart_screen");
        
        // Test cart validation before checkout
        var validationResult = cartDataValidationService.validateCartForOrderCreation(testCart);
        assertTrue(validationResult.isValid(), "Cart should be valid for cart screen checkout");
        
        // Test order creation from cart screen
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(testCart.getCartSessionId(), null);
        assertNotNull(order, "Order should be created from cart screen");
    }
    
    private void testDeliveryInfoControllerIntegration() throws Exception {
        // Simulate delivery info screen controller operations
        OrderEntity testOrder = CartToOrderTestDataFactory.createCompleteTestOrder(testOrderId + "_delivery");
        
        // Test order loading for delivery screen
        OrderEntity loadedOrder = orderDataLoaderService.loadCompleteOrderData(testOrder.getOrderId());
        assertTrue(orderDataLoaderService.validateOrderDataCompleteness(loadedOrder),
                  "Order should have complete data for delivery screen");
        
        // Test delivery info validation
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        var deliveryValidation = orderDataValidationService.validateDeliveryInfo(deliveryInfo);
        assertTrue(deliveryValidation.isValid(), "Delivery info should be valid");
        
        // Test setting delivery information
        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(testOrder.getOrderId(), deliveryInfo, false);
        assertNotNull(orderWithDelivery.getDeliveryInfo(), "Order should have delivery info after setting");
    }
    
    private void testOrderSummaryControllerIntegration() throws Exception {
        // Simulate order summary screen controller operations
        OrderEntity testOrder = CartToOrderTestDataFactory.createCompleteTestOrder(testOrderId + "_summary");
        
        // Add delivery info
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        testOrder.setDeliveryInfo(deliveryInfo);
        
        // Test order validation for display
        var displayValidation = orderDataValidationService.validateOrderForDisplay(testOrder);
        assertTrue(displayValidation.isValid(), "Order should be valid for summary display");
        
        // Test order summary DTO creation
        var summaryDTO = orderDataLoaderService.createOrderSummaryDTO(testOrder);
        assertNotNull(summaryDTO, "Order summary DTO should be created");
        assertNotNull(summaryDTO.orderId(), "Summary DTO should have order ID");
        
        // Test validation for payment
        var paymentValidation = orderDataValidationService.validateOrderForPayment(testOrder);
        assertTrue(paymentValidation.isValid(), "Order should be valid for payment");
    }
    
    private void testServiceInterdependencies() throws Exception {
        // Test that services work together correctly
        OrderEntity testOrder = CartToOrderTestDataFactory.createCompleteTestOrder(testOrderId + "_interdep");
        
        // Test Order Data Loader + Order Data Validation
        OrderEntity loadedOrder = orderDataLoaderService.loadCompleteOrderData(testOrder.getOrderId());
        var validationResult = orderDataValidationService.validateOrderComprehensive(loadedOrder);
        assertTrue(validationResult.isValid(), "Loaded order should be valid");
        
        // Test Order Data Validation + Delivery Calculation
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        var deliveryValidation = orderDataValidationService.validateDeliveryInfo(deliveryInfo);
        assertTrue(deliveryValidation.isValid(), "Delivery info should be valid");
        
        float shippingFee = deliveryCalculationService.calculateShippingFee(loadedOrder, false);
        assertTrue(shippingFee >= 0, "Shipping fee should be calculated");
    }
    
    private void validateDataConsistencyAcrossServices(Cart originalCart, OrderEntity finalOrder) {
        // Validate item count consistency
        assertEquals(originalCart.getItems().size(), finalOrder.getOrderItems().size(),
                    "Item count should be consistent across services");
        
        // Validate pricing consistency
        float cartTotal = 0f;
        for (CartItem cartItem : originalCart.getItems()) {
            cartTotal += cartItem.getProduct().getPrice() * cartItem.getQuantity();
        }
        
        assertEquals(cartTotal, finalOrder.getTotalProductPriceExclVAT(), 0.01f,
                    "Total price should be consistent across services");
        
        // Validate product metadata preservation
        for (OrderItem orderItem : finalOrder.getOrderItems()) {
            assertNotNull(orderItem.getProduct().getTitle(), "Product title should be preserved");
            assertNotNull(orderItem.getProduct().getCategory(), "Product category should be preserved");
            assertTrue(orderItem.getPriceAtTimeOfOrder() > 0, "Price should be preserved");
        }
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup test data
        try {
            System.out.println("Cleaning up enhanced services integration test data");
            // In a real implementation, we would clean up test orders and carts
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}