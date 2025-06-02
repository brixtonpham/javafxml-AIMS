package com.aims.core.application.dtos;

import java.util.List;

// Using record for immutability and conciseness
public record OrderSummaryDTO(
    String orderId,
    List<OrderItemDTO> items, // Will define OrderItemDTO next
    float totalProductPriceExclVAT,
    float totalProductPriceInclVAT,
    float deliveryFee,
    float totalAmountToBePaid,
    DeliveryInfoDTO deliveryInfo,
    RushDeliveryDetailsDTO rushDeliveryDetails // Nullable
) {}

// Define OrderItemDTO if different from CartItemDTO, or reuse CartItemDTO
// For simplicity, let's assume OrderItemDTO is similar to CartItemDTO for now
// but specifically for ordered items.
// You might want to add priceAtTimeOfOrder here if it's fixed.

// Define RushDeliveryDetailsDTO
record RushDeliveryDetailsDTO(
    List<OrderItemDTO> rushItems, // Items specifically for rush delivery
    float rushDeliverySubFee, // Fee specifically for the rush items
    String rushDeliveryInstructions,
    java.time.LocalDateTime rushDeliveryTime
) {}