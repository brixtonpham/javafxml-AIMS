package com.aims.core.application.services;

import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.OrderItem;
import com.aims.core.shared.exceptions.InventoryException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service interface for real-time stock validation and inventory checking.
 * Provides comprehensive stock validation capabilities to prevent overselling
 * and ensure inventory integrity during the checkout process.
 */
public interface IStockValidationService {
    
    /**
     * Validates stock availability for a single product.
     * Checks both actual stock and reserved quantities.
     * 
     * @param productId Product to validate stock for
     * @param requestedQuantity Quantity being requested
     * @return StockValidationResult with validation outcome
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Product not found
     */
    StockValidationResult validateProductStock(String productId, int requestedQuantity) 
            throws SQLException, ResourceNotFoundException;
    
    /**
     * Validates stock availability for multiple products at once.
     * Optimized for bulk validation scenarios like cart checkout.
     * 
     * @param items List of items to validate
     * @return BulkStockValidationResult with detailed validation outcomes
     * @throws SQLException Database error
     */
    BulkStockValidationResult validateBulkStock(List<CartItem> items) 
            throws SQLException;
    
    /**
     * Validates stock for an entire cart before checkout.
     * Comprehensive validation including stock availability and constraints.
     * 
     * @param cart Cart to validate
     * @return CartStockValidationResult with complete validation details
     * @throws SQLException Database error
     * @throws ValidationException Invalid cart data
     */
    CartStockValidationResult validateCartStock(Cart cart) 
            throws SQLException, ValidationException;
    
    /**
     * Validates stock for order items during order processing.
     * Used when converting cart to order or modifying existing orders.
     * 
     * @param orderItems List of order items to validate
     * @return BulkStockValidationResult with validation outcomes
     * @throws SQLException Database error
     */
    BulkStockValidationResult validateOrderItemsStock(List<OrderItem> orderItems) 
            throws SQLException;
    
    /**
     * Gets real-time stock information for a product.
     * Includes actual stock, reserved quantities, and available stock.
     * 
     * @param productId Product to get stock information for
     * @return StockInfo with detailed stock information
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Product not found
     */
    StockInfo getStockInfo(String productId) 
            throws SQLException, ResourceNotFoundException;
    
    /**
     * Gets real-time stock information for multiple products.
     * Optimized for bulk stock information retrieval.
     * 
     * @param productIds List of product IDs to get stock information for
     * @return Map of product ID to StockInfo
     * @throws SQLException Database error
     */
    Map<String, StockInfo> getBulkStockInfo(List<String> productIds) 
            throws SQLException;
    
    /**
     * Generates insufficient stock notifications for failed validations.
     * Creates detailed messages for UI display.
     * 
     * @param validationResult Failed validation result
     * @return InsufficientStockNotification with user-friendly messages
     */
    InsufficientStockNotification generateInsufficientStockNotification(
            BulkStockValidationResult validationResult);
    
    /**
     * Checks if stock levels are critically low for monitoring purposes.
     * Used for inventory management and restocking alerts.
     * 
     * @param productId Product to check
     * @param threshold Critical stock threshold
     * @return true if stock is below threshold
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Product not found
     */
    boolean isStockCriticallyLow(String productId, int threshold) 
            throws SQLException, ResourceNotFoundException;
    
    /**
     * Represents the result of stock validation for a single product.
     */
    public static class StockValidationResult {
        private final boolean valid;
        private final String productId;
        private final String productTitle;
        private final int requestedQuantity;
        private final int actualStock;
        private final int reservedStock;
        private final int availableStock;
        private final String message;
        private final String reasonCode;
        
        public StockValidationResult(boolean valid, String productId, String productTitle,
                                   int requestedQuantity, int actualStock, int reservedStock,
                                   int availableStock, String message, String reasonCode) {
            this.valid = valid;
            this.productId = productId;
            this.productTitle = productTitle;
            this.requestedQuantity = requestedQuantity;
            this.actualStock = actualStock;
            this.reservedStock = reservedStock;
            this.availableStock = availableStock;
            this.message = message;
            this.reasonCode = reasonCode;
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public String getProductId() { return productId; }
        public String getProductTitle() { return productTitle; }
        public int getRequestedQuantity() { return requestedQuantity; }
        public int getActualStock() { return actualStock; }
        public int getReservedStock() { return reservedStock; }
        public int getAvailableStock() { return availableStock; }
        public String getMessage() { return message; }
        public String getValidationMessage() { return message; } // Alias for compatibility
        public String getReasonCode() { return reasonCode; }
        public int getShortfallQuantity() {
            return Math.max(0, requestedQuantity - availableStock);
        }
    }
    
