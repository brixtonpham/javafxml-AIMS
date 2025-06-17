package com.aims.core.application.dtos.validation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation result specifically for rush delivery with rush delivery-specific context
 */
public class RushDeliveryValidationResult {
    private boolean isValid;
    private ValidationSeverity severity;
    private List<ValidationIssue> issues;
    private List<String> recoverySuggestions;
    private LocalDateTime validationTimestamp;
    private boolean rushDeliveryAvailable;
    private boolean addressEligible;
    private boolean itemsEligible;
    private String eligibilityReason;
    private List<String> eligibleItems;
    private List<String> ineligibleItems;
    
    public RushDeliveryValidationResult() {
        this.issues = new ArrayList<>();
        this.recoverySuggestions = new ArrayList<>();
        this.validationTimestamp = LocalDateTime.now();
        this.severity = ValidationSeverity.INFO;
        this.isValid = true;
        this.rushDeliveryAvailable = false;
        this.addressEligible = false;
        this.itemsEligible = false;
        this.eligibleItems = new ArrayList<>();
        this.ineligibleItems = new ArrayList<>();
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
    
    public boolean isRushDeliveryAvailable() {
        return rushDeliveryAvailable;
    }
    
    public void setRushDeliveryAvailable(boolean rushDeliveryAvailable) {
        this.rushDeliveryAvailable = rushDeliveryAvailable;
    }
    
    public boolean isAddressEligible() {
        return addressEligible;
    }
    
    public void setAddressEligible(boolean addressEligible) {
        this.addressEligible = addressEligible;
    }
    
    public boolean isItemsEligible() {
        return itemsEligible;
    }
    
    public void setItemsEligible(boolean itemsEligible) {
        this.itemsEligible = itemsEligible;
    }
    
    public String getEligibilityReason() {
        return eligibilityReason;
    }
    
    public void setEligibilityReason(String eligibilityReason) {
        this.eligibilityReason = eligibilityReason;
    }
    
    public List<String> getEligibleItems() {
        return eligibleItems;
    }
    
    public void setEligibleItems(List<String> eligibleItems) {
        this.eligibleItems = eligibleItems != null ? eligibleItems : new ArrayList<>();
    }
    
    public List<String> getIneligibleItems() {
        return ineligibleItems;
    }
    
    public void setIneligibleItems(List<String> ineligibleItems) {
        this.ineligibleItems = ineligibleItems != null ? ineligibleItems : new ArrayList<>();
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
    
    public void addEligibleItem(String item) {
        if (item != null && !item.trim().isEmpty()) {
            this.eligibleItems.add(item);
        }
    }
    
    public void addIneligibleItem(String item) {
        if (item != null && !item.trim().isEmpty()) {
            this.ineligibleItems.add(item);
        }
    }
    
    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> 
            issue.getSeverity() == ValidationSeverity.ERROR || 
            issue.getSeverity() == ValidationSeverity.CRITICAL);
    }
    
    public boolean hasEligibleItems() {
        return !eligibleItems.isEmpty();
    }
    
    public boolean hasIneligibleItems() {
        return !ineligibleItems.isEmpty();
    }
    
    public int getEligibleItemCount() {
        return eligibleItems.size();
    }
    
    public int getIneligibleItemCount() {
        return ineligibleItems.size();
    }
    
    /**
     * Gets a summary of rush delivery eligibility
     */
    public String getEligibilitySummary() {
        if (rushDeliveryAvailable) {
            return "Rush delivery available - " + eligibleItems.size() + " eligible items";
        } else if (!addressEligible) {
            return "Rush delivery unavailable - address not eligible";
        } else if (!itemsEligible) {
            return "Rush delivery unavailable - no eligible items";
        } else {
            return "Rush delivery unavailable - " + (eligibilityReason != null ? eligibilityReason : "unknown reason");
        }
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
        return "RushDeliveryValidationResult{" +
                "isValid=" + isValid +
                ", severity=" + severity +
                ", rushDeliveryAvailable=" + rushDeliveryAvailable +
                ", addressEligible=" + addressEligible +
                ", itemsEligible=" + itemsEligible +
                ", eligibleItems=" + eligibleItems.size() +
                ", ineligibleItems=" + ineligibleItems.size() +
                ", issueCount=" + issues.size() +
                '}';
    }
}