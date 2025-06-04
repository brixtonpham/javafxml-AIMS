package com.aims.test.base;

import com.aims.test.config.TestDatabaseConfig;
import com.aims.test.utils.TestDataManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

/**
 * Base class for UI tests
 */
public abstract class BaseUITest {
    
    @BeforeAll
    static void setUpClass() {
        // Enable test mode
        TestDatabaseConfig.enableTestMode();
        
        // Initialize test database if needed
        if (!TestDataManager.isTestDatabaseReady()) {
            TestDataManager.seedTestData();
        }
    }
    
    @BeforeEach
    void setUp() {
        // Setup for each test
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup after each test
    }
    
    // Helper methods for test data
    protected void seedSpecificData(String dataType) {
        TestDataManager.seedDataForTestCase(dataType);
    }
    
    protected void seedDataForTestCase(String testCaseName) {
        TestDataManager.seedDataForTestCase(testCaseName);
    }
    
    protected void clearTestData() {
        TestDataManager.clearTestData();
    }
    
    protected void resetTestData() {
        TestDataManager.resetTestData();
    }
    
    protected void seedDataForSpecificTest(String testCaseName) {
        TestDataManager.seedDataForTestCase(testCaseName);
    }
    
    protected void cleanupAfterTest() {
        TestDataManager.resetTestData();
    }
}
