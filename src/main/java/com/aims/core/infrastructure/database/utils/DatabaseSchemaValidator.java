package com.aims.core.infrastructure.database.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for validating and repairing the database schema.
 * Specifically addresses the missing LP table issue that breaks search functionality.
 */
public class DatabaseSchemaValidator {
    
    private static final String[] REQUIRED_TABLES = {
        "PRODUCT", "BOOK", "CD", "DVD", "LP", "USER_ACCOUNT", "ROLE",
        "USER_ROLE_ASSIGNMENT", "CART", "CART_ITEM", "ORDER_ENTITY",
        "ORDER_ITEM", "DELIVERY_INFO", "INVOICE", "PAYMENT_METHOD",
        "CARD_DETAILS", "PAYMENT_TRANSACTION", "PRODUCT_MANAGER_AUDIT_LOG"
    };
    
    /**
     * Validates that all required tables exist in the database
     * @param conn Database connection
     * @return true if all tables exist, false otherwise
     * @throws SQLException if database error occurs
     */
    public static boolean validateSchema(Connection conn) throws SQLException {
        List<String> missingTables = findMissingTables(conn);
        
        if (missingTables.isEmpty()) {
            System.out.println("✓ All required tables exist");
            return true;
        }
        
        System.out.println("❌ Missing tables: " + missingTables);
        return false;
    }
    
    /**
     * Finds all missing tables from the required table list
     * @param conn Database connection
     * @return List of missing table names
     * @throws SQLException if database error occurs
     */
    public static List<String> findMissingTables(Connection conn) throws SQLException {
        List<String> missingTables = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();
        
        for (String tableName : REQUIRED_TABLES) {
            if (!tableExists(metaData, tableName)) {
                missingTables.add(tableName);
            }
        }
        
        return missingTables;
    }
    
    /**
     * Checks if a specific table exists in the database
     * @param metaData Database metadata
     * @param tableName Name of the table to check
     * @return true if table exists, false otherwise
     * @throws SQLException if database error occurs
     */
    private static boolean tableExists(DatabaseMetaData metaData, String tableName) 
            throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
    
    /**
     * Creates the missing LP table that's causing search functionality to fail
     * @param conn Database connection
     * @throws SQLException if table creation fails
     */
    public static void createMissingLPTable(Connection conn) throws SQLException {
        String createLPTable = """
            CREATE TABLE IF NOT EXISTS LP (
                productID TEXT PRIMARY KEY,
                artists TEXT,
                recordLabel TEXT,
                tracklist TEXT,
                genre TEXT,
                releaseDate TEXT,
                FOREIGN KEY (productID) REFERENCES PRODUCT(productID) 
                    ON DELETE CASCADE ON UPDATE CASCADE
            )""";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createLPTable);
            System.out.println("✓ LP table created successfully");
            
            // Create indexes for better performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lp_artists ON LP(artists)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lp_genre ON LP(genre)");
            System.out.println("✓ LP table indexes created");
        }
    }
    
    /**
     * Creates the missing PRODUCT_MANAGER_AUDIT_LOG table
     * @param conn Database connection
     * @throws SQLException if table creation fails
     */
    public static void createMissingAuditTable(Connection conn) throws SQLException {
        String createAuditTable = """
            CREATE TABLE IF NOT EXISTS PRODUCT_MANAGER_AUDIT_LOG (
                auditLogID TEXT PRIMARY KEY,
                managerId TEXT NOT NULL,
                operationType TEXT NOT NULL,
                productId TEXT,
                operationDateTime TEXT NOT NULL,
                details TEXT,
                FOREIGN KEY (managerId) REFERENCES USER_ACCOUNT(userID) ON DELETE CASCADE ON UPDATE CASCADE,
                FOREIGN KEY (productId) REFERENCES PRODUCT(productID) ON DELETE SET NULL ON UPDATE CASCADE
            )""";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createAuditTable);
            System.out.println("✓ PRODUCT_MANAGER_AUDIT_LOG table created successfully");
            
            // Create indexes for better performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_manager_date ON PRODUCT_MANAGER_AUDIT_LOG(managerId, operationDateTime)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_product ON PRODUCT_MANAGER_AUDIT_LOG(productId)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_operation_type ON PRODUCT_MANAGER_AUDIT_LOG(operationType)");
            System.out.println("✓ PRODUCT_MANAGER_AUDIT_LOG table indexes created");
        }
    }
    
    /**
     * Attempts to repair the database schema by creating missing tables
     * @param conn Database connection
     * @throws SQLException if repair fails
     */
    public static void repairSchema(Connection conn) throws SQLException {
        System.out.println("Starting database schema repair...");
        
        List<String> missingTables = findMissingTables(conn);
        
        if (missingTables.contains("LP")) {
            createMissingLPTable(conn);
            System.out.println("✓ LP table repair completed");
        }
        
        if (missingTables.contains("PRODUCT_MANAGER_AUDIT_LOG")) {
            createMissingAuditTable(conn);
            System.out.println("✓ PRODUCT_MANAGER_AUDIT_LOG table repair completed");
        }
        
        // Add other table creation logic here if needed in the future
        
        // Verify repair was successful
        if (validateSchema(conn)) {
            System.out.println("✓ Database schema repair completed successfully");
        } else {
            List<String> stillMissing = findMissingTables(conn);
            System.err.println("❌ Database schema repair failed - still missing tables: " + stillMissing);
        }
    }
    
    /**
     * Quick check if LP table exists (used by ProductDAOImpl)
     * @param conn Database connection
     * @return true if LP table exists, false otherwise
     */
    public static boolean lpTableExists(Connection conn) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            return tableExists(metaData, "LP");
        } catch (SQLException e) {
            System.err.println("Error checking LP table existence: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Quick check if a specific table exists (helper method for DAO classes)
     * @param conn Database connection
     * @param tableName Name of the table to check
     * @return true if table exists, false otherwise
     */
    public static boolean checkTableExists(Connection conn, String tableName) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            return tableExists(metaData, tableName);
        } catch (SQLException e) {
            System.err.println("Error checking table existence for " + tableName + ": " + e.getMessage());
            return false;
        }
    }
}