package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.DeliveryInfo;
import java.sql.SQLException;
import java.util.List; // Not typically needed for DeliveryInfo as it's often 1-to-1 with Order

public interface IDeliveryInfoDAO {

    /**
     * Adds new Delivery Information to the database for a specific order.
     *
     * @param deliveryInfo The DeliveryInfo object to add.
     * @throws SQLException If a database access error occurs or if DeliveryInfo for the order already exists.
     */
    void add(DeliveryInfo deliveryInfo) throws SQLException;

    /**
     * Updates existing Delivery Information in the database for a specific order.
     *
     * @param deliveryInfo The DeliveryInfo object with updated information.
     * @throws SQLException If a database access error occurs or if no record found for the orderId.
     */
    void update(DeliveryInfo deliveryInfo) throws SQLException;

    /**
     * Retrieves Delivery Information from the database by its specific ID.
     *
     * @param deliveryInfoId The ID of the DeliveryInfo record.
     * @return The DeliveryInfo object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    DeliveryInfo getById(String deliveryInfoId) throws SQLException;

    /**
     * Retrieves Delivery Information associated with a specific order ID.
     * Since it's a one-to-one relationship, this should return a single object.
     *
     * @param orderId The ID of the order.
     * @return The DeliveryInfo object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    DeliveryInfo getByOrderId(String orderId) throws SQLException;

    /**
     * Deletes Delivery Information from the database by its ID.
     *
     * @param deliveryInfoId The ID of the DeliveryInfo record to delete.
     * @throws SQLException If a database access error occurs.
     */
    void deleteById(String deliveryInfoId) throws SQLException;

    /**
     * Deletes Delivery Information associated with a specific order ID.
     * Useful if an order is deleted and cascading delete is not relied upon or needs to be explicit.
     *
     * @param orderId The ID of the order whose delivery information is to be deleted.
     * @throws SQLException If a database access error occurs.
     */
    void deleteByOrderId(String orderId) throws SQLException;
}