package com.aims.core.presentation.utils;

import com.aims.core.entities.OrderEntity;
import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.presentation.controllers.OrderSummaryController;
import com.aims.core.presentation.controllers.DeliveryInfoScreenController;
import com.aims.core.presentation.controllers.PaymentMethodScreenController;
import com.aims.core.shared.NavigationService;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * Unified Navigation Manager - Single entry point for all checkout navigation.
 * 
 * This class consolidates all navigation strategies into a single, reliable system that:
 * - Provides a single point of entry for all checkout navigation
 * - Implements comprehensive validation and fallback mechanisms
 * - Preserves order data across navigation transitions
 * - Provides detailed logging and monitoring
 * - Handles MainLayoutController availability issues
 * 
 * Phase 2 of the Payment Flow Navigation Fix - replaces multiple conflicting navigation approaches.
 * 
 * @author AIMS Navigation Enhancement Team
 * @version 2.0
 * @since Phase 2 - Unified Navigation Architecture
 */
public class UnifiedNavigationManager {
    
    private static final Logger logger = Logger.getLogger(UnifiedNavigationManager.class.getName());
    
    // Navigation result enumeration
    public enum NavigationResult {
        SUCCESS("Navigation completed successfully"),
        PARTIAL_SUCCESS("Navigation completed with warnings"),
        FAILED_RECOVERABLE("Navigation failed but recovery possible"),
        FAILED_CRITICAL("Navigation failed critically"),
        CANCELLED("Navigation was cancelled"),
        DATA_PRESERVED("Navigation failed but data was preserved");
        
        private final String description;
        
        NavigationResult(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isSuccess() {
            return this == SUCCESS || this == PARTIAL_SUCCESS;
        }
        
        public boolean hasDataPreservation() {
            return this == DATA_PRESERVED || this == PARTIAL_SUCCESS;
        }
    }
    
    // Navigation request wrapper
    public static class NavigationRequest {
        private final String sessionId;
        private final String targetScreen;
        private final OrderEntity order;
        private final Object sourceController;
        private final LocalDateTime requestTime;
        private final String requestId;
        
        public NavigationRequest(String targetScreen, OrderEntity order, Object sourceController) {
            this.sessionId = generateSessionId();
            this.targetScreen = targetScreen;
            this.order = order;
            this.sourceController = sourceController;
            this.requestTime = LocalDateTime.now();
            this.requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        
        private String generateSessionId() {
            return "NAV_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getTargetScreen() { return targetScreen; }
        public OrderEntity getOrder() { return order; }
        public Object getSourceController() { return sourceController; }
        public LocalDateTime getRequestTime() { return requestTime; }
        public String getRequestId() { return requestId; }
    }
    
    // State tracking
    private static NavigationRequest lastNavigationRequest;
    private static NavigationResult lastNavigationResult;
    private static Exception lastNavigationError;
    private static int successfulNavigations = 0;
    private static int failedNavigations = 0;
    
    /**
     * Navigate to Order Summary screen with comprehensive validation and data preservation.
     * 
     * @param order The order entity to display
     * @param sourceController The source controller initiating navigation
     * @return NavigationResult indicating success or failure with details
     */
    public static NavigationResult navigateToOrderSummary(OrderEntity order, Object sourceController) {
        return navigateWithOrderData("order_summary_screen.fxml", order, sourceController);
    }
    
    /**
     * Navigate to Payment Method screen with comprehensive validation and data preservation.
     * 
     * @param order The order entity ready for payment
     * @param sourceController The source controller initiating navigation
     * @return NavigationResult indicating success or failure with details
     */
    public static NavigationResult navigateToPaymentMethod(OrderEntity order, Object sourceController) {
        return navigateWithOrderData("payment_method_screen.fxml", order, sourceController);
    }
    
    /**
     * Navigate to Delivery Info screen with comprehensive validation and data preservation.
     * 
     * @param order The order entity to edit
     * @param sourceController The source controller initiating navigation
     * @return NavigationResult indicating success or failure with details
     */
    public static NavigationResult navigateToDeliveryInfo(OrderEntity order, Object sourceController) {
        return navigateWithOrderData("delivery_info_screen.fxml", order, sourceController);
    }
    
    /**
     * Core navigation method with order data preservation and comprehensive fallback strategies.
     * 
     * @param targetScreen The target screen FXML filename
     * @param order The order data to preserve and transfer
     * @param sourceController The source controller
     * @return NavigationResult indicating success or failure
     */
    public static NavigationResult navigateWithOrderData(String targetScreen, OrderEntity order, Object sourceController) {
        NavigationRequest request = new NavigationRequest(targetScreen, order, sourceController);
        lastNavigationRequest = request;
        
        logger.info("UnifiedNavigationManager.navigateWithOrderData: Starting navigation request " + 
            request.getRequestId() + " to " + targetScreen);
        
        try {
            // Step 1: Validate navigation prerequisites
            if (!validateNavigationRequest(request)) {
                return recordResult(NavigationResult.FAILED_CRITICAL, new IllegalArgumentException("Navigation request validation failed"));
            }
            
            // Step 2: Preserve order data before navigation attempt
            String sessionId = request.getSessionId();
            try {
                preserveOrderData(request);
                logger.info("UnifiedNavigationManager.navigateWithOrderData: Order data preserved with session " + sessionId);
            } catch (Exception e) {
                logger.warning("UnifiedNavigationManager.navigateWithOrderData: Order data preservation failed: " + e.getMessage());
                sessionId = null; // Clear session ID if preservation failed
            }
            
            // Step 3: Get validated MainLayoutController
            MainLayoutController controller = getValidatedMainLayoutController();
            if (controller == null) {
                logger.warning("UnifiedNavigationManager.navigateWithOrderData: No MainLayoutController available, using emergency navigation");
                return executeEmergencyNavigation(request);
            }
            
            // Step 4: Execute navigation with validated controller
            NavigationResult result = executeNavigationWithController(request, controller, sessionId);
            
            // Step 5: Validate and return result
            return recordResult(result, null);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "UnifiedNavigationManager.navigateWithOrderData: Critical error during navigation", e);
            return recordResult(NavigationResult.FAILED_CRITICAL, e);
        }
    }
    
