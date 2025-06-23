package com.aims.core.application.impl;

import com.aims.core.application.services.IRushOrderService;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.shared.exceptions.ValidationException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of IRushOrderService for handling rush order validation and calculations.
 * Implements business rules for rush delivery per problem statement lines 93-129.
 */
public class RushOrderServiceImpl implements IRushOrderService {

    // Hanoi inner districts eligible for rush delivery
    private static final List<String> HANOI_INNER_DISTRICTS = Arrays.asList(
        "Ba Đình", "Hoàn Kiếm", "Tây Hồ", "Long Biên", "Cầu Giấy", 
        "Đống Đa", "Hai Bà Trưng", "Hoàng Mai", "Thanh Xuân", "Nam Từ Liêm",
        "Bắc Từ Liêm", "Hà Đông"
    );
    
    // Rush delivery constants
    private static final String HANOI_CITY = "Hà Nội";
    private static final float BASE_RUSH_FEE = 50000.0f; // 50,000 VND base fee
    private static final float RUSH_FEE_PER_KG = 15000.0f; // 15,000 VND per kg
    private static final int RUSH_DELIVERY_HOURS = 3; // 3-hour delivery
    private static final String CUTOFF_TIME = "18:00"; // Orders after 6 PM go to next day

    @Override
    public RushOrderEligibilityResult validateRushOrderEligibility(DeliveryInfo deliveryInfo) 
            throws ValidationException {
        
        if (deliveryInfo == null) {
            throw new ValidationException("Delivery information cannot be null for rush order validation");
        }

        // Check if delivery is in Hanoi
        String city = deliveryInfo.getCity();
        if (city == null) {
            city = deliveryInfo.getDeliveryProvinceCity();
        }
        
        if (city == null || !city.toLowerCase().contains("hà nội") && !city.toLowerCase().contains("hanoi")) {
            return new RushOrderEligibilityResult(
                false, 
                "Rush delivery is only available in Hanoi inner districts", 
                "CITY_NOT_ELIGIBLE",
                HANOI_INNER_DISTRICTS
            );
        }

        // Check if district is eligible
        String district = deliveryInfo.getDistrict();
        if (district == null || district.trim().isEmpty()) {
            return new RushOrderEligibilityResult(
                false, 
                "District information is required for rush delivery validation", 
                "MISSING_DISTRICT",
                HANOI_INNER_DISTRICTS
            );
        }

        boolean districtEligible = isDistrictEligibleForRushDelivery(district, city);
        
        if (!districtEligible) {
            return new RushOrderEligibilityResult(
                false, 
                String.format("District '%s' is not eligible for rush delivery. Available districts: %s", 
                            district, String.join(", ", HANOI_INNER_DISTRICTS)), 
                "DISTRICT_NOT_ELIGIBLE",
                HANOI_INNER_DISTRICTS
            );
        }

        return new RushOrderEligibilityResult(
            true, 
            "Address is eligible for rush delivery", 
            "ELIGIBLE",
            HANOI_INNER_DISTRICTS
        );
    }

    @Override
    public RushOrderEligibilityResult validateOrderItemsForRushDelivery(List<OrderItem> orderItems) 
            throws ValidationException {
        
        if (orderItems == null || orderItems.isEmpty()) {
            throw new ValidationException("Order items cannot be null or empty for rush delivery validation");
        }

        // Check each item for rush delivery eligibility
        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            if (product == null) {
                return new RushOrderEligibilityResult(
                    false, 
                    "Product information missing for rush delivery validation", 
                    "MISSING_PRODUCT_INFO",
                    HANOI_INNER_DISTRICTS
                );
            }

            // Check if product type supports rush delivery
            // For AIMS, assume all products support rush delivery unless specified otherwise
            // This can be extended later with product-specific rules
            if (!isProductEligibleForRushDelivery(product)) {
                return new RushOrderEligibilityResult(
                    false, 
                    String.format("Product '%s' is not eligible for rush delivery", product.getTitle()), 
                    "PRODUCT_NOT_ELIGIBLE",
                    HANOI_INNER_DISTRICTS
                );
            }
        }

