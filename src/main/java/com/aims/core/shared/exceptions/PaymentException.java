package com.aims.core.shared.exceptions;

public class PaymentException extends Exception {
    private String gatewayErrorCode; // Optional: To store specific error code from payment gateway

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentException(String message, String gatewayErrorCode) {
        super(message);
        this.gatewayErrorCode = gatewayErrorCode;
    }

    public PaymentException(String message, String gatewayErrorCode, Throwable cause) {
        super(message, cause);
        this.gatewayErrorCode = gatewayErrorCode;
    }

    public String getGatewayErrorCode() {
        return gatewayErrorCode;
    }
}