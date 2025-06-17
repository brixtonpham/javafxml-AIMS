package com.aims.core.application.dtos.validation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation result specifically for delivery information with delivery-specific context
 */
public class DeliveryValidationResult {
    private boolean isValid;
    private ValidationSeverity severity;
    private List<ValidationIssue> issues;
    private List<String> recoverySuggestions;
    private LocalDateTime validationTimestamp;
    private boolean rushDeliveryEligible;
    private String deliveryMethod;
    private String addressValidationStatus;
    
    public DeliveryValidationResult() {
        this.issues = new ArrayList<>();
        this.recoverySuggestions = new ArrayList<>();
        this.validationTimestamp = LocalDateTime.now();
        this.severity = ValidationSeverity.INFO;
        this.isValid = true;
        this.rushDeliveryEligible = false;
    }
    
    // Getters and setters
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public ValidationSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(ValidationSeverity severity) {
        this.severity = severity;
    }
    
    public List<ValidationIssue> getIssues() {
        return issues;
    }
    
    public void setIssues(List<ValidationIssue> issues) {
        this.issues = issues != null ? issues : new ArrayList<>();
        updateValidationState();
    }
    
    public List<String> getRecoverySuggestions() {
        return recoverySuggestions;
    }
    
    public void setRecoverySuggestions(List<String> recoverySuggestions) {
        this.recoverySuggestions = recoverySuggestions != null ? recoverySuggestions : new ArrayList<>();
    }
    
    public LocalDateTime getValidationTimestamp() {
        return validationTimestamp;
    }
    
    public void setValidationTimestamp(LocalDateTime validationTimestamp) {
        this.validationTimestamp = validationTimestamp;
    }
    
    public boolean isRushDeliveryEligible() {
        return rushDeliveryEligible;
    }
    
    public void setRushDeliveryEligible(boolean rushDeliveryEligible) {
        this.rushDeliveryEligible = rushDeliveryEligible;
    }
    
    public String getDeliveryMethod() {
        return deliveryMethod;
    }
    
    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }
    
    public String getAddressValidationStatus() {
        return addressValidationStatus;
    }
    
    public void setAddressValidationStatus(String addressValidationStatus) {
        this.addressValidationStatus = addressValidationStatus;
    }
    
    // Utility methods
    public void addIssue(ValidationIssue issue) {
        if (issue != null) {
            this.issues.add(issue);
            updateValidationState();
        }
    }
    
    public void addRecoverySuggestion(String suggestion) {
        if (suggestion != null && !suggestion.trim().isEmpty()) {
            this.recoverySuggestions.add(suggestion);
        }
    }
    
    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> 
            issue.getSeverity() == ValidationSeverity.ERROR || 
            issue.getSeverity() == ValidationSeverity.CRITICAL);
    }
    
    private void updateValidationState() {
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == ValidationSeverity.CRITICAL)) {
            this.isValid = false;
            this.severity = ValidationSeverity.CRITICAL;
        } else if (issues.stream().anyMatch(issue -> issue.getSeverity() == ValidationSeverity.ERROR)) {
            this.isValid = false;
            this.severity = ValidationSeverity.ERROR;
        } else if (issues.stream().anyMatch(issue -> issue.getSeverity() == ValidationSeverity.WARNING)) {
            this.isValid = true;
            this.severity = ValidationSeverity.WARNING;
        } else {
            this.isValid = true;
            this.severity = ValidationSeverity.INFO;
        }
    }
    
    @Override
    public String toString() {
        return "DeliveryValidationResult{" +
                "isValid=" + isValid +
                ", severity=" + severity +
                ", deliveryMethod='" + deliveryMethod + '\'' +
                ", rushEligible=" + rushDeliveryEligible +
                ", issueCount=" + issues.size() +
                '}';
    }
}