        return new RushOrderEligibilityResult(
            true, 
            "All order items are eligible for rush delivery", 
            "ELIGIBLE",
            HANOI_INNER_DISTRICTS
        );
    }

    @Override
    public float calculateRushDeliveryFee(List<OrderItem> orderItems, DeliveryInfo deliveryInfo) 
            throws ValidationException, SQLException {
        
        if (orderItems == null || orderItems.isEmpty()) {
            throw new ValidationException("Order items cannot be null or empty for rush fee calculation");
        }

        // Calculate total weight of order items
        float totalWeight = 0.0f;
        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            if (product != null) {
                // Assume each product has a weight field, or use default weight based on type
                float itemWeight = getProductWeight(product);
                totalWeight += itemWeight * item.getQuantity();
            }
        }

        // Calculate rush delivery fee: base fee + weight-based fee
        float rushFee = BASE_RUSH_FEE + (totalWeight * RUSH_FEE_PER_KG);

        // Apply district-based multiplier if needed
        String district = deliveryInfo.getDistrict();
        float districtMultiplier = getDistrictMultiplier(district);
        rushFee *= districtMultiplier;

        // Round to nearest 1000 VND for convenience
        return Math.round(rushFee / 1000.0f) * 1000.0f;
    }

    @Override
    public RushOrderValidationResult validateCompleteRushOrder(List<OrderItem> orderItems, 
                                                              DeliveryInfo deliveryInfo) 
            throws ValidationException, SQLException {
        
        // Validate address eligibility
        RushOrderEligibilityResult addressEligibility = validateRushOrderEligibility(deliveryInfo);
        
        // Validate item eligibility
        RushOrderEligibilityResult itemEligibility = validateOrderItemsForRushDelivery(orderItems);
        
        // Calculate rush delivery fee
        float rushFee = 0.0f;
        RushDeliveryTimeEstimate timeEstimate = null;
        
        boolean overallValid = addressEligibility.isEligible() && itemEligibility.isEligible();
        String message;
        
        if (overallValid) {
            rushFee = calculateRushDeliveryFee(orderItems, deliveryInfo);
            timeEstimate = calculateRushDeliveryTime(deliveryInfo.getDistrict());
            message = "Rush order is valid and ready for processing";
        } else {
            if (!addressEligibility.isEligible()) {
                message = "Rush order validation failed: " + addressEligibility.getMessage();
            } else {
                message = "Rush order validation failed: " + itemEligibility.getMessage();
            }
        }

        return new RushOrderValidationResult(
            overallValid,
            message,
            addressEligibility,
            itemEligibility,
            rushFee,
            timeEstimate
        );
    }

    @Override
    public boolean isDistrictEligibleForRushDelivery(String district, String city) {
        if (district == null || city == null) {
            return false;
        }
        
        // Check if city is Hanoi
        if (!city.toLowerCase().contains("hà nội") && !city.toLowerCase().contains("hanoi")) {
            return false;
        }
        
        // Check if district is in the eligible list (case-insensitive)
        return HANOI_INNER_DISTRICTS.stream()
            .anyMatch(eligibleDistrict -> 
                eligibleDistrict.toLowerCase().equals(district.toLowerCase().trim()));
    }

    @Override
    public List<String> getEligibleRushDeliveryDistricts() {
        return HANOI_INNER_DISTRICTS;
    }

    @Override
    public RushDeliveryTimeEstimate calculateRushDeliveryTime(String district) {
        LocalTime currentTime = LocalTime.now();
        LocalTime cutoff = LocalTime.parse(CUTOFF_TIME);
        
        boolean availableToday = currentTime.isBefore(cutoff);
        
        String timeWindow;
        if (availableToday) {
            LocalTime estimatedDelivery = currentTime.plusHours(RUSH_DELIVERY_HOURS);
            timeWindow = String.format("%02d:%02d - %02d:%02d", 
                                     currentTime.getHour(), currentTime.getMinute(),
                                     estimatedDelivery.getHour(), estimatedDelivery.getMinute());
        } else {
            timeWindow = "09:00 - 12:00 (Next day)";
        }
        
        return new RushDeliveryTimeEstimate(
            RUSH_DELIVERY_HOURS,
            timeWindow,
            CUTOFF_TIME,
            availableToday
        );
    }

    /**
     * Helper method to determine if a product is eligible for rush delivery.
     * This can be extended with product-specific rules.
     */
    private boolean isProductEligibleForRushDelivery(Product product) {
        // For now, assume all products are eligible for rush delivery
        // This can be extended later to check product-specific attributes
        // e.g., fragile items, oversized items, etc.
        return true;
    }

    /**
     * Helper method to get product weight for shipping calculations.
     * Uses estimated weights based on product type.
     */
    private float getProductWeight(Product product) {
        // Use product type to estimate weight in kg
        String productType = product.getClass().getSimpleName();
        
        switch (productType) {
            case "Book":
                return 0.5f; // 500g average for books
            case "CD":
                return 0.1f; // 100g for CDs
            case "DVD":
                return 0.15f; // 150g for DVDs
            case "LP":
                return 0.3f; // 300g for vinyl records
            default:
                return 0.5f; // Default weight
        }
    }

    /**
     * Helper method to get district-based pricing multiplier.
     * Some districts may have higher delivery costs.
     */
    private float getDistrictMultiplier(String district) {
        if (district == null) {
            return 1.0f;
        }
        
        // Apply multipliers based on district accessibility
        switch (district.toLowerCase()) {
            case "tây hồ":
            case "long biên":
                return 1.2f; // 20% surcharge for outer districts
            case "hoàn kiếm":
            case "ba đình":
                return 0.9f; // 10% discount for central districts
            default:
                return 1.0f; // Standard pricing
        }
    }
}