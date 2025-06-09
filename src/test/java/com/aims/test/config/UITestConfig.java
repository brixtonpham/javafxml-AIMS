package com.aims.test.config;

import com.aims.test.utils.TestDatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * UI Test Configuration
 * Configuration settings specific to UI testing
 * 
 * Features:
 * - Test database paths
 * - UI test settings  
 * - Mock configurations
 * - Test environment properties
 */
public class UITestConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(UITestConfig.class);
    
    // Configuration properties
    private static Properties testProperties;
    private static boolean initialized = false;
    
    // Default configuration values
    private static final String DEFAULT_TEST_DB_PATH = "src/test/resources/aims_test.db";
    private static final String DEFAULT_TEST_TIMEOUT = "30000"; // 30 seconds
    private static final String DEFAULT_UI_WAIT_TIMEOUT = "5000"; // 5 seconds
    private static final boolean DEFAULT_ENABLE_UI_AUTOMATION = false;
    private static final boolean DEFAULT_ENABLE_MOCK_SERVICES = true;
    private static final boolean DEFAULT_ENABLE_TEST_DATA_SEEDING = true;
    
    // Configuration keys
    public static final String TEST_DB_PATH_KEY = "test.database.path";
    public static final String TEST_TIMEOUT_KEY = "test.timeout.default";
    public static final String UI_WAIT_TIMEOUT_KEY = "test.ui.wait.timeout";
    public static final String ENABLE_UI_AUTOMATION_KEY = "test.ui.automation.enabled";
    public static final String ENABLE_MOCK_SERVICES_KEY = "test.mock.services.enabled";
    public static final String ENABLE_TEST_DATA_SEEDING_KEY = "test.data.seeding.enabled";
    public static final String TEST_SCENARIO_KEY = "test.scenario.current";
    public static final String TEST_DATA_RESET_MODE_KEY = "test.data.reset.mode";
    
    // Test data reset modes
    public enum DataResetMode {
        NEVER,      // Never reset test data
        BEFORE_EACH, // Reset before each test
        BEFORE_CLASS, // Reset before each test class
        MANUAL      // Manual reset only
    }
    
    static {
        initialize();
    }
    
    /**
     * Initialize configuration
     */
    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        logger.info("Initializing UI Test Configuration...");
        
        testProperties = new Properties();
        
        // Load default properties
        loadDefaultProperties();
        
        // Load properties from file if exists
        loadPropertiesFromFile();
        
        // Load environment-specific overrides
        loadEnvironmentOverrides();
        
        initialized = true;
        logger.info("✓ UI Test Configuration initialized");
    }
    
    /**
     * Load default configuration properties
     */
    private static void loadDefaultProperties() {
        testProperties.setProperty(TEST_DB_PATH_KEY, DEFAULT_TEST_DB_PATH);
        testProperties.setProperty(TEST_TIMEOUT_KEY, DEFAULT_TEST_TIMEOUT);
        testProperties.setProperty(UI_WAIT_TIMEOUT_KEY, DEFAULT_UI_WAIT_TIMEOUT);
        testProperties.setProperty(ENABLE_UI_AUTOMATION_KEY, String.valueOf(DEFAULT_ENABLE_UI_AUTOMATION));
        testProperties.setProperty(ENABLE_MOCK_SERVICES_KEY, String.valueOf(DEFAULT_ENABLE_MOCK_SERVICES));
        testProperties.setProperty(ENABLE_TEST_DATA_SEEDING_KEY, String.valueOf(DEFAULT_ENABLE_TEST_DATA_SEEDING));
        testProperties.setProperty(TEST_DATA_RESET_MODE_KEY, DataResetMode.BEFORE_CLASS.name());
        
        logger.debug("Default properties loaded");
    }
    
    /**
     * Load properties from ui-test.properties file if it exists
     */
    private static void loadPropertiesFromFile() {
        try (InputStream input = UITestConfig.class.getClassLoader().getResourceAsStream("ui-test.properties")) {
            if (input != null) {
                Properties fileProperties = new Properties();
                fileProperties.load(input);
                
                // Merge file properties with defaults
                for (String key : fileProperties.stringPropertyNames()) {
                    testProperties.setProperty(key, fileProperties.getProperty(key));
                }
                
                logger.debug("Properties loaded from ui-test.properties");
            } else {
                logger.debug("No ui-test.properties file found, using defaults");
            }
        } catch (IOException e) {
            logger.warn("Failed to load ui-test.properties: {}", e.getMessage());
        }
    }
    
    /**
     * Load environment-specific overrides from system properties
     */
    private static void loadEnvironmentOverrides() {
        // Check for system property overrides
        for (String key : testProperties.stringPropertyNames()) {
            String systemValue = System.getProperty(key);
            if (systemValue != null) {
                testProperties.setProperty(key, systemValue);
                logger.debug("Override from system property: {} = {}", key, systemValue);
            }
        }
    }
    
    /**
     * Get test database path
     */
    public static String getTestDatabasePath() {
        return testProperties.getProperty(TEST_DB_PATH_KEY, DEFAULT_TEST_DB_PATH);
    }
    
    /**
     * Get test database URL
     */
    public static String getTestDatabaseUrl() {
        return "jdbc:sqlite:" + getTestDatabasePath();
    }
    
    /**
     * Get default test timeout in milliseconds
     */
    public static int getTestTimeout() {
        return Integer.parseInt(testProperties.getProperty(TEST_TIMEOUT_KEY, DEFAULT_TEST_TIMEOUT));
    }
    
    /**
     * Get UI wait timeout in milliseconds
     */
    public static int getUIWaitTimeout() {
        return Integer.parseInt(testProperties.getProperty(UI_WAIT_TIMEOUT_KEY, DEFAULT_UI_WAIT_TIMEOUT));
    }
    
    /**
     * Check if UI automation is enabled
     */
    public static boolean isUIAutomationEnabled() {
        return Boolean.parseBoolean(testProperties.getProperty(ENABLE_UI_AUTOMATION_KEY, String.valueOf(DEFAULT_ENABLE_UI_AUTOMATION)));
    }
    
    /**
     * Check if mock services are enabled
     */
    public static boolean isMockServicesEnabled() {
        return Boolean.parseBoolean(testProperties.getProperty(ENABLE_MOCK_SERVICES_KEY, String.valueOf(DEFAULT_ENABLE_MOCK_SERVICES)));
    }
    
    /**
     * Check if test data seeding is enabled
     */
    public static boolean isTestDataSeedingEnabled() {
        return Boolean.parseBoolean(testProperties.getProperty(ENABLE_TEST_DATA_SEEDING_KEY, String.valueOf(DEFAULT_ENABLE_TEST_DATA_SEEDING)));
    }
    
    /**
     * Get current test scenario
     */
    public static String getCurrentTestScenario() {
        return testProperties.getProperty(TEST_SCENARIO_KEY, "DEFAULT");
    }
    
    /**
     * Set current test scenario
     */
    public static void setCurrentTestScenario(String scenario) {
        testProperties.setProperty(TEST_SCENARIO_KEY, scenario);
        logger.debug("Test scenario set to: {}", scenario);
    }
    
    /**
     * Get data reset mode
     */
    public static DataResetMode getDataResetMode() {
        String mode = testProperties.getProperty(TEST_DATA_RESET_MODE_KEY, DataResetMode.BEFORE_CLASS.name());
        try {
            return DataResetMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid data reset mode: {}. Using default: {}", mode, DataResetMode.BEFORE_CLASS);
            return DataResetMode.BEFORE_CLASS;
        }
    }
    
    /**
     * Set data reset mode
     */
    public static void setDataResetMode(DataResetMode mode) {
        testProperties.setProperty(TEST_DATA_RESET_MODE_KEY, mode.name());
        logger.debug("Data reset mode set to: {}", mode);
    }
    
    /**
     * Get property value
     */
    public static String getProperty(String key) {
        return testProperties.getProperty(key);
    }
    
    /**
     * Get property value with default
     */
    public static String getProperty(String key, String defaultValue) {
        return testProperties.getProperty(key, defaultValue);
    }
    
    /**
     * Set property value
     */
    public static void setProperty(String key, String value) {
        testProperties.setProperty(key, value);
        logger.debug("Property set: {} = {}", key, value);
    }
    
    /**
     * Check if running in CI environment
     */
    public static boolean isCIEnvironment() {
        return System.getenv("CI") != null || 
               System.getenv("GITHUB_ACTIONS") != null ||
               System.getenv("JENKINS_URL") != null;
    }
    
    /**
     * Check if running in debug mode
     */
    public static boolean isDebugMode() {
        return Boolean.parseBoolean(System.getProperty("test.debug", "false"));
    }
    
    /**
     * Get test environment setup configuration
     */
    public static TestEnvironmentConfig getEnvironmentConfig() {
        return TestEnvironmentConfig.builder()
            .withDatabasePath(getTestDatabasePath())
            .withTimeout(getTestTimeout())
            .withUIWaitTimeout(getUIWaitTimeout())
            .withMockServices(isMockServicesEnabled())
            .withDataSeeding(isTestDataSeedingEnabled())
            .withUIAutomation(isUIAutomationEnabled())
            .withDataResetMode(getDataResetMode())
            .build();
    }
    
    /**
     * Setup test environment based on configuration
     */
    public static void setupTestEnvironment() {
        logger.info("Setting up UI test environment...");
        
        // Enable test database mode
        TestDatabaseConfig.enableTestMode();
        
        // Initialize test database if data seeding is enabled
        if (isTestDataSeedingEnabled()) {
            if (!TestDatabaseManager.isTestDatabaseReady()) {
                TestDatabaseManager.initializeTestDatabase();
            }
        }
        
        // Configure timeouts
        System.setProperty("test.timeout", String.valueOf(getTestTimeout()));
        System.setProperty("ui.wait.timeout", String.valueOf(getUIWaitTimeout()));
        
        // Setup mock services if enabled
        if (isMockServicesEnabled()) {
            setupMockServices();
        }
        
        logger.info("✓ UI test environment setup completed");
    }
    
    /**
     * Setup mock services
     */
    private static void setupMockServices() {
        logger.debug("Setting up mock services...");
        
        // Configure mock payment service
        System.setProperty("payment.service.mock", "true");
        
        // Configure mock email service
        System.setProperty("email.service.mock", "true");
        
        // Configure test mode for external services
        System.setProperty("vnpay.test.mode", "true");
        
        logger.debug("✓ Mock services configured");
    }
    
    /**
     * Cleanup test environment
     */
    public static void cleanupTestEnvironment() {
        logger.info("Cleaning up UI test environment...");
        
        try {
            // Cleanup test database
            TestDatabaseManager.shutdown();
            
            // Clear test properties
            System.clearProperty("test.timeout");
            System.clearProperty("ui.wait.timeout");
            System.clearProperty("payment.service.mock");
            System.clearProperty("email.service.mock");
            System.clearProperty("vnpay.test.mode");
            
            logger.info("✓ UI test environment cleanup completed");
            
        } catch (Exception e) {
            logger.warn("Warning during test environment cleanup: {}", e.getMessage());
        }
    }
    
    /**
     * Test environment configuration builder
     */
    public static class TestEnvironmentConfig {
        private String databasePath;
        private int timeout;
        private int uiWaitTimeout;
        private boolean mockServices;
        private boolean dataSeeding;
        private boolean uiAutomation;
        private DataResetMode dataResetMode;
        
        private TestEnvironmentConfig() {}
        
        public static TestEnvironmentConfig builder() {
            return new TestEnvironmentConfig();
        }
        
        public TestEnvironmentConfig withDatabasePath(String path) {
            this.databasePath = path;
            return this;
        }
        
        public TestEnvironmentConfig withTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public TestEnvironmentConfig withUIWaitTimeout(int timeout) {
            this.uiWaitTimeout = timeout;
            return this;
        }
        
        public TestEnvironmentConfig withMockServices(boolean enabled) {
            this.mockServices = enabled;
            return this;
        }
        
        public TestEnvironmentConfig withDataSeeding(boolean enabled) {
            this.dataSeeding = enabled;
            return this;
        }
        
        public TestEnvironmentConfig withUIAutomation(boolean enabled) {
            this.uiAutomation = enabled;
            return this;
        }
        
        public TestEnvironmentConfig withDataResetMode(DataResetMode mode) {
            this.dataResetMode = mode;
            return this;
        }
        
        public TestEnvironmentConfig build() {
            return this;
        }
        
        // Getters
        public String getDatabasePath() { return databasePath; }
        public int getTimeout() { return timeout; }
        public int getUiWaitTimeout() { return uiWaitTimeout; }
        public boolean isMockServices() { return mockServices; }
        public boolean isDataSeeding() { return dataSeeding; }
        public boolean isUiAutomation() { return uiAutomation; }
        public DataResetMode getDataResetMode() { return dataResetMode; }
    }
    
    /**
     * Get all configuration properties for debugging
     */
    public static Properties getAllProperties() {
        return new Properties(testProperties);
    }
    
    /**
     * Print current configuration
     */
    public static void printConfiguration() {
        logger.info("=== UI Test Configuration ===");
        logger.info("Test Database Path: {}", getTestDatabasePath());
        logger.info("Test Timeout: {} ms", getTestTimeout());
        logger.info("UI Wait Timeout: {} ms", getUIWaitTimeout());
        logger.info("UI Automation Enabled: {}", isUIAutomationEnabled());
        logger.info("Mock Services Enabled: {}", isMockServicesEnabled());
        logger.info("Test Data Seeding Enabled: {}", isTestDataSeedingEnabled());
        logger.info("Data Reset Mode: {}", getDataResetMode());
        logger.info("Current Test Scenario: {}", getCurrentTestScenario());
        logger.info("CI Environment: {}", isCIEnvironment());
        logger.info("Debug Mode: {}", isDebugMode());
        logger.info("=============================");
    }
}