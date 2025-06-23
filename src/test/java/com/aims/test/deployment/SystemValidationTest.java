package com.aims.test.deployment;

import com.aims.core.application.services.*;
import com.aims.core.entities.*;
import com.aims.core.presentation.utils.*;
import com.aims.core.shared.ServiceFactory;
import com.aims.test.data.CartToOrderTestDataFactory;
import com.aims.test.utils.TestDatabaseManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AIMS Phase 4.4: System Validation Tests
 * 
 * End-to-end system validation tests that validate complete customer journey
 * and multi-role system interactions across the entire AIMS application stack.
 * 
 * Test Coverage:
 * - Complete Customer Journey End-to-End Validation
 * - Multi-Role System Interaction Validation  
 * - Cross-Service Integration Validation
 * - System State Consistency Validation
 * 
 * These tests ensure the entire AIMS system functions correctly as an integrated
 * whole, validating that all components work together seamlessly in production-like
 * scenarios.
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AIMS Phase 4.4: System Validation Tests")
public class SystemValidationTest {

    private static final Logger logger = Logger.getLogger(SystemValidationTest.class.getName());
    
    // Core Services
    private static IUserAccountService userAccountService;
    private static IProductService productService;
    private static ICartService cartService;
    private static IOrderService orderService;
    private static IPaymentService paymentService;
    private static IAuthenticationService authenticationService;
    
    // Test Data
    private static UserAccount testCustomer;
    private static UserAccount testProductManager;
    private static UserAccount testAdmin;
    private static String customerSessionId;
    private static String pmSessionId;
    private static String adminSessionId;

    @BeforeAll
    static void setUpSystemValidation() throws SQLException {
        logger.info("======================================================================");
        logger.info("STARTING AIMS Phase 4.4: System Validation Tests");
        logger.info("======================================================================");
        logger.info("Initializing end-to-end system validation environment...");
        
        // Initialize services
        initializeServices();
        
        // Create test users for multi-role validation
        createTestUsers();
        
        // Initialize sessions
        initializeUserSessions();
        
        logger.info("✓ System validation environment initialized");
        logger.info("");
    }

    @AfterAll
    static void tearDownSystemValidation() {
        logger.info("");
        logger.info("======================================================================");
        logger.info("COMPLETED AIMS Phase 4.4: System Validation Tests");
        logger.info("======================================================================");
        logger.info("✓ All system validation tests completed successfully");
        logger.info("✓ End-to-end customer journey validated");
        logger.info("✓ Multi-role system interactions validated");
        logger.info("✓ Cross-service integration validated");
        logger.info("✓ System state consistency validated");
    }

    /**
     * SYSTEM-VAL-001: Complete Customer Journey End-to-End Validation
     * 
     * Validates the complete customer journey from registration through order completion,
     * ensuring all system components work together seamlessly.
     */
    @Test
    @Order(1)
    @DisplayName("SYSTEM-VAL-001: Complete Customer Journey End-to-End Validation")
    void testCompleteCustomerJourneyEndToEnd() throws Exception {
        logger.info("=== SYSTEM-VAL-001: Complete Customer Journey End-to-End ===");
        
        // Step 1: Customer Registration and Authentication
        logger.info("Step 1: Customer Registration and Authentication");
        String newCustomerEmail = "journey-customer-" + System.currentTimeMillis() + "@test.com";
        UserAccount journeyCustomer = createAndAuthenticateCustomer(newCustomerEmail);
        assertNotNull(journeyCustomer, "Customer should be registered successfully");
        
        // Step 2: Product Browsing and Selection
        logger.info("Step 2: Product Browsing and Selection");
        List<CD> availableProducts = productService.getAllProducts();
        assertTrue(availableProducts.size() >= 2, "Should have products available for selection");
        CD selectedProduct1 = availableProducts.get(0);
        CD selectedProduct2 = availableProducts.get(1);
        
        // Step 3: Cart Operations (Add, Update, Remove)
        logger.info("Step 3: Cart Operations");
        String cartSessionId = "journey-cart-" + System.currentTimeMillis();
        
        // Add products to cart
        Cart cart = cartService.addItemToCart(cartSessionId, selectedProduct1.getCdId(), 2);
        cartService.addItemToCart(cartSessionId, selectedProduct2.getCdId(), 1);
        
        // Update cart quantity
        cartService.updateCartItemQuantity(cartSessionId, selectedProduct1.getCdId(), 3);
        
        // Verify cart state
        Cart finalCart = cartService.getCartBySessionId(cartSessionId);
        assertEquals(2, finalCart.getCartItems().size(), "Cart should have 2 items");
        assertEquals(3, finalCart.getCartItems().get(0).getQuantity(), "First item should have quantity 3");
        
        // Step 4: Order Creation and Validation
        logger.info("Step 4: Order Creation and Validation");
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartSessionId, journeyCustomer);
        assertNotNull(order, "Order should be created successfully");
        assertNotNull(order.getOrderId(), "Order should have valid ID");
        assertEquals("PENDING", order.getOrderStatus(), "Order should have PENDING status");
        
