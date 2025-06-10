package com.aims.core.application.services;

import com.aims.core.shared.exceptions.ValidationException;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Service interface for Product Manager audit operations.
 * Handles tracking and validation of Product Manager daily operation limits.
 */
public interface IProductManagerAuditService {

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
     * Checks if a Product Manager can perform the requested number of operations based on daily limits.
     * - UPDATE and DELETE operations: Max 30 per day combined
     * - ADD operations: Unlimited
     *
     * @param managerId The ID of the product manager
     * @param operationsCount The number of operations to validate
     * @throws ValidationException If the operation would exceed daily limits
     * @throws SQLException If a database error occurs
     */
    void checkDailyOperationLimit(String managerId, int operationsCount) throws ValidationException, SQLException;

    /**
     * Checks if a Product Manager can perform a price update on a specific product.
     * - PRICE_UPDATE operations: Max 2 per product per day
     *
     * @param managerId The ID of the product manager
     * @param productId The ID of the product
     * @throws ValidationException If the operation would exceed daily limits
     * @throws SQLException If a database error occurs
     */
    void checkPriceUpdateLimit(String managerId, String productId) throws ValidationException, SQLException;

    /**
     * Gets the operation count for a manager on a specific date.
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
}