package com.aims.core.presentation.utils;

/**
 * Centralized cart session management utility.
 * Ensures consistent cart session IDs across all UI components.
 * This provides a temporary solution until a proper application-wide session manager is implemented.
 */
public class CartSessionManager {
    
    // CRITICAL FIX: Make session ID thread-safe with volatile keyword
    private static volatile String guestCartSessionId = null;
    
    // CRITICAL FIX: Add synchronization lock for thread-safe operations
    private static final Object SESSION_LOCK = new Object();
    
    /**
     * Gets or creates a persistent cart session ID.
     * This ensures cart continuity across all UI interactions.
     * CRITICAL FIX: Thread-safe implementation to prevent race conditions.
     * @return A persistent cart session ID
     */
    public static String getOrCreateCartSessionId() {
        // Double-checked locking pattern for thread safety
        if (guestCartSessionId == null) {
            synchronized (SESSION_LOCK) {
                if (guestCartSessionId == null) {
                    guestCartSessionId = "guest_cart_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
                    System.out.println("CartSessionManager.getOrCreateCartSessionId: Created new cart session ID: " + guestCartSessionId);
                }
            }
        } else {
            System.out.println("CartSessionManager.getOrCreateCartSessionId: Using existing cart session ID: " + guestCartSessionId);
        }
        return guestCartSessionId;
    }
    
    /**
     * Gets the current cart session ID without creating a new one.
     * @return The current cart session ID, or null if none exists
     */
    public static String getCurrentCartSessionId() {
        return guestCartSessionId;
    }
    
    /**
     * Resets the cart session (for testing or logout scenarios).
     * This method can be called when a user logs out or when starting a fresh session.
     * CRITICAL FIX: Thread-safe implementation.
     */
    public static void resetCartSession() {
        synchronized (SESSION_LOCK) {
            guestCartSessionId = null;
            System.out.println("CartSessionManager.resetCartSession: Cart session reset");
        }
    }
    
    /**
     * Sets a specific cart session ID (useful for user login scenarios).
     * CRITICAL FIX: Thread-safe implementation.
     * @param sessionId The session ID to set
     */
    public static void setCartSessionId(String sessionId) {
        synchronized (SESSION_LOCK) {
            guestCartSessionId = sessionId;
            System.out.println("CartSessionManager.setCartSessionId: Cart session ID set to: " + sessionId);
        }
    }
    
    /**
     * Checks if a cart session is currently active.
     * @return true if a cart session exists, false otherwise
     */
    public static boolean hasActiveSession() {
        return guestCartSessionId != null && !guestCartSessionId.trim().isEmpty();
    }
}