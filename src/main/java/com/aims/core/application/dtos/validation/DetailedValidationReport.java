package com.aims.core.application.dtos.validation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive detailed validation report that aggregates all validation results
 * and provides detailed analysis, recommendations, and recovery suggestions
 */
public class DetailedValidationReport {
    private String orderId;
    private LocalDateTime validationTimestamp;
    private boolean overallValid;
    private ValidationSeverity overallSeverity;
    private String validationSummary;
    
    // Section results
    private Map<String, ValidationSection> sections;
    private List<ValidationIssue> allIssues;
    private List<String> recommendations;
    private List<String> recoverySuggestions;
    
    // Statistics
    private int totalIssuesFound;
    private int criticalIssuesCount;
    private int errorIssuesCount;
    private int warningIssuesCount;
    private int infoIssuesCount;
    
    // Validation context
    private Map<String, Object> validationContext;
    
    public DetailedValidationReport() {
        this.sections = new HashMap<>();
        this.allIssues = new ArrayList<>();
        this.recommendations = new ArrayList<>();
        this.recoverySuggestions = new ArrayList<>();
        this.validationContext = new HashMap<>();
        this.validationTimestamp = LocalDateTime.now();
        this.overallValid = true;
        this.overallSeverity = ValidationSeverity.INFO;
    }
    
    public DetailedValidationReport(String orderId) {
        this();
        this.orderId = orderId;
    }
    
