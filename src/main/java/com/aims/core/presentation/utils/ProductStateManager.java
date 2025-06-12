package com.aims.core.presentation.utils;

import com.aims.core.entities.Product;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralized product state manager to ensure consistency across UI components.
 * Implements observer pattern for real-time product updates.
 * 
 * This class addresses the stock availability persistence issue by providing
 * a single source of truth for product state across all UI components.
 */
public class ProductStateManager {
    
    private static final Map<String, Product> productCache = new ConcurrentHashMap<>();
    private static final List<ProductStateListener> listeners = new CopyOnWriteArrayList<>();
    private static final Object LOCK = new Object();
    
    public interface ProductStateListener {
        /**
         * Called when a product's state has been updated
         * @param product The updated product
         */
        void onProductUpdated(Product product);
        
        /**
         * Returns the product ID this listener is interested in
         * @return Product ID or null for all products
         */
        default String getInterestedProductId() {
            return null; // Listen to all products by default
        }
    }
    
    /**
     * Get cached product by ID
     */
    public static Product getProduct(String productId) {
        return productCache.get(productId);
    }
    
    /**
     * Update product state and notify all interested listeners
     */
    public static void updateProduct(Product product) {
        if (product == null || product.getProductId() == null) {
            return;
        }
        
        synchronized (LOCK) {
            Product oldProduct = productCache.get(product.getProductId());
            productCache.put(product.getProductId(), product);
            
            // Only notify if there's an actual change
            if (oldProduct == null || !isSameState(oldProduct, product)) {
                System.out.println("ProductStateManager.updateProduct: State changed for " + product.getTitle() + 
                                 " - Stock: " + product.getQuantityInStock() + 
                                 " (was: " + (oldProduct != null ? oldProduct.getQuantityInStock() : "null") + ")");
                notifyListeners(product);
            }
        }
    }
    
    /**
     * Add a product state listener
     */
    public static void addListener(ProductStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            System.out.println("ProductStateManager.addListener: Added listener, total: " + listeners.size());
        }
    }
    
    /**
     * Remove a product state listener
     */
    public static void removeListener(ProductStateListener listener) {
        boolean removed = listeners.remove(listener);
        if (removed) {
            System.out.println("ProductStateManager.removeListener: Removed listener, total: " + listeners.size());
        }
    }
    
    /**
     * Clear all cached products and listeners (for testing/reset)
     */
    public static void clear() {
        synchronized (LOCK) {
            productCache.clear();
            listeners.clear();
            System.out.println("ProductStateManager.clear: Cleared all state and listeners");
        }
    }
    
    /**
     * Get the number of active listeners (for monitoring)
     */
    public static int getListenerCount() {
        return listeners.size();
    }
    
    /**
     * Get the number of cached products (for monitoring)
     */
    public static int getCachedProductCount() {
        return productCache.size();
    }
    
    /**
     * Notify listeners about product updates
     */
    private static void notifyListeners(Product product) {
        for (ProductStateListener listener : listeners) {
            String interestedId = listener.getInterestedProductId();
            if (interestedId == null || interestedId.equals(product.getProductId())) {
                // Ensure UI updates happen on JavaFX Application Thread
                Platform.runLater(() -> {
                    try {
                        listener.onProductUpdated(product);
                    } catch (Exception e) {
                        System.err.println("Error notifying product state listener: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }
    }
    
    /**
     * Check if two products have the same relevant state
     */
    private static boolean isSameState(Product oldProduct, Product newProduct) {
        return oldProduct.getQuantityInStock() == newProduct.getQuantityInStock() &&
               Float.compare(oldProduct.getPrice(), newProduct.getPrice()) == 0 &&
               oldProduct.getTitle().equals(newProduct.getTitle());
    }
    
    /**
     * Force refresh of a specific product (triggers re-fetch from database)
     */
    public static void invalidateProduct(String productId) {
        synchronized (LOCK) {
            Product removed = productCache.remove(productId);
            if (removed != null) {
                System.out.println("ProductStateManager.invalidateProduct: Invalidated " + productId);
            }
        }
    }
    
    /**
     * Check if a product is currently cached
     */
    public static boolean isProductCached(String productId) {
        return productCache.containsKey(productId);
    }
}