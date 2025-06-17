package com.aims.core.application.dtos;

import java.util.List;
import java.time.LocalDateTime;

/**
 * Enhanced Order Summary DTO with complete order information
 *
 * Provides comprehensive order data for UI consumption with all required fields
 * properly populated and validated.
 */
public record OrderSummaryDTO(
    String orderId,
    List<OrderItemDTO> items,
    float totalProductPriceExclVAT,
    float totalProductPriceInclVAT,
    float deliveryFee,
    float totalAmountToBePaid,
    DeliveryInfoDTO deliveryInfo,
    RushDeliveryDetailsDTO rushDeliveryDetails, // Nullable
    
    // Enhanced fields for complete order information
    String orderStatus,
    LocalDateTime orderDate,
    UserSummaryDTO customer, // Customer information
    PaymentSummaryDTO paymentSummary, // Payment method and status
    float vatAmount, // Calculated VAT amount
    boolean hasRushDelivery,
    boolean isPaid,
    String specialInstructions // Combined delivery and order instructions
) {
    // Validation constructor
    public OrderSummaryDTO {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (items == null) {
            items = List.of(); // Provide empty list as fallback
        }
        if (totalProductPriceExclVAT < 0) {
            throw new IllegalArgumentException("Total product price excluding VAT cannot be negative");
        }
        if (totalProductPriceInclVAT < 0) {
            throw new IllegalArgumentException("Total product price including VAT cannot be negative");
        }
        if (deliveryFee < 0) {
            throw new IllegalArgumentException("Delivery fee cannot be negative");
        }
        if (totalAmountToBePaid < 0) {
            throw new IllegalArgumentException("Total amount to be paid cannot be negative");
        }
        
        // Calculate VAT amount if not provided
        if (vatAmount == 0 && totalProductPriceExclVAT > 0) {
            vatAmount = totalProductPriceInclVAT - totalProductPriceExclVAT;
        }
    }
    
    /**
     * Creates a basic OrderSummaryDTO with essential fields only
     */
    public static OrderSummaryDTO createBasic(String orderId, List<OrderItemDTO> items,
                                            float totalExclVAT, float totalInclVAT,
                                            float deliveryFee, float totalAmount,
                                            DeliveryInfoDTO deliveryInfo) {
        return new OrderSummaryDTO(
            orderId, items, totalExclVAT, totalInclVAT, deliveryFee, totalAmount,
            deliveryInfo, null, "UNKNOWN", LocalDateTime.now(), null, null,
            totalInclVAT - totalExclVAT, false, false, null
        );
    }
}