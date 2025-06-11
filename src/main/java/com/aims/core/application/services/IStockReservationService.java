package com.aims.core.application.services;

import com.aims.core.shared.exceptions.InventoryException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;

import java.sql.SQLException;
import java.util.Map;

/**
 * Service for managing temporary stock reservations during checkout process
 * to prevent overselling and race conditions.
 */
public interface IStockReservationService {
    
    /**
     * Reserves stock for a product temporarily during checkout
     * @param productId Product to reserve stock for
     * @param quantity Quantity to reserve
     * @param reservationId Unique reservation identifier (e.g., order ID or session ID)
     * @param timeoutMinutes How long to hold the reservation (default: 15 minutes)
     * @return true if reservation successful, false if insufficient stock
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Product not found
     * @throws ValidationException Invalid parameters
     */
    boolean reserveStock(String productId, int quantity, String reservationId, int timeoutMinutes) 
            throws SQLException, ResourceNotFoundException, ValidationException;
    
    /**
     * Confirms and commits a stock reservation (decrements actual stock)
     * @param reservationId Reservation to confirm
     * @throws SQLException Database error
     * @throws InventoryException Reservation not found or expired
     */
    void confirmReservation(String reservationId) 
            throws SQLException, InventoryException;
    
    /**
     * Releases a stock reservation (makes stock available again)
     * @param reservationId Reservation to release
     * @throws SQLException Database error
     */
    void releaseReservation(String reservationId) throws SQLException;
    
    /**
     * Checks if sufficient stock is available for reservation
     * @param productId Product to check
     * @param quantity Quantity needed
     * @return true if sufficient stock available
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Product not found
     */
    boolean isStockAvailable(String productId, int quantity) 
            throws SQLException, ResourceNotFoundException;
    
    /**
     * Gets current available stock (actual stock minus active reservations)
     * @param productId Product to check
     * @return Available stock quantity
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Product not found
     */
    int getAvailableStock(String productId) 
            throws SQLException, ResourceNotFoundException;
    
    /**
     * Cleans up expired reservations
     * @return Number of expired reservations cleaned up
     * @throws SQLException Database error
     */
    int cleanupExpiredReservations() throws SQLException;
    
    /**
     * Gets all active reservations for monitoring purposes
     * @return Map of reservation ID to product details
     * @throws SQLException Database error
     */
    Map<String, String> getActiveReservations() throws SQLException;
}