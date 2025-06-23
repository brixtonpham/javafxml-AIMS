package com.aims.core.shared.exceptions;

/**
 * Exception thrown when data integrity violations are detected.
 * This includes database constraint violations, data corruption, referential integrity issues, etc.
 */
public class DataIntegrityException extends Exception {
    
    private final String integrityViolationType;
    private final String affectedEntity;

    public DataIntegrityException(String message) {
        super(message);
        this.integrityViolationType = "UNKNOWN";
        this.affectedEntity = null;
    }

    public DataIntegrityException(String message, Throwable cause) {
        super(message, cause);
        this.integrityViolationType = "UNKNOWN";
        this.affectedEntity = null;
    }

    public DataIntegrityException(String message, String integrityViolationType, String affectedEntity) {
        super(message);
        this.integrityViolationType = integrityViolationType;
        this.affectedEntity = affectedEntity;
    }

    public DataIntegrityException(String message, String integrityViolationType, String affectedEntity, Throwable cause) {
        super(message, cause);
        this.integrityViolationType = integrityViolationType;
        this.affectedEntity = affectedEntity;
    }

    public String getIntegrityViolationType() {
        return integrityViolationType;
    }

    public String getAffectedEntity() {
        return affectedEntity;
    }

    @Override
    public String toString() {
        return String.format("DataIntegrityException{type='%s', entity='%s', message='%s'}", 
                           integrityViolationType, affectedEntity, getMessage());
    }
}
