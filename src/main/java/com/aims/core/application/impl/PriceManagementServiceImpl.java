package com.aims.core.application.impl;

import org.springframework.stereotype.Service;
import com.aims.core.application.services.IPriceManagementService;
import com.aims.core.application.services.IProductManagerAuditService;
import com.aims.core.shared.exceptions.ValidationException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of IPriceManagementService.
 * Handles price validation and tracking per problem statement requirements.
 */
@Service
public class PriceManagementServiceImpl implements IPriceManagementService {

    private final IProductManagerAuditService auditService;
    
    // Business rule constants from problem statement lines 38-40
    private static final float MIN_PRICE_PERCENTAGE = 0.30f; // 30%
    private static final float MAX_PRICE_PERCENTAGE = 1.50f; // 150%
    private static final int MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY = 2;

    public PriceManagementServiceImpl(IProductManagerAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public boolean validatePriceRange(float newPrice, float productValue) {
        if (productValue <= 0) {
            return false;
        }
        
        float minPrice = productValue * MIN_PRICE_PERCENTAGE;
        float maxPrice = productValue * MAX_PRICE_PERCENTAGE;
        
        return newPrice >= minPrice && newPrice <= maxPrice;
    }

    @Override
    public boolean canUpdatePrice(String productId, String managerId) throws SQLException {
        LocalDate today = LocalDate.now();
        int currentUpdates = auditService.getPriceUpdateCount(managerId, productId, today);
        return currentUpdates < MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY;
    }

    @Override
    public void recordPriceUpdate(String productId, String managerId, float oldPrice, float newPrice) throws SQLException {
        String details = String.format("Price changed from %.2f to %.2f", oldPrice, newPrice);
        auditService.logOperation(managerId, "PRICE_UPDATE", productId, details);
    }

    @Override
    public List<PriceUpdateRecord> getDailyPriceUpdates(String managerId, LocalDate date) throws SQLException {
        // Note: This would require extending the audit DAO to return detailed records
        // For now, returning empty list as the current audit system only tracks counts
        // This would be implemented when the audit DAO is enhanced
        return new ArrayList<>();
    }

    @Override
    public PriceValidationResult validatePriceUpdate(String productId, float newPrice, float productValue, String managerId) 
            throws ValidationException, SQLException {
        
        PriceRange validRange = calculateValidPriceRange(productValue);
        
        // Check price range first
        if (!validatePriceRange(newPrice, productValue)) {
            String message = String.format(
                "Price %.2f is outside valid range. Must be between %.2f and %.2f (30%%-150%% of product value %.2f)",
                newPrice, validRange.getMinimumPrice(), validRange.getMaximumPrice(), productValue
            );
            return new PriceValidationResult(false, message, validRange);
        }
        
        // Check daily update limit
        if (!canUpdatePrice(productId, managerId)) {
            LocalDate today = LocalDate.now();
            int currentUpdates = auditService.getPriceUpdateCount(managerId, productId, today);
            String message = String.format(
                "Daily price update limit exceeded for product %s. Updates today: %d, Maximum allowed: %d",
                productId, currentUpdates, MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY
            );
            return new PriceValidationResult(false, message, validRange);
        }
        
        return new PriceValidationResult(true, "Price update is valid", validRange);
    }

    @Override
    public PriceRange calculateValidPriceRange(float productValue) {
        if (productValue <= 0) {
            return new PriceRange(0, 0);
        }
        
        float minPrice = productValue * MIN_PRICE_PERCENTAGE;
        float maxPrice = productValue * MAX_PRICE_PERCENTAGE;
        
        return new PriceRange(minPrice, maxPrice);
    }

    /**
     * Validates a price update and records it if valid.
     * This is a convenience method that combines validation and recording.
     *
     * @param productId The ID of the product
     * @param oldPrice The current price
     * @param newPrice The proposed new price
     * @param productValue The base value of the product
     * @param managerId The ID of the product manager
     * @throws ValidationException If validation fails
     * @throws SQLException If database error occurs
     */
    public void validateAndRecordPriceUpdate(String productId, float oldPrice, float newPrice, 
                                           float productValue, String managerId) 
            throws ValidationException, SQLException {
        
        PriceValidationResult result = validatePriceUpdate(productId, newPrice, productValue, managerId);
        
        if (!result.isValid()) {
            throw new ValidationException(result.getMessage());
        }
        
        recordPriceUpdate(productId, managerId, oldPrice, newPrice);
    }

    /**
     * Gets the remaining price updates allowed for a product today.
     *
     * @param productId The ID of the product
     * @param managerId The ID of the product manager
     * @return Number of price updates remaining today
     * @throws SQLException If database error occurs
     */
    public int getRemainingPriceUpdates(String productId, String managerId) throws SQLException {
        LocalDate today = LocalDate.now();
        int currentUpdates = auditService.getPriceUpdateCount(managerId, productId, today);
        return Math.max(0, MAX_PRICE_UPDATES_PER_PRODUCT_PER_DAY - currentUpdates);
    }

    /**
     * Checks if a price is at the boundary of the valid range.
     *
     * @param price The price to check
     * @param productValue The base value of the product
     * @return true if price is at 30% or 150% boundary
     */
    public boolean isPriceAtBoundary(float price, float productValue) {
        if (productValue <= 0) {
            return false;
        }
        
        float minPrice = productValue * MIN_PRICE_PERCENTAGE;
        float maxPrice = productValue * MAX_PRICE_PERCENTAGE;
        
        // Allow small floating point tolerance
        float tolerance = 0.01f;
        
        return Math.abs(price - minPrice) < tolerance || Math.abs(price - maxPrice) < tolerance;
    }
}