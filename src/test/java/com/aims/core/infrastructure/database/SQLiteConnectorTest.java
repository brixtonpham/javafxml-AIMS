package com.aims.core.infrastructure.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteConnectorTest {

    private SQLiteConnector connector;
    private Connection connection;
    private static final String TEST_DATABASE_URL = "jdbc:sqlite:src/test/resources/test_aims_database.db";
    private static final String TEST_DB_FILE_PATH = "src/test/resources/test_aims_database.db";
    private static final String TEST_DB_SCRIPT_PATH = "src/main/java/com/aims/core/infrastructure/database/scripts/V1__create_tables.sql";


    @BeforeEach
    void setUp() throws SQLException, IOException {
        // Create a directory for the test database if it doesn't exist
        Path testDbDir = Paths.get("src/test/resources");
        if (!Files.exists(testDbDir)) {
            Files.createDirectories(testDbDir);
        }

        // Delete the old test database file if it exists
        File dbFile = new File(TEST_DB_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }

        // Create a new test database and initialize schema
        try {
            Process process = new ProcessBuilder("sqlite3", TEST_DB_FILE_PATH, ".read " + TEST_DB_SCRIPT_PATH)
                                .redirectErrorStream(true)
                                .start();
            process.waitFor(); // Wait for the script to finish
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to initialize test database schema", e);
        }


        // Temporarily change the DATABASE_URL in SQLiteConnector for testing
        // This is a common approach but has its downsides (modifying static final fields).
        // A better approach would be to make DATABASE_URL configurable or use dependency injection.
        // For this example, we'll stick to a simpler (though less ideal) reflection approach if needed,
        // or preferably, ensure SQLiteConnector can be instantiated with a specific URL for testing.

        // Since SQLiteConnector is a Singleton and its DATABASE_URL is final and static,
        // we will test its ability to connect to the default database path specified within it,
        // assuming that path is 'src/main/resources/aims_database.db' as per its current implementation.
        // The setup above for 'test_aims_database.db' is more for if we could redirect the connector.
        // For now, we ensure the main DB path is valid for the test.

        connector = SQLiteConnector.getInstance();
    }

    @Test
    void testGetConnection() {
        try {
            connection = connector.getConnection();
            assertNotNull(connection, "Connection should not be null.");
            assertFalse(connection.isClosed(), "Connection should be open.");
            // You could also try a simple query to ensure the connection is usable
            // For example, checking if PRAGMA foreign_keys is ON or querying a system table
            assertTrue(connection.isValid(1), "Connection should be valid.");
        } catch (SQLException e) {
            fail("SQLException occurred during getConnection: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connector.closeConnection(); // Use the Singleton's close method
        }
        // Clean up the test database file
        File dbFile = new File(TEST_DB_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }
}
