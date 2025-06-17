package com.aims.core.presentation.utils;

import com.aims.core.entities.OrderEntity;
import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.presentation.controllers.OrderSummaryController;
import com.aims.core.presentation.controllers.DeliveryInfoScreenController;
import com.aims.core.presentation.controllers.PaymentMethodScreenController;
import com.aims.core.shared.NavigationService;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.application.services.IOrderDataLoaderService;
import com.aims.core.application.services.ICartDataValidationService;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.time.LocalDateTime;

/**
 * Enhanced Checkout Navigation Wrapper with integration to Enhanced Navigation Manager and Order Data Context Manager.
 *
 * This class provides specialized navigation for checkout flow with comprehensive data validation,
 * order data injection, controller type safety, and enhanced error handling.
 *
 * Features:
 * - Integration with EnhancedNavigationManager for robust navigation
 * - Order data preservation using OrderDataContextManager
 * - Enhanced service injection and validation
 * - Comprehensive error handling and recovery
 * - Session-based data persistence during navigation
 * - Controller type safety and validation
 *
 * @author AIMS Navigation Enhancement Team
 * @version 2.0 - Enhanced Integration
 * @since Phase 1 - Navigation Enhancement
 */
public class CheckoutNavigationWrapper {
    
    private static final Logger logger = Logger.getLogger(CheckoutNavigationWrapper.class.getName());
    
    // Enhanced navigation and data management
    private static String currentSessionId;
    private static LocalDateTime lastNavigationTime;
    private static EnhancedNavigationManager.NavigationResult lastNavigationResult;
    
    // Enhanced services
    private static IOrderDataLoaderService orderDataLoaderService;
    private static ICartDataValidationService cartDataValidationService;
    
    // Legacy compatibility
    private static OrderEntity lastOrderData;
    private static MainLayoutController lastMainLayoutController;
    private static Exception lastNavigationError;
    
    // Initialize enhanced services
    static {
        initializeEnhancedServices();
    }
    
    /**
     * Initialize enhanced services for data validation and loading
     */
    private static void initializeEnhancedServices() {
        try {
            ServiceFactory serviceFactory = ServiceFactory.getInstance();
            orderDataLoaderService = serviceFactory.getOrderDataLoaderService();
            cartDataValidationService = serviceFactory.getCartDataValidationService();
            logger.info("CheckoutNavigationWrapper: Enhanced services initialized successfully");
        } catch (Exception e) {
            logger.log(Level.WARNING, "CheckoutNavigationWrapper: Failed to initialize enhanced services", e);
        }
    }
    
    /**
     * Navigate from delivery info screen to order summary with enhanced navigation and data management.
     *
     * @param order The order entity with delivery information
     * @param mainLayoutController The main layout controller
     * @param sourceController The source delivery info controller
     * @return true if navigation succeeded, false otherwise
     */
    public static boolean navigateToOrderSummary(OrderEntity order, MainLayoutController mainLayoutController,
                                                DeliveryInfoScreenController sourceController) {
        logger.info("CheckoutNavigationWrapper.navigateToOrderSummary: Starting enhanced navigation to order summary");
        
        try {
            // Create session for order data persistence
            currentSessionId = OrderDataContextManager.generateSessionId(order != null ? order.getOrderId() : null);
            
            // Preserve order data using enhanced context manager
            if (order != null) {
                OrderDataContextManager.preserveOrderData(currentSessionId, order);
                logger.info("CheckoutNavigationWrapper.navigateToOrderSummary: Order data preserved in session " + currentSessionId);
            }
            
            // Validate order data before navigation
            if (!validateOrderForNavigation(order, "order_summary")) {
                logger.warning("CheckoutNavigationWrapper.navigateToOrderSummary: Order validation failed");
                return false;
            }
            
            // Use Enhanced Navigation Manager for robust navigation
            EnhancedNavigationManager.NavigationResult result =
                EnhancedNavigationManager.navigateToOrderSummary(order, mainLayoutController);
            
            lastNavigationResult = result;
            lastNavigationTime = LocalDateTime.now();
            
            if (result.isSuccess()) {
                logger.info("CheckoutNavigationWrapper.navigateToOrderSummary: Enhanced navigation succeeded with result: " + result);
                
                // Inject enhanced services into the loaded controller if possible
                injectEnhancedServicesIntoController(mainLayoutController, "order_summary_screen.fxml");
                
                // Preserve legacy compatibility
                preserveOrderData(order, mainLayoutController);
                
                return true;
            } else {
                logger.warning("CheckoutNavigationWrapper.navigateToOrderSummary: Enhanced navigation failed with result: " + result);
                
                // Attempt legacy fallback navigation
                return attemptLegacyFallbackNavigation(order, mainLayoutController, sourceController, "order_summary");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "CheckoutNavigationWrapper.navigateToOrderSummary: Critical error in enhanced navigation", e);
            lastNavigationError = e;
            
            // Attempt legacy fallback as last resort
            return attemptLegacyFallbackNavigation(order, mainLayoutController, sourceController, "order_summary");
        }
    }
    
