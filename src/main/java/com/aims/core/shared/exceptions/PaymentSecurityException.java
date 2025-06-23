package com.aims.core.shared.exceptions;

/**
 * Exception thrown when payment security violations are detected.
 * This includes fraud detection, invalid payment methods, security token issues, etc.
 */
public class PaymentSecurityException extends Exception {
    
    private final String securityViolationType;
    private final String paymentId;

    public PaymentSecurityException(String message) {
        super(message);
        this.securityViolationType = "UNKNOWN";
        this.paymentId = null;
    }

    public PaymentSecurityException(String message, Throwable cause) {
        super(message, cause);
        this.securityViolationType = "UNKNOWN";
        this.paymentId = null;
    }

    public PaymentSecurityException(String message, String securityViolationType, String paymentId) {
        super(message);
        this.securityViolationType = securityViolationType;
        this.paymentId = paymentId;
    }

    public PaymentSecurityException(String message, String securityViolationType, String paymentId, Throwable cause) {
        super(message, cause);
        this.securityViolationType = securityViolationType;
        this.paymentId = paymentId;
    }

    public String getSecurityViolationType() {
        return securityViolationType;
    }

    public String getPaymentId() {
        return paymentId;
    }

    @Override
    public String toString() {
        return String.format("PaymentSecurityException{type='%s', paymentId='%s', message='%s'}", 
                           securityViolationType, paymentId, getMessage());
    }
}
