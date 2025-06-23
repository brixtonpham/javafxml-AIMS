package com.aims.test.compliance;

import com.aims.core.application.services.*;
import com.aims.core.entities.*;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.test.base.BaseUITest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AIMS Phase 4.1: Product Manager Constraints Compliance Tests
 * 
 * Validates product manager operational constraints from AIMS-ProblemStatement-v2.0.pdf:
 * - Lines 16-17: Max 2 price updates per day per product
 * - Lines 18-19: Max 30 operations per day for security
 * - Lines 38-40: Price constraints 30%-150% validation
 * 
 * Total Tests: 8 product manager compliance validation tests
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductManagerConstraintsComplianceTest extends BaseUITest {

    private static final Logger logger = Logger.getLogger(ProductManagerConstraintsComplianceTest.class.getName());

    // Business Rule Constants from Problem Statement
    private static final int MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY = 2; // Lines 16-17
    private static final int MAX_DAILY_OPERATIONS_FOR_SECURITY = 30; // Lines 18-19
    private static final float PRICE_CONSTRAINT_MIN_PERCENTAGE = 0.30f; // Line 38: 30% minimum
    private static final float PRICE_CONSTRAINT_MAX_PERCENTAGE = 1.50f; // Line 40: 150% maximum

    // Core Services
    private IOperationConstraintService operationConstraintService;
    private IPriceManagementService priceManagementService;
    private IProductService productService;

    // Test Data
    private String testManagerId;
    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        logger.info("=== Setting up Product Manager Constraints Compliance Tests ===");
        
        // Initialize services
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        operationConstraintService = serviceFactory.getOperationConstraintService();
        priceManagementService = serviceFactory.getPriceManagementService();
        productService = serviceFactory.getProductService();
        
        // Generate test manager ID
        testManagerId = "pm-compliance-" + System.currentTimeMillis();
        
        // Setup test products
        setupConstraintTestProducts();
        
        logger.info("✓ Product manager constraints compliance test setup completed");
    }

    // ========================================
    // Price Update Daily Limits Compliance Tests (Lines 16-17)
    // ========================================

    @Test
    @Order(1)
    @DisplayName("PS-PM-001: Max 2 price updates per day per product - Lines 16-17")
    void testMaxPriceUpdatesPerProductPerDay() throws Exception {
        logger.info("=== Testing Max 2 Price Updates Per Product Per Day - Lines 16-17 ===");
        
        Product testProduct = testProducts.get(0);
        String productId = testProduct.getProductId();
        float originalPrice = testProduct.getPrice();
        float baseProductValue = originalPrice; // Assume price equals product value for testing
        
        // Test first price update (should succeed)
        float newPrice1 = calculateValidPrice(baseProductValue, 1.10f); // 10% increase
        IPriceManagementService.PriceValidationResult result1 = 
            priceManagementService.validatePriceUpdate(productId, newPrice1, baseProductValue, testManagerId);
        
        assertTrue(result1.isValid(), "First price update should be allowed");
        
        // Record the first update
        priceManagementService.recordPriceUpdate(productId, testManagerId, originalPrice, newPrice1);
        
        // Test second price update (should succeed)
        float newPrice2 = calculateValidPrice(baseProductValue, 1.20f); // 20% increase
        IPriceManagementService.PriceValidationResult result2 = 
            priceManagementService.validatePriceUpdate(productId, newPrice2, baseProductValue, testManagerId);
        
        assertTrue(result2.isValid(), "Second price update should be allowed");
        
        // Record the second update
        priceManagementService.recordPriceUpdate(productId, testManagerId, newPrice1, newPrice2);
        
        // Test third price update (should fail - exceeds daily limit)
        float newPrice3 = calculateValidPrice(baseProductValue, 1.30f); // 30% increase
        IPriceManagementService.PriceValidationResult result3 = 
            priceManagementService.validatePriceUpdate(productId, newPrice3, baseProductValue, testManagerId);
        
        assertFalse(result3.isValid(), 
            "Third price update should be rejected (exceeds max 2 updates per product per day)");
        assertTrue(result3.getMessage().toLowerCase().contains("daily limit") || 
                  result3.getMessage().toLowerCase().contains("maximum"), 
            "Error message should indicate daily limit exceeded");
        
        // Verify daily price update count
        List<IPriceManagementService.PriceUpdateRecord> dailyUpdates = 
            priceManagementService.getDailyPriceUpdates(testManagerId, LocalDate.now());
        
        long productUpdatesCount = dailyUpdates.stream()
            .filter(record -> record.getProductId().equals(productId))
            .count();
        
        assertEquals(MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY, productUpdatesCount,
            "Should have exactly " + MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY + " price updates for the product today");
        
        logger.info("✓ Price update daily limit per product compliance verified");
    }

    @Test
    @Order(2)
    @DisplayName("PS-PM-002: Different products can each have 2 price updates - Lines 16-17")
    void testPriceUpdateLimitPerProductIndependence() throws Exception {
        logger.info("=== Testing Price Update Limit Independence Per Product - Lines 16-17 ===");
        
        Product product1 = testProducts.get(0);
        Product product2 = testProducts.get(1);
        
        // Update product 1 twice (should succeed)
        for (int i = 1; i <= 2; i++) {
            float newPrice = calculateValidPrice(product1.getPrice(), 1.0f + (i * 0.1f));
            boolean canUpdate = priceManagementService.canUpdatePrice(product1.getProductId(), testManagerId);
            assertTrue(canUpdate, "Should allow price update " + i + " for product 1");
            
            priceManagementService.recordPriceUpdate(
                product1.getProductId(), testManagerId, product1.getPrice(), newPrice);
        }
        
        // Update product 2 twice (should also succeed independently)
        for (int i = 1; i <= 2; i++) {
            float newPrice = calculateValidPrice(product2.getPrice(), 1.0f + (i * 0.1f));
            boolean canUpdate = priceManagementService.canUpdatePrice(product2.getProductId(), testManagerId);
            assertTrue(canUpdate, "Should allow price update " + i + " for product 2");
            
            priceManagementService.recordPriceUpdate(
                product2.getProductId(), testManagerId, product2.getPrice(), newPrice);
        }
        
        // Third update on product 1 should fail
        boolean canUpdateProduct1Again = priceManagementService.canUpdatePrice(product1.getProductId(), testManagerId);
        assertFalse(canUpdateProduct1Again, "Should not allow third price update for product 1");
        
        // Third update on product 2 should also fail
        boolean canUpdateProduct2Again = priceManagementService.canUpdatePrice(product2.getProductId(), testManagerId);
        assertFalse(canUpdateProduct2Again, "Should not allow third price update for product 2");
        
        logger.info("✓ Price update limits apply independently per product");
    }

    // ========================================
    // Daily Operations Security Limits Compliance Tests (Lines 18-19)
    // ========================================

    @Test
    @Order(3)
    @DisplayName("PS-PM-003: Max 30 operations per day for security - Lines 18-19")
    void testMaxDailyOperationsForSecurity() throws Exception {
        logger.info("=== Testing Max 30 Operations Per Day For Security - Lines 18-19 ===");
        
        String securityTestManagerId = "security-test-" + System.currentTimeMillis();
        
        // Perform operations up to the limit
        int successfulOperations = 0;
        List<String> testProductIds = new ArrayList<>();
        
        // Create additional test products for this test
        for (int i = 0; i < 35; i++) {
            Product product = createTestProduct("SEC_TEST_" + i, "Security Test Product " + i, 50000.0f);
            testProductIds.add(product.getProductId());
        }
        
        // Perform various operations up to the daily limit
        for (int i = 0; i < MAX_DAILY_OPERATIONS_FOR_SECURITY + 5; i++) {
            try {
                IOperationConstraintService.OperationType operation = 
                    (i % 2 == 0) ? IOperationConstraintService.OperationType.EDIT 
                                : IOperationConstraintService.OperationType.PRICE_UPDATE;
                
                String productId = testProductIds.get(i % testProductIds.size());
                
                // Check if operation is allowed
                boolean isAllowed = false;
                if (operation == IOperationConstraintService.OperationType.EDIT) {
                    isAllowed = operationConstraintService.canEditProduct(securityTestManagerId, productId);
                } else {
                    isAllowed = operationConstraintService.canUpdatePrice(securityTestManagerId, productId);
                }
                
                if (isAllowed) {
                    // Record the operation
                    operationConstraintService.recordOperation(securityTestManagerId, operation, 
                        List.of(productId));
                    successfulOperations++;
                } else {
                    // Should be rejected after reaching limit
                    assertTrue(successfulOperations >= MAX_DAILY_OPERATIONS_FOR_SECURITY,
                        "Operations should be rejected only after reaching the daily limit of " + 
                        MAX_DAILY_OPERATIONS_FOR_SECURITY);
                    break;
                }
                
            } catch (ValidationException e) {
                // Expected when limit is exceeded
                assertTrue(successfulOperations >= MAX_DAILY_OPERATIONS_FOR_SECURITY,
                    "Validation exception should occur only after reaching daily limit");
                break;
            }
        }
        
        // Verify the exact limit was enforced
        assertEquals(MAX_DAILY_OPERATIONS_FOR_SECURITY, successfulOperations,
            "Should allow exactly " + MAX_DAILY_OPERATIONS_FOR_SECURITY + " operations per day");
        
        // Verify quota status
        IOperationConstraintService.OperationQuotaStatus quotaStatus = 
            operationConstraintService.getQuotaStatus(securityTestManagerId, LocalDate.now());
        
        assertEquals(MAX_DAILY_OPERATIONS_FOR_SECURITY, quotaStatus.getDailyOperationsUsed(),
            "Quota status should reflect " + MAX_DAILY_OPERATIONS_FOR_SECURITY + " operations used");
        assertEquals(0, quotaStatus.getDailyOperationsRemaining(),
            "Should have 0 operations remaining for today");
        
        logger.info("✓ Daily operations security limit compliance verified");
    }

    @Test
    @Order(4)
    @DisplayName("PS-PM-004: Operation types count toward daily security limit - Lines 18-19")
    void testOperationTypesCountTowardDailyLimit() throws Exception {
        logger.info("=== Testing Operation Types Count Toward Daily Limit - Lines 18-19 ===");
        
        String testManagerId2 = "ops-test-" + System.currentTimeMillis();
        Product testProduct = testProducts.get(0);
        
        // Perform a mix of different operation types
        int editOperations = 10;
        int priceUpdateOperations = 10;
        int deleteOperations = 10; // Total: 30 operations (at limit)
        
        // Perform edit operations
        for (int i = 0; i < editOperations; i++) {
            operationConstraintService.validateSingleOperation(testManagerId2, 
                IOperationConstraintService.OperationType.EDIT, testProduct.getProductId());
            operationConstraintService.recordOperation(testManagerId2, 
                IOperationConstraintService.OperationType.EDIT, List.of(testProduct.getProductId()));
        }
        
        // Perform price update operations
        for (int i = 0; i < priceUpdateOperations; i++) {
            operationConstraintService.validateSingleOperation(testManagerId2, 
                IOperationConstraintService.OperationType.PRICE_UPDATE, testProduct.getProductId());
            operationConstraintService.recordOperation(testManagerId2, 
                IOperationConstraintService.OperationType.PRICE_UPDATE, List.of(testProduct.getProductId()));
        }
        
        // Perform delete operations
        for (int i = 0; i < deleteOperations; i++) {
            String productToDelete = "DELETE_TEST_" + i;
            operationConstraintService.validateSingleOperation(testManagerId2, 
                IOperationConstraintService.OperationType.DELETE, productToDelete);
            operationConstraintService.recordOperation(testManagerId2, 
                IOperationConstraintService.OperationType.DELETE, List.of(productToDelete));
        }
        
        // Verify total operations count
        IOperationConstraintService.OperationQuotaStatus quotaStatus = 
            operationConstraintService.getQuotaStatus(testManagerId2, LocalDate.now());
        
        int expectedTotalOperations = editOperations + priceUpdateOperations + deleteOperations;
        assertEquals(expectedTotalOperations, quotaStatus.getDailyOperationsUsed(),
            "All operation types should count toward daily limit");
        
        // Next operation should be rejected
        assertThrows(ValidationException.class, () -> {
            operationConstraintService.validateSingleOperation(testManagerId2, 
                IOperationConstraintService.OperationType.EDIT, testProduct.getProductId());
        }, "Additional operation should be rejected after reaching daily limit");
        
        logger.info("✓ All operation types count toward daily security limit");
    }

    // ========================================
    // Price Constraints Compliance Tests (Lines 38-40)
    // ========================================

    @Test
    @Order(5)
    @DisplayName("PS-PM-005: Price constraints 30%-150% validation - Lines 38-40")
    void testPriceConstraintValidation() throws Exception {
        logger.info("=== Testing Price Constraints 30%-150% Validation - Lines 38-40 ===");
        
        Product testProduct = testProducts.get(0);
        float productValue = 100000.0f; // Base product value for testing
        
        // Test minimum price constraint (30%)
        float minimumValidPrice = productValue * PRICE_CONSTRAINT_MIN_PERCENTAGE;
        boolean minPriceValid = priceManagementService.validatePriceRange(minimumValidPrice, productValue);
        assertTrue(minPriceValid, "Price at 30% of product value should be valid");
        
        // Test below minimum (29%)
        float belowMinPrice = productValue * 0.29f;
        boolean belowMinValid = priceManagementService.validatePriceRange(belowMinPrice, productValue);
        assertFalse(belowMinValid, "Price below 30% of product value should be invalid");
        
        // Test maximum price constraint (150%)
        float maximumValidPrice = productValue * PRICE_CONSTRAINT_MAX_PERCENTAGE;
        boolean maxPriceValid = priceManagementService.validatePriceRange(maximumValidPrice, productValue);
        assertTrue(maxPriceValid, "Price at 150% of product value should be valid");
        
        // Test above maximum (151%)
        float aboveMaxPrice = productValue * 1.51f;
        boolean aboveMaxValid = priceManagementService.validatePriceRange(aboveMaxPrice, productValue);
        assertFalse(aboveMaxValid, "Price above 150% of product value should be invalid");
        
        // Test valid range boundaries
        IPriceManagementService.PriceRange validRange = 
            priceManagementService.calculateValidPriceRange(productValue);
        
        assertEquals(productValue * PRICE_CONSTRAINT_MIN_PERCENTAGE, validRange.getMinimumPrice(), 0.01f,
            "Minimum price should be 30% of product value");
        assertEquals(productValue * PRICE_CONSTRAINT_MAX_PERCENTAGE, validRange.getMaximumPrice(), 0.01f,
            "Maximum price should be 150% of product value");
        
        // Test price within valid range
        float midRangePrice = productValue * 0.90f; // 90% of product value
        assertTrue(validRange.isWithinRange(midRangePrice), 
            "Price at 90% of product value should be within valid range");
        
        logger.info("✓ Price constraint validation (30%-150%) compliance verified");
    }

    @Test
    @Order(6)
    @DisplayName("PS-PM-006: Price constraint validation with real product updates - Lines 38-40")
    void testPriceConstraintValidationWithRealUpdates() throws Exception {
        logger.info("=== Testing Price Constraint Validation With Real Updates - Lines 38-40 ===");
        
        Product testProduct = testProducts.get(2);
        String productId = testProduct.getProductId();
        float productValue = testProduct.getPrice();
        
        // Test valid price update within constraints
        float validNewPrice = productValue * 1.25f; // 125% - within 30%-150% range
        IPriceManagementService.PriceValidationResult validResult = 
            priceManagementService.validatePriceUpdate(productId, validNewPrice, productValue, testManagerId);
        
        assertTrue(validResult.isValid(), "Price update within 30%-150% range should be valid");
        assertNotNull(validResult.getValidRange(), "Valid result should include price range information");
        
        // Test invalid price update - too low
        float tooLowPrice = productValue * 0.25f; // 25% - below minimum
        IPriceManagementService.PriceValidationResult lowResult = 
            priceManagementService.validatePriceUpdate(productId, tooLowPrice, productValue, testManagerId);
        
        assertFalse(lowResult.isValid(), "Price update below 30% should be invalid");
        assertTrue(lowResult.getMessage().toLowerCase().contains("minimum") || 
                  lowResult.getMessage().toLowerCase().contains("30%"),
            "Error message should mention minimum price constraint");
        
        // Test invalid price update - too high
        float tooHighPrice = productValue * 1.75f; // 175% - above maximum
        IPriceManagementService.PriceValidationResult highResult = 
            priceManagementService.validatePriceUpdate(productId, tooHighPrice, productValue, testManagerId);
        
        assertFalse(highResult.isValid(), "Price update above 150% should be invalid");
        assertTrue(highResult.getMessage().toLowerCase().contains("maximum") || 
                  highResult.getMessage().toLowerCase().contains("150%"),
            "Error message should mention maximum price constraint");
        
        // Test edge cases
        float exactMinPrice = productValue * PRICE_CONSTRAINT_MIN_PERCENTAGE;
        IPriceManagementService.PriceValidationResult minEdgeResult = 
            priceManagementService.validatePriceUpdate(productId, exactMinPrice, productValue, testManagerId);
        assertTrue(minEdgeResult.isValid(), "Price at exactly 30% should be valid");
        
        float exactMaxPrice = productValue * PRICE_CONSTRAINT_MAX_PERCENTAGE;
        IPriceManagementService.PriceValidationResult maxEdgeResult = 
            priceManagementService.validatePriceUpdate(productId, exactMaxPrice, productValue, testManagerId);
        assertTrue(maxEdgeResult.isValid(), "Price at exactly 150% should be valid");
        
        logger.info("✓ Price constraint validation with real product updates verified");
    }

    // ========================================
    // Concurrent Operations and Edge Cases
    // ========================================

    @Test
    @Order(7)
    @DisplayName("PS-PM-007: Concurrent edit session limits - Manager constraint")
    void testConcurrentEditSessionLimits() throws Exception {
        logger.info("=== Testing Concurrent Edit Session Limits ===");
        
        Product product1 = testProducts.get(0);
        Product product2 = testProducts.get(1);
        
        // Start first edit session
        operationConstraintService.startEditSession(testManagerId, product1.getProductId());
        
        // Verify active edit session
        assertTrue(operationConstraintService.hasActiveEditSession(testManagerId),
            "Should have active edit session after starting one");
        
        // Try to start second concurrent edit session (should fail)
        assertThrows(ValidationException.class, () -> {
            operationConstraintService.startEditSession(testManagerId, product2.getProductId());
        }, "Should not allow concurrent edit sessions for same manager");
        
        // Verify still only one active session
        IOperationConstraintService.OperationQuotaStatus quotaStatus = 
            operationConstraintService.getQuotaStatus(testManagerId, LocalDate.now());
        assertTrue(quotaStatus.hasActiveEditSession(), "Should still have active edit session");
        assertEquals(product1.getProductId(), quotaStatus.getActiveEditProductId(),
            "Active edit session should be for the first product");
        
        // End first edit session
        operationConstraintService.endEditSession(testManagerId, product1.getProductId());
        
        // Verify no active edit session
        assertFalse(operationConstraintService.hasActiveEditSession(testManagerId),
            "Should not have active edit session after ending it");
        
        // Now should be able to start edit session for second product
        assertDoesNotThrow(() -> {
            operationConstraintService.startEditSession(testManagerId, product2.getProductId());
        }, "Should allow new edit session after ending previous one");
        
        // Clean up
        operationConstraintService.endEditSession(testManagerId, product2.getProductId());
        
        logger.info("✓ Concurrent edit session limits compliance verified");
    }

    @Test
    @Order(8)
    @DisplayName("PS-PM-008: Bulk operations respect daily limits - Lines 18-19")
    void testBulkOperationsRespectDailyLimits() throws Exception {
        logger.info("=== Testing Bulk Operations Respect Daily Limits - Lines 18-19 ===");
        
        String bulkTestManagerId = "bulk-test-" + System.currentTimeMillis();
        
        // Create test products for bulk operations
        List<String> productIds = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Product product = createTestProduct("BULK_TEST_" + i, "Bulk Test Product " + i, 60000.0f);
            productIds.add(product.getProductId());
        }
        
        // Perform some individual operations first (use up 20 of 30 daily operations)
        for (int i = 0; i < 20; i++) {
            operationConstraintService.recordOperation(bulkTestManagerId, 
                IOperationConstraintService.OperationType.EDIT, List.of(productIds.get(i % 5)));
        }
        
        // Verify 10 operations remaining
        IOperationConstraintService.OperationQuotaStatus quotaStatus = 
            operationConstraintService.getQuotaStatus(bulkTestManagerId, LocalDate.now());
        assertEquals(10, quotaStatus.getDailyOperationsRemaining(),
            "Should have 10 operations remaining");
        
        // Try bulk operation with 5 products (should succeed - within remaining limit)
        List<String> smallBulkList = productIds.subList(0, 5);
        assertDoesNotThrow(() -> {
            operationConstraintService.validateBulkOperation(bulkTestManagerId, 
                IOperationConstraintService.OperationType.BULK_DELETE, smallBulkList);
        }, "Bulk operation within remaining daily limit should be allowed");
        
        // Record the bulk operation
        operationConstraintService.recordOperation(bulkTestManagerId, 
            IOperationConstraintService.OperationType.BULK_DELETE, smallBulkList);
        
        // Try bulk operation with 10 products (should fail - exceeds remaining limit)
        List<String> largeBulkList = productIds.subList(5, 15);
        assertThrows(ValidationException.class, () -> {
            operationConstraintService.validateBulkOperation(bulkTestManagerId, 
                IOperationConstraintService.OperationType.BULK_DELETE, largeBulkList);
        }, "Bulk operation exceeding remaining daily limit should be rejected");
        
        // Verify final quota status
        IOperationConstraintService.OperationQuotaStatus finalQuotaStatus = 
            operationConstraintService.getQuotaStatus(bulkTestManagerId, LocalDate.now());
        assertEquals(25, finalQuotaStatus.getDailyOperationsUsed(),
            "Should have used 25 operations (20 individual + 5 bulk)");
        assertEquals(5, finalQuotaStatus.getDailyOperationsRemaining(),
            "Should have 5 operations remaining");
        
        logger.info("✓ Bulk operations respect daily limits compliance verified");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private void setupConstraintTestProducts() {
        try {
            testProducts = new ArrayList<>();
            
            // Create test products for constraint testing
            testProducts.add(createTestProduct("CONSTRAINT_001", "Constraint Test Product 1", 80000.0f));
            testProducts.add(createTestProduct("CONSTRAINT_002", "Constraint Test Product 2", 60000.0f));
            testProducts.add(createTestProduct("CONSTRAINT_003", "Constraint Test Product 3", 100000.0f));
            testProducts.add(createTestProduct("CONSTRAINT_004", "Constraint Test Product 4", 75000.0f));
            
        } catch (Exception e) {
            logger.warning("Failed to setup constraint test products: " + e.getMessage());
            testProducts = new ArrayList<>();
        }
    }

    private Product createTestProduct(String productId, String title, float price) throws Exception {
        Product product = new Product();
        product.setProductId(productId);
        product.setTitle(title);
        product.setPrice(price);
        product.setQuantityInStock(50);
        product.setCategory("Constraint Test");
        product.setDescription("Product for constraint compliance testing");
        product.setImageUrl("constraint-test.jpg");
        product.setBarcode("123456789");
        product.setDimensions("20x15x2");
        product.setWeight(0.5f);
        
        return productService.addProduct(product);
    }

    private float calculateValidPrice(float productValue, float multiplier) {
        float proposedPrice = productValue * multiplier;
        float minPrice = productValue * PRICE_CONSTRAINT_MIN_PERCENTAGE;
        float maxPrice = productValue * PRICE_CONSTRAINT_MAX_PERCENTAGE;
        
        // Ensure price is within valid range
        return Math.max(minPrice, Math.min(maxPrice, proposedPrice));
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        clearTestData();
    }

    @AfterAll
    static void tearDownClass() {
        logger.info("=== Product Manager Constraints Compliance Tests Completed ===");
    }
}