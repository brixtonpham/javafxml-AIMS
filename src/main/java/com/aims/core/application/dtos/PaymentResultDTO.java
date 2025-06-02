package com.aims.core.application.dtos;

import java.time.LocalDateTime;
import java.util.Map;

// Using record for immutability
public record PaymentResultDTO(
    String aimsTransactionId,
    String externalTransactionId, // VNPay's transaction ID
    String status, // e.g., "SUCCESS", "FAILED", "PENDING_GATEWAY_CALLBACK"
    String message,
    String paymentUrl, // If redirect is needed
    Float amount,
    LocalDateTime transactionDateTime,
    Map<String, String> gatewayResponseData // Raw response data from gateway if needed
) {}