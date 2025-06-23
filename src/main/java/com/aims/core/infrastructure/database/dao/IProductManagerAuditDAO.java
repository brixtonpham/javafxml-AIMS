package com.aims.core.infrastructure.database.dao;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * DAO interface for Product Manager audit operations.
 * Handles tracking and logging of Product Manager activities.
 */
public interface IProductManagerAuditDAO {

    /**
     * Logs an operation performed by a Product Manager.
     *
     * @param managerId The ID of the product manager
     * @param operationType The type of operation (ADD, UPDATE, DELETE, PRICE_UPDATE)
     * @param productId The ID of the product affected (if applicable)
     * @param details Additional operation details
     * @throws SQLException If a database error occurs
     */
    void logOperation(String managerId, String operationType, String productId, String details) throws SQLException;

    /**
     * Gets the total operation count for a manager on a specific date.
     * Counts UPDATE and DELETE operations (ADD operations are unlimited).
     *
     * @param managerId The ID of the product manager
     * @param date The date to check
     * @return The number of operations performed on that date
     * @throws SQLException If a database error occurs
     */
    int getOperationCount(String managerId, LocalDate date) throws SQLException;

    /**
     * Gets the price update count for a manager and product on a specific date.
     *
     * @param managerId The ID of the product manager
     * @param productId The ID of the product
     * @param date The date to check
     * @return The number of price updates performed on that date for that product
     * @throws SQLException If a database error occurs
     */
    int getPriceUpdateCount(String managerId, String productId, LocalDate date) throws SQLException;

    /**
     * Gets all operations performed by a manager on a specific date.
     *
     * @param managerId The ID of the product manager
     * @param date The date to check
     * @return List of operation keys for that manager and date
     * @throws SQLException If a database error occurs
     */
    java.util.List<String> getManagerOperations(String managerId, LocalDate date) throws SQLException;

    /**
     * Cleans up old audit records (optional, for maintenance).
     *
     * @param daysToKeep Number of days of records to keep
     * @throws SQLException If a database error occurs
     */
    void cleanupOldRecords(int daysToKeep) throws SQLException;
}