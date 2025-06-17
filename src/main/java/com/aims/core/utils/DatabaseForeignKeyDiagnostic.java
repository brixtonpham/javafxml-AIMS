package com.aims.core.utils;

import com.aims.core.infrastructure.database.SQLiteConnector;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diagnostic utility for investigating SQLite foreign key constraint failures
 * Specifically designed to analyze payment transaction foreign key violations
 */
public class DatabaseForeignKeyDiagnostic {
    
    private static final String TARGET_ORDER_ID = "ORD-1566f0d2-9558-41fb-b658-eddc427029fc";
    
    public static class DiagnosticResult {
        private String checkName;
        private boolean passed;
        private String message;
        private Map<String, Object> data;
        
        public DiagnosticResult(String checkName, boolean passed, String message) {
            this.checkName = checkName;
            this.passed = passed;
            this.message = message;
            this.data = new HashMap<>();
        }
        
        // Getters and setters
        public String getCheckName() { return checkName; }
        public boolean isPassed() { return passed; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
        public void addData(String key, Object value) { this.data.put(key, value); }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s", 
                passed ? "PASS" : "FAIL", checkName, message);
        }
    }
    
    /**
     * Run comprehensive foreign key constraint diagnostics
     */
    public static List<DiagnosticResult> runFullDiagnostic() {
        List<DiagnosticResult> results = new ArrayList<>();
        
        System.out.println("=== DATABASE FOREIGN KEY DIAGNOSTIC ===");
        System.out.println("Target Order ID: " + TARGET_ORDER_ID);
        System.out.println("Diagnostic Time: " + LocalDateTime.now());
        System.out.println();
        
        // 1. Check foreign key enforcement
        results.add(checkForeignKeyEnforcement());
        
        // 2. Check target order existence
        results.add(checkTargetOrderExists());
        
        // 3. Check for orphaned payment transactions
        results.add(checkOrphanedPaymentTransactions());
        
        // 4. Check payment method foreign key integrity
        results.add(checkPaymentMethodIntegrity());
        
        // 5. Check order creation flow integrity
        results.add(checkOrderCreationIntegrity());
        
        // 6. Check recent order patterns
        results.add(checkRecentOrderPatterns());
        
        // 7. Run SQLite foreign key check
        results.add(runSQLiteForeignKeyCheck());
        
        // Print summary
        printDiagnosticSummary(results);
        
        return results;
    }
    
    /**
     * Check if foreign key constraints are enabled
     */
    private static DiagnosticResult checkForeignKeyEnforcement() {
        try (Connection conn = SQLiteConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys")) {
            
            if (rs.next()) {
                boolean enabled = rs.getInt(1) == 1;
                DiagnosticResult result = new DiagnosticResult(
                    "Foreign Key Enforcement", 
                    enabled, 
                    enabled ? "Foreign key constraints are ENABLED" : "Foreign key constraints are DISABLED"
                );
                result.addData("foreign_keys_enabled", enabled);
                return result;
            }
        } catch (SQLException e) {
            return new DiagnosticResult(
                "Foreign Key Enforcement", 
                false, 
                "Failed to check foreign key enforcement: " + e.getMessage()
            );
        }
        
        return new DiagnosticResult(
            "Foreign Key Enforcement", 
            false, 
            "Unable to determine foreign key enforcement status"
        );
    }
    
    /**
     * Check if the target order exists in ORDER_ENTITY table
     */
    private static DiagnosticResult checkTargetOrderExists() {
        String sql = "SELECT COUNT(*) as count, " +
                    "MIN(orderDate) as orderDate, " +
                    "MIN(order_status) as status " +
                    "FROM ORDER_ENTITY WHERE orderID = ?";
        
        try (Connection conn = SQLiteConnector.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, TARGET_ORDER_ID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    boolean exists = count > 0;
                    
                    DiagnosticResult result = new DiagnosticResult(
                        "Target Order Existence",
                        exists,
                        exists ? 
                            String.format("Order %s EXISTS (Status: %s, Date: %s)", 
                                TARGET_ORDER_ID, rs.getString("status"), rs.getString("orderDate")) :
                            String.format("Order %s DOES NOT EXIST in ORDER_ENTITY table", TARGET_ORDER_ID)
                    );
                    
                    result.addData("order_count", count);
                    if (exists) {
                        result.addData("order_date", rs.getString("orderDate"));
                        result.addData("order_status", rs.getString("status"));
                    }
                    
                    return result;
                }
            }
        } catch (SQLException e) {
            return new DiagnosticResult(
                "Target Order Existence", 
                false, 
                "Failed to check order existence: " + e.getMessage()
            );
        }
        
        return new DiagnosticResult(
            "Target Order Existence", 
            false, 
            "Unable to check order existence"
        );
    }
    
    /**
     * Check for orphaned payment transactions
     */
    private static DiagnosticResult checkOrphanedPaymentTransactions() {
        String sql = "SELECT COUNT(*) as orphaned_count " +
                    "FROM PAYMENT_TRANSACTION pt " +
                    "LEFT JOIN ORDER_ENTITY oe ON pt.orderID = oe.orderID " +
                    "WHERE oe.orderID IS NULL";
        
        try (Connection conn = SQLiteConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int orphanedCount = rs.getInt("orphaned_count");
                boolean hasOrphans = orphanedCount > 0;
                
                DiagnosticResult result = new DiagnosticResult(
                    "Orphaned Payment Transactions",
                    !hasOrphans,
                    hasOrphans ? 
                        String.format("Found %d orphaned payment transactions", orphanedCount) :
                        "No orphaned payment transactions found"
                );
                
                result.addData("orphaned_count", orphanedCount);
                
                // If orphans found, get details
                if (hasOrphans) {
                    String detailSql = "SELECT pt.transactionID, pt.orderID, pt.transaction_status, pt.transactionDateTime " +
                                     "FROM PAYMENT_TRANSACTION pt " +
                                     "LEFT JOIN ORDER_ENTITY oe ON pt.orderID = oe.orderID " +
                                     "WHERE oe.orderID IS NULL " +
                                     "ORDER BY pt.transactionDateTime DESC LIMIT 5";
                    
                    try (Statement detailStmt = conn.createStatement();
                         ResultSet detailRs = detailStmt.executeQuery(detailSql)) {
                        
                        List<String> orphanDetails = new ArrayList<>();
                        while (detailRs.next()) {
                            orphanDetails.add(String.format("TxnID: %s, OrderID: %s, Status: %s, Date: %s",
                                detailRs.getString("transactionID"),
                                detailRs.getString("orderID"),
                                detailRs.getString("transaction_status"),
                                detailRs.getString("transactionDateTime")
                            ));
                        }
                        result.addData("orphan_details", orphanDetails);
                    }
                }
                
                return result;
            }
        } catch (SQLException e) {
            return new DiagnosticResult(
                "Orphaned Payment Transactions", 
                false, 
                "Failed to check orphaned transactions: " + e.getMessage()
            );
        }
        
        return new DiagnosticResult(
            "Orphaned Payment Transactions", 
            false, 
            "Unable to check orphaned transactions"
        );
    }
    
    /**
     * Check payment method foreign key integrity
     */
    private static DiagnosticResult checkPaymentMethodIntegrity() {
        String sql = "SELECT COUNT(*) as invalid_count " +
                    "FROM PAYMENT_TRANSACTION pt " +
                    "LEFT JOIN PAYMENT_METHOD pm ON pt.paymentMethodID = pm.paymentMethodID " +
                    "WHERE pt.paymentMethodID IS NOT NULL AND pm.paymentMethodID IS NULL";
        
        try (Connection conn = SQLiteConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int invalidCount = rs.getInt("invalid_count");
                boolean hasInvalid = invalidCount > 0;
                
                DiagnosticResult result = new DiagnosticResult(
                    "Payment Method FK Integrity",
                    !hasInvalid,
                    hasInvalid ? 
                        String.format("Found %d payment transactions with invalid payment method IDs", invalidCount) :
                        "All payment method foreign keys are valid"
                );
                
                result.addData("invalid_payment_method_count", invalidCount);
                return result;
            }
        } catch (SQLException e) {
            return new DiagnosticResult(
                "Payment Method FK Integrity", 
                false, 
                "Failed to check payment method integrity: " + e.getMessage()
            );
        }
        
        return new DiagnosticResult(
            "Payment Method FK Integrity", 
            false, 
            "Unable to check payment method integrity"
        );
    }
    
    /**
     * Check order creation flow integrity
     */
    private static DiagnosticResult checkOrderCreationIntegrity() {
        StringBuilder message = new StringBuilder();
        Map<String, Integer> orphanCounts = new HashMap<>();
        boolean hasIssues = false;
        
        try (Connection conn = SQLiteConnector.getInstance().getConnection()) {
            
            // Check ORDER_ITEM without parent order
            String orderItemSql = "SELECT COUNT(*) as count FROM ORDER_ITEM oi " +
                                "LEFT JOIN ORDER_ENTITY oe ON oi.orderID = oe.orderID " +
                                "WHERE oe.orderID IS NULL";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(orderItemSql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    orphanCounts.put("order_items", count);
                    if (count > 0) hasIssues = true;
                }
            }
            
            // Check DELIVERY_INFO without parent order
            String deliveryInfoSql = "SELECT COUNT(*) as count FROM DELIVERY_INFO di " +
                                   "LEFT JOIN ORDER_ENTITY oe ON di.orderID = oe.orderID " +
                                   "WHERE oe.orderID IS NULL";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(deliveryInfoSql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    orphanCounts.put("delivery_info", count);
                    if (count > 0) hasIssues = true;
                }
            }
            
            // Check INVOICE without parent order
            String invoiceSql = "SELECT COUNT(*) as count FROM INVOICE i " +
                              "LEFT JOIN ORDER_ENTITY oe ON i.orderID = oe.orderID " +
                              "WHERE oe.orderID IS NULL";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(invoiceSql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    orphanCounts.put("invoices", count);
                    if (count > 0) hasIssues = true;
                }
            }
            
        } catch (SQLException e) {
            return new DiagnosticResult(
                "Order Creation Integrity", 
                false, 
                "Failed to check order creation integrity: " + e.getMessage()
            );
        }
        
        if (hasIssues) {
            message.append("Found orphaned records: ");
            orphanCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .forEach(entry -> message.append(String.format("%s(%d) ", entry.getKey(), entry.getValue())));
        } else {
            message.append("No orphaned order-related records found");
        }
        
        DiagnosticResult result = new DiagnosticResult(
            "Order Creation Integrity",
            !hasIssues,
            message.toString()
        );
        
        orphanCounts.forEach(result::addData);
        return result;
    }
    
    /**
     * Check recent order patterns
     */
    private static DiagnosticResult checkRecentOrderPatterns() {
        String sql = "SELECT COUNT(*) as recent_count, " +
                    "MIN(orderDate) as oldest_date, " +
                    "MAX(orderDate) as newest_date " +
                    "FROM ORDER_ENTITY " +
                    "WHERE orderDate >= datetime('now', '-1 day')";
        
        try (Connection conn = SQLiteConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int recentCount = rs.getInt("recent_count");
                
                DiagnosticResult result = new DiagnosticResult(
                    "Recent Order Patterns",
                    true,
                    String.format("Found %d orders in the last 24 hours", recentCount)
                );
                
                result.addData("recent_order_count", recentCount);
                result.addData("oldest_recent_date", rs.getString("oldest_date"));
                result.addData("newest_recent_date", rs.getString("newest_date"));
                
                return result;
            }
        } catch (SQLException e) {
            return new DiagnosticResult(
                "Recent Order Patterns", 
                false, 
                "Failed to check recent order patterns: " + e.getMessage()
            );
        }
        
        return new DiagnosticResult(
            "Recent Order Patterns", 
            false, 
            "Unable to check recent order patterns"
        );
    }
    
    /**
     * Run SQLite built-in foreign key check
     */
    private static DiagnosticResult runSQLiteForeignKeyCheck() {
        try (Connection conn = SQLiteConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_check")) {
            
            List<String> violations = new ArrayList<>();
            while (rs.next()) {
                violations.add(String.format("Table: %s, RowID: %s, Parent: %s, FK Index: %s",
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
            
            boolean hasViolations = !violations.isEmpty();
            
            DiagnosticResult result = new DiagnosticResult(
                "SQLite FK Check",
                !hasViolations,
                hasViolations ? 
                    String.format("Found %d foreign key violations", violations.size()) :
                    "No foreign key violations detected"
            );
            
            result.addData("violation_count", violations.size());
            result.addData("violations", violations);
            
            return result;
        } catch (SQLException e) {
            return new DiagnosticResult(
                "SQLite FK Check", 
                false, 
                "Failed to run SQLite foreign key check: " + e.getMessage()
            );
        }
    }
    
    /**
     * Print diagnostic summary
     */
    private static void printDiagnosticSummary(List<DiagnosticResult> results) {
        System.out.println("\n=== DIAGNOSTIC SUMMARY ===");
        
        long passedCount = results.stream().mapToLong(r -> r.isPassed() ? 1 : 0).sum();
        long failedCount = results.size() - passedCount;
        
        System.out.printf("Total Checks: %d | Passed: %d | Failed: %d%n", 
            results.size(), passedCount, failedCount);
        System.out.println();
        
        for (DiagnosticResult result : results) {
            System.out.println(result);
            if (!result.getData().isEmpty() && !result.isPassed()) {
                result.getData().forEach((key, value) -> {
                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        if (!list.isEmpty()) {
                            System.out.println("  " + key + ":");
                            list.forEach(item -> System.out.println("    - " + item));
                        }
                    } else {
                        System.out.println("  " + key + ": " + value);
                    }
                });
            }
            System.out.println();
        }
        
        if (failedCount > 0) {
            System.out.println("🚨 CRITICAL ISSUES DETECTED - Immediate attention required!");
        } else {
            System.out.println("✅ All diagnostic checks passed");
        }
    }
    
    /**
     * Main method for running diagnostics
     */
    public static void main(String[] args) {
        System.out.println("Running Database Foreign Key Constraint Diagnostics...\n");
        
        try {
            List<DiagnosticResult> results = runFullDiagnostic();
            
            // Check for specific target order
            boolean targetOrderExists = results.stream()
                .filter(r -> "Target Order Existence".equals(r.getCheckName()))
                .findFirst()
                .map(DiagnosticResult::isPassed)
                .orElse(false);
            
            if (!targetOrderExists) {
                System.out.println("\n🎯 ROOT CAUSE IDENTIFIED:");
                System.out.println("Order " + TARGET_ORDER_ID + " does not exist in ORDER_ENTITY table.");
                System.out.println("This explains the SQLITE_CONSTRAINT_FOREIGNKEY error in PaymentTransactionDAOImpl.add()");
                System.out.println("\nRECOMMENDED ACTIONS:");
                System.out.println("1. Investigate why order creation failed");
                System.out.println("2. Add order existence validation before payment processing");
                System.out.println("3. Implement transaction rollback for failed order creation");
            }
            
        } catch (Exception e) {
            System.err.println("Diagnostic execution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
