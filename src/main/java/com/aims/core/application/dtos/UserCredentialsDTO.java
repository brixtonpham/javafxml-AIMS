package com.aims.core.application.dtos;

// Using record
public record UserCredentialsDTO(
    String username,
    String password // Plain text password from user input
) {}