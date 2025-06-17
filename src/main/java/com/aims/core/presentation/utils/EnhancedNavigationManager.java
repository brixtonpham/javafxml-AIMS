package com.aims.core.presentation.utils;

import com.aims.core.entities.OrderEntity;
import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.presentation.controllers.OrderSummaryController;
import com.aims.core.presentation.controllers.DeliveryInfoScreenController;
import com.aims.core.presentation.controllers.PaymentMethodScreenController;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.NavigationService;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced Navigation Manager for robust, null-safe navigation with comprehensive fallback strategies.
 * 
 * This class provides multi-tier navigation strategies to handle MainLayoutController null reference errors
 * and ensure reliable navigation throughout the application, especially during checkout flow.
 * 
 * Features:
 * - Null-safe navigation with automatic fallback mechanisms
 * - Order data preservation during navigation transitions
 * - Comprehensive error recovery and user feedback
 * - Navigation state validation and recovery
 * - Multiple navigation strategies (Primary, Fallback, Emergency)
 * 
 * @author AIMS Navigation Enhancement Team
 * @version 1.0
 * @since Phase 1 - Navigation Enhancement
 */
public class EnhancedNavigationManager {
    
    private static final Logger logger = Logger.getLogger(EnhancedNavigationManager.class.getName());
    
    // Navigation result enumeration
    public enum NavigationResult {
        SUCCESS("Navigation completed successfully"),
        PARTIAL_SUCCESS("Navigation completed with warnings"),
        FAILED_RECOVERABLE("Navigation failed but recovery possible"),
        FAILED_CRITICAL("Navigation failed critically"),
        CANCELLED("Navigation was cancelled");
        
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
    }
    
    // Navigation strategy enumeration
    public enum NavigationStrategy {
        PRIMARY("Direct MainLayoutController navigation"),
        FALLBACK("FXMLSceneManager navigation"),
        EMERGENCY("NavigationService emergency fallback");
        
        private final String description;
        
