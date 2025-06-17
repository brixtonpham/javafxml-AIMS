package com.aims.core.application.services;

import com.aims.core.entities.OrderEntity;
import com.aims.core.application.dtos.OrderSummaryDTO;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;

/**
 * Enhanced Order Data Loading Service
 * 
 * Addresses lazy loading issues and provides comprehensive order data retrieval
 * with all relationships properly loaded for UI consumption.
 */
public interface IOrderDataLoaderService {
    
    /**
     * Loads complete order data with ALL relationships eagerly loaded.
     * This method ensures that all associated entities (OrderItems, DeliveryInfo, 
     * Invoice, PaymentTransactions) are properly loaded to avoid lazy loading issues.
     * 
     * @param orderId The ID of the order to load
     * @return OrderEntity with all relationships loaded
     * @throws ResourceNotFoundException If the order is not found
     */
    OrderEntity loadCompleteOrderData(String orderId) throws ResourceNotFoundException;
    
    /**
     * Creates comprehensive OrderSummaryDTO with validation.
     * Converts a complete OrderEntity to a validated DTO structure suitable
     * for UI consumption with all required fields properly populated.
     * 
     * @param order The complete OrderEntity to convert
     * @return OrderSummaryDTO with all required fields populated
     * @throws ValidationException If order data is incomplete or invalid
     */
    OrderSummaryDTO createOrderSummaryDTO(OrderEntity order) throws ValidationException;
    
    /**
     * Validates order data completeness for UI display.
     * Checks that all required relationships and fields are present
     * and properly loaded for UI consumption.
     * 
     * @param order The OrderEntity to validate
     * @return true if order data is complete, false otherwise
     */
    boolean validateOrderDataCompleteness(OrderEntity order);
    
    /**
     * Loads order with fallback mechanisms for missing data.
     * Attempts to load complete order data with graceful handling
     * of missing or null relationships, providing fallback values
     * where appropriate.
     * 
     * @param orderId The ID of the order to load
     * @return OrderEntity with complete data or appropriate fallbacks
     * @throws ResourceNotFoundException If the order is not found
     */
    OrderEntity loadOrderWithFallbacks(String orderId) throws ResourceNotFoundException;
    
    /**
     * Refreshes all relationships for an existing order entity.
     * Useful when you have an order entity but need to ensure all
     * relationships are properly loaded and up-to-date.
     * 
     * @param order The OrderEntity to refresh
     * @return OrderEntity with refreshed relationships
     * @throws ResourceNotFoundException If related data is not found
     */
    OrderEntity refreshOrderRelationships(OrderEntity order) throws ResourceNotFoundException;
    
    /**
     * Validates that all lazy-loaded relationships are initialized.
     * Checks specifically for common lazy loading issues and ensures
     * all relationships are properly accessible.
     * 
     * @param order The OrderEntity to check
     * @return true if all relationships are initialized, false otherwise
     */
    boolean validateLazyLoadingInitialization(OrderEntity order);
}