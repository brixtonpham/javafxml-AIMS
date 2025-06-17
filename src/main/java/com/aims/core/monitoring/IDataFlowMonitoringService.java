package com.aims.core.monitoring;

import com.aims.core.monitoring.dto.DataTransferMetrics;
import com.aims.core.monitoring.dto.OrderCompletenessMetrics;
import com.aims.core.monitoring.dto.ValidationPerformanceMetrics;
import com.aims.core.monitoring.dto.DataConsistencyMetrics;
import com.aims.core.monitoring.dto.DataFlowHealthReport;
import com.aims.core.monitoring.dto.DataFlowAnomaly;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Flow Monitoring Service Interface
 * 
 * Provides comprehensive monitoring and alerting for cart-to-order data flow,
 * ensuring data consistency, performance tracking, and early detection of issues.
 */
public interface IDataFlowMonitoringService {
    
    /**
     * Monitors cart-to-order data transfer process
     * 
     * @param cartSessionId The cart session ID being converted
     * @param orderId The resulting order ID
     * @param metrics The data transfer metrics
     */
    void monitorCartToOrderTransfer(String cartSessionId, String orderId, DataTransferMetrics metrics);
    
    /**
     * Monitors order data completeness throughout customer journey
     * 
     * @param orderId The order ID being monitored
     * @param screen The current screen/stage in the journey
     * @param metrics The order completeness metrics
     */
    void monitorOrderDataCompleteness(String orderId, String screen, OrderCompletenessMetrics metrics);
    
    /**
     * Monitors validation service performance and effectiveness
     * 
     * @param validationType The type of validation being performed
     * @param metrics The validation performance metrics
     */
    void monitorValidationPerformance(String validationType, ValidationPerformanceMetrics metrics);
    
    /**
     * Monitors data consistency across different screens and transitions
     * 
     * @param orderId The order ID being monitored
     * @param fromScreen The source screen
     * @param toScreen The destination screen
     * @param metrics The data consistency metrics
     */
    void monitorDataConsistency(String orderId, String fromScreen, String toScreen, DataConsistencyMetrics metrics);
    
    /**
     * Generates comprehensive data flow health report
     * 
     * @param startDate The start date for the report period
     * @param endDate The end date for the report period
     * @return DataFlowHealthReport containing system health metrics
     */
    DataFlowHealthReport generateHealthReport(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Detects and returns data flow anomalies for a specific order
     * 
     * @param orderId The order ID to check for anomalies
     * @return List of detected anomalies
     */
    List<DataFlowAnomaly> detectAnomalies(String orderId);
    
    /**
     * Tracks real-time performance metrics
     * 
     * @param operationType The type of operation being tracked
     * @param executionTimeMs The execution time in milliseconds
     * @param success Whether the operation was successful
     */
    void trackPerformanceMetric(String operationType, long executionTimeMs, boolean success);
    
    /**
     * Records data validation errors for analysis
     * 
     * @param orderId The order ID where validation failed
     * @param validationType The type of validation that failed
     * @param errorMessage The error message
     * @param severity The severity level of the error
     */
    void recordValidationError(String orderId, String validationType, String errorMessage, String severity);
    
    /**
     * Gets real-time system health status
     * 
     * @return Current system health status
     */
    String getSystemHealthStatus();
    
    /**
     * Triggers alert for critical data flow issues
     * 
     * @param alertType The type of alert
     * @param orderId The affected order ID
     * @param message The alert message
     * @param severity The alert severity
     */
    void triggerAlert(String alertType, String orderId, String message, String severity);
}