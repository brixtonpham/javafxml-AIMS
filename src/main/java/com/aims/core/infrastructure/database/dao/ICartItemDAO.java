package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.CartItem;
import java.sql.SQLException;
import java.util.List;

public interface ICartItemDAO {

    /**
     * Adds a new CartItem to the database.
     * If the item (same product in the same cart) already exists,
     * this method might update its quantity or throw an error,
     * depending on business logic (often handled by the service layer).
     * For simplicity, this DAO method will assume it's a new entry
     * or an update if PK exists.
     *
     * @param cartItem The CartItem object to add.
     * @throws SQLException If a database access error occurs.
     */
    void add(CartItem cartItem) throws SQLException;

    /**
     * Updates an existing CartItem in the database (e.g., quantity change).
     *
     * @param cartItem The CartItem object with updated information.
     * @throws SQLException If a database access error occurs.
     */
    void update(CartItem cartItem) throws SQLException;

    /**
     * Deletes a CartItem from the database using its composite key.
     *
     * @param cartSessionId The session ID of the cart.
     * @param productId The ID of the product.
     * @throws SQLException If a database access error occurs.
     */
    void delete(String cartSessionId, String productId) throws SQLException;

    /**
     * Deletes all CartItems associated with a specific cart session ID.
     * Used when clearing or deleting a cart.
     *
     * @param cartSessionId The session ID of the cart whose items are to be deleted.
     * @throws SQLException If a database access error occurs.
     */
    void deleteByCartSessionId(String cartSessionId) throws SQLException;

    /**
     * Retrieves a specific CartItem from the database by its composite key.
     *
     * @param cartSessionId The session ID of the cart.
     * @param productId The ID of the product.
     * @return The CartItem object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    CartItem getByIds(String cartSessionId, String productId) throws SQLException;

    /**
     * Retrieves all CartItems associated with a specific cart session ID.
     *
     * @param cartSessionId The session ID of the cart.
     * @return A list of CartItem objects.
     * @throws SQLException If a database access error occurs.
     */
    List<CartItem> getItemsByCartSessionId(String cartSessionId) throws SQLException;
}