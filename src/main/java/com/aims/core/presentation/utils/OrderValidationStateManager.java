package com.aims.core.presentation.utils;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.enums.OrderStatus;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * OrderValidationStateManager
 * 
 * Manages validation states across navigation to prevent validation bypass issues
 * and provide comprehensive state tracking for the payment flow.
 * 
 * Key Features:
 * - Thread-safe concurrent state management
 * - Automatic cleanup of expired validation states
 * - Validation context tracking for debugging
 * - Navigation state conflict prevention
 * - Timestamp tracking for audit trails
 * 
 * This utility prevents common payment flow issues such as:
 * - Users bypassing validation steps through navigation
 * - Stale validation states persisting across sessions
 * - Race conditions in multi-threaded validation scenarios
 * - Loss of validation context during screen transitions
 */
public class OrderValidationStateManager {
    
    private static final Logger logger = Logger.getLogger(OrderValidationStateManager.class.getName());
    
    // Singleton instance for global state management
    private static volatile OrderValidationStateManager instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    // State storage with concurrent access support
    private final ConcurrentHashMap<String, ValidationState> validationStates;
    private final ConcurrentHashMap<String, NavigationContext> navigationContexts;
    
    // Cleanup and maintenance
    private final ScheduledExecutorService cleanupExecutor;
    private final AtomicLong stateIdGenerator;
    
    // Configuration constants
    private static final Duration DEFAULT_STATE_EXPIRY = Duration.ofHours(2);
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(15);
    private static final int MAX_STATES_PER_ORDER = 10;
    
    /**
     * Validation state information
     */
    public static class ValidationState {
        private final String stateId;
        private final String orderId;
        private final LocalDateTime timestamp;
        private final LocalDateTime expiryTime;
        private final ValidationStep currentStep;
        private final ValidationResult result;
        private final String validationContext;
        private final Map<String, Object> additionalData;
        private volatile boolean isValid;
        
        public ValidationState(String stateId, String orderId, ValidationStep step, 
                             ValidationResult result, String context) {
            this.stateId = stateId;
            this.orderId = orderId;
            this.timestamp = LocalDateTime.now();
            this.expiryTime = timestamp.plus(DEFAULT_STATE_EXPIRY);
            this.currentStep = step;
            this.result = result;
            this.validationContext = context;
            this.additionalData = new ConcurrentHashMap<>();
            this.isValid = true;
        }
        
        // Getters
        public String getStateId() { return stateId; }
        public String getOrderId() { return orderId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public LocalDateTime getExpiryTime() { return expiryTime; }
        public ValidationStep getCurrentStep() { return currentStep; }
        public ValidationResult getResult() { return result; }
        public String getValidationContext() { return validationContext; }
        public Map<String, Object> getAdditionalData() { return additionalData; }
        public boolean isValid() { return isValid && LocalDateTime.now().isBefore(expiryTime); }
        
        public void invalidate() { this.isValid = false; }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
        
        public Duration getAge() {
            return Duration.between(timestamp, LocalDateTime.now());
        }
    }
    
    /**
     * Navigation context to track user movement through payment flow
     */
    public static class NavigationContext {
        private final String orderId;
        private final String fromScreen;
        private final String toScreen;
        private final LocalDateTime navigationTime;
        private final Map<String, Object> preservedData;
        
        public NavigationContext(String orderId, String fromScreen, String toScreen) {
            this.orderId = orderId;
            this.fromScreen = fromScreen;
            this.toScreen = toScreen;
            this.navigationTime = LocalDateTime.now();
            this.preservedData = new ConcurrentHashMap<>();
        }
        
        // Getters
        public String getOrderId() { return orderId; }
        public String getFromScreen() { return fromScreen; }
        public String getToScreen() { return toScreen; }
        public LocalDateTime getNavigationTime() { return navigationTime; }
        public Map<String, Object> getPreservedData() { return preservedData; }
    }
    
    /**
     * Validation steps in the payment flow
     */
    public enum ValidationStep {
        ORDER_CREATION("Order Creation"),
        DELIVERY_INFO_VALIDATION("Delivery Information Validation"),
        ORDER_SUMMARY_VALIDATION("Order Summary Validation"),
        PAYMENT_METHOD_SELECTION("Payment Method Selection"),
        PAYMENT_PROCESSING("Payment Processing"),
        PAYMENT_COMPLETION("Payment Completion");
        
