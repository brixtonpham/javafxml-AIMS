package com.aims.core.application.services;

import com.aims.core.shared.exceptions.ValidationException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for enforcing daily operation constraints.
 * Implements business rules per problem statement lines 16-19.
 */
public interface IOperationConstraintService {

    /**
     * Checks if a product manager can add a product.
     * Per problem statement: Unlimited additions allowed.
     *
     * @param managerId The ID of the product manager
     * @return always true (unlimited additions)
     */
    boolean canAddProduct(String managerId);

    /**
     * Checks if a product manager can edit a specific product.
     * Per problem statement: Max 1 product edit at a time, max 30 total operations per day.
     *
     * @param managerId The ID of the product manager
     * @param productId The ID of the product to edit
     * @return true if edit is allowed
     * @throws SQLException If database error occurs
     */
    boolean canEditProduct(String managerId, String productId) throws SQLException;

    /**
     * Checks if a product manager can delete the specified products.
     * Per problem statement: Max 10 products deletion at once, max 30 total operations per day.
     *
     * @param managerId The ID of the product manager
     * @param productIds List of product IDs to delete
     * @return true if deletion is allowed
     * @throws SQLException If database error occurs
     */
    boolean canDeleteProducts(String managerId, List<String> productIds) throws SQLException;

    /**
     * Checks if a product manager can update the price of a specific product.
     * Per problem statement: Max 2 price updates per product per day.
     *
     * @param managerId The ID of the product manager
     * @param productId The ID of the product
     * @return true if price update is allowed
     * @throws SQLException If database error occurs
     */
    boolean canUpdatePrice(String managerId, String productId) throws SQLException;

    /**
     * Gets the current quota status for a product manager.
     *
     * @param managerId The ID of the product manager
     * @param date The date to check
     * @return OperationQuotaStatus with current usage and limits
     * @throws SQLException If database error occurs
     */
    OperationQuotaStatus getQuotaStatus(String managerId, LocalDate date) throws SQLException;

    /**
     * Validates a bulk operation against all constraints.
     *
     * @param managerId The ID of the product manager
     * @param operation The type of operation
     * @param productIds List of product IDs involved
     * @throws ValidationException If operation violates constraints
     * @throws SQLException If database error occurs
     */
    void validateBulkOperation(String managerId, OperationType operation, List<String> productIds) 
            throws ValidationException, SQLException;

    /**
     * Validates a single product operation against constraints.
     *
     * @param managerId The ID of the product manager
     * @param operation The type of operation
     * @param productId The ID of the product (optional for some operations)
     * @throws ValidationException If operation violates constraints
     * @throws SQLException If database error occurs
     */
    void validateSingleOperation(String managerId, OperationType operation, String productId) 
            throws ValidationException, SQLException;

    /**
     * Records an operation for quota tracking.
     *
     * @param managerId The ID of the product manager
     * @param operation The type of operation
     * @param productIds List of product IDs involved
     * @throws SQLException If database error occurs
     */
    void recordOperation(String managerId, OperationType operation, List<String> productIds) 
            throws SQLException;

    /**
     * Checks if there are any active edit sessions for a manager.
     * Per problem statement: Max 1 concurrent edit operation.
     *
     * @param managerId The ID of the product manager
     * @return true if there are active edit sessions
     * @throws SQLException If database error occurs
     */
    boolean hasActiveEditSession(String managerId) throws SQLException;

    /**
     * Starts an edit session for a product.
     *
     * @param managerId The ID of the product manager
     * @param productId The ID of the product being edited
     * @throws ValidationException If concurrent edit limit exceeded
     * @throws SQLException If database error occurs
     */
    void startEditSession(String managerId, String productId) throws ValidationException, SQLException;

    /**
     * Ends an edit session for a product.
     *
     * @param managerId The ID of the product manager
     * @param productId The ID of the product being edited
     * @throws SQLException If database error occurs
     */
    void endEditSession(String managerId, String productId) throws SQLException;

    /**
     * Enum for operation types.
     */
    public enum OperationType {
        ADD,
        EDIT,
        DELETE,
        PRICE_UPDATE,
        BULK_DELETE
    }

    /**
     * Represents the current quota status for a product manager.
     */
    public static class OperationQuotaStatus {
        private final int dailyOperationsUsed;
        private final int dailyOperationsLimit;
        private final int additionsToday;
        private final int editsToday;
        private final int deletionsToday;
        private final int priceUpdatesTotal;
        private final boolean hasActiveEditSession;
        private final String activeEditProductId;
        private final LocalDate date;

        public OperationQuotaStatus(int dailyOperationsUsed, int dailyOperationsLimit,
                                  int additionsToday, int editsToday, int deletionsToday,
                                  int priceUpdatesTotal, boolean hasActiveEditSession,
                                  String activeEditProductId, LocalDate date) {
            this.dailyOperationsUsed = dailyOperationsUsed;
            this.dailyOperationsLimit = dailyOperationsLimit;
            this.additionsToday = additionsToday;
            this.editsToday = editsToday;
            this.deletionsToday = deletionsToday;
            this.priceUpdatesTotal = priceUpdatesTotal;
            this.hasActiveEditSession = hasActiveEditSession;
            this.activeEditProductId = activeEditProductId;
            this.date = date;
        }

        // Getters
        public int getDailyOperationsUsed() { return dailyOperationsUsed; }
        public int getDailyOperationsLimit() { return dailyOperationsLimit; }
        public int getDailyOperationsRemaining() { return dailyOperationsLimit - dailyOperationsUsed; }
        public int getAdditionsToday() { return additionsToday; }
        public int getEditsToday() { return editsToday; }
        public int getDeletionsToday() { return deletionsToday; }
        public int getPriceUpdatesTotal() { return priceUpdatesTotal; }
        public boolean hasActiveEditSession() { return hasActiveEditSession; }
        public String getActiveEditProductId() { return activeEditProductId; }
        public LocalDate getDate() { return date; }
        
        public boolean canPerformOperation(OperationType operation, int quantity) {
            switch (operation) {
                case ADD:
                    return true; // Unlimited additions
                case EDIT:
                    return !hasActiveEditSession && getDailyOperationsRemaining() >= quantity;
                case DELETE:
                case BULK_DELETE:
                    return quantity <= 10 && getDailyOperationsRemaining() >= quantity;
                case PRICE_UPDATE:
                    return getDailyOperationsRemaining() >= quantity;
                default:
                    return false;
            }
        }
    }

    /**
     * Represents the result of an operation validation.
     */
    public static class OperationValidationResult {
        private final boolean allowed;
        private final String message;
        private final OperationType operation;
        private final int requestedQuantity;
        private final OperationQuotaStatus currentStatus;

        public OperationValidationResult(boolean allowed, String message, OperationType operation,
                                       int requestedQuantity, OperationQuotaStatus currentStatus) {
            this.allowed = allowed;
            this.message = message;
            this.operation = operation;
            this.requestedQuantity = requestedQuantity;
            this.currentStatus = currentStatus;
        }

        // Getters
        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
        public OperationType getOperation() { return operation; }
        public int getRequestedQuantity() { return requestedQuantity; }
        public OperationQuotaStatus getCurrentStatus() { return currentStatus; }
    }
}