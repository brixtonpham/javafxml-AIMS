package com.aims.test.base;

import com.aims.test.config.UITestConfig;
import com.aims.test.utils.TestDatabaseManager;
import com.aims.test.utils.UITestDataSeeder;
import com.aims.test.utils.ScreenTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Base Test Class for Manual UI Testing
 * Base class for manual UI testing with common utilities
 * 
 * Features:
 * - Test setup/cleanup
 * - Data seeding helpers
 * - Assertion utilities
 * - Test execution logging
 * - Manual test step verification
 */
public abstract class ManualUITestBase {
    
    protected static final Logger logger = LoggerFactory.getLogger(ManualUITestBase.class);
    
    // Test execution tracking
    protected static final List<String> testExecutionLog = new ArrayList<>();
    protected String currentTestName;
    protected String currentTestScenario;
    protected LocalDateTime testStartTime;
    
    // Test helper instances
    protected static ScreenTestHelper screenHelper;
    
    // Test configuration
    protected static UITestConfig.TestEnvironmentConfig testConfig;
    
    @BeforeAll
    static void setUpTestEnvironment() {
        logger.info("=== Setting Up Manual UI Test Environment ===");
        
        try {
            // Initialize test configuration
            testConfig = UITestConfig.getEnvironmentConfig();
            UITestConfig.setupTestEnvironment();
            
            // Initialize test database
            if (UITestConfig.isTestDataSeedingEnabled()) {
                TestDatabaseManager.initializeTestDatabase();
                logger.info("✓ Test database initialized");
            }
            
            // Initialize screen helper
            screenHelper = new ScreenTestHelper();
            
            // Print configuration for manual testers
            UITestConfig.printConfiguration();
            
            logger.info("✅ Manual UI Test Environment Ready");
            
        } catch (Exception e) {
            logger.error("❌ Failed to setup test environment", e);
            throw new RuntimeException("Test environment setup failed", e);
        }
    }
    
