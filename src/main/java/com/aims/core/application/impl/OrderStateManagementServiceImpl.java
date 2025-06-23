package com.aims.core.application.impl;

import org.springframework.stereotype.Service;
import com.aims.core.application.services.IOrderStateManagementService;
import com.aims.core.application.services.IStockValidationService;
import com.aims.core.application.services.IStockReservationService;
import com.aims.core.application.services.IOrderService;
import com.aims.core.application.services.INotificationService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.enums.OrderStatus;
import com.aims.core.infrastructure.database.dao.IOrderEntityDAO;
import com.aims.core.infrastructure.database.dao.IOrderItemDAO;
import com.aims.core.shared.exceptions.InventoryException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Implementation of IOrderStateManagementService providing comprehensive order state management
 * with product manager approval workflows, stock validation integration, and audit trail.
 */
@Service
public class OrderStateManagementServiceImpl implements IOrderStateManagementService {
    
    private static final Logger logger = Logger.getLogger(OrderStateManagementServiceImpl.class.getName());
    
    // Dependencies
    private final IOrderEntityDAO orderEntityDAO;
    private final IOrderItemDAO orderItemDAO;
    private final IStockValidationService stockValidationService;
    private final IStockReservationService stockReservationService;
    private final INotificationService notificationService;
    
    // State transition history storage (in production, this would be database-backed)
    private final Map<String, List<StateTransitionRecord>> stateTransitionHistory = new ConcurrentHashMap<>();
    
    // Valid state transitions based on business rules
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new HashMap<>();
    