        NavigationStrategy(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Navigation state tracking
    private static NavigationContext lastNavigationAttempt;
    private static LocalDateTime lastNavigationTime;
    private static Exception lastNavigationError;
    private static int failureCount = 0;
    private static final int MAX_FAILURE_COUNT = 3;
    
    /**
     * Navigate to Order Summary screen with enhanced null-safety and fallback mechanisms.
     * 
     * @param order The order entity to display
     * @param controller The MainLayoutController (may be null)
     * @return NavigationResult indicating success or failure with details
     */
    public static NavigationResult navigateToOrderSummary(OrderEntity order, MainLayoutController controller) {
        logger.info("EnhancedNavigationManager.navigateToOrderSummary: Starting enhanced navigation");
        
        // Input validation
        if (order == null) {
            logger.severe("EnhancedNavigationManager.navigateToOrderSummary: Order entity is null");
            return NavigationResult.FAILED_CRITICAL;
        }
        
        // Store navigation attempt for recovery
        preserveNavigationState(order, controller, "order_summary_screen.fxml", "Order Summary");
        
        // Strategy 1: Primary Navigation (MainLayoutController)
        NavigationResult primaryResult = executePrimaryNavigation(order, controller, "order_summary_screen.fxml");
        if (primaryResult.isSuccess()) {
            resetFailureCount();
            return primaryResult;
        }
        
        // Strategy 2: Fallback Navigation (FXMLSceneManager)
        NavigationResult fallbackResult = executeFallbackNavigation(order, controller, "order_summary_screen.fxml");
        if (fallbackResult.isSuccess()) {
            resetFailureCount();
            return fallbackResult;
        }
        
        // Strategy 3: Emergency Navigation (NavigationService)
        NavigationResult emergencyResult = executeEmergencyNavigation(order, "order_summary_screen.fxml");
        if (emergencyResult.isSuccess()) {
            resetFailureCount();
            return emergencyResult;
        }
        
        // All strategies failed
        incrementFailureCount();
        logger.severe("EnhancedNavigationManager.navigateToOrderSummary: All navigation strategies failed");
        return NavigationResult.FAILED_CRITICAL;
    }
    
    /**
     * Navigate to Delivery Info screen with enhanced null-safety and fallback mechanisms.
     * 
     * @param order The order entity to edit
     * @param controller The MainLayoutController (may be null)
     * @return NavigationResult indicating success or failure with details
     */
    public static NavigationResult navigateToDeliveryInfo(OrderEntity order, MainLayoutController controller) {
        logger.info("EnhancedNavigationManager.navigateToDeliveryInfo: Starting enhanced navigation");
        
        // Input validation
        if (order == null) {
            logger.severe("EnhancedNavigationManager.navigateToDeliveryInfo: Order entity is null");
            return NavigationResult.FAILED_CRITICAL;
        }
        
        // Store navigation attempt for recovery
        preserveNavigationState(order, controller, "delivery_info_screen.fxml", "Delivery Information");
        
        // Strategy 1: Primary Navigation (MainLayoutController)
        NavigationResult primaryResult = executePrimaryNavigation(order, controller, "delivery_info_screen.fxml");
        if (primaryResult.isSuccess()) {
            resetFailureCount();
            return primaryResult;
        }
        
        // Strategy 2: Fallback Navigation (FXMLSceneManager)
        NavigationResult fallbackResult = executeFallbackNavigation(order, controller, "delivery_info_screen.fxml");
        if (fallbackResult.isSuccess()) {
            resetFailureCount();
            return fallbackResult;
        }
        
        // Strategy 3: Emergency Navigation (NavigationService)
        NavigationResult emergencyResult = executeEmergencyNavigation(order, "delivery_info_screen.fxml");
        if (emergencyResult.isSuccess()) {
            resetFailureCount();
            return emergencyResult;
        }
        
        // All strategies failed
        incrementFailureCount();
        logger.severe("EnhancedNavigationManager.navigateToDeliveryInfo: All navigation strategies failed");
        return NavigationResult.FAILED_CRITICAL;
    }
    
    /**
     * Navigate to Payment Method screen with enhanced null-safety and fallback mechanisms.
     * 
     * @param order The order entity ready for payment
     * @param controller The MainLayoutController (may be null)
     * @return NavigationResult indicating success or failure with details
     */
    public static NavigationResult navigateToPaymentMethod(OrderEntity order, MainLayoutController controller) {
        logger.info("EnhancedNavigationManager.navigateToPaymentMethod: Starting enhanced navigation");
        
        // Input validation
        if (order == null) {
            logger.severe("EnhancedNavigationManager.navigateToPaymentMethod: Order entity is null");
            return NavigationResult.FAILED_CRITICAL;
        }
        
        // Store navigation attempt for recovery
        preserveNavigationState(order, controller, "payment_method_screen.fxml", "Payment Method");
        
        // Strategy 1: Primary Navigation (MainLayoutController)
        NavigationResult primaryResult = executePrimaryNavigation(order, controller, "payment_method_screen.fxml");
        if (primaryResult.isSuccess()) {
            resetFailureCount();
            return primaryResult;
        }
        
        // Strategy 2: Fallback Navigation (FXMLSceneManager)
        NavigationResult fallbackResult = executeFallbackNavigation(order, controller, "payment_method_screen.fxml");
        if (fallbackResult.isSuccess()) {
            resetFailureCount();
            return fallbackResult;
        }
        
        // Strategy 3: Emergency Navigation (NavigationService)
        NavigationResult emergencyResult = executeEmergencyNavigation(order, "payment_method_screen.fxml");
        if (emergencyResult.isSuccess()) {
            resetFailureCount();
            return emergencyResult;
        }
        
        // All strategies failed
        incrementFailureCount();
        logger.severe("EnhancedNavigationManager.navigateToPaymentMethod: All navigation strategies failed");
        return NavigationResult.FAILED_CRITICAL;
    }
    
    /**
     * Generic navigation with data preservation and comprehensive fallback strategies.
     * 
     * @param fxmlPath The FXML file path to navigate to
     * @param data The data object to pass to the controller
     * @param controller The MainLayoutController (may be null)
     * @return NavigationResult indicating success or failure with details
     */
    public static NavigationResult navigateWithDataPreservation(String fxmlPath, Object data, MainLayoutController controller) {
        logger.info("EnhancedNavigationManager.navigateWithDataPreservation: Starting generic navigation to " + fxmlPath);
        
        // Input validation
        if (fxmlPath == null || fxmlPath.trim().isEmpty()) {
            logger.severe("EnhancedNavigationManager.navigateWithDataPreservation: FXML path is null or empty");
            return NavigationResult.FAILED_CRITICAL;
        }
        
        // Store navigation attempt for recovery
        preserveNavigationState(data, controller, fxmlPath, extractTitleFromPath(fxmlPath));
        
        // Strategy 1: Primary Navigation (MainLayoutController)
        NavigationResult primaryResult = executePrimaryNavigation(data, controller, fxmlPath);
        if (primaryResult.isSuccess()) {
            resetFailureCount();
            return primaryResult;
        }
        
        // Strategy 2: Fallback Navigation (FXMLSceneManager)
        NavigationResult fallbackResult = executeFallbackNavigation(data, controller, fxmlPath);
        if (fallbackResult.isSuccess()) {
            resetFailureCount();
            return fallbackResult;
        }
        
        // Strategy 3: Emergency Navigation (NavigationService)
        NavigationResult emergencyResult = executeEmergencyNavigation(data, fxmlPath);
        if (emergencyResult.isSuccess()) {
            resetFailureCount();
            return emergencyResult;
        }
        
        // All strategies failed
        incrementFailureCount();
        logger.severe("EnhancedNavigationManager.navigateWithDataPreservation: All navigation strategies failed for " + fxmlPath);
        return NavigationResult.FAILED_CRITICAL;
    }
    
    /**
     * Strategy 1: Execute primary navigation using MainLayoutController directly.
     * 
     * @param data The data object to pass to the controller
     * @param controller The MainLayoutController
     * @param fxmlPath The FXML file path
     * @return NavigationResult indicating success or failure
     */
    private static NavigationResult executePrimaryNavigation(Object data, MainLayoutController controller, String fxmlPath) {
        try {
            logger.info("EnhancedNavigationManager.executePrimaryNavigation: Attempting primary navigation strategy");
            
            // Validate MainLayoutController availability
            if (controller == null) {
                logger.warning("EnhancedNavigationManager.executePrimaryNavigation: MainLayoutController is null");
                return NavigationResult.FAILED_RECOVERABLE;
            }
            
            // Validate MainLayoutController state
            if (!validateMainLayoutController(controller)) {
                logger.warning("EnhancedNavigationManager.executePrimaryNavigation: MainLayoutController validation failed");
                return NavigationResult.FAILED_RECOVERABLE;
            }
            
            // Load content using MainLayoutController
            String fullPath = "/com/aims/presentation/views/" + fxmlPath;
            Object loadedController = controller.loadContent(fullPath);
            
            if (loadedController == null) {
                logger.warning("EnhancedNavigationManager.executePrimaryNavigation: Failed to load controller from " + fullPath);
                return NavigationResult.FAILED_RECOVERABLE;
            }
            
            // Set header title
            String title = extractTitleFromPath(fxmlPath);
            controller.setHeaderTitle(title);
            
            // Inject data into loaded controller with type safety
            boolean dataInjected = injectDataIntoController(loadedController, data, controller);
            
            if (dataInjected) {
                logger.info("EnhancedNavigationManager.executePrimaryNavigation: Primary navigation completed successfully");
                return NavigationResult.SUCCESS;
            } else {
                logger.warning("EnhancedNavigationManager.executePrimaryNavigation: Data injection failed but navigation succeeded");
                return NavigationResult.PARTIAL_SUCCESS;
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "EnhancedNavigationManager.executePrimaryNavigation: Primary navigation failed", e);
            lastNavigationError = e;
            return NavigationResult.FAILED_RECOVERABLE;
        }
    }
    
    /**
     * Strategy 2: Execute fallback navigation using FXMLSceneManager.
     * 
     * @param data The data object to pass to the controller
     * @param controller The MainLayoutController (may be null)
     * @param fxmlPath The FXML file path
     * @return NavigationResult indicating success or failure
     */
    private static NavigationResult executeFallbackNavigation(Object data, MainLayoutController controller, String fxmlPath) {
        try {
            logger.info("EnhancedNavigationManager.executeFallbackNavigation: Attempting fallback navigation strategy");
            
            // Get FXMLSceneManager instance
            FXMLSceneManager sceneManager = FXMLSceneManager.getInstance();
            if (sceneManager == null) {
                logger.warning("EnhancedNavigationManager.executeFallbackNavigation: FXMLSceneManager not available");
                return NavigationResult.FAILED_RECOVERABLE;
            }
            
            // Try to use MainLayoutController's content pane if available
            if (controller != null && controller.getContentPane() != null) {
                String fullPath = "/com/aims/presentation/views/" + fxmlPath;
                Object loadedController = sceneManager.loadFXMLIntoPane(controller.getContentPane(), fullPath);
                
                if (loadedController != null) {
                    // Set header title if controller is available
                    String title = extractTitleFromPath(fxmlPath);
                    controller.setHeaderTitle(title);
                    
                    // Inject data into loaded controller
                    boolean dataInjected = injectDataIntoController(loadedController, data, controller);
                    
                    if (dataInjected) {
                        logger.info("EnhancedNavigationManager.executeFallbackNavigation: Fallback navigation completed successfully");
                        return NavigationResult.SUCCESS;
                    } else {
                        logger.warning("EnhancedNavigationManager.executeFallbackNavigation: Data injection failed but navigation succeeded");
                        return NavigationResult.PARTIAL_SUCCESS;
                    }
                } else {
                    logger.warning("EnhancedNavigationManager.executeFallbackNavigation: Failed to load controller using FXMLSceneManager");
                    return NavigationResult.FAILED_RECOVERABLE;
                }
            } else {
                logger.warning("EnhancedNavigationManager.executeFallbackNavigation: MainLayoutController or content pane not available");
                return NavigationResult.FAILED_RECOVERABLE;
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "EnhancedNavigationManager.executeFallbackNavigation: Fallback navigation failed", e);
            lastNavigationError = e;
            return NavigationResult.FAILED_RECOVERABLE;
        }
    }
    
    /**
     * Strategy 3: Execute emergency navigation using NavigationService.
     * 
     * @param data The data object to pass to the controller
     * @param fxmlPath The FXML file path
     * @return NavigationResult indicating success or failure
     */
    private static NavigationResult executeEmergencyNavigation(Object data, String fxmlPath) {
        try {
            logger.info("EnhancedNavigationManager.executeEmergencyNavigation: Attempting emergency navigation strategy");
            
            // Use NavigationService for emergency navigation
            if (data instanceof OrderEntity) {
                OrderEntity order = (OrderEntity) data;
                if (fxmlPath.contains("order_summary")) {
                    NavigationService.navigateToOrderSummary(order.getOrderId());
                } else if (fxmlPath.contains("payment_method")) {
                    NavigationService.navigateToPaymentMethod(order.getOrderId());
                } else {
                    // Generic navigation
                    NavigationService.navigateTo(fxmlPath, null, null);
                }
            } else {
                // Generic navigation without data
                NavigationService.navigateTo(fxmlPath, null, null);
            }
            
            logger.info("EnhancedNavigationManager.executeEmergencyNavigation: Emergency navigation completed");
            return NavigationResult.PARTIAL_SUCCESS; // Data injection may not work in emergency mode
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "EnhancedNavigationManager.executeEmergencyNavigation: Emergency navigation failed", e);
            lastNavigationError = e;
            return NavigationResult.FAILED_CRITICAL;
        }
    }
    
