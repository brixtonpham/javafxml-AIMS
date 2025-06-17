package com.aims.core.application.dtos.validation;

/**
 * Enumeration representing the severity levels of validation issues
 */
public enum ValidationSeverity {
    /**
     * Information only - no action required
     */
    INFO,
    
    /**
     * Warning - can proceed but user should be aware
     */
    WARNING,
    
    /**
     * Error - cannot proceed, requires user action to fix
     */
    ERROR,
    
    /**
     * Critical system error - requires technical intervention
     */
    CRITICAL;
    
    /**
     * Determines if this severity level blocks processing
     * 
     * @return true if ERROR or CRITICAL, false otherwise
     */
    public boolean isBlocking() {
        return this == ERROR || this == CRITICAL;
    }
    
    /**
     * Determines if this severity level requires immediate attention
     * 
     * @return true if CRITICAL, false otherwise
     */
    public boolean isCritical() {
        return this == CRITICAL;
    }
    
    /**
     * Gets the display priority for sorting (higher = more important)
     * 
     * @return Priority value for display ordering
     */
    public int getDisplayPriority() {
        switch (this) {
            case CRITICAL: return 4;
            case ERROR: return 3;
            case WARNING: return 2;
            case INFO: return 1;
            default: return 0;
        }
    }
}