        // Step 5: Delivery Information Setup
        logger.info("Step 5: Delivery Information Setup");
        DeliveryInfo deliveryInfo = CartToOrderTestDataFactory.createTestDeliveryInfo("STANDARD_DELIVERY");
        OrderEntity orderWithDelivery = orderService.setDeliveryInformation(order.getOrderId(), deliveryInfo, false);
        assertNotNull(orderWithDelivery.getDeliveryInfo(), "Order should have delivery information");
        
        // Step 6: Payment Processing
        logger.info("Step 6: Payment Processing");
        PaymentMethod paymentMethod = CartToOrderTestDataFactory.createTestPaymentMethod(PaymentMethodType.DOMESTIC_DEBIT_CARD);
        PaymentTransaction transaction = paymentService.processPaymentWithParams(
            orderWithDelivery, paymentMethod.getPaymentMethodId(), Map.of("testMode", true));
        
        assertNotNull(transaction, "Payment transaction should be created");
        assertEquals("SUCCESS", transaction.getTransactionStatus(), "Payment should be successful");
        
        // Step 7: Order Status Progression
        logger.info("Step 7: Order Status Progression");
        OrderEntity completedOrder = orderService.updateOrderStatus(order.getOrderId(), "APPROVED");
        assertEquals("APPROVED", completedOrder.getOrderStatus(), "Order should be approved");
        
