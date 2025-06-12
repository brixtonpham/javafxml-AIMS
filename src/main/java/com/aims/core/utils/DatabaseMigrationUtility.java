package com.aims.core.utils;

import com.aims.core.infrastructure.database.SQLiteConnector;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * Utility class for running database migrations
 */
public class DatabaseMigrationUtility {

    public static void main(String[] args) {
        System.out.println("Running database migration to add gatewayResponseData column...");
        
        try {
            runMigration();
            System.out.println("Migration completed successfully!");
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void runMigration() throws SQLException, IOException {
        // Read the migration script
        Path migrationPath = Paths.get("src/main/java/com/aims/core/infrastructure/database/scripts/V2__add_gateway_response_data.sql");
        String migrationScript = Files.readString(migrationPath);

        // Execute the migration
        try (Connection conn = SQLiteConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Split the script by semicolons and execute each statement
            String[] statements = migrationScript.split(";");
            for (String statement : statements) {
                statement = statement.trim();
                if (!statement.isEmpty() && !statement.startsWith("--")) {
                    System.out.println("Executing: " + statement);
                    stmt.execute(statement);
                }
            }
        }
    }

    /**
     * Check if the gatewayResponseData column exists
     */
    public static boolean checkIfGatewayResponseDataColumnExists() {
        try (Connection conn = SQLiteConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Try to select the column - if it fails, column doesn't exist
            stmt.executeQuery("SELECT gatewayResponseData FROM PAYMENT_TRANSACTION LIMIT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Run migration only if needed
     */
    public static void runMigrationIfNeeded() {
        try {
            if (!checkIfGatewayResponseDataColumnExists()) {
                System.out.println("gatewayResponseData column not found, running migration...");
                runMigration();
                System.out.println("Migration completed successfully!");
            } else {
                System.out.println("gatewayResponseData column already exists, no migration needed.");
            }
        } catch (Exception e) {
            System.err.println("Error during migration check/execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
}