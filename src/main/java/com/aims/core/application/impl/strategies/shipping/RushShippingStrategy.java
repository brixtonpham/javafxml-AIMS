package com.aims.core.application.impl.strategies.shipping;

import com.aims.core.application.services.strategies.IShippingFeeStrategy;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import com.aims.core.shared.exceptions.ValidationException;

import java.util.List;

public class RushShippingStrategy implements IShippingFeeStrategy {

    private static final float RUSH_DELIVERY_SURCHARGE_PER_ITEM = 10000f;
    private final IShippingFeeStrategy standardShippingStrategy; // Composition: uses standard calculation as base

    public RushShippingStrategy(IShippingFeeStrategy standardShippingStrategy) {
        this.standardShippingStrategy = standardShippingStrategy;
    }

    @Override
    public float calculateFee(List<OrderItem> orderItems, DeliveryInfo deliveryInfo) throws ValidationException {
        if (orderItems == null || orderItems.isEmpty() || deliveryInfo == null) {
            throw new ValidationException("Order items and delivery information are required for rush shipping calculation.");
        }
        if (deliveryInfo.getDeliveryProvinceCity() == null ||
            !deliveryInfo.getDeliveryProvinceCity().trim().toLowerCase().contains("hanoi")) {
            // Further checks for "only districts" would go here if more detail is available
            throw new ValidationException("Rush delivery is currently only available for inner city Hanoi districts.");
        }

        // Calculate standard shipping fee for these items first
        float baseShippingFee = standardShippingStrategy.calculateFee(orderItems, deliveryInfo);

        // Add rush surcharge per item
        // The problem statement says "10,000 VND per rush order delivery item".
        // So, it's not per *quantity* of an item, but per *type* of item in the rush group.
        // However, if an order item has quantity > 1, it is usually considered one "item" for such surcharges.
        // Let's assume it's per OrderItem line that is designated for rush.
        // If the requirement meant "per individual physical unit", the logic would be different.
        // The problem states "customers will be charged an additional 10,000 VND per rush order delivery item."
        // This implies it's per item *line* in the rush delivery group.
        float totalRushSurcharge = 0;
        for (OrderItem item : orderItems) {
             if (item.isEligibleForRushDelivery()) { // Assuming items passed here are already filtered for rush
                 totalRushSurcharge += RUSH_DELIVERY_SURCHARGE_PER_ITEM;
             }
        }
        // If only items eligible and *chosen* for rush are passed to this strategy, then simply:
        // totalRushSurcharge = orderItems.size() * RUSH_DELIVERY_SURCHARGE_PER_ITEM;


        float totalFee = baseShippingFee + totalRushSurcharge;
        System.out.println("Rush Shipping: Base Fee: " + baseShippingFee + ", Rush Surcharge: " + totalRushSurcharge + ", Total Fee: " + totalFee);
        return totalFee;
    }
}