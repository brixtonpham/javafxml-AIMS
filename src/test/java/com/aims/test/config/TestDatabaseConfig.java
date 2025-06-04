package com.aims.test.config;

import com.aims.core.infrastructure.database.SQLiteConnector;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utility class để cấu hình database cho môi trường test
 * Quản lý việc chuyển đổi giữa production DB và test DB
 */
public class TestDatabaseConfig {
    
    // Đường dẫn đến các database
    private static final String PRODUCTION_DB_URL = "jdbc:sqlite:src/main/resources/aims_database.db";
    private static final String TEST_DB_URL = "jdbc:sqlite:src/test/resources/aims_test.db";
    
    // Biến theo dõi trạng thái hiện tại
    private static boolean isTestMode = false;
    
    /**
     * Chuyển sang chế độ test - sử dụng aims_test.db
     */
    public static void enableTestMode() {
        System.setProperty("TEST_DB_URL", TEST_DB_URL);
        isTestMode = true;
        
        // Reset connection để force tạo connection mới đến test DB
        SQLiteConnector connector = SQLiteConnector.getInstance();
        connector.setConnection(null);
        
        System.out.println("✓ Test mode enabled - Using: " + TEST_DB_URL);
    }
    
    /**
     * Chuyển về chế độ production - sử dụng aims_database.db
     */
    public static void disableTestMode() {
        System.clearProperty("TEST_DB_URL");
        isTestMode = false;
        
        // Reset connection để force tạo connection mới đến production DB
        SQLiteConnector connector = SQLiteConnector.getInstance();
        connector.setConnection(null);
        
        System.out.println("✓ Test mode disabled - Using: " + PRODUCTION_DB_URL);
    }
    
    /**
     * Kiểm tra xem hiện tại có đang ở chế độ test không
     */
    public static boolean isTestMode() {
        return isTestMode;
    }
    
    /**
     * Lấy connection đến database hiện tại (test hoặc production)
     */
    public static Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }
    
    /**
     * Lấy URL của database hiện tại
     */
    public static String getCurrentDatabaseUrl() {
        return isTestMode ? TEST_DB_URL : PRODUCTION_DB_URL;
    }
    
    /**
     * Khởi tạo test database với schema và seed data
     */
    public static void initializeTestDatabase() {
        if (!isTestMode) {
            throw new IllegalStateException("Phải enable test mode trước khi initialize test database");
        }
        
        try {
            // Kiểm tra connection đến test DB
            Connection conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("✓ Test database connection established successfully");
            } else {
                throw new RuntimeException("Không thể kết nối đến test database");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi khởi tạo test database: " + e.getMessage(), e);
        }
    }
    
    /**
     * Utility method để đảm bảo test environment được setup đúng cách
     */
    public static void setupTestEnvironment() {
        enableTestMode();
        initializeTestDatabase();
    }
    
    /**
     * Cleanup test environment và chuyển về production mode
     */
    public static void cleanupTestEnvironment() {
        try {
            // Đóng connection hiện tại nếu có
            SQLiteConnector connector = SQLiteConnector.getInstance();
            connector.closeConnection();
        } catch (Exception e) {
            System.err.println("Warning: Lỗi khi cleanup test environment: " + e.getMessage());
        } finally {
            disableTestMode();
        }
    }
}
