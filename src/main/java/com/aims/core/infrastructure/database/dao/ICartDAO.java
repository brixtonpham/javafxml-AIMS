package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem; // For methods that might add/remove items
import com.aims.core.entities.UserAccount;

import java.sql.SQLException;
import java.util.List;

public interface ICartDAO {

    /**
     * Retrieves a Cart from the database by its session ID.
     *
     * @param cartSessionId The session ID of the cart to retrieve.
     * @return The Cart object if found, otherwise null. This should include its items.
     * @throws SQLException If a database access error occurs.
     */
    Cart getBySessionId(String cartSessionId) throws SQLException;

    /**
     * Retrieves the active Cart for a given UserAccount.
     * This might be the same as getBySessionId if the session is tied to the user,
     * or it could look for a cart specifically linked to the user.
     *
     * @param userId The ID of the user.
     * @return The Cart object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    Cart getByUserId(String userId) throws SQLException;

    /**
     * Creates or updates a Cart in the database.
     * If the cart exists, it might update its lastUpdated timestamp or user association.
     * If it doesn't exist, it creates a new one.
     *
     * @param cart The Cart object to save or update.
     * @throws SQLException If a database access error occurs.
     */
    void saveOrUpdate(Cart cart) throws SQLException;


    /**
     * Deletes a Cart from the database by its session ID.
     * This would also typically delete all associated CartItems due to CASCADE.
     *
     * @param cartSessionId The session ID of the cart to delete.
     * @throws SQLException If a database access error occurs.
     */
    void deleteBySessionId(String cartSessionId) throws SQLException;

    /**
     * Adds an item to a specific cart.
     * Note: This might be better handled at the service layer which then calls
     * ICartItemDAO, but including it here for completeness if CartDAO directly manages items.
     *
     * @param cartSessionId The session ID of the cart.
     * @param item The CartItem to add.
     * @throws SQLException If a database access error occurs.
     */
    void addItemToCart(String cartSessionId, CartItem item) throws SQLException;

    /**
     * Removes an item from a specific cart.
     *
     * @param cartSessionId The session ID of the cart.
     * @param productId The ID of the product to remove.
     * @throws SQLException If a database access error occurs.
     */
    void removeItemFromCart(String cartSessionId, String productId) throws SQLException;

    /**
     * Updates the quantity of an item in a specific cart.
     *
     * @param cartSessionId The session ID of the cart.
     * @param productId The ID of the product whose quantity is to be updated.
     * @param quantity The new quantity.
     * @throws SQLException If a database access error occurs.
     */
    void updateItemQuantity(String cartSessionId, String productId, int quantity) throws SQLException;

    /**
     * Clears all items from a cart (empties the cart).
     *
     * @param cartSessionId The session ID of the cart to clear.
     * @throws SQLException If a database access error occurs.
     */
    void clearCart(String cartSessionId) throws SQLException;

}