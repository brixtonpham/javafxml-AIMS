package com.aims.core.presentation.utils;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.application.dtos.OrderSummaryDTO;
import com.aims.core.application.dtos.OrderItemDTO;
import com.aims.core.application.dtos.DeliveryInfoDTO;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Enhanced UI Helper for OrderSummaryController
 * 
 * Provides utility methods for enhanced UI population, validation display,
 * and progressive data loading support.
 */
public class OrderSummaryUIHelper {
    
    private static final Logger logger = Logger.getLogger(OrderSummaryUIHelper.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final float VAT_RATE = 0.10f;
    
    /**
     * Format currency amount with proper VND formatting
     */
    public static String formatCurrency(float amount) {
        return String.format("%,.0f VND", amount);
    }
    
    /**
     * Format date with consistent formatting
     */
    public static String formatDate(java.time.LocalDateTime date) {
        return date != null ? date.format(DATE_TIME_FORMATTER) : "N/A";
    }
    
    /**
     * Calculate VAT amount from pricing information
     */
    public static float calculateVATAmount(float totalInclVAT, float totalExclVAT) {
        return totalInclVAT - totalExclVAT;
    }
    
    /**
     * Validate and set text for UI components with null safety
     */
    public static void setTextSafely(Label label, String text) {
        if (label != null) {
            label.setText(text != null ? text : "N/A");
        }
    }
    
    /**
     * Validate and set text for Text components with null safety
     */
    public static void setTextSafely(Text textComponent, String text) {
        if (textComponent != null) {
            textComponent.setText(text != null ? text : "N/A");
        }
    }
    
    /**
     * Create enhanced error display message
     */
    public static String createErrorMessage(String context, String details) {
        return String.format("Error in %s: %s", context, details);
    }
    
    /**
     * Create enhanced warning message for incomplete data
     */
    public static String createDataIncompleteWarning(String missingData) {
        return String.format("Warning: %s is incomplete. Display may be limited.", missingData);
    }
    
    /**
     * Validate order data completeness for UI display
     */
    public static boolean validateOrderDataForUI(OrderEntity order) {
        if (order == null) {
            logger.warning("OrderSummaryUIHelper.validateOrderDataForUI: Order is null");
            return false;
        }
        
        boolean isValid = true;
        StringBuilder issues = new StringBuilder();
        
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            issues.append("Order ID missing; ");
            isValid = false;
        }
        
        if (order.getOrderDate() == null) {
            issues.append("Order date missing; ");
            isValid = false;
        }
        
        if (order.getDeliveryInfo() == null) {
            issues.append("Delivery information missing; ");
            isValid = false;
        }
        
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            issues.append("Order items missing; ");
            isValid = false;
        }
        
        if (order.getTotalAmountPaid() <= 0) {
            issues.append("Invalid total amount; ");
            isValid = false;
        }
        
        if (!isValid) {
            logger.warning("OrderSummaryUIHelper.validateOrderDataForUI: Validation issues found: " + issues.toString());
        }
        