    /**
     * Navigate from order summary to payment method with enhanced navigation and data management.
     *
     * @param order The order entity ready for payment
     * @param mainLayoutController The main layout controller
     * @param sourceController The source order summary controller
     * @return true if navigation succeeded, false otherwise
     */
    public static boolean navigateToPaymentMethod(OrderEntity order, MainLayoutController mainLayoutController,
                                                OrderSummaryController sourceController) {
        logger.info("CheckoutNavigationWrapper.navigateToPaymentMethod: Starting enhanced navigation to payment method");
        
        try {
            // Use existing session or create new one
            if (currentSessionId == null) {
                currentSessionId = OrderDataContextManager.generateSessionId(order != null ? order.getOrderId() : null);
            }
            
            // Update order data in session
            if (order != null) {
                OrderDataContextManager.preserveOrderData(currentSessionId, order);
                logger.info("CheckoutNavigationWrapper.navigateToPaymentMethod: Order data updated in session " + currentSessionId);
            }
            
            // Validate order data for payment
            if (!validateOrderForNavigation(order, "payment_method")) {
                logger.warning("CheckoutNavigationWrapper.navigateToPaymentMethod: Order validation failed for payment");
                return false;
            }
            
            // Use Enhanced Navigation Manager for robust navigation
            EnhancedNavigationManager.NavigationResult result =
                EnhancedNavigationManager.navigateToPaymentMethod(order, mainLayoutController);
            
            lastNavigationResult = result;
            lastNavigationTime = LocalDateTime.now();
            
            if (result.isSuccess()) {
                logger.info("CheckoutNavigationWrapper.navigateToPaymentMethod: Enhanced navigation succeeded with result: " + result);
                
                // Inject enhanced services into the loaded controller if possible
                injectEnhancedServicesIntoController(mainLayoutController, "payment_method_screen.fxml");
                
                // Preserve legacy compatibility
                preserveOrderData(order, mainLayoutController);
                
                return true;
            } else {
                logger.warning("CheckoutNavigationWrapper.navigateToPaymentMethod: Enhanced navigation failed with result: " + result);
                
                // Attempt legacy fallback navigation
                return attemptLegacyFallbackNavigation(order, mainLayoutController, sourceController, "payment_method");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "CheckoutNavigationWrapper.navigateToPaymentMethod: Critical error in enhanced navigation", e);
            lastNavigationError = e;
            
            // Attempt legacy fallback as last resort
            return attemptLegacyFallbackNavigation(order, mainLayoutController, sourceController, "payment_method");
        }
    }
    
    /**
     * Navigate back to delivery info with enhanced data preservation.
     *
     * @param order The order entity to edit
     * @param mainLayoutController The main layout controller
     * @param sourceController The source controller
     * @return true if navigation succeeded, false otherwise
     */
    public static boolean navigateToDeliveryInfo(OrderEntity order, MainLayoutController mainLayoutController,
                                                Object sourceController) {
        logger.info("CheckoutNavigationWrapper.navigateToDeliveryInfo: Starting enhanced navigation to delivery info");
        
        try {
            // Use existing session or create new one
            if (currentSessionId == null) {
                currentSessionId = OrderDataContextManager.generateSessionId(order != null ? order.getOrderId() : null);
            }
            
            // Preserve order data in session
            if (order != null) {
                OrderDataContextManager.preserveOrderData(currentSessionId, order);
                logger.info("CheckoutNavigationWrapper.navigateToDeliveryInfo: Order data preserved in session " + currentSessionId);
            }
            
            // Use Enhanced Navigation Manager for robust navigation
            EnhancedNavigationManager.NavigationResult result =
                EnhancedNavigationManager.navigateToDeliveryInfo(order, mainLayoutController);
            
            lastNavigationResult = result;
            lastNavigationTime = LocalDateTime.now();
            
            if (result.isSuccess()) {
                logger.info("CheckoutNavigationWrapper.navigateToDeliveryInfo: Enhanced navigation succeeded with result: " + result);
                
                // Inject enhanced services into the loaded controller if possible
                injectEnhancedServicesIntoController(mainLayoutController, "delivery_info_screen.fxml");
                
                // Preserve legacy compatibility
                preserveOrderData(order, mainLayoutController);
                
                return true;
            } else {
                logger.warning("CheckoutNavigationWrapper.navigateToDeliveryInfo: Enhanced navigation failed with result: " + result);
                
                // Attempt legacy fallback navigation
                return attemptLegacyFallbackNavigation(order, mainLayoutController, sourceController, "delivery_info");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "CheckoutNavigationWrapper.navigateToDeliveryInfo: Critical error in enhanced navigation", e);
            lastNavigationError = e;
            
            // Attempt legacy fallback as last resort
            return attemptLegacyFallbackNavigation(order, mainLayoutController, sourceController, "delivery_info");
        }
    }
    