    @BeforeEach
    void setUpTest() {
        testStartTime = LocalDateTime.now();
        currentTestName = getTestMethodName();
        
        logger.info("🧪 Starting Test: {}", currentTestName);
        logger.info("📅 Test Start Time: {}", testStartTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Reset data based on configuration
        if (shouldResetDataBeforeTest()) {
            resetTestDataForCurrentTest();
        }
        
        // Log test start
        logTestStep("TEST_START", "Test execution started: " + currentTestName);
    }
    
    @AfterEach
    void tearDownTest() {
        LocalDateTime testEndTime = LocalDateTime.now();
        
        logger.info("✅ Test Completed: {}", currentTestName);
        logger.info("📅 Test End Time: {}", testEndTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Log test completion
        logTestStep("TEST_END", "Test execution completed: " + currentTestName);
        
        // Cleanup based on configuration
        if (shouldCleanupAfterTest()) {
            cleanupAfterCurrentTest();
        }
        
        // Print test summary
        printTestSummary();
    }
    
    @AfterAll
    static void tearDownTestEnvironment() {
        logger.info("=== Cleaning Up Manual UI Test Environment ===");
        
        try {
            // Print execution log
            printExecutionLog();
            
            // Cleanup test environment
            UITestConfig.cleanupTestEnvironment();
            
            logger.info("✅ Manual UI Test Environment Cleanup Completed");
            
        } catch (Exception e) {
            logger.warn("⚠️ Warning during test environment cleanup: {}", e.getMessage());
        }
    }
    
    // ========================================
    // Test Data Management Methods
    // ========================================
    
    /**
     * Seed data for specific test scenario
     */
    protected void seedDataForScenario(String scenarioName) {
        logger.info("🌱 Seeding data for scenario: {}", scenarioName);
        this.currentTestScenario = scenarioName;
        
        try {
            UITestDataSeeder.seedDataForScenario(scenarioName);
            logTestStep("DATA_SEED", "Seeded data for scenario: " + scenarioName);
        } catch (Exception e) {
            logTestStep("DATA_SEED_ERROR", "Failed to seed data for scenario: " + scenarioName + " - " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Reset test data to clean state
     */
    protected void resetTestData() {
        logger.info("🔄 Resetting test data...");
        
        try {
            TestDatabaseManager.resetTestDatabase();
            logTestStep("DATA_RESET", "Test data reset completed");
        } catch (Exception e) {
            logTestStep("DATA_RESET_ERROR", "Failed to reset test data: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Clear specific scenario data
     */
    protected void clearScenarioData(String scenarioName) {
        logger.info("🧹 Clearing scenario data: {}", scenarioName);
        
        try {
            UITestDataSeeder.resetScenarioData(scenarioName);
            logTestStep("DATA_CLEAR", "Cleared scenario data: " + scenarioName);
        } catch (Exception e) {
            logTestStep("DATA_CLEAR_ERROR", "Failed to clear scenario data: " + scenarioName + " - " + e.getMessage());
            throw e;
        }
    }
    
    // ========================================
    // Manual Testing Helper Methods
    // ========================================
    
    /**
     * Log a manual test step with timestamp
     */
    protected void logTestStep(String stepType, String description) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
        String logEntry = String.format("[%s] %s: %s", timestamp, stepType, description);
        
        testExecutionLog.add(logEntry);
        logger.info("📝 {}", logEntry);
    }
    
    /**
     * Verify manual test step with expected result
     */
    protected void verifyManualStep(String stepDescription, String expectedResult, boolean actualResult) {
        String status = actualResult ? "✅ PASS" : "❌ FAIL";
        String logMessage = String.format("%s - %s | Expected: %s", status, stepDescription, expectedResult);
        
        logTestStep("VERIFICATION", logMessage);
        
        if (!actualResult) {
            logger.error("❌ Manual verification failed: {}", stepDescription);
        }
    }
    
    /**
     * Prompt for manual verification
     */
    protected void promptManualVerification(String instruction, String expectedResult) {
        logger.info("👀 MANUAL VERIFICATION REQUIRED:");
        logger.info("📋 Instruction: {}", instruction);
        logger.info("✅ Expected Result: {}", expectedResult);
        logger.info("⏳ Please verify and continue...");
        
        logTestStep("MANUAL_PROMPT", "Manual verification: " + instruction);
    }
    
    /**
     * Wait for manual interaction
     */
    protected void waitForManualAction(String actionDescription) {
        logger.info("⏸️ WAITING FOR MANUAL ACTION:");
        logger.info("🔧 Action Required: {}", actionDescription);
        logger.info("▶️ Press Enter to continue after completing the action...");
        
        logTestStep("MANUAL_WAIT", "Waiting for manual action: " + actionDescription);
        
        // In a real manual testing scenario, this could wait for user input
        // For automated execution, we just log the requirement
    }
    
    /**
     * Assert that a condition is true with detailed logging
     */
    protected void assertManual(boolean condition, String description) {
        if (condition) {
            logTestStep("ASSERT_PASS", "✅ " + description);
            logger.info("✅ Assertion passed: {}", description);
        } else {
            logTestStep("ASSERT_FAIL", "❌ " + description);
            logger.error("❌ Assertion failed: {}", description);
            throw new AssertionError("Manual assertion failed: " + description);
        }
    }
    
    /**
     * Assert that two values are equal with detailed logging
     */
    protected void assertEqualsManual(Object expected, Object actual, String description) {
        boolean isEqual = (expected == null && actual == null) || 
                         (expected != null && expected.equals(actual));
        
        if (isEqual) {
            logTestStep("ASSERT_EQUALS_PASS", String.format("✅ %s | Expected: %s, Actual: %s", 
                description, expected, actual));
        } else {
            logTestStep("ASSERT_EQUALS_FAIL", String.format("❌ %s | Expected: %s, Actual: %s", 
                description, expected, actual));
            throw new AssertionError(String.format("Values not equal - %s: expected <%s> but was <%s>", 
                description, expected, actual));
        }
    }
    
    // ========================================
    // Database Helper Methods
    // ========================================
    
    /**
     * Get test database connection
     */
    protected Connection getTestConnection() throws SQLException {
        return TestDatabaseManager.getTestConnection();
    }
    
    /**
     * Execute custom test SQL
     */
    protected void executeTestSQL(String sql) throws SQLException {
        TestDatabaseManager.executeTestSQL(sql);
        logTestStep("SQL_EXECUTE", "Executed custom SQL: " + sql);
    }
    
    /**
     * Check if table has test data
     */
    protected boolean hasTestData(String tableName) {
        boolean hasData = TestDatabaseManager.hasTestData(tableName);
        logTestStep("DATA_CHECK", String.format("Table %s has data: %s", tableName, hasData));
        return hasData;
    }
    
    // ========================================
    // Screen Testing Helper Methods
    // ========================================
    
    /**
     * Verify screen element exists
     */
    protected void verifyScreenElement(String elementName, String expectedState) {
        boolean exists = screenHelper.verifyElementExists(elementName);
        verifyManualStep("Screen element '" + elementName + "' exists", expectedState, exists);
    }
    
    /**
     * Log screen navigation
     */
    protected void logScreenNavigation(String fromScreen, String toScreen) {
        logTestStep("NAVIGATION", String.format("Navigated from %s to %s", fromScreen, toScreen));
    }
    
    /**
     * Log user action
     */
    protected void logUserAction(String action, String target) {
        logTestStep("USER_ACTION", String.format("Action: %s on %s", action, target));
    }
    
    // ========================================
    // Private Helper Methods
    // ========================================
    
    private String getTestMethodName() {
        // Get the test method name from the stack trace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getMethodName().startsWith("test") || 
                element.getMethodName().contains("TC")) {
                return element.getMethodName();
            }
        }
        return "UnknownTest";
    }
    
    private boolean shouldResetDataBeforeTest() {
        UITestConfig.DataResetMode mode = testConfig.getDataResetMode();
        return mode == UITestConfig.DataResetMode.BEFORE_EACH;
    }
    
    private boolean shouldCleanupAfterTest() {
        // Always cleanup unless in debug mode
        return !UITestConfig.isDebugMode();
    }
    
    private void resetTestDataForCurrentTest() {
        logger.debug("Resetting test data before test...");
        resetTestData();
    }
    
    private void cleanupAfterCurrentTest() {
        logger.debug("Cleaning up after test...");
        // Cleanup specific to the current test if needed
    }
    
    private void printTestSummary() {
        logger.info("📊 Test Summary for: {}", currentTestName);
        logger.info("⏱️ Execution Time: {} seconds", 
            java.time.Duration.between(testStartTime, LocalDateTime.now()).getSeconds());
        logger.info("📝 Test Steps Logged: {}", 
            testExecutionLog.stream().filter(log -> log.contains(currentTestName)).count());
        
        if (currentTestScenario != null) {
            logger.info("🎯 Test Scenario: {}", currentTestScenario);
        }
    }
    
    private static void printExecutionLog() {
        logger.info("📋 Test Execution Log:");
        logger.info("=".repeat(80));
        
        for (String logEntry : testExecutionLog) {
            logger.info(logEntry);
        }
        
        logger.info("=".repeat(80));
        logger.info("📊 Total Test Steps: {}", testExecutionLog.size());
    }
    
    // ========================================
    // Template Methods for Subclasses
    // ========================================
    
    /**
     * Setup specific to test class - override in subclasses
     */
    protected void setupTestClass() {
        // Override in subclasses for specific setup
    }
    
    /**
     * Setup specific to individual test - override in subclasses
     */
    protected void setupIndividualTest() {
        // Override in subclasses for specific setup
    }
    
    /**
     * Cleanup specific to test class - override in subclasses
     */
    protected void cleanupTestClass() {
        // Override in subclasses for specific cleanup
    }
    
    /**
     * Get test-specific scenario data requirements
     */
    protected String getRequiredTestScenario() {
        // Override in subclasses to specify required scenario
        return "DEFAULT";
    }
    
    /**
     * Validate test prerequisites
     */
    protected void validateTestPrerequisites() {
        // Override in subclasses to validate specific prerequisites
        assertManual(TestDatabaseManager.isTestDatabaseReady(), 
            "Test database should be ready before test execution");
    }
}