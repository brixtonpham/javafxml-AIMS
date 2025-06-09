package com.aims.test.utils;

import com.aims.test.config.TestDatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Enhanced TestDataManager - delegates to new infrastructure
 * Maintains backward compatibility with existing tests
 */
public class TestDataManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDataManager.class);
    
    public static boolean isTestDatabaseReady() {
        return TestDatabaseManager.isTestDatabaseReady();
    }
    
    public static void seedTestData() {
        logger.info("Seeding test data via TestDatabaseManager...");
        TestDatabaseManager.initializeTestDatabase();
    }
    
    public static void clearTestData() {
        logger.info("Clearing test data...");
        try (Connection conn = TestDatabaseManager.getTestConnection();
             Statement stmt = conn.createStatement()) {
            TestDatabaseManager.clearAllTestData(conn);
        } catch (Exception e) {
            logger.error("Failed to clear test data", e);
            throw new RuntimeException("Failed to clear test data: " + e.getMessage(), e);
        }
    }
    
    public static void resetTestData() {
        logger.info("Resetting test data via TestDatabaseManager...");
        TestDatabaseManager.resetTestDatabase();
    }
    
    public static void seedDataForTestCase(String testCaseName) {
        logger.info("Seeding data for test case: {}", testCaseName);
        
        // Map test case names to scenarios
        String scenario = mapTestCaseToScenario(testCaseName);
        if (scenario != null) {
            UITestDataSeeder.seedDataForScenario(scenario);
        } else {
            logger.warn("No scenario mapping found for test case: {}", testCaseName);
        }
    }
    
    /**
     * Map legacy test case names to new scenario names
     */
    private static String mapTestCaseToScenario(String testCaseName) {
        if (testCaseName == null) {
            return null;
        }
        
        String upperCaseName = testCaseName.toUpperCase();
        
        // Map common test case patterns to scenarios
        if (upperCaseName.contains("EMPTY_CART") || upperCaseName.contains("NO_ITEMS")) {
            return "EMPTY_CART";
        } else if (upperCaseName.contains("CART") && upperCaseName.contains("ITEMS")) {
            return "POPULATED_CART";
        } else if (upperCaseName.contains("STOCK") && upperCaseName.contains("ISSUE")) {
            return "STOCK_ISSUES";
        } else if (upperCaseName.contains("RUSH") || upperCaseName.contains("DELIVERY")) {
            return "RUSH_DELIVERY_ELIGIBLE";
        } else if (upperCaseName.contains("FREE") && upperCaseName.contains("SHIPPING")) {
            return "FREE_SHIPPING_THRESHOLD";
        } else if (upperCaseName.contains("OUT_OF_STOCK")) {
            return "OUT_OF_STOCK_PRODUCTS";
        } else if (upperCaseName.contains("LOW_STOCK")) {
            return "LOW_STOCK_PRODUCTS";
        } else if (upperCaseName.contains("SEARCH")) {
            return "SEARCH_TEST_DATA";
        } else if (upperCaseName.contains("CATEGORY") || upperCaseName.contains("FILTER")) {
            return "CATEGORY_FILTER_DATA";
        } else if (upperCaseName.contains("PAYMENT")) {
            return "PAYMENT_TEST_DATA";
        }
        
        return null;
    }
}
