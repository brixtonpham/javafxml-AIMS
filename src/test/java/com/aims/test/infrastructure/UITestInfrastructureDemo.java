package com.aims.test.infrastructure;

import com.aims.test.base.ManualUITestBase;
import com.aims.test.config.UITestConfig;
import com.aims.test.utils.TestDatabaseManager;
import com.aims.test.utils.UITestDataSeeder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

/**
 * UI Test Infrastructure Demonstration
 * 
 * This test demonstrates how to use the Phase 1 UI Testing Infrastructure:
 * - TestDatabaseManager
 * - UITestDataSeeder  
 * - UITestConfig
 * - ManualUITestBase
 * - ScreenTestHelper
 */
@DisplayName("UI Test Infrastructure Demo")
public class UITestInfrastructureDemo extends ManualUITestBase {
    
    private static final Logger logger = LoggerFactory.getLogger(UITestInfrastructureDemo.class);
    
    @Test
    @DisplayName("Demo 01: Test Database Management")
    void demoTestDatabaseManagement() {
        logTestStep("DEMO_START", "Demonstrating Test Database Management");
        
        // Verify test database is ready
        assertManual(TestDatabaseManager.isTestDatabaseReady(), 
            "Test database should be ready");
        
        // Check if we have test data
        assertManual(hasTestData("PRODUCT"), 
            "Products table should have test data");
        assertManual(hasTestData("ROLE"), 
            "Roles table should have test data");
        
        // Get product count
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM PRODUCT")) {
            
            if (rs.next()) {
                int productCount = rs.getInt("count");
                logTestStep("DATA_VERIFICATION", "Product count: " + productCount);
                assertManual(productCount >= 30, 
                    "Should have at least 30 products for comprehensive testing");
            }
        } catch (Exception e) {
            logTestStep("DATA_VERIFICATION_ERROR", "Failed to verify product count: " + e.getMessage());
            throw new RuntimeException(e);
        }
        
