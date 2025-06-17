package com.aims.core.presentation.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import com.aims.core.application.services.IPaymentService;
import com.aims.core.application.services.IOrderService;
import com.aims.core.application.services.IOrderValidationService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.shared.NavigationService;
import com.aims.core.presentation.utils.EnhancedNavigationManager;
import com.aims.core.presentation.utils.MainLayoutControllerRegistry;
import com.aims.core.presentation.utils.UnifiedNavigationManager;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.utils.PaymentMethodFactory;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.TimeUnit;

public class PaymentMethodScreenController {

    @FXML private RadioButton vnpayCreditCardRadio;
    @FXML private RadioButton domesticCardRadio;
    @FXML private ToggleGroup paymentMethodToggleGroup;
    @FXML private Label selectedMethodDescriptionLabel;
    @FXML private Label errorMessageLabel;
    @FXML private Button proceedButton;

    private IPaymentService paymentService;
    private IOrderService orderService;
    private IOrderValidationService orderValidationService;
    private MainLayoutController mainLayoutController;
    private String currentOrderId;
    private OrderEntity currentOrder;
    
    private static final Logger logger = Logger.getLogger(PaymentMethodScreenController.class.getName());

    public PaymentMethodScreenController() {
        // Defer service initialization until after FXML injection
        logger.info("PaymentMethodScreenController: Constructor called, deferring service initialization");
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    public void setPaymentService(IPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setOrderData(OrderEntity order) {
        // ENHANCED: Better validation and logging for order data injection
        System.out.println("PAYMENT_METHOD: Setting order data - " +
                          (order != null ? "Order ID: " + order.getOrderId() : "ORDER IS NULL"));
        
        if (order == null) {
            System.err.println("PAYMENT_METHOD_ERROR: Received null order - this will cause payment flow failures");
            showError("Order information is missing. Please return to previous screen and try again.");
            this.currentOrder = null;
            this.currentOrderId = null;
            updateUI();
            return;
        }
        
        // ENHANCED: Validate order completeness
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            System.err.println("PAYMENT_METHOD_ERROR: Order ID is missing");
            showError("Invalid order data. Please restart the order process.");
            this.currentOrder = null;
            this.currentOrderId = null;
            updateUI();
            return;
        }
        
        if (order.getDeliveryInfo() == null) {
            System.err.println("PAYMENT_METHOD_ERROR: Delivery information missing for Order " + order.getOrderId());
            showError("Delivery information is missing. Please complete delivery details first.");
            this.currentOrder = null;
            this.currentOrderId = null;
            updateUI();
            return;
        }
        
        // ENHANCED: Validate order items
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            System.err.println("PAYMENT_METHOD_ERROR: No order items found for Order " + order.getOrderId());
            showError("Order contains no items. Please add items to your cart.");
            this.currentOrder = null;
            this.currentOrderId = null;
            updateUI();
            return;
        }
        
        this.currentOrder = order;
        this.currentOrderId = order.getOrderId();
        System.out.println("PAYMENT_METHOD: Order data set successfully - ID: " + this.currentOrderId);
        updateUI();
    }