    // Getters and setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public LocalDateTime getValidationTimestamp() {
        return validationTimestamp;
    }
    
    public void setValidationTimestamp(LocalDateTime validationTimestamp) {
        this.validationTimestamp = validationTimestamp;
    }
    
    public boolean isOverallValid() {
        return overallValid;
    }
    
    public void setOverallValid(boolean overallValid) {
        this.overallValid = overallValid;
    }
    
    public ValidationSeverity getOverallSeverity() {
        return overallSeverity;
    }
    
    public void setOverallSeverity(ValidationSeverity overallSeverity) {
        this.overallSeverity = overallSeverity;
    }
    
    public String getValidationSummary() {
        return validationSummary;
    }
    
    public void setValidationSummary(String validationSummary) {
        this.validationSummary = validationSummary;
    }
    
    public Map<String, ValidationSection> getSections() {
        return sections;
    }
    
    public void setSections(Map<String, ValidationSection> sections) {
        this.sections = sections != null ? sections : new HashMap<>();
    }
    
    public List<ValidationIssue> getAllIssues() {
        return allIssues;
    }
    
    public void setAllIssues(List<ValidationIssue> allIssues) {
        this.allIssues = allIssues != null ? allIssues : new ArrayList<>();
        updateStatistics();
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
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
    
    // Statistics getters
    public int getTotalIssuesFound() {
        return totalIssuesFound;
    }
    
    public int getCriticalIssuesCount() {
        return criticalIssuesCount;
    }
    
    public int getErrorIssuesCount() {
        return errorIssuesCount;
    }
    
    public int getWarningIssuesCount() {
        return warningIssuesCount;
    }
    
    public int getInfoIssuesCount() {
        return infoIssuesCount;
    }
    
    // Section management
    public void addSection(String sectionName, OrderValidationResult result) {
        ValidationSection section = new ValidationSection(sectionName, result);
        sections.put(sectionName, section);
        
        // Add issues to overall list
        if (result != null && result.getIssues() != null) {
            allIssues.addAll(result.getIssues());
            recoverySuggestions.addAll(result.getRecoverySuggestions());
        }
        
        updateOverallState();
    }
    
    public void addSection(String sectionName, ValidationSection section) {
        sections.put(sectionName, section);
        updateOverallState();
    }
    
    public ValidationSection getSection(String sectionName) {
        return sections.get(sectionName);
    }
    
    public boolean hasSectionErrors(String sectionName) {
        ValidationSection section = sections.get(sectionName);
        return section != null && section.hasErrors();
    }
    
    // Issue management
    public void addIssue(ValidationIssue issue) {
        if (issue != null) {
            allIssues.add(issue);
            updateOverallState();
        }
    }
    
    public void addRecommendation(String recommendation) {
        if (recommendation != null && !recommendation.trim().isEmpty()) {
            recommendations.add(recommendation);
        }
    }
    
    public void addRecoverySuggestion(String suggestion) {
        if (suggestion != null && !suggestion.trim().isEmpty()) {
            recoverySuggestions.add(suggestion);
        }
    }
    
    public void addContextInfo(String key, Object value) {
        validationContext.put(key, value);
    }
    
    // Query methods
    public boolean hasErrors() {
        return errorIssuesCount > 0 || criticalIssuesCount > 0;
    }
    
    public boolean hasCriticalErrors() {
        return criticalIssuesCount > 0;
    }
    
    public boolean hasWarnings() {
        return warningIssuesCount > 0;
    }
    
    public List<ValidationIssue> getErrorIssues() {
        return allIssues.stream()
            .filter(issue -> issue.getSeverity() == ValidationSeverity.ERROR || 
                           issue.getSeverity() == ValidationSeverity.CRITICAL)
            .collect(Collectors.toList());
    }
    
    public List<ValidationIssue> getCriticalIssues() {
        return allIssues.stream()
            .filter(issue -> issue.getSeverity() == ValidationSeverity.CRITICAL)
            .collect(Collectors.toList());
    }
    
    public List<ValidationIssue> getWarningIssues() {
        return allIssues.stream()
            .filter(issue -> issue.getSeverity() == ValidationSeverity.WARNING)
            .collect(Collectors.toList());
    }
    
    // Report generation
    public void generateSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (totalIssuesFound == 0) {
            summary.append("Validation completed successfully - no issues found");
        } else {
            summary.append("Validation completed with ").append(totalIssuesFound).append(" issue(s): ");
            
            List<String> parts = new ArrayList<>();
            if (criticalIssuesCount > 0) {
                parts.add(criticalIssuesCount + " critical");
            }
            if (errorIssuesCount > 0) {
                parts.add(errorIssuesCount + " error(s)");
            }
            if (warningIssuesCount > 0) {
                parts.add(warningIssuesCount + " warning(s)");
            }
            if (infoIssuesCount > 0) {
                parts.add(infoIssuesCount + " info");
            }
            
            summary.append(String.join(", ", parts));
        }
        
        this.validationSummary = summary.toString();
    }
    
    public void generateRecommendations() {
        recommendations.clear();
        
        if (hasCriticalErrors()) {
            recommendations.add("Critical issues detected - contact technical support immediately");
        }
        
        if (hasErrors()) {
            recommendations.add("Errors must be fixed before proceeding with order processing");
        }
        
        if (hasWarnings()) {
            recommendations.add("Warnings should be reviewed - order can proceed but may have issues");
        }
        
        // Section-specific recommendations
        for (Map.Entry<String, ValidationSection> entry : sections.entrySet()) {
            String sectionName = entry.getKey();
            ValidationSection section = entry.getValue();
            
            if (section.hasErrors()) {
                recommendations.add("Fix " + sectionName.toLowerCase() + " issues before proceeding");
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Order validation passed - ready for processing");
        }
    }
    
    private void updateOverallState() {
        updateStatistics();
        
        // Determine overall validity and severity
        if (criticalIssuesCount > 0) {
            overallValid = false;
            overallSeverity = ValidationSeverity.CRITICAL;
        } else if (errorIssuesCount > 0) {
            overallValid = false;
            overallSeverity = ValidationSeverity.ERROR;
        } else if (warningIssuesCount > 0) {
            overallValid = true; // Can proceed with warnings
            overallSeverity = ValidationSeverity.WARNING;
        } else {
            overallValid = true;
            overallSeverity = ValidationSeverity.INFO;
        }
    }
    
    private void updateStatistics() {
        criticalIssuesCount = (int) allIssues.stream()
            .filter(issue -> issue.getSeverity() == ValidationSeverity.CRITICAL)
            .count();
        
        errorIssuesCount = (int) allIssues.stream()
            .filter(issue -> issue.getSeverity() == ValidationSeverity.ERROR)
            .count();
        
        warningIssuesCount = (int) allIssues.stream()
            .filter(issue -> issue.getSeverity() == ValidationSeverity.WARNING)
            .count();
        
        infoIssuesCount = (int) allIssues.stream()
            .filter(issue -> issue.getSeverity() == ValidationSeverity.INFO)
            .count();
        
        totalIssuesFound = allIssues.size();
    }
    
    @Override
    public String toString() {
        return "DetailedValidationReport{" +
                "orderId='" + orderId + '\'' +
                ", overallValid=" + overallValid +
                ", overallSeverity=" + overallSeverity +
                ", totalIssues=" + totalIssuesFound +
                ", sections=" + sections.keySet() +
                '}';
    }
    
    /**
     * Inner class representing a validation section
     */
    public static class ValidationSection {
        private String sectionName;
        private boolean valid;
        private ValidationSeverity severity;
        private List<ValidationIssue> issues;
        private String sectionSummary;
        
        public ValidationSection(String sectionName, OrderValidationResult result) {
            this.sectionName = sectionName;
            this.issues = new ArrayList<>();
            
            if (result != null) {
                this.valid = result.isValid();
                this.severity = result.getSeverity();
                this.issues = new ArrayList<>(result.getIssues());
                this.sectionSummary = result.getValidationSummary();
            } else {
                this.valid = true;
                this.severity = ValidationSeverity.INFO;
                this.sectionSummary = "No validation performed";
            }
        }
        
        // Getters
        public String getSectionName() {
            return sectionName;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public ValidationSeverity getSeverity() {
            return severity;
        }
        
        public List<ValidationIssue> getIssues() {
            return issues;
        }
        
        public String getSectionSummary() {
            return sectionSummary;
        }
        
        public boolean hasErrors() {
            return issues.stream().anyMatch(issue -> 
                issue.getSeverity() == ValidationSeverity.ERROR || 
                issue.getSeverity() == ValidationSeverity.CRITICAL);
        }
        
        public int getIssueCount() {
            return issues.size();
        }
        
        @Override
        public String toString() {
            return "ValidationSection{" +
                    "name='" + sectionName + '\'' +
                    ", valid=" + valid +
                    ", severity=" + severity +
                    ", issueCount=" + issues.size() +
                    '}';
        }
    }
}