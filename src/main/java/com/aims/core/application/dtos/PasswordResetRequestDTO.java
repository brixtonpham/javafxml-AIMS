package com.aims.core.application.dtos;

// Using record
public record PasswordResetRequestDTO(
    String userIdToReset,
    String newPlainTextPassword // Admin sets this new password directly
) {}