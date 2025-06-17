package com.aims.core.monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Consistency Metrics DTO
 * 
 * Tracks data consistency across different screens and transitions
 * to ensure data integrity throughout the customer journey.
 */
public class DataConsistencyMetrics {
    private String orderId;
    private String fromScreen;
    private String toScreen;
    private LocalDateTime transitionTime;
    private boolean dataConsistent;
    private List<String> inconsistencies;
    private Map<String, Object> beforeTransitionData;
    private Map<String, Object> afterTransitionData;
    private float consistencyScore;
    private String transitionType;
    private List<String> fieldsChecked;
    private List<String> fieldsModified;
    private boolean criticalDataLoss;
    private String sessionId;
    
    public DataConsistencyMetrics() {
        this.transitionTime = LocalDateTime.now();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private DataConsistencyMetrics metrics = new DataConsistencyMetrics();
        
        public Builder orderId(String orderId) {
            metrics.orderId = orderId;
            return this;
        }
        
        public Builder fromScreen(String fromScreen) {
            metrics.fromScreen = fromScreen;
            return this;
        }
        
        public Builder toScreen(String toScreen) {
            metrics.toScreen = toScreen;
            return this;
        }
        
        public Builder dataConsistent(boolean dataConsistent) {
            metrics.dataConsistent = dataConsistent;
            return this;
        }
        
        public Builder inconsistencies(List<String> inconsistencies) {
            metrics.inconsistencies = inconsistencies;
            return this;
        }
        
        public Builder beforeTransitionData(Map<String, Object> beforeTransitionData) {
            metrics.beforeTransitionData = beforeTransitionData;
            return this;
        }
        
        public Builder afterTransitionData(Map<String, Object> afterTransitionData) {
            metrics.afterTransitionData = afterTransitionData;
            return this;
        }
        
        public Builder transitionType(String transitionType) {
            metrics.transitionType = transitionType;
            return this;
        }
        
        public Builder fieldsChecked(List<String> fieldsChecked) {
            metrics.fieldsChecked = fieldsChecked;
            return this;
        }
        
        public Builder fieldsModified(List<String> fieldsModified) {
            metrics.fieldsModified = fieldsModified;
            return this;
        }
        
        public Builder criticalDataLoss(boolean criticalDataLoss) {
            metrics.criticalDataLoss = criticalDataLoss;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            metrics.sessionId = sessionId;
            return this;
        }
        
        public DataConsistencyMetrics build() {
            // Calculate consistency score
            metrics.consistencyScore = calculateConsistencyScore();
            return metrics;
        }
        
        private float calculateConsistencyScore() {
            if (metrics.fieldsChecked == null || metrics.fieldsChecked.isEmpty()) {
                return 100.0f; // Default score if no fields to check
            }
            
            int totalFields = metrics.fieldsChecked.size();
            int inconsistentFields = metrics.inconsistencies != null ? metrics.inconsistencies.size() : 0;
            
            if (metrics.criticalDataLoss) {
                return 0.0f; // Critical failure
            }
            
            return ((float) (totalFields - inconsistentFields) / totalFields) * 100.0f;
        }
    }
    
    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getFromScreen() { return fromScreen; }
    public void setFromScreen(String fromScreen) { this.fromScreen = fromScreen; }
    
    public String getToScreen() { return toScreen; }
    public void setToScreen(String toScreen) { this.toScreen = toScreen; }
    
    public LocalDateTime getTransitionTime() { return transitionTime; }
    public void setTransitionTime(LocalDateTime transitionTime) { this.transitionTime = transitionTime; }
    
    public boolean isDataConsistent() { return dataConsistent; }
    public void setDataConsistent(boolean dataConsistent) { this.dataConsistent = dataConsistent; }
    
    public List<String> getInconsistencies() { return inconsistencies; }
    public void setInconsistencies(List<String> inconsistencies) { this.inconsistencies = inconsistencies; }
    
    public Map<String, Object> getBeforeTransitionData() { return beforeTransitionData; }
    public void setBeforeTransitionData(Map<String, Object> beforeTransitionData) { this.beforeTransitionData = beforeTransitionData; }
    
    public Map<String, Object> getAfterTransitionData() { return afterTransitionData; }
    public void setAfterTransitionData(Map<String, Object> afterTransitionData) { this.afterTransitionData = afterTransitionData; }
    
    public float getConsistencyScore() { return consistencyScore; }
    public void setConsistencyScore(float consistencyScore) { this.consistencyScore = consistencyScore; }
    
    public String getTransitionType() { return transitionType; }
    public void setTransitionType(String transitionType) { this.transitionType = transitionType; }
    
    public List<String> getFieldsChecked() { return fieldsChecked; }
    public void setFieldsChecked(List<String> fieldsChecked) { this.fieldsChecked = fieldsChecked; }
    
    public List<String> getFieldsModified() { return fieldsModified; }
    public void setFieldsModified(List<String> fieldsModified) { this.fieldsModified = fieldsModified; }
    
    public boolean isCriticalDataLoss() { return criticalDataLoss; }
    public void setCriticalDataLoss(boolean criticalDataLoss) { this.criticalDataLoss = criticalDataLoss; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    // Utility methods
    public String getConsistencyStatus() {
        if (criticalDataLoss) {
            return "CRITICAL_FAILURE";
        } else if (consistencyScore >= 95.0f) {
            return "EXCELLENT";
        } else if (consistencyScore >= 85.0f) {
            return "GOOD";
        } else if (consistencyScore >= 70.0f) {
            return "ACCEPTABLE";
        } else {
            return "POOR";
        }
    }
    
    public boolean hasInconsistencies() {
        return inconsistencies != null && !inconsistencies.isEmpty();
    }
    
    public int getInconsistencyCount() {
        return inconsistencies != null ? inconsistencies.size() : 0;
    }
    
    public String getTransitionPath() {
        return fromScreen + " -> " + toScreen;
    }
    
    public boolean requiresAttention() {
        return criticalDataLoss || consistencyScore < 85.0f || getInconsistencyCount() > 2;
    }
    
    @Override
    public String toString() {
        return String.format("DataConsistencyMetrics{orderId='%s', transition='%s', " +
                           "consistent=%s, score=%.1f%%, status='%s', inconsistencies=%d}", 
                           orderId, getTransitionPath(), dataConsistent, 
                           consistencyScore, getConsistencyStatus(), getInconsistencyCount());
    }
}