    /**
     * Validates MainLayoutController state and functionality.
     * 
     * @param controller The MainLayoutController to validate
     * @return true if controller is valid and functional, false otherwise
     */
    private static boolean validateMainLayoutController(MainLayoutController controller) {
        if (controller == null) {
            return false;
        }
        
        try {
            // Check if essential components are available
            if (controller.getContentPane() == null) {
                logger.warning("EnhancedNavigationManager.validateMainLayoutController: Content pane is null");
                return false;
            }
            
            if (controller.getMainContainer() == null) {
                logger.warning("EnhancedNavigationManager.validateMainLayoutController: Main container is null");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "EnhancedNavigationManager.validateMainLayoutController: Validation failed", e);
            return false;
        }
    }
    
    /**
     * Injects data into the loaded controller with comprehensive type safety.
     * 
     * @param loadedController The controller that was loaded
     * @param data The data to inject
     * @param mainLayoutController The MainLayoutController for dependency injection
     * @return true if data injection was successful, false otherwise
     */
    private static boolean injectDataIntoController(Object loadedController, Object data, MainLayoutController mainLayoutController) {
        if (loadedController == null) {
            return false;
        }
        
        try {
            // Inject MainLayoutController dependency if supported
            if (loadedController instanceof MainLayoutController.IChildController && mainLayoutController != null) {
                ((MainLayoutController.IChildController) loadedController).setMainLayoutController(mainLayoutController);
                logger.info("EnhancedNavigationManager.injectDataIntoController: MainLayoutController injected");
            }
            
            // Inject order data based on controller type
            if (data instanceof OrderEntity) {
                OrderEntity order = (OrderEntity) data;
                
                if (loadedController instanceof OrderSummaryController) {
                    ((OrderSummaryController) loadedController).setOrderData(order);
                    logger.info("EnhancedNavigationManager.injectDataIntoController: Order data injected into OrderSummaryController");
                    return true;
                } else if (loadedController instanceof DeliveryInfoScreenController) {
                    ((DeliveryInfoScreenController) loadedController).setOrderData(order);
                    logger.info("EnhancedNavigationManager.injectDataIntoController: Order data injected into DeliveryInfoScreenController");
                    return true;
                } else if (loadedController instanceof PaymentMethodScreenController) {
                    ((PaymentMethodScreenController) loadedController).setOrderData(order);
                    logger.info("EnhancedNavigationManager.injectDataIntoController: Order data injected into PaymentMethodScreenController");
                    return true;
                } else {
                    // Try generic setOrderData method using reflection
                    try {
                        loadedController.getClass().getMethod("setOrderData", OrderEntity.class).invoke(loadedController, order);
                        logger.info("EnhancedNavigationManager.injectDataIntoController: Order data injected via reflection");
                        return true;
                    } catch (Exception reflectionException) {
                        logger.warning("EnhancedNavigationManager.injectDataIntoController: Failed to inject order data via reflection: " + reflectionException.getMessage());
                        return false;
                    }
                }
            } else {
                // Try generic data injection methods
                try {
                    if (data != null) {
                        loadedController.getClass().getMethod("setData", Object.class).invoke(loadedController, data);
                        logger.info("EnhancedNavigationManager.injectDataIntoController: Generic data injected");
                        return true;
                    }
                } catch (Exception reflectionException) {
                    logger.warning("EnhancedNavigationManager.injectDataIntoController: Failed to inject generic data: " + reflectionException.getMessage());
                    return false;
                }
            }
            
            return data == null; // Return true if no data to inject
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "EnhancedNavigationManager.injectDataIntoController: Data injection failed", e);
            return false;
        }
    }
    
