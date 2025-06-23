package com.aims.core.infrastructure.database.dao;

import com.aims.core.infrastructure.database.SQLiteConnector;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public class ProductManagerAuditDAOImpl implements IProductManagerAuditDAO {

    private Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }

    @Override
    public void logOperation(String managerId, String operationType, String productId, String details) throws SQLException {
        String sql = "INSERT INTO PRODUCT_MANAGER_AUDIT_LOG (auditLogID, managerId, operationType, productId, operationDateTime, details) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String auditLogId = generateAuditLogId();
            pstmt.setString(1, auditLogId);
            pstmt.setString(2, managerId);
            pstmt.setString(3, operationType);
            pstmt.setString(4, productId);
            pstmt.setString(5, LocalDateTime.now().toString());
            pstmt.setString(6, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public int getOperationCount(String managerId, LocalDate date) throws SQLException {
        // Count UPDATE and DELETE operations only (ADD operations are unlimited)
        String sql = "SELECT COUNT(*) FROM PRODUCT_MANAGER_AUDIT_LOG WHERE managerId = ? AND DATE(operationDateTime) = ? AND operationType IN ('UPDATE', 'DELETE')";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, managerId);
            pstmt.setString(2, date.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return 0;
    }

    @Override
    public int getPriceUpdateCount(String managerId, String productId, LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PRODUCT_MANAGER_AUDIT_LOG WHERE managerId = ? AND productId = ? AND DATE(operationDateTime) = ? AND operationType = 'PRICE_UPDATE'";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, managerId);
            pstmt.setString(2, productId);
            pstmt.setString(3, date.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return 0;
    }

    @Override
    public java.util.List<String> getManagerOperations(String managerId, LocalDate date) throws SQLException {
        String sql = "SELECT details FROM PRODUCT_MANAGER_AUDIT_LOG WHERE managerId = ? AND DATE(operationDateTime) = ?";
        java.util.List<String> operations = new java.util.ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, managerId);
            pstmt.setString(2, date.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    operations.add(rs.getString("details"));
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return operations;
    }

    @Override
    public void cleanupOldRecords(int daysToKeep) throws SQLException {
        String sql = "DELETE FROM PRODUCT_MANAGER_AUDIT_LOG WHERE DATE(operationDateTime) < DATE('now', '-' || ? || ' days')";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, daysToKeep);
            int deletedRows = pstmt.executeUpdate();
            System.out.println("Cleaned up " + deletedRows + " old audit records");
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    private String generateAuditLogId() {
        return "AUDIT_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}