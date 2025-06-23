package com.aims.core.application.impl;

import com.aims.core.application.services.IProductManagerAuditService;
import com.aims.core.infrastructure.database.dao.IProductManagerAuditDAO;
import com.aims.core.shared.exceptions.ValidationException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;

@Service
public class ProductManagerAuditServiceImpl implements IProductManagerAuditService {

    private final IProductManagerAuditDAO auditDAO;
    
    private static final int MAX_OPERATIONS_PER_DAY = 30;
    private static final int MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY = 2;

    public ProductManagerAuditServiceImpl(IProductManagerAuditDAO auditDAO) {
        this.auditDAO = auditDAO;
    }

    @Override
    public void checkDailyOperationLimit(String managerId, int operationsCount) throws ValidationException, SQLException {
        LocalDate today = LocalDate.now();
        int currentOperations = auditDAO.getOperationCount(managerId, today);
        
        if (currentOperations + operationsCount > MAX_OPERATIONS_PER_DAY) {
            throw new ValidationException(
                String.format("Daily operation limit exceeded. Current operations: %d, Requested: %d, Maximum allowed: %d",
                    currentOperations, operationsCount, MAX_OPERATIONS_PER_DAY)
            );
        }
    }

    @Override
    public void checkPriceUpdateLimit(String managerId, String productId) throws ValidationException, SQLException {
        LocalDate today = LocalDate.now();
        int currentPriceUpdates = auditDAO.getPriceUpdateCount(managerId, productId, today);
        
        if (currentPriceUpdates >= MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY) {
            throw new ValidationException(
                String.format("Price update limit exceeded for product %s. Current updates today: %d, Maximum allowed: %d",
                    productId, currentPriceUpdates, MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY)
            );
        }
    }

    @Override
    public void logOperation(String managerId, String operationType, String productId, String details) throws SQLException {
        auditDAO.logOperation(managerId, operationType, productId, details);
    }

    @Override
    public int getOperationCount(String managerId, LocalDate date) throws SQLException {
        return auditDAO.getOperationCount(managerId, date);
    }

    @Override
    public int getPriceUpdateCount(String managerId, String productId, LocalDate date) throws SQLException {
        return auditDAO.getPriceUpdateCount(managerId, productId, date);
    }

    @Override
    public java.util.List<String> getManagerOperations(String managerId, String dateKey) throws SQLException {
        // Convert dateKey to LocalDate for DAO call
        LocalDate date = LocalDate.parse(dateKey);
        // For now, return a simple implementation - in production this would query the database
        // This method should return operation keys for the given manager and date
        return auditDAO.getManagerOperations(managerId, date);
    }

    @Override
    public void recordOperation(String managerId, String operationKey) throws SQLException {
        // Extract operation details from the operation key and log the operation
        // Operation key format is typically: OPERATION_TYPE_PRODUCT_ID_DATE_ADDITIONAL_INFO
        String[] parts = operationKey.split("_");
        if (parts.length >= 2) {
            String operationType = parts[0];
            String productId = parts.length > 1 ? parts[1] : "";
            String details = operationKey; // Use full key as details
            logOperation(managerId, operationType, productId, details);
        } else {
            // Fallback for malformed operation keys
            logOperation(managerId, "UNKNOWN", "", operationKey);
        }
    }
}