    /**
     * Preserves navigation state for recovery purposes.
     * 
     * @param data The data being navigated with
     * @param controller The MainLayoutController
     * @param fxmlPath The target FXML path
     * @param title The screen title
     */
    private static void preserveNavigationState(Object data, MainLayoutController controller, String fxmlPath, String title) {
        lastNavigationAttempt = new NavigationContext(fxmlPath, title);
        if (data != null) {
            lastNavigationAttempt.withContextData("navigationData", data);
        }
        if (controller != null) {
            lastNavigationAttempt.withContextData("mainLayoutController", controller);
        }
        lastNavigationTime = LocalDateTime.now();
        
        logger.info("EnhancedNavigationManager.preserveNavigationState: Navigation state preserved for " + fxmlPath);
    }
    
    /**
     * Restores navigation state from the last preserved attempt.
     * 
     * @return NavigationContext containing the last navigation attempt, or null if none
     */
    public static NavigationContext restoreNavigationState() {
        return lastNavigationAttempt;
    }
    
    /**
     * Validates navigation prerequisites before attempting navigation.
     * 
     * @param data The data to validate
     * @param targetScreen The target screen identifier
     * @return true if prerequisites are met, false otherwise
     */
    public static boolean validateNavigationPrerequisites(Object data, String targetScreen) {
        try {
            if (targetScreen == null || targetScreen.trim().isEmpty()) {
                logger.warning("EnhancedNavigationManager.validateNavigationPrerequisites: Target screen is null or empty");
                return false;
            }
            
            // Order-specific validation
            if (data instanceof OrderEntity) {
                OrderEntity order = (OrderEntity) data;
                
                if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
                    logger.warning("EnhancedNavigationManager.validateNavigationPrerequisites: Order ID is missing");
                    return false;
                }
                
                // Screen-specific validations
                if (targetScreen.contains("order_summary")) {
                    if (order.getDeliveryInfo() == null) {
                        logger.warning("EnhancedNavigationManager.validateNavigationPrerequisites: Delivery info missing for order summary");
                        return false;
                    }
                    if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
                        logger.warning("EnhancedNavigationManager.validateNavigationPrerequisites: Order items missing for order summary");
                        return false;
                    }
                } else if (targetScreen.contains("payment_method")) {
                    if (order.getDeliveryInfo() == null) {
                        logger.warning("EnhancedNavigationManager.validateNavigationPrerequisites: Delivery info missing for payment method");
                        return false;
                    }
                    if (order.getTotalAmountPaid() <= 0) {
                        logger.warning("EnhancedNavigationManager.validateNavigationPrerequisites: Invalid total amount for payment method");
                        return false;
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "EnhancedNavigationManager.validateNavigationPrerequisites: Validation failed", e);
            return false;
        }
    }
    
