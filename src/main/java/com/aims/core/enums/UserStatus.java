package com.aims.core.enums;

public enum UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_ACTIVATION,
    DELETED;

    // Optional: Add methods if needed, for example, to get a status from a string
    public static UserStatus fromString(String statusStr) {
        for (UserStatus status : UserStatus.values()) {
            if (status.name().equalsIgnoreCase(statusStr)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No constant with text " + statusStr + " found");
    }
}
