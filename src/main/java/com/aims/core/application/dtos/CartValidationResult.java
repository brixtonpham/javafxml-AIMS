package com.aims.core.application.dtos;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing the result of cart validation for order creation.
 * Contains validation status, error messages, and recommendations for cart improvement.
 */
public class CartValidationResult {
    
    private boolean isValid;
    private boolean hasStockIssues;
    private boolean hasMetadataIssues;
    private boolean hasIntegrityIssues;
    private List<String> validationErrors;
    private List<String> warnings;
    private List<String> missingProductIds;
    private List<String> insufficientStockProductIds;
    private List<String> missingMetadataProductIds;
    private int totalItemsValidated;
    private int validItemsCount;
    private int invalidItemsCount;
    
    public CartValidationResult() {
        this.validationErrors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.missingProductIds = new ArrayList<>();
        this.insufficientStockProductIds = new ArrayList<>();
        this.missingMetadataProductIds = new ArrayList<>();
        this.isValid = true;
        this.hasStockIssues = false;
        this.hasMetadataIssues = false;
        this.hasIntegrityIssues = false;
        this.totalItemsValidated = 0;
        this.validItemsCount = 0;
        this.invalidItemsCount = 0;
    }
    
    /**
     * Factory method to create a successful validation result
     */
    public static CartValidationResult success(int totalItems) {
        CartValidationResult result = new CartValidationResult();
        result.isValid = true;
        result.totalItemsValidated = totalItems;
        result.validItemsCount = totalItems;
        result.invalidItemsCount = 0;
        return result;
    }
    
    /**
     * Factory method to create a failed validation result
     */
    public static CartValidationResult failure(String errorMessage) {
        CartValidationResult result = new CartValidationResult();
        result.isValid = false;
        result.addValidationError(errorMessage);
        return result;
    }
    
    // Helper methods for adding validation issues
    public void addValidationError(String error) {
        this.validationErrors.add(error);
        this.isValid = false;
        this.invalidItemsCount++;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public void addMissingProduct(String productId) {
        this.missingProductIds.add(productId);
        this.hasIntegrityIssues = true;
        this.isValid = false;
    }
    
    public void addInsufficientStockProduct(String productId) {
        this.insufficientStockProductIds.add(productId);
        this.hasStockIssues = true;
        this.isValid = false;
    }
    
    public void addMissingMetadataProduct(String productId) {
        this.missingMetadataProductIds.add(productId);
        this.hasMetadataIssues = true;
    }
    
    public void incrementValidItems() {
        this.validItemsCount++;
    }
    
    public void setTotalItemsValidated(int total) {
        this.totalItemsValidated = total;
    }
    
    // Getters and Setters
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public boolean hasStockIssues() {
        return hasStockIssues;
    }
    
    public void setHasStockIssues(boolean hasStockIssues) {
        this.hasStockIssues = hasStockIssues;
    }
    
    public boolean hasMetadataIssues() {
        return hasMetadataIssues;
    }
    
    public void setHasMetadataIssues(boolean hasMetadataIssues) {
        this.hasMetadataIssues = hasMetadataIssues;
    }
    
    public boolean hasIntegrityIssues() {
        return hasIntegrityIssues;
    }
    
    public void setHasIntegrityIssues(boolean hasIntegrityIssues) {
        this.hasIntegrityIssues = hasIntegrityIssues;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public List<String> getMissingProductIds() {
        return missingProductIds;
    }
    
    public void setMissingProductIds(List<String> missingProductIds) {
        this.missingProductIds = missingProductIds;
    }
    
    public List<String> getInsufficientStockProductIds() {
        return insufficientStockProductIds;
    }
    
    public void setInsufficientStockProductIds(List<String> insufficientStockProductIds) {
        this.insufficientStockProductIds = insufficientStockProductIds;
    }
    
    public List<String> getMissingMetadataProductIds() {
        return missingMetadataProductIds;
    }
    
    public void setMissingMetadataProductIds(List<String> missingMetadataProductIds) {
        this.missingMetadataProductIds = missingMetadataProductIds;
    }
    
    public int getTotalItemsValidated() {
        return totalItemsValidated;
    }
    
    public int getValidItemsCount() {
        return validItemsCount;
    }
    
    public void setValidItemsCount(int validItemsCount) {
        this.validItemsCount = validItemsCount;
    }
    
    public int getInvalidItemsCount() {
        return invalidItemsCount;
    }
    
    public void setInvalidItemsCount(int invalidItemsCount) {
        this.invalidItemsCount = invalidItemsCount;
    }
    
    /**
     * Returns a summary of the validation result
     */
    public String getSummary() {
        if (isValid) {
            return String.format("Cart validation successful: %d/%d items valid", 
                validItemsCount, totalItemsValidated);
        } else {
            StringBuilder summary = new StringBuilder();
            summary.append(String.format("Cart validation failed: %d/%d items invalid. ", 
                invalidItemsCount, totalItemsValidated));
            
            if (hasStockIssues) {
                summary.append(String.format("%d items with stock issues. ", 
                    insufficientStockProductIds.size()));
            }
            if (hasMetadataIssues) {
                summary.append(String.format("%d items with metadata issues. ", 
                    missingMetadataProductIds.size()));
            }
            if (hasIntegrityIssues) {
                summary.append(String.format("%d missing products. ", 
                    missingProductIds.size()));
            }
            
            return summary.toString().trim();
        }
    }
    
    @Override
    public String toString() {
        return "CartValidationResult{" +
                "isValid=" + isValid +
                ", hasStockIssues=" + hasStockIssues +
                ", hasMetadataIssues=" + hasMetadataIssues +
                ", hasIntegrityIssues=" + hasIntegrityIssues +
                ", totalItemsValidated=" + totalItemsValidated +
                ", validItemsCount=" + validItemsCount +
                ", invalidItemsCount=" + invalidItemsCount +
                ", errorsCount=" + validationErrors.size() +
                ", warningsCount=" + warnings.size() +
                '}';
    }
}