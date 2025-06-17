package com.aims.core.shared.exceptions;

public class OrderException extends Exception {
    private String orderErrorCode;

    public OrderException(String message) {
        super(message);
    }

    public OrderException(String message, String errorCode) {
        super(message);
        this.orderErrorCode = errorCode;
    }

    public OrderException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrderException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.orderErrorCode = errorCode;
    }

    public String getOrderErrorCode() {
        return orderErrorCode;
    }
}