    /**
     * Validates the navigation request for completeness and correctness.
     * 
     * @param request The navigation request to validate
     * @return true if request is valid, false otherwise
     */
    private static boolean validateNavigationRequest(NavigationRequest request) {
        if (request == null) {
            logger.severe("UnifiedNavigationManager.validateNavigationRequest: Navigation request is null");
            return false;
        }
        
        if (request.getTargetScreen() == null || request.getTargetScreen().trim().isEmpty()) {
            logger.severe("UnifiedNavigationManager.validateNavigationRequest: Target screen is null or empty");
            return false;
        }
        
        if (request.getOrder() == null) {
            logger.severe("UnifiedNavigationManager.validateNavigationRequest: Order data is null");
            return false;
        }
        
        if (request.getOrder().getOrderId() == null || request.getOrder().getOrderId().trim().isEmpty()) {
            logger.severe("UnifiedNavigationManager.validateNavigationRequest: Order ID is missing");
            return false;
        }
        
        // Screen-specific validation
        if (!validateScreenSpecificRequirements(request)) {
            return false;
        }
        
        logger.fine("UnifiedNavigationManager.validateNavigationRequest: Request validation passed for " + request.getRequestId());
        return true;
    }
    
    /**
     * Validates screen-specific requirements for the navigation.
     * 
     * @param request The navigation request
     * @return true if screen-specific requirements are met, false otherwise
     */
    private static boolean validateScreenSpecificRequirements(NavigationRequest request) {
        String targetScreen = request.getTargetScreen();
        OrderEntity order = request.getOrder();
        
        if (targetScreen.contains("payment_method")) {
            if (order.getDeliveryInfo() == null) {
                logger.warning("UnifiedNavigationManager.validateScreenSpecificRequirements: Delivery info missing for payment method screen");
                return false;
            }
            if (order.getTotalAmountPaid() <= 0) {
                logger.warning("UnifiedNavigationManager.validateScreenSpecificRequirements: Invalid total amount for payment method screen");
                return false;
            }
        } else if (targetScreen.contains("order_summary")) {
            if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
                logger.warning("UnifiedNavigationManager.validateScreenSpecificRequirements: Order items missing for order summary screen");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Preserves order data before navigation attempt using OrderDataContextManager.
     *
     * @param request The navigation request containing order data
     * @throws Exception if preservation fails
     */
    private static void preserveOrderData(NavigationRequest request) throws Exception {
        try {
            OrderDataContextManager.preserveOrderData(
                request.getSessionId(),
                request.getOrder()
            );
            
            logger.fine("UnifiedNavigationManager.preserveOrderData: Order data preserved for session " + request.getSessionId());
        } catch (Exception e) {
            logger.log(Level.WARNING, "UnifiedNavigationManager.preserveOrderData: Error preserving order data", e);
            throw e; // Re-throw to let caller handle
        }
    }
    
    /**
     * Gets a validated MainLayoutController using the registry with fallback strategies.
     * 
     * @return A validated MainLayoutController, or null if none available
     */
    private static MainLayoutController getValidatedMainLayoutController() {
        try {
            // Strategy 1: Get from registry with timeout
            MainLayoutController controller = MainLayoutControllerRegistry.getInstance(3, TimeUnit.SECONDS);
            if (controller != null) {
                logger.fine("UnifiedNavigationManager.getValidatedMainLayoutController: Controller obtained from registry");
                return controller;
            }
            
            // Strategy 2: Force registry re-validation
            if (MainLayoutControllerRegistry.revalidate()) {
                controller = MainLayoutControllerRegistry.getInstanceImmediate();
                if (controller != null) {
                    logger.info("UnifiedNavigationManager.getValidatedMainLayoutController: Controller obtained after re-validation");
                    return controller;
                }
            }
            
            logger.warning("UnifiedNavigationManager.getValidatedMainLayoutController: No MainLayoutController available");
            return null;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "UnifiedNavigationManager.getValidatedMainLayoutController: Error getting controller", e);
            return null;
        }
    }
    
    /**
     * Executes navigation with a validated MainLayoutController.
     * 
     * @param request The navigation request
     * @param controller The validated MainLayoutController
     * @param sessionId The session ID for preserved data
     * @return NavigationResult indicating success or failure
     */
    private static NavigationResult executeNavigationWithController(NavigationRequest request, 
                                                                   MainLayoutController controller, 
                                                                   String sessionId) {
        try {
            String fullPath = "/com/aims/presentation/views/" + request.getTargetScreen();
            String screenTitle = extractTitleFromScreen(request.getTargetScreen());
            
            logger.info("UnifiedNavigationManager.executeNavigationWithController: Loading " + fullPath);
            
            // Load the target screen
            Object loadedController = controller.loadContent(fullPath);
            if (loadedController == null) {
                logger.warning("UnifiedNavigationManager.executeNavigationWithController: Failed to load controller");
                return NavigationResult.FAILED_RECOVERABLE;
            }
            
            // Set header title
            controller.setHeaderTitle(screenTitle);
            
            // Inject order data into the loaded controller
            boolean dataInjected = injectOrderDataIntoController(loadedController, request.getOrder(), controller);
            
            if (dataInjected) {
                logger.info("UnifiedNavigationManager.executeNavigationWithController: Navigation and data injection successful");
                return NavigationResult.SUCCESS;
            } else {
                logger.warning("UnifiedNavigationManager.executeNavigationWithController: Navigation successful but data injection failed");
                return NavigationResult.PARTIAL_SUCCESS;
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "UnifiedNavigationManager.executeNavigationWithController: Navigation execution failed", e);
            return NavigationResult.FAILED_RECOVERABLE;
        }
    }
    
    /**
     * Executes emergency navigation when MainLayoutController is not available.
     * 
     * @param request The navigation request
     * @return NavigationResult indicating emergency navigation outcome
     */
    private static NavigationResult executeEmergencyNavigation(NavigationRequest request) {
        try {
            logger.warning("UnifiedNavigationManager.executeEmergencyNavigation: Executing emergency navigation for " + request.getRequestId());
            
            // Use NavigationService as emergency fallback
            OrderEntity order = request.getOrder();
            String targetScreen = request.getTargetScreen();
            
            if (targetScreen.contains("order_summary")) {
                NavigationService.navigateToOrderSummary(order.getOrderId());
            } else if (targetScreen.contains("payment_method")) {
                NavigationService.navigateToPaymentMethod(order.getOrderId());
            } else if (targetScreen.contains("delivery_info")) {
                // Generic navigation without specific method
                NavigationService.navigateTo(request.getTargetScreen(), null, null);
            } else {
                NavigationService.navigateTo(request.getTargetScreen(), null, null);
            }
            
            logger.info("UnifiedNavigationManager.executeEmergencyNavigation: Emergency navigation completed");
            return NavigationResult.DATA_PRESERVED; // Data was preserved but injection uncertain
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "UnifiedNavigationManager.executeEmergencyNavigation: Emergency navigation failed", e);
            return NavigationResult.FAILED_CRITICAL;
        }
    }
    
