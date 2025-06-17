package com.aims.core.application.services;

import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.application.dtos.CartValidationResult;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import java.sql.SQLException;

/**
 * Service for validating cart data before order creation to ensure complete
 * and accurate cart-to-order conversion with comprehensive data preservation.
 */
public interface ICartDataValidationService {
    
    /**
     * Validates cart completeness before order creation.
     * Checks for missing product data, stock availability, and data integrity.
     * 
     * @param cart The cart to validate
     * @return CartValidationResult containing validation status and details
     * @throws SQLException If database errors occur during validation
     */
    CartValidationResult validateCartForOrderCreation(Cart cart) throws SQLException;
    
    /**
     * Validates individual cart items for order conversion.
     * Ensures product exists, has complete metadata, and sufficient stock.
     * 
     * @param cartItem The cart item to validate
     * @return true if cart item is valid for order conversion
     * @throws SQLException If database errors occur during validation
     */
    boolean validateCartItemForOrderConversion(CartItem cartItem) throws SQLException;
    
    /**
     * Enriches cart items with missing product metadata by loading complete
     * product data including images, descriptions, categories, and variants.
     * 
     * @param cart The cart to enrich with complete product metadata
     * @return Cart with enriched product data
     * @throws ResourceNotFoundException If any product cannot be found
     * @throws SQLException If database errors occur during enrichment
     */
    Cart enrichCartWithProductMetadata(Cart cart) throws ResourceNotFoundException, SQLException;
    
    /**
     * Validates product availability and stock levels for all cart items.
     * Checks if products still exist and have sufficient stock.
     * 
     * @param cart The cart to validate for stock availability
     * @return true if all products have sufficient stock
     * @throws SQLException If database errors occur during validation
     */
    boolean validateStockAvailability(Cart cart) throws SQLException;
    
    /**
     * Validates cart data integrity including proper relationships and
     * consistent data state across all cart items.
     * 
     * @param cart The cart to validate for data integrity
     * @return true if cart data integrity is valid
     * @throws SQLException If database errors occur during validation
     */
    boolean validateCartDataIntegrity(Cart cart) throws SQLException;
    
    /**
     * Validates that all required product metadata is present for order creation.
     * Ensures images, descriptions, categories, and pricing are complete.
     * 
     * @param cart The cart to validate for metadata completeness
     * @return true if all required metadata is present
     * @throws SQLException If database errors occur during validation
     */
    boolean validateProductMetadataCompleteness(Cart cart) throws SQLException;
}