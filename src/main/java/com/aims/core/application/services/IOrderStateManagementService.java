package com.aims.core.application.services;

import com.aims.core.entities.OrderEntity;
import com.aims.core.enums.OrderStatus;
import com.aims.core.shared.exceptions.InventoryException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing order state transitions and product manager approval workflows.
 * Provides comprehensive order state management with business rule validation,
 * stock integration, and audit trail capabilities.
 */
public interface IOrderStateManagementService {
    
    /**
     * Initiates the product manager approval workflow for an order.
     * Transitions order from any valid state to PENDING_PROCESSING.
     * 
     * @param orderId Order to submit for approval
     * @param submittedBy User ID of the person submitting for approval
     * @return StateTransitionResult with transition outcome
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Order not found
     * @throws ValidationException Invalid state transition or business rules violated
     */
    StateTransitionResult submitForApproval(String orderId, String submittedBy) 
            throws SQLException, ResourceNotFoundException, ValidationException;
    
    /**
     * Approves an order with automatic stock validation and reservation.
     * Transitions order from PENDING_PROCESSING to APPROVED.
     * Integrates with IStockValidationService for inventory checks.
     * 
     * @param orderId Order to approve
     * @param productManagerId Product manager performing the approval
     * @param approvalNotes Optional approval notes
     * @return OrderApprovalResult with approval outcome and stock validation
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Order not found
     * @throws ValidationException Invalid state transition or business rules violated
     * @throws InventoryException Insufficient stock for approval
     */
    OrderApprovalResult approveOrder(String orderId, String productManagerId, String approvalNotes) 
            throws SQLException, ResourceNotFoundException, ValidationException, InventoryException;
    
    /**
     * Rejects an order with specified reason.
     * Transitions order from PENDING_PROCESSING to REJECTED.
     * 
     * @param orderId Order to reject
     * @param productManagerId Product manager performing the rejection
     * @param rejectionReason Reason for rejection
     * @param rejectionNotes Optional detailed rejection notes
     * @return StateTransitionResult with rejection outcome
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Order not found
     * @throws ValidationException Invalid state transition
     */
    StateTransitionResult rejectOrder(String orderId, String productManagerId, 
                                    String rejectionReason, String rejectionNotes) 
            throws SQLException, ResourceNotFoundException, ValidationException;
    
    /**
     * Validates if a state transition is allowed based on business rules.
     * 
     * @param orderId Order to validate transition for
     * @param fromStatus Current order status
     * @param toStatus Target order status
     * @param performedBy User performing the transition
     * @return StateTransitionValidationResult with validation outcome
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Order not found
     */
    StateTransitionValidationResult validateStateTransition(String orderId, OrderStatus fromStatus, 
                                                           OrderStatus toStatus, String performedBy) 
            throws SQLException, ResourceNotFoundException;
    
    /**
     * Performs a general state transition with validation and audit logging.
     * 
     * @param orderId Order to transition
     * @param toStatus Target status
     * @param performedBy User performing the transition
     * @param transitionReason Reason for the transition
     * @param transitionNotes Optional detailed notes
     * @return StateTransitionResult with transition outcome
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Order not found
     * @throws ValidationException Invalid state transition or business rules violated
     */
    StateTransitionResult transitionOrderState(String orderId, OrderStatus toStatus, 
                                              String performedBy, String transitionReason, 
                                              String transitionNotes) 
            throws SQLException, ResourceNotFoundException, ValidationException;
    
    /**
     * Gets the complete state transition history for an order.
     * 
     * @param orderId Order to get history for
     * @return List of StateTransitionRecord in chronological order
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Order not found
     */
    List<StateTransitionRecord> getOrderStateHistory(String orderId) 
            throws SQLException, ResourceNotFoundException;
    
    /**
     * Gets all orders currently pending product manager approval.
     * 
     * @param productManagerId Optional filter by specific product manager
     * @return List of orders in PENDING_PROCESSING status
     * @throws SQLException Database error
     */
    List<OrderEntity> getPendingApprovalOrders(String productManagerId) 
            throws SQLException;
    
    /**
     * Gets valid next states for an order based on current status and business rules.
     * 
     * @param orderId Order to get valid transitions for
     * @param performedBy User context for transition validation
     * @return Map of valid target states to transition reasons
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Order not found
     */
    Map<OrderStatus, String> getValidNextStates(String orderId, String performedBy) 
            throws SQLException, ResourceNotFoundException;
    
    /**
     * Validates order stock before state transitions that require inventory checks.
     * Integrates with IStockValidationService for comprehensive validation.
     * 
     * @param orderId Order to validate stock for
     * @return OrderStockValidationResult with detailed stock validation
     * @throws SQLException Database error
     * @throws ResourceNotFoundException Order not found
     */
    OrderStockValidationResult validateOrderStock(String orderId) 
            throws SQLException, ResourceNotFoundException;
    