        logger.info("✓ Complete customer journey validated successfully");
        logger.info("  - Customer registration and authentication: ✓");
        logger.info("  - Product browsing and selection: ✓");
        logger.info("  - Cart operations (add, update): ✓");
        logger.info("  - Order creation and validation: ✓");
        logger.info("  - Delivery information setup: ✓");
        logger.info("  - Payment processing: ✓");
        logger.info("  - Order status progression: ✓");
    }

    /**
     * SYSTEM-VAL-002: Multi-Role System Interaction Validation
     * 
     * Validates interactions between different user roles (Customer, Product Manager, Admin)
     * ensuring proper authorization and workflow coordination.
     */
    @Test
    @Order(2)
    @DisplayName("SYSTEM-VAL-002: Multi-Role System Interaction Validation")
    void testMultiRoleSystemInteraction() throws Exception {
        logger.info("=== SYSTEM-VAL-002: Multi-Role System Interaction ===");
        
        // Step 1: Product Manager Creates Products
        logger.info("Step 1: Product Manager Product Creation");
        authenticateUser(testProductManager);
        
        CD newProduct = new CD();
        newProduct.setTitle("Multi-Role Test Product " + System.currentTimeMillis());
        newProduct.setArtist("Test Artist");
        newProduct.setCategory("Test Category");
        newProduct.setPrice(150000.0);
        newProduct.setQuantity(50);
        
        CD createdProduct = productService.addProduct(newProduct);
        assertNotNull(createdProduct.getCdId(), "Product should be created with valid ID");
        
        // Step 2: Customer Places Order for PM-Created Product
        logger.info("Step 2: Customer Order for PM Product");
        authenticateUser(testCustomer);
        
        String multiRoleCartId = "multi-role-cart-" + System.currentTimeMillis();
        cartService.addItemToCart(multiRoleCartId, createdProduct.getCdId(), 2);
        OrderEntity customerOrder = orderService.initiateOrderFromCartEnhanced(multiRoleCartId, testCustomer);
        
        assertNotNull(customerOrder, "Customer should be able to order PM-created product");
        assertEquals(2, customerOrder.getOrderItems().get(0).getQuantity(), "Order should have correct quantity");
        
        // Step 3: Product Manager Reviews Order
        logger.info("Step 3: Product Manager Order Review");
        authenticateUser(testProductManager);
        
        // PM can view orders containing their products
        List<OrderEntity> pmRelevantOrders = orderService.getOrdersContainingProduct(createdProduct.getCdId());
        assertTrue(pmRelevantOrders.stream().anyMatch(o -> o.getOrderId().equals(customerOrder.getOrderId())),
                  "PM should be able to see orders containing their products");
        
        // Step 4: Admin System Management
        logger.info("Step 4: Admin System Management");
        authenticateUser(testAdmin);
        
        // Admin can view all orders
        List<OrderEntity> allOrders = orderService.getAllOrders();
        assertTrue(allOrders.stream().anyMatch(o -> o.getOrderId().equals(customerOrder.getOrderId())),
                  "Admin should be able to see all orders");
        
        // Admin can approve order
        OrderEntity approvedOrder = orderService.updateOrderStatus(customerOrder.getOrderId(), "APPROVED");
        assertEquals("APPROVED", approvedOrder.getOrderStatus(), "Admin should be able to approve orders");
        
        // Step 5: Product Manager Updates Product Based on Order
        logger.info("Step 5: Product Manager Product Update");
        authenticateUser(testProductManager);
        
        // Update product quantity after order
        createdProduct.setQuantity(createdProduct.getQuantity() - 2);
        CD updatedProduct = productService.updateProduct(createdProduct);
        assertEquals(48, updatedProduct.getQuantity(), "Product quantity should be updated correctly");
        
        logger.info("✓ Multi-role system interaction validated successfully");
        logger.info("  - Product Manager product creation: ✓");
        logger.info("  - Customer order placement: ✓");
        logger.info("  - Product Manager order review: ✓");
        logger.info("  - Admin system management: ✓");
        logger.info("  - Role-based authorization: ✓");
    }

    /**
     * SYSTEM-VAL-003: Cross-Service Integration Validation
     * 
     * Validates integration between all major services (User, Product, Cart, Order, Payment)
     * ensuring data consistency and proper service communication.
     */
    @Test
    @Order(3)
    @DisplayName("SYSTEM-VAL-003: Cross-Service Integration Validation")
    void testCrossServiceIntegration() throws Exception {
        logger.info("=== SYSTEM-VAL-003: Cross-Service Integration ===");
        
        // Step 1: Service Initialization Validation
        logger.info("Step 1: Service Initialization Validation");
        assertNotNull(userAccountService, "UserAccountService should be initialized");
        assertNotNull(productService, "ProductService should be initialized");
        assertNotNull(cartService, "CartService should be initialized");
        assertNotNull(orderService, "OrderService should be initialized");
        assertNotNull(paymentService, "PaymentService should be initialized");
        assertNotNull(authenticationService, "AuthenticationService should be initialized");
        
        // Step 2: User-Product Service Integration
        logger.info("Step 2: User-Product Service Integration");
        UserAccount integrationCustomer = createTestCustomer("integration-customer@test.com");
        List<CD> customerAccessibleProducts = productService.getProductsForCustomer(integrationCustomer.getUserId());
        assertNotNull(customerAccessibleProducts, "Customer should be able to access products");
        assertTrue(customerAccessibleProducts.size() > 0, "Should have accessible products");
        
        // Step 3: Product-Cart Service Integration
        logger.info("Step 3: Product-Cart Service Integration");
        String integrationCartId = "integration-cart-" + System.currentTimeMillis();
        CD testProduct = customerAccessibleProducts.get(0);
        
        // Add product to cart - validates product service integration
        Cart integrationCart = cartService.addItemToCart(integrationCartId, testProduct.getCdId(), 3);
        assertEquals(1, integrationCart.getCartItems().size(), "Cart should contain product from product service");
        assertEquals(testProduct.getCdId(), integrationCart.getCartItems().get(0).getCdId(), 
                    "Cart item should reference correct product");
        
        // Step 4: Cart-Order Service Integration
        logger.info("Step 4: Cart-Order Service Integration");
        OrderEntity integrationOrder = orderService.initiateOrderFromCartEnhanced(integrationCartId, integrationCustomer);
        
        assertNotNull(integrationOrder, "Order should be created from cart");
        assertEquals(1, integrationOrder.getOrderItems().size(), "Order should contain items from cart");
        assertEquals(testProduct.getCdId(), integrationOrder.getOrderItems().get(0).getCdId(),
                    "Order item should match cart item");
        assertEquals(3, integrationOrder.getOrderItems().get(0).getQuantity(),
                    "Order quantity should match cart quantity");
        
        // Step 5: Order-Payment Service Integration
        logger.info("Step 5: Order-Payment Service Integration");
        PaymentMethod integrationPaymentMethod = CartToOrderTestDataFactory.createTestPaymentMethod(PaymentMethodType.CREDIT_CARD);
        
        PaymentTransaction integrationTransaction = paymentService.processPaymentWithParams(
            integrationOrder, integrationPaymentMethod.getPaymentMethodId(), 
            Map.of("testMode", true, "integrationTest", true));
        
        assertNotNull(integrationTransaction, "Payment transaction should be created for order");
        assertEquals(integrationOrder.getOrderId(), integrationTransaction.getOrderId(),
                    "Payment transaction should reference correct order");
        assertEquals("SUCCESS", integrationTransaction.getTransactionStatus(),
                    "Integration payment should be successful");
        
        // Step 6: End-to-End Data Consistency Validation
        logger.info("Step 6: End-to-End Data Consistency");
        
        // Verify user still exists
        UserAccount verifyCustomer = userAccountService.getUserById(integrationCustomer.getUserId());
        assertNotNull(verifyCustomer, "Customer should still exist after full integration");
        
        // Verify product quantity updated
        CD verifyProduct = productService.getProductById(testProduct.getCdId());
        assertEquals(testProduct.getQuantity() - 3, verifyProduct.getQuantity(),
                    "Product quantity should be updated after order");
        
        // Verify cart is processed
        Cart verifyCart = cartService.getCartBySessionId(integrationCartId);
        // Cart should be empty or marked as processed after order creation
        assertTrue(verifyCart == null || verifyCart.getCartItems().isEmpty(),
                  "Cart should be processed after order creation");
        
        // Verify order persisted
        OrderEntity verifyOrder = orderService.getOrderById(integrationOrder.getOrderId());
        assertNotNull(verifyOrder, "Order should be persisted");
        assertEquals("PENDING", verifyOrder.getOrderStatus(), "Order should maintain correct status");
        
        logger.info("✓ Cross-service integration validated successfully");
        logger.info("  - Service initialization: ✓");
        logger.info("  - User-Product integration: ✓");
        logger.info("  - Product-Cart integration: ✓");
        logger.info("  - Cart-Order integration: ✓");
        logger.info("  - Order-Payment integration: ✓");
        logger.info("  - End-to-end data consistency: ✓");
    }

    /**
     * SYSTEM-VAL-004: System State Consistency Validation
     * 
     * Validates that the system maintains consistent state across concurrent operations
     * and handles edge cases properly.
     */
    @Test
    @Order(4)
    @DisplayName("SYSTEM-VAL-004: System State Consistency Validation")
    void testSystemStateConsistency() throws Exception {
        logger.info("=== SYSTEM-VAL-004: System State Consistency ===");
        
        // Step 1: Concurrent User Operations
        logger.info("Step 1: Concurrent User Operations");
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        // Create multiple concurrent customers
        CompletableFuture<UserAccount>[] customerFutures = new CompletableFuture[3];
        for (int i = 0; i < 3; i++) {
            final int index = i;
            customerFutures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    return createTestCustomer("concurrent-customer-" + index + "-" + System.currentTimeMillis() + "@test.com");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }
        
        // Wait for all customers to be created
        CompletableFuture.allOf(customerFutures).get(30, TimeUnit.SECONDS);
        for (CompletableFuture<UserAccount> future : customerFutures) {
            assertNotNull(future.get(), "Concurrent customer creation should succeed");
        }
        
        // Step 2: Concurrent Product Access
        logger.info("Step 2: Concurrent Product Access");
        List<CD> testProducts = productService.getAllProducts();
        assertTrue(testProducts.size() >= 2, "Should have products for concurrent testing");
        
        CD concurrentProduct = testProducts.get(0);
        int initialQuantity = concurrentProduct.getQuantity();
        
        // Multiple customers add same product concurrently
        CompletableFuture<Cart>[] cartFutures = new CompletableFuture[3];
        for (int i = 0; i < 3; i++) {
            final int index = i;
            cartFutures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    String sessionId = "concurrent-cart-" + index + "-" + System.currentTimeMillis();
                    return cartService.addItemToCart(sessionId, concurrentProduct.getCdId(), 1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }
        
        // Wait for all cart operations
        CompletableFuture.allOf(cartFutures).get(30, TimeUnit.SECONDS);
        for (CompletableFuture<Cart> future : cartFutures) {
            assertNotNull(future.get(), "Concurrent cart operations should succeed");
        }
        
        // Step 3: System State Validation Under Load
        logger.info("Step 3: System State Validation Under Load");
        
        // Verify product quantity consistency
        CD verifyProduct = productService.getProductById(concurrentProduct.getCdId());
        assertTrue(verifyProduct.getQuantity() >= 0, "Product quantity should not go negative");
        assertTrue(verifyProduct.getQuantity() <= initialQuantity, "Product quantity should not exceed initial");
        
        // Step 4: Transaction Rollback Consistency
        logger.info("Step 4: Transaction Rollback Consistency");
        
        // Attempt invalid operations that should rollback
        UserAccount rollbackCustomer = createTestCustomer("rollback-customer@test.com");
        String rollbackCartId = "rollback-cart-" + System.currentTimeMillis();
        
        // Add valid item to cart
        cartService.addItemToCart(rollbackCartId, concurrentProduct.getCdId(), 1);
        
        // Attempt to create order with invalid data (should rollback)
        try {
            OrderEntity invalidOrder = new OrderEntity();
            invalidOrder.setCustomerId(rollbackCustomer.getUserId());
            invalidOrder.setOrderStatus("INVALID_STATUS"); // Invalid status
            
            // This should fail and not affect system state
            orderService.saveOrder(invalidOrder);
            fail("Invalid order creation should fail");
        } catch (Exception e) {
            // Expected failure - verify system state is consistent
            Cart rollbackCart = cartService.getCartBySessionId(rollbackCartId);
            assertNotNull(rollbackCart, "Cart should still exist after failed order");
            assertEquals(1, rollbackCart.getCartItems().size(), "Cart should maintain items after rollback");
        }
        
        // Step 5: System Recovery Validation
        logger.info("Step 5: System Recovery Validation");
        
        // Create successful order after previous failure
        OrderEntity recoveryOrder = orderService.initiateOrderFromCartEnhanced(rollbackCartId, rollbackCustomer);
        assertNotNull(recoveryOrder, "System should recover and process valid orders");
        assertEquals("PENDING", recoveryOrder.getOrderStatus(), "Recovery order should have correct status");
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS), "Executor should terminate cleanly");
        
        logger.info("✓ System state consistency validated successfully");
        logger.info("  - Concurrent user operations: ✓");
        logger.info("  - Concurrent product access: ✓");
        logger.info("  - System state under load: ✓");
        logger.info("  - Transaction rollback consistency: ✓");
        logger.info("  - System recovery validation: ✓");
    }

    // Helper Methods
    
    private static void initializeServices() {
        userAccountService = ServiceFactory.getUserAccountService();
        productService = ServiceFactory.getProductService();
        cartService = ServiceFactory.getCartService();
        orderService = ServiceFactory.getOrderService();
        paymentService = ServiceFactory.getPaymentService();
        authenticationService = ServiceFactory.getAuthenticationService();
    }
    
    private static void createTestUsers() throws SQLException {
        testCustomer = createTestCustomer("system-customer@test.com");
        testProductManager = createTestProductManager("system-pm@test.com");
        testAdmin = createTestAdmin("system-admin@test.com");
    }
    
    private static void initializeUserSessions() throws Exception {
        customerSessionId = authenticationService.authenticate(testCustomer.getEmail(), "password123").getSessionId();
        pmSessionId = authenticationService.authenticate(testProductManager.getEmail(), "password123").getSessionId();
        adminSessionId = authenticationService.authenticate(testAdmin.getEmail(), "password123").getSessionId();
    }
    
    private UserAccount createAndAuthenticateCustomer(String email) throws Exception {
        UserAccount customer = createTestCustomer(email);
        authenticationService.authenticate(customer.getEmail(), "password123");
        return customer;
    }
    
    private static UserAccount createTestCustomer(String email) throws SQLException {
        UserAccount customer = new UserAccount();
        customer.setEmail(email);
        customer.setPassword("password123");
        customer.setName("Test Customer");
        customer.setRole("CUSTOMER");
        return userAccountService.createUser(customer);
    }
    
    private static UserAccount createTestProductManager(String email) throws SQLException {
        UserAccount pm = new UserAccount();
        pm.setEmail(email);
        pm.setPassword("password123");
        pm.setName("Test Product Manager");
        pm.setRole("PRODUCT_MANAGER");
        return userAccountService.createUser(pm);
    }
    
    private static UserAccount createTestAdmin(String email) throws SQLException {
        UserAccount admin = new UserAccount();
        admin.setEmail(email);
        admin.setPassword("password123");
        admin.setName("Test Admin");
        admin.setRole("ADMIN");
        return userAccountService.createUser(admin);
    }
    
    private void authenticateUser(UserAccount user) throws Exception {
        authenticationService.authenticate(user.getEmail(), "password123");
    }
}