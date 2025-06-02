package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderEntity; // For setting the order reference
import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.core.infrastructure.database.dao.IDeliveryInfoDAO;
// Assuming IOrderEntityDAO exists if we need to fully reconstruct OrderEntity
// import com.aims.infrastructure.database.dao.IOrderEntityDAO;


import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException; // Added import

public class DeliveryInfoDAOImpl implements IDeliveryInfoDAO {

    // Optional: Inject IOrderEntityDAO if you need to fetch the full OrderEntity object
    // private final IOrderEntityDAO orderEntityDAO;
    // public DeliveryInfoDAOImpl(IOrderEntityDAO orderEntityDAO) {
    //     this.orderEntityDAO = orderEntityDAO;
    // }

    public DeliveryInfoDAOImpl() {
        // Default constructor
    }


    private Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }

    private DeliveryInfo mapResultSetToDeliveryInfo(ResultSet rs) throws SQLException {
        if (rs == null) return null;

        DeliveryInfo deliveryInfo = new DeliveryInfo();
        deliveryInfo.setDeliveryInfoId(rs.getString("deliveryInfoID"));

        // To fully set the OrderEntity object, you might need IOrderEntityDAO.
        // For simplicity here, we'll create a placeholder OrderEntity with just the ID,
        // assuming the service layer or the entity itself handles lazy loading or reconstruction if needed.
        OrderEntity order = new OrderEntity();
        order.setOrderId(rs.getString("orderID"));
        deliveryInfo.setOrderEntity(order); // Changed from setOrder to setOrderEntity
        // Alternatively, if IOrderEntityDAO is injected:
        // OrderEntity order = orderEntityDAO.getById(rs.getString("orderID"));
        // deliveryInfo.setOrderEntity(order);


        deliveryInfo.setRecipientName(rs.getString("recipientName"));
        deliveryInfo.setEmail(rs.getString("email"));
        deliveryInfo.setPhoneNumber(rs.getString("phoneNumber"));
        deliveryInfo.setDeliveryProvinceCity(rs.getString("deliveryProvinceCity"));
        deliveryInfo.setDeliveryAddress(rs.getString("deliveryAddress"));
        deliveryInfo.setDeliveryInstructions(rs.getString("deliveryInstructions"));
        deliveryInfo.setDeliveryMethodChosen(rs.getString("deliveryMethodChosen"));

        String rushTimeStr = rs.getString("requestedRushDeliveryTime");
        if (rushTimeStr != null && !rushTimeStr.isEmpty()) {
            try {
                 deliveryInfo.setRequestedRushDeliveryTime(LocalDateTime.parse(rushTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (DateTimeParseException e) { // More specific exception for format issues
                System.err.println("Error parsing requestedRushDeliveryTime '" + rushTimeStr +
                                   "' due to invalid format for deliveryInfoID: " + deliveryInfo.getDeliveryInfoId() +
                                   ". Expected ISO_LOCAL_DATE_TIME. Details: " + e.getMessage());
                deliveryInfo.setRequestedRushDeliveryTime(null); // Fallback to null
            } catch (Exception e) { // Catch any other unexpected error during parsing
                System.err.println("An unexpected error occurred while parsing requestedRushDeliveryTime '" + rushTimeStr +
                                   "' for deliveryInfoID: " + deliveryInfo.getDeliveryInfoId() + ". Details: " + e.getMessage());
                deliveryInfo.setRequestedRushDeliveryTime(null); // Fallback to null
            }
        } else {
            deliveryInfo.setRequestedRushDeliveryTime(null);
        }

        return deliveryInfo;
    }

    @Override
    public void add(DeliveryInfo deliveryInfo) throws SQLException {
        String sql = "INSERT INTO DELIVERY_INFO (deliveryInfoID, orderID, recipientName, email, phoneNumber, " +
                     "deliveryProvinceCity, deliveryAddress, deliveryInstructions, deliveryMethodChosen, requestedRushDeliveryTime) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, deliveryInfo.getDeliveryInfoId());
            pstmt.setString(2, deliveryInfo.getOrderEntity().getOrderId()); // Changed from getOrder to getOrderEntity
            pstmt.setString(3, deliveryInfo.getRecipientName());
            pstmt.setString(4, deliveryInfo.getEmail());
            pstmt.setString(5, deliveryInfo.getPhoneNumber());
            pstmt.setString(6, deliveryInfo.getDeliveryProvinceCity());
            pstmt.setString(7, deliveryInfo.getDeliveryAddress());
            pstmt.setString(8, deliveryInfo.getDeliveryInstructions());
            pstmt.setString(9, deliveryInfo.getDeliveryMethodChosen());

            if (deliveryInfo.getRequestedRushDeliveryTime() != null) {
                pstmt.setString(10, deliveryInfo.getRequestedRushDeliveryTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                pstmt.setNull(10, Types.VARCHAR);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Check for specific SQLite error code for UNIQUE constraint violation (e.g., on orderID if it's UNIQUE)
            // Error code 19: SQLITE_CONSTRAINT
            if (e.getErrorCode() == 19 && e.getMessage().toLowerCase().contains("delivery_info.orderid")) {
                 throw new SQLException("Delivery information for order ID '" + deliveryInfo.getOrderEntity().getOrderId() + "' already exists.", e.getSQLState(), e.getErrorCode(), e); // Changed from getOrder to getOrderEntity
            }
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void update(DeliveryInfo deliveryInfo) throws SQLException {
        String sql = "UPDATE DELIVERY_INFO SET recipientName = ?, email = ?, phoneNumber = ?, " +
                     "deliveryProvinceCity = ?, deliveryAddress = ?, deliveryInstructions = ?, " +
                     "deliveryMethodChosen = ?, requestedRushDeliveryTime = ? " +
                     "WHERE deliveryInfoID = ? AND orderID = ?"; // Usually update by deliveryInfoID or orderID
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, deliveryInfo.getRecipientName());
            pstmt.setString(2, deliveryInfo.getEmail());
            pstmt.setString(3, deliveryInfo.getPhoneNumber());
            pstmt.setString(4, deliveryInfo.getDeliveryProvinceCity());
            pstmt.setString(5, deliveryInfo.getDeliveryAddress());
            pstmt.setString(6, deliveryInfo.getDeliveryInstructions());
            pstmt.setString(7, deliveryInfo.getDeliveryMethodChosen());
            if (deliveryInfo.getRequestedRushDeliveryTime() != null) {
                pstmt.setString(8, deliveryInfo.getRequestedRushDeliveryTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                pstmt.setNull(8, Types.VARCHAR);
            }
            pstmt.setString(9, deliveryInfo.getDeliveryInfoId());
            pstmt.setString(10, deliveryInfo.getOrderEntity().getOrderId()); // Changed from getOrder to getOrderEntity

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating delivery info failed, no matching record found for ID: " + deliveryInfo.getDeliveryInfoId() + " and Order ID: " + deliveryInfo.getOrderEntity().getOrderId()); // Changed from getOrder to getOrderEntity
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public DeliveryInfo getById(String deliveryInfoId) throws SQLException {
        String sql = "SELECT * FROM DELIVERY_INFO WHERE deliveryInfoID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deliveryInfoId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToDeliveryInfo(rs);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return null;
    }

    @Override
    public DeliveryInfo getByOrderId(String orderId) throws SQLException {
        String sql = "SELECT * FROM DELIVERY_INFO WHERE orderID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToDeliveryInfo(rs);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return null;
    }

    @Override
    public void deleteById(String deliveryInfoId) throws SQLException {
        String sql = "DELETE FROM DELIVERY_INFO WHERE deliveryInfoID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deliveryInfoId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void deleteByOrderId(String orderId) throws SQLException {
        String sql = "DELETE FROM DELIVERY_INFO WHERE orderID = ?";
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