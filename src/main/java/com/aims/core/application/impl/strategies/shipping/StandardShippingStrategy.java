package com.aims.core.application.impl.strategies.shipping;

import com.aims.core.application.services.strategies.IShippingFeeStrategy;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.shared.exceptions.ValidationException;

import java.util.List;

public class StandardShippingStrategy implements IShippingFeeStrategy {

    private static final float HANOI_HCM_BASE_FEE_3KG = 22000f;
    private static final float OTHER_PLACES_BASE_FEE_0_5KG = 30000f;
    private static final float ADDITIONAL_FEE_PER_0_5KG = 2500f;

    @Override
    public float calculateFee(List<OrderItem> orderItems, DeliveryInfo deliveryInfo) throws ValidationException {
        if (orderItems == null || orderItems.isEmpty() || deliveryInfo == null) {
            throw new ValidationException("Order items and delivery information are required for shipping calculation.");
        }
        if (deliveryInfo.getDeliveryProvinceCity() == null || deliveryInfo.getDeliveryProvinceCity().trim().isEmpty()) {
            throw new ValidationException("Delivery province/city is required.");
        }

        float totalWeightKg = 0f;
        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            if (product == null) {
                throw new ValidationException("Product details missing for an order item.");
            }
            totalWeightKg += product.getWeightKg() * item.getQuantity();
        }

        if (totalWeightKg <= 0) return 0f; // Or throw exception if weight must be positive

        float shippingFee;
        String province = deliveryInfo.getDeliveryProvinceCity().trim().toLowerCase();
        // This is a simplified check. A more robust solution would use a predefined list
        // or a more sophisticated address parsing for "inner city".
        boolean isInnerCityHanoiOrHCM = province.contains("hanoi") || province.contains("ho chi minh");


        if (isInnerCityHanoiOrHCM) {
            shippingFee = HANOI_HCM_BASE_FEE_3KG;
            if (totalWeightKg > 3f) {
                float additionalWeight = totalWeightKg - 3f;
                shippingFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        } else {
            shippingFee = OTHER_PLACES_BASE_FEE_0_5KG; // For the first 0.5kg
            if (totalWeightKg > 0.5f) {
                float additionalWeight = totalWeightKg - 0.5f;
                shippingFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        }
        System.out.println("Standard Shipping: Total Weight: " + totalWeightKg + "kg, Province: " + province + ", Fee: " + shippingFee);
        return shippingFee;
    }
}