    public void initData(String orderId) {
        logger.info("PaymentMethodScreenController.initData: Initializing with order ID: " + orderId);
        
        // Ensure services are initialized
        if (paymentService == null || orderService == null || orderValidationService == null) {
            initializeServices();
        }
        
        this.currentOrderId = orderId;
        if (orderId != null && !orderId.trim().isEmpty()) {
            try {
                if (orderService != null) {
                    this.currentOrder = orderService.getOrderById(orderId);
                    if (this.currentOrder != null) {
                        logger.info("PaymentMethodScreenController.initData: Order loaded successfully: " + orderId);
                        updateUI();
                    } else {
                        logger.warning("PaymentMethodScreenController.initData: Order service returned null for ID: " + orderId);
                        showError("Order not found. Please restart the order process.");
                    }
                } else {
                    logger.severe("PaymentMethodScreenController.initData: OrderService is null, cannot load order");
                    showError("System error. Please refresh the page.");
                }
            } catch (ResourceNotFoundException e) {
                logger.warning("PaymentMethodScreenController.initData: Order not found: " + e.getMessage());
                showError("Could not find order details. Please try again.");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "PaymentMethodScreenController.initData: Unexpected error loading order", e);
                showError("An error occurred loading order details. Please try again.");
            }
        } else {
            logger.warning("PaymentMethodScreenController.initData: Invalid order ID provided");
            showError("Invalid order information. Please restart the order process.");
        }
    }

    @FXML
    private void initialize() {
        logger.info("PaymentMethodScreenController.initialize: Starting FXML initialization");
        
        // Initialize services after FXML injection is complete
        initializeServices();
        
        // Setup UI components
        setupPaymentMethodToggleListeners();
        
        // Set default error state
        if (errorMessageLabel != null) {
            errorMessageLabel.setText("");
            errorMessageLabel.setVisible(false);
        }
        
        logger.info("PaymentMethodScreenController.initialize: FXML initialization completed successfully");
    }

    private void initializeServices() {
        try {
            if (this.paymentService == null) {
                this.paymentService = ServiceFactory.getPaymentService();
                logger.fine("PaymentMethodScreenController.initializeServices: PaymentService initialized");
            }
            if (this.orderService == null) {
                this.orderService = ServiceFactory.getOrderService();
                logger.fine("PaymentMethodScreenController.initializeServices: OrderService initialized");
            }
            if (this.orderValidationService == null) {
                this.orderValidationService = ServiceFactory.getOrderValidationService();
                logger.fine("PaymentMethodScreenController.initializeServices: OrderValidationService initialized");
            }
            logger.info("PaymentMethodScreenController.initializeServices: All services initialized successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PaymentMethodScreenController.initializeServices: Error initializing services", e);
            showError("System initialization error. Please refresh the page.");
        }
    }

    private void setupPaymentMethodToggleListeners() {
        if (paymentMethodToggleGroup != null) {
            paymentMethodToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == vnpayCreditCardRadio) {
                    selectedMethodDescriptionLabel.setText(
                        "Pay with international credit/debit cards. All major cards are accepted."
                    );
                } else if (newVal == domesticCardRadio) {
                    selectedMethodDescriptionLabel.setText(
                        "Pay with domestic ATM/debit cards from Vietnamese banks."
                    );
                }
            });
        }
        
        // Set initial description if a radio button is already selected
        if (vnpayCreditCardRadio != null && vnpayCreditCardRadio.isSelected()) {
            selectedMethodDescriptionLabel.setText(
                "Pay with international credit/debit cards. All major cards are accepted."
            );
        }
    }

    private void updateUI() {
        if (currentOrder != null) {
            proceedButton.setDisable(false);
            errorMessageLabel.setVisible(false);
            // Additional UI updates based on order data
        } else {
            proceedButton.setDisable(true);
            showError("No order data available");
        }
    }

    @FXML
    private void handleProceedAction() {
        try {
            clearError();
            showProcessing();
            
            logger.info("PAYMENT_METHOD: Starting enhanced payment validation process");
            
            // CRITICAL FIX 1: Use OrderValidationService instead of manual validation
            OrderEntity validatedOrder = validateOrderForPaymentEnhanced();
            if (validatedOrder == null) {
                // Error already displayed by validation method
                return;
            }
            
            // Update current order with validated data
            this.currentOrder = validatedOrder;
            this.currentOrderId = validatedOrder.getOrderId();

            // Get selected payment method type with null pointer protection
            RadioButton selected = (RadioButton) paymentMethodToggleGroup.getSelectedToggle();
            if (selected == null) {
                logger.warning("PAYMENT_METHOD_ERROR: No payment method selected");
                showError("Please select a payment method.");
                return;
            }
            
            String paymentType = selected.getUserData() != null ? selected.getUserData().toString() : null;
            if (paymentType == null) {
                logger.warning("PAYMENT_METHOD_ERROR: Invalid payment method user data");
                showError("Invalid payment method selected. Please try selecting again.");
                return;
            }

            logger.info("PAYMENT_METHOD: Enhanced validation completed successfully, proceeding with payment type: " + paymentType);
            logger.info("PAYMENT_METHOD: Order " + validatedOrder.getOrderId() + " validated and ready for payment processing");

            // Generate VNPay payment method ID if needed
            String paymentMethodId = generatePaymentMethodId(paymentType);

            // Navigate to the next screen
            NavigationService.navigateTo("payment_processing_screen.fxml", mainLayoutController, (controller) -> {
                PaymentProcessingScreenController processingController = (PaymentProcessingScreenController) controller;
                processingController.initPaymentFlow(validatedOrder, paymentMethodId);
                logger.info("PAYMENT_METHOD: Successfully navigated to payment processing with validated order");
            });

        } catch (Exception e) {
            logger.log(Level.SEVERE, "PAYMENT_METHOD_ERROR: Unexpected error during enhanced payment setup", e);
            showError("An unexpected error occurred during payment setup. Please try again.");
        }
    }
    
    /**
     * CRITICAL FIX 1: Enhanced order validation using OrderValidationService
     * Replaces manual validation logic with robust service-based validation
     * Provides comprehensive error recovery and detailed logging
     */
    private OrderEntity validateOrderForPaymentEnhanced() {
        try {
            logger.info("PAYMENT_METHOD: Starting enhanced order validation for payment");
            
            // Phase 1: Basic order ID validation
            if (currentOrderId == null || currentOrderId.trim().isEmpty()) {
                logger.warning("PAYMENT_METHOD_VALIDATION: Order ID is null or empty");
                showError("Invalid order state. Please restart the order process.");
                return null;
            }
            
            // Phase 2: Check if order exists using validation service
            if (!orderValidationService.orderExists(currentOrderId)) {
                logger.warning("PAYMENT_METHOD_VALIDATION: Order does not exist: " + currentOrderId);
                showError("Order not found. Please restart the order process.");
                return null;
            }
            
            // Phase 3: Use OrderValidationService for comprehensive validation
            OrderEntity validatedOrder = orderValidationService.getValidatedOrderForPayment(currentOrderId);
            
            logger.info("PAYMENT_METHOD_VALIDATION: Order validation successful for: " + currentOrderId);
            logger.info("PAYMENT_METHOD_VALIDATION: Order status: " + validatedOrder.getOrderStatus());
            logger.info("PAYMENT_METHOD_VALIDATION: Items count: " +
                       (validatedOrder.getOrderItems() != null ? validatedOrder.getOrderItems().size() : 0));
            logger.info("PAYMENT_METHOD_VALIDATION: Delivery info: " +
                       (validatedOrder.getDeliveryInfo() != null ? "Available" : "Missing"));
            logger.info("PAYMENT_METHOD_VALIDATION: Total amount: " + validatedOrder.getTotalAmountPaid());
            
            return validatedOrder;
            
        } catch (ValidationException e) {
            logger.log(Level.WARNING, "PAYMENT_METHOD_VALIDATION: Order validation failed: " + e.getMessage());
            showError(e.getMessage());
            return null;
            
        } catch (ResourceNotFoundException e) {
            logger.log(Level.WARNING, "PAYMENT_METHOD_VALIDATION: Order not found during validation: " + e.getMessage());
            showError("Order not found. Please restart the order process.");
            return null;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PAYMENT_METHOD_VALIDATION: Unexpected error during order validation", e);
            
            // Enhanced error recovery - try to reload order data
            if (attemptOrderDataRecovery()) {
                logger.info("PAYMENT_METHOD_VALIDATION: Order data recovery successful, retrying validation");
                // Retry validation once after recovery
                try {
                    return orderValidationService.getValidatedOrderForPayment(currentOrderId);
                } catch (Exception retryException) {
                    logger.log(Level.SEVERE, "PAYMENT_METHOD_VALIDATION: Retry validation also failed", retryException);
                }
            }
            
            showError("Unable to validate order for payment. Please return to previous screen and try again.");
            return null;
        }
    }
    
    /**
     * Enhanced error recovery mechanism for order data issues
     * Attempts to reload order data when validation service encounters problems
     */
    private boolean attemptOrderDataRecovery() {
        try {
            logger.info("PAYMENT_METHOD_RECOVERY: Attempting order data recovery for: " + currentOrderId);
            
            if (currentOrderId == null) {
                logger.warning("PAYMENT_METHOD_RECOVERY: Cannot recover - order ID is null");
                return false;
            }
            
            // Try to reload order using OrderService as fallback
            OrderEntity recoveredOrder = orderService.getOrderById(currentOrderId);
            if (recoveredOrder != null) {
                this.currentOrder = recoveredOrder;
                logger.info("PAYMENT_METHOD_RECOVERY: Order data recovery successful");
                return true;
            }
            
            logger.warning("PAYMENT_METHOD_RECOVERY: Failed to recover order data - order not found");
            return false;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "PAYMENT_METHOD_RECOVERY: Error during order data recovery", e);
            return false;
        }
    }

    @FXML
    private void handleBackToOrderSummaryAction() {
        logger.info("PaymentMethodScreenController.handleBackToOrderSummaryAction: Navigating back to order summary with Enhanced Navigation Manager");
        
        try {
            if (currentOrder == null) {
                logger.warning("PaymentMethodScreenController.handleBackToOrderSummaryAction: No current order available");
                showError("Order information is missing. Please restart the order process.");
                return;
            }
            
            // PHASE 1 FIX: Enhanced MainLayoutController validation with registry fallback
            MainLayoutController validatedController = validateMainLayoutControllerWithFallback();
            if (validatedController == null) {
                logger.warning("PaymentMethodScreenController.handleBackToOrderSummaryAction: MainLayoutController not available after all attempts, using NavigationService fallback");
                NavigationService.navigateToOrderSummary(currentOrderId);
                return;
            }
            
            // Update local reference if obtained from registry
            if (mainLayoutController == null) {
                mainLayoutController = validatedController;
                logger.info("PaymentMethodScreenController.handleBackToOrderSummaryAction: MainLayoutController obtained from registry");
            }
            
            // PHASE 2: Use Unified Navigation Manager for consolidated navigation
            UnifiedNavigationManager.NavigationResult result = UnifiedNavigationManager.navigateToOrderSummary(
                currentOrder, this);
            
            if (result.isSuccess()) {
                logger.info("PaymentMethodScreenController.handleBackToOrderSummaryAction: Unified navigation successful: " + result.getDescription());
            } else if (result.hasDataPreservation()) {
                logger.warning("PaymentMethodScreenController.handleBackToOrderSummaryAction: Navigation failed but data preserved: " + result.getDescription());
                showError("Navigation encountered issues but your order data has been preserved. Please try again.");
            } else {
                logger.warning("PaymentMethodScreenController.handleBackToOrderSummaryAction: Unified navigation failed, using enhanced fallback");
                
                // Enhanced fallback with validation
                MainLayoutController fallbackController = validateMainLayoutControllerWithFallback();
                if (fallbackController != null) {
                    Object controller = fallbackController.loadContent("/com/aims/presentation/views/order_summary_screen.fxml");
                    fallbackController.setHeaderTitle("Order Summary");
                    
                    if (controller instanceof OrderSummaryController) {
                        OrderSummaryController orderSummaryController = (OrderSummaryController) controller;
                        orderSummaryController.setMainLayoutController(fallbackController);
                        orderSummaryController.setOrderData(currentOrder);
                        logger.info("PaymentMethodScreenController.handleBackToOrderSummaryAction: Enhanced fallback completed successfully");
                    } else {
                        logger.warning("PaymentMethodScreenController.handleBackToOrderSummaryAction: Enhanced fallback controller validation failed");
                        NavigationService.navigateToOrderSummary(currentOrderId);
                    }
                } else {
                    logger.warning("PaymentMethodScreenController.handleBackToOrderSummaryAction: All enhanced strategies failed, using NavigationService");
                    NavigationService.navigateToOrderSummary(currentOrderId);
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PaymentMethodScreenController.handleBackToOrderSummaryAction: Error during navigation", e);
            showError("Navigation error occurred. Please try again.");
        }
    }

    private void showError(String message) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(true);
        proceedButton.setDisable(false);
    }
    
    private void clearError() {
        errorMessageLabel.setVisible(false);
        errorMessageLabel.setText("");
    }
    
    private void showProcessing() {
        proceedButton.setDisable(true);
        clearError();
    }
    
    private String generatePaymentMethodId(String paymentType) {
        // Use PaymentMethodFactory to generate VNPay payment method ID
        return PaymentMethodFactory.createVNPayPaymentMethodId(paymentType);
    }
    
    /**
     * PHASE 1 FIX: Validates MainLayoutController with comprehensive fallback strategies.
     * Uses MainLayoutControllerRegistry as primary source with multiple validation attempts.
     *
     * @return A valid MainLayoutController instance, or null if none available
     */
    private MainLayoutController validateMainLayoutControllerWithFallback() {
        try {
            logger.fine("PaymentMethodScreenController.validateMainLayoutControllerWithFallback: Starting validation");
            
            // Strategy 1: Check current instance
            if (mainLayoutController != null) {
                try {
                    // Quick validation
                    if (mainLayoutController.getContentPane() != null && mainLayoutController.getMainContainer() != null) {
                        logger.fine("PaymentMethodScreenController.validateMainLayoutControllerWithFallback: Current instance is valid");
                        return mainLayoutController;
                    } else {
                        logger.warning("PaymentMethodScreenController.validateMainLayoutControllerWithFallback: Current instance failed validation");
                        mainLayoutController = null; // Clear invalid reference
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "PaymentMethodScreenController.validateMainLayoutControllerWithFallback: Current instance validation error", e);
                    mainLayoutController = null; // Clear problematic reference
                }
            }
            
            // Strategy 2: Get from MainLayoutControllerRegistry
            try {
                MainLayoutController registryController = MainLayoutControllerRegistry.getInstance(3, TimeUnit.SECONDS);
                if (registryController != null) {
                    logger.info("PaymentMethodScreenController.validateMainLayoutControllerWithFallback: Successfully obtained controller from registry");
                    return registryController;
                } else {
                    logger.warning("PaymentMethodScreenController.validateMainLayoutControllerWithFallback: Registry returned null controller");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "PaymentMethodScreenController.validateMainLayoutControllerWithFallback: Registry access failed", e);
            }
            
            // Strategy 3: Force registry re-validation
            try {
                if (MainLayoutControllerRegistry.revalidate()) {
                    MainLayoutController revalidatedController = MainLayoutControllerRegistry.getInstanceImmediate();
                    if (revalidatedController != null) {
                        logger.info("PaymentMethodScreenController.validateMainLayoutControllerWithFallback: Successfully obtained controller after re-validation");
                        return revalidatedController;
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "PaymentMethodScreenController.validateMainLayoutControllerWithFallback: Re-validation failed", e);
            }
            
            // All strategies failed
            logger.severe("PaymentMethodScreenController.validateMainLayoutControllerWithFallback: All validation strategies failed");
            logMainLayoutControllerDebugInfo();
            return null;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PaymentMethodScreenController.validateMainLayoutControllerWithFallback: Unexpected error during validation", e);
            return null;
        }
    }
    
    /**
     * Logs comprehensive debug information about MainLayoutController state for troubleshooting.
     */
    private void logMainLayoutControllerDebugInfo() {
        try {
            StringBuilder debugInfo = new StringBuilder();
            debugInfo.append("PaymentMethodScreenController MainLayoutController Debug Info:\n");
            debugInfo.append("Local Instance: ").append(mainLayoutController != null ? "available" : "null").append("\n");
            debugInfo.append("Registry Debug Info:\n").append(MainLayoutControllerRegistry.getDebugInfo()).append("\n");
            
            // Additional context
            debugInfo.append("Current Order: ").append(currentOrder != null ? currentOrder.getOrderId() : "null").append("\n");
            debugInfo.append("Order ID: ").append(currentOrderId != null ? currentOrderId : "null").append("\n");
            
            logger.severe("PaymentMethodScreenController.logMainLayoutControllerDebugInfo: " + debugInfo.toString());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PaymentMethodScreenController.logMainLayoutControllerDebugInfo: Error generating debug info", e);
        }
    }
}