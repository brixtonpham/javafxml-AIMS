package com.aims.core.monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Metrics DTO
 * 
 * Captures comprehensive metrics for cart-to-order data transfer operations
 * including timing, completeness, and quality indicators.
 */
public class DataTransferMetrics {
    private String cartSessionId;
    private String orderId;
    private int itemCount;
    private long transferStartTime;
    private long transferEndTime;
    private boolean dataComplete;
    private List<String> missingFields;
    private Map<String, Object> additionalMetrics;
    private LocalDateTime timestamp;
    private float completenessPercentage;
    private String transferSource;
    private String transferDestination;
    private boolean validationPassed;
    private List<String> validationErrors;
    
    public DataTransferMetrics() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Builder pattern for easier construction
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private DataTransferMetrics metrics = new DataTransferMetrics();
        
        public Builder cartSessionId(String cartSessionId) {
            metrics.cartSessionId = cartSessionId;
            return this;
        }
        
        public Builder orderId(String orderId) {
            metrics.orderId = orderId;
            return this;
        }
        
        public Builder itemCount(int itemCount) {
            metrics.itemCount = itemCount;
            return this;
        }
        
        public Builder transferStartTime(long transferStartTime) {
            metrics.transferStartTime = transferStartTime;
            return this;
        }
        
        public Builder transferEndTime(long transferEndTime) {
            metrics.transferEndTime = transferEndTime;
            return this;
        }
        
        public Builder dataComplete(boolean dataComplete) {
            metrics.dataComplete = dataComplete;
            return this;
        }
        
        public Builder missingFields(List<String> missingFields) {
            metrics.missingFields = missingFields;
            return this;
        }
        
        public Builder additionalMetrics(Map<String, Object> additionalMetrics) {
            metrics.additionalMetrics = additionalMetrics;
            return this;
        }
        
        public Builder completenessPercentage(float completenessPercentage) {
            metrics.completenessPercentage = completenessPercentage;
            return this;
        }
        
        public Builder transferSource(String transferSource) {
            metrics.transferSource = transferSource;
            return this;
        }
        
        public Builder transferDestination(String transferDestination) {
            metrics.transferDestination = transferDestination;
            return this;
        }
        
        public Builder validationPassed(boolean validationPassed) {
            metrics.validationPassed = validationPassed;
            return this;
        }
        
        public Builder validationErrors(List<String> validationErrors) {
            metrics.validationErrors = validationErrors;
            return this;
        }
        
        public DataTransferMetrics build() {
            // Calculate completion percentage if not set
            if (metrics.completenessPercentage == 0 && metrics.missingFields != null) {
                metrics.completenessPercentage = calculateCompleteness();
            }
            return metrics;
        }
        
        private float calculateCompleteness() {
            if (metrics.missingFields == null || metrics.missingFields.isEmpty()) {
                return 100.0f;
            }
            // Simple calculation - could be more sophisticated
            int totalFields = 10; // Assume 10 critical fields
            int missingCount = metrics.missingFields.size();
            return ((float) (totalFields - missingCount) / totalFields) * 100.0f;
        }
    }
    
    // Getters and setters
    public String getCartSessionId() { return cartSessionId; }
    public void setCartSessionId(String cartSessionId) { this.cartSessionId = cartSessionId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    
    public long getTransferStartTime() { return transferStartTime; }
    public void setTransferStartTime(long transferStartTime) { this.transferStartTime = transferStartTime; }
    
    public long getTransferEndTime() { return transferEndTime; }
    public void setTransferEndTime(long transferEndTime) { this.transferEndTime = transferEndTime; }
    
    public boolean isDataComplete() { return dataComplete; }
    public void setDataComplete(boolean dataComplete) { this.dataComplete = dataComplete; }
    
    public List<String> getMissingFields() { return missingFields; }
    public void setMissingFields(List<String> missingFields) { this.missingFields = missingFields; }
    
    public Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }
    public void setAdditionalMetrics(Map<String, Object> additionalMetrics) { this.additionalMetrics = additionalMetrics; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public float getCompletenessPercentage() { return completenessPercentage; }
    public void setCompletenessPercentage(float completenessPercentage) { this.completenessPercentage = completenessPercentage; }
    
    public String getTransferSource() { return transferSource; }
    public void setTransferSource(String transferSource) { this.transferSource = transferSource; }
    
    public String getTransferDestination() { return transferDestination; }
    public void setTransferDestination(String transferDestination) { this.transferDestination = transferDestination; }
    
    public boolean isValidationPassed() { return validationPassed; }
    public void setValidationPassed(boolean validationPassed) { this.validationPassed = validationPassed; }
    
    public List<String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }
    
    // Utility methods
    public long getTransferDurationMs() {
        if (transferEndTime > 0 && transferStartTime > 0) {
            return transferEndTime - transferStartTime;
        }
        return 0;
    }
    
    public boolean isPerformanceAcceptable() {
        long duration = getTransferDurationMs();
        return duration < 5000; // 5 seconds threshold
    }
    
    @Override
    public String toString() {
        return String.format("DataTransferMetrics{cartSessionId='%s', orderId='%s', itemCount=%d, " +
                           "dataComplete=%s, completenessPercentage=%.1f%%, duration=%dms}", 
                           cartSessionId, orderId, itemCount, dataComplete, completenessPercentage, getTransferDurationMs());
    }
}