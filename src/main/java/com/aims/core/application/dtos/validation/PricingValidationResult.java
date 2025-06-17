package com.aims.core.application.dtos.validation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation result specifically for pricing calculations with pricing-specific context
 */
public class PricingValidationResult {
    private boolean isValid;
    private ValidationSeverity severity;
    private List<ValidationIssue> issues;
    private List<String> recoverySuggestions;
    private LocalDateTime validationTimestamp;
    private float calculatedSubtotal;
    private float calculatedVAT;
    private float calculatedDeliveryFee;
    private float calculatedTotal;
    private float tolerance;
    
    public PricingValidationResult() {
        this.issues = new ArrayList<>();
        this.recoverySuggestions = new ArrayList<>();
        this.validationTimestamp = LocalDateTime.now();
        this.severity = ValidationSeverity.INFO;
        this.isValid = true;
        this.tolerance = 0.01f; // Default tolerance for floating point comparisons
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
    
    public float getCalculatedSubtotal() {
        return calculatedSubtotal;
    }
    
    public void setCalculatedSubtotal(float calculatedSubtotal) {
        this.calculatedSubtotal = calculatedSubtotal;
    }
    
    public float getCalculatedVAT() {
        return calculatedVAT;
    }
    
    public void setCalculatedVAT(float calculatedVAT) {
        this.calculatedVAT = calculatedVAT;
    }
    
    public float getCalculatedDeliveryFee() {
        return calculatedDeliveryFee;
    }
    
    public void setCalculatedDeliveryFee(float calculatedDeliveryFee) {
        this.calculatedDeliveryFee = calculatedDeliveryFee;
    }
    
    public float getCalculatedTotal() {
        return calculatedTotal;
    }
    
    public void setCalculatedTotal(float calculatedTotal) {
        this.calculatedTotal = calculatedTotal;
    }
    
    public float getTolerance() {
        return tolerance;
    }
    
    public void setTolerance(float tolerance) {
        this.tolerance = tolerance;
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
    
    /**
     * Checks if two values are equal within the tolerance
     */
    public boolean isWithinTolerance(float value1, float value2) {
        return Math.abs(value1 - value2) <= tolerance;
    }
    
    /**
     * Gets the pricing summary for display
     */
    public String getPricingSummary() {
        return String.format("Subtotal: %.2f, VAT: %.2f, Delivery: %.2f, Total: %.2f", 
                           calculatedSubtotal, calculatedVAT, calculatedDeliveryFee, calculatedTotal);
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
        return "PricingValidationResult{" +
                "isValid=" + isValid +
                ", severity=" + severity +
                ", calculatedTotal=" + calculatedTotal +
                ", issueCount=" + issues.size() +
                '}';
    }
}