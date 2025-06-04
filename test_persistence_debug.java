// Test script to debug data persistence
import com.aims.test.config.TestDatabaseConfig;
import com.aims.test.utils.TestDataManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

public class TestPersistenceDebug {
    public static void main(String[] args) {
        try {
            System.out.println("🔧 Testing data persistence...");
            
            // Enable test mode
            TestDatabaseConfig.enableTestMode();
            
            // Check current data
            checkData("Before seeding");
            
            // Force seed data
            TestDataManager.seedTestData();
            
            // Check data again
            checkData("After seeding");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void checkData(String stage) throws Exception {
        System.out.println("\n🔍 " + stage + ":");
        
        try (Connection conn = TestDatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String[] tables = {"USER_ACCOUNT", "PRODUCT", "ORDER_ENTITY"};
            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        System.out.println("  - " + table + ": " + count + " records");
                    }
                }
            }
        }
    }
}