    static {
        // Initialize valid state transitions
        VALID_TRANSITIONS.put(OrderStatus.PENDING_DELIVERY_INFO, 
            Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.PENDING_PAYMENT, 
            Set.of(OrderStatus.PENDING_PROCESSING, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.PAYMENT_FAILED, 
            Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.PENDING_PROCESSING, 
            Set.of(OrderStatus.APPROVED, OrderStatus.REJECTED));
        VALID_TRANSITIONS.put(OrderStatus.APPROVED, 
            Set.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.REJECTED, 
            Set.of(OrderStatus.PENDING_PROCESSING, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.SHIPPING, 
            Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
        VALID_TRANSITIONS.put(OrderStatus.DELIVERED, 
            Set.of(OrderStatus.REFUNDED));
        VALID_TRANSITIONS.put(OrderStatus.CANCELLED, 
            Set.of(OrderStatus.REFUNDED));
        VALID_TRANSITIONS.put(OrderStatus.REFUNDED, 
            Set.of()); // Terminal state
        VALID_TRANSITIONS.put(OrderStatus.ERROR_STOCK_UPDATE_FAILED, 
            Set.of(OrderStatus.PENDING_PROCESSING, OrderStatus.CANCELLED));
    }
    
    public OrderStateManagementServiceImpl(IOrderEntityDAO orderEntityDAO,
                                         IOrderItemDAO orderItemDAO,
                                         IStockValidationService stockValidationService,
                                         IStockReservationService stockReservationService,
                                         INotificationService notificationService) {
        this.orderEntityDAO = orderEntityDAO;
        this.orderItemDAO = orderItemDAO;
        this.stockValidationService = stockValidationService;
        this.stockReservationService = stockReservationService;
        this.notificationService = notificationService;
        
        logger.info("OrderStateManagementService initialized with stock validation integration");
    }
    
    @Override
    public StateTransitionResult submitForApproval(String orderId, String submittedBy) 
            throws SQLException, ResourceNotFoundException, ValidationException {
        
        logger.info("Submitting order " + orderId + " for approval by " + submittedBy);
        
        OrderEntity order = orderEntityDAO.getById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        
        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus targetStatus = OrderStatus.PENDING_PROCESSING;
        
        // Validate transition
        StateTransitionValidationResult validation = validateStateTransition(
            orderId, currentStatus, targetStatus, submittedBy);
        
        if (!validation.isValid()) {
            throw new ValidationException("Invalid state transition: " + validation.getValidationMessage());
        }
        
        // Perform the transition
        return transitionOrderState(orderId, targetStatus, submittedBy, 
            "SUBMITTED_FOR_APPROVAL", "Order submitted for product manager approval");
    }
    
    @Override
    public OrderApprovalResult approveOrder(String orderId, String productManagerId, String approvalNotes) 
            throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        
        logger.info("Product manager " + productManagerId + " approving order " + orderId);
        
        OrderEntity order = orderEntityDAO.getById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        
        if (order.getOrderStatus() != OrderStatus.PENDING_PROCESSING) {
            throw new ValidationException("Order must be in PENDING_PROCESSING status for approval");
        }
        
        // Validate stock before approval
        OrderStockValidationResult stockValidation = validateOrderStock(orderId);
        List<String> approvalWarnings = new ArrayList<>();
        
        if (!stockValidation.isValid()) {
            throw new InventoryException("Cannot approve order due to insufficient stock: " + 
                String.join(", ", stockValidation.getStockIssues()));
        }
        
        // Reserve stock for the order
        String reservationId = "APPROVAL_" + orderId + "_" + System.currentTimeMillis();
        boolean stockReserved = false;
        
        try {
            // Reserve stock for each order item
            for (OrderItem item : order.getOrderItems()) {
                boolean reserved = stockReservationService.reserveStock(
                    item.getProduct().getProductId(), 
                    item.getQuantity(), 
                    reservationId, 
                    60 // 60 minutes reservation
                );
                
                if (!reserved) {
                    throw new InventoryException("Failed to reserve stock for product: " + 
                        item.getProduct().getTitle());
                }
            }
            stockReserved = true;
            
            // Perform state transition
            StateTransitionResult transitionResult = transitionOrderState(
                orderId, OrderStatus.APPROVED, productManagerId, 
                "PRODUCT_MANAGER_APPROVAL", approvalNotes);
            
            // Send notification
            notificationService.sendOrderStatusUpdateNotification(
                order, OrderStatus.PENDING_PROCESSING.name(), OrderStatus.APPROVED.name(),
                "Order approved by Product Manager: " + productManagerId);
            
            Map<String, Object> approvalMetadata = new HashMap<>();
            approvalMetadata.put("approvedBy", productManagerId);
            approvalMetadata.put("approvalTimestamp", LocalDateTime.now());
            approvalMetadata.put("stockReservationId", reservationId);
            
            return new OrderApprovalResult(transitionResult, stockValidation, stockReserved, 
                reservationId, approvalWarnings, approvalMetadata);
            
        } catch (Exception e) {
            // If approval fails and stock was reserved, release it
            if (stockReserved) {
                try {
                    stockReservationService.releaseReservation(reservationId);
                } catch (SQLException releaseError) {
                    logger.log(Level.WARNING, "Failed to release reservation after approval failure", releaseError);
                }
            }
            throw e;
        }
    }
    
    @Override
    public StateTransitionResult rejectOrder(String orderId, String productManagerId, 
                                           String rejectionReason, String rejectionNotes) 
            throws SQLException, ResourceNotFoundException, ValidationException {
        
        logger.info("Product manager " + productManagerId + " rejecting order " + orderId + " with reason: " + rejectionReason);
        
        OrderEntity order = orderEntityDAO.getById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        
        if (order.getOrderStatus() != OrderStatus.PENDING_PROCESSING) {
            throw new ValidationException("Order must be in PENDING_PROCESSING status for rejection");
        }
        
        // Create detailed rejection notes
        String detailedNotes = String.format("Rejection Reason: %s%s%s", 
            rejectionReason,
            rejectionNotes != null ? " | Notes: " : "",
            rejectionNotes != null ? rejectionNotes : "");
        
        // Perform state transition
        StateTransitionResult result = transitionOrderState(
            orderId, OrderStatus.REJECTED, productManagerId, 
            rejectionReason, detailedNotes);
        
        // Send notification
        notificationService.sendOrderStatusUpdateNotification(
            order, OrderStatus.PENDING_PROCESSING.name(), OrderStatus.REJECTED.name(),
            "Order rejected by Product Manager: " + rejectionReason);
        
        return result;
    }
    
    @Override
    public StateTransitionValidationResult validateStateTransition(String orderId, OrderStatus fromStatus, 
                                                                  OrderStatus toStatus, String performedBy) 
            throws SQLException, ResourceNotFoundException {
        
        logger.fine("Validating state transition for order " + orderId + " from " + fromStatus + " to " + toStatus);
        
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, String> context = new HashMap<>();
        
        // Check if transition is allowed by business rules
        Set<OrderStatus> validNextStates = VALID_TRANSITIONS.get(fromStatus);
        if (validNextStates == null || !validNextStates.contains(toStatus)) {
            violations.add(String.format("Transition from %s to %s is not allowed by business rules", 
                fromStatus, toStatus));
        }
        
        // Additional validation based on specific transitions
        switch (toStatus) {
            case APPROVED:
                if (performedBy == null || performedBy.trim().isEmpty()) {
                    violations.add("Product manager ID is required for approval");
                }
                break;
            case REJECTED:
                if (performedBy == null || performedBy.trim().isEmpty()) {
                    violations.add("Product manager ID is required for rejection");
                }
                break;
            case SHIPPING:
                // Could add validation for shipping readiness
                break;
        }
        
        context.put("orderId", orderId);
        context.put("performedBy", performedBy);
        context.put("validationTimestamp", LocalDateTime.now().toString());
        
        boolean isValid = violations.isEmpty();
        String message = isValid ? "State transition validation passed" : 
            "Validation failed: " + String.join("; ", violations);
        
        return new StateTransitionValidationResult(isValid, orderId, fromStatus, toStatus, 
            message, violations, warnings, context);
    }
    
    @Override
    public StateTransitionResult transitionOrderState(String orderId, OrderStatus toStatus, 
                                                     String performedBy, String transitionReason, 
                                                     String transitionNotes) 
            throws SQLException, ResourceNotFoundException, ValidationException {
        
        logger.info("Transitioning order " + orderId + " to status " + toStatus + " by " + performedBy);
        
        OrderEntity order = orderEntityDAO.getById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        
        OrderStatus previousStatus = order.getOrderStatus();
        
        // Validate the transition
        StateTransitionValidationResult validation = validateStateTransition(
            orderId, previousStatus, toStatus, performedBy);
        
        if (!validation.isValid()) {
            throw new ValidationException("Invalid state transition: " + validation.getValidationMessage());
        }
        
        // Perform the actual state change
        LocalDateTime transitionTimestamp = LocalDateTime.now();
        String transitionId = generateTransitionId(orderId, transitionTimestamp);
        
        try {
            // Update order status
            orderEntityDAO.updateStatus(orderId, toStatus);
            
            // Record transition in history
            StateTransitionRecord record = new StateTransitionRecord(
                transitionId, orderId, previousStatus, toStatus, performedBy, 
                transitionTimestamp, transitionReason, transitionNotes, true, 
                validation.getValidationContext());
            
            recordStateTransition(record);
            
            logger.info("Successfully transitioned order " + orderId + " from " + previousStatus + " to " + toStatus);
            
            return new StateTransitionResult(true, orderId, previousStatus, toStatus, 
                performedBy, transitionTimestamp, transitionReason, transitionNotes, 
                transitionId, validation.getWarnings(), validation.getValidationContext());
                
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to transition order state", e);
            
            // Record failed transition
            StateTransitionRecord failedRecord = new StateTransitionRecord(
                transitionId, orderId, previousStatus, toStatus, performedBy, 
                transitionTimestamp, transitionReason, "FAILED: " + e.getMessage(), false, 
                validation.getValidationContext());
            
            recordStateTransition(failedRecord);
            
            throw new ValidationException("Failed to update order status: " + e.getMessage());
        }
    }
    
    @Override
    public List<StateTransitionRecord> getOrderStateHistory(String orderId) 
            throws SQLException, ResourceNotFoundException {
        
        // Verify order exists
        OrderEntity order = orderEntityDAO.getById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        
        return stateTransitionHistory.getOrDefault(orderId, new ArrayList<>())
            .stream()
            .sorted(Comparator.comparing(StateTransitionRecord::getTransitionTimestamp))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<OrderEntity> getPendingApprovalOrders(String productManagerId) throws SQLException {
        
        List<OrderEntity> allPendingOrders = orderEntityDAO.getByStatus(OrderStatus.PENDING_PROCESSING);
        
        // In a real implementation, you might filter by product manager assignment
        // For now, return all pending orders
        logger.info("Retrieved " + allPendingOrders.size() + " orders pending approval");
        
        return allPendingOrders;
    }
    
    @Override
    public Map<OrderStatus, String> getValidNextStates(String orderId, String performedBy) 
            throws SQLException, ResourceNotFoundException {
        
        OrderEntity order = orderEntityDAO.getById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        
        OrderStatus currentStatus = order.getOrderStatus();
        Set<OrderStatus> validStates = VALID_TRANSITIONS.getOrDefault(currentStatus, new HashSet<>());
        
        Map<OrderStatus, String> result = new HashMap<>();
        for (OrderStatus status : validStates) {
            result.put(status, getTransitionReason(currentStatus, status));
        }
        
        return result;
    }
    
    @Override
    public OrderStockValidationResult validateOrderStock(String orderId) 
            throws SQLException, ResourceNotFoundException {
        
        logger.info("Validating stock for order " + orderId);
        
        OrderEntity order = orderEntityDAO.getById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        
        // Convert order items to a format suitable for stock validation
        List<OrderItem> orderItems = order.getOrderItems();
        
        // Use the stock validation service to check availability
        IStockValidationService.BulkStockValidationResult bulkResult = 
            stockValidationService.validateOrderItemsStock(orderItems);
        
        List<String> stockIssues = new ArrayList<>();
        List<String> stockWarnings = new ArrayList<>();
        Map<String, Integer> stockShortfalls = new HashMap<>();
        
        if (!bulkResult.isAllValid()) {
            for (IStockValidationService.StockValidationResult failedResult : bulkResult.getFailedValidations()) {
                stockIssues.add(String.format("Product %s: %s", 
                    failedResult.getProductTitle(), failedResult.getMessage()));
                stockShortfalls.put(failedResult.getProductId(), failedResult.getShortfallQuantity());
            }
        }
        
        // Check for low stock warnings
        for (IStockValidationService.StockValidationResult result : bulkResult.getIndividualResults()) {
            if (result.isValid() && result.getAvailableStock() < result.getRequestedQuantity() * 2) {
                stockWarnings.add(String.format("Low stock warning for %s: only %d available", 
                    result.getProductTitle(), result.getAvailableStock()));
            }
        }
        
        boolean canProceedWithPartialStock = stockIssues.isEmpty() || 
            stockShortfalls.values().stream().mapToInt(Integer::intValue).sum() < 
            orderItems.stream().mapToInt(OrderItem::getQuantity).sum() / 2;
        
        return new OrderStockValidationResult(bulkResult.isAllValid(), orderId, bulkResult, 
            stockIssues, stockWarnings, canProceedWithPartialStock, stockShortfalls);
    }
    
    @Override
    public OrderStateStatistics getOrderStateStatistics(LocalDateTime fromDate, LocalDateTime toDate) 
            throws SQLException {
        
        logger.info("Generating order state statistics from " + fromDate + " to " + toDate);
        
        // This is a simplified implementation - in production, this would query the database
        Map<OrderStatus, Integer> statusCounts = new HashMap<>();
        Map<String, Integer> transitionCounts = new HashMap<>();
        Map<String, Integer> productManagerActivity = new HashMap<>();
        List<String> topRejectionReasons = new ArrayList<>();
        
        // Count transitions within date range
        int totalTransitions = 0;
        int totalApprovals = 0;
        int totalRejections = 0;
        double totalApprovalTimeMillis = 0;
        int approvalTimeCount = 0;
        
        for (List<StateTransitionRecord> orderHistory : stateTransitionHistory.values()) {
            for (StateTransitionRecord record : orderHistory) {
                if (record.getTransitionTimestamp().isAfter(fromDate) && 
                    record.getTransitionTimestamp().isBefore(toDate)) {
                    
                    totalTransitions++;
                    
                    String transitionKey = record.getFromStatus() + "_TO_" + record.getToStatus();
                    transitionCounts.merge(transitionKey, 1, Integer::sum);
                    
                    if (record.getToStatus() == OrderStatus.APPROVED) {
                        totalApprovals++;
                        productManagerActivity.merge(record.getPerformedBy(), 1, Integer::sum);
                    } else if (record.getToStatus() == OrderStatus.REJECTED) {
                        totalRejections++;
                        topRejectionReasons.add(record.getTransitionReason());
                    }
                }
            }
        }
        
        // Calculate average approval time (simplified)
        double averageApprovalTime = approvalTimeCount > 0 ? totalApprovalTimeMillis / approvalTimeCount : 0;
        
        return new OrderStateStatistics(fromDate, toDate, statusCounts, transitionCounts,
            totalTransitions, totalApprovals, totalRejections, averageApprovalTime,
            productManagerActivity, topRejectionReasons.stream().distinct().limit(5).collect(Collectors.toList()));
    }
    
    // Helper methods
    
    private void recordStateTransition(StateTransitionRecord record) {
        stateTransitionHistory.computeIfAbsent(record.getOrderId(), k -> new ArrayList<>()).add(record);
        
        // Limit history size to prevent memory issues (in production, use database)
        List<StateTransitionRecord> history = stateTransitionHistory.get(record.getOrderId());
        if (history.size() > 100) {
            history.remove(0); // Remove oldest record
        }
    }
    
    private String generateTransitionId(String orderId, LocalDateTime timestamp) {
        return String.format("TRANS_%s_%d", orderId, timestamp.toEpochSecond(java.time.ZoneOffset.UTC));
    }
    
    private String getTransitionReason(OrderStatus fromStatus, OrderStatus toStatus) {
        return switch (toStatus) {
            case PENDING_PROCESSING -> "Submit for approval";
            case APPROVED -> "Product manager approval";
            case REJECTED -> "Product manager rejection";
            case SHIPPING -> "Begin shipping process";
            case DELIVERED -> "Order delivered successfully";
            case CANCELLED -> "Order cancellation";
            case REFUNDED -> "Process refund";
            default -> "Status update";
        };
    }
}