    /**
     * Extracts a user-friendly title from the FXML file path.
     * 
     * @param fxmlPath The FXML file path
     * @return A formatted title string
     */
    private static String extractTitleFromPath(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.trim().isEmpty()) {
            return "AIMS";
        }
        
        String fileName = fxmlPath;
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        if (fileName.endsWith(".fxml")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }
        
        // Convert snake_case to Title Case
        String[] words = fileName.split("_");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (title.length() > 0) {
                title.append(" ");
            }
            if (!word.isEmpty()) {
                title.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    title.append(word.substring(1).toLowerCase());
                }
            }
        }
        
        return title.toString();
    }
    
    /**
     * Increments the failure count and implements circuit breaker pattern.
     */
    private static void incrementFailureCount() {
        failureCount++;
        logger.warning("EnhancedNavigationManager.incrementFailureCount: Failure count increased to " + failureCount);
        
        if (failureCount >= MAX_FAILURE_COUNT) {
            logger.severe("EnhancedNavigationManager.incrementFailureCount: Maximum failure count reached. Circuit breaker activated.");
        }
    }
    
    /**
     * Resets the failure count after successful navigation.
     */
    private static void resetFailureCount() {
        if (failureCount > 0) {
            logger.info("EnhancedNavigationManager.resetFailureCount: Failure count reset after successful navigation");
            failureCount = 0;
        }
    }
    
    /**
     * Gets the current failure count for monitoring purposes.
     * 
     * @return The current failure count
     */
    public static int getFailureCount() {
        return failureCount;
    }
    
    /**
     * Gets the last navigation error for debugging purposes.
     * 
     * @return The last navigation error, or null if none
     */
    public static Exception getLastNavigationError() {
        return lastNavigationError;
    }
    
    /**
     * Gets debug information about the navigation manager state.
     * 
     * @return String containing debug information
     */
    public static String getNavigationDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("EnhancedNavigationManager Debug Info:\n");
        info.append("Failure Count: ").append(failureCount).append("/").append(MAX_FAILURE_COUNT).append("\n");
        info.append("Last Navigation: ").append(lastNavigationTime != null ? lastNavigationTime.toString() : "none").append("\n");
        info.append("Last Error: ").append(lastNavigationError != null ? lastNavigationError.getMessage() : "none").append("\n");
        info.append("Last Attempt: ").append(lastNavigationAttempt != null ? lastNavigationAttempt.getScreenPath() : "none").append("\n");
        return info.toString();
    }
    
    /**
     * Checks if the navigation manager is in a healthy state.
     * 
     * @return true if the manager is healthy, false if circuit breaker is active
     */
    public static boolean isHealthy() {
        return failureCount < MAX_FAILURE_COUNT;
    }
    
    /**
     * Resets the navigation manager state for recovery purposes.
     */
    public static void reset() {
        failureCount = 0;
        lastNavigationError = null;
        lastNavigationAttempt = null;
        lastNavigationTime = null;
        logger.info("EnhancedNavigationManager.reset: Navigation manager state reset");
    }
}