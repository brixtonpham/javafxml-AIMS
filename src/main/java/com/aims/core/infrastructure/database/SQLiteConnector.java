package com.aims.core.infrastructure.database;

import com.aims.core.infrastructure.database.utils.DatabaseSchemaValidator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.logging.Logger;
import java.util.logging.Level;

public class SQLiteConnector {

    private static final Logger logger = Logger.getLogger(SQLiteConnector.class.getName());
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:src/main/resources/aims_database.db";
    private static volatile SQLiteConnector instance;
    private Connection connection;
    private String currentDbUrl; // To track the URL of the current connection

    private SQLiteConnector() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLiteConnector Error: SQLite JDBC Driver not found.");
            throw new RuntimeException("SQLite JDBC Driver not found", e);
        }
    }

    public static SQLiteConnector getInstance() {
        if (instance == null) {
            synchronized (SQLiteConnector.class) {
                if (instance == null) {
                    instance = new SQLiteConnector();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            String dbUrlToUse = System.getProperty("TEST_DB_URL", DEFAULT_DB_URL);

            // If connection is null, closed, or URL has changed, create a new one.
            if (this.connection == null || this.connection.isClosed() || !dbUrlToUse.equals(this.currentDbUrl)) {
                // System.out.println("SQLiteConnector: Establishing new connection to: " + dbUrlToUse);
                if (this.connection != null && !this.connection.isClosed()) {
                    this.connection.close(); // Close existing connection if URL is changing
                }
                this.connection = DriverManager.getConnection(dbUrlToUse);
                this.currentDbUrl = dbUrlToUse; // Store the URL of the new connection
                
                // Enhanced foreign key constraint setup with validation
                configureForeignKeyConstraints();
                
                // Validate foreign key constraints are properly enabled
                validateForeignKeyConstraints();
                
                // Validate and repair schema after new connection
                try {
                    validateAndRepairSchema();
                } catch (SQLException e) {
                    System.err.println("SQLiteConnector: Schema validation failed, continuing with limited functionality: " + e.getMessage());
                }
            } else {
                // System.out.println("SQLiteConnector: Reusing existing connection to: " + this.currentDbUrl);
            }
        } catch (SQLException e) {
            System.err.println("SQLiteConnector Error: Failed to get or create database connection to " + System.getProperty("TEST_DB_URL", DEFAULT_DB_URL));
            printSQLException(e);
            throw new RuntimeException("Failed to get database connection", e);
        }
        return this.connection;
    }

    public void closeConnection() {
        // Only close if not using a test DB, or if explicitly told to for test DBs by nullifying TEST_DB_URL first.
        // Test classes should manage their own lifecycle by clearing TEST_DB_URL and then calling close, or by directly closing the conn they get.
        // For general purpose, this closeConnection might be called by the application on shutdown.
        String testDbUrl = System.getProperty("TEST_DB_URL");
        if (testDbUrl == null) { // Only close the default connection if no test DB is active
            try {
                if (this.connection != null && !this.connection.isClosed()) {
                    // System.out.println("SQLiteConnector: Closing default DB connection: " + this.currentDbUrl);
                    this.connection.close();
                    this.connection = null;
                    this.currentDbUrl = null;
                }
            } catch (SQLException e) {
                System.err.println("SQLiteConnector Error: Failed to close database connection.");
                printSQLException(e);
            }
        } else {
            // If TEST_DB_URL is set, assume the test environment is managing the connection lifecycle.
            // The connection might be closed via @AfterAll in the test class by getting it and closing it.
            // Or, if setConnection(null) is called, that also indicates a reset.
            // System.out.println("SQLiteConnector: TEST_DB_URL is set ('" + testDbUrl + "'). Connection closure is managed by test environment or by calling setConnection(null).");
        }
    }

    // Allows tests to directly set a connection (e.g., an in-memory one or one to a temp file)
    // Also used to reset the connection for testing purposes.
    public void setConnection(Connection conn) {
        // System.out.println("SQLiteConnector: setConnection called.");
        if (this.connection != null && conn != this.connection) {
            try {
                if (!this.connection.isClosed()) {
                    // System.out.println("SQLiteConnector: Closing previous connection before setting new one.");
                    this.connection.close();
                }
            } catch (SQLException e) {
                System.err.println("SQLiteConnector Error: Failed to close existing connection during setConnection.");
                printSQLException(e);
            }
        }
        this.connection = conn;
        if (conn == null) {
            this.currentDbUrl = null; // Reset currentDbUrl if connection is nulled
            // System.out.println("SQLiteConnector: Connection set to null.");
        } else {
            try {
                if (!conn.isClosed()) {
                    this.currentDbUrl = conn.getMetaData().getURL(); // Update currentDbUrl from the new connection
                    logger.log(Level.INFO, "New connection set. URL: " + this.currentDbUrl);
                    // Ensure foreign keys are configured for this new connection
                    configureForeignKeyConstraints();
                    validateForeignKeyConstraints();
                } else {
                     this.currentDbUrl = null; // Connection is closed
                }
            } catch (SQLException e) {
                System.err.println("SQLiteConnector Error: SQLException when trying to get URL or set PRAGMA for externally set connection.");
                printSQLException(e);
                this.currentDbUrl = "<error_getting_url>"; // Mark that URL couldn't be retrieved
            }
        }
    }

    /**
     * Validates the database schema and repairs it if necessary
     * Specifically addresses the missing LP table issue that breaks search functionality
     * @throws SQLException if schema validation or repair fails
     */
    public void validateAndRepairSchema() throws SQLException {
        if (this.connection == null) {
            throw new SQLException("No database connection available for schema validation");
        }
        
        try {
            if (!DatabaseSchemaValidator.validateSchema(this.connection)) {
                System.out.println("SQLiteConnector: Schema issues detected, attempting repair...");
                DatabaseSchemaValidator.repairSchema(this.connection);
            }
        } catch (SQLException e) {
            System.err.println("SQLiteConnector: Schema validation/repair failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Enhanced foreign key constraint configuration with comprehensive validation
     */
    private void configureForeignKeyConstraints() throws SQLException {
        logger.log(Level.INFO, "Configuring foreign key constraints for connection: " + this.currentDbUrl);
        
        try (Statement stmt = this.connection.createStatement()) {
            // Enable foreign key constraints
            stmt.execute("PRAGMA foreign_keys = ON;");
            
            // Set additional pragmas for better constraint handling
            stmt.execute("PRAGMA defer_foreign_keys = OFF;"); // Immediate constraint checking
            stmt.execute("PRAGMA recursive_triggers = ON;"); // Enable recursive triggers for cascade operations
            
            logger.log(Level.INFO, "Foreign key constraints configured successfully");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to configure foreign key constraints", e);
            throw new SQLException("Critical error: Unable to enable foreign key constraints. " +
                                 "This may cause data integrity issues.", e);
        }
    }

    /**
     * Validates that foreign key constraints are properly enabled
     */
    private void validateForeignKeyConstraints() throws SQLException {
        logger.log(Level.FINE, "Validating foreign key constraint status");
        
        try (Statement stmt = this.connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
            
            if (rs.next()) {
                boolean foreignKeysEnabled = rs.getInt(1) == 1;
                if (!foreignKeysEnabled) {
                    String errorMsg = "CRITICAL: Foreign key constraints are not enabled despite configuration attempt";
                    logger.log(Level.SEVERE, errorMsg);
                    throw new SQLException(errorMsg + ". Database operations may fail with constraint violations.");
                }
                logger.log(Level.FINE, "Foreign key constraints validation passed");
            } else {
                logger.log(Level.WARNING, "Unable to verify foreign key constraint status");
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to validate foreign key constraints", e);
            throw new SQLException("Unable to verify foreign key constraint configuration", e);
        }
    }

    /**
     * Performs connection health check including foreign key constraint verification
     */
    public boolean validateConnectionHealth() {
        if (this.connection == null) {
            logger.log(Level.WARNING, "Connection health check failed: no connection available");
            return false;
        }
        
        try {
            // Check if connection is still valid
            if (this.connection.isClosed() || !this.connection.isValid(5)) {
                logger.log(Level.WARNING, "Connection health check failed: connection is closed or invalid");
                return false;
            }
            
            // Verify foreign key constraints are still enabled
            try (Statement stmt = this.connection.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
                
                if (rs.next()) {
                    boolean foreignKeysEnabled = rs.getInt(1) == 1;
                    if (!foreignKeysEnabled) {
                        logger.log(Level.SEVERE, "Connection health check failed: foreign key constraints disabled");
                        return false;
                    }
                }
            }
            
            // Test basic database operation
            try (Statement stmt = this.connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1;")) {
                
                if (!rs.next() || rs.getInt(1) != 1) {
                    logger.log(Level.WARNING, "Connection health check failed: basic query test failed");
                    return false;
                }
            }
            
            logger.log(Level.FINE, "Connection health check passed");
            return true;
            
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Connection health check failed due to SQLException", e);
            return false;
        }
    }

    /**
     * Validates connection before critical operations
     */
    public void validateConnectionBeforeCriticalOperation() throws SQLException {
        if (!validateConnectionHealth()) {
            logger.log(Level.WARNING, "Connection validation failed before critical operation, attempting to reconnect");
            
            // Attempt to reconnect
            try {
                String dbUrlToUse = System.getProperty("TEST_DB_URL", DEFAULT_DB_URL);
                if (this.connection != null && !this.connection.isClosed()) {
                    this.connection.close();
                }
                this.connection = DriverManager.getConnection(dbUrlToUse);
                this.currentDbUrl = dbUrlToUse;
                configureForeignKeyConstraints();
                validateForeignKeyConstraints();
                
                logger.log(Level.INFO, "Connection successfully reestablished before critical operation");
                
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to reestablish connection before critical operation", e);
                throw new SQLException("Unable to establish a valid database connection for critical operation", e);
            }
        }
    }

    public static void printSQLException(SQLException ex) {
        for (Throwable e : ex) {
            if (e instanceof SQLException) {
                System.err.println("SQLState: " + ((SQLException) e).getSQLState());
                System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
                System.err.println("Message: " + e.getMessage());
                Throwable t = ex.getCause();
                while (t != null) {
                    System.err.println("Cause: " + t);
                    t = t.getCause();
                }
            }
        }
    }
}