    /**
     * Gets comprehensive order state management statistics.
     * Used for monitoring and reporting purposes.
     * 
     * @param fromDate Start date for statistics
     * @param toDate End date for statistics
     * @return OrderStateStatistics with detailed metrics
     * @throws SQLException Database error
     */
    OrderStateStatistics getOrderStateStatistics(LocalDateTime fromDate, LocalDateTime toDate) 
            throws SQLException;
    
    /**
     * Represents the result of a state transition operation.
     */
    public static class StateTransitionResult {
        private final boolean successful;
        private final String orderId;
        private final OrderStatus previousStatus;
        private final OrderStatus newStatus;
        private final String performedBy;
        private final LocalDateTime transitionTimestamp;
        private final String transitionReason;
        private final String transitionNotes;
        private final String transitionId;
        private final List<String> validationWarnings;
        private final Map<String, String> additionalData;
        
        public StateTransitionResult(boolean successful, String orderId, OrderStatus previousStatus,
                                   OrderStatus newStatus, String performedBy, LocalDateTime transitionTimestamp,
                                   String transitionReason, String transitionNotes, String transitionId,
                                   List<String> validationWarnings, Map<String, String> additionalData) {
            this.successful = successful;
            this.orderId = orderId;
            this.previousStatus = previousStatus;
            this.newStatus = newStatus;
            this.performedBy = performedBy;
            this.transitionTimestamp = transitionTimestamp;
            this.transitionReason = transitionReason;
            this.transitionNotes = transitionNotes;
            this.transitionId = transitionId;
            this.validationWarnings = validationWarnings;
            this.additionalData = additionalData;
        }
        
        // Getters
        public boolean isSuccessful() { return successful; }
        public String getOrderId() { return orderId; }
        public OrderStatus getPreviousStatus() { return previousStatus; }
        public OrderStatus getNewStatus() { return newStatus; }
        public String getPerformedBy() { return performedBy; }
        public LocalDateTime getTransitionTimestamp() { return transitionTimestamp; }
        public String getTransitionReason() { return transitionReason; }
        public String getTransitionNotes() { return transitionNotes; }
        public String getTransitionId() { return transitionId; }
        public List<String> getValidationWarnings() { return validationWarnings; }
        public Map<String, String> getAdditionalData() { return additionalData; }
    }
    
    /**
     * Represents the result of an order approval operation with stock validation.
     */
    public static class OrderApprovalResult {
        private final StateTransitionResult transitionResult;
        private final OrderStockValidationResult stockValidationResult;
        private final boolean stockReserved;
        private final String reservationId;
        private final List<String> approvalWarnings;
        private final Map<String, Object> approvalMetadata;
        
        public OrderApprovalResult(StateTransitionResult transitionResult, 
                                 OrderStockValidationResult stockValidationResult,
                                 boolean stockReserved, String reservationId,
                                 List<String> approvalWarnings, Map<String, Object> approvalMetadata) {
            this.transitionResult = transitionResult;
            this.stockValidationResult = stockValidationResult;
            this.stockReserved = stockReserved;
            this.reservationId = reservationId;
            this.approvalWarnings = approvalWarnings;
            this.approvalMetadata = approvalMetadata;
        }
        
        // Getters
        public StateTransitionResult getTransitionResult() { return transitionResult; }
        public OrderStockValidationResult getStockValidationResult() { return stockValidationResult; }
        public boolean isStockReserved() { return stockReserved; }
        public String getReservationId() { return reservationId; }
        public List<String> getApprovalWarnings() { return approvalWarnings; }
        public Map<String, Object> getApprovalMetadata() { return approvalMetadata; }
        public boolean isSuccessful() { return transitionResult.isSuccessful() && stockValidationResult.isValid(); }
    }
    
    /**
     * Represents the result of state transition validation.
     */
    public static class StateTransitionValidationResult {
        private final boolean valid;
        private final String orderId;
        private final OrderStatus fromStatus;
        private final OrderStatus toStatus;
        private final String validationMessage;
        private final List<String> businessRuleViolations;
        private final List<String> warnings;
        private final Map<String, String> validationContext;
        
        public StateTransitionValidationResult(boolean valid, String orderId, OrderStatus fromStatus,
                                             OrderStatus toStatus, String validationMessage,
                                             List<String> businessRuleViolations, List<String> warnings,
                                             Map<String, String> validationContext) {
            this.valid = valid;
            this.orderId = orderId;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.validationMessage = validationMessage;
            this.businessRuleViolations = businessRuleViolations;
            this.warnings = warnings;
            this.validationContext = validationContext;
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public String getOrderId() { return orderId; }
        public OrderStatus getFromStatus() { return fromStatus; }
        public OrderStatus getToStatus() { return toStatus; }
        public String getValidationMessage() { return validationMessage; }
        public List<String> getBusinessRuleViolations() { return businessRuleViolations; }
        public List<String> getWarnings() { return warnings; }
        public Map<String, String> getValidationContext() { return validationContext; }
    }
    
