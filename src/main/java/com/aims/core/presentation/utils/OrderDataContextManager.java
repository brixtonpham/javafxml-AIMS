package com.aims.core.presentation.utils;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import com.aims.core.application.services.IOrderDataLoaderService;
import com.aims.core.application.services.ICartDataValidationService;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.UUID;

/**
 * Order Data Context Manager for session-based order data persistence and cross-controller sharing.
 * 
 * This class provides comprehensive order data management during navigation transitions,
 * ensuring data integrity and preventing data loss during the checkout flow.
 * 
 * Features:
 * - Session-based order data persistence
 * - Form state preservation and recovery
 * - Data validation and completeness checks
 * - Cross-controller data sharing
 * - Automatic data cleanup and memory management
 * - Thread-safe operations for concurrent access
 * 
 * @author AIMS Navigation Enhancement Team
 * @version 1.0
 * @since Phase 1 - Navigation Enhancement
 */
public class OrderDataContextManager {
    
    private static final Logger logger = Logger.getLogger(OrderDataContextManager.class.getName());
    
    // Session storage for order data
    private static final ConcurrentMap<String, OrderDataContext> orderDataSessions = new ConcurrentHashMap<>();
    
    // Form state storage
    private static final ConcurrentMap<String, Map<String, Object>> formStateSessions = new ConcurrentHashMap<>();
    
    // Enhanced services for validation and data loading
    private static IOrderDataLoaderService orderDataLoaderService;
    private static ICartDataValidationService cartDataValidationService;
    
    // Configuration constants
    private static final long SESSION_TIMEOUT_MINUTES = 30;
    private static final int MAX_SESSIONS = 100;
    private static final String DEFAULT_SESSION_PREFIX = "AIMS_ORDER_";
    
    // Initialize services
    static {
        initializeServices();
    }
    
    /**
     * Data context wrapper for order information with metadata
     */
    public static class OrderDataContext {
        private final String sessionId;
        private final LocalDateTime createdAt;
        private LocalDateTime lastAccessed;
        private OrderEntity orderEntity;
        private ValidationStatus validationStatus;
        private Map<String, Object> metadata;
        
        public OrderDataContext(String sessionId, OrderEntity orderEntity) {
            this.sessionId = sessionId;
            this.orderEntity = orderEntity;
            this.createdAt = LocalDateTime.now();
            this.lastAccessed = LocalDateTime.now();
            this.validationStatus = ValidationStatus.UNKNOWN;
            this.metadata = new HashMap<>();
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastAccessed() { return lastAccessed; }
        public OrderEntity getOrderEntity() { return orderEntity; }
        public ValidationStatus getValidationStatus() { return validationStatus; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        public void setOrderEntity(OrderEntity orderEntity) { 
            this.orderEntity = orderEntity;
            this.lastAccessed = LocalDateTime.now();
        }
        
        public void setValidationStatus(ValidationStatus status) { 
            this.validationStatus = status;
            this.lastAccessed = LocalDateTime.now();
        }
        
        public void updateLastAccessed() {
            this.lastAccessed = LocalDateTime.now();
        }
        
        public void addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            this.lastAccessed = LocalDateTime.now();
        }
        
        public boolean isExpired() {
            return lastAccessed.plusMinutes(SESSION_TIMEOUT_MINUTES).isBefore(LocalDateTime.now());
        }
    }
    
    /**
     * Validation status enumeration
     */
    public enum ValidationStatus {
        UNKNOWN("Validation status unknown"),
        VALID("Data is valid and complete"),
        INVALID("Data contains validation errors"),
        INCOMPLETE("Data is missing required fields"),
        CORRUPTED("Data appears to be corrupted");
        
        private final String description;
        
        ValidationStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Recovery result for data recovery operations
     */
    public static class RecoveryResult {
        private final boolean successful;
        private final OrderEntity recoveredOrder;
        private final String message;
        
        public RecoveryResult(boolean successful, OrderEntity recoveredOrder, String message) {
            this.successful = successful;
            this.recoveredOrder = recoveredOrder;
            this.message = message;
        }
        
        public boolean isSuccessful() { return successful; }
        public OrderEntity getRecoveredOrder() { return recoveredOrder; }
        public String getMessage() { return message; }
    }
    
    /**
     * Initialize enhanced services for data validation and loading
     */
    private static void initializeServices() {
        try {
            ServiceFactory serviceFactory = ServiceFactory.getInstance();
            orderDataLoaderService = serviceFactory.getOrderDataLoaderService();
            cartDataValidationService = serviceFactory.getCartDataValidationService();
            logger.info("OrderDataContextManager: Enhanced services initialized successfully");
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderDataContextManager: Failed to initialize enhanced services", e);
        }
    }
    
