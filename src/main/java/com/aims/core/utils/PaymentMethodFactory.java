package com.aims.core.utils;

import com.aims.core.enums.PaymentMethodType;
import java.util.UUID;

/**
 * Utility class for creating and managing payment method identifiers
 */
public class PaymentMethodFactory {

    /**
     * Create a VNPay payment method ID based on the payment type
     */
    public static String createVNPayPaymentMethodId(String paymentType) {
        if (paymentType == null) {
            return generateDefaultVNPayId();
        }
        
        switch (paymentType.toUpperCase()) {
            case "CREDIT_CARD":
            case "VNPAY_CREDIT_CARD":
                return "VNPAY_CC_" + generateUniqueId();
                
            case "DOMESTIC_DEBIT_CARD":
            case "VNPAY_DOMESTIC_CARD":
                return "VNPAY_DC_" + generateUniqueId();
                
            case "VNPAY_WALLET":
                return "VNPAY_WALLET_" + generateUniqueId();
                
            case "VNPAY_BANK_TRANSFER":
                return "VNPAY_BANK_" + generateUniqueId();
                
            default:
                return generateDefaultVNPayId();
        }
    }
    
    /**
     * Convert payment type string to PaymentMethodType enum
     */
    public static PaymentMethodType convertToPaymentMethodType(String paymentType) {
        if (paymentType == null) {
            return PaymentMethodType.CREDIT_CARD; // Default
        }
        
        switch (paymentType.toUpperCase()) {
            case "CREDIT_CARD":
            case "VNPAY_CREDIT_CARD":
                return PaymentMethodType.CREDIT_CARD;
                
            case "DOMESTIC_DEBIT_CARD":
            case "VNPAY_DOMESTIC_CARD":
                return PaymentMethodType.DOMESTIC_DEBIT_CARD;
                
            default:
                return PaymentMethodType.CREDIT_CARD;
        }
    }
    
    /**
     * Check if a payment method ID is for VNPay
     */
    public static boolean isVNPayPaymentMethod(String paymentMethodId) {
        return paymentMethodId != null && paymentMethodId.toUpperCase().startsWith("VNPAY");
    }
    
    /**
     * Extract payment type from VNPay payment method ID
     */
    public static String extractPaymentTypeFromVNPayId(String vnpayPaymentMethodId) {
        if (vnpayPaymentMethodId == null || !isVNPayPaymentMethod(vnpayPaymentMethodId)) {
            return "CREDIT_CARD"; // Default
        }
        
        if (vnpayPaymentMethodId.contains("_CC_")) {
            return "VNPAY_CREDIT_CARD";
        } else if (vnpayPaymentMethodId.contains("_DC_")) {
            return "VNPAY_DOMESTIC_CARD";
        } else if (vnpayPaymentMethodId.contains("_WALLET_")) {
            return "VNPAY_WALLET";
        } else if (vnpayPaymentMethodId.contains("_BANK_")) {
            return "VNPAY_BANK_TRANSFER";
        }
        
        return "VNPAY_CREDIT_CARD"; // Default
    }
    
    /**
     * Generate a unique identifier for payment methods
     */
    private static String generateUniqueId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Generate a default VNPay payment method ID
     */
    private static String generateDefaultVNPayId() {
        return "VNPAY_DEFAULT_" + generateUniqueId();
    }
    
    /**
     * Create a temporary payment method ID for guest users
     */
    public static String createTemporaryPaymentMethodId(String paymentType) {
        String baseId = createVNPayPaymentMethodId(paymentType);
        return "TEMP_" + baseId;
    }
    
    /**
     * Validate if a payment method ID format is correct
     */
    public static boolean isValidPaymentMethodId(String paymentMethodId) {
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            return false;
        }
        
        // Basic validation - should contain alphanumeric characters and underscores
        return paymentMethodId.matches("^[A-Za-z0-9_-]+$") && paymentMethodId.length() >= 5;
    }
}