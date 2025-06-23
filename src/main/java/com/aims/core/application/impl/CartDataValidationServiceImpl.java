package com.aims.core.application.impl;

import com.aims.core.application.services.ICartDataValidationService;
import com.aims.core.application.dtos.CartValidationResult;
import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.Product;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * Implementation of cart data validation service for comprehensive cart-to-order
 * conversion validation with complete data preservation and integrity checking.
 */
@Service
public class CartDataValidationServiceImpl implements ICartDataValidationService {
    
    private final IProductDAO productDAO;
    
    public CartDataValidationServiceImpl(IProductDAO productDAO) {
        this.productDAO = productDAO;
    }
    
    @Override
    public CartValidationResult validateCartForOrderCreation(Cart cart) throws SQLException {
        if (cart == null) {
            return CartValidationResult.failure("Cart cannot be null");
        }
        
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return CartValidationResult.failure("Cart is empty");
        }
        
        CartValidationResult result = new CartValidationResult();
        result.setTotalItemsValidated(cart.getItems().size());
        
        System.out.println("CART VALIDATION: Starting comprehensive validation for " + 
                          cart.getItems().size() + " items");
        
        // Validate each cart item
        for (CartItem cartItem : cart.getItems()) {
            try {
                if (validateCartItemForOrderConversion(cartItem)) {
                    result.incrementValidItems();
                } else {
                    result.addValidationError("Cart item validation failed for product: " + 
                        (cartItem.getProduct() != null ? cartItem.getProduct().getProductId() : "unknown"));
                }
            } catch (SQLException e) {
                result.addValidationError("Database error validating cart item: " + e.getMessage());
            }
        }
        
        // Additional comprehensive validations
        if (!validateStockAvailability(cart)) {
            result.addValidationError("Stock availability validation failed");
        }
        
        if (!validateCartDataIntegrity(cart)) {
            result.addValidationError("Cart data integrity validation failed");
        }
        
        if (!validateProductMetadataCompleteness(cart)) {
            result.addWarning("Some products have incomplete metadata - will be enriched during conversion");
        }
        
