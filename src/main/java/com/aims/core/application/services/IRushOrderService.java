package com.aims.core.application.services;

import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import com.aims.core.shared.exceptions.ValidationException;
import java.sql.SQLException;
import java.util.List;

/**
 * Service interface for handling rush order validation and calculations.
 * Implements business rules for rush delivery per problem statement lines 93-129.
 */
public interface IRushOrderService {

    /**
     * Validates if a delivery address is eligible for rush delivery.
     * Per problem statement: Rush delivery only available in Hanoi inner districts.
     *
     * @param deliveryInfo The delivery information to validate
     * @return RushOrderEligibilityResult with validation outcome
     * @throws ValidationException If validation fails
     */
    RushOrderEligibilityResult validateRushOrderEligibility(DeliveryInfo deliveryInfo) 
            throws ValidationException;

    /**
     * Validates if order items are eligible for rush delivery.
     * Some products may have restrictions on rush delivery.
     *
     * @param orderItems List of order items to validate
     * @return RushOrderEligibilityResult with validation outcome
     * @throws ValidationException If validation fails
     */
    RushOrderEligibilityResult validateOrderItemsForRushDelivery(List<OrderItem> orderItems) 
            throws ValidationException;

    /**
     * Calculates the additional rush delivery fee based on order items.
     * Rush delivery has separate fee calculation from standard delivery.
     *
     * @param orderItems List of order items
     * @param deliveryInfo Delivery information
     * @return Rush delivery fee in VND
     * @throws ValidationException If calculation fails
     * @throws SQLException If database error occurs
     */
    float calculateRushDeliveryFee(List<OrderItem> orderItems, DeliveryInfo deliveryInfo) 
            throws ValidationException, SQLException;

    /**
     * Validates a complete rush order request.
     * Combines address eligibility, item eligibility, and fee calculation.
     *
     * @param orderItems List of order items
     * @param deliveryInfo Delivery information
     * @return RushOrderValidationResult with complete validation outcome
     * @throws ValidationException If validation fails
     * @throws SQLException If database error occurs
     */
    RushOrderValidationResult validateCompleteRushOrder(List<OrderItem> orderItems, 
                                                       DeliveryInfo deliveryInfo) 
            throws ValidationException, SQLException;

    /**
     * Checks if a specific district is eligible for rush delivery.
     * Helper method for address validation.
     *
     * @param district The district name to check
     * @param city The city name (should be Hanoi for rush delivery)
     * @return true if district is eligible for rush delivery
     */
    boolean isDistrictEligibleForRushDelivery(String district, String city);

    /**
     * Gets the list of Hanoi inner districts eligible for rush delivery.
     *
     * @return List of eligible district names
     */
    List<String> getEligibleRushDeliveryDistricts();

    /**
     * Calculates the estimated rush delivery time based on current time and district.
     *
     * @param district The delivery district
     * @return Estimated delivery time details
     */
    RushDeliveryTimeEstimate calculateRushDeliveryTime(String district);

    /**
     * Represents the result of rush order eligibility validation.
     */
    public static class RushOrderEligibilityResult {
        private final boolean eligible;
        private final String message;
        private final String reasonCode;
        private final List<String> eligibleDistricts;

        public RushOrderEligibilityResult(boolean eligible, String message, String reasonCode, 
                                        List<String> eligibleDistricts) {
            this.eligible = eligible;
            this.message = message;
            this.reasonCode = reasonCode;
            this.eligibleDistricts = eligibleDistricts;
        }

        // Getters
        public boolean isEligible() { return eligible; }
        public String getMessage() { return message; }
        public String getReasonCode() { return reasonCode; }
        public List<String> getEligibleDistricts() { return eligibleDistricts; }
    }

    /**
     * Represents the complete validation result for a rush order.
     */
    public static class RushOrderValidationResult {
        private final boolean valid;
        private final String message;
        private final RushOrderEligibilityResult addressEligibility;
        private final RushOrderEligibilityResult itemEligibility;
        private final float rushDeliveryFee;
        private final RushDeliveryTimeEstimate timeEstimate;

        public RushOrderValidationResult(boolean valid, String message,
                                       RushOrderEligibilityResult addressEligibility,
                                       RushOrderEligibilityResult itemEligibility,
                                       float rushDeliveryFee,
                                       RushDeliveryTimeEstimate timeEstimate) {
            this.valid = valid;
            this.message = message;
            this.addressEligibility = addressEligibility;
            this.itemEligibility = itemEligibility;
            this.rushDeliveryFee = rushDeliveryFee;
            this.timeEstimate = timeEstimate;
        }

        // Getters
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public RushOrderEligibilityResult getAddressEligibility() { return addressEligibility; }
        public RushOrderEligibilityResult getItemEligibility() { return itemEligibility; }
        public float getRushDeliveryFee() { return rushDeliveryFee; }
        public RushDeliveryTimeEstimate getTimeEstimate() { return timeEstimate; }
    }

    /**
     * Represents rush delivery time estimation details.
     */
    public static class RushDeliveryTimeEstimate {
        private final int estimatedHours;
        private final String timeWindow;
        private final String cutoffTime;
        private final boolean availableToday;

        public RushDeliveryTimeEstimate(int estimatedHours, String timeWindow, 
                                      String cutoffTime, boolean availableToday) {
            this.estimatedHours = estimatedHours;
            this.timeWindow = timeWindow;
            this.cutoffTime = cutoffTime;
            this.availableToday = availableToday;
        }

        // Getters
        public int getEstimatedHours() { return estimatedHours; }
        public String getTimeWindow() { return timeWindow; }
        public String getCutoffTime() { return cutoffTime; }
        public boolean isAvailableToday() { return availableToday; }
    }
}