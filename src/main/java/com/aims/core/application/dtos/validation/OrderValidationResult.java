package com.aims.core.application.dtos.validation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive validation result for order entities with detailed 
 * issue tracking, recovery suggestions, and context information
 */
public class OrderValidationResult {
    private boolean isValid;
    private ValidationSeverity severity;
    private List<ValidationIssue> issues;
    private List<String> recoverySuggestions;
    private Map<String, Object> validationContext;
    private LocalDateTime validationTimestamp;
    private String validationSummary;
    
    public OrderValidationResult() {
        this.issues = new ArrayList<>();
        this.recoverySuggestions = new ArrayList<>();
        this.validationContext = new HashMap<>();
        this.validationTimestamp = LocalDateTime.now();
        this.severity = ValidationSeverity.INFO;
        this.isValid = true;
    }
    
    public OrderValidationResult(boolean isValid) {
        this();
        this.isValid = isValid;
        if (!isValid) {
            this.severity = ValidationSeverity.ERROR;
        }
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
    
    public Map<String, Object> getValidationContext() {
        return validationContext;
    }
    
    public void setValidationContext(Map<String, Object> validationContext) {
        this.validationContext = validationContext != null ? validationContext : new HashMap<>();
    }
    
    public LocalDateTime getValidationTimestamp() {
        return validationTimestamp;
    }
    
    public void setValidationTimestamp(LocalDateTime validationTimestamp) {
        this.validationTimestamp = validationTimestamp;
    }
    
    public String getValidationSummary() {
        return validationSummary;
    }
    
    public void setValidationSummary(String validationSummary) {
        this.validationSummary = validationSummary;
    }
    
    // Utility methods for adding issues
    public void addIssue(ValidationIssue issue) {
        if (issue != null) {
            this.issues.add(issue);
            updateValidationState();
        }
    }
    
    public void addError(String field, String code, String message) {
        addIssue(new ValidationIssue(field, code, message, ValidationSeverity.ERROR));
    }
    
    public void addWarning(String field, String code, String message) {
        addIssue(new ValidationIssue(field, code, message, ValidationSeverity.WARNING));
    }
    
    public void addInfo(String field, String code, String message) {
        addIssue(new ValidationIssue(field, code, message, ValidationSeverity.INFO));
    }
    
    public void addCritical(String field, String code, String message) {
        addIssue(new ValidationIssue(field, code, message, ValidationSeverity.CRITICAL));
    }
    
    public void addRecoverySuggestion(String suggestion) {
        if (suggestion != null && !suggestion.trim().isEmpty()) {
            this.recoverySuggestions.add(suggestion);
        }
    }
    
    public void addContextInfo(String key, Object value) {
        this.validationContext.put(key, value);
    }
    
    // Query methods
    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> 
            issue.getSeverity() == ValidationSeverity.ERROR || 
            issue.getSeverity() == ValidationSeverity.CRITICAL);
    }
    
    public boolean hasWarnings() {
        return issues.stream().anyMatch(issue -> 
            issue.getSeverity() == ValidationSeverity.WARNING);
    }
    
    public boolean hasCriticalErrors() {
        return issues.stream().anyMatch(issue -> 
            issue.getSeverity() == ValidationSeverity.CRITICAL);
    }
    
    public List<ValidationIssue> getErrors() {
        return issues.stream()
            .filter(issue -> issue.getSeverity() == ValidationSeverity.ERROR || 
                           issue.getSeverity() == ValidationSeverity.CRITICAL)
            .collect(Collectors.toList());
    }
    
    public List<ValidationIssue> getWarnings() {
        return issues.stream()
            .filter(issue -> issue.getSeverity() == ValidationSeverity.WARNING)
            .collect(Collectors.toList());
    }
    
    public List<ValidationIssue> getCriticalErrors() {
        return issues.stream()
            .filter(issue -> issue.getSeverity() == ValidationSeverity.CRITICAL)
            .collect(Collectors.toList());
    }
    
    public String getErrorSummary() {
        return getErrors().stream()
            .map(ValidationIssue::getUserFriendlyMessage)
            .collect(Collectors.joining("; "));
    }
    
    public String getCriticalErrorSummary() {
        return getCriticalErrors().stream()
            .map(ValidationIssue::getUserFriendlyMessage)
            .collect(Collectors.joining("; "));
    }
    
    public String getWarningSummary() {
        return getWarnings().stream()
            .map(ValidationIssue::getUserFriendlyMessage)
            .collect(Collectors.joining("; "));
    }
    
    public int getIssueCount() {
        return issues.size();
    }
    
    public int getErrorCount() {
        return getErrors().size();
    }
    
    public int getWarningCount() {
        return getWarnings().size();
    }
    
    public int getCriticalErrorCount() {
        return getCriticalErrors().size();
    }
    
    /**
     * Updates the overall validation state based on current issues
     */
    private void updateValidationState() {
        if (hasCriticalErrors()) {
            this.isValid = false;
            this.severity = ValidationSeverity.CRITICAL;
        } else if (hasErrors()) {
            this.isValid = false;
            this.severity = ValidationSeverity.ERROR;
        } else if (hasWarnings()) {
            this.isValid = true; // Can proceed with warnings
            this.severity = ValidationSeverity.WARNING;
        } else {
            this.isValid = true;
            this.severity = ValidationSeverity.INFO;
        }
        
        generateValidationSummary();
    }
    
    /**
     * Generates a human-readable validation summary
     */
    private void generateValidationSummary() {
        if (issues.isEmpty()) {
            this.validationSummary = "Validation passed - no issues found";
            return;
        }
        
        StringBuilder summary = new StringBuilder();
        
        if (hasCriticalErrors()) {
            summary.append(getCriticalErrorCount()).append(" critical error(s)");
        }
        
        if (hasErrors()) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(getErrorCount()).append(" error(s)");
        }
        
        if (hasWarnings()) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(getWarningCount()).append(" warning(s)");
        }
        
        summary.append(" found");
        this.validationSummary = summary.toString();
    }
    
    /**
     * Merges another validation result into this one
     * 
     * @param other The other validation result to merge
     */
    public void merge(OrderValidationResult other) {
        if (other != null) {
            this.issues.addAll(other.getIssues());
            this.recoverySuggestions.addAll(other.getRecoverySuggestions());
            this.validationContext.putAll(other.getValidationContext());
            updateValidationState();
        }
    }
    
    @Override
    public String toString() {
        return "OrderValidationResult{" +
                "isValid=" + isValid +
                ", severity=" + severity +
                ", issueCount=" + issues.size() +
                ", summary='" + validationSummary + '\'' +
                '}';
    }
    
    /**
     * Builder pattern for creating OrderValidationResult instances
     */
    public static class Builder {
        private OrderValidationResult result = new OrderValidationResult();
        
        public Builder valid(boolean valid) {
            result.setValid(valid);
            return this;
        }
        
        public Builder severity(ValidationSeverity severity) {
            result.setSeverity(severity);
            return this;
        }
        
        public Builder addIssue(ValidationIssue issue) {
            result.addIssue(issue);
            return this;
        }
        
        public Builder addError(String field, String code, String message) {
            result.addError(field, code, message);
            return this;
        }
        
        public Builder addWarning(String field, String code, String message) {
            result.addWarning(field, code, message);
            return this;
        }
        
        public Builder addInfo(String field, String code, String message) {
            result.addInfo(field, code, message);
            return this;
        }
        
        public Builder addCritical(String field, String code, String message) {
            result.addCritical(field, code, message);
            return this;
        }
        
        public Builder addRecoverySuggestion(String suggestion) {
            result.addRecoverySuggestion(suggestion);
            return this;
        }
        
        public Builder addContextInfo(String key, Object value) {
            result.addContextInfo(key, value);
            return this;
        }
        
        public Builder validationSummary(String summary) {
            result.setValidationSummary(summary);
            return this;
        }
        
        public OrderValidationResult build() {
            result.updateValidationState();
            return result;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}