        return isValid;
    }
    
    /**
     * Validate OrderSummaryDTO for UI display
     */
    public static boolean validateOrderSummaryDTOForUI(OrderSummaryDTO dto) {
        if (dto == null) {
            logger.warning("OrderSummaryUIHelper.validateOrderSummaryDTOForUI: DTO is null");
            return false;
        }
        
        boolean isValid = true;
        StringBuilder issues = new StringBuilder();
        
        if (dto.orderId() == null || dto.orderId().trim().isEmpty()) {
            issues.append("Order ID missing in DTO; ");
            isValid = false;
        }
        
        if (dto.items() == null || dto.items().isEmpty()) {
            issues.append("Order items missing in DTO; ");
            isValid = false;
        }
        
        if (dto.deliveryInfo() == null) {
            issues.append("Delivery info missing in DTO; ");
            isValid = false;
        }
        
        if (dto.totalAmountToBePaid() <= 0) {
            issues.append("Invalid total amount in DTO; ");
            isValid = false;
        }
        
        if (!isValid) {
            logger.warning("OrderSummaryUIHelper.validateOrderSummaryDTOForUI: DTO validation issues found: " + issues.toString());
        }
        
        return isValid;
    }
    
    /**
     * Create fallback display text for missing data
     */
    public static String createFallbackText(String fieldName, String defaultValue) {
        return defaultValue != null ? defaultValue : ("No " + fieldName + " available");
    }
    
    /**
     * Format delivery method display text
     */
    public static String formatDeliveryMethod(String method, boolean isRush) {
        if (method == null) {
            return isRush ? "RUSH_DELIVERY" : "STANDARD_DELIVERY";
        }
        return method;
    }
    
    /**
     * Format delivery method from DTO
     */
    public static String formatDeliveryMethod(DeliveryInfoDTO deliveryInfo) {
        if (deliveryInfo == null) {
            return "N/A";
        }
        return deliveryInfo.isRushOrder() ? "RUSH_DELIVERY" : "STANDARD_DELIVERY";
    }
    
    /**
     * Validate rush delivery display requirements
     */
    public static boolean shouldDisplayRushDelivery(DeliveryInfo deliveryInfo) {
        return deliveryInfo != null && 
               "RUSH_DELIVERY".equalsIgnoreCase(deliveryInfo.getDeliveryMethodChosen()) &&
               deliveryInfo.getRequestedRushDeliveryTime() != null;
    }
    
    /**
     * Validate rush delivery display from DTO
     */
    public static boolean shouldDisplayRushDelivery(DeliveryInfoDTO deliveryInfo, 
                                                   com.aims.core.application.dtos.RushDeliveryDetailsDTO rushDetails) {
        return deliveryInfo != null && 
               deliveryInfo.isRushOrder() && 
               rushDetails != null &&
               deliveryInfo.getRequestedRushDeliveryTime() != null;
    }
    
    /**
     * Clear UI container safely
     */
    public static void clearContainer(VBox container) {
        if (container != null) {
            container.getChildren().clear();
        }
    }
    
    /**
     * Set visibility and managed state together
     */
    public static void setVisibilityAndManaged(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }
    
    /**
     * Create enhanced product title display
     */
    public static String createProductDisplayTitle(OrderItem item) {
        if (item == null) {
            return "Unknown Product";
        }
        
        String title = "Unknown Product";
        if (item.getProduct() != null && item.getProduct().getTitle() != null) {
            title = item.getProduct().getTitle();
        }
        
        return title + " (x" + item.getQuantity() + ")";
    }
    
    /**
     * Create enhanced product title display from DTO
     */
    public static String createProductDisplayTitle(OrderItemDTO itemDTO) {
        if (itemDTO == null) {
            return "Unknown Product";
        }
        
        String title = itemDTO.getTitle() != null ? itemDTO.getTitle() : "Unknown Product";
        return title + " (x" + itemDTO.getQuantity() + ")";
    }
    
    /**
     * Format item pricing display
     */
    public static String formatItemPricing(float unitPrice, int quantity) {
        return String.format("Price/unit: %,.0f VND, Total: %,.0f VND", 
                           unitPrice, unitPrice * quantity);
    }
    
    /**
     * Format item pricing from DTO
     */
    public static String formatItemPricingFromDTO(OrderItemDTO itemDTO) {
        return formatItemPricing(itemDTO.getPriceAtTimeOfOrder(), itemDTO.getQuantity());
    }
    
    /**
     * Create comprehensive error message for UI display
     */
    public static String createComprehensiveErrorMessage(String operation, Exception error) {
        StringBuilder message = new StringBuilder();
        message.append("Error during ").append(operation).append(": ");
        
        if (error.getMessage() != null) {
            message.append(error.getMessage());
        } else {
            message.append("Unknown error occurred");
        }
        
        // Add suggestion for user action
        message.append(". Please try refreshing or contact support if the problem persists.");
        
        return message.toString();
    }
    
    /**
     * Log UI operation for debugging
     */
    public static void logUIOperation(String operation, String details) {
        logger.info(String.format("OrderSummaryUIHelper.%s: %s", operation, details));
    }
    
    /**
     * Log UI warning for debugging
     */
    public static void logUIWarning(String operation, String warning) {
        logger.warning(String.format("OrderSummaryUIHelper.%s: %s", operation, warning));
    }
    
    /**
     * Log UI error for debugging
     */
    public static void logUIError(String operation, Exception error) {
        logger.log(Level.SEVERE, String.format("OrderSummaryUIHelper.%s: Error occurred", operation), error);
    }
}