package com.aims.core.presentation.utils;

import com.aims.core.presentation.controllers.MainLayoutController;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Thread-safe registry for MainLayoutController with timeout-based waiting and validation.
 * 
 * This registry solves the critical MainLayoutController null reference issue by providing:
 * - Thread-safe access to the MainLayoutController instance
 * - Timeout-based waiting for initialization
 * - Validation of controller state and functionality
 * - Automatic notification of waiting components
 * - Comprehensive error handling and logging
 * 
 * @author AIMS Navigation Enhancement Team
 * @version 1.0
 * @since Phase 1 - MainLayoutController Fix
 */
public class MainLayoutControllerRegistry {
    
    private static final Logger logger = Logger.getLogger(MainLayoutControllerRegistry.class.getName());
    
    // Thread-safe reference to the MainLayoutController
    private static final AtomicReference<MainLayoutController> instance = new AtomicReference<>();
    
    // Synchronization for waiting threads
    private static final CountDownLatch initializationLatch = new CountDownLatch(1);
    
    // Configuration
    private static final long DEFAULT_TIMEOUT_SECONDS = 10;
    private static final long VALIDATION_TIMEOUT_SECONDS = 5;
    
    // State tracking
    private static volatile boolean isInitialized = false;
    private static volatile boolean isValidated = false;
    private static volatile long initializationTime = 0;
    private static volatile Exception lastValidationError = null;
    
    /**
     * Sets the MainLayoutController instance and notifies waiting threads.
     * This should be called once during application startup.
     * 
     * @param controller The MainLayoutController instance to register
     * @throws IllegalStateException if controller is already set or if controller is invalid
     */
    public static void setInstance(MainLayoutController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("MainLayoutController cannot be null");
        }
        
        // Check if already initialized
        if (isInitialized) {
            logger.warning("MainLayoutControllerRegistry.setInstance: Controller already initialized, ignoring duplicate call");
            return;
        }
        
