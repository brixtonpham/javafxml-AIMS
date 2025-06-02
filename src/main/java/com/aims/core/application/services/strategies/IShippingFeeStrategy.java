package com.aims.core.application.services.strategies;

import com.aims.core.entities.OrderEntity; // Or a DTO/List of items with weight/dimensions
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.shared.exceptions.ValidationException;

import java.util.List; // If passing a list of items directly
import com.aims.core.entities.OrderItem; // If strategy operates on list of items


/**
 * Interface defining the strategy for calculating shipping fees.
 */
public interface IShippingFeeStrategy {

    /**
     * Calculates the shipping fee based on the specific strategy.
     *
     * @param orderItems A list of OrderItem objects in the shipment group (e.g., all standard items, or all rush items).
     * Each OrderItem should have access to its Product's weight and dimensions.
     * @param deliveryInfo The delivery information containing the destination address and province/city.
     * @return The calculated shipping fee for this group of items using this strategy.
     * @throws ValidationException If necessary information for calculation is missing or invalid (e.g., weight, dimensions, address).
     */
    float calculateFee(List<OrderItem> orderItems, DeliveryInfo deliveryInfo) throws ValidationException;

    /**
     * Checks if this shipping strategy is applicable for the given items and delivery information.
     * For example, RushShippingStrategy would check if the address is eligible and items support rush.
     *
     * @param orderItems The list of items to be shipped.
     * @param deliveryInfo The delivery information.
     * @return true if the strategy can be applied, false otherwise.
     */
    // boolean isApplicable(List<OrderItem> orderItems, DeliveryInfo deliveryInfo); // This might be better handled by the DeliveryCalculationService when selecting a strategy
}