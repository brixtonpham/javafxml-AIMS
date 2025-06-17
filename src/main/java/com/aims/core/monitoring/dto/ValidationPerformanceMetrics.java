package com.aims.core.monitoring.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Validation Performance Metrics DTO
 * 
 * Tracks performance and effectiveness of validation services
 * to ensure optimal system performance and validation quality.
 */
public class ValidationPerformanceMetrics {
    private String validationType;
    private long executionTimeMs;
    private boolean validationPassed;
    private String orderId;
    private LocalDateTime timestamp;
    private int validationRulesApplied;
    private int warningsGenerated;
    private int errorsDetected;
    private String validationSource;
    private Map<String, Object> performanceDetails;
    private boolean performanceThresholdMet;
    private String validationEngine;
    
    public ValidationPerformanceMetrics() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ValidationPerformanceMetrics metrics = new ValidationPerformanceMetrics();
        
        public Builder validationType(String validationType) {
            metrics.validationType = validationType;
            return this;
        }
        
        public Builder executionTimeMs(long executionTimeMs) {
            metrics.executionTimeMs = executionTimeMs;
            return this;
        }
        
        public Builder validationPassed(boolean validationPassed) {
            metrics.validationPassed = validationPassed;
            return this;
        }
        
        public Builder orderId(String orderId) {
            metrics.orderId = orderId;
            return this;
        }
        
        public Builder validationRulesApplied(int validationRulesApplied) {
            metrics.validationRulesApplied = validationRulesApplied;
            return this;
        }
        
        public Builder warningsGenerated(int warningsGenerated) {
            metrics.warningsGenerated = warningsGenerated;
            return this;
        }
        
        public Builder errorsDetected(int errorsDetected) {
            metrics.errorsDetected = errorsDetected;
            return this;
        }
        
        public Builder validationSource(String validationSource) {
            metrics.validationSource = validationSource;
            return this;
        }
        
        public Builder performanceDetails(Map<String, Object> performanceDetails) {
            metrics.performanceDetails = performanceDetails;
            return this;
        }
        
        public Builder validationEngine(String validationEngine) {
            metrics.validationEngine = validationEngine;
            return this;
        }
        
        public ValidationPerformanceMetrics build() {
            // Determine if performance threshold was met (< 2 seconds for validation)
            metrics.performanceThresholdMet = metrics.executionTimeMs < 2000;
            return metrics;
        }
    }
    
    // Getters and setters
    public String getValidationType() { return validationType; }
    public void setValidationType(String validationType) { this.validationType = validationType; }
    
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    
    public boolean isValidationPassed() { return validationPassed; }
    public void setValidationPassed(boolean validationPassed) { this.validationPassed = validationPassed; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public int getValidationRulesApplied() { return validationRulesApplied; }
    public void setValidationRulesApplied(int validationRulesApplied) { this.validationRulesApplied = validationRulesApplied; }
    
    public int getWarningsGenerated() { return warningsGenerated; }
    public void setWarningsGenerated(int warningsGenerated) { this.warningsGenerated = warningsGenerated; }
    
    public int getErrorsDetected() { return errorsDetected; }
    public void setErrorsDetected(int errorsDetected) { this.errorsDetected = errorsDetected; }
    
    public String getValidationSource() { return validationSource; }
    public void setValidationSource(String validationSource) { this.validationSource = validationSource; }
    
    public Map<String, Object> getPerformanceDetails() { return performanceDetails; }
    public void setPerformanceDetails(Map<String, Object> performanceDetails) { this.performanceDetails = performanceDetails; }
    
    public boolean isPerformanceThresholdMet() { return performanceThresholdMet; }
    public void setPerformanceThresholdMet(boolean performanceThresholdMet) { this.performanceThresholdMet = performanceThresholdMet; }
    
    public String getValidationEngine() { return validationEngine; }
    public void setValidationEngine(String validationEngine) { this.validationEngine = validationEngine; }
    
    // Utility methods
    public String getPerformanceStatus() {
        if (performanceThresholdMet) {
            return "OPTIMAL";
        } else if (executionTimeMs < 5000) {
            return "ACCEPTABLE";
        } else {
            return "SLOW";
        }
    }
    
    public boolean hasIssues() {
        return errorsDetected > 0 || warningsGenerated > 3 || !performanceThresholdMet;
    }
    
    public float getValidationEfficiency() {
        if (validationRulesApplied == 0) return 0.0f;
        return ((float) (validationRulesApplied - errorsDetected) / validationRulesApplied) * 100.0f;
    }
    
    @Override
    public String toString() {
        return String.format("ValidationPerformanceMetrics{type='%s', executionTime=%dms, " +
                           "passed=%s, rules=%d, errors=%d, performance='%s'}", 
                           validationType, executionTimeMs, validationPassed, 
                           validationRulesApplied, errorsDetected, getPerformanceStatus());
    }
}