        logTestStep("DEMO_COMPLETE", "Test Database Management demo completed");
    }
    
    @Test
    @DisplayName("Demo 02: Test Data Seeding Scenarios")
    void demoTestDataSeeding() {
        logTestStep("DEMO_START", "Demonstrating Test Data Seeding");
        
        // Demo available scenarios
        Map<String, String> scenarios = UITestDataSeeder.getAvailableScenarios();
        logTestStep("SCENARIOS_AVAILABLE", "Available scenarios: " + scenarios.size());
        
        for (Map.Entry<String, String> scenario : scenarios.entrySet()) {
            logger.info("📋 Scenario: {} - {}", scenario.getKey(), scenario.getValue());
        }
        
        // Demo seeding specific scenarios
        logTestStep("SCENARIO_SEED", "Seeding empty cart scenario");
        seedDataForScenario("EMPTY_CART");
        
        // Verify empty cart was created
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM CART WHERE cartSessionID = 'test-empty-cart-session'")) {
            
            if (rs.next()) {
                int cartCount = rs.getInt("count");
                assertManual(cartCount > 0, "Empty cart session should be created");
            }
        } catch (Exception e) {
            logTestStep("SCENARIO_VERIFICATION_ERROR", "Failed to verify scenario: " + e.getMessage());
            throw new RuntimeException(e);
        }
        
        // Demo another scenario
        logTestStep("SCENARIO_SEED", "Seeding populated cart scenario");
        seedDataForScenario("POPULATED_CART");
        
        // Verify populated cart
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM CART_ITEM WHERE cartSessionID = 'test-populated-cart-session'")) {
            
            if (rs.next()) {
                int itemCount = rs.getInt("count");
                assertManual(itemCount > 0, "Populated cart should have items");
                logTestStep("SCENARIO_VERIFICATION", "Populated cart has " + itemCount + " items");
            }
        } catch (Exception e) {
            logTestStep("SCENARIO_VERIFICATION_ERROR", "Failed to verify populated cart: " + e.getMessage());
            throw new RuntimeException(e);
        }
        
        logTestStep("DEMO_COMPLETE", "Test Data Seeding demo completed");
    }
    
    @Test
    @DisplayName("Demo 03: UI Test Configuration")
    void demoUITestConfiguration() {
        logTestStep("DEMO_START", "Demonstrating UI Test Configuration");
        
        // Demo configuration access
        String dbPath = UITestConfig.getTestDatabasePath();
        int timeout = UITestConfig.getTestTimeout();
        boolean mockEnabled = UITestConfig.isMockServicesEnabled();
        
        logTestStep("CONFIG_VALUES", String.format(
            "DB Path: %s, Timeout: %d ms, Mock Services: %s", 
            dbPath, timeout, mockEnabled));
        
        // Demo environment configuration
        UITestConfig.TestEnvironmentConfig envConfig = UITestConfig.getEnvironmentConfig();
        assertManual(envConfig != null, "Environment configuration should be available");
        
        // Demo configuration properties
        logTestStep("CONFIG_DEMO", "Current test scenario: " + UITestConfig.getCurrentTestScenario());
        logTestStep("CONFIG_DEMO", "Data reset mode: " + UITestConfig.getDataResetMode());
        logTestStep("CONFIG_DEMO", "CI Environment: " + UITestConfig.isCIEnvironment());
        logTestStep("CONFIG_DEMO", "Debug Mode: " + UITestConfig.isDebugMode());
        
        // Demo setting configuration
        UITestConfig.setCurrentTestScenario("DEMO_SCENARIO");
        assertEqualsManual("DEMO_SCENARIO", UITestConfig.getCurrentTestScenario(), 
            "Test scenario should be updated");
        
        logTestStep("DEMO_COMPLETE", "UI Test Configuration demo completed");
    }
    
    @Test
    @DisplayName("Demo 04: Screen Test Helper")
    void demoScreenTestHelper() {
        logTestStep("DEMO_START", "Demonstrating Screen Test Helper");
        
        // Demo navigation
        screenHelper.navigateToScreen("HomeScreen");
        assertEqualsManual("HomeScreen", screenHelper.getCurrentScreen(), 
            "Should navigate to HomeScreen");
        
        // Demo element verification
        screenHelper.setElementState("search_button", true);
        assertManual(screenHelper.verifyElementExists("search_button"), 
            "Search button should exist");
        
        // Demo user actions
        screenHelper.enterText("search_field", "Java Programming");
        screenHelper.clickButton("search_button");
        
        // Demo screen state
        screenHelper.setScreenState("search_results_count", 5);
        Object resultsCount = screenHelper.getScreenState("search_results_count");
        assertEqualsManual(5, resultsCount, "Search results count should be 5");
        
        // Demo navigation history
        screenHelper.navigateToScreen("ProductDetailScreen");
        screenHelper.navigateBack();
        assertEqualsManual("HomeScreen", screenHelper.getCurrentScreen(), 
            "Should navigate back to HomeScreen");
        
        // Demo verification summary
        Map<String, Object> summary = screenHelper.getVerificationSummary();
        logTestStep("SCREEN_SUMMARY", "Verification summary: " + summary);
        
        logTestStep("DEMO_COMPLETE", "Screen Test Helper demo completed");
    }
    
    @Test
    @DisplayName("Demo 05: Manual Testing Features")
    void demoManualTestingFeatures() {
        logTestStep("DEMO_START", "Demonstrating Manual Testing Features");
        
        // Demo manual verification prompts
        promptManualVerification(
            "Navigate to AIMS application home screen",
            "Home screen should display with product list"
        );
        
        waitForManualAction("Click on the first product to view details");
        
        // Demo manual assertions
        verifyManualStep("Product details screen displayed", 
            "All product information should be visible", true);
        
        // Demo test step logging
        logTestStep("USER_INTERACTION", "User clicked 'Add to Cart' button");
        logTestStep("SYSTEM_RESPONSE", "Product added to cart successfully");
        
        // Demo screen navigation logging
        logScreenNavigation("ProductDetailScreen", "CartScreen");
        
        // Demo user action logging
        logUserAction("Click", "Proceed to Checkout button");
        
        logTestStep("DEMO_COMPLETE", "Manual Testing Features demo completed");
    }
    
    @Test
    @DisplayName("Demo 06: Complete Test Scenario")
    void demoCompleteTestScenario() {
        logTestStep("DEMO_START", "Demonstrating Complete Test Scenario");
        
        // Step 1: Setup test data
        seedDataForScenario("SEARCH_TEST_DATA");
        logTestStep("SETUP", "Search test data seeded");
        
        // Step 2: Navigate to application
        screenHelper.navigateToScreen("HomeScreen");
        verifyScreenElement("product_list", "visible");
        
        // Step 3: Perform search
        screenHelper.enterText("search_field", "Guide");
        screenHelper.clickButton("search_button");
        logUserAction("Search", "for 'Guide'");
        
        // Step 4: Verify results
        screenHelper.navigateToScreen("SearchResultsScreen");
        screenHelper.setScreenState("search_results", 3); // Simulate 3 results found
        
        verifyManualStep("Search results displayed", 
            "Products containing 'Guide' should be shown", true);
        
        // Step 5: Add item to cart
        screenHelper.clickButton("add_to_cart_button_1");
        logUserAction("Add to Cart", "first search result");
        
        // Step 6: Verify cart update
        seedDataForScenario("POPULATED_CART");
        verifyManualStep("Cart updated", 
            "Cart should show 1 item added", true);
        
        // Step 7: Navigate to cart
        screenHelper.navigateToScreen("CartScreen");
        logScreenNavigation("SearchResultsScreen", "CartScreen");
        
        // Step 8: Verify cart contents
        promptManualVerification(
            "Review cart contents",
            "Cart should display the added product with correct details"
        );
        
        logTestStep("DEMO_COMPLETE", "Complete Test Scenario demo completed");
    }
    
    @Override
    protected String getRequiredTestScenario() {
        return "DEFAULT";
    }
    
    @Override
    protected void validateTestPrerequisites() {
        super.validateTestPrerequisites();
        
        // Additional validation for demo
        assertManual(UITestConfig.isTestDataSeedingEnabled(), 
            "Test data seeding should be enabled for demo");
        assertManual(screenHelper != null, 
            "Screen helper should be initialized");
        
        logTestStep("PREREQUISITES", "All demo prerequisites validated");
    }
}