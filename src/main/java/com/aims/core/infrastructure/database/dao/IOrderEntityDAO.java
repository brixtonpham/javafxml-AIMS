package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem; // For methods that might add/remove items if managed here
import com.aims.core.enums.OrderStatus;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface IOrderEntityDAO {

    /**
     * Retrieves an OrderEntity from the database by its ID.
     * This should also load associated OrderItems, DeliveryInfo, etc., or provide methods to do so.
     *
     * @param orderId The ID of the order to retrieve.
     * @return The OrderEntity object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    OrderEntity getById(String orderId) throws SQLException;

    /**
     * Retrieves all OrderEntities from the database.
     * Optionally, this could be paginated or filtered.
     *
     * @return A list of all OrderEntity objects.
     * @throws SQLException If a database access error occurs.
     */
    List<OrderEntity> getAll() throws SQLException;

    /**
     * Retrieves all OrderEntities for a specific user.
     *
     * @param userId The ID of the user whose orders are to be retrieved.
     * @return A list of OrderEntity objects for the user.
     * @throws SQLException If a database access error occurs.
     */
    List<OrderEntity> getByUserId(String userId) throws SQLException;

    /**
     * Retrieves OrderEntities based on their status.
     *
     * @param status The status of the orders to retrieve.
     * @return A list of OrderEntity objects with the given status.
     * @throws SQLException If a database access error occurs.
     */
    List<OrderEntity> getByStatus(OrderStatus status) throws SQLException;

    /**
     * Retrieves OrderEntities within a specific date range.
     *
     * @param startDate The start of the date range.
     * @param endDate The end of the date range.
     * @return A list of OrderEntity objects within the date range.
     * @throws SQLException If a database access error occurs.
     */
    List<OrderEntity> getByDateRange(LocalDateTime startDate, LocalDateTime endDate) throws SQLException;

    /**
     * Adds a new OrderEntity to the database.
     * Associated OrderItems, DeliveryInfo, etc., should typically be saved separately
     * by their respective DAOs or through service layer coordination.
     *
     * @param order The OrderEntity object to add.
     * @throws SQLException If a database access error occurs.
     */
    void add(OrderEntity order) throws SQLException;

    /**
     * Updates an existing OrderEntity's information in the database,
     * primarily its status or calculated amounts.
     *
     * @param order The OrderEntity object with updated information.
     * @throws SQLException If a database access error occurs.
     */
    void update(OrderEntity order) throws SQLException;

    /**
     * Updates the status of a specific order.
     *
     * @param orderId The ID of the order to update.
     * @param newStatus The new status for the order.
     * @throws SQLException If a database access error occurs.
     */
    void updateStatus(String orderId, OrderStatus newStatus) throws SQLException;

    /**
     * Deletes an OrderEntity from the database by its ID.
     * Note: Consider implications for related entities (OrderItems, DeliveryInfo, etc.).
     * Database constraints (ON DELETE CASCADE) or service layer logic should handle this.
     *
     * @param orderId The ID of the order to delete.
     * @throws SQLException If a database access error occurs.
     */
    void delete(String orderId) throws SQLException;

    // Potentially methods to manage OrderItems if not strictly separated
    // void addOrderItem(String orderId, OrderItem item) throws SQLException;
    // void removeOrderItem(String orderId, String productId) throws SQLException;
}