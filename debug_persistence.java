import java.sql.*;

public class debug_persistence {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite:src/test/resources/aims_test.db";
        
        try {
            // Test 1: Direct connection and insert
            System.out.println("=== Test 1: Direct Connection Test ===");
            Connection conn1 = DriverManager.getConnection(dbUrl);
            conn1.setAutoCommit(false);
            
            Statement stmt1 = conn1.createStatement();
            stmt1.execute("PRAGMA foreign_keys = ON;");
            stmt1.execute("INSERT INTO USER_ACCOUNT (userID, username, password_hash, email, user_status) VALUES ('test_999', 'test_user', 'test_pass', 'test@example.com', 'ACTIVE');");
            conn1.commit();
            
            // Check if data exists in same connection
            ResultSet rs1 = stmt1.executeQuery("SELECT COUNT(*) FROM USER_ACCOUNT WHERE userID = 'test_999'");
            rs1.next();
            System.out.println("Data in same connection: " + rs1.getInt(1));
            
            stmt1.close();
            conn1.close();
            
            // Test 2: Fresh connection
            System.out.println("=== Test 2: Fresh Connection Test ===");
            Connection conn2 = DriverManager.getConnection(dbUrl);
            Statement stmt2 = conn2.createStatement();
            
            ResultSet rs2 = stmt2.executeQuery("SELECT COUNT(*) FROM USER_ACCOUNT WHERE userID = 'test_999'");
            rs2.next();
            System.out.println("Data in fresh connection: " + rs2.getInt(1));
            
            stmt2.close();
            conn2.close();
            
            // Test 3: Check total users
            System.out.println("=== Test 3: Total User Count ===");
            Connection conn3 = DriverManager.getConnection(dbUrl);
            Statement stmt3 = conn3.createStatement();
            
            ResultSet rs3 = stmt3.executeQuery("SELECT COUNT(*) FROM USER_ACCOUNT");
            rs3.next();
            System.out.println("Total users in database: " + rs3.getInt(1));
            
            stmt3.close();
            conn3.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
