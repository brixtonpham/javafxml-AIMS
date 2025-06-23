package com.aims.core.shared.exceptions;

/**
 * Exception thrown when audit-related operations fail.
 * This includes audit log failures, audit trail corruption, audit service unavailability, etc.
 */
public class AuditException extends Exception {
    
    private final String auditOperation;
    private final String entityId;

    public AuditException(String message) {
        super(message);
        this.auditOperation = "UNKNOWN";
        this.entityId = null;
    }

    public AuditException(String message, Throwable cause) {
        super(message, cause);
        this.auditOperation = "UNKNOWN";
        this.entityId = null;
    }

    public AuditException(String message, String auditOperation, String entityId) {
        super(message);
        this.auditOperation = auditOperation;
        this.entityId = entityId;
    }

    public AuditException(String message, String auditOperation, String entityId, Throwable cause) {
        super(message, cause);
        this.auditOperation = auditOperation;
        this.entityId = entityId;
    }

    public String getAuditOperation() {
        return auditOperation;
    }

    public String getEntityId() {
        return entityId;
    }

    @Override
    public String toString() {
        return String.format("AuditException{operation='%s', entityId='%s', message='%s'}", 
                           auditOperation, entityId, getMessage());
    }
}
