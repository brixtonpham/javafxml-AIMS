package com.aims.core.application.dtos;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Rush Delivery Details DTO
 * 
 * Contains information about rush delivery options and eligible items.
 */
public record RushDeliveryDetailsDTO(
    List<OrderItemDTO> rushItems, // Items specifically eligible for rush delivery
    float rushDeliverySubFee, // Additional fee for rush delivery
    String rushDeliveryInstructions, // Special instructions for rush delivery
    LocalDateTime rushDeliveryTime, // Requested rush delivery time
    boolean rushDeliveryAvailable, // Whether rush delivery is available for this order
    String rushDeliveryStatus // Status of rush delivery (REQUESTED, CONFIRMED, etc.)
) {
    // Validation constructor
    public RushDeliveryDetailsDTO {
        if (rushItems == null) {
            rushItems = List.of();
        }
        if (rushDeliverySubFee < 0) {
            rushDeliverySubFee = 0;
        }
        if (rushDeliveryStatus == null) {
            rushDeliveryStatus = "UNKNOWN";
        }
    }
}