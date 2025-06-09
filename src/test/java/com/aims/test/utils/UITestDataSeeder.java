package com.aims.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * UI Test Data Seeder
 * Programmatic test data setup for different test scenarios
 * 
 * Features:
 * - Scenario-specific seeding
 * - Data cleanup
 * - Test case preparation
 * - Dynamic data generation for specific test needs
 */
public class UITestDataSeeder {
    
    private static final Logger logger = LoggerFactory.getLogger(UITestDataSeeder.class);
    
    /**
     * Seed data for specific test scenario
     */
    public static void seedDataForScenario(String scenarioName) {
        try {
            logger.info("Seeding data for scenario: {}", scenarioName);
            
            Connection conn = TestDatabaseManager.getTestConnection();
            
            switch (scenarioName.toUpperCase()) {
                case "EMPTY_CART":
                    seedEmptyCartScenario(conn);
                    break;
                case "POPULATED_CART":
                    seedPopulatedCartScenario(conn);
                    break;
                case "STOCK_ISSUES":
                    seedStockIssuesScenario(conn);
                    break;
                case "RUSH_DELIVERY_ELIGIBLE":
                    seedRushDeliveryEligibleScenario(conn);
                    break;
                case "FREE_SHIPPING_THRESHOLD":
                    seedFreeShippingScenario(conn);
                    break;
                case "OUT_OF_STOCK_PRODUCTS":
                    seedOutOfStockScenario(conn);
                    break;
                case "LOW_STOCK_PRODUCTS":
                    seedLowStockScenario(conn);
                    break;
                case "SEARCH_TEST_DATA":
                    seedSearchTestData(conn);
                    break;
                case "CATEGORY_FILTER_DATA":
                    seedCategoryFilterData(conn);
                    break;
                case "PAYMENT_TEST_DATA":
                    seedPaymentTestData(conn);
                    break;
                default:
                    logger.warn("Unknown scenario: {}", scenarioName);
            }
            
            conn.close();
            logger.info("✓ Scenario data seeded successfully: {}", scenarioName);
            
        } catch (Exception e) {
            logger.error("❌ Failed to seed data for scenario: {}", scenarioName, e);
            throw new RuntimeException("Scenario seeding failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Seed empty cart scenario - no cart items
     */
    private static void seedEmptyCartScenario(Connection conn) throws SQLException {
        logger.debug("Setting up empty cart scenario");
        
        // Clear existing cart data
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM CART_ITEM");
            stmt.execute("DELETE FROM CART");
        }
        
        // Create empty cart session
        String sql = "INSERT INTO CART (cartSessionID, userID, lastUpdated) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "test-empty-cart-session");
            pstmt.setString(2, null); // Guest cart
            pstmt.setString(3, "2024-01-01 10:00:00");
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Seed populated cart scenario - cart with various items
     */
    private static void seedPopulatedCartScenario(Connection conn) throws SQLException {
        logger.debug("Setting up populated cart scenario");
        
        // Clear existing cart data
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM CART_ITEM");
            stmt.execute("DELETE FROM CART");
        }
        
        // Create cart session
        String cartSQL = "INSERT INTO CART (cartSessionID, userID, lastUpdated) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(cartSQL)) {
            pstmt.setString(1, "test-populated-cart-session");
            pstmt.setString(2, null);
            pstmt.setString(3, "2024-01-01 10:00:00");
            pstmt.executeUpdate();
        }
        
        // Add various items to cart
        String itemSQL = "INSERT INTO CART_ITEM (cartSessionID, productID, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(itemSQL)) {
            // Add some books
            pstmt.setString(1, "test-populated-cart-session");
            pstmt.setString(2, "BOOK_001");
            pstmt.setInt(3, 2);
            pstmt.addBatch();
            
            // Add a CD
            pstmt.setString(1, "test-populated-cart-session");
            pstmt.setString(2, "CD_001");
            pstmt.setInt(3, 1);
            pstmt.addBatch();
            
            // Add a DVD
            pstmt.setString(1, "test-populated-cart-session");
            pstmt.setString(2, "DVD_001");
            pstmt.setInt(3, 1);
            pstmt.addBatch();
            
            pstmt.executeBatch();
        }
    }
    
