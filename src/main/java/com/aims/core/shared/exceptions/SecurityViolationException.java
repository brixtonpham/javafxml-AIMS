package com.aims.core.shared.exceptions;

/**
 * Exception thrown when security violations are detected.
 * This includes unauthorized access attempts, invalid credentials, privilege escalation, etc.
 */
public class SecurityViolationException extends Exception {
    
    private final String violationType;
    private final String userId;
    private final String resource;

    public SecurityViolationException(String message) {
        super(message);
        this.violationType = "UNKNOWN";
        this.userId = null;
        this.resource = null;
    }

    public SecurityViolationException(String message, Throwable cause) {
        super(message, cause);
        this.violationType = "UNKNOWN";
        this.userId = null;
        this.resource = null;
    }

    public SecurityViolationException(String message, String violationType, String userId, String resource) {
        super(message);
        this.violationType = violationType;
        this.userId = userId;
        this.resource = resource;
    }

    public SecurityViolationException(String message, String violationType, String userId, String resource, Throwable cause) {
        super(message, cause);
        this.violationType = violationType;
        this.userId = userId;
        this.resource = resource;
    }

    public String getViolationType() {
        return violationType;
    }

    public String getUserId() {
        return userId;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public String toString() {
        return String.format("SecurityViolationException{type='%s', user='%s', resource='%s', message='%s'}", 
                           violationType, userId, resource, getMessage());
    }
}
