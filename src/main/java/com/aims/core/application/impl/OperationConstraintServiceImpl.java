package com.aims.core.application.impl;

import com.aims.core.application.services.IOperationConstraintService;
import com.aims.core.application.services.IProductManagerAuditService;
import com.aims.core.shared.exceptions.ValidationException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of IOperationConstraintService that enforces daily operation constraints.
 * Implements business rules per problem statement lines 16-19.
 */
public class OperationConstraintServiceImpl implements IOperationConstraintService {

    private static final int DAILY_OPERATIONS_LIMIT = 30;
    private static final int MAX_BULK_DELETE_SIZE = 10;
    private static final int MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY = 2;
    
    private final IProductManagerAuditService auditService;
    
    // Thread-safe storage for active edit sessions
    private final Map<String, String> activeEditSessions = new ConcurrentHashMap<>();
    
    public OperationConstraintServiceImpl(IProductManagerAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public boolean canAddProduct(String managerId) {
        // Per problem statement: Unlimited additions allowed
        return true;
    }

    @Override
    public boolean canEditProduct(String managerId, String productId) throws SQLException {
        // Check for active edit session (max 1 concurrent edit)
        if (hasActiveEditSession(managerId)) {
            return false;
        }
        
        // Check daily operations limit
        OperationQuotaStatus status = getQuotaStatus(managerId, LocalDate.now());
        return status.canPerformOperation(OperationType.EDIT, 1);
    }

    @Override
    public boolean canDeleteProducts(String managerId, List<String> productIds) throws SQLException {
        if (productIds == null || productIds.isEmpty()) {
            return false;
        }
        
        // Check bulk delete size limit
        if (productIds.size() > MAX_BULK_DELETE_SIZE) {
            return false;
        }
        
        // Check daily operations limit
        OperationQuotaStatus status = getQuotaStatus(managerId, LocalDate.now());
        return status.canPerformOperation(OperationType.BULK_DELETE, productIds.size());
    }

    @Override
    public boolean canUpdatePrice(String managerId, String productId) throws SQLException {
        OperationQuotaStatus status = getQuotaStatus(managerId, LocalDate.now());
        return status.canPerformOperation(OperationType.PRICE_UPDATE, 1);
    }

    @Override
    public OperationQuotaStatus getQuotaStatus(String managerId, LocalDate date) throws SQLException {
        String dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        // Get all operations for the manager on the given date
        List<String> operations = auditService.getManagerOperations(managerId, dateKey);
        
        // Count operations by type
        int totalOperations = 0;
        int additionsToday = 0;
        int editsToday = 0;
        int deletionsToday = 0;
        int priceUpdatesTotal = 0;
        
        for (String operation : operations) {
            if (operation.startsWith("ADD_")) {
                additionsToday++;
                // Additions don't count toward daily limit
            } else if (operation.startsWith("EDIT_")) {
                editsToday++;
                totalOperations++;
            } else if (operation.startsWith("DELETE_")) {
                deletionsToday++;
                totalOperations++;
            } else if (operation.startsWith("PRICE_UPDATE_")) {
                priceUpdatesTotal++;
                totalOperations++;
            }
        }
        
        // Check for active edit session
        boolean hasActiveSession = hasActiveEditSession(managerId);
        String activeEditProductId = activeEditSessions.get(managerId);
        
        return new OperationQuotaStatus(
            totalOperations,
            DAILY_OPERATIONS_LIMIT,
            additionsToday,
            editsToday,
            deletionsToday,
            priceUpdatesTotal,
            hasActiveSession,
            activeEditProductId,
            date
        );
    }

    @Override
    public void validateBulkOperation(String managerId, OperationType operation, List<String> productIds) 
            throws ValidationException, SQLException {
        
        if (productIds == null || productIds.isEmpty()) {
            throw new ValidationException("Product IDs list cannot be null or empty");
        }
        
        int quantity = productIds.size();
        OperationQuotaStatus status = getQuotaStatus(managerId, LocalDate.now());
        
        switch (operation) {
            case ADD:
                // Unlimited additions allowed
                break;
                
            case BULK_DELETE:
                if (quantity > MAX_BULK_DELETE_SIZE) {
                    throw new ValidationException(
                        String.format("Cannot delete %d products at once. Maximum allowed: %d", 
                                    quantity, MAX_BULK_DELETE_SIZE));
                }
                if (!status.canPerformOperation(operation, quantity)) {
                    throw new ValidationException(
                        String.format("Bulk delete operation would exceed daily limit. " +
                                    "Remaining operations: %d, Requested: %d", 
                                    status.getDailyOperationsRemaining(), quantity));
                }
                break;
                
            case EDIT:
                if (status.hasActiveEditSession()) {
                    throw new ValidationException(
                        String.format("Cannot start bulk edit. Active edit session exists for product: %s", 
                                    status.getActiveEditProductId()));
                }
                if (!status.canPerformOperation(operation, quantity)) {
                    throw new ValidationException(
                        String.format("Bulk edit operation would exceed daily limit. " +
                                    "Remaining operations: %d, Requested: %d", 
                                    status.getDailyOperationsRemaining(), quantity));
                }
                break;
                
            default:
                throw new ValidationException("Unsupported bulk operation type: " + operation);
        }
    }

    @Override
    public void validateSingleOperation(String managerId, OperationType operation, String productId) 
            throws ValidationException, SQLException {
        
        OperationQuotaStatus status = getQuotaStatus(managerId, LocalDate.now());
        
        switch (operation) {
            case ADD:
                // Unlimited additions allowed
                break;
                
            case EDIT:
                if (status.hasActiveEditSession()) {
                    throw new ValidationException(
                        String.format("Cannot start edit. Active edit session exists for product: %s", 
                                    status.getActiveEditProductId()));
                }
                if (!status.canPerformOperation(operation, 1)) {
                    throw new ValidationException(
                        String.format("Edit operation would exceed daily limit. " +
                                    "Remaining operations: %d", status.getDailyOperationsRemaining()));
                }
                break;
                
            case DELETE:
                if (!status.canPerformOperation(operation, 1)) {
                    throw new ValidationException(
                        String.format("Delete operation would exceed daily limit. " +
                                    "Remaining operations: %d", status.getDailyOperationsRemaining()));
                }
                break;
                
            case PRICE_UPDATE:
                if (!status.canPerformOperation(operation, 1)) {
                    throw new ValidationException(
                        String.format("Price update would exceed daily limit. " +
                                    "Remaining operations: %d", status.getDailyOperationsRemaining()));
                }
                break;
                
            default:
                throw new ValidationException("Unsupported operation type: " + operation);
        }
    }

    @Override
    public void recordOperation(String managerId, OperationType operation, List<String> productIds) 
            throws SQLException {
        
        String dateKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        for (String productId : productIds) {
            String operationKey = operation.name() + "_" + productId + "_" + dateKey;
            auditService.recordOperation(managerId, operationKey);
        }
    }

    @Override
    public boolean hasActiveEditSession(String managerId) throws SQLException {
        return activeEditSessions.containsKey(managerId);
    }

    @Override
    public void startEditSession(String managerId, String productId) throws ValidationException, SQLException {
        if (hasActiveEditSession(managerId)) {
            throw new ValidationException(
                String.format("Manager %s already has an active edit session for product: %s", 
                            managerId, activeEditSessions.get(managerId)));
        }
        
        activeEditSessions.put(managerId, productId);
        
        // Record the start of edit session
        String operationKey = "EDIT_SESSION_START_" + productId + "_" + 
                            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        auditService.recordOperation(managerId, operationKey);
    }

    @Override
    public void endEditSession(String managerId, String productId) throws SQLException {
        String activeProductId = activeEditSessions.remove(managerId);
        
        if (activeProductId != null) {
            // Record the end of edit session
            String operationKey = "EDIT_SESSION_END_" + productId + "_" + 
                                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            auditService.recordOperation(managerId, operationKey);
        }
    }

    /**
     * Helper method to validate price update constraints for a specific product.
     * This integrates with the PriceManagementService constraints.
     */
    public boolean canUpdatePriceForProduct(String managerId, String productId) throws SQLException {
        String dateKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<String> operations = auditService.getManagerOperations(managerId, dateKey);
        
        // Count price updates for this specific product today
        int priceUpdatesForProduct = 0;
        String priceUpdatePrefix = "PRICE_UPDATE_" + productId;
        
        for (String operation : operations) {
            if (operation.startsWith(priceUpdatePrefix)) {
                priceUpdatesForProduct++;
            }
        }
        
        return priceUpdatesForProduct < MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY;
    }

    /**
     * Records a price update operation for audit tracking.
     */
    public void recordPriceUpdate(String managerId, String productId, float oldPrice, float newPrice) 
            throws SQLException {
        String dateKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String operationKey = String.format("PRICE_UPDATE_%s_%s_%.2f_TO_%.2f", 
                                          productId, dateKey, oldPrice, newPrice);
        auditService.recordOperation(managerId, operationKey);
    }
}