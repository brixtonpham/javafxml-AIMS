package com.aims.core.enums;

/**
 * Enumeration representing the status of a payment transaction.
 * Used to track payment processing states throughout the payment lifecycle.
 */
public enum PaymentStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded"),
    PARTIALLY_REFUNDED("Partially Refunded"),
    EXPIRED("Expired"),
    AUTHORIZED("Authorized"),
    CAPTURED("Captured"),
    VOIDED("Voided");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if the payment status indicates a successful payment
     */
    public boolean isSuccessful() {
        return this == COMPLETED || this == CAPTURED;
    }

    /**
     * Check if the payment status indicates a failed payment
     */
    public boolean isFailed() {
        return this == FAILED || this == CANCELLED || this == EXPIRED || this == VOIDED;
    }

    /**
     * Check if the payment status indicates a pending payment
     */
    public boolean isPending() {
        return this == PENDING || this == PROCESSING || this == AUTHORIZED;
    }

    /**
     * Check if the payment can be refunded
     */
    public boolean canBeRefunded() {
        return this == COMPLETED || this == CAPTURED || this == PARTIALLY_REFUNDED;
    }
}
