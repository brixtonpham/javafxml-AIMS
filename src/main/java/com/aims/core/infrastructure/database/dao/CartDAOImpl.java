package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.Product;
import com.aims.core.entities.UserAccount;
import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.core.infrastructure.database.dao.ICartDAO;
import com.aims.core.infrastructure.database.dao.ICartItemDAO; // For delegating item operations
import com.aims.core.infrastructure.database.dao.IProductDAO; // For fetching product details for CartItem
import com.aims.core.infrastructure.database.dao.IUserAccountDAO; // For fetching UserAccount if needed


import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CartDAOImpl implements ICartDAO {

    // Optional: Inject CartItemDAO for item-specific operations
    // This promotes better separation of concerns.
    private final ICartItemDAO cartItemDAO;
    private final IProductDAO productDAO; // To reconstruct Product objects for CartItems
    private final IUserAccountDAO userAccountDAO; // To reconstruct UserAccount objects


    public CartDAOImpl(ICartItemDAO cartItemDAO, IProductDAO productDAO, IUserAccountDAO userAccountDAO) {
        this.cartItemDAO = cartItemDAO; // Assume it's injected or instantiated
        this.productDAO = productDAO;
        this.userAccountDAO = userAccountDAO;
    }


    private Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }

    private Cart mapResultSetToCart(ResultSet rs) throws SQLException {
        if (rs == null) return null;
        Cart cart = new Cart();
        cart.setCartSessionId(rs.getString("cartSessionID"));
        String userId = rs.getString("userID");
        if (userId != null) {
            // In a real scenario, you might fetch the UserAccount object here
            // For now, just setting the ID or a placeholder/proxy.
            // UserAccount user = new UserAccount(); user.setUserId(userId); // Simplified
            UserAccount user = userAccountDAO.getById(userId); // Better, fetches full user
            cart.setUserAccount(user);
        }
        String lastUpdatedStr = rs.getString("lastUpdated");
        if (lastUpdatedStr != null) {
            cart.setLastUpdated(LocalDateTime.parse(lastUpdatedStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        // Items will be loaded separately or by a join
        return cart;
    }


    @Override
    public Cart getBySessionId(String cartSessionId) throws SQLException {
        String sql = "SELECT * FROM CART WHERE cartSessionID = ?";
        Cart cart = null;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cartSessionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                cart = mapResultSetToCart(rs);
                // Load cart items
                if (cart != null) {
                    cart.setItems(cartItemDAO.getItemsByCartSessionId(cartSessionId));
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return cart;
    }

    @Override
    public Cart getByUserId(String userId) throws SQLException {
        String sql = "SELECT * FROM CART WHERE userID = ?"; // Assuming one active cart per user
        Cart cart = null;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { // Get the first one, assuming one active cart
                cart = mapResultSetToCart(rs);
                if (cart != null) {
                    cart.setItems(cartItemDAO.getItemsByCartSessionId(cart.getCartSessionId()));
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return cart;
    }

    @Override
    public void saveOrUpdate(Cart cart) throws SQLException {
        // Check if cart exists
        Cart existingCart = getBySessionId(cart.getCartSessionId());
        String sql;

        if (existingCart == null) {
            // Insert new cart
            sql = "INSERT INTO CART (cartSessionID, userID, lastUpdated) VALUES (?, ?, ?)";
        } else {
            // Update existing cart
            sql = "UPDATE CART SET userID = ?, lastUpdated = ? WHERE cartSessionID = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (existingCart == null) {
                pstmt.setString(1, cart.getCartSessionId());
                if (cart.getUserAccount() != null) {
                    pstmt.setString(2, cart.getUserAccount().getUserId());
                } else {
                    pstmt.setNull(2, Types.VARCHAR);
                }
                pstmt.setString(3, cart.getLastUpdated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                if (cart.getUserAccount() != null) {
                    pstmt.setString(1, cart.getUserAccount().getUserId());
                } else {
                    pstmt.setNull(1, Types.VARCHAR);
                }
                pstmt.setString(2, cart.getLastUpdated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                pstmt.setString(3, cart.getCartSessionId());
            }
            pstmt.executeUpdate();

            // Save/Update cart items separately (often managed by CartItemDAO or service layer)
            // For simplicity, if CartDAO directly manages, could iterate cart.getItems()
            // and call cartItemDAO.saveOrUpdate(item) or similar.
            // This example assumes CartItemDAO is primarily used for fetching items in getBy... methods.

        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }


    @Override
    public void deleteBySessionId(String cartSessionId) throws SQLException {
        // Relies on ON DELETE CASCADE for CART_ITEM in the database schema
        String sql = "DELETE FROM CART WHERE cartSessionID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cartSessionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    // Methods for directly manipulating items within CartDAO:
    // These can also be implemented by delegating to ICartItemDAO.

    @Override
    public void addItemToCart(String cartSessionId, CartItem item) throws SQLException {
        // Ensure cart exists or create it first (could be part of service logic)
        // This DAO method assumes cart exists.
        // Delegates to CartItemDAO for actual item addition logic.
        item.setCart(this.getBySessionId(cartSessionId)); // Set the cart reference
        cartItemDAO.add(item); // Assuming CartItemDAO has an add method
    }

    @Override
    public void removeItemFromCart(String cartSessionId, String productId) throws SQLException {
        cartItemDAO.delete(cartSessionId, productId); // Assuming CartItemDAO has delete by composite ID
    }

    @Override
    public void updateItemQuantity(String cartSessionId, String productId, int quantity) throws SQLException {
        CartItem item = cartItemDAO.getByIds(cartSessionId, productId);
        if (item != null) {
            item.setQuantity(quantity);
            cartItemDAO.update(item); // Assuming CartItemDAO has an update method
        } else {
            // Optionally throw an exception if item not found
            throw new SQLException("Cart item not found for cartSessionId: " + cartSessionId + " and productId: " + productId);
        }
    }

    @Override
    public void clearCart(String cartSessionId) throws SQLException {
        // Deletes all CartItems associated with this cartSessionId
        cartItemDAO.deleteByCartSessionId(cartSessionId); // Assuming CartItemDAO has this method
    }
}