    /**
     * Preserves order data in session storage with comprehensive validation.
     * 
     * @param sessionId The session identifier
     * @param order The order entity to preserve
     */
    public static void preserveOrderData(String sessionId, OrderEntity order) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            logger.warning("OrderDataContextManager.preserveOrderData: Session ID is null or empty");
            return;
        }
        
        if (order == null) {
            logger.warning("OrderDataContextManager.preserveOrderData: Order entity is null");
            return;
        }
        
        try {
            // Cleanup expired sessions before adding new one
            cleanupExpiredSessions();
            
            // Check session limit
            if (orderDataSessions.size() >= MAX_SESSIONS) {
                logger.warning("OrderDataContextManager.preserveOrderData: Maximum sessions reached, cleaning up oldest");
                cleanupOldestSessions();
            }
            
            // Create order data context
            OrderDataContext context = new OrderDataContext(sessionId, order);
            
            // Validate order data and set status
            ValidationStatus status = validateOrderDataInternal(order);
            context.setValidationStatus(status);
            
            // Add metadata
            context.addMetadata("orderId", order.getOrderId());
            context.addMetadata("preservedAt", LocalDateTime.now());
            if (order.getDeliveryInfo() != null) {
                context.addMetadata("hasDeliveryInfo", true);
            }
            if (order.getOrderItems() != null) {
                context.addMetadata("itemCount", order.getOrderItems().size());
            }
            
            // Store in session
            orderDataSessions.put(sessionId, context);
            
            logger.info("OrderDataContextManager.preserveOrderData: Order data preserved for session " + sessionId + 
                       " with status " + status + " for Order " + order.getOrderId());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderDataContextManager.preserveOrderData: Error preserving order data", e);
        }
    }
    
    /**
     * Retrieves order data from session storage with automatic validation.
     * 
     * @param sessionId The session identifier
     * @return The order entity, or null if not found or expired
     */
    public static OrderEntity retrieveOrderData(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            logger.warning("OrderDataContextManager.retrieveOrderData: Session ID is null or empty");
            return null;
        }
        
