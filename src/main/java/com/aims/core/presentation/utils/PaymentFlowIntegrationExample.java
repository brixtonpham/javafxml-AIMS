package com.aims.core.presentation.utils;

import com.aims.core.entities.OrderEntity;
import com.aims.core.presentation.controllers.OrderSummaryController;
import com.aims.core.presentation.controllers.PaymentMethodScreenController;
import com.aims.core.presentation.controllers.DeliveryInfoScreenController;
import com.aims.core.shared.ServiceFactory;

import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * PaymentFlowIntegrationExample
 * 
 * This class demonstrates how to integrate the OrderValidationStateManager and PaymentFlowLogger
 * into existing payment flow controllers to enhance monitoring and prevent validation bypass issues.
 * 
 * Integration Points:
 * - Controller initialization: Start payment flow sessions and validation tracking
 * - Navigation events: Track user movement and preserve validation states
 * - Validation checkpoints: Log validation results and create state records
 * - Error handling: Log critical errors with full context
 * - Performance monitoring: Track operation timing and user behavior
 * 
 * This serves as a reference implementation for retrofitting the monitoring utilities
 * into the existing payment flow controllers.
 */
public class PaymentFlowIntegrationExample {
    
    private static final Logger logger = Logger.getLogger(PaymentFlowIntegrationExample.class.getName());
    
    /**
     * Example integration for OrderSummaryController
     * This shows how to enhance the existing handleProceedToPaymentMethodAction method
     */
    public static void enhanceOrderSummaryController(OrderSummaryController controller, OrderEntity order) {
        // Get monitoring utilities
        OrderValidationStateManager stateManager = ServiceFactory.getOrderValidationStateManager();
        PaymentFlowLogger flowLogger = ServiceFactory.getPaymentFlowLogger();
        
        try {
            // Start or get existing payment flow session
            PaymentFlowLogger.PaymentFlowSession session = flowLogger.getSessionByOrderId(order.getOrderId());
            String sessionId;
            
            if (session == null) {
                sessionId = flowLogger.startPaymentFlowSession(order.getOrderId());
                logger.info("Started new payment flow session: " + sessionId + " for order: " + order.getOrderId());
            } else {
                sessionId = session.getSessionId();
                logger.info("Using existing payment flow session: " + sessionId + " for order: " + order.getOrderId());
            }
            
            // Log navigation to order summary
            flowLogger.logNavigationStep(sessionId, "DELIVERY_INFO", "ORDER_SUMMARY", 
                Map.of("order_total", order.getTotalAmountPaid(), "items_count", order.getOrderItems().size()));
            
            // Create validation state for order summary
            String validationStateId = stateManager.createValidationState(
                order.getOrderId(),
                OrderValidationStateManager.ValidationStep.ORDER_SUMMARY_VALIDATION,
                OrderValidationStateManager.ValidationResult.PASSED,
                "Order summary validation completed successfully"
            );
            
            // Log validation step
            flowLogger.logValidationStep(sessionId, "ORDER_SUMMARY_VALIDATION", true,
                "Order summary validated - ready for payment method selection",
                Map.of("validation_state_id", validationStateId, "order_status", order.getOrderStatus()));
            
            // Example: Enhanced button state logging
            flowLogger.logButtonStateChange(sessionId, "proceedToPaymentMethodButton", "disabled", "enabled",
                "Order validation passed - enabling payment progression");
            
            // Example: Performance monitoring
            long startTime = System.currentTimeMillis();
            
            // ... existing navigation logic here ...
            
            long endTime = System.currentTimeMillis();
            Duration operationTime = Duration.ofMillis(endTime - startTime);
            
            flowLogger.logPerformanceMetrics(sessionId, "ORDER_SUMMARY_TO_PAYMENT_METHOD_NAVIGATION", operationTime,
                Map.of("validation_time_ms", operationTime.toMillis(), "order_complexity", order.getOrderItems().size()));
            
        } catch (Exception e) {
            // Enhanced error logging with full context
            PaymentFlowLogger flowLogger2 = ServiceFactory.getPaymentFlowLogger();
            PaymentFlowLogger.PaymentFlowSession session = flowLogger2.getSessionByOrderId(order.getOrderId());
            String sessionId = session != null ? session.getSessionId() : "NO_SESSION";
            
            flowLogger2.logCriticalError(sessionId, "ORDER_SUMMARY_NAVIGATION_ERROR", e,
                Map.of(
                    "order_id", order.getOrderId(),
                    "controller", "OrderSummaryController",
                    "action", "handleProceedToPaymentMethodAction",
                    "order_status", order.getOrderStatus(),
                    "total_amount", order.getTotalAmountPaid()
                ));
            
            throw e; // Re-throw after logging
        }
    }
    