    /**
     * Validates order data for navigation to specific screen.
     *
     * @param order The order entity to validate
     * @param targetScreen The target screen identifier
     * @return true if order is valid for navigation, false otherwise
     */
    private static boolean validateOrderForNavigation(OrderEntity order, String targetScreen) {
        if (order == null) {
            logger.warning("CheckoutNavigationWrapper.validateOrderForNavigation: Order is null");
            return false;
        }
        
        try {
            // Use enhanced validation if available
            if (orderDataLoaderService != null) {
                boolean isComplete = orderDataLoaderService.validateOrderDataCompleteness(order);
                if (!isComplete) {
                    logger.warning("CheckoutNavigationWrapper.validateOrderForNavigation: Order data completeness validation failed");
                    return false;
                }
            }
            
            // Screen-specific validation
            if ("payment_method".equals(targetScreen)) {
                if (order.getDeliveryInfo() == null) {
                    logger.warning("CheckoutNavigationWrapper.validateOrderForNavigation: Delivery info missing for payment method screen");
                    return false;
                }
                if (order.getTotalAmountPaid() <= 0) {
                    logger.warning("CheckoutNavigationWrapper.validateOrderForNavigation: Invalid total amount for payment method screen");
                    return false;
                }
            } else if ("order_summary".equals(targetScreen)) {
                if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
                    logger.warning("CheckoutNavigationWrapper.validateOrderForNavigation: Order items missing for order summary screen");
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "CheckoutNavigationWrapper.validateOrderForNavigation: Validation error", e);
            return false;
        }
    }
    
