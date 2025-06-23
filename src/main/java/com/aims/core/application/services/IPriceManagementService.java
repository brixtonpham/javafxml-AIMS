package com.aims.core.application.services;

import com.aims.core.shared.exceptions.ValidationException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing product price validation and constraints.
 * Implements business rules for price changes and daily limits per problem statement lines 38-40.
 */
public interface IPriceManagementService {

    /**
     * Validates that a price change is within allowed range (30%-150% of product value).
     * Per problem statement lines 38-40: Price must be between 30% and 150% of product value.
     *
     * @param newPrice The proposed new price
     * @param productValue The base value of the product
     * @return true if price is within valid range
     */
    boolean validatePriceRange(float newPrice, float productValue);

    /**
     * Checks if a product manager can update the price for a specific product.
     * Enforces maximum 2 price updates per product per day per manager.
     *
     * @param productId The ID of the product
     * @param managerId The ID of the product manager
     * @return true if price update is allowed
     * @throws SQLException If database error occurs
     */
    boolean canUpdatePrice(String productId, String managerId) throws SQLException;

    /**
     * Records a price update operation in the audit trail.
     * Tracks old and new prices for audit purposes.
     *
     * @param productId The ID of the product
     * @param managerId The ID of the product manager
     * @param oldPrice The previous price
     * @param newPrice The new price
     * @throws SQLException If database error occurs
     */
    void recordPriceUpdate(String productId, String managerId, float oldPrice, float newPrice) throws SQLException;

    /**
     * Gets all price update records for a manager on a specific date.
     *
     * @param managerId The ID of the product manager
     * @param date The date to check
     * @return List of price update records
     * @throws SQLException If database error occurs
     */
    List<PriceUpdateRecord> getDailyPriceUpdates(String managerId, LocalDate date) throws SQLException;

    /**
     * Validates a price update request against all business rules.
     * Combines range validation and daily limit checks.
     *
     * @param productId The ID of the product
     * @param newPrice The proposed new price
     * @param productValue The base value of the product
     * @param managerId The ID of the product manager
     * @return PriceValidationResult with validation outcome
     * @throws ValidationException If validation fails
     * @throws SQLException If database error occurs
     */
    PriceValidationResult validatePriceUpdate(String productId, float newPrice, float productValue, String managerId) 
            throws ValidationException, SQLException;

    /**
     * Calculates the valid price range for a product.
     *
     * @param productValue The base value of the product
     * @return PriceRange object with minimum and maximum allowed prices
     */
    PriceRange calculateValidPriceRange(float productValue);

    /**
     * Represents a price update record for audit purposes.
     */
    public static class PriceUpdateRecord {
        private final String productId;
        private final String managerId;
        private final float oldPrice;
        private final float newPrice;
        private final LocalDate updateDate;
        private final String updateTime;

        public PriceUpdateRecord(String productId, String managerId, float oldPrice, float newPrice, 
                               LocalDate updateDate, String updateTime) {
            this.productId = productId;
            this.managerId = managerId;
            this.oldPrice = oldPrice;
            this.newPrice = newPrice;
            this.updateDate = updateDate;
            this.updateTime = updateTime;
        }

        // Getters
        public String getProductId() { return productId; }
        public String getManagerId() { return managerId; }
        public float getOldPrice() { return oldPrice; }
        public float getNewPrice() { return newPrice; }
        public LocalDate getUpdateDate() { return updateDate; }
        public String getUpdateTime() { return updateTime; }
    }

    /**
     * Represents the result of price validation.
     */
    public static class PriceValidationResult {
        private final boolean valid;
        private final String message;
        private final PriceRange validRange;

        public PriceValidationResult(boolean valid, String message, PriceRange validRange) {
            this.valid = valid;
            this.message = message;
            this.validRange = validRange;
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public PriceRange getValidRange() { return validRange; }
    }

    /**
     * Represents a valid price range for a product.
     */
    public static class PriceRange {
        private final float minimumPrice;
        private final float maximumPrice;

        public PriceRange(float minimumPrice, float maximumPrice) {
            this.minimumPrice = minimumPrice;
            this.maximumPrice = maximumPrice;
        }

        // Getters
        public float getMinimumPrice() { return minimumPrice; }
        public float getMaximumPrice() { return maximumPrice; }

        public boolean isWithinRange(float price) {
            return price >= minimumPrice && price <= maximumPrice;
        }
    }
}