    /**
     * Injects order data into the loaded controller with type safety.
     * 
     * @param loadedController The controller that was loaded
     * @param order The order data to inject
     * @param mainLayoutController The MainLayoutController for dependency injection
     * @return true if injection was successful, false otherwise
     */
    private static boolean injectOrderDataIntoController(Object loadedController, OrderEntity order, MainLayoutController mainLayoutController) {
        if (loadedController == null || order == null) {
            return false;
        }
        
        try {
            // Inject MainLayoutController dependency if supported
            if (loadedController instanceof MainLayoutController.IChildController && mainLayoutController != null) {
                ((MainLayoutController.IChildController) loadedController).setMainLayoutController(mainLayoutController);
                logger.fine("UnifiedNavigationManager.injectOrderDataIntoController: MainLayoutController injected");
            }
            
            // Inject order data based on controller type
            if (loadedController instanceof OrderSummaryController) {
                ((OrderSummaryController) loadedController).setOrderData(order);
                logger.info("UnifiedNavigationManager.injectOrderDataIntoController: Order data injected into OrderSummaryController");
                return true;
            } else if (loadedController instanceof DeliveryInfoScreenController) {
                ((DeliveryInfoScreenController) loadedController).setOrderData(order);
                logger.info("UnifiedNavigationManager.injectOrderDataIntoController: Order data injected into DeliveryInfoScreenController");
                return true;
            } else if (loadedController instanceof PaymentMethodScreenController) {
                ((PaymentMethodScreenController) loadedController).setOrderData(order);
                logger.info("UnifiedNavigationManager.injectOrderDataIntoController: Order data injected into PaymentMethodScreenController");
                return true;
            } else {
                // Try generic setOrderData method using reflection
                try {
                    loadedController.getClass().getMethod("setOrderData", OrderEntity.class).invoke(loadedController, order);
                    logger.info("UnifiedNavigationManager.injectOrderDataIntoController: Order data injected via reflection");
                    return true;
                } catch (Exception reflectionException) {
                    logger.warning("UnifiedNavigationManager.injectOrderDataIntoController: Failed to inject order data via reflection: " + 
                        reflectionException.getMessage());
                    return false;
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "UnifiedNavigationManager.injectOrderDataIntoController: Data injection failed", e);
            return false;
        }
    }
    
    /**
     * Extracts a user-friendly title from the screen filename.
     * 
     * @param screenFilename The FXML filename
     * @return A formatted title string
     */
    private static String extractTitleFromScreen(String screenFilename) {
        if (screenFilename == null) return "AIMS";
        
        if (screenFilename.contains("order_summary")) return "Order Summary";
        if (screenFilename.contains("payment_method")) return "Payment Method";
        if (screenFilename.contains("delivery_info")) return "Delivery Information";
        
        // Generic title extraction
        String name = screenFilename.replace(".fxml", "").replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    
    /**
     * Records the navigation result and updates statistics.
     * 
     * @param result The navigation result
     * @param error The error (if any)
     * @return The recorded result
     */
    private static NavigationResult recordResult(NavigationResult result, Exception error) {
        lastNavigationResult = result;
        lastNavigationError = error;
        
        if (result.isSuccess()) {
            successfulNavigations++;
        } else {
            failedNavigations++;
        }
        
        logger.info("UnifiedNavigationManager.recordResult: Navigation result: " + result + 
            " (Success: " + successfulNavigations + ", Failed: " + failedNavigations + ")");
        
        return result;
    }
    
    /**
     * Gets comprehensive debug information about the navigation manager state.
     * 
     * @return String containing detailed navigation state information
     */
    public static String getNavigationDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("UnifiedNavigationManager Debug Info:\n");
        info.append("Last Request: ").append(lastNavigationRequest != null ? 
            lastNavigationRequest.getRequestId() + " -> " + lastNavigationRequest.getTargetScreen() : "none").append("\n");
        info.append("Last Result: ").append(lastNavigationResult != null ? lastNavigationResult : "none").append("\n");
        info.append("Last Error: ").append(lastNavigationError != null ? lastNavigationError.getMessage() : "none").append("\n");
        info.append("Success Count: ").append(successfulNavigations).append("\n");
        info.append("Failure Count: ").append(failedNavigations).append("\n");
        info.append("Success Rate: ").append(getSuccessRate()).append("%\n");
        info.append("MainLayoutController Available: ").append(MainLayoutControllerRegistry.isAvailable()).append("\n");
        return info.toString();
    }
    
    /**
     * Gets the current navigation success rate.
     * 
     * @return Success rate as a percentage
     */
    public static double getSuccessRate() {
        int total = successfulNavigations + failedNavigations;
        if (total == 0) return 100.0;
        return (double) successfulNavigations / total * 100.0;
    }
    
    /**
     * Resets navigation statistics (for testing purposes).
     */
    public static void resetStatistics() {
        successfulNavigations = 0;
        failedNavigations = 0;
        lastNavigationRequest = null;
        lastNavigationResult = null;
        lastNavigationError = null;
        logger.info("UnifiedNavigationManager.resetStatistics: Statistics reset");
    }
    
    /**
     * Gets the last navigation request (for debugging).
     * 
     * @return The last navigation request, or null if none
     */
    public static NavigationRequest getLastNavigationRequest() {
        return lastNavigationRequest;
    }
    
    /**
     * Gets the last navigation result (for monitoring).
     * 
     * @return The last navigation result, or null if none
     */
    public static NavigationResult getLastNavigationResult() {
        return lastNavigationResult;
    }
}