    /**
     * Injects enhanced services into the loaded controller.
     *
     * @param mainLayoutController The main layout controller
     * @param fxmlPath The FXML path that was loaded
     */
    private static void injectEnhancedServicesIntoController(MainLayoutController mainLayoutController, String fxmlPath) {
        if (mainLayoutController == null) {
            return;
        }
        
        try {
            // This is a simplified implementation - in practice, you might need to get a reference to the loaded controller
            // The Enhanced Navigation Manager already handles service injection, so this is mainly for additional setup
            logger.info("CheckoutNavigationWrapper.injectEnhancedServicesIntoController: Enhanced services injection attempted for " + fxmlPath);
            
            // You could extend this to perform additional controller-specific setup
            // For now, we rely on the Enhanced Navigation Manager's injection logic
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "CheckoutNavigationWrapper.injectEnhancedServicesIntoController: Service injection failed", e);
        }
    }
    
    /**
     * Attempts legacy fallback navigation using the original tier-based approach.
     *
     * @param order The order entity
     * @param mainLayoutController The main layout controller
     * @param sourceController The source controller
     * @param targetScreen The target screen identifier
     * @return true if navigation succeeded, false otherwise
     */
    private static boolean attemptLegacyFallbackNavigation(OrderEntity order, MainLayoutController mainLayoutController,
                                                          Object sourceController, String targetScreen) {
        logger.info("CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: Attempting legacy fallback for " + targetScreen);
        
        try {
            // Preserve order data for fallback scenarios
            preserveOrderData(order, mainLayoutController);
            
            if ("order_summary".equals(targetScreen)) {
                // Tier 1: MainLayoutController direct navigation
                if (attemptMainLayoutNavigation(order, mainLayoutController)) {
                    logger.info("CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: Legacy Tier 1 navigation succeeded for order summary");
                    return true;
                }
                
                // Tier 2: FXMLSceneManager navigation
                if (sourceController instanceof DeliveryInfoScreenController) {
                    if (attemptFXMLSceneManagerNavigation(order, mainLayoutController, (DeliveryInfoScreenController) sourceController)) {
                        logger.info("CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: Legacy Tier 2 navigation succeeded for order summary");
                        return true;
                    }
                } else {
                    // Generic FXMLSceneManager navigation without source controller
                    if (attemptGenericFXMLNavigation(order, mainLayoutController, "/com/aims/presentation/views/order_summary_screen.fxml", "Order Summary")) {
                        logger.info("CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: Generic Tier 2 navigation succeeded for order summary");
                        return true;
                    }
                }
                
                // Tier 3: NavigationService fallback
                if (attemptNavigationServiceFallback(order, mainLayoutController)) {
                    logger.info("CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: Legacy Tier 3 navigation succeeded for order summary");
                    return true;
                }
                
            } else if ("payment_method".equals(targetScreen)) {
                // Tier 1: MainLayoutController direct navigation
                if (attemptPaymentMethodMainLayoutNavigation(order, mainLayoutController)) {
                    logger.info("CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: Legacy Tier 1 navigation succeeded for payment method");
                    return true;
                }
                
                // Tier 2: FXMLSceneManager navigation
                if (sourceController instanceof OrderSummaryController) {
                    if (attemptPaymentMethodFXMLNavigation(order, mainLayoutController, (OrderSummaryController) sourceController)) {
                        logger.info("CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: Legacy Tier 2 navigation succeeded for payment method");
                        return true;
                    }
                } else {
                    // Generic FXMLSceneManager navigation without source controller
                    if (attemptGenericFXMLNavigation(order, mainLayoutController, "/com/aims/presentation/views/payment_method_screen.fxml", "Payment Method")) {
                        logger.info("CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: Generic Tier 2 navigation succeeded for payment method");
                        return true;
                    }
                }
                
                // Tier 3: NavigationService fallback
                if (attemptPaymentMethodNavigationServiceFallback(order, mainLayoutController)) {
                    logger.info("CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: Legacy Tier 3 navigation succeeded for payment method");
                    return true;
                }
                
            } else if ("delivery_info".equals(targetScreen)) {
                // Use generic navigation for delivery info
                if (mainLayoutController != null) {
                    Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/delivery_info_screen.fxml");
                    mainLayoutController.setHeaderTitle("Delivery Information");
                    
                    if (controller instanceof DeliveryInfoScreenController) {
                        ((DeliveryInfoScreenController) controller).setOrderData(order);
                        logger.info("CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: Legacy navigation succeeded for delivery info");
                        return true;
                    }
                }
            }
            
            // All legacy tiers failed
            logger.warning("CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: All legacy navigation tiers failed for " + targetScreen);
            handleNavigationFailure("All legacy navigation methods failed for " + targetScreen, order);
            return false;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "CheckoutNavigationWrapper.attemptLegacyFallbackNavigation: Error in legacy fallback", e);
            lastNavigationError = e;
            return false;
        }
    }
    
    /**
     * Generic FXML navigation without specific source controller.
     *
     * @param order The order entity
     * @param mainLayoutController The main layout controller
     * @param fxmlPath The FXML path to load
     * @param title The screen title
     * @return true if navigation succeeded, false otherwise
     */
    private static boolean attemptGenericFXMLNavigation(OrderEntity order, MainLayoutController mainLayoutController,
                                                       String fxmlPath, String title) {
        try {
            logger.info("CheckoutNavigationWrapper.attemptGenericFXMLNavigation: Attempting generic navigation to " + fxmlPath);
            
            FXMLSceneManager sceneManager = FXMLSceneManager.getInstance();
            if (sceneManager == null || mainLayoutController == null) {
                logger.warning("CheckoutNavigationWrapper.attemptGenericFXMLNavigation: SceneManager or MainLayoutController is null");
                return false;
            }
            
            // Use FXMLSceneManager for navigation with proper service injection
            Object controller = sceneManager.loadFXMLIntoPane(
                mainLayoutController.getContentPane(),
                fxmlPath
            );
            
            mainLayoutController.setHeaderTitle(title);
            
            if (controller != null) {
                // Inject order data based on controller type
                if (controller instanceof OrderSummaryController) {
                    OrderSummaryController orderSummaryController = (OrderSummaryController) controller;
                    orderSummaryController.setMainLayoutController(mainLayoutController);
                    orderSummaryController.setOrderData(order);
                } else if (controller instanceof PaymentMethodScreenController) {
                    PaymentMethodScreenController paymentController = (PaymentMethodScreenController) controller;
                    paymentController.setOrderData(order);
                } else if (controller instanceof DeliveryInfoScreenController) {
                    DeliveryInfoScreenController deliveryController = (DeliveryInfoScreenController) controller;
                    deliveryController.setOrderData(order);
                }
                
                logger.info("CheckoutNavigationWrapper.attemptGenericFXMLNavigation: Generic navigation completed successfully");
                return true;
            } else {
                logger.warning("CheckoutNavigationWrapper.attemptGenericFXMLNavigation: Controller loading failed");
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "CheckoutNavigationWrapper.attemptGenericFXMLNavigation: Generic navigation failed", e);
            return false;
        }
    }
    
    /**
     * Tier 1: Attempt navigation using MainLayoutController directly
     */
    private static boolean attemptMainLayoutNavigation(OrderEntity order, MainLayoutController mainLayoutController) {
        try {
            logger.info("CheckoutNavigationWrapper.attemptMainLayoutNavigation: Attempting Tier 1 navigation");
            
            if (mainLayoutController == null) {
                logger.warning("CheckoutNavigationWrapper.attemptMainLayoutNavigation: MainLayoutController is null");
                return false;
            }
            
            // Load order summary screen using MainLayoutController
            Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/order_summary_screen.fxml");
            mainLayoutController.setHeaderTitle("Order Summary & Confirmation");
            
            if (controller instanceof OrderSummaryController) {
                OrderSummaryController orderSummaryController = (OrderSummaryController) controller;
                
                // Enhanced controller setup with dependency injection
                orderSummaryController.setMainLayoutController(mainLayoutController);
                
                // Inject enhanced services if available
                try {
                    orderSummaryController.setOrderDataLoaderService(
                        com.aims.core.shared.ServiceFactory.getInstance().getOrderDataLoaderService()
                    );
                    orderSummaryController.setCartDataValidationService(
                        com.aims.core.shared.ServiceFactory.getInstance().getCartDataValidationService()
                    );
                } catch (Exception serviceException) {
                    logger.log(Level.WARNING, "CheckoutNavigationWrapper.attemptMainLayoutNavigation: Could not inject enhanced services", serviceException);
                }
                
                // Set order data with comprehensive validation
                orderSummaryController.setOrderData(order);
                
                logger.info("CheckoutNavigationWrapper.attemptMainLayoutNavigation: Tier 1 navigation completed successfully");
                return true;
            } else {
                logger.warning("CheckoutNavigationWrapper.attemptMainLayoutNavigation: Controller is not OrderSummaryController: " + 
                    (controller != null ? controller.getClass().getSimpleName() : "null"));
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "CheckoutNavigationWrapper.attemptMainLayoutNavigation: Tier 1 navigation failed", e);
            lastNavigationError = e;
            return false;
        }
    }
    
    /**
     * Tier 2: Attempt navigation using FXMLSceneManager
     */
    private static boolean attemptFXMLSceneManagerNavigation(OrderEntity order, MainLayoutController mainLayoutController,
                                                           DeliveryInfoScreenController sourceController) {
        try {
            logger.info("CheckoutNavigationWrapper.attemptFXMLSceneManagerNavigation: Attempting Tier 2 navigation");
            
            FXMLSceneManager sceneManager = FXMLSceneManager.getInstance();
            if (sceneManager == null || mainLayoutController == null) {
                logger.warning("CheckoutNavigationWrapper.attemptFXMLSceneManagerNavigation: SceneManager or MainLayoutController is null");
                return false;
            }
            
            // Use FXMLSceneManager for navigation with proper service injection
            Object controller = sceneManager.loadFXMLIntoPane(
                mainLayoutController.getContentPane(), 
                "/com/aims/presentation/views/order_summary_screen.fxml"
            );
            
            mainLayoutController.setHeaderTitle("Order Summary & Confirmation");
            
            if (controller instanceof OrderSummaryController) {
                OrderSummaryController orderSummaryController = (OrderSummaryController) controller;
                orderSummaryController.setMainLayoutController(mainLayoutController);
                orderSummaryController.setOrderData(order);
                
                logger.info("CheckoutNavigationWrapper.attemptFXMLSceneManagerNavigation: Tier 2 navigation completed successfully");
                return true;
            } else {
                logger.warning("CheckoutNavigationWrapper.attemptFXMLSceneManagerNavigation: Controller type mismatch");
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "CheckoutNavigationWrapper.attemptFXMLSceneManagerNavigation: Tier 2 navigation failed", e);
            lastNavigationError = e;
            return false;
        }
    }
    
    /**
     * Tier 3: Attempt navigation using NavigationService as final fallback
     */
    private static boolean attemptNavigationServiceFallback(OrderEntity order, MainLayoutController mainLayoutController) {
        try {
            logger.info("CheckoutNavigationWrapper.attemptNavigationServiceFallback: Attempting Tier 3 navigation");
            
            // Use enhanced NavigationService method
            NavigationService.navigateToOrderSummary(order, mainLayoutController);
            
            logger.info("CheckoutNavigationWrapper.attemptNavigationServiceFallback: Tier 3 navigation completed");
            return true;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "CheckoutNavigationWrapper.attemptNavigationServiceFallback: Tier 3 navigation failed", e);
            lastNavigationError = e;
            return false;
        }
    }
    
    /**
     * Tier 1: Attempt payment method navigation using MainLayoutController directly
     */
    private static boolean attemptPaymentMethodMainLayoutNavigation(OrderEntity order, MainLayoutController mainLayoutController) {
        try {
            logger.info("CheckoutNavigationWrapper.attemptPaymentMethodMainLayoutNavigation: Attempting Tier 1 payment navigation");
            
            if (mainLayoutController == null) {
                logger.warning("CheckoutNavigationWrapper.attemptPaymentMethodMainLayoutNavigation: MainLayoutController is null");
                return false;
            }
            
            // Load payment method screen using MainLayoutController
            Object controller = mainLayoutController.loadContent("/com/aims/presentation/views/payment_method_screen.fxml");
            mainLayoutController.setHeaderTitle("Select Payment Method");
            
            if (controller instanceof PaymentMethodScreenController) {
                PaymentMethodScreenController paymentController = (PaymentMethodScreenController) controller;
                paymentController.setOrderData(order);
                
                logger.info("CheckoutNavigationWrapper.attemptPaymentMethodMainLayoutNavigation: Tier 1 payment navigation completed successfully");
                return true;
            } else {
                logger.warning("CheckoutNavigationWrapper.attemptPaymentMethodMainLayoutNavigation: Controller is not PaymentMethodScreenController: " + 
                    (controller != null ? controller.getClass().getSimpleName() : "null"));
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "CheckoutNavigationWrapper.attemptPaymentMethodMainLayoutNavigation: Tier 1 payment navigation failed", e);
            lastNavigationError = e;
            return false;
        }
    }
    
    /**
     * Tier 2: Attempt payment method navigation using FXMLSceneManager
     */
    private static boolean attemptPaymentMethodFXMLNavigation(OrderEntity order, MainLayoutController mainLayoutController,
                                                            OrderSummaryController sourceController) {
        try {
            logger.info("CheckoutNavigationWrapper.attemptPaymentMethodFXMLNavigation: Attempting Tier 2 payment navigation");
            
            FXMLSceneManager sceneManager = FXMLSceneManager.getInstance();
            if (sceneManager == null || mainLayoutController == null) {
                logger.warning("CheckoutNavigationWrapper.attemptPaymentMethodFXMLNavigation: SceneManager or MainLayoutController is null");
                return false;
            }
            
            // Use FXMLSceneManager for payment navigation
            Object controller = sceneManager.loadFXMLIntoPane(
                mainLayoutController.getContentPane(), 
                "/com/aims/presentation/views/payment_method_screen.fxml"
            );
            
            mainLayoutController.setHeaderTitle("Select Payment Method");
            
            if (controller instanceof PaymentMethodScreenController) {
                PaymentMethodScreenController paymentController = (PaymentMethodScreenController) controller;
                paymentController.setOrderData(order);
                
                logger.info("CheckoutNavigationWrapper.attemptPaymentMethodFXMLNavigation: Tier 2 payment navigation completed successfully");
                return true;
            } else {
                logger.warning("CheckoutNavigationWrapper.attemptPaymentMethodFXMLNavigation: Controller type mismatch");
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "CheckoutNavigationWrapper.attemptPaymentMethodFXMLNavigation: Tier 2 payment navigation failed", e);
            lastNavigationError = e;
            return false;
        }
    }
    
    /**
     * Tier 3: Attempt payment method navigation using NavigationService as final fallback
     */
    private static boolean attemptPaymentMethodNavigationServiceFallback(OrderEntity order, MainLayoutController mainLayoutController) {
        try {
            logger.info("CheckoutNavigationWrapper.attemptPaymentMethodNavigationServiceFallback: Attempting Tier 3 payment navigation");
            
            // Use enhanced NavigationService method
            NavigationService.navigateToPaymentMethod(order, mainLayoutController);
            
            logger.info("CheckoutNavigationWrapper.attemptPaymentMethodNavigationServiceFallback: Tier 3 payment navigation completed");
            return true;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "CheckoutNavigationWrapper.attemptPaymentMethodNavigationServiceFallback: Tier 3 payment navigation failed", e);
            lastNavigationError = e;
            return false;
        }
    }
    
    /**
     * Preserve order data for fallback scenarios and error recovery
     */
    private static void preserveOrderData(OrderEntity order, MainLayoutController mainLayoutController) {
        lastOrderData = order;
        lastMainLayoutController = mainLayoutController;
        lastNavigationError = null;
        
        if (order != null) {
            logger.info("CheckoutNavigationWrapper.preserveOrderData: Order data preserved for Order ID: " + order.getOrderId());
        } else {
            logger.warning("CheckoutNavigationWrapper.preserveOrderData: Null order data preserved");
        }
    }
    
    /**
     * Handle navigation failure with comprehensive error logging and recovery options
     */
    private static void handleNavigationFailure(String context, OrderEntity order) {
        String errorMessage = "Navigation failure in " + context;
        if (order != null) {
            errorMessage += " for Order ID: " + order.getOrderId();
        }
        if (lastNavigationError != null) {
            errorMessage += " - Last error: " + lastNavigationError.getMessage();
        }
        
        logger.severe("CheckoutNavigationWrapper.handleNavigationFailure: " + errorMessage);
        
        // Log detailed error information for debugging
        if (lastNavigationError != null) {
            logger.log(Level.SEVERE, "CheckoutNavigationWrapper.handleNavigationFailure: Detailed error", lastNavigationError);
        }
        
        // Could implement additional recovery mechanisms here:
        // - Show error dialog to user
        // - Save order state to temporary storage
        // - Provide manual navigation options
        // - Send error report to logging service
    }
    
    /**
     * Get the last preserved order data for recovery scenarios
     * @return The last order data that was being navigated, or null if none
     */
    public static OrderEntity getLastOrderData() {
        return lastOrderData;
    }
    
    /**
     * Get the last navigation error for debugging
     * @return The last navigation error, or null if none
     */
    public static Exception getLastNavigationError() {
        return lastNavigationError;
    }
    
    /**
     * Clear preserved navigation state
     */
    public static void clearNavigationState() {
        lastOrderData = null;
        lastMainLayoutController = null;
        lastNavigationError = null;
        logger.info("CheckoutNavigationWrapper.clearNavigationState: Navigation state cleared");
    }
    
    /**
     * Check if there is preserved order data available for recovery
     * @return true if order data is available for recovery
     */
    public static boolean hasPreservedOrderData() {
        return lastOrderData != null;
    }
    
    /**
     * Get enhanced navigation debug information for troubleshooting
     * @return String containing comprehensive navigation state information
     */
    public static String getNavigationDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("CheckoutNavigationWrapper Enhanced Debug Info:\n");
        info.append("=== Enhanced Navigation State ===\n");
        info.append("Current Session: ").append(currentSessionId != null ? currentSessionId : "none").append("\n");
        info.append("Last Navigation Time: ").append(lastNavigationTime != null ? lastNavigationTime.toString() : "none").append("\n");
        info.append("Last Navigation Result: ").append(lastNavigationResult != null ? lastNavigationResult.toString() : "none").append("\n");
        
        info.append("\n=== Legacy Compatibility State ===\n");
        info.append("Last Order: ").append(lastOrderData != null ? lastOrderData.getOrderId() : "null").append("\n");
        info.append("Last MainLayout: ").append(lastMainLayoutController != null ? "available" : "null").append("\n");
        info.append("Last Error: ").append(lastNavigationError != null ? lastNavigationError.getMessage() : "none").append("\n");
        
        info.append("\n=== Enhanced Services Status ===\n");
        info.append("OrderDataLoaderService: ").append(orderDataLoaderService != null ? "available" : "unavailable").append("\n");
        info.append("CartDataValidationService: ").append(cartDataValidationService != null ? "available" : "unavailable").append("\n");
        
        info.append("\n=== Session Data Status ===\n");
        if (currentSessionId != null) {
            info.append("Session Has Order Data: ").append(OrderDataContextManager.hasOrderData(currentSessionId)).append("\n");
        }
        
        info.append("\n=== Enhanced Navigation Manager Status ===\n");
        info.append("Navigation Manager Healthy: ").append(EnhancedNavigationManager.isHealthy()).append("\n");
        info.append("Navigation Failure Count: ").append(EnhancedNavigationManager.getFailureCount()).append("\n");
        
        return info.toString();
    }
    
    /**
     * Gets the current session ID for order data persistence.
     *
     * @return The current session ID, or null if none
     */
    public static String getCurrentSessionId() {
        return currentSessionId;
    }
    
    /**
     * Sets the current session ID (for advanced use cases).
     *
     * @param sessionId The session ID to set
     */
    public static void setCurrentSessionId(String sessionId) {
        currentSessionId = sessionId;
        logger.info("CheckoutNavigationWrapper.setCurrentSessionId: Session ID set to " + sessionId);
    }
    
    /**
     * Gets the last navigation result for monitoring purposes.
     *
     * @return The last navigation result, or null if none
     */
    public static EnhancedNavigationManager.NavigationResult getLastNavigationResult() {
        return lastNavigationResult;
    }
    
    /**
     * Checks if the checkout navigation wrapper is in a healthy state.
     *
     * @return true if the wrapper is healthy, false if there are issues
     */
    public static boolean isHealthy() {
        try {
            // Check enhanced navigation manager health
            boolean enhancedNavigationHealthy = EnhancedNavigationManager.isHealthy();
            
            // Check service availability
            boolean servicesAvailable = orderDataLoaderService != null && cartDataValidationService != null;
            
            // Check for recent navigation errors
            boolean noRecentErrors = lastNavigationError == null ||
                (lastNavigationTime != null && lastNavigationTime.plusMinutes(5).isAfter(java.time.LocalDateTime.now()));
            
            return enhancedNavigationHealthy && servicesAvailable && noRecentErrors;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "CheckoutNavigationWrapper.isHealthy: Health check failed", e);
            return false;
        }
    }
    
    /**
     * Resets the checkout navigation wrapper state for recovery purposes.
     */
    public static void reset() {
        // Clear enhanced state
        currentSessionId = null;
        lastNavigationTime = null;
        lastNavigationResult = null;
        
        // Clear legacy state
        lastOrderData = null;
        lastMainLayoutController = null;
        lastNavigationError = null;
        
        // Reset enhanced navigation manager
        EnhancedNavigationManager.reset();
        
        logger.info("CheckoutNavigationWrapper.reset: Checkout navigation wrapper state reset");
    }
    
    /**
     * Validates that all required components are available for checkout navigation.
     *
     * @return true if all components are available, false otherwise
     */
    public static boolean validateComponents() {
        try {
            // Check enhanced navigation manager
            if (!EnhancedNavigationManager.isHealthy()) {
                logger.warning("CheckoutNavigationWrapper.validateComponents: Enhanced navigation manager is not healthy");
                return false;
            }
            
            // Check enhanced services
            if (orderDataLoaderService == null) {
                logger.warning("CheckoutNavigationWrapper.validateComponents: OrderDataLoaderService is not available");
                return false;
            }
            
            if (cartDataValidationService == null) {
                logger.warning("CheckoutNavigationWrapper.validateComponents: CartDataValidationService is not available");
                return false;
            }
            
            // Check FXMLSceneManager
            FXMLSceneManager sceneManager = FXMLSceneManager.getInstance();
            if (sceneManager == null) {
                logger.warning("CheckoutNavigationWrapper.validateComponents: FXMLSceneManager is not available");
                return false;
            }
            
            logger.info("CheckoutNavigationWrapper.validateComponents: All components are available and healthy");
            return true;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "CheckoutNavigationWrapper.validateComponents: Component validation failed", e);
            return false;
        }
    }
}