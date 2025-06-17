package com.aims.core.application.dtos;

/**
 * User Summary DTO for order display
 * 
 * Contains essential user information for order summaries.
 */
public record UserSummaryDTO(
    String userId,
    String fullName,
    String email,
    boolean isGuest
) {}