        private final String description;
        
        ValidationStep(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * Validation results
     */
    public enum ValidationResult {
        PENDING("Validation in progress"),
        PASSED("Validation successful"),
        FAILED("Validation failed"),
        BYPASSED("Validation bypassed"),
        EXPIRED("Validation expired"),
        CANCELLED("Validation cancelled");
        
        private final String description;
        
        ValidationResult(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    /**
     * Private constructor for singleton pattern
     */
    private OrderValidationStateManager() {
        this.validationStates = new ConcurrentHashMap<>();
        this.navigationContexts = new ConcurrentHashMap<>();
        this.stateIdGenerator = new AtomicLong(1);
        
        // Initialize cleanup executor
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "OrderValidationStateManager-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Schedule periodic cleanup
        startPeriodicCleanup();
        
        logger.info("OrderValidationStateManager initialized successfully");
    }
    
    /**
     * Get singleton instance with thread-safe lazy initialization
     */
    public static OrderValidationStateManager getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new OrderValidationStateManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Create and track a new validation state
     */
    public String createValidationState(String orderId, ValidationStep step, 
                                      ValidationResult result, String context) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        
        String stateId = generateStateId();
        ValidationState state = new ValidationState(stateId, orderId, step, result, context);
        
        // Prevent state overflow per order
        cleanupOldStatesForOrder(orderId);
        
        validationStates.put(stateId, state);
        
        logger.info(String.format("Created validation state: %s for order: %s, step: %s, result: %s", 
                                 stateId, orderId, step.getDescription(), result.getDescription()));
        
        return stateId;
    }
    
    /**
     * Update existing validation state
     */
    public boolean updateValidationState(String stateId, ValidationResult newResult, String context) {
        ValidationState existingState = validationStates.get(stateId);
        if (existingState == null || !existingState.isValid()) {
            logger.warning("Attempted to update non-existent or invalid validation state: " + stateId);
            return false;
        }
        
        // Create new state with updated result
        ValidationState updatedState = new ValidationState(
            stateId, 
            existingState.getOrderId(), 
            existingState.getCurrentStep(), 
            newResult, 
            context
        );
        
        // Copy additional data
        updatedState.getAdditionalData().putAll(existingState.getAdditionalData());
        
        validationStates.put(stateId, updatedState);
        
        logger.info(String.format("Updated validation state: %s, new result: %s", 
                                 stateId, newResult.getDescription()));
        
        return true;
    }
    
    /**
     * Get validation state by ID
     */
    public Optional<ValidationState> getValidationState(String stateId) {
        ValidationState state = validationStates.get(stateId);
        if (state != null && state.isValid()) {
            return Optional.of(state);
        }
        return Optional.empty();
    }
    
    /**
     * Get all validation states for an order
     */
    public Map<String, ValidationState> getValidationStatesForOrder(String orderId) {
        Map<String, ValidationState> orderStates = new ConcurrentHashMap<>();
        
        validationStates.entrySet().stream()
            .filter(entry -> orderId.equals(entry.getValue().getOrderId()))
            .filter(entry -> entry.getValue().isValid())
            .forEach(entry -> orderStates.put(entry.getKey(), entry.getValue()));
        
        return orderStates;
    }
    
    /**
     * Check if order has valid validation for specific step
     */
    public boolean hasValidValidation(String orderId, ValidationStep step) {
        return validationStates.values().stream()
            .anyMatch(state -> orderId.equals(state.getOrderId()) 
                           && step.equals(state.getCurrentStep())
                           && state.getResult() == ValidationResult.PASSED
                           && state.isValid());
    }
    
    /**
     * Invalidate all validation states for an order
     */
    public void invalidateOrderValidations(String orderId) {
        validationStates.values().stream()
            .filter(state -> orderId.equals(state.getOrderId()))
            .forEach(ValidationState::invalidate);
        
        logger.info("Invalidated all validation states for order: " + orderId);
    }
    
    /**
     * Track navigation between screens
     */
    public void trackNavigation(String orderId, String fromScreen, String toScreen) {
        String contextId = orderId + "_" + System.currentTimeMillis();
        NavigationContext context = new NavigationContext(orderId, fromScreen, toScreen);
        
        navigationContexts.put(contextId, context);
        
        logger.info(String.format("Tracked navigation for order %s: %s -> %s", 
                                 orderId, fromScreen, toScreen));
        
        // Check for potential validation bypass
        checkForValidationBypass(orderId, fromScreen, toScreen);
    }
    
    /**
     * Preserve data during navigation
     */
    public void preserveNavigationData(String orderId, String key, Object value) {
        navigationContexts.values().stream()
            .filter(context -> orderId.equals(context.getOrderId()))
            .reduce((first, second) -> second) // Get most recent
            .ifPresent(context -> context.getPreservedData().put(key, value));
    }
    
    /**
     * Retrieve preserved navigation data
     */
    public Optional<Object> getPreservedNavigationData(String orderId, String key) {
        return navigationContexts.values().stream()
            .filter(context -> orderId.equals(context.getOrderId()))
            .reduce((first, second) -> second) // Get most recent
            .map(context -> context.getPreservedData().get(key));
    }
    
    /**
     * Validate order against current validation states
     */
    public ValidationSummary validateOrderForPayment(OrderEntity order) {
        if (order == null || order.getOrderId() == null) {
            return new ValidationSummary(false, "Order information is missing");
        }
        
        String orderId = order.getOrderId();
        Map<String, ValidationState> orderStates = getValidationStatesForOrder(orderId);
        
        ValidationSummary summary = new ValidationSummary(true, "Order validation successful");
        
        // Check delivery info validation
        if (!hasValidValidation(orderId, ValidationStep.DELIVERY_INFO_VALIDATION)) {
            if (order.getDeliveryInfo() == null) {
                summary.addWarning("Delivery information validation missing");
                summary.setValid(false);
            } else {
                // Auto-create validation state if delivery info exists
                createValidationState(orderId, ValidationStep.DELIVERY_INFO_VALIDATION, 
                                    ValidationResult.PASSED, "Auto-validated existing delivery info");
            }
        }
        
        // Check order summary validation
        if (!hasValidValidation(orderId, ValidationStep.ORDER_SUMMARY_VALIDATION)) {
            summary.addWarning("Order summary validation missing");
        }
        
        // Additional validations
        validateOrderBusinessRules(order, summary);
        
        return summary;
    }
    
    /**
     * Validation summary result
     */
    public static class ValidationSummary {
        private boolean valid;
        private String message;
        private final java.util.List<String> warnings;
        private final java.util.List<String> errors;
        private final LocalDateTime timestamp;
        
        public ValidationSummary(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
            this.warnings = new java.util.ArrayList<>();
            this.errors = new java.util.ArrayList<>();
            this.timestamp = LocalDateTime.now();
        }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public java.util.List<String> getWarnings() { return warnings; }
        public java.util.List<String> getErrors() { return errors; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public void addWarning(String warning) { warnings.add(warning); }
        public void addError(String error) { 
            errors.add(error); 
            setValid(false);
        }
        
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }
    
    /**
     * Check for potential validation bypass through navigation
     */
    private void checkForValidationBypass(String orderId, String fromScreen, String toScreen) {
        // Define the expected flow sequence
        if ("OrderSummaryController".equals(fromScreen) && "PaymentMethodScreenController".equals(toScreen)) {
            if (!hasValidValidation(orderId, ValidationStep.ORDER_SUMMARY_VALIDATION)) {
                logger.warning(String.format("POTENTIAL VALIDATION BYPASS: Order %s navigated to payment method without order summary validation", orderId));
                createValidationState(orderId, ValidationStep.ORDER_SUMMARY_VALIDATION, 
                                    ValidationResult.BYPASSED, "Navigation without proper validation");
            }
        }
        
        if ("DeliveryInfoScreenController".equals(fromScreen) && "OrderSummaryController".equals(toScreen)) {
            if (!hasValidValidation(orderId, ValidationStep.DELIVERY_INFO_VALIDATION)) {
                logger.warning(String.format("POTENTIAL VALIDATION BYPASS: Order %s navigated to order summary without delivery info validation", orderId));
                createValidationState(orderId, ValidationStep.DELIVERY_INFO_VALIDATION, 
                                    ValidationResult.BYPASSED, "Navigation without proper validation");
            }
        }
    }
    
    /**
     * Validate business rules for order
     */
    private void validateOrderBusinessRules(OrderEntity order, ValidationSummary summary) {
        try {
            // Basic order validation
            if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
                summary.addError("Order must contain at least one item");
            }
            
            if (order.getTotalAmountPaid() <= 0) {
                summary.addError("Order total amount must be greater than zero");
            }
            
            if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
                summary.addWarning("Order status is not pending payment: " + order.getOrderStatus());
            }
            
            // Delivery info validation
            DeliveryInfo deliveryInfo = order.getDeliveryInfo();
            if (deliveryInfo != null) {
                if (deliveryInfo.getRecipientName() == null || deliveryInfo.getRecipientName().trim().isEmpty()) {
                    summary.addError("Recipient name is required");
                }
                
                if (deliveryInfo.getDeliveryAddress() == null || deliveryInfo.getDeliveryAddress().trim().isEmpty()) {
                    summary.addError("Delivery address is required");
                }
                
                if (deliveryInfo.getPhoneNumber() == null || deliveryInfo.getPhoneNumber().trim().isEmpty()) {
                    summary.addError("Phone number is required");
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during business rule validation for order: " + order.getOrderId(), e);
            summary.addError("Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Clean up old states for an order to prevent memory overflow
     */
    private void cleanupOldStatesForOrder(String orderId) {
        java.util.List<String> orderStateIds = validationStates.entrySet().stream()
            .filter(entry -> orderId.equals(entry.getValue().getOrderId()))
            .sorted((e1, e2) -> e2.getValue().getTimestamp().compareTo(e1.getValue().getTimestamp()))
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
        
        if (orderStateIds.size() >= MAX_STATES_PER_ORDER) {
            // Remove oldest states beyond limit
            orderStateIds.subList(MAX_STATES_PER_ORDER - 1, orderStateIds.size())
                .forEach(stateId -> {
                    validationStates.remove(stateId);
                    logger.fine("Removed old validation state: " + stateId);
                });
        }
    }
    
    /**
     * Start periodic cleanup of expired states
     */
    private void startPeriodicCleanup() {
        cleanupExecutor.scheduleAtFixedRate(
            this::performCleanup,
            CLEANUP_INTERVAL.toMinutes(),
            CLEANUP_INTERVAL.toMinutes(),
            TimeUnit.MINUTES
        );
    }
    
    /**
     * Perform cleanup of expired states and contexts
     */
    private void performCleanup() {
        try {
            int removedStates = 0;
            int removedContexts = 0;
            
            // Clean up expired validation states
            java.util.Iterator<Map.Entry<String, ValidationState>> stateIterator = validationStates.entrySet().iterator();
            while (stateIterator.hasNext()) {
                Map.Entry<String, ValidationState> entry = stateIterator.next();
                if (entry.getValue().isExpired()) {
                    stateIterator.remove();
                    removedStates++;
                }
            }
            
            // Clean up old navigation contexts (older than 1 hour)
            LocalDateTime cutoff = LocalDateTime.now().minus(Duration.ofHours(1));
            java.util.Iterator<Map.Entry<String, NavigationContext>> contextIterator = navigationContexts.entrySet().iterator();
            while (contextIterator.hasNext()) {
                Map.Entry<String, NavigationContext> entry = contextIterator.next();
                if (entry.getValue().getNavigationTime().isBefore(cutoff)) {
                    contextIterator.remove();
                    removedContexts++;
                }
            }
            
            if (removedStates > 0 || removedContexts > 0) {
                logger.info(String.format("Cleanup completed: removed %d expired validation states and %d old navigation contexts", 
                                         removedStates, removedContexts));
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during periodic cleanup", e);
        }
    }
    
    /**
     * Generate unique state ID
     */
    private String generateStateId() {
        return "VS_" + System.currentTimeMillis() + "_" + stateIdGenerator.getAndIncrement();
    }
    
    /**
     * Get debug information about current state
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("OrderValidationStateManager Debug Info:\n");
        info.append("Total validation states: ").append(validationStates.size()).append("\n");
        info.append("Total navigation contexts: ").append(navigationContexts.size()).append("\n");
        
        Map<String, Long> statesByOrder = validationStates.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                ValidationState::getOrderId, 
                java.util.stream.Collectors.counting()
            ));
        
        info.append("States by order:\n");
        statesByOrder.forEach((orderId, count) -> 
            info.append("  ").append(orderId).append(": ").append(count).append(" states\n"));
        
        return info.toString();
    }
    
    /**
     * Shutdown the manager gracefully
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        validationStates.clear();
        navigationContexts.clear();
        
        logger.info("OrderValidationStateManager shutdown completed");
    }
}