        synchronized (MainLayoutControllerRegistry.class) {
            // Double-check locking pattern
            if (isInitialized) {
                logger.warning("MainLayoutControllerRegistry.setInstance: Controller already initialized during synchronization");
                return;
            }
            
            try {
                // Validate the controller before setting
                if (validateController(controller)) {
                    instance.set(controller);
                    isInitialized = true;
                    isValidated = true;
                    initializationTime = System.currentTimeMillis();
                    
                    // Notify all waiting threads
                    initializationLatch.countDown();
                    
                    logger.info("MainLayoutControllerRegistry.setInstance: MainLayoutController registered successfully");
                } else {
                    throw new IllegalStateException("MainLayoutController validation failed: " + 
                        (lastValidationError != null ? lastValidationError.getMessage() : "Unknown validation error"));
                }
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "MainLayoutControllerRegistry.setInstance: Failed to register MainLayoutController", e);
                throw new IllegalStateException("Failed to register MainLayoutController", e);
            }
        }
    }
    
    /**
     * Gets the MainLayoutController instance with default timeout.
     * 
     * @return The MainLayoutController instance, or null if not available within timeout
     */
    public static MainLayoutController getInstance() {
        return getInstance(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
    
    /**
     * Gets the MainLayoutController instance with specified timeout.
     * 
     * @param timeout The maximum time to wait for initialization
     * @param unit The time unit of the timeout argument
     * @return The MainLayoutController instance, or null if not available within timeout
     */
    public static MainLayoutController getInstance(long timeout, TimeUnit unit) {
        try {
            // If already initialized, return immediately
            if (isInitialized && isValidated) {
                MainLayoutController controller = instance.get();
                if (controller != null && validateControllerQuick(controller)) {
                    return controller;
                } else {
                    logger.warning("MainLayoutControllerRegistry.getInstance: Controller validation failed, marking as invalid");
                    markAsInvalid();
                }
            }
            
            // Wait for initialization if not yet available
            if (!isInitialized) {
                logger.info("MainLayoutControllerRegistry.getInstance: Waiting for MainLayoutController initialization...");
                
                boolean initialized = initializationLatch.await(timeout, unit);
                if (!initialized) {
                    logger.warning("MainLayoutControllerRegistry.getInstance: Timeout waiting for MainLayoutController initialization");
                    return null;
                }
            }
            
            // Return the controller if available and valid
            MainLayoutController controller = instance.get();
            if (controller != null && (isValidated || validateController(controller))) {
                return controller;
            } else {
                logger.warning("MainLayoutControllerRegistry.getInstance: Controller is null or validation failed");
                return null;
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "MainLayoutControllerRegistry.getInstance: Interrupted while waiting for controller", e);
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "MainLayoutControllerRegistry.getInstance: Unexpected error getting controller", e);
            return null;
        }
    }
    
    /**
     * Gets the MainLayoutController instance immediately without waiting.
     * 
     * @return The MainLayoutController instance, or null if not available
     */
    public static MainLayoutController getInstanceImmediate() {
        if (isInitialized && isValidated) {
            return instance.get();
        }
        return null;
    }
    
    /**
     * Checks if the MainLayoutController is available and valid.
     * 
     * @return true if controller is available and valid, false otherwise
     */
    public static boolean isAvailable() {
        return isInitialized && isValidated && instance.get() != null;
    }
    
    /**
     * Validates the MainLayoutController for proper functionality.
     * 
     * @param controller The controller to validate
     * @return true if controller is valid, false otherwise
     */
    private static boolean validateController(MainLayoutController controller) {
        try {
            if (controller == null) {
                lastValidationError = new IllegalArgumentException("Controller is null");
                return false;
            }
            
            // Check essential components
            if (controller.getContentPane() == null) {
                lastValidationError = new IllegalStateException("Controller content pane is null");
                return false;
            }
            
            if (controller.getMainContainer() == null) {
                lastValidationError = new IllegalStateException("Controller main container is null");
                return false;
            }
            
            // Additional validation can be added here
            // For example, checking if controller is properly initialized
            
            lastValidationError = null;
            logger.fine("MainLayoutControllerRegistry.validateController: Controller validation passed");
            return true;
            
        } catch (Exception e) {
            lastValidationError = e;
            logger.log(Level.WARNING, "MainLayoutControllerRegistry.validateController: Controller validation failed", e);
            return false;
        }
    }
    
    /**
     * Quick validation without extensive checks (for performance).
     * 
     * @param controller The controller to validate
     * @return true if controller appears valid, false otherwise
     */
    private static boolean validateControllerQuick(MainLayoutController controller) {
        try {
            return controller != null && controller.getContentPane() != null;
        } catch (Exception e) {
            logger.log(Level.FINE, "MainLayoutControllerRegistry.validateControllerQuick: Quick validation failed", e);
            return false;
        }
    }
    
    /**
     * Marks the controller as invalid and triggers re-validation on next access.
     */
    private static void markAsInvalid() {
        isValidated = false;
        logger.warning("MainLayoutControllerRegistry.markAsInvalid: Controller marked as invalid");
    }
    
    /**
     * Forces re-validation of the current controller.
     * 
     * @return true if controller is valid after re-validation, false otherwise
     */
    public static boolean revalidate() {
        MainLayoutController controller = instance.get();
        if (controller != null) {
            boolean valid = validateController(controller);
            isValidated = valid;
            logger.info("MainLayoutControllerRegistry.revalidate: Controller re-validation result: " + valid);
            return valid;
        } else {
            isValidated = false;
            logger.warning("MainLayoutControllerRegistry.revalidate: No controller to re-validate");
            return false;
        }
    }
    
    /**
     * Resets the registry state (for testing or recovery purposes).
     * This should only be used in exceptional circumstances.
     */
    public static void reset() {
        synchronized (MainLayoutControllerRegistry.class) {
            instance.set(null);
            isInitialized = false;
            isValidated = false;
            initializationTime = 0;
            lastValidationError = null;
            logger.warning("MainLayoutControllerRegistry.reset: Registry state has been reset");
        }
    }
    
    /**
     * Gets comprehensive debug information about the registry state.
     * 
     * @return String containing detailed registry state information
     */
    public static String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("MainLayoutControllerRegistry Debug Info:\n");
        info.append("Initialized: ").append(isInitialized).append("\n");
        info.append("Validated: ").append(isValidated).append("\n");
        info.append("Controller Instance: ").append(instance.get() != null ? "available" : "null").append("\n");
        info.append("Initialization Time: ").append(initializationTime > 0 ? 
            new java.util.Date(initializationTime).toString() : "not initialized").append("\n");
        info.append("Last Validation Error: ").append(lastValidationError != null ? 
            lastValidationError.getMessage() : "none").append("\n");
        info.append("Latch Count: ").append(initializationLatch.getCount()).append("\n");
        return info.toString();
    }
    
    /**
     * Gets the initialization time of the controller.
     * 
     * @return The initialization time in milliseconds, or 0 if not initialized
     */
    public static long getInitializationTime() {
        return initializationTime;
    }
    
    /**
     * Gets the last validation error (if any).
     * 
     * @return The last validation error, or null if no error
     */
    public static Exception getLastValidationError() {
        return lastValidationError;
    }
    
    /**
     * Checks if the controller was initialized within the specified time frame.
     * 
     * @param maxAgeMillis Maximum age in milliseconds
     * @return true if controller is recent enough, false otherwise
     */
    public static boolean isRecentlyInitialized(long maxAgeMillis) {
        if (initializationTime == 0) {
            return false;
        }
        return (System.currentTimeMillis() - initializationTime) <= maxAgeMillis;
    }
}