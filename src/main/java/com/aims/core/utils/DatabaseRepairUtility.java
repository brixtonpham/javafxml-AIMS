package com.aims.core.utils;

import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.core.infrastructure.database.utils.DatabaseSchemaValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Simple utility to manually repair the database schema issues,
 * specifically the missing LP table that breaks search functionality.
 */
public class DatabaseRepairUtility {
    
    public static void main(String[] args) {
        System.out.println("=== AIMS Database Repair Utility ===");
        System.out.println("Purpose: Fix missing LP table causing search functionality errors");
        System.out.println();
        
        try {
            repairDatabase();
            System.out.println("✅ Database repair completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Database repair failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public static void repairDatabase() throws SQLException {
        System.out.println("Step 1: Establishing database connection...");
        SQLiteConnector connector = SQLiteConnector.getInstance();
        Connection conn = connector.getConnection();
        System.out.println("✓ Database connection established");
        
        System.out.println("\nStep 2: Checking current schema status...");
        boolean isValid = DatabaseSchemaValidator.validateSchema(conn);
        
        if (isValid) {
            System.out.println("✓ Database schema is already valid - no repair needed");
            return;
        }
        
        System.out.println("\nStep 3: Attempting schema repair...");
        DatabaseSchemaValidator.repairSchema(conn);
        
        System.out.println("\nStep 4: Verifying repair was successful...");
        boolean isValidAfterRepair = DatabaseSchemaValidator.validateSchema(conn);
        
        if (isValidAfterRepair) {
            System.out.println("✓ Schema repair verified successfully");
        } else {
            throw new SQLException("Schema repair verification failed");
        }
        
        System.out.println("\nStep 5: Testing search functionality...");
        testSearchFunctionality(conn);
        System.out.println("✓ Search functionality test passed");
    }
    
    private static void testSearchFunctionality(Connection conn) throws SQLException {
        // Simple test to ensure the repaired schema works for search queries
        String testQuery = """
            SELECT COUNT(DISTINCT p.productID) 
            FROM PRODUCT p 
            LEFT JOIN BOOK b ON p.productID = b.productID 
            LEFT JOIN CD c ON p.productID = c.productID 
            LEFT JOIN DVD d ON p.productID = d.productID 
            LEFT JOIN LP l ON p.productID = l.productID 
            WHERE p.quantityInStock > 0
            """;
        
        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(testQuery)) {
            
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("  - Found " + count + " products in database");
                System.out.println("  - All product type joins working correctly");
            }
        }
    }
    
    /**
     * Alternative repair method that manually creates the LP table
     * if the automatic repair doesn't work
     */
    public static void manualLPTableCreation() throws SQLException {
        System.out.println("=== Manual LP Table Creation ===");
        
        SQLiteConnector connector = SQLiteConnector.getInstance();
        Connection conn = connector.getConnection();
        
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
        
        String createIndex1 = "CREATE INDEX IF NOT EXISTS idx_lp_artists ON LP(artists)";
        String createIndex2 = "CREATE INDEX IF NOT EXISTS idx_lp_genre ON LP(genre)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createLPTable);
            System.out.println("✓ LP table created");
            
            stmt.execute(createIndex1);
            stmt.execute(createIndex2);
            System.out.println("✓ LP table indexes created");
            
            // Verify table exists
            String checkQuery = "SELECT name FROM sqlite_master WHERE type='table' AND name='LP'";
            try (java.sql.ResultSet rs = stmt.executeQuery(checkQuery)) {
                if (rs.next()) {
                    System.out.println("✓ LP table creation verified");
                } else {
                    throw new SQLException("LP table was not created properly");
                }
            }
        }
    }
    
    /**
     * Quick health check method
     */
    public static boolean isDatabaseHealthy() {
        try {
            SQLiteConnector connector = SQLiteConnector.getInstance();
            Connection conn = connector.getConnection();
            
            // Check if all required tables exist
            boolean isValid = DatabaseSchemaValidator.validateSchema(conn);
            
            if (!isValid) {
                System.out.println("Database schema issues detected");
                return false;
            }
            
            // Test search query
            testSearchFunctionality(conn);
            
            System.out.println("✓ Database is healthy");
            return true;
            
        } catch (Exception e) {
            System.err.println("Database health check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Print database status information
     */
    public static void printDatabaseStatus() {
        try {
            SQLiteConnector connector = SQLiteConnector.getInstance();
            Connection conn = connector.getConnection();
            
            System.out.println("=== Database Status ===");
            
            // Check each table including audit table
            String[] tables = {"PRODUCT", "BOOK", "CD", "DVD", "LP", "PRODUCT_MANAGER_AUDIT_LOG"};
            
            for (String table : tables) {
                boolean exists = DatabaseSchemaValidator.checkTableExists(conn, table);
                System.out.println(table + " table: " + (exists ? "✓ EXISTS" : "❌ MISSING"));
            }
            
            // Check if search would work
            try {
                testSearchFunctionality(conn);
                System.out.println("Search functionality: ✓ WORKING");
            } catch (SQLException e) {
                System.out.println("Search functionality: ❌ BROKEN (" + e.getMessage() + ")");
            }
            
        } catch (Exception e) {
            System.err.println("Unable to check database status: " + e.getMessage());
        }
    }
}