    /**
     * Example integration for PaymentMethodScreenController
     * This shows how to enhance payment method selection and validation
     */
    public static void enhancePaymentMethodController(PaymentMethodScreenController controller, OrderEntity order) {
        OrderValidationStateManager stateManager = ServiceFactory.getOrderValidationStateManager();
        PaymentFlowLogger flowLogger = ServiceFactory.getPaymentFlowLogger();
        
        try {
            // Get existing session or create new one
            PaymentFlowLogger.PaymentFlowSession session = flowLogger.getSessionByOrderId(order.getOrderId());
            String sessionId = session != null ? session.getSessionId() : 
                              flowLogger.startPaymentFlowSession(order.getOrderId());
            
            // Log navigation to payment method screen
            flowLogger.logNavigationStep(sessionId, "ORDER_SUMMARY", "PAYMENT_METHOD",
                Map.of("order_validated", true, "payment_amount", order.getTotalAmountPaid()));
            
            // Verify validation state preservation
            boolean hasOrderSummaryValidation = stateManager.hasValidValidation(order.getOrderId(),
                OrderValidationStateManager.ValidationStep.ORDER_SUMMARY_VALIDATION);
            
            if (!hasOrderSummaryValidation) {
                // Log potential validation bypass
                flowLogger.logSecurityEvent(sessionId, "VALIDATION_BYPASS_DETECTED",
                    "User reached payment method screen without proper order summary validation",
                    Map.of("order_id", order.getOrderId(), "bypass_type", "order_summary_validation"));
                
                // Create bypass state for tracking
                stateManager.createValidationState(
                    order.getOrderId(),
                    OrderValidationStateManager.ValidationStep.ORDER_SUMMARY_VALIDATION,
                    OrderValidationStateManager.ValidationResult.BYPASSED,
                    "Reached payment method screen without proper validation"
                );
            }
            
            // Validate order for payment readiness
            OrderValidationStateManager.ValidationSummary summary = stateManager.validateOrderForPayment(order);
            
            flowLogger.logValidationStep(sessionId, "PAYMENT_READINESS_CHECK", summary.isValid(),
                summary.getMessage(),
                Map.of(
                    "has_warnings", summary.hasWarnings(),
                    "has_errors", summary.hasErrors(),
                    "warnings", summary.getWarnings(),
                    "errors", summary.getErrors()
                ));
            
            // Create payment method selection validation state
            if (summary.isValid()) {
                stateManager.createValidationState(
                    order.getOrderId(),
                    OrderValidationStateManager.ValidationStep.PAYMENT_METHOD_SELECTION,
                    OrderValidationStateManager.ValidationResult.PASSED,
                    "Payment method screen validation passed"
                );
            }
            
        } catch (Exception e) {
            PaymentFlowLogger.PaymentFlowSession session = flowLogger.getSessionByOrderId(order.getOrderId());
            String sessionId = session != null ? session.getSessionId() : "NO_SESSION";
            
            flowLogger.logCriticalError(sessionId, "PAYMENT_METHOD_VALIDATION_ERROR", e,
                Map.of(
                    "order_id", order.getOrderId(),
                    "controller", "PaymentMethodScreenController",
                    "validation_context", "payment_readiness_check"
                ));
            
            throw e;
        }
    }
    
