package com.aims.core.monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Flow Health Report DTO
 * 
 * Comprehensive health report for cart-to-order data flow monitoring
 * providing system-wide metrics and health indicators.
 */
public class DataFlowHealthReport {
    private LocalDateTime reportGenerationTime;
    private LocalDateTime reportPeriodStart;
    private LocalDateTime reportPeriodEnd;
    private String overallHealthStatus;
    private float overallHealthScore;
    
    // Cart-to-Order Conversion Metrics
    private int totalConversions;
    private int successfulConversions;
    private int failedConversions;
    private float conversionSuccessRate;
    private long averageConversionTimeMs;
    
    // Data Completeness Metrics
    private float averageDataCompleteness;
    private int ordersWithCompleteData;
    private int ordersWithIncompleteData;
    private List<String> mostCommonMissingFields;
    
    // Performance Metrics
    private long averageValidationTimeMs;
    private int validationsPerformed;
    private int validationFailures;
    private float validationSuccessRate;
    
    // Consistency Metrics
    private float averageConsistencyScore;
    private int consistencyChecksPerformed;
    private int consistencyFailures;
    private List<String> mostCommonInconsistencies;
    
    // Error and Issue Metrics
    private int totalErrors;
    private int criticalErrors;
    private int warnings;
    private Map<String, Integer> errorsByType;
    private List<String> topErrorMessages;
    
    // System Performance
    private Map<String, Long> performanceByOperation;
    private Map<String, Float> throughputByHour;
    private boolean performanceThresholdsMet;
    
    // Recommendations and Alerts
    private List<String> recommendations;
    private List<String> activeAlerts;
    private List<String> resolvedIssues;
    
    public DataFlowHealthReport() {
        this.reportGenerationTime = LocalDateTime.now();
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private DataFlowHealthReport report = new DataFlowHealthReport();
        
        public Builder reportPeriodStart(LocalDateTime reportPeriodStart) {
            report.reportPeriodStart = reportPeriodStart;
            return this;
        }
        
        public Builder reportPeriodEnd(LocalDateTime reportPeriodEnd) {
            report.reportPeriodEnd = reportPeriodEnd;
            return this;
        }
        
        public Builder totalConversions(int totalConversions) {
            report.totalConversions = totalConversions;
            return this;
        }
        
        public Builder successfulConversions(int successfulConversions) {
            report.successfulConversions = successfulConversions;
            return this;
        }
        
        public Builder failedConversions(int failedConversions) {
            report.failedConversions = failedConversions;
            return this;
        }
        
        public Builder averageConversionTimeMs(long averageConversionTimeMs) {
            report.averageConversionTimeMs = averageConversionTimeMs;
            return this;
        }
        
        public Builder averageDataCompleteness(float averageDataCompleteness) {
            report.averageDataCompleteness = averageDataCompleteness;
            return this;
        }
        
        public Builder ordersWithCompleteData(int ordersWithCompleteData) {
            report.ordersWithCompleteData = ordersWithCompleteData;
            return this;
        }
        
        public Builder ordersWithIncompleteData(int ordersWithIncompleteData) {
            report.ordersWithIncompleteData = ordersWithIncompleteData;
            return this;
        }
        
        public Builder mostCommonMissingFields(List<String> mostCommonMissingFields) {
            report.mostCommonMissingFields = mostCommonMissingFields;
            return this;
        }
        
        public Builder averageValidationTimeMs(long averageValidationTimeMs) {
            report.averageValidationTimeMs = averageValidationTimeMs;
            return this;
        }
        
        public Builder validationsPerformed(int validationsPerformed) {
            report.validationsPerformed = validationsPerformed;
            return this;
        }
        
        public Builder validationFailures(int validationFailures) {
            report.validationFailures = validationFailures;
            return this;
        }
        
        public Builder averageConsistencyScore(float averageConsistencyScore) {
            report.averageConsistencyScore = averageConsistencyScore;
            return this;
        }
        
        public Builder consistencyChecksPerformed(int consistencyChecksPerformed) {
            report.consistencyChecksPerformed = consistencyChecksPerformed;
            return this;
        }
        
        public Builder consistencyFailures(int consistencyFailures) {
            report.consistencyFailures = consistencyFailures;
            return this;
        }
        
        public Builder mostCommonInconsistencies(List<String> mostCommonInconsistencies) {
            report.mostCommonInconsistencies = mostCommonInconsistencies;
            return this;
        }
        
        public Builder totalErrors(int totalErrors) {
            report.totalErrors = totalErrors;
            return this;
        }
        
        public Builder criticalErrors(int criticalErrors) {
            report.criticalErrors = criticalErrors;
            return this;
        }
        
        public Builder warnings(int warnings) {
            report.warnings = warnings;
            return this;
        }
        
        public Builder errorsByType(Map<String, Integer> errorsByType) {
            report.errorsByType = errorsByType;
            return this;
        }
        
        public Builder topErrorMessages(List<String> topErrorMessages) {
            report.topErrorMessages = topErrorMessages;
            return this;
        }
        
        public Builder performanceByOperation(Map<String, Long> performanceByOperation) {
            report.performanceByOperation = performanceByOperation;
            return this;
        }
        
        public Builder throughputByHour(Map<String, Float> throughputByHour) {
            report.throughputByHour = throughputByHour;
            return this;
        }
        
        public Builder performanceThresholdsMet(boolean performanceThresholdsMet) {
            report.performanceThresholdsMet = performanceThresholdsMet;
            return this;
        }
        
        public Builder recommendations(List<String> recommendations) {
            report.recommendations = recommendations;
            return this;
        }
        
        public Builder activeAlerts(List<String> activeAlerts) {
            report.activeAlerts = activeAlerts;
            return this;
        }
        
        public Builder resolvedIssues(List<String> resolvedIssues) {
            report.resolvedIssues = resolvedIssues;
            return this;
        }
        
        public DataFlowHealthReport build() {
            // Calculate derived metrics
            calculateDerivedMetrics();
            calculateOverallHealth();
            return report;
        }
        
        private void calculateDerivedMetrics() {
            // Calculate conversion success rate
            if (report.totalConversions > 0) {
                report.conversionSuccessRate = ((float) report.successfulConversions / report.totalConversions) * 100.0f;
            }
            
            // Calculate validation success rate
            if (report.validationsPerformed > 0) {
                report.validationSuccessRate = ((float) (report.validationsPerformed - report.validationFailures) / report.validationsPerformed) * 100.0f;
            }
        }
        
        private void calculateOverallHealth() {
            float healthScore = 0.0f;
            int factors = 0;
            
            // Factor 1: Conversion success rate (weight: 30%)
            if (report.conversionSuccessRate > 0) {
                healthScore += report.conversionSuccessRate * 0.3f;
                factors++;
            }
            
            // Factor 2: Data completeness (weight: 25%)
            if (report.averageDataCompleteness > 0) {
                healthScore += report.averageDataCompleteness * 0.25f;
                factors++;
            }
            
            // Factor 3: Validation success rate (weight: 20%)
            if (report.validationSuccessRate > 0) {
                healthScore += report.validationSuccessRate * 0.2f;
                factors++;
            }
            
            // Factor 4: Consistency score (weight: 15%)
            if (report.averageConsistencyScore > 0) {
                healthScore += report.averageConsistencyScore * 0.15f;
                factors++;
            }
            
            // Factor 5: Performance thresholds (weight: 10%)
            if (report.performanceThresholdsMet) {
                healthScore += 100.0f * 0.1f;
            }
            factors++;
            
            report.overallHealthScore = factors > 0 ? healthScore : 0.0f;
            
            // Determine overall health status
            if (report.overallHealthScore >= 90.0f) {
                report.overallHealthStatus = "EXCELLENT";
            } else if (report.overallHealthScore >= 80.0f) {
                report.overallHealthStatus = "GOOD";
            } else if (report.overallHealthScore >= 70.0f) {
                report.overallHealthStatus = "FAIR";
            } else if (report.overallHealthScore >= 60.0f) {
                report.overallHealthStatus = "POOR";
            } else {
                report.overallHealthStatus = "CRITICAL";
            }
        }
    }
    
