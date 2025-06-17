package com.aims.core.monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Completeness Metrics DTO
 * 
 * Tracks order data completeness throughout the customer journey
 * to ensure all required information is present at each stage.
 */
public class OrderCompletenessMetrics {
    private String orderId;
    private String screen;
    private LocalDateTime checkTime;
    private boolean hasOrderItems;
    private boolean hasDeliveryInfo;
    private boolean hasPricingInfo;
    private boolean hasCustomerInfo;
    private boolean hasPaymentInfo;
    private boolean hasProductMetadata;
    private float completenessPercentage;
    private List<String> missingComponents;
    private List<String> validationWarnings;
    private String customerType; // guest or registered
    private int itemCount;
    private boolean isRushDelivery;
    
    public OrderCompletenessMetrics() {
        this.checkTime = LocalDateTime.now();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private OrderCompletenessMetrics metrics = new OrderCompletenessMetrics();
        
        public Builder orderId(String orderId) {
            metrics.orderId = orderId;
            return this;
        }
        
        public Builder screen(String screen) {
            metrics.screen = screen;
            return this;
        }
        
        public Builder hasOrderItems(boolean hasOrderItems) {
            metrics.hasOrderItems = hasOrderItems;
            return this;
        }
        
        public Builder hasDeliveryInfo(boolean hasDeliveryInfo) {
            metrics.hasDeliveryInfo = hasDeliveryInfo;
            return this;
        }
        
        public Builder hasPricingInfo(boolean hasPricingInfo) {
            metrics.hasPricingInfo = hasPricingInfo;
            return this;
        }
        
        public Builder hasCustomerInfo(boolean hasCustomerInfo) {
            metrics.hasCustomerInfo = hasCustomerInfo;
            return this;
        }
        
        public Builder hasPaymentInfo(boolean hasPaymentInfo) {
            metrics.hasPaymentInfo = hasPaymentInfo;
            return this;
        }
        
        public Builder hasProductMetadata(boolean hasProductMetadata) {
            metrics.hasProductMetadata = hasProductMetadata;
            return this;
        }
        
        public Builder missingComponents(List<String> missingComponents) {
            metrics.missingComponents = missingComponents;
            return this;
        }
        
        public Builder validationWarnings(List<String> validationWarnings) {
            metrics.validationWarnings = validationWarnings;
            return this;
        }
        
        public Builder customerType(String customerType) {
            metrics.customerType = customerType;
            return this;
        }
        
        public Builder itemCount(int itemCount) {
            metrics.itemCount = itemCount;
            return this;
        }
        
        public Builder isRushDelivery(boolean isRushDelivery) {
            metrics.isRushDelivery = isRushDelivery;
            return this;
        }
        
        public OrderCompletenessMetrics build() {
            // Calculate completeness percentage
            metrics.completenessPercentage = calculateCompleteness();
            return metrics;
        }
        
        private float calculateCompleteness() {
            int totalChecks = 6; // Number of completeness checks
            int passedChecks = 0;
            
            if (metrics.hasOrderItems) passedChecks++;
            if (metrics.hasDeliveryInfo) passedChecks++;
            if (metrics.hasPricingInfo) passedChecks++;
            if (metrics.hasCustomerInfo) passedChecks++;
            if (metrics.hasPaymentInfo) passedChecks++;
            if (metrics.hasProductMetadata) passedChecks++;
            
            return ((float) passedChecks / totalChecks) * 100.0f;
        }
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getScreen() { return screen; }
    public void setScreen(String screen) { this.screen = screen; }
    
    public LocalDateTime getCheckTime() { return checkTime; }
    public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
    
    public boolean isHasOrderItems() { return hasOrderItems; }
    public void setHasOrderItems(boolean hasOrderItems) { this.hasOrderItems = hasOrderItems; }
    
    public boolean isHasDeliveryInfo() { return hasDeliveryInfo; }
    public void setHasDeliveryInfo(boolean hasDeliveryInfo) { this.hasDeliveryInfo = hasDeliveryInfo; }
    
    public boolean isHasPricingInfo() { return hasPricingInfo; }
    public void setHasPricingInfo(boolean hasPricingInfo) { this.hasPricingInfo = hasPricingInfo; }
    
    public boolean isHasCustomerInfo() { return hasCustomerInfo; }
    public void setHasCustomerInfo(boolean hasCustomerInfo) { this.hasCustomerInfo = hasCustomerInfo; }
    
    public boolean isHasPaymentInfo() { return hasPaymentInfo; }
    public void setHasPaymentInfo(boolean hasPaymentInfo) { this.hasPaymentInfo = hasPaymentInfo; }
    
    public boolean isHasProductMetadata() { return hasProductMetadata; }
    public void setHasProductMetadata(boolean hasProductMetadata) { this.hasProductMetadata = hasProductMetadata; }
    
    public float getCompletenessPercentage() { return completenessPercentage; }
    public void setCompletenessPercentage(float completenessPercentage) { this.completenessPercentage = completenessPercentage; }
    
    public List<String> getMissingComponents() { return missingComponents; }
    public void setMissingComponents(List<String> missingComponents) { this.missingComponents = missingComponents; }
    
    public List<String> getValidationWarnings() { return validationWarnings; }
    public void setValidationWarnings(List<String> validationWarnings) { this.validationWarnings = validationWarnings; }
    
    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }
    
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    
    public boolean isRushDelivery() { return isRushDelivery; }
    public void setRushDelivery(boolean rushDelivery) { isRushDelivery = rushDelivery; }
    
    // Utility methods
    public boolean isFullyComplete() {
        return completenessPercentage >= 100.0f;
    }
    
    public boolean hasWarnings() {
        return validationWarnings != null && !validationWarnings.isEmpty();
    }
    
    public String getCompletenessStatus() {
        if (completenessPercentage >= 100.0f) {
            return "COMPLETE";
        } else if (completenessPercentage >= 75.0f) {
            return "MOSTLY_COMPLETE";
        } else if (completenessPercentage >= 50.0f) {
            return "PARTIALLY_COMPLETE";
        } else {
            return "INCOMPLETE";
        }
    }
    
    @Override
    public String toString() {
        return String.format("OrderCompletenessMetrics{orderId='%s', screen='%s', " +
                           "completeness=%.1f%%, status='%s', itemCount=%d}", 
                           orderId, screen, completenessPercentage, getCompletenessStatus(), itemCount);
    }
}