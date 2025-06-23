package com.aims.core.application.impl;

import com.aims.core.application.services.IStockReservationService;
import com.aims.core.application.services.IStockValidationService;
import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.shared.exceptions.InventoryException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of stock validation service for real-time inventory checking
 * and overselling prevention. Integrates with stock reservation service for
 * comprehensive stock management during checkout processes.
 */
@Service
public class StockValidationServiceImpl implements IStockValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(StockValidationServiceImpl.class);
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
    
    private final IProductDAO productDAO;
    private final IStockReservationService stockReservationService;
    
    public StockValidationServiceImpl(IProductDAO productDAO, 
                                    IStockReservationService stockReservationService) {
        this.productDAO = productDAO;
        this.stockReservationService = stockReservationService;
    }
    
    @Override
    public StockValidationResult validateProductStock(String productId, int requestedQuantity)
            throws SQLException, ResourceNotFoundException {
        
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive");
        }
        
        logger.debug("Validating stock for product {} with quantity {}", productId, requestedQuantity);
        
        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found");
        }
        
        int actualStock = product.getQuantityInStock();
        int availableStock = stockReservationService.getAvailableStock(productId);
        int reservedStock = actualStock - availableStock;
        
        boolean isValid = availableStock >= requestedQuantity;
        String message;
        String reasonCode;
        
        if (isValid) {
            message = String.format("Stock validation passed for %s", product.getTitle());
            reasonCode = "STOCK_AVAILABLE";
            logger.debug("Stock validation passed for product {}: requested={}, available={}", 
                        productId, requestedQuantity, availableStock);
        } else {
            message = String.format("Insufficient stock for %s. Requested: %d, Available: %d", 
                                  product.getTitle(), requestedQuantity, availableStock);
            reasonCode = "INSUFFICIENT_STOCK";
            logger.warn("Stock validation failed for product {}: requested={}, available={}", 
                       productId, requestedQuantity, availableStock);
        }
        
        return new StockValidationResult(
            isValid, productId, product.getTitle(), requestedQuantity, 
            actualStock, reservedStock, availableStock, message, reasonCode
        );
    }
    
    @Override
    public BulkStockValidationResult validateBulkStock(List<CartItem> items) 
            throws SQLException {
        
        if (items == null || items.isEmpty()) {
            return new BulkStockValidationResult(
                true, new ArrayList<>(), new ArrayList<>(), 0, 0,
                "No items to validate"
            );
        }
        
        logger.info("Validating bulk stock for {} items", items.size());
        
        List<StockValidationResult> individualResults = new ArrayList<>();
        List<StockValidationResult> failedValidations = new ArrayList<>();
        
        for (CartItem item : items) {
            try {
                String productId = item.getProduct().getProductId();
                int quantity = item.getQuantity();
                
                StockValidationResult result = validateProductStock(productId, quantity);
                individualResults.add(result);
                
                if (!result.isValid()) {
                    failedValidations.add(result);
                }
                
            } catch (ResourceNotFoundException e) {
                logger.error("Product not found during bulk validation: {}", e.getMessage());
                StockValidationResult errorResult = new StockValidationResult(
                    false, item.getProduct().getProductId(), 
                    item.getProduct().getTitle(), item.getQuantity(),
                    0, 0, 0, "Product not found: " + e.getMessage(), "PRODUCT_NOT_FOUND"
                );
                individualResults.add(errorResult);
                failedValidations.add(errorResult);
            }
        }
        
        boolean allValid = failedValidations.isEmpty();
        String overallMessage = allValid 
            ? "All items passed stock validation"
            : String.format("%d out of %d items failed stock validation", 
                           failedValidations.size(), items.size());
        
        logger.info("Bulk stock validation completed: {} items checked, {} failed", 
                   items.size(), failedValidations.size());
        
        return new BulkStockValidationResult(
            allValid, individualResults, failedValidations, 
            items.size(), failedValidations.size(), overallMessage
        );
    }
    
    @Override
    public CartStockValidationResult validateCartStock(Cart cart) 
            throws SQLException, ValidationException {
        
        if (cart == null) {
            throw new ValidationException("Cart cannot be null");
        }
        
        String cartSessionId = cart.getCartSessionId();
        List<CartItem> items = cart.getItems();
        
        logger.info("Validating stock for cart {} with {} items", cartSessionId, items.size());
        
        BulkStockValidationResult bulkResult = validateBulkStock(items);
        
        // Calculate cart totals
        float totalCartValue = (float) items.stream()
            .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
            .sum();
        
        int totalItemCount = items.stream()
            .mapToInt(CartItem::getQuantity)
            .sum();
        
        boolean isValid = bulkResult.isAllValid();
        String cartValidationMessage = isValid 
            ? String.format("Cart validation passed: %d items, total value: %.2f VND", 
                           totalItemCount, totalCartValue)
            : String.format("Cart validation failed: %d items have stock issues", 
                           bulkResult.getTotalFailedProducts());
        
        logger.info("Cart validation completed for {}: valid={}, items={}, value={}", 
                   cartSessionId, isValid, totalItemCount, totalCartValue);
        
        return new CartStockValidationResult(
            isValid, cartSessionId, bulkResult, totalCartValue, 
            totalItemCount, cartValidationMessage
        );
    }
    
    @Override
    public BulkStockValidationResult validateOrderItemsStock(List<OrderItem> orderItems) 
            throws SQLException {
        
        if (orderItems == null || orderItems.isEmpty()) {
            return new BulkStockValidationResult(
                true, new ArrayList<>(), new ArrayList<>(), 0, 0,
                "No order items to validate"
            );
        }
        
        logger.info("Validating stock for {} order items", orderItems.size());
        
        // Convert OrderItems to CartItems for validation
        List<CartItem> cartItems = orderItems.stream()
            .map(orderItem -> {
                CartItem cartItem = new CartItem();
                cartItem.setProduct(orderItem.getProduct());
                cartItem.setQuantity(orderItem.getQuantity());
                return cartItem;
            })
            .collect(Collectors.toList());
        
        return validateBulkStock(cartItems);
    }
    
    @Override
    public StockInfo getStockInfo(String productId)
            throws SQLException, ResourceNotFoundException {
        
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        
        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found");
        }
        
        int actualStock = product.getQuantityInStock();
        int availableStock = stockReservationService.getAvailableStock(productId);
        int reservedStock = actualStock - availableStock;
        
        boolean inStock = availableStock > 0;
        boolean lowStock = actualStock <= DEFAULT_LOW_STOCK_THRESHOLD;
        
        logger.debug("Retrieved stock info for product {}: actual={}, available={}, reserved={}", 
                    productId, actualStock, availableStock, reservedStock);
        
        return new StockInfo(
            productId, product.getTitle(), actualStock, reservedStock, 
            availableStock, inStock, lowStock, DEFAULT_LOW_STOCK_THRESHOLD
        );
    }
    
    @Override
    public Map<String, StockInfo> getBulkStockInfo(List<String> productIds) 
            throws SQLException {
        
        if (productIds == null || productIds.isEmpty()) {
            return new HashMap<>();
        }
        
        logger.debug("Retrieving bulk stock info for {} products", productIds.size());
        
        Map<String, StockInfo> stockInfoMap = new HashMap<>();
        
        for (String productId : productIds) {
            try {
                StockInfo stockInfo = getStockInfo(productId);
                stockInfoMap.put(productId, stockInfo);
            } catch (ResourceNotFoundException e) {
                logger.warn("Product {} not found during bulk stock info retrieval", productId);
                // Create empty stock info for missing products
                StockInfo emptyStockInfo = new StockInfo(
                    productId, "Product Not Found", 0, 0, 0, false, false, 0
                );
                stockInfoMap.put(productId, emptyStockInfo);
            }
        }
        
        return stockInfoMap;
    }
    
    @Override
    public InsufficientStockNotification generateInsufficientStockNotification(
            BulkStockValidationResult validationResult) {
        
        if (validationResult.isAllValid()) {
            return new InsufficientStockNotification(
                "Stock Validation Passed",
                "All items are available in sufficient quantities.",
                new ArrayList<>(),
                new HashMap<>(),
                true
            );
        }
        
        List<String> productMessages = new ArrayList<>();
        Map<String, String> suggestedActions = new HashMap<>();
        boolean canProceedWithAvailableStock = true;
        
        for (StockValidationResult failedResult : validationResult.getFailedValidations()) {
            String productMessage = String.format(
                "%s: Requested %d, but only %d available (shortfall: %d)",
                failedResult.getProductTitle(),
                failedResult.getRequestedQuantity(),
                failedResult.getAvailableStock(),
                failedResult.getShortfallQuantity()
            );
            productMessages.add(productMessage);
            
            if (failedResult.getAvailableStock() == 0) {
                canProceedWithAvailableStock = false;
                suggestedActions.put(failedResult.getProductId(), "Remove from cart - out of stock");
            } else {
                suggestedActions.put(failedResult.getProductId(), 
                    String.format("Reduce quantity to %d or less", failedResult.getAvailableStock()));
            }
        }
        
        String title = "Insufficient Stock";
        String message = String.format(
            "%d product(s) in your cart have insufficient stock. Please review and adjust quantities.",
            validationResult.getTotalFailedProducts()
        );
        
        logger.info("Generated insufficient stock notification for {} failed products", 
                   validationResult.getTotalFailedProducts());
        
        return new InsufficientStockNotification(
            title, message, productMessages, suggestedActions, canProceedWithAvailableStock
        );
    }
    
    @Override
    public boolean isStockCriticallyLow(String productId, int threshold)
            throws SQLException, ResourceNotFoundException {
        
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        if (threshold < 0) {
            threshold = DEFAULT_LOW_STOCK_THRESHOLD;
        }
        
        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found");
        }
        
        int actualStock = product.getQuantityInStock();
        boolean isCriticallyLow = actualStock <= threshold;
        
        if (isCriticallyLow) {
            logger.warn("Product {} has critically low stock: {} (threshold: {})", 
                       productId, actualStock, threshold);
        }
        
        return isCriticallyLow;
    }
    
    /**
     * Validates stock constraints for special product types.
     * Can be extended for product-specific validation rules.
     */
    private boolean validateProductConstraints(Product product, int requestedQuantity) {
        // Basic constraint validation
        if (product.getQuantityInStock() < 0) {
            logger.error("Product {} has negative stock: {}", 
                        product.getProductId(), product.getQuantityInStock());
            return false;
        }
        
        // Can be extended for other constraints like max order quantity per customer
        return true;
    }
    
    /**
     * Logs detailed stock validation metrics for monitoring.
     */
    private void logStockValidationMetrics(BulkStockValidationResult result) {
        if (logger.isInfoEnabled()) {
            logger.info("Stock Validation Metrics - Total: {}, Failed: {}, Success Rate: {:.2f}%",
                       result.getTotalProductsChecked(),
                       result.getTotalFailedProducts(),
                       ((double)(result.getTotalProductsChecked() - result.getTotalFailedProducts()) / 
                        result.getTotalProductsChecked()) * 100);
        }
    }
}