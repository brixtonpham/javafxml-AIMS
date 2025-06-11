package com.aims.core.utils;

import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for validating delivery information and order items
 * before shipping fee calculation attempts.
 */
public class DeliveryInfoValidator {
    
    /**
     * Result class for validation operations
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final List<String> errors;
        
        public ValidationResult(boolean isValid, List<String> errors) {
            this.isValid = isValid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
    
    /**
     * Validates delivery information and order items for shipping calculation.
     * 
     * @param info The delivery information to validate
     * @param items The list of order items to validate
     * @return ValidationResult containing validation status and any errors
     */
    public static ValidationResult validateForCalculation(DeliveryInfo info, List<OrderItem> items) {
        List<String> errors = new ArrayList<>();
        
        // Delivery info validation
        if (info == null) {
            errors.add("Delivery information is required");
            return new ValidationResult(false, errors);
        }
        
        // Address validation
        if (isNullOrEmpty(info.getDeliveryProvinceCity())) {
            errors.add("Province/City is required for shipping calculation");
        }
        if (isNullOrEmpty(info.getDeliveryAddress())) {
            errors.add("Delivery address is required for shipping calculation");
        }
        
        // Contact information validation
        if (isNullOrEmpty(info.getRecipientName())) {
            errors.add("Recipient name is required");
        }
        if (isNullOrEmpty(info.getPhoneNumber())) {
            errors.add("Phone number is required");
        }
        
        // Product validation
        if (items == null || items.isEmpty()) {
            errors.add("Order must contain items for shipping calculation");
        } else {
            for (int i = 0; i < items.size(); i++) {
                OrderItem item = items.get(i);
                if (item == null) {
                    errors.add("Invalid order item at position " + (i + 1));
                    continue;
                }
                
                if (item.getProduct() == null) {
                    errors.add("Product information missing for order item " + (i + 1));
                    continue;
                }
                
                if (item.getQuantity() <= 0) {
                    errors.add("Invalid quantity for product: " + item.getProduct().getTitle());
                }
                
                if (item.getProduct().getWeightKg() <= 0) {
                    errors.add("Product weight information missing for: " + item.getProduct().getTitle());
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validates basic delivery information fields for UI feedback.
     * Less strict than full calculation validation.
     * 
     * @param info The delivery information to validate
     * @return ValidationResult containing validation status and any errors
     */
    public static ValidationResult validateBasicFields(DeliveryInfo info) {
        List<String> errors = new ArrayList<>();
        
        if (info == null) {
            errors.add("Delivery information is required");
            return new ValidationResult(false, errors);
        }
        
        if (isNullOrEmpty(info.getRecipientName())) {
            errors.add("Recipient name is required");
        }
        
        if (isNullOrEmpty(info.getPhoneNumber())) {
            errors.add("Phone number is required");
        }
        
        if (isNullOrEmpty(info.getDeliveryProvinceCity())) {
            errors.add("Province/City is required");
        }
        
        if (isNullOrEmpty(info.getDeliveryAddress())) {
            errors.add("Delivery address is required");
        }
        
        // Optional: Email format validation
        if (!isNullOrEmpty(info.getEmail()) && !isValidEmail(info.getEmail())) {
            errors.add("Please enter a valid email address");
        }
        
        // Optional: Phone format validation
        if (!isNullOrEmpty(info.getPhoneNumber()) && !isValidPhoneNumber(info.getPhoneNumber())) {
            errors.add("Please enter a valid phone number");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validates rush delivery specific requirements.
     * 
     * @param info The delivery information to validate
     * @return ValidationResult containing validation status and any errors
     */
    public static ValidationResult validateRushDelivery(DeliveryInfo info) {
        List<String> errors = new ArrayList<>();
        
        if (info == null) {
            errors.add("Delivery information is required for rush delivery validation");
            return new ValidationResult(false, errors);
        }
        
        if (isNullOrEmpty(info.getDeliveryProvinceCity())) {
            errors.add("Province/City is required for rush delivery eligibility check");
        }
        
        if (isNullOrEmpty(info.getDeliveryAddress())) {
            errors.add("Detailed address is required for rush delivery eligibility check");
        }
        
        if ("RUSH_DELIVERY".equalsIgnoreCase(info.getDeliveryMethodChosen())) {
            if (info.getRequestedRushDeliveryTime() == null) {
                errors.add("Rush delivery time must be specified");
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Checks if a string is null or empty (including whitespace only).
     * 
     * @param str The string to check
     * @return true if the string is null or empty/whitespace only
     */
    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Basic email validation.
     * 
     * @param email The email to validate
     * @return true if the email format appears valid
     */
    private static boolean isValidEmail(String email) {
        if (isNullOrEmpty(email)) {
            return false;
        }
        // Simple regex for basic email validation
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
    
    /**
     * Basic phone number validation for Vietnamese numbers.
     * 
     * @param phone The phone number to validate
     * @return true if the phone number format appears valid
     */
    private static boolean isValidPhoneNumber(String phone) {
        if (isNullOrEmpty(phone)) {
            return false;
        }
        // Remove spaces, dashes, and parentheses
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        
        // Check for Vietnamese phone number patterns
        // Mobile: starts with +84 or 0, followed by 9 digits
        // Landline: starts with +84 or 0, followed by area code and 7-8 digits
        return cleanPhone.matches("^(\\+84|0)[0-9]{9,10}$");
    }
}