package com.aims.core.monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Flow Anomaly DTO
 * 
 * Represents detected anomalies in the cart-to-order data flow
 * for investigation and resolution tracking.
 */
public class DataFlowAnomaly {
    private String anomalyId;
    private String orderId;
    private String sessionId;
    private String anomalyType;
    private String severity;
    private String description;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private String status;
    private String detectionSource;
    private Map<String, Object> anomalyData;
    private List<String> affectedFields;
    private String rootCause;
    private List<String> recommendedActions;
    private String assignedTo;
    private List<String> investigationNotes;
    private boolean requiresManualIntervention;
    private String resolutionDetails;
    
    public DataFlowAnomaly() {
        this.detectedAt = LocalDateTime.now();
        this.status = "DETECTED";
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private DataFlowAnomaly anomaly = new DataFlowAnomaly();
        
        public Builder anomalyId(String anomalyId) {
            anomaly.anomalyId = anomalyId;
            return this;
        }
        
        public Builder orderId(String orderId) {
            anomaly.orderId = orderId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            anomaly.sessionId = sessionId;
            return this;
        }
        
        public Builder anomalyType(String anomalyType) {
            anomaly.anomalyType = anomalyType;
            return this;
        }
        
        public Builder severity(String severity) {
            anomaly.severity = severity;
            return this;
        }
        
        public Builder description(String description) {
            anomaly.description = description;
            return this;
        }
        
        public Builder detectionSource(String detectionSource) {
            anomaly.detectionSource = detectionSource;
            return this;
        }
        
        public Builder anomalyData(Map<String, Object> anomalyData) {
            anomaly.anomalyData = anomalyData;
            return this;
        }
        
        public Builder affectedFields(List<String> affectedFields) {
            anomaly.affectedFields = affectedFields;
            return this;
        }
        
        public Builder rootCause(String rootCause) {
            anomaly.rootCause = rootCause;
            return this;
        }
        
        public Builder recommendedActions(List<String> recommendedActions) {
            anomaly.recommendedActions = recommendedActions;
            return this;
        }
        
        public Builder assignedTo(String assignedTo) {
            anomaly.assignedTo = assignedTo;
            return this;
        }
        
        public Builder investigationNotes(List<String> investigationNotes) {
            anomaly.investigationNotes = investigationNotes;
            return this;
        }
        
        public Builder requiresManualIntervention(boolean requiresManualIntervention) {
            anomaly.requiresManualIntervention = requiresManualIntervention;
            return this;
        }
        
        public DataFlowAnomaly build() {
            // Generate ID if not provided
            if (anomaly.anomalyId == null) {
                anomaly.anomalyId = generateAnomalyId();
            }
            return anomaly;
        }
        
        private String generateAnomalyId() {
            return "ANOM-" + System.currentTimeMillis() + "-" + 
                   (anomaly.orderId != null ? anomaly.orderId.hashCode() : "UNKNOWN");
        }
    }
    
    // Getters and setters
    public String getAnomalyId() { return anomalyId; }
    public void setAnomalyId(String anomalyId) { this.anomalyId = anomalyId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
    
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getDetectionSource() { return detectionSource; }
    public void setDetectionSource(String detectionSource) { this.detectionSource = detectionSource; }
    
    public Map<String, Object> getAnomalyData() { return anomalyData; }
    public void setAnomalyData(Map<String, Object> anomalyData) { this.anomalyData = anomalyData; }
    
    public List<String> getAffectedFields() { return affectedFields; }
    public void setAffectedFields(List<String> affectedFields) { this.affectedFields = affectedFields; }
    
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    
    public List<String> getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }
    
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    
    public List<String> getInvestigationNotes() { return investigationNotes; }
    public void setInvestigationNotes(List<String> investigationNotes) { this.investigationNotes = investigationNotes; }
    
    public boolean isRequiresManualIntervention() { return requiresManualIntervention; }
    public void setRequiresManualIntervention(boolean requiresManualIntervention) { this.requiresManualIntervention = requiresManualIntervention; }
    
    public String getResolutionDetails() { return resolutionDetails; }
    public void setResolutionDetails(String resolutionDetails) { this.resolutionDetails = resolutionDetails; }
    
    // Utility methods
    public boolean isResolved() {
        return "RESOLVED".equals(status) && resolvedAt != null;
    }
    
    public boolean isCritical() {
        return "CRITICAL".equals(severity) || "HIGH".equals(severity);
    }
    
    public boolean isActive() {
        return "DETECTED".equals(status) || "INVESTIGATING".equals(status) || "IN_PROGRESS".equals(status);
    }
    
    public long getAgeInHours() {
        if (detectedAt == null) return 0;
        return java.time.Duration.between(detectedAt, LocalDateTime.now()).toHours();
    }
    
    public boolean isOverdue() {
        long ageHours = getAgeInHours();
        if (isCritical()) {
            return ageHours > 4; // Critical issues should be resolved within 4 hours
        } else if ("MEDIUM".equals(severity)) {
            return ageHours > 24; // Medium issues within 24 hours
        } else {
            return ageHours > 72; // Low issues within 72 hours
        }
    }
    
    public void markAsInvestigating(String investigator) {
        this.status = "INVESTIGATING";
        this.assignedTo = investigator;
    }
    
    public void markAsResolved(String resolutionDetails) {
        this.status = "RESOLVED";
        this.resolvedAt = LocalDateTime.now();
        this.resolutionDetails = resolutionDetails;
    }
    
    public void addInvestigationNote(String note) {
        if (this.investigationNotes == null) {
            this.investigationNotes = new java.util.ArrayList<>();
        }
        this.investigationNotes.add(LocalDateTime.now() + ": " + note);
    }
    
    @Override
    public String toString() {
        return String.format("DataFlowAnomaly{id='%s', type='%s', severity='%s', " +
                           "status='%s', orderId='%s', age=%dh, overdue=%s}", 
                           anomalyId, anomalyType, severity, status, orderId, 
                           getAgeInHours(), isOverdue());
    }
    
    // Common anomaly types as constants
    public static final String TYPE_DATA_LOSS = "DATA_LOSS";
    public static final String TYPE_INCONSISTENT_STATE = "INCONSISTENT_STATE";
    public static final String TYPE_VALIDATION_FAILURE = "VALIDATION_FAILURE";
    public static final String TYPE_PERFORMANCE_DEGRADATION = "PERFORMANCE_DEGRADATION";
    public static final String TYPE_CONVERSION_FAILURE = "CONVERSION_FAILURE";
    public static final String TYPE_MISSING_METADATA = "MISSING_METADATA";
    
    // Severity levels as constants
    public static final String SEVERITY_CRITICAL = "CRITICAL";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_LOW = "LOW";
    
    // Status values as constants
    public static final String STATUS_DETECTED = "DETECTED";
    public static final String STATUS_INVESTIGATING = "INVESTIGATING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_DISMISSED = "DISMISSED";
}