    /**
     * Seed stock issues scenario - cart items with quantities exceeding stock
     */
    private static void seedStockIssuesScenario(Connection conn) throws SQLException {
        logger.debug("Setting up stock issues scenario");
        
        // First ensure we have products with limited stock
        String updateStockSQL = "UPDATE PRODUCT SET quantityInStock = ? WHERE productID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateStockSQL)) {
            pstmt.setInt(1, 2); // Only 2 in stock
            pstmt.setString(2, "BOOK_002");
            pstmt.addBatch();
            
            pstmt.setInt(1, 1); // Only 1 in stock
            pstmt.setString(2, "CD_002");
            pstmt.addBatch();
            
            pstmt.executeBatch();
        }
        
        // Clear existing cart data
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM CART_ITEM");
            stmt.execute("DELETE FROM CART");
        }
        
        // Create cart with stock issues
        String cartSQL = "INSERT INTO CART (cartSessionID, userID, lastUpdated) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(cartSQL)) {
            pstmt.setString(1, "test-stock-issues-cart");
            pstmt.setString(2, null);
            pstmt.setString(3, "2024-01-01 10:00:00");
            pstmt.executeUpdate();
        }
        
        // Add items exceeding stock
        String itemSQL = "INSERT INTO CART_ITEM (cartSessionID, productID, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(itemSQL)) {
            // Request 5 but only 2 available
            pstmt.setString(1, "test-stock-issues-cart");
            pstmt.setString(2, "BOOK_002");
            pstmt.setInt(3, 5);
            pstmt.addBatch();
            
            // Request 3 but only 1 available
            pstmt.setString(1, "test-stock-issues-cart");
            pstmt.setString(2, "CD_002");
            pstmt.setInt(3, 3);
            pstmt.addBatch();
            
            pstmt.executeBatch();
        }
    }
    
    /**
     * Seed rush delivery eligible scenario
     */
    private static void seedRushDeliveryEligibleScenario(Connection conn) throws SQLException {
        logger.debug("Setting up rush delivery eligible scenario");
        
        // Clear existing cart data
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM CART_ITEM");
            stmt.execute("DELETE FROM CART");
        }
        
        // Create cart with rush-eligible products
        String cartSQL = "INSERT INTO CART (cartSessionID, userID, lastUpdated) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(cartSQL)) {
            pstmt.setString(1, "test-rush-eligible-cart");
            pstmt.setString(2, null);
            pstmt.setString(3, "2024-01-01 10:00:00");
            pstmt.executeUpdate();
        }
        
        // Add rush-eligible items (Books and CDs are typically rush-eligible)
        String itemSQL = "INSERT INTO CART_ITEM (cartSessionID, productID, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(itemSQL)) {
            pstmt.setString(1, "test-rush-eligible-cart");
            pstmt.setString(2, "BOOK_001");
            pstmt.setInt(3, 1);
            pstmt.addBatch();
            
            pstmt.setString(1, "test-rush-eligible-cart");
            pstmt.setString(2, "CD_001");
            pstmt.setInt(3, 1);
            pstmt.addBatch();
            
            pstmt.executeBatch();
        }
    }
    
    /**
     * Seed free shipping threshold scenario
     */
    private static void seedFreeShippingScenario(Connection conn) throws SQLException {
        logger.debug("Setting up free shipping threshold scenario");
        
        // Clear existing cart data
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM CART_ITEM");
            stmt.execute("DELETE FROM CART");
        }
        
        // Create cart that meets free shipping threshold (>100,000 VND)
        String cartSQL = "INSERT INTO CART (cartSessionID, userID, lastUpdated) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(cartSQL)) {
            pstmt.setString(1, "test-free-shipping-cart");
            pstmt.setString(2, null);
            pstmt.setString(3, "2024-01-01 10:00:00");
            pstmt.executeUpdate();
        }
        
        // Add high-value items to meet threshold
        String itemSQL = "INSERT INTO CART_ITEM (cartSessionID, productID, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(itemSQL)) {
            // Add expensive books/CDs to exceed 100k VND
            pstmt.setString(1, "test-free-shipping-cart");
            pstmt.setString(2, "BOOK_003"); // Assume this is a high-value book
            pstmt.setInt(3, 2);
            pstmt.addBatch();
            
            pstmt.setString(1, "test-free-shipping-cart");
            pstmt.setString(2, "DVD_003"); // High-value DVD
            pstmt.setInt(3, 1);
            pstmt.addBatch();
            
            pstmt.executeBatch();
        }
    }
    
    /**
     * Seed out of stock scenario
     */
    private static void seedOutOfStockScenario(Connection conn) throws SQLException {
        logger.debug("Setting up out of stock scenario");
        
        // Set specific products to out of stock
        String updateSQL = "UPDATE PRODUCT SET quantityInStock = 0 WHERE productID IN (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setString(1, "BOOK_OUT_OF_STOCK");
            pstmt.setString(2, "CD_OUT_OF_STOCK");
            pstmt.setString(3, "DVD_OUT_OF_STOCK");
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Seed low stock scenario
     */
    private static void seedLowStockScenario(Connection conn) throws SQLException {
        logger.debug("Setting up low stock scenario");
        
        // Set specific products to low stock (1-3 items)
        String updateSQL = "UPDATE PRODUCT SET quantityInStock = ? WHERE productID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setInt(1, 1);
            pstmt.setString(2, "BOOK_LOW_STOCK_1");
            pstmt.addBatch();
            
            pstmt.setInt(1, 2);
            pstmt.setString(2, "CD_LOW_STOCK_2");
            pstmt.addBatch();
            
            pstmt.setInt(1, 3);
            pstmt.setString(2, "DVD_LOW_STOCK_3");
            pstmt.addBatch();
            
            pstmt.executeBatch();
        }
    }
    
    /**
     * Seed search test data - products with specific searchable terms
     */
    private static void seedSearchTestData(Connection conn) throws SQLException {
        logger.debug("Setting up search test data");
        
        // This method can update existing products to have searchable titles
        // or insert additional test products for search scenarios
        
        String updateSQL = "UPDATE PRODUCT SET title = ? WHERE productID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            // Update some products to have searchable terms
            pstmt.setString(1, "The Complete Guide to Java Programming");
            pstmt.setString(2, "BOOK_001");
            pstmt.addBatch();
            
            pstmt.setString(1, "JavaScript: The Definitive Guide");
            pstmt.setString(2, "BOOK_002");
            pstmt.addBatch();
            
            pstmt.setString(1, "Guide to Modern Web Development");
            pstmt.setString(2, "BOOK_003");
            pstmt.addBatch();
            
            pstmt.executeBatch();
        }
    }
    
    /**
     * Seed category filter test data
     */
    private static void seedCategoryFilterData(Connection conn) throws SQLException {
        logger.debug("Setting up category filter test data");
        
        // Ensure we have products in each category with good distribution
        String updateSQL = "UPDATE PRODUCT SET category = ? WHERE productID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            // Books
            pstmt.setString(1, "BOOK");
            pstmt.setString(2, "BOOK_001");
            pstmt.addBatch();
            
            // CDs  
            pstmt.setString(1, "CD");
            pstmt.setString(2, "CD_001");
            pstmt.addBatch();
            
            // DVDs
            pstmt.setString(1, "DVD");
            pstmt.setString(2, "DVD_001");
            pstmt.addBatch();
            
            pstmt.executeBatch();
        }
    }
    
    /**
     * Seed payment test data - test users, payment methods, orders
     */
    private static void seedPaymentTestData(Connection conn) throws SQLException {
        logger.debug("Setting up payment test data");
        
        // Clear existing payment-related test data
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM PAYMENT_TRANSACTION WHERE transactionID LIKE 'TEST_%'");
            stmt.execute("DELETE FROM CARD_DETAILS WHERE paymentMethodID LIKE 'TEST_%'");
            stmt.execute("DELETE FROM PAYMENT_METHOD WHERE paymentMethodID LIKE 'TEST_%'");
        }
        
        // Add test payment method
        String paymentMethodSQL = "INSERT INTO PAYMENT_METHOD (paymentMethodID, methodType, userID, isDefault) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(paymentMethodSQL)) {
            pstmt.setString(1, "TEST_PAYMENT_METHOD_001");
            pstmt.setString(2, "CREDIT_CARD");
            pstmt.setString(3, null); // Guest payment
            pstmt.setInt(4, 1);
            pstmt.executeUpdate();
        }
        
        // Add test card details
        String cardSQL = "INSERT INTO CARD_DETAILS (paymentMethodID, cardholderName, cardNumber_masked, expiryDate_MMYY, issuingBank) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(cardSQL)) {
            pstmt.setString(1, "TEST_PAYMENT_METHOD_001");
            pstmt.setString(2, "Test Cardholder");
            pstmt.setString(3, "**** **** **** 1234");
            pstmt.setString(4, "12/25");
            pstmt.setString(5, "Test Bank");
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Reset specific scenario data
     */
    public static void resetScenarioData(String scenarioName) {
        try {
            logger.info("Resetting scenario data: {}", scenarioName);
            
            Connection conn = TestDatabaseManager.getTestConnection();
            
            // Clear scenario-specific data based on scenario type
            switch (scenarioName.toUpperCase()) {
                case "CART_SCENARIOS":
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("DELETE FROM CART_ITEM");
                        stmt.execute("DELETE FROM CART");
                    }
                    break;
                case "STOCK_SCENARIOS":
                    // Reset all product stock to default levels
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("UPDATE PRODUCT SET quantityInStock = 10 WHERE quantityInStock <= 5");
                    }
                    break;
                case "PAYMENT_SCENARIOS":
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("DELETE FROM PAYMENT_TRANSACTION WHERE transactionID LIKE 'TEST_%'");
                        stmt.execute("DELETE FROM CARD_DETAILS WHERE paymentMethodID LIKE 'TEST_%'");
                        stmt.execute("DELETE FROM PAYMENT_METHOD WHERE paymentMethodID LIKE 'TEST_%'");
                    }
                    break;
            }
            
            conn.close();
            logger.info("✓ Scenario data reset: {}", scenarioName);
            
        } catch (Exception e) {
            logger.error("❌ Failed to reset scenario data: {}", scenarioName, e);
            throw new RuntimeException("Scenario reset failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get available test scenarios
     */
    public static Map<String, String> getAvailableScenarios() {
        Map<String, String> scenarios = new HashMap<>();
        scenarios.put("EMPTY_CART", "Empty shopping cart for testing empty state");
        scenarios.put("POPULATED_CART", "Cart with various products for testing cart functionality");
        scenarios.put("STOCK_ISSUES", "Cart with items exceeding available stock");
        scenarios.put("RUSH_DELIVERY_ELIGIBLE", "Cart with rush-delivery eligible products");
        scenarios.put("FREE_SHIPPING_THRESHOLD", "Cart meeting free shipping requirements");
        scenarios.put("OUT_OF_STOCK_PRODUCTS", "Products with zero stock");
        scenarios.put("LOW_STOCK_PRODUCTS", "Products with low stock levels");
        scenarios.put("SEARCH_TEST_DATA", "Products with searchable terms");
        scenarios.put("CATEGORY_FILTER_DATA", "Products distributed across categories");
        scenarios.put("PAYMENT_TEST_DATA", "Test payment methods and transactions");
        return scenarios;
    }
}