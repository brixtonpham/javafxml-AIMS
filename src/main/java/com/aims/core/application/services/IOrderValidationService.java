package com.aims.core.application.services;

import com.aims.core.entities.OrderEntity;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import java.sql.SQLException;

/**
 * Service interface for transaction-safe order validation.
 * Provides atomic order validation operations for payment processing.
 */
public interface IOrderValidationService {

    /**
     * Checks if an order is ready for payment processing.
     * Performs comprehensive validation of order status, delivery info, and business rules.
     *
     * @param orderId The ID of the order to validate
     * @return true if order is ready for payment, false otherwise
     * @throws SQLException If a database error occurs
     * @throws ResourceNotFoundException If the order is not found
     */
    boolean isOrderReadyForPayment(String orderId) throws SQLException, ResourceNotFoundException;

    /**
     * Fetches and validates an order for payment processing atomically.
     * This method provides the authoritative order state from database.
     *
     * @param orderId The ID of the order to fetch and validate
     * @return The validated OrderEntity ready for payment
     * @throws SQLException If a database error occurs
     * @throws ResourceNotFoundException If the order is not found
     * @throws ValidationException If the order is not in a valid state for payment
     */
    OrderEntity getValidatedOrderForPayment(String orderId) throws SQLException, ResourceNotFoundException, ValidationException;

    /**
     * Performs comprehensive order integrity check.
     * Validates order items, delivery info, amounts, and business logic.
     *
     * @param orderId The ID of the order to validate
     * @return true if order integrity is valid, false otherwise
     * @throws SQLException If a database error occurs
     * @throws ResourceNotFoundException If the order is not found
     */
    boolean validateOrderIntegrity(String orderId) throws SQLException, ResourceNotFoundException;

    /**
     * Validates that an order exists in the database and is accessible.
     * This is a fast existence check for early validation.
     *
     * @param orderId The ID of the order to check
     * @return true if order exists and is accessible, false otherwise
     * @throws SQLException If a database error occurs
     */
    boolean orderExists(String orderId) throws SQLException;

    /**
     * Validates order business rules for payment processing.
     * Checks order items, amounts, delivery requirements, etc.
     *
     * @param order The order to validate
     * @throws ValidationException If business rules are violated
     */
    void validateOrderBusinessRules(OrderEntity order) throws ValidationException;
}