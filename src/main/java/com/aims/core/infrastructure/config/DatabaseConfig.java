package com.aims.core.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Database configuration management for AIMS application.
 * Handles database connection parameters and configuration loading.
 */
public class DatabaseConfig {
    
    private static DatabaseConfig instance;
    private Properties properties;
    
    // Default configuration values
    private static final String DEFAULT_DB_PATH = "src/main/resources/aims_database.db";
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:" + DEFAULT_DB_PATH;
    private static final String CONFIG_FILE = "app.properties";
    
    private DatabaseConfig() {
        loadConfiguration();
    }
    
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        properties = new Properties();
        
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
                System.out.println("DatabaseConfig: Loaded configuration from " + CONFIG_FILE);
            } else {
                System.out.println("DatabaseConfig: Configuration file not found, using defaults");
                setDefaultProperties();
            }
        } catch (IOException e) {
            System.err.println("DatabaseConfig: Error loading configuration: " + e.getMessage());
            setDefaultProperties();
        }
    }
    
    private void setDefaultProperties() {
        properties.setProperty("database.url", DEFAULT_DB_URL);
        properties.setProperty("database.path", DEFAULT_DB_PATH);
        properties.setProperty("database.driver", "org.sqlite.JDBC");
        properties.setProperty("database.connection.timeout", "30000");
        properties.setProperty("database.max.connections", "10");
    }
    
    /**
     * Gets the database URL for connections.
     * @return The database URL
     */
    public String getDatabaseUrl() {
        return properties.getProperty("database.url", DEFAULT_DB_URL);
    }
    
    /**
     * Gets the database file path.
     * @return The database file path
     */
    public String getDatabasePath() {
        return properties.getProperty("database.path", DEFAULT_DB_PATH);
    }
    
    /**
     * Gets the database driver class name.
     * @return The driver class name
     */
    public String getDatabaseDriver() {
        return properties.getProperty("database.driver", "org.sqlite.JDBC");
    }
    
    /**
     * Gets the connection timeout in milliseconds.
     * @return The connection timeout
     */
    public int getConnectionTimeout() {
        try {
            return Integer.parseInt(properties.getProperty("database.connection.timeout", "30000"));
        } catch (NumberFormatException e) {
            return 30000;
        }
    }
    
    /**
     * Gets the maximum number of connections.
     * @return The maximum connections
     */
    public int getMaxConnections() {
        try {
            return Integer.parseInt(properties.getProperty("database.max.connections", "10"));
        } catch (NumberFormatException e) {
            return 10;
        }
    }
    
    /**
     * Validates the database configuration.
     * @return true if configuration is valid, false otherwise
     */
    public boolean validateConfiguration() {
        try {
            // Check if database URL is valid
            String url = getDatabaseUrl();
            if (url == null || url.trim().isEmpty()) {
                System.err.println("DatabaseConfig: Invalid database URL");
                return false;
            }
            
            // Check if driver is available
            Class.forName(getDatabaseDriver());
            
            System.out.println("DatabaseConfig: Configuration validation successful");
            return true;
            
        } catch (ClassNotFoundException e) {
            System.err.println("DatabaseConfig: Database driver not found: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("DatabaseConfig: Configuration validation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Prints current configuration for debugging.
     */
    public void printConfiguration() {
        System.out.println("=== Database Configuration ===");
        System.out.println("URL: " + getDatabaseUrl());
        System.out.println("Path: " + getDatabasePath());
        System.out.println("Driver: " + getDatabaseDriver());
        System.out.println("Timeout: " + getConnectionTimeout() + "ms");
        System.out.println("Max Connections: " + getMaxConnections());
        System.out.println("==============================");
    }
}
