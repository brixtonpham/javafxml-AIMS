package com.aims.core.presentation.utils;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to debounce product refresh operations and prevent excessive database calls.
 * Useful when multiple UI components might trigger product refreshes simultaneously.
 * 
 * This addresses performance concerns in the stock availability persistence fix by
 * preventing rapid-fire database queries when multiple components request updates.
 */
public class DebouncedProductRefresh {
    
    private static final Map<String, Timer> refreshTimers = new ConcurrentHashMap<>();
    private static final int REFRESH_DELAY_MS = 300; // 300ms delay to batch requests
    
    /**
     * Schedule a debounced refresh for a specific product
     * @param productId The product ID to refresh
     * @param refreshAction The action to execute after the delay
     */
    public static void scheduleRefresh(String productId, Runnable refreshAction) {
        if (productId == null || refreshAction == null) {
            return;
        }
        
        // Cancel any existing timer for this product
        Timer existingTimer = refreshTimers.get(productId);
        if (existingTimer != null) {
            existingTimer.cancel();
            System.out.println("DebouncedProductRefresh.scheduleRefresh: Cancelled existing timer for " + productId);
        }
        
        // Schedule new refresh
        Timer newTimer = new Timer();
        newTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    System.out.println("DebouncedProductRefresh: Executing refresh for " + productId);
                    refreshAction.run();
                } catch (Exception e) {
                    System.err.println("Error in debounced product refresh for " + productId + ": " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    refreshTimers.remove(productId);
                }
            }
        }, REFRESH_DELAY_MS);
        
        refreshTimers.put(productId, newTimer);
        System.out.println("DebouncedProductRefresh.scheduleRefresh: Scheduled refresh for " + productId + " in " + REFRESH_DELAY_MS + "ms");
    }
    
    /**
     * Schedule a debounced refresh with custom delay
     * @param productId The product ID to refresh
     * @param refreshAction The action to execute after the delay
     * @param delayMs Custom delay in milliseconds
     */
    public static void scheduleRefresh(String productId, Runnable refreshAction, int delayMs) {
        if (productId == null || refreshAction == null) {
            return;
        }
        
        // Cancel any existing timer for this product
        Timer existingTimer = refreshTimers.get(productId);
        if (existingTimer != null) {
            existingTimer.cancel();
        }
        
        // Schedule new refresh with custom delay
        Timer newTimer = new Timer();
        newTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    refreshAction.run();
                } catch (Exception e) {
                    System.err.println("Error in debounced product refresh for " + productId + ": " + e.getMessage());
                } finally {
                    refreshTimers.remove(productId);
                }
            }
        }, delayMs);
        
        refreshTimers.put(productId, newTimer);
    }
    
    /**
     * Cancel any pending refresh for a specific product
     * @param productId The product ID
     */
    public static void cancelRefresh(String productId) {
        Timer timer = refreshTimers.remove(productId);
        if (timer != null) {
            timer.cancel();
            System.out.println("DebouncedProductRefresh.cancelRefresh: Cancelled refresh for " + productId);
        }
    }
    
    /**
     * Cancel all pending refreshes
     */
    public static void cancelAllRefreshes() {
        for (Map.Entry<String, Timer> entry : refreshTimers.entrySet()) {
            entry.getValue().cancel();
        }
        int cancelled = refreshTimers.size();
        refreshTimers.clear();
        System.out.println("DebouncedProductRefresh.cancelAllRefreshes: Cancelled " + cancelled + " pending refreshes");
    }
    
    /**
     * Check if a refresh is currently pending for a product
     * @param productId The product ID to check
     * @return true if a refresh is pending, false otherwise
     */
    public static boolean isPendingRefresh(String productId) {
        return refreshTimers.containsKey(productId);
    }
    
    /**
     * Get the number of currently pending refreshes
     * @return Number of pending refreshes
     */
    public static int getPendingRefreshCount() {
        return refreshTimers.size();
    }
    
    /**
     * Execute a refresh immediately, cancelling any pending debounced refresh
     * @param productId The product ID to refresh
     * @param refreshAction The action to execute immediately
     */
    public static void executeImmediateRefresh(String productId, Runnable refreshAction) {
        if (productId == null || refreshAction == null) {
            return;
        }
        
        // Cancel any pending refresh
        cancelRefresh(productId);
        
        // Execute immediately
        try {
            System.out.println("DebouncedProductRefresh.executeImmediateRefresh: Executing immediate refresh for " + productId);
            refreshAction.run();
        } catch (Exception e) {
            System.err.println("Error in immediate product refresh for " + productId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}