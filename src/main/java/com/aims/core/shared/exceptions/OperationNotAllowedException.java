package com.aims.core.shared.exceptions;

public class OperationNotAllowedException extends Exception {
    public OperationNotAllowedException(String message) {
        super(message);
    }

    public OperationNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}