    /**
     * Example integration for DeliveryInfoScreenController
     * This shows how to track delivery information validation
     */
    public static void enhanceDeliveryInfoController(DeliveryInfoScreenController controller, OrderEntity order) {
        OrderValidationStateManager stateManager = ServiceFactory.getOrderValidationStateManager();
        PaymentFlowLogger flowLogger = ServiceFactory.getPaymentFlowLogger();
        
        try {
            // Start payment flow session if this is the entry point
            String sessionId = flowLogger.startPaymentFlowSession(order.getOrderId());
            
            // Log navigation to delivery info (usually from cart or home)
            flowLogger.logNavigationStep(sessionId, "CART_SCREEN", "DELIVERY_INFO",
                Map.of("order_created", true, "items_count", order.getOrderItems().size()));
            
            // Example: Log user actions during form filling
            flowLogger.logUserAction(sessionId, "DELIVERY_FORM_OPENED",
                Map.of("order_id", order.getOrderId(), "screen", "DeliveryInfoScreenController"));
            
            // When delivery info is validated (this would be called after form submission)
            if (order.getDeliveryInfo() != null) {
                // Create validation state
                String validationStateId = stateManager.createValidationState(
                    order.getOrderId(),
                    OrderValidationStateManager.ValidationStep.DELIVERY_INFO_VALIDATION,
                    OrderValidationStateManager.ValidationResult.PASSED,
                    "Delivery information validated successfully"
                );
                
                // Log successful validation
                flowLogger.logValidationStep(sessionId, "DELIVERY_INFO_VALIDATION", true,
                    "Delivery information completed and validated",
                    Map.of(
                        "validation_state_id", validationStateId,
                        "delivery_method", order.getDeliveryInfo().getDeliveryMethodChosen(),
                        "recipient", order.getDeliveryInfo().getRecipientName(),
                        "province", order.getDeliveryInfo().getDeliveryProvinceCity()
                    ));
                
                // Log button state change
                flowLogger.logButtonStateChange(sessionId, "proceedToOrderSummaryButton", 
                    "disabled", "enabled", "Delivery info validation completed");
            }
            
        } catch (Exception e) {
            PaymentFlowLogger.PaymentFlowSession session = flowLogger.getSessionByOrderId(order.getOrderId());
            String sessionId = session != null ? session.getSessionId() : "NO_SESSION";
            
            flowLogger.logCriticalError(sessionId, "DELIVERY_INFO_VALIDATION_ERROR", e,
                Map.of(
                    "order_id", order.getOrderId(),
                    "controller", "DeliveryInfoScreenController",
                    "has_delivery_info", order.getDeliveryInfo() != null
                ));
            
            throw e;
        }
    }
    
    /**
     * Example of tracking order state transitions
     * This can be called whenever an order's state changes
     */
    public static void trackOrderStateTransition(OrderEntity orderBefore, OrderEntity orderAfter, String reason) {
        PaymentFlowLogger flowLogger = ServiceFactory.getPaymentFlowLogger();
        
        String orderId = orderAfter != null ? orderAfter.getOrderId() : 
                        (orderBefore != null ? orderBefore.getOrderId() : null);
        
        if (orderId != null) {
            PaymentFlowLogger.PaymentFlowSession session = flowLogger.getSessionByOrderId(orderId);
            String sessionId = session != null ? session.getSessionId() : 
                              flowLogger.startPaymentFlowSession(orderId);
            
            flowLogger.logOrderStateTransition(sessionId, orderBefore, orderAfter, reason);
        }
    }
    
    /**
     * Example of performance monitoring for controller operations
     */
    public static void monitorControllerOperation(String orderId, String operationName, Runnable operation) {
        PaymentFlowLogger flowLogger = ServiceFactory.getPaymentFlowLogger();
        
        PaymentFlowLogger.PaymentFlowSession session = flowLogger.getSessionByOrderId(orderId);
        String sessionId = session != null ? session.getSessionId() : "NO_SESSION";
        
        long startTime = System.currentTimeMillis();
        
        try {
            operation.run();
            
            long endTime = System.currentTimeMillis();
            Duration duration = Duration.ofMillis(endTime - startTime);
            
            flowLogger.logPerformanceMetrics(sessionId, operationName, duration,
                Map.of("success", true, "operation_type", "controller_method"));
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            Duration duration = Duration.ofMillis(endTime - startTime);
            
            flowLogger.logPerformanceMetrics(sessionId, operationName + "_FAILED", duration,
                Map.of("success", false, "error", e.getMessage(), "operation_type", "controller_method"));
            
            flowLogger.logCriticalError(sessionId, "CONTROLLER_OPERATION_FAILURE", e,
                Map.of("operation", operationName, "order_id", orderId));
            
            throw new RuntimeException("Controller operation failed: " + operationName, e);
        }
    }
    
    /**
     * Example cleanup method to be called when payment flow is completed or cancelled
     */
    public static void cleanupPaymentFlow(String orderId, String reason) {
        try {
            PaymentFlowLogger flowLogger = ServiceFactory.getPaymentFlowLogger();
            OrderValidationStateManager stateManager = ServiceFactory.getOrderValidationStateManager();
            
            // End payment flow session
            PaymentFlowLogger.PaymentFlowSession session = flowLogger.getSessionByOrderId(orderId);
            if (session != null) {
                flowLogger.endPaymentFlowSession(session.getSessionId(), reason);
            }
            
            // Invalidate validation states if payment was cancelled
            if ("CANCELLED".equals(reason) || "ERROR".equals(reason)) {
                stateManager.invalidateOrderValidations(orderId);
            }
            
            logger.info("Payment flow cleanup completed for order: " + orderId + ", reason: " + reason);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during payment flow cleanup for order: " + orderId, e);
        }
    }
}