    /**
     * Represents a state transition record for audit trail.
     */
    public static class StateTransitionRecord {
        private final String transitionId;
        private final String orderId;
        private final OrderStatus fromStatus;
        private final OrderStatus toStatus;
        private final String performedBy;
        private final LocalDateTime transitionTimestamp;
        private final String transitionReason;
        private final String transitionNotes;
        private final boolean successful;
        private final Map<String, String> metadata;
        
        public StateTransitionRecord(String transitionId, String orderId, OrderStatus fromStatus,
                                   OrderStatus toStatus, String performedBy, LocalDateTime transitionTimestamp,
                                   String transitionReason, String transitionNotes, boolean successful,
                                   Map<String, String> metadata) {
            this.transitionId = transitionId;
            this.orderId = orderId;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.performedBy = performedBy;
            this.transitionTimestamp = transitionTimestamp;
            this.transitionReason = transitionReason;
            this.transitionNotes = transitionNotes;
            this.successful = successful;
            this.metadata = metadata;
        }
        
        // Getters
        public String getTransitionId() { return transitionId; }
        public String getOrderId() { return orderId; }
        public OrderStatus getFromStatus() { return fromStatus; }
        public OrderStatus getToStatus() { return toStatus; }
        public String getPerformedBy() { return performedBy; }
        public LocalDateTime getTransitionTimestamp() { return transitionTimestamp; }
        public String getTransitionReason() { return transitionReason; }
        public String getTransitionNotes() { return transitionNotes; }
        public boolean isSuccessful() { return successful; }
        public Map<String, String> getMetadata() { return metadata; }
    }
    
    /**
     * Represents order stock validation result with integration from IStockValidationService.
     */
    public static class OrderStockValidationResult {
        private final boolean valid;
        private final String orderId;
        private final IStockValidationService.BulkStockValidationResult bulkValidationResult;
        private final List<String> stockIssues;
        private final List<String> stockWarnings;
        private final boolean canProceedWithPartialStock;
        private final Map<String, Integer> productStockShortfalls;
        
        public OrderStockValidationResult(boolean valid, String orderId,
                                        IStockValidationService.BulkStockValidationResult bulkValidationResult,
                                        List<String> stockIssues, List<String> stockWarnings,
                                        boolean canProceedWithPartialStock,
                                        Map<String, Integer> productStockShortfalls) {
            this.valid = valid;
            this.orderId = orderId;
            this.bulkValidationResult = bulkValidationResult;
            this.stockIssues = stockIssues;
            this.stockWarnings = stockWarnings;
            this.canProceedWithPartialStock = canProceedWithPartialStock;
            this.productStockShortfalls = productStockShortfalls;
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public String getOrderId() { return orderId; }
        public IStockValidationService.BulkStockValidationResult getBulkValidationResult() { return bulkValidationResult; }
        public List<String> getStockIssues() { return stockIssues; }
        public List<String> getStockWarnings() { return stockWarnings; }
        public boolean canProceedWithPartialStock() { return canProceedWithPartialStock; }
        public Map<String, Integer> getProductStockShortfalls() { return productStockShortfalls; }
    }
    
    /**
     * Represents comprehensive order state management statistics.
     */
    public static class OrderStateStatistics {
        private final LocalDateTime fromDate;
        private final LocalDateTime toDate;
        private final Map<OrderStatus, Integer> statusCounts;
        private final Map<String, Integer> transitionCounts;
        private final int totalTransitions;
        private final int totalApprovals;
        private final int totalRejections;
        private final double averageApprovalTime;
        private final Map<String, Integer> productManagerActivity;
        private final List<String> topRejectionReasons;
        
        public OrderStateStatistics(LocalDateTime fromDate, LocalDateTime toDate,
                                  Map<OrderStatus, Integer> statusCounts, Map<String, Integer> transitionCounts,
                                  int totalTransitions, int totalApprovals, int totalRejections,
                                  double averageApprovalTime, Map<String, Integer> productManagerActivity,
                                  List<String> topRejectionReasons) {
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.statusCounts = statusCounts;
            this.transitionCounts = transitionCounts;
            this.totalTransitions = totalTransitions;
            this.totalApprovals = totalApprovals;
            this.totalRejections = totalRejections;
            this.averageApprovalTime = averageApprovalTime;
            this.productManagerActivity = productManagerActivity;
            this.topRejectionReasons = topRejectionReasons;
        }
        
        // Getters
        public LocalDateTime getFromDate() { return fromDate; }
        public LocalDateTime getToDate() { return toDate; }
        public Map<OrderStatus, Integer> getStatusCounts() { return statusCounts; }
        public Map<String, Integer> getTransitionCounts() { return transitionCounts; }
        public int getTotalTransitions() { return totalTransitions; }
        public int getTotalApprovals() { return totalApprovals; }
        public int getTotalRejections() { return totalRejections; }
        public double getAverageApprovalTime() { return averageApprovalTime; }
        public Map<String, Integer> getProductManagerActivity() { return productManagerActivity; }
        public List<String> getTopRejectionReasons() { return topRejectionReasons; }
    }
}