    // Getters (setters omitted for brevity, but would be included)
    public LocalDateTime getReportGenerationTime() { return reportGenerationTime; }
    public LocalDateTime getReportPeriodStart() { return reportPeriodStart; }
    public LocalDateTime getReportPeriodEnd() { return reportPeriodEnd; }
    public String getOverallHealthStatus() { return overallHealthStatus; }
    public float getOverallHealthScore() { return overallHealthScore; }
    public int getTotalConversions() { return totalConversions; }
    public int getSuccessfulConversions() { return successfulConversions; }
    public int getFailedConversions() { return failedConversions; }
    public float getConversionSuccessRate() { return conversionSuccessRate; }
    public long getAverageConversionTimeMs() { return averageConversionTimeMs; }
    public float getAverageDataCompleteness() { return averageDataCompleteness; }
    public int getOrdersWithCompleteData() { return ordersWithCompleteData; }
    public int getOrdersWithIncompleteData() { return ordersWithIncompleteData; }
    public List<String> getMostCommonMissingFields() { return mostCommonMissingFields; }
    public long getAverageValidationTimeMs() { return averageValidationTimeMs; }
    public int getValidationsPerformed() { return validationsPerformed; }
    public int getValidationFailures() { return validationFailures; }
    public float getValidationSuccessRate() { return validationSuccessRate; }
    public float getAverageConsistencyScore() { return averageConsistencyScore; }
    public int getConsistencyChecksPerformed() { return consistencyChecksPerformed; }
    public int getConsistencyFailures() { return consistencyFailures; }
    public List<String> getMostCommonInconsistencies() { return mostCommonInconsistencies; }
    public int getTotalErrors() { return totalErrors; }
    public int getCriticalErrors() { return criticalErrors; }
    public int getWarnings() { return warnings; }
    public Map<String, Integer> getErrorsByType() { return errorsByType; }
    public List<String> getTopErrorMessages() { return topErrorMessages; }
    public Map<String, Long> getPerformanceByOperation() { return performanceByOperation; }
    public Map<String, Float> getThroughputByHour() { return throughputByHour; }
    public boolean isPerformanceThresholdsMet() { return performanceThresholdsMet; }
    public List<String> getRecommendations() { return recommendations; }
    public List<String> getActiveAlerts() { return activeAlerts; }
    public List<String> getResolvedIssues() { return resolvedIssues; }
    
    // Utility methods
    public boolean requiresImmediateAttention() {
        return "CRITICAL".equals(overallHealthStatus) || criticalErrors > 0;
    }
    
    public boolean hasActiveIssues() {
        return activeAlerts != null && !activeAlerts.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("DataFlowHealthReport{status='%s', score=%.1f%%, " +
                           "conversions=%d/%d (%.1f%%), errors=%d, criticalErrors=%d}", 
                           overallHealthStatus, overallHealthScore, 
                           successfulConversions, totalConversions, conversionSuccessRate,
                           totalErrors, criticalErrors);
    }
}