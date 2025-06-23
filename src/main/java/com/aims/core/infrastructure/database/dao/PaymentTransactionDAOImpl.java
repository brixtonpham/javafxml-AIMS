package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.TransactionType;
import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.core.infrastructure.database.dao.IPaymentTransactionDAO;
import com.aims.core.infrastructure.database.dao.IOrderEntityDAO; // To fetch OrderEntity details
import com.aims.core.infrastructure.database.dao.IPaymentMethodDAO; // To fetch PaymentMethod details
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PaymentTransactionDAOImpl implements IPaymentTransactionDAO {

    private final IOrderEntityDAO orderEntityDAO; // For reconstructing OrderEntity
    private final IPaymentMethodDAO paymentMethodDAO; // For reconstructing PaymentMethod

    // Helper class to store raw transaction data
    private static class RawTransactionData {
        String transactionId;
        String orderId;
        String paymentMethodId;
        String transactionType;
        String externalTransactionId;
        String transactionStatus;
        String dateTimeStr;
        float amount;
        String transactionContent;
        String gatewayResponseData;
    }

    @Autowired
    public PaymentTransactionDAOImpl(IOrderEntityDAO orderEntityDAO, IPaymentMethodDAO paymentMethodDAO) {
        this.orderEntityDAO = orderEntityDAO;
        this.paymentMethodDAO = paymentMethodDAO;
    }

    private Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }

    @Override
    public void add(PaymentTransaction transaction) throws SQLException {
        // CRITICAL: Add pre-validation before database insertion
        validateTransactionIntegrity(transaction);
        
        String sql = "INSERT INTO PAYMENT_TRANSACTION (transactionID, orderID, paymentMethodID, transactionType, " +
                     "externalTransactionID, transaction_status, transactionDateTime, amount, transactionContent, gatewayResponseData) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, transaction.getTransactionId());
            pstmt.setString(2, transaction.getOrder().getOrderId());

            if (transaction.getPaymentMethod() != null) {
                pstmt.setString(3, transaction.getPaymentMethod().getPaymentMethodId());
            } else {
                pstmt.setNull(3, Types.VARCHAR); // Payment method might be null if transaction failed early
            }

            pstmt.setString(4, transaction.getTransactionType().name());
            pstmt.setString(5, transaction.getExternalTransactionId());
            pstmt.setString(6, transaction.getTransactionStatus());
            pstmt.setString(7, transaction.getTransactionDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setFloat(8, transaction.getAmount());
            pstmt.setString(9, transaction.getTransactionContent());
            pstmt.setString(10, transaction.getGatewayResponseData());
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            // Enhanced error handling for foreign key constraint violations
            if (e.getErrorCode() == 19) { // SQLite constraint error
                if (e.getMessage().contains("FOREIGN KEY constraint failed")) {
                    handleForeignKeyConstraintError(transaction, e);
                } else if (e.getMessage().contains("UNIQUE constraint failed")) {
                    throw new SQLException("Payment transaction with ID " + transaction.getTransactionId() + 
                                         " already exists. Cannot create duplicate transaction.", e);
                }
            }
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    /**
     * Validate transaction integrity before database insertion
     */
    private void validateTransactionIntegrity(PaymentTransaction transaction) throws SQLException {
        if (transaction == null) {
            throw new SQLException("Payment transaction cannot be null");
        }
        
        if (transaction.getTransactionId() == null || transaction.getTransactionId().trim().isEmpty()) {
            throw new SQLException("Payment transaction ID cannot be null or empty");
        }
        
        if (transaction.getOrder() == null || transaction.getOrder().getOrderId() == null) {
            throw new SQLException("Payment transaction must be associated with a valid order");
        }
        
        // CRITICAL: Verify order exists in database before creating payment transaction
        String orderValidationSql = "SELECT COUNT(*) FROM ORDER_ENTITY WHERE orderID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(orderValidationSql)) {
            
            pstmt.setString(1, transaction.getOrder().getOrderId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    throw new SQLException("Order " + transaction.getOrder().getOrderId() + 
                                         " does not exist in ORDER_ENTITY table. Cannot create payment transaction.");
                }
            }
        }
        
        // Validate payment method if provided
        if (transaction.getPaymentMethod() != null && 
            transaction.getPaymentMethod().getPaymentMethodId() != null &&
            !transaction.getPaymentMethod().getPaymentMethodId().startsWith("VNPAY_TEMP_")) {
            
            String pmValidationSql = "SELECT COUNT(*) FROM PAYMENT_METHOD WHERE paymentMethodID = ?";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(pmValidationSql)) {
                
                pstmt.setString(1, transaction.getPaymentMethod().getPaymentMethodId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        throw new SQLException("Payment method " + transaction.getPaymentMethod().getPaymentMethodId() + 
                                             " does not exist in PAYMENT_METHOD table.");
                    }
                }
            }
        }
    }

    /**
     * Handle foreign key constraint errors with detailed diagnostics
     */
    private void handleForeignKeyConstraintError(PaymentTransaction transaction, SQLException originalException) throws SQLException {
        StringBuilder diagnosticMessage = new StringBuilder();
        diagnosticMessage.append("Foreign Key Constraint Violation in Payment Transaction Creation:\n");
        
        String orderId = transaction.getOrder() != null ? transaction.getOrder().getOrderId() : "NULL";
        String paymentMethodId = transaction.getPaymentMethod() != null ? 
                               transaction.getPaymentMethod().getPaymentMethodId() : "NULL";
        
        diagnosticMessage.append(String.format("Transaction ID: %s\n", transaction.getTransactionId()));
        diagnosticMessage.append(String.format("Order ID: %s\n", orderId));
        diagnosticMessage.append(String.format("Payment Method ID: %s\n", paymentMethodId));
        
        // Check which foreign key failed
        try (Connection conn = getConnection()) {
            
            // Check order existence
            if (!"NULL".equals(orderId)) {
                String orderCheckSql = "SELECT COUNT(*) FROM ORDER_ENTITY WHERE orderID = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(orderCheckSql)) {
                    pstmt.setString(1, orderId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            boolean orderExists = rs.getInt(1) > 0;
                            diagnosticMessage.append(String.format("Order exists in database: %s\n", orderExists));
                            if (!orderExists) {
                                diagnosticMessage.append("ROOT CAUSE: Order does not exist in ORDER_ENTITY table\n");
                            }
                        }
                    }
                }
            }
            
            // Check payment method existence
            if (!"NULL".equals(paymentMethodId) && !paymentMethodId.startsWith("VNPAY_TEMP_")) {
                String pmCheckSql = "SELECT COUNT(*) FROM PAYMENT_METHOD WHERE paymentMethodID = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(pmCheckSql)) {
                    pstmt.setString(1, paymentMethodId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            boolean pmExists = rs.getInt(1) > 0;
                            diagnosticMessage.append(String.format("Payment method exists in database: %s\n", pmExists));
                            if (!pmExists) {
                                diagnosticMessage.append("SECONDARY ISSUE: Payment method does not exist in PAYMENT_METHOD table\n");
                            }
                        }
                    }
                }
            }
            
        } catch (SQLException diagnosisException) {
            diagnosticMessage.append("Failed to run diagnostic queries: ").append(diagnosisException.getMessage()).append("\n");
        }
        
        diagnosticMessage.append("Original Error: ").append(originalException.getMessage());
        
        throw new SQLException(diagnosticMessage.toString(), originalException);
    }

    @Override
    public PaymentTransaction getById(String transactionId) throws SQLException {
        String sql = "SELECT * FROM PAYMENT_TRANSACTION WHERE transactionID = ?";
        
        // First phase: collect raw data
        String orderId = null;
        String paymentMethodId = null;
        PaymentTransaction transaction = null;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                transaction = new PaymentTransaction();
                // Extract ALL primitive data first
                transaction.setTransactionId(rs.getString("transactionID"));
                orderId = rs.getString("orderID");
                paymentMethodId = rs.getString("paymentMethodID");
                transaction.setTransactionType(TransactionType.valueOf(rs.getString("transactionType")));
                transaction.setExternalTransactionId(rs.getString("externalTransactionID"));
                transaction.setTransactionStatus(rs.getString("transaction_status"));
                transaction.setAmount(rs.getFloat("amount"));
                transaction.setTransactionContent(rs.getString("transactionContent"));
                transaction.setGatewayResponseData(rs.getString("gatewayResponseData"));
                
                String dateTimeStr = rs.getString("transactionDateTime");
                if (dateTimeStr != null) {
                    transaction.setTransactionDateTime(LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        
        // Second phase: populate related entities (after ResultSet is closed)
        if (transaction != null) {
            if (orderId != null) {
                OrderEntity order = orderEntityDAO.getById(orderId);
                transaction.setOrder(order);
            }
            if (paymentMethodId != null) {
                PaymentMethod pm = paymentMethodDAO.getById(paymentMethodId);
                transaction.setPaymentMethod(pm);
            }
        }
        
        return transaction;
    }

    @Override
    public List<PaymentTransaction> getByOrderId(String orderId) throws SQLException {
        List<PaymentTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT_TRANSACTION WHERE orderID = ? ORDER BY transactionDateTime DESC";
        
        // First phase: collect all raw data
        List<RawTransactionData> rawDataList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                RawTransactionData rawData = new RawTransactionData();
                rawData.transactionId = rs.getString("transactionID");
                rawData.orderId = rs.getString("orderID");
                rawData.paymentMethodId = rs.getString("paymentMethodID");
                rawData.transactionType = rs.getString("transactionType");
                rawData.externalTransactionId = rs.getString("externalTransactionID");
                rawData.transactionStatus = rs.getString("transaction_status");
                rawData.dateTimeStr = rs.getString("transactionDateTime");
                rawData.amount = rs.getFloat("amount");
                rawData.transactionContent = rs.getString("transactionContent");
                rawData.gatewayResponseData = rs.getString("gatewayResponseData");
                rawDataList.add(rawData);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        
        // Second phase: create transaction objects and populate related entities
        for (RawTransactionData rawData : rawDataList) {
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setTransactionId(rawData.transactionId);
            transaction.setTransactionType(TransactionType.valueOf(rawData.transactionType));
            transaction.setExternalTransactionId(rawData.externalTransactionId);
            transaction.setTransactionStatus(rawData.transactionStatus);
            transaction.setAmount(rawData.amount);
            transaction.setTransactionContent(rawData.transactionContent);
            transaction.setGatewayResponseData(rawData.gatewayResponseData);
            
            if (rawData.dateTimeStr != null) {
                transaction.setTransactionDateTime(LocalDateTime.parse(rawData.dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
            if (rawData.orderId != null) {
                OrderEntity order = orderEntityDAO.getById(rawData.orderId);
                transaction.setOrder(order);
            }
            if (rawData.paymentMethodId != null) {
                PaymentMethod pm = paymentMethodDAO.getById(rawData.paymentMethodId);
                transaction.setPaymentMethod(pm);
            }
            
            transactions.add(transaction);
        }
        
        return transactions;
    }

    @Override
    public List<PaymentTransaction> getAll() throws SQLException {
        List<PaymentTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT_TRANSACTION ORDER BY transactionDateTime DESC";
        
        // First phase: collect all raw data
        List<RawTransactionData> rawDataList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                RawTransactionData rawData = new RawTransactionData();
                rawData.transactionId = rs.getString("transactionID");
                rawData.orderId = rs.getString("orderID");
                rawData.paymentMethodId = rs.getString("paymentMethodID");
                rawData.transactionType = rs.getString("transactionType");
                rawData.externalTransactionId = rs.getString("externalTransactionID");
                rawData.transactionStatus = rs.getString("transaction_status");
                rawData.dateTimeStr = rs.getString("transactionDateTime");
                rawData.amount = rs.getFloat("amount");
                rawData.transactionContent = rs.getString("transactionContent");
                rawData.gatewayResponseData = rs.getString("gatewayResponseData");
                rawDataList.add(rawData);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        
        // Second phase: create transaction objects and populate related entities
        for (RawTransactionData rawData : rawDataList) {
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setTransactionId(rawData.transactionId);
            transaction.setTransactionType(TransactionType.valueOf(rawData.transactionType));
            transaction.setExternalTransactionId(rawData.externalTransactionId);
            transaction.setTransactionStatus(rawData.transactionStatus);
            transaction.setAmount(rawData.amount);
            transaction.setTransactionContent(rawData.transactionContent);
            
            if (rawData.dateTimeStr != null) {
                transaction.setTransactionDateTime(LocalDateTime.parse(rawData.dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
            if (rawData.orderId != null) {
                OrderEntity order = orderEntityDAO.getById(rawData.orderId);
                transaction.setOrder(order);
            }
            if (rawData.paymentMethodId != null) {
                PaymentMethod pm = paymentMethodDAO.getById(rawData.paymentMethodId);
                transaction.setPaymentMethod(pm);
            }
            
            transactions.add(transaction);
        }
        
        return transactions;
    }

    @Override
    public List<PaymentTransaction> getByTransactionType(TransactionType transactionType) throws SQLException {
        List<PaymentTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT_TRANSACTION WHERE transactionType = ? ORDER BY transactionDateTime DESC";
        
        // First phase: collect all raw data
        List<RawTransactionData> rawDataList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionType.name());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                RawTransactionData rawData = new RawTransactionData();
                rawData.transactionId = rs.getString("transactionID");
                rawData.orderId = rs.getString("orderID");
                rawData.paymentMethodId = rs.getString("paymentMethodID");
                rawData.transactionType = rs.getString("transactionType");
                rawData.externalTransactionId = rs.getString("externalTransactionID");
                rawData.transactionStatus = rs.getString("transaction_status");
                rawData.dateTimeStr = rs.getString("transactionDateTime");
                rawData.amount = rs.getFloat("amount");
                rawData.transactionContent = rs.getString("transactionContent");
                rawDataList.add(rawData);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        
        // Second phase: create transaction objects and populate related entities
        for (RawTransactionData rawData : rawDataList) {
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setTransactionId(rawData.transactionId);
            transaction.setTransactionType(TransactionType.valueOf(rawData.transactionType));
            transaction.setExternalTransactionId(rawData.externalTransactionId);
            transaction.setTransactionStatus(rawData.transactionStatus);
            transaction.setAmount(rawData.amount);
            transaction.setTransactionContent(rawData.transactionContent);
            
            if (rawData.dateTimeStr != null) {
                transaction.setTransactionDateTime(LocalDateTime.parse(rawData.dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
            if (rawData.orderId != null) {
                OrderEntity order = orderEntityDAO.getById(rawData.orderId);
                transaction.setOrder(order);
            }
            if (rawData.paymentMethodId != null) {
                PaymentMethod pm = paymentMethodDAO.getById(rawData.paymentMethodId);
                transaction.setPaymentMethod(pm);
            }
            
            transactions.add(transaction);
        }
        
        return transactions;
    }

    @Override
    public List<PaymentTransaction> getByDateRange(LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        List<PaymentTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT_TRANSACTION WHERE transactionDateTime >= ? AND transactionDateTime <= ? " +
                     "ORDER BY transactionDateTime DESC";
        
        // First phase: collect all raw data
        List<RawTransactionData> rawDataList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setString(2, endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                RawTransactionData rawData = new RawTransactionData();
                rawData.transactionId = rs.getString("transactionID");
                rawData.orderId = rs.getString("orderID");
                rawData.paymentMethodId = rs.getString("paymentMethodID");
                rawData.transactionType = rs.getString("transactionType");
                rawData.externalTransactionId = rs.getString("externalTransactionID");
                rawData.transactionStatus = rs.getString("transaction_status");
                rawData.dateTimeStr = rs.getString("transactionDateTime");
                rawData.amount = rs.getFloat("amount");
                rawData.transactionContent = rs.getString("transactionContent");
                rawDataList.add(rawData);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        
        // Second phase: create transaction objects and populate related entities
        for (RawTransactionData rawData : rawDataList) {
            PaymentTransaction transaction = new PaymentTransaction();
            transaction.setTransactionId(rawData.transactionId);
            transaction.setTransactionType(TransactionType.valueOf(rawData.transactionType));
            transaction.setExternalTransactionId(rawData.externalTransactionId);
            transaction.setTransactionStatus(rawData.transactionStatus);
            transaction.setAmount(rawData.amount);
            transaction.setTransactionContent(rawData.transactionContent);
            
            if (rawData.dateTimeStr != null) {
                transaction.setTransactionDateTime(LocalDateTime.parse(rawData.dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
            if (rawData.orderId != null) {
                OrderEntity order = orderEntityDAO.getById(rawData.orderId);
                transaction.setOrder(order);
            }
            if (rawData.paymentMethodId != null) {
                PaymentMethod pm = paymentMethodDAO.getById(rawData.paymentMethodId);
                transaction.setPaymentMethod(pm);
            }
            
            transactions.add(transaction);
        }
        
        return transactions;
    }

    @Override
    public void updateStatus(String transactionId, String newStatus, String externalTransactionId) throws SQLException {
        // Only update status and optionally the external transaction ID
        String sql = "UPDATE PAYMENT_TRANSACTION SET transaction_status = ?, externalTransactionID = ? " +
                     "WHERE transactionID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, externalTransactionId); // Can be the same if not changed
            pstmt.setString(3, transactionId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating payment transaction status failed, no record found for ID: " + transactionId);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void updateStatusAndGatewayData(String transactionId, String newStatus, String externalTransactionId, String gatewayResponseData) throws SQLException {
        String sql = "UPDATE PAYMENT_TRANSACTION SET transaction_status = ?, externalTransactionID = ?, gatewayResponseData = ? " +
                     "WHERE transactionID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, externalTransactionId);
            pstmt.setString(3, gatewayResponseData);
            pstmt.setString(4, transactionId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating payment transaction status and gateway data failed, no record found for ID: " + transactionId);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }
}