package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.core.infrastructure.database.dao.IOrderItemDAO;
import com.aims.core.infrastructure.database.dao.IProductDAO; // To fetch Product details
// Assuming IOrderEntityDAO exists if we need to fetch OrderEntity details, but often not needed for OrderItem mapping
// import com.aims.infrastructure.database.dao.IOrderEntityDAO;
import org.springframework.stereotype.Repository;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderItemDAOImpl implements IOrderItemDAO {

    private final IProductDAO productDAO; // Used to reconstruct Product objects

    // Constructor to inject dependencies
    public OrderItemDAOImpl(IProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    private Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }

    private OrderItem mapResultSetToOrderItem(ResultSet rs, OrderEntity order) throws SQLException {
        if (rs == null) return null;

        // Read all fields from the ResultSet first before any external DAO calls
        String productId = rs.getString("productID");
        int quantity = rs.getInt("quantity");
        float priceAtTimeOfOrder = rs.getFloat("priceAtTimeOfOrder");
        boolean isEligibleForRushDelivery = rs.getInt("isEligibleForRushDelivery") == 1;

        // Now fetch the Product, which is an external call
        Product product = productDAO.getById(productId); 

        if (product == null) {
            System.err.println("Warning: Product with ID " + productId + " not found for order item in order " + (order != null ? order.getOrderId() : "UNKNOWN_ORDER"));
            return null; 
        }

        OrderItem item = new OrderItem();
        item.setOrderEntity(order); 
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setPriceAtTimeOfOrder(priceAtTimeOfOrder);
        item.setEligibleForRushDelivery(isEligibleForRushDelivery);
        return item;
    }


    @Override
    public void add(OrderItem orderItem) throws SQLException {
        // It's generally assumed an OrderItem is unique per orderId and productId.
        // If an attempt is made to add an existing combination, it might violate a PK constraint.
        // The service layer should typically ensure items are correctly added (e.g., new item or update quantity).
        String sql = "INSERT INTO ORDER_ITEM (orderID, productID, quantity, priceAtTimeOfOrder, isEligibleForRushDelivery) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, orderItem.getOrderEntity().getOrderId()); // Changed from getOrder to getOrderEntity
            pstmt.setString(2, orderItem.getProduct().getProductId());
            pstmt.setInt(3, orderItem.getQuantity());
            pstmt.setFloat(4, orderItem.getPriceAtTimeOfOrder());
            pstmt.setInt(5, orderItem.isEligibleForRushDelivery() ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void update(OrderItem orderItem) throws SQLException {
        // Typically, only quantity might be updated for an order item post-creation,
        // priceAtTimeOfOrder should remain fixed. Rush eligibility might change in some scenarios.
        String sql = "UPDATE ORDER_ITEM SET quantity = ?, priceAtTimeOfOrder = ?, isEligibleForRushDelivery = ? " +
                     "WHERE orderID = ? AND productID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderItem.getQuantity());
            pstmt.setFloat(2, orderItem.getPriceAtTimeOfOrder());
            pstmt.setInt(3, orderItem.isEligibleForRushDelivery() ? 1 : 0);
            pstmt.setString(4, orderItem.getOrderEntity().getOrderId()); // Changed from getOrder to getOrderEntity
            pstmt.setString(5, orderItem.getProduct().getProductId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                 // Item not found for update, could log or throw exception
                // System.err.println("Warning: OrderItem not found for update: OrderID "
                // + orderItem.getOrder().getOrderId() + ", ProductID " + orderItem.getProduct().getProductId());
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void delete(String orderId, String productId) throws SQLException {
        String sql = "DELETE FROM ORDER_ITEM WHERE orderID = ? AND productID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            pstmt.setString(2, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void deleteByOrderId(String orderId) throws SQLException {
        String sql = "DELETE FROM ORDER_ITEM WHERE orderID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public OrderItem getByIds(String orderId, String productId) throws SQLException {
        String sql = "SELECT * FROM ORDER_ITEM WHERE orderID = ? AND productID = ?";
        OrderItem item = null;
        Connection conn = getConnection(); // Get connection
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, orderId);
            pstmt.setString(2, productId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                OrderEntity tempOrder = new OrderEntity();
                tempOrder.setOrderId(orderId);
                item = mapResultSetToOrderItem(rs, tempOrder);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        } finally {
            // Close ResultSet and PreparedStatement, but not the Connection
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    SQLiteConnector.printSQLException(e);
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    SQLiteConnector.printSQLException(e);
                }
            }
            // Connection is NOT closed here
        }
        return item;
    }

    // This method is used by OrderEntityDAOImpl.getById to load items for a specific order.
    // It assumes the OrderEntity object (parent) is already known and passed.
    @Override
    public List<OrderItem> getItemsByOrderId(String orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM ORDER_ITEM WHERE orderID = ?";
        OrderEntity mockOrder = new OrderEntity(); // Create a temporary OrderEntity shell
        mockOrder.setOrderId(orderId); // Set its ID for context in mapResultSetToOrderItem

        Connection conn = getConnection(); // Get connection
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, orderId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                OrderItem item = mapResultSetToOrderItem(rs, mockOrder);
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        } finally {
            // Close ResultSet and PreparedStatement, but not the Connection
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    SQLiteConnector.printSQLException(e);
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    SQLiteConnector.printSQLException(e);
                }
            }
            // Connection is NOT closed here
        }
        return items;
    }
}