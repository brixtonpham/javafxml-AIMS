package com.aims.test.utils;

import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.test.config.TestDatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Enhanced Test Database Manager for UI Testing
 * Handles test-specific database operations, manages test database lifecycle
 * 
 * Features:
 * - Create/reset test database 
 * - Connection management
 * - Schema setup
 * - Test data isolation
 */
public class TestDatabaseManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDatabaseManager.class);
    
    // Test database configuration
    private static final String TEST_DB_PATH = "src/test/resources/aims_test.db";
    private static final String TEST_DB_URL = "jdbc:sqlite:" + TEST_DB_PATH;
    
    // SQL Script paths
    private static final String SCHEMA_SCRIPT = "/com/aims/core/infrastructure/database/scripts/V1__create_tables.sql";
    private static final String INITIAL_DATA_SCRIPT = "/com/aims/core/infrastructure/database/scripts/V2__seed_initial_data.sql";
    private static final String UI_TEST_DATA_SCRIPT = "/test_data/V3__seed_ui_test_data.sql";
    
    private static boolean isInitialized = false;
    
    /**
     * Initialize test database with clean schema and base data
     */
    public static synchronized void initializeTestDatabase() {
        try {
            logger.info("Initializing test database...");
            
            // Enable test mode
            TestDatabaseConfig.enableTestMode();
            
            // Get connection to ensure database file is created
            Connection conn = getTestConnection();
            
            // Create schema
            executeScript(conn, SCHEMA_SCRIPT);
            logger.info("✓ Database schema created");
            
            // Seed initial data (roles, base configuration)
            executeScript(conn, INITIAL_DATA_SCRIPT);
            logger.info("✓ Initial data seeded");
            
            // Seed UI test data
            executeScript(conn, UI_TEST_DATA_SCRIPT);
            logger.info("✓ UI test data seeded");
            
            conn.close();
            isInitialized = true;
            
            logger.info("✅ Test database initialization completed successfully");
            
        } catch (Exception e) {
            logger.error("❌ Failed to initialize test database", e);
            throw new RuntimeException("Test database initialization failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reset test database to clean state
     */
    public static synchronized void resetTestDatabase() {
        try {
            logger.info("Resetting test database...");
            
            Connection conn = getTestConnection();
            
            // Clear all data but keep schema
            clearAllTestData(conn);
            
            // Re-seed initial data
            executeScript(conn, INITIAL_DATA_SCRIPT);
            
            // Re-seed UI test data
            executeScript(conn, UI_TEST_DATA_SCRIPT);
            
            conn.close();
            
            logger.info("✅ Test database reset completed");
            
        } catch (Exception e) {
            logger.error("❌ Failed to reset test database", e);
            throw new RuntimeException("Test database reset failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Clear all test data while preserving schema
     */
    public static void clearAllTestData(Connection conn) throws SQLException {
        logger.info("Clearing all test data...");
        
        try (Statement stmt = conn.createStatement()) {
            // Disable foreign key constraints temporarily
            stmt.execute("PRAGMA foreign_keys = OFF");
            
            // Clear data in dependency order (child tables first)
            String[] tablesToClear = {
                "PAYMENT_TRANSACTION",
                "CARD_DETAILS", 
                "PAYMENT_METHOD",
                "INVOICE",
                "DELIVERY_INFO",
                "ORDER_ITEM",
                "ORDER_ENTITY",
                "CART_ITEM",
                "CART",
                "USER_ROLE_ASSIGNMENT",
                "BOOK",
                "CD", 
                "DVD",
                "PRODUCT",
                "USER_ACCOUNT",
                "ROLE"
            };
            
            for (String table : tablesToClear) {
                stmt.execute("DELETE FROM " + table);
                logger.debug("Cleared table: {}", table);
            }
            
            // Re-enable foreign key constraints
            stmt.execute("PRAGMA foreign_keys = ON");
            
            logger.info("✓ All test data cleared");
        }
    }
    
    /**
     * Get connection to test database
     */
    public static Connection getTestConnection() throws SQLException {
        if (!TestDatabaseConfig.isTestMode()) {
            TestDatabaseConfig.enableTestMode();
        }
        return SQLiteConnector.getInstance().getConnection();
    }
    
    /**
     * Execute SQL script from resources
     */
    private static void executeScript(Connection conn, String scriptPath) throws SQLException, IOException {
        logger.debug("Executing script: {}", scriptPath);
        
        InputStream inputStream = TestDatabaseManager.class.getResourceAsStream(scriptPath);
        if (inputStream == null) {
            throw new IOException("Could not find script: " + scriptPath);
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             Statement stmt = conn.createStatement()) {
            
            StringBuilder sql = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                
                sql.append(line).append("\n");
                
                // Execute statement when we hit a semicolon
                if (line.endsWith(";")) {
                    String statement = sql.toString().trim();
                    if (!statement.isEmpty()) {
                        stmt.execute(statement);
                    }
                    sql.setLength(0); // Clear the buffer
                }
            }
            
            // Execute any remaining SQL
            String remaining = sql.toString().trim();
            if (!remaining.isEmpty()) {
                stmt.execute(remaining);
            }
        }
        
        logger.debug("✓ Script executed successfully: {}", scriptPath);
    }
    
    /**
     * Check if test database is properly initialized
     */
    public static boolean isTestDatabaseReady() {
        try {
            if (!isInitialized) {
                return false;
            }
            
            Connection conn = getTestConnection();
            Statement stmt = conn.createStatement();
            
            // Check if core tables exist and have data
            stmt.executeQuery("SELECT COUNT(*) FROM PRODUCT");
            stmt.executeQuery("SELECT COUNT(*) FROM ROLE");
            
            stmt.close();
            conn.close();
            
            return true;
            
        } catch (SQLException e) {
            logger.warn("Test database not ready: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get test database file path
     */
    public static String getTestDatabasePath() {
        return TEST_DB_PATH;
    }
    
    /**
     * Get test database URL
     */
    public static String getTestDatabaseUrl() {
        return TEST_DB_URL;
    }
    
    /**
     * Cleanup and shutdown test database
     */
    public static void shutdown() {
        try {
            if (TestDatabaseConfig.isTestMode()) {
                SQLiteConnector.getInstance().closeConnection();
                TestDatabaseConfig.disableTestMode();
                logger.info("✓ Test database shutdown completed");
            }
        } catch (Exception e) {
            logger.warn("Warning during test database shutdown: {}", e.getMessage());
        } finally {
            isInitialized = false;
        }
    }
    
    /**
     * Execute custom SQL for test setup
     */
    public static void executeTestSQL(String sql) throws SQLException {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.debug("Executed test SQL: {}", sql);
        }
    }
    
    /**
     * Check if specific table has data
     */
    public static boolean hasTestData(String tableName) {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) as count FROM " + tableName);
            return rs.next() && rs.getInt("count") > 0;
        } catch (SQLException e) {
            logger.warn("Error checking test data for table {}: {}", tableName, e.getMessage());
            return false;
        }
    }
}