    /**
     * Represents the result of bulk stock validation for multiple products.
     */
    public static class BulkStockValidationResult {
        private final boolean allValid;
        private final List<StockValidationResult> individualResults;
        private final List<StockValidationResult> failedValidations;
        private final int totalProductsChecked;
        private final int totalFailedProducts;
        private final String overallMessage;
        
        public BulkStockValidationResult(boolean allValid, List<StockValidationResult> individualResults,
                                       List<StockValidationResult> failedValidations, 
                                       int totalProductsChecked, int totalFailedProducts,
                                       String overallMessage) {
            this.allValid = allValid;
            this.individualResults = individualResults;
            this.failedValidations = failedValidations;
            this.totalProductsChecked = totalProductsChecked;
            this.totalFailedProducts = totalFailedProducts;
            this.overallMessage = overallMessage;
        }
        
        // Getters
        public boolean isAllValid() { return allValid; }
        public List<StockValidationResult> getIndividualResults() { return individualResults; }
        public List<StockValidationResult> getFailedValidations() { return failedValidations; }
        public int getTotalProductsChecked() { return totalProductsChecked; }
        public int getTotalFailedProducts() { return totalFailedProducts; }
        public String getOverallMessage() { return overallMessage; }
        public boolean hasFailures() { return !failedValidations.isEmpty(); }
    }
    
    /**
     * Represents the result of cart stock validation with additional cart-specific information.
     */
    public static class CartStockValidationResult {
        private final boolean valid;
        private final String cartSessionId;
        private final BulkStockValidationResult bulkValidationResult;
        private final float totalCartValue;
        private final int totalItemCount;
        private final String cartValidationMessage;
        
        public CartStockValidationResult(boolean valid, String cartSessionId,
                                       BulkStockValidationResult bulkValidationResult,
                                       float totalCartValue, int totalItemCount,
                                       String cartValidationMessage) {
            this.valid = valid;
            this.cartSessionId = cartSessionId;
            this.bulkValidationResult = bulkValidationResult;
            this.totalCartValue = totalCartValue;
            this.totalItemCount = totalItemCount;
            this.cartValidationMessage = cartValidationMessage;
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public String getCartSessionId() { return cartSessionId; }
        public BulkStockValidationResult getBulkValidationResult() { return bulkValidationResult; }
        public float getTotalCartValue() { return totalCartValue; }
        public int getTotalItemCount() { return totalItemCount; }
        public String getCartValidationMessage() { return cartValidationMessage; }
    }
    
    /**
     * Represents detailed stock information for a product.
     */
    public static class StockInfo {
        private final String productId;
        private final String productTitle;
        private final int actualStock;
        private final int reservedStock;
        private final int availableStock;
        private final boolean inStock;
        private final boolean lowStock;
        private final int lowStockThreshold;
        
        public StockInfo(String productId, String productTitle, int actualStock,
                        int reservedStock, int availableStock, boolean inStock,
                        boolean lowStock, int lowStockThreshold) {
            this.productId = productId;
            this.productTitle = productTitle;
            this.actualStock = actualStock;
            this.reservedStock = reservedStock;
            this.availableStock = availableStock;
            this.inStock = inStock;
            this.lowStock = lowStock;
            this.lowStockThreshold = lowStockThreshold;
        }
        
        // Getters
        public String getProductId() { return productId; }
        public String getProductTitle() { return productTitle; }
        public int getActualStock() { return actualStock; }
        public int getReservedStock() { return reservedStock; }
        public int getAvailableStock() { return availableStock; }
        public boolean isInStock() { return inStock; }
        public boolean isLowStock() { return lowStock; }
        public int getLowStockThreshold() { return lowStockThreshold; }
    }
    
    /**
     * Represents insufficient stock notifications for UI display.
     */
    public static class InsufficientStockNotification {
        private final String title;
        private final String message;
        private final List<String> productMessages;
        private final Map<String, String> suggestedActions;
        private final boolean canProceedWithAvailableStock;
        
        public InsufficientStockNotification(String title, String message,
                                           List<String> productMessages,
                                           Map<String, String> suggestedActions,
                                           boolean canProceedWithAvailableStock) {
            this.title = title;
            this.message = message;
            this.productMessages = productMessages;
            this.suggestedActions = suggestedActions;
            this.canProceedWithAvailableStock = canProceedWithAvailableStock;
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public List<String> getProductMessages() { return productMessages; }
        public Map<String, String> getSuggestedActions() { return suggestedActions; }
        public boolean canProceedWithAvailableStock() { return canProceedWithAvailableStock; }
    }
}