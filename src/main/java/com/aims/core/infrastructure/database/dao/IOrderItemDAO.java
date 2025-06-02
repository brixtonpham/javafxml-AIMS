package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.OrderItem;
import java.sql.SQLException;
import java.util.List;

public interface IOrderItemDAO {

    /**
     * Adds a new OrderItem to the database.
     *
     * @param orderItem The OrderItem object to add.
     * @throws SQLException If a database access error occurs.
     */
    void add(OrderItem orderItem) throws SQLException;

    /**
     * Updates an existing OrderItem in the database.
     * (e.g., if quantity or price needed adjustment post-order creation, though rare for price).
     *
     * @param orderItem The OrderItem object with updated information.
     * @throws SQLException If a database access error occurs.
     */
    void update(OrderItem orderItem) throws SQLException;

    /**
     * Deletes an OrderItem from the database using its composite key (orderId and productId).
     *
     * @param orderId The ID of the order containing the item.
     * @param productId The ID of the product in the item.
     * @throws SQLException If a database access error occurs.
     */
    void delete(String orderId, String productId) throws SQLException;

    /**
     * Deletes all OrderItems associated with a specific order ID.
     * Used when an entire order is cancelled or deleted.
     *
     * @param orderId The ID of the order whose items are to be deleted.
     * @throws SQLException If a database access error occurs.
     */
    void deleteByOrderId(String orderId) throws SQLException;

    /**
     * Retrieves a specific OrderItem from the database by its composite key.
     *
     * @param orderId The ID of the order.
     * @param productId The ID of the product.
     * @return The OrderItem object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    OrderItem getByIds(String orderId, String productId) throws SQLException;

    /**
     * Retrieves all OrderItems associated with a specific order ID.
     *
     * @param orderId The ID of the order.
     * @return A list of OrderItem objects for the given order.
     * @throws SQLException If a database access error occurs.
     */
    List<OrderItem> getItemsByOrderId(String orderId) throws SQLException;
}