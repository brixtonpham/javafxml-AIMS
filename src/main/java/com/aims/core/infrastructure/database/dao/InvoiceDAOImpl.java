package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Invoice;
import com.aims.core.entities.OrderEntity; // For setting the order reference
import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.core.infrastructure.database.dao.IInvoiceDAO;
// Assuming IOrderEntityDAO exists if we need to fully reconstruct OrderEntity
// import com.aims.infrastructure.database.dao.IOrderEntityDAO;


import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InvoiceDAOImpl implements IInvoiceDAO {

    // Optional: Inject IOrderEntityDAO if you need to fetch the full OrderEntity object
    // private final IOrderEntityDAO orderEntityDAO;
    // public InvoiceDAOImpl(IOrderEntityDAO orderEntityDAO) {
    //     this.orderEntityDAO = orderEntityDAO;
    // }

    public InvoiceDAOImpl() {
        // Default constructor
    }

    private Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }

    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        if (rs == null) return null;

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(rs.getString("invoiceID"));

        // To fully set the OrderEntity object, you might need IOrderEntityDAO.
        // For simplicity here, we'll create a placeholder OrderEntity with just the ID.
        OrderEntity order = new OrderEntity();
        order.setOrderId(rs.getString("orderID"));
        invoice.setOrderEntity(order); // Changed from setOrder to setOrderEntity
        // Alternatively, if IOrderEntityDAO is injected:
        // OrderEntity order = orderEntityDAO.getById(rs.getString("orderID"));
        // invoice.setOrderEntity(order);


        String invoiceDateStr = rs.getString("invoiceDate");
        if (invoiceDateStr != null) {
            invoice.setInvoiceDate(LocalDateTime.parse(invoiceDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        invoice.setInvoicedTotalAmount(rs.getFloat("invoicedTotalAmount"));

        return invoice;
    }

    @Override
    public void add(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO INVOICE (invoiceID, orderID, invoiceDate, invoicedTotalAmount) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, invoice.getInvoiceId());
            pstmt.setString(2, invoice.getOrderEntity().getOrderId()); // Changed from getOrder to getOrderEntity
            pstmt.setString(3, invoice.getInvoiceDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setFloat(4, invoice.getInvoicedTotalAmount());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Check for specific SQLite error code for UNIQUE constraint violation (e.g., on orderID)
            // Error code 19: SQLITE_CONSTRAINT
            if (e.getErrorCode() == 19 && e.getMessage().toLowerCase().contains("invoice.orderid")) {
                 throw new SQLException("Invoice for order ID '" + invoice.getOrderEntity().getOrderId() + "' already exists.", e.getSQLState(), e.getErrorCode(), e); // Changed from getOrder to getOrderEntity
            }
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void update(Invoice invoice) throws SQLException {
        // Invoices are often immutable once created.
        // This method is provided for completeness but might not be used frequently.
        // If used, ensure only appropriate fields are updated.
        String sql = "UPDATE INVOICE SET invoiceDate = ?, invoicedTotalAmount = ? " +
                     "WHERE invoiceID = ? AND orderID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, invoice.getInvoiceDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.setFloat(2, invoice.getInvoicedTotalAmount());
            pstmt.setString(3, invoice.getInvoiceId());
            pstmt.setString(4, invoice.getOrderEntity().getOrderId()); // Changed from getOrder to getOrderEntity

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating invoice failed, no matching record found for ID: " + invoice.getInvoiceId());
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public Invoice getById(String invoiceId) throws SQLException {
        String sql = "SELECT * FROM INVOICE WHERE invoiceID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, invoiceId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToInvoice(rs);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return null;
    }

    @Override
    public Invoice getByOrderId(String orderId) throws SQLException {
        String sql = "SELECT * FROM INVOICE WHERE orderID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToInvoice(rs);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return null;
    }

    @Override
    public void deleteById(String invoiceId) throws SQLException {
        String sql = "DELETE FROM INVOICE WHERE invoiceID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, invoiceId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void deleteByOrderId(String orderId) throws SQLException {
        String sql = "DELETE FROM INVOICE WHERE orderID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }
}