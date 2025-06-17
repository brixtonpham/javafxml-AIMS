package com.aims.core.application.dtos.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single validation issue with detailed information
 * for user-friendly error reporting and recovery suggestions
 */
public class ValidationIssue {
    private String field;
    private String code;
    private String message;
    private ValidationSeverity severity;
    private String userFriendlyMessage;
    private List<String> possibleFixes;
    private Object actualValue;
    private Object expectedValue;
    
    public ValidationIssue() {
        this.possibleFixes = new ArrayList<>();
    }
    
    public ValidationIssue(String field, String code, String message, ValidationSeverity severity) {
        this();
        this.field = field;
        this.code = code;
        this.message = message;
        this.severity = severity;
        this.userFriendlyMessage = message; // Default to technical message
    }
    
    public ValidationIssue(String field, String code, String message, ValidationSeverity severity, 
                          String userFriendlyMessage) {
        this(field, code, message, severity);
        this.userFriendlyMessage = userFriendlyMessage;
    }
    
    // Getters and setters
    public String getField() {
        return field;
    }
    
    public void setField(String field) {
        this.field = field;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public ValidationSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(ValidationSeverity severity) {
        this.severity = severity;
    }
    
    public String getUserFriendlyMessage() {
        return userFriendlyMessage != null ? userFriendlyMessage : message;
    }
    
    public void setUserFriendlyMessage(String userFriendlyMessage) {
        this.userFriendlyMessage = userFriendlyMessage;
    }
    
    public List<String> getPossibleFixes() {
        return possibleFixes;
    }
    
    public void setPossibleFixes(List<String> possibleFixes) {
        this.possibleFixes = possibleFixes != null ? possibleFixes : new ArrayList<>();
    }
    
    public void addPossibleFix(String fix) {
        if (fix != null && !fix.trim().isEmpty()) {
            this.possibleFixes.add(fix);
        }
    }
    
    public Object getActualValue() {
        return actualValue;
    }
    
    public void setActualValue(Object actualValue) {
        this.actualValue = actualValue;
    }
    
    public Object getExpectedValue() {
        return expectedValue;
    }
    
    public void setExpectedValue(Object expectedValue) {
        this.expectedValue = expectedValue;
    }
    
    /**
     * Checks if this issue blocks processing
     * 
     * @return true if severity is ERROR or CRITICAL
     */
    public boolean isBlocking() {
        return severity != null && severity.isBlocking();
    }
    
    /**
     * Checks if this issue is critical
     * 
     * @return true if severity is CRITICAL
     */
    public boolean isCritical() {
        return severity != null && severity.isCritical();
    }
    
    /**
     * Gets a summary of the issue for logging or display
     * 
     * @return String summary of the issue
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        if (severity != null) {
            sb.append("[").append(severity.name()).append("] ");
        }
        if (field != null) {
            sb.append(field).append(": ");
        }
        sb.append(getUserFriendlyMessage());
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationIssue that = (ValidationIssue) o;
        return Objects.equals(field, that.field) && 
               Objects.equals(code, that.code) && 
               Objects.equals(severity, that.severity);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(field, code, severity);
    }
    
    @Override
    public String toString() {
        return "ValidationIssue{" +
                "field='" + field + '\'' +
                ", code='" + code + '\'' +
                ", severity=" + severity +
                ", message='" + getUserFriendlyMessage() + '\'' +
                '}';
    }
    
    /**
     * Builder pattern for creating ValidationIssue instances
     */
    public static class Builder {
        private ValidationIssue issue = new ValidationIssue();
        
        public Builder field(String field) {
            issue.setField(field);
            return this;
        }
        
        public Builder code(String code) {
            issue.setCode(code);
            return this;
        }
        
        public Builder message(String message) {
            issue.setMessage(message);
            return this;
        }
        
        public Builder severity(ValidationSeverity severity) {
            issue.setSeverity(severity);
            return this;
        }
        
        public Builder userFriendlyMessage(String userFriendlyMessage) {
            issue.setUserFriendlyMessage(userFriendlyMessage);
            return this;
        }
        
        public Builder possibleFix(String fix) {
            issue.addPossibleFix(fix);
            return this;
        }
        
        public Builder actualValue(Object actualValue) {
            issue.setActualValue(actualValue);
            return this;
        }
        
        public Builder expectedValue(Object expectedValue) {
            issue.setExpectedValue(expectedValue);
            return this;
        }
        
        public ValidationIssue build() {
            return issue;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}