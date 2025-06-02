package com.aims.core.application.dtos;

// Using record
public record ChangePasswordDTO(
    String userId, // Can be implicit if taken from authenticated user session
    String oldPassword, // Plain text
    String newPassword  // Plain text
) {}