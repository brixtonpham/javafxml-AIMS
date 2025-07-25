package com.aims.core.application.services;

import com.aims.core.entities.OrderEntity; // Or a DTO representing order items and delivery info
import com.aims.core.entities.DeliveryInfo; // Or a DTO
import com.aims.core.shared.exceptions.ValidationException;
// import com.aims.core.dtos.DeliveryCalculationRequestDTO; // Could be useful

import java.sql.SQLException; // Though likely not used directly if no DAO interaction

/**
 * Service interface for calculating delivery fees based on various factors
 * including product dimensions, weight, delivery location, and rush order options.
 */
public interface IDeliveryCalculationService {

    /**
     * Calculates the shipping fee for a given order or a set of items and delivery information.
     * This method considers standard shipping rules, rush order options, and free shipping eligibility.
     *
     * @param order The OrderEntity containing items and delivery destination.
     * Alternatively, specific DTOs for items and delivery details could be used.
     * @param isRushOrder A boolean indicating if rush delivery is requested for eligible items.
     * @return The total calculated shipping fee for the order.
     * @throws ValidationException If essential information for calculation is missing or invalid.
     */
    float calculateShippingFee(OrderEntity order, boolean isRushOrder) throws ValidationException;

    /**
     * Checks if an order qualifies for free standard shipping.
     * Does not apply to rush order items.
     *
     * @param totalValueNonRushItems The total value of items in the order that are not part of a rush delivery.
     * @return The amount of shipping fee discount applicable (e.g., up to 25,000 VND).
     */
    float getFreeShippingDiscount(float totalValueNonRushItems);

    /**
     * Determines if a specific delivery address is eligible for rush order service.
     * Currently, only inner city districts of Hanoi.
     *
     * @param deliveryInfo The delivery information containing the address and province/city.
     * @return true if the address is eligible for rush delivery, false otherwise.
     */
    boolean isRushDeliveryAddressEligible(DeliveryInfo deliveryInfo);

    /**
     * Calculates the shipping fee based on the future requirement of using
     * actual weight vs. dimensional weight.
     *
     * @param actualWeightKg The actual weight of the package in kilograms.
     * @param lengthCm The length of the package in centimeters.
     * @param widthCm The width of the package in centimeters.
     * @param heightCm The height of the package in centimeters.
     * @param deliveryProvinceCity The province or city for delivery (e.g., "Hanoi", "Ho Chi Minh City", "Other").
     * @param isInnerCityHanoiOrHCM Boolean indicating if the address is in inner city Hanoi or HCM.
     * @return The calculated shipping fee.
     * @throws ValidationException If dimensions or weight are invalid.
     */
    float calculateFeeWithDimensionalWeight(
            float actualWeightKg,
            float lengthCm,
            float widthCm,
            float heightCm,
            String deliveryProvinceCity,
            boolean isInnerCityHanoiOrHCM) throws ValidationException;

    /**
     * Enhanced method to calculate delivery fee breakdown for detailed reporting.
     * Provides separate calculation of base fee, regional adjustments, and rush charges.
     *
     * @param order The OrderEntity containing items and delivery destination.
     * @param isRushOrder A boolean indicating if rush delivery is requested.
     * @return DeliveryFeeBreakdown object containing detailed fee components.
     * @throws ValidationException If essential information is missing or invalid.
     */
    default DeliveryFeeBreakdown calculateDeliveryFeeBreakdown(OrderEntity order, boolean isRushOrder) throws ValidationException {
        float totalFee = calculateShippingFee(order, isRushOrder);
        return new DeliveryFeeBreakdown(totalFee, 0f, 0f, 0f, 0f);
    }

    /**
     * Enhanced method to get region-specific delivery time estimates.
     *
     * @param deliveryInfo The delivery information containing address details.
     * @param isRushOrder Whether this is a rush order.
     * @return Estimated delivery days based on region and order type.
     */
    default int getEstimatedDeliveryDays(DeliveryInfo deliveryInfo, boolean isRushOrder) {
        if (isRushOrder && isRushDeliveryAddressEligible(deliveryInfo)) {
            return 1; // Rush delivery: next day
        }
        
        if (deliveryInfo != null && deliveryInfo.getDeliveryProvinceCity() != null) {
            String province = deliveryInfo.getDeliveryProvinceCity().toLowerCase();
            if (province.contains("hanoi") || province.contains("ho chi minh")) {
                return 2; // Major cities: 2 days
            }
        }
        return 3; // Other provinces: 3 days
    }

    /**
     * Data class for detailed delivery fee breakdown
     */
    class DeliveryFeeBreakdown {
        private final float totalFee;
        private final float baseFee;
        private final float regionalAdjustment;
        private final float rushSurcharge;
        private final float freeShippingDiscount;

        public DeliveryFeeBreakdown(float totalFee, float baseFee, float regionalAdjustment,
                                   float rushSurcharge, float freeShippingDiscount) {
            this.totalFee = totalFee;
            this.baseFee = baseFee;
            this.regionalAdjustment = regionalAdjustment;
            this.rushSurcharge = rushSurcharge;
            this.freeShippingDiscount = freeShippingDiscount;
        }

        public float getTotalFee() { return totalFee; }
        public float getBaseFee() { return baseFee; }
        public float getRegionalAdjustment() { return regionalAdjustment; }
        public float getRushSurcharge() { return rushSurcharge; }
        public float getFreeShippingDiscount() { return freeShippingDiscount; }
    }
}