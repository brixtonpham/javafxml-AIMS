package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.Product;
import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.core.infrastructure.database.dao.ICartDAO; // May not be needed directly if Cart object is passed
import com.aims.core.infrastructure.database.dao.ICartItemDAO;
import com.aims.core.infrastructure.database.dao.IProductDAO; // To fetch Product details

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CartItemDAOImpl implements ICartItemDAO {

    private final IProductDAO productDAO; // Used to reconstruct Product objects

    public CartItemDAOImpl(IProductDAO productDAO) {
        this.productDAO = productDAO; // Inject ProductDAO
    }

    private Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }

    private CartItem mapResultSetToCartItem(ResultSet rs, Cart cart) throws SQLException {
        if (rs == null) return null;
        String productId = rs.getString("productID");
        Product product = productDAO.getById(productId); // Fetch full product details

        if (product == null) {
            // This case should ideally not happen if data integrity is maintained
            // or handle it by skipping the item or throwing an error
            System.err.println("Warning: Product with ID " + productId + " not found for cart item.");
            return null;
        }

        CartItem item = new CartItem();
        item.setCart(cart); // The cart object is passed in or reconstructed if needed
        item.setProduct(product);
        item.setQuantity(rs.getInt("quantity"));
        return item;
    }


    @Override
    public void add(CartItem cartItem) throws SQLException {
        // This method assumes that if a CartItem with the same composite key
        // (cartSessionId, productId) exists, it should be updated.
        // Otherwise, it inserts a new one. This is an "upsert" logic.
        String selectSql = "SELECT quantity FROM CART_ITEM WHERE cartSessionID = ? AND productID = ?";
        String insertSql = "INSERT INTO CART_ITEM (cartSessionID, productID, quantity) VALUES (?, ?, ?)";
        String updateSql = "UPDATE CART_ITEM SET quantity = ? WHERE cartSessionID = ? AND productID = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction for upsert logic

            int existingQuantity = 0;
            boolean itemExists = false;

            try (PreparedStatement selectPstmt = conn.prepareStatement(selectSql)) {
                selectPstmt.setString(1, cartItem.getCart().getCartSessionId());
                selectPstmt.setString(2, cartItem.getProduct().getProductId());
                ResultSet rs = selectPstmt.executeQuery();
                if (rs.next()) {
                    existingQuantity = rs.getInt("quantity");
                    itemExists = true;
                }
            }

            if (itemExists) {
                // Item exists, update quantity (typically add to existing, but here we set to new quantity)
                // Business logic for "adding" vs "setting" quantity should be in service layer.
                // This DAO method will just set the provided quantity.
                try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                    updatePstmt.setInt(1, cartItem.getQuantity());
                    updatePstmt.setString(2, cartItem.getCart().getCartSessionId());
                    updatePstmt.setString(3, cartItem.getProduct().getProductId());
                    updatePstmt.executeUpdate();
                }
            } else {
                // Item does not exist, insert new
                try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                    insertPstmt.setString(1, cartItem.getCart().getCartSessionId());
                    insertPstmt.setString(2, cartItem.getProduct().getProductId());
                    insertPstmt.setInt(3, cartItem.getQuantity());
                    insertPstmt.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    SQLiteConnector.printSQLException(ex);
                }
            }
            SQLiteConnector.printSQLException(e);
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    SQLiteConnector.printSQLException(ex);
                }
            }
        }
    }

    @Override
    public void update(CartItem cartItem) throws SQLException {
        String sql = "UPDATE CART_ITEM SET quantity = ? WHERE cartSessionID = ? AND productID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cartItem.getQuantity());
            pstmt.setString(2, cartItem.getCart().getCartSessionId());
            pstmt.setString(3, cartItem.getProduct().getProductId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                // This means the item to update was not found.
                // Depending on requirements, you might throw an exception or log this.
                // For now, we let it pass, but in a real app, this might indicate an issue.
                // throw new SQLException("CartItem not found for update with cartId: " +
                // cartItem.getCart().getCartSessionId() + " and productId: " + cartItem.getProduct().getProductId());
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void delete(String cartSessionId, String productId) throws SQLException {
        String sql = "DELETE FROM CART_ITEM WHERE cartSessionID = ? AND productID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cartSessionId);
            pstmt.setString(2, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void deleteByCartSessionId(String cartSessionId) throws SQLException {
        String sql = "DELETE FROM CART_ITEM WHERE cartSessionID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cartSessionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public CartItem getByIds(String cartSessionId, String productId) throws SQLException {
        String sql = "SELECT * FROM CART_ITEM WHERE cartSessionID = ? AND productID = ?";
        CartItem item = null;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cartSessionId);
            pstmt.setString(2, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // To fully construct CartItem, we need the Cart object.
                // For simplicity, we might only fetch it if absolutely needed here,
                // or assume the service layer handles reconstructing the full Cart object.
                // Here, we'll create a placeholder Cart for the item.
                Cart tempCart = new Cart();
                tempCart.setCartSessionId(cartSessionId);
                // No need to fetch UserAccount for this temp cart in this specific context

                item = mapResultSetToCartItem(rs, tempCart);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return item;
    }

    @Override
    public List<CartItem> getItemsByCartSessionId(String cartSessionId) throws SQLException {
        List<CartItem> items = new ArrayList<>();
        String sql = "SELECT * FROM CART_ITEM WHERE cartSessionID = ?";

        // First, fetch the Cart object itself if needed, or construct a temporary one
        // For this method, it's implied we have the cart context, so a temporary
        // Cart object with just the session ID might suffice for mapResultSetToCartItem.
        Cart tempCart = new Cart();
        tempCart.setCartSessionId(cartSessionId);
        // Optionally, if the Cart needs its UserAccount for items:
        // String cartUserSql = "SELECT userID FROM CART WHERE cartSessionID = ?";
        // try (Connection conn = getConnection(); PreparedStatement userPstmt = conn.prepareStatement(cartUserSql)) {
        //     userPstmt.setString(1, cartSessionId);
        //     ResultSet userRs = userPstmt.executeQuery();
        //     if (userRs.next() && userRs.getString("userID") != null) {
        //         UserAccount user = productDAO.getUserAccountById(userRs.getString("userID")); // Assuming IUserAccountDAO
        //         tempCart.setUserAccount(user);
        //     }
        // }


        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cartSessionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                CartItem item = mapResultSetToCartItem(rs, tempCart);
                if (item != null) { // mapResultSetToCartItem might return null if product not found
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return items;
    }
}