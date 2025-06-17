package com.aims.core.application.dtos;

import java.time.LocalDateTime;

/**
 * Payment Summary DTO for order display
 * 
 * Contains essential payment information for order summaries.
 */
public record PaymentSummaryDTO(
    String paymentMethodType, // CREDIT_CARD, DOMESTIC_CARD, etc.
    String paymentStatus, // PENDING, SUCCESS, FAILED, etc.
    String lastFourDigits, // Last 4 digits of card (if applicable)
    LocalDateTime paymentDate,
    String transactionId,
    boolean refundAvailable
) {}