        System.out.println("CART VALIDATION: Completed - " + result.getSummary());
        return result;
    }
    
    @Override
    public boolean validateCartItemForOrderConversion(CartItem cartItem) throws SQLException {
        if (cartItem == null) {
            System.err.println("CART ITEM VALIDATION: Cart item is null");
            return false;
        }
        
        if (cartItem.getProduct() == null) {
            System.err.println("CART ITEM VALIDATION: Product is null in cart item");
            return false;
        }
        
        if (cartItem.getQuantity() <= 0) {
            System.err.println("CART ITEM VALIDATION: Invalid quantity: " + cartItem.getQuantity());
            return false;
        }
        
        // Validate product exists and has current data
        Product currentProduct = productDAO.getById(cartItem.getProduct().getProductId());
        if (currentProduct == null) {
            System.err.println("CART ITEM VALIDATION: Product not found: " + 
                             cartItem.getProduct().getProductId());
            return false;
        }
        
        // Validate stock availability
        if (currentProduct.getQuantityInStock() < cartItem.getQuantity()) {
            System.err.println("CART ITEM VALIDATION: Insufficient stock for product: " + 
                             currentProduct.getProductId() + 
                             " (requested: " + cartItem.getQuantity() + 
                             ", available: " + currentProduct.getQuantityInStock() + ")");
            return false;
        }
        
        System.out.println("CART ITEM VALIDATION: Valid - Product: " + 
                          currentProduct.getProductId() + ", Quantity: " + cartItem.getQuantity());
        return true;
    }
    
    @Override
    public Cart enrichCartWithProductMetadata(Cart cart) throws ResourceNotFoundException, SQLException {
        if (cart == null || cart.getItems() == null) {
            throw new ResourceNotFoundException("Cart or cart items cannot be null");
        }
        
        System.out.println("CART ENRICHMENT: Starting metadata enrichment for " + 
                          cart.getItems().size() + " items");
        
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getProduct() != null) {
                // Load complete product data with all metadata
                Product completeProduct = productDAO.getById(cartItem.getProduct().getProductId());
                if (completeProduct == null) {
                    throw new ResourceNotFoundException("Product not found during enrichment: " + 
                        cartItem.getProduct().getProductId());
                }
                
                // Replace with complete product data
                cartItem.setProduct(completeProduct);
                
                System.out.println("CART ENRICHMENT: Enriched product: " + 
                                  completeProduct.getProductId() + " with complete metadata");
            }
        }
        
        System.out.println("CART ENRICHMENT: Completed for all cart items");
        return cart;
    }
    
    @Override
    public boolean validateStockAvailability(Cart cart) throws SQLException {
        if (cart == null || cart.getItems() == null) {
            return false;
        }
        
        boolean allStockAvailable = true;
        
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getProduct() != null) {
                Product currentProduct = productDAO.getById(cartItem.getProduct().getProductId());
                if (currentProduct == null || 
                    currentProduct.getQuantityInStock() < cartItem.getQuantity()) {
                    
                    System.err.println("STOCK VALIDATION: Stock issue for product: " + 
                        (currentProduct != null ? currentProduct.getProductId() : "not found") +
                        " (requested: " + cartItem.getQuantity() + 
                        ", available: " + (currentProduct != null ? currentProduct.getQuantityInStock() : 0) + ")");
                    allStockAvailable = false;
                }
            }
        }
        
        return allStockAvailable;
    }
    
    @Override
    public boolean validateCartDataIntegrity(Cart cart) throws SQLException {
        if (cart == null) {
            return false;
        }
        
        if (cart.getCartSessionId() == null || cart.getCartSessionId().trim().isEmpty()) {
            System.err.println("DATA INTEGRITY: Cart session ID is null or empty");
            return false;
        }
        
        if (cart.getItems() == null) {
            System.err.println("DATA INTEGRITY: Cart items list is null");
            return false;
        }
        
        // Validate all cart items have proper relationships
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getCart() == null || 
                !cart.getCartSessionId().equals(cartItem.getCart().getCartSessionId())) {
                System.err.println("DATA INTEGRITY: Cart item has invalid cart relationship");
                return false;
            }
            
            if (cartItem.getProduct() == null || 
                cartItem.getProduct().getProductId() == null) {
                System.err.println("DATA INTEGRITY: Cart item has invalid product relationship");
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public boolean validateProductMetadataCompleteness(Cart cart) throws SQLException {
        if (cart == null || cart.getItems() == null) {
            return false;
        }
        
        boolean allMetadataComplete = true;
        
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getProduct() != null) {
                Product product = cartItem.getProduct();
                
                // Check for essential metadata
                if (product.getTitle() == null || product.getTitle().trim().isEmpty()) {
                    System.out.println("METADATA CHECK: Missing title for product: " + product.getProductId());
                    allMetadataComplete = false;
                }
                
                if (product.getDescription() == null || product.getDescription().trim().isEmpty()) {
                    System.out.println("METADATA CHECK: Missing description for product: " + product.getProductId());
                    allMetadataComplete = false;
                }
                
                if (product.getImageUrl() == null || product.getImageUrl().trim().isEmpty()) {
                    System.out.println("METADATA CHECK: Missing image URL for product: " + product.getProductId());
                    allMetadataComplete = false;
                }
                
                if (product.getCategory() == null || product.getCategory().trim().isEmpty()) {
                    System.out.println("METADATA CHECK: Missing category for product: " + product.getProductId());
                    allMetadataComplete = false;
                }
                
                if (product.getWeightKg() <= 0) {
                    System.out.println("METADATA CHECK: Missing or invalid weight for product: " + product.getProductId());
                    allMetadataComplete = false;
                }
                
                if (product.getDimensionsCm() == null || product.getDimensionsCm().trim().isEmpty()) {
                    System.out.println("METADATA CHECK: Missing dimensions for product: " + product.getProductId());
                    allMetadataComplete = false;
                }
            }
        }
        
        return allMetadataComplete;
    }
}