        try {
            OrderDataContext context = orderDataSessions.get(sessionId);
            if (context == null) {
                logger.info("OrderDataContextManager.retrieveOrderData: No order data found for session " + sessionId);
                return null;
            }
            
            // Check if session is expired
            if (context.isExpired()) {
                logger.warning("OrderDataContextManager.retrieveOrderData: Session " + sessionId + " has expired");
                orderDataSessions.remove(sessionId);
                return null;
            }
            
            // Update last accessed time
            context.updateLastAccessed();
            
            OrderEntity order = context.getOrderEntity();
            if (order != null) {
                logger.info("OrderDataContextManager.retrieveOrderData: Order data retrieved for session " + sessionId + 
                           " for Order " + order.getOrderId());
                
                // Optionally refresh order data if it's stale
                if (shouldRefreshOrderData(context)) {
                    OrderEntity refreshedOrder = refreshOrderDataFromService(order);
                    if (refreshedOrder != null) {
                        context.setOrderEntity(refreshedOrder);
                        logger.info("OrderDataContextManager.retrieveOrderData: Order data refreshed for " + order.getOrderId());
                        return refreshedOrder;
                    }
                }
            }
            
            return order;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderDataContextManager.retrieveOrderData: Error retrieving order data", e);
            return null;
        }
    }
    
    /**
     * Checks if order data exists for the given session.
     * 
     * @param sessionId The session identifier
     * @return true if order data exists and is not expired, false otherwise
     */
    public static boolean hasOrderData(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        
        OrderDataContext context = orderDataSessions.get(sessionId);
        if (context == null) {
            return false;
        }
        
        if (context.isExpired()) {
            orderDataSessions.remove(sessionId);
            return false;
        }
        
        return context.getOrderEntity() != null;
    }
    
    /**
     * Clears order data for the given session.
     * 
     * @param sessionId The session identifier
     */
    public static void clearOrderData(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }
        
        OrderDataContext context = orderDataSessions.remove(sessionId);
        if (context != null) {
            logger.info("OrderDataContextManager.clearOrderData: Order data cleared for session " + sessionId);
        }
    }
    
    /**
     * Validates order data completeness with enhanced validation services.
     * 
     * @param order The order entity to validate
     * @return ValidationResult containing validation status and details
     */
    public static ValidationResult validateOrderDataCompleteness(OrderEntity order) {
        if (order == null) {
            return new ValidationResult(false, "Order entity is null");
        }
        
        try {
            // Use enhanced order data loader service if available
            if (orderDataLoaderService != null) {
                boolean isComplete = orderDataLoaderService.validateOrderDataCompleteness(order);
                if (!isComplete) {
                    return new ValidationResult(false, "Order data completeness validation failed via OrderDataLoaderService");
                }
                
                // Additional lazy loading validation
                boolean lazyLoadingValid = orderDataLoaderService.validateLazyLoadingInitialization(order);
                if (!lazyLoadingValid) {
                    return new ValidationResult(false, "Order lazy loading validation failed");
                }
            }
            
            // Basic validation checks
            ValidationResult basicValidation = validateOrderDataBasic(order);
            if (!basicValidation.isValid()) {
                return basicValidation;
            }
            
            logger.info("OrderDataContextManager.validateOrderDataCompleteness: Order validation passed for " + order.getOrderId());
            return new ValidationResult(true, "Order data validation completed successfully");
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderDataContextManager.validateOrderDataCompleteness: Validation error", e);
            return new ValidationResult(false, "Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Enriches order data with missing information using enhanced services.
     * 
     * @param partialOrder The partial order entity
     * @return Enriched order entity, or original if enrichment fails
     */
    public static OrderEntity enrichOrderData(OrderEntity partialOrder) {
        if (partialOrder == null) {
            logger.warning("OrderDataContextManager.enrichOrderData: Partial order is null");
            return null;
        }
        
        try {
            if (orderDataLoaderService != null) {
                // Try to refresh order relationships
                OrderEntity enrichedOrder = orderDataLoaderService.refreshOrderRelationships(partialOrder);
                if (enrichedOrder != null) {
                    logger.info("OrderDataContextManager.enrichOrderData: Order data enriched for " + partialOrder.getOrderId());
                    return enrichedOrder;
                }
                
                // Try to load complete order data
                enrichedOrder = orderDataLoaderService.loadCompleteOrderData(partialOrder.getOrderId());
                if (enrichedOrder != null) {
                    logger.info("OrderDataContextManager.enrichOrderData: Complete order data loaded for " + partialOrder.getOrderId());
                    return enrichedOrder;
                }
            }
            
            logger.warning("OrderDataContextManager.enrichOrderData: Could not enrich order data, returning original");
            return partialOrder;
            
        } catch (ResourceNotFoundException e) {
            logger.warning("OrderDataContextManager.enrichOrderData: Order not found during enrichment: " + e.getMessage());
            return partialOrder;
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderDataContextManager.enrichOrderData: Error enriching order data", e);
            return partialOrder;
        }
    }
    
    /**
     * Checks if order data can be recovered for the given session.
     * 
     * @param sessionId The session identifier
     * @return true if recovery is possible, false otherwise
     */
    public static boolean canRecoverOrderData(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        
        // Check if session exists
        OrderDataContext context = orderDataSessions.get(sessionId);
        if (context != null && !context.isExpired()) {
            return true;
        }
        
        // Check if we can extract order ID from session ID for service recovery
        String orderId = extractOrderIdFromSessionId(sessionId);
        if (orderId != null && orderDataLoaderService != null) {
            try {
                OrderEntity recoveredOrder = orderDataLoaderService.loadCompleteOrderData(orderId);
                return recoveredOrder != null;
            } catch (Exception e) {
                logger.log(Level.INFO, "OrderDataContextManager.canRecoverOrderData: Service recovery not possible", e);
            }
        }
        
        return false;
    }
    
    /**
     * Attempts to recover order data using multiple recovery strategies.
     * 
     * @param sessionId The session identifier
     * @return RecoveryResult containing recovery status and recovered data
     */
    public static RecoveryResult attemptDataRecovery(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return new RecoveryResult(false, null, "Session ID is null or empty");
        }
        
        try {
            // Strategy 1: Check if data still exists in session (may be marked as expired)
            OrderDataContext context = orderDataSessions.get(sessionId);
            if (context != null && context.getOrderEntity() != null) {
                // Update session to prevent expiration
                context.updateLastAccessed();
                logger.info("OrderDataContextManager.attemptDataRecovery: Recovered order data from existing session");
                return new RecoveryResult(true, context.getOrderEntity(), "Recovered from existing session");
            }
            
            // Strategy 2: Try to recover from enhanced services using session metadata
            String orderId = extractOrderIdFromSessionId(sessionId);
            if (orderId != null && orderDataLoaderService != null) {
                OrderEntity recoveredOrder = orderDataLoaderService.loadCompleteOrderData(orderId);
                if (recoveredOrder != null) {
                    // Re-preserve the recovered data
                    preserveOrderData(sessionId, recoveredOrder);
                    logger.info("OrderDataContextManager.attemptDataRecovery: Recovered order data from service for " + orderId);
                    return new RecoveryResult(true, recoveredOrder, "Recovered from order data service");
                }
            }
            
            // Strategy 3: Check form state for partial recovery
            Map<String, Object> formState = retrieveFormState(sessionId);
            if (formState != null && !formState.isEmpty()) {
                // Try to reconstruct order from form state
                OrderEntity reconstructedOrder = reconstructOrderFromFormState(formState);
                if (reconstructedOrder != null) {
                    preserveOrderData(sessionId, reconstructedOrder);
                    logger.info("OrderDataContextManager.attemptDataRecovery: Partially recovered order data from form state");
                    return new RecoveryResult(true, reconstructedOrder, "Partially recovered from form state");
                }
            }
            
            return new RecoveryResult(false, null, "All recovery strategies failed");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderDataContextManager.attemptDataRecovery: Error during data recovery", e);
            return new RecoveryResult(false, null, "Recovery error: " + e.getMessage());
        }
    }
    
    /**
     * Preserves form state for navigation recovery.
     * 
     * @param formId The form identifier
     * @param formData The form data to preserve
     */
    public static void preserveFormState(String formId, Map<String, Object> formData) {
        if (formId == null || formId.trim().isEmpty()) {
            logger.warning("OrderDataContextManager.preserveFormState: Form ID is null or empty");
            return;
        }
        
        if (formData == null || formData.isEmpty()) {
            logger.warning("OrderDataContextManager.preserveFormState: Form data is null or empty");
            return;
        }
        
        try {
            // Add metadata
            Map<String, Object> enrichedFormData = new HashMap<>(formData);
            enrichedFormData.put("preservedAt", LocalDateTime.now());
            enrichedFormData.put("formId", formId);
            
            formStateSessions.put(formId, enrichedFormData);
            
            logger.info("OrderDataContextManager.preserveFormState: Form state preserved for " + formId);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OrderDataContextManager.preserveFormState: Error preserving form state", e);
        }
    }
    
    /**
     * Retrieves form state from session storage.
     * 
     * @param formId The form identifier
     * @return The form state map, or null if not found
     */
    public static Map<String, Object> retrieveFormState(String formId) {
        if (formId == null || formId.trim().isEmpty()) {
            return null;
        }
        
        Map<String, Object> formState = formStateSessions.get(formId);
        if (formState != null) {
            logger.info("OrderDataContextManager.retrieveFormState: Form state retrieved for " + formId);
        }
        
        return formState;
    }
    
    /**
     * Clears form state for the given form ID.
     * 
     * @param formId The form identifier
     */
    public static void clearFormState(String formId) {
        if (formId == null || formId.trim().isEmpty()) {
            return;
        }
        
        Map<String, Object> formState = formStateSessions.remove(formId);
        if (formState != null) {
            logger.info("OrderDataContextManager.clearFormState: Form state cleared for " + formId);
        }
    }
    
    /**
     * Validates navigation context for data consistency.
     * 
     * @param context The navigation context to validate
     * @return ValidationResult containing validation status and details
     */
    public static ValidationResult validateNavigationContext(NavigationContext context) {
        if (context == null) {
            return new ValidationResult(false, "Navigation context is null");
        }
        
        try {
            // Check if context has order data
            Object orderData = context.getContextData("navigationData");
            if (orderData instanceof OrderEntity) {
                OrderEntity order = (OrderEntity) orderData;
                return validateOrderDataCompleteness(order);
            }
            
            // Check if context has session reference
            String sessionId = context.getContextData("sessionId", String.class);
            if (sessionId != null && hasOrderData(sessionId)) {
                return new ValidationResult(true, "Navigation context has valid session reference");
            }
            
            return new ValidationResult(true, "Navigation context validation passed");
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "OrderDataContextManager.validateNavigationContext: Validation error", e);
            return new ValidationResult(false, "Navigation context validation error: " + e.getMessage());
        }
    }
    
    /**
     * Generates a unique session ID for order data storage.
     * 
     * @param orderId The order ID to include in the session
     * @return A unique session identifier
     */
    public static String generateSessionId(String orderId) {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        if (orderId != null && !orderId.trim().isEmpty()) {
            return DEFAULT_SESSION_PREFIX + orderId + "_" + uniqueId;
        } else {
            return DEFAULT_SESSION_PREFIX + "TEMP_" + uniqueId;
        }
    }
    
    /**
     * Gets debug information about the context manager state.
     * 
     * @return String containing debug information
     */
    public static String getContextManagerDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("OrderDataContextManager Debug Info:\n");
        info.append("Active Sessions: ").append(orderDataSessions.size()).append("/").append(MAX_SESSIONS).append("\n");
        info.append("Form States: ").append(formStateSessions.size()).append("\n");
        info.append("Services Available: ").append(orderDataLoaderService != null && cartDataValidationService != null).append("\n");
        
        // Session details
        info.append("Session Details:\n");
        orderDataSessions.forEach((sessionId, context) -> {
            info.append("  ").append(sessionId).append(": Order ")
                .append(context.getOrderEntity() != null ? context.getOrderEntity().getOrderId() : "null")
                .append(", Status: ").append(context.getValidationStatus())
                .append(", Expires: ").append(context.isExpired() ? "YES" : "NO").append("\n");
        });
        
        return info.toString();
    }
    
    /**
     * Cleans up expired sessions to free memory.
     */
    public static void cleanupExpiredSessions() {
        int removedCount = 0;
        var iterator = orderDataSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.info("OrderDataContextManager.cleanupExpiredSessions: Cleaned up " + removedCount + " expired sessions");
        }
    }
    
    /**
     * Cleans up oldest sessions when limit is reached.
     */
    private static void cleanupOldestSessions() {
        if (orderDataSessions.size() < MAX_SESSIONS) {
            return;
        }
        
        // Find and remove oldest sessions
        orderDataSessions.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().getCreatedAt().compareTo(e2.getValue().getCreatedAt()))
            .limit(MAX_SESSIONS / 4) // Remove 25% of sessions
            .forEach(entry -> orderDataSessions.remove(entry.getKey()));
        
        logger.info("OrderDataContextManager.cleanupOldestSessions: Cleaned up oldest sessions");
    }
    
    // Helper methods
    
    private static ValidationStatus validateOrderDataInternal(OrderEntity order) {
        try {
            ValidationResult result = validateOrderDataCompleteness(order);
            return result.isValid() ? ValidationStatus.VALID : ValidationStatus.INVALID;
        } catch (Exception e) {
            return ValidationStatus.CORRUPTED;
        }
    }
    
    private static ValidationResult validateOrderDataBasic(OrderEntity order) {
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            return new ValidationResult(false, "Order ID is missing");
        }
        
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return new ValidationResult(false, "Order items are missing");
        }
        
        if (order.getTotalAmountPaid() <= 0) {
            return new ValidationResult(false, "Total amount is invalid");
        }
        
        return new ValidationResult(true, "Basic validation passed");
    }
    
    private static boolean shouldRefreshOrderData(OrderDataContext context) {
        // Refresh if data is older than 5 minutes
        return context.getLastAccessed().plusMinutes(5).isBefore(LocalDateTime.now());
    }
    
    private static OrderEntity refreshOrderDataFromService(OrderEntity order) {
        if (orderDataLoaderService == null || order == null) {
            return null;
        }
        
        try {
            return orderDataLoaderService.loadCompleteOrderData(order.getOrderId());
        } catch (Exception e) {
            logger.log(Level.INFO, "OrderDataContextManager.refreshOrderDataFromService: Refresh failed", e);
            return null;
        }
    }
    
    private static String extractOrderIdFromSessionId(String sessionId) {
        if (sessionId == null || !sessionId.startsWith(DEFAULT_SESSION_PREFIX)) {
            return null;
        }
        
        String remaining = sessionId.substring(DEFAULT_SESSION_PREFIX.length());
        int underscoreIndex = remaining.lastIndexOf("_");
        if (underscoreIndex > 0) {
            return remaining.substring(0, underscoreIndex);
        }
        
        return null;
    }
    
    private static OrderEntity reconstructOrderFromFormState(Map<String, Object> formState) {
        // This is a simplified reconstruction - in practice, you might want more sophisticated logic
        try {
            String orderId = (String) formState.get("orderId");
            if (orderId != null && orderDataLoaderService != null) {
                return orderDataLoaderService.loadCompleteOrderData(orderId);
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "OrderDataContextManager.reconstructOrderFromFormState: Reconstruction failed", e);
        }
        
        return null;
    }
    
    /**
     * Validation result class for validation operations
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}