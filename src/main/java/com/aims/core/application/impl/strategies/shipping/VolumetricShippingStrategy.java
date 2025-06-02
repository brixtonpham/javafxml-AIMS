package com.aims.core.application.impl.strategies.shipping;

import com.aims.core.application.services.strategies.IShippingFeeStrategy;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.shared.exceptions.ValidationException;

import java.util.List;

public class VolumetricShippingStrategy implements IShippingFeeStrategy {

    private static final float HANOI_HCM_BASE_FEE_3KG = 22000f;
    private static final float OTHER_PLACES_BASE_FEE_0_5KG = 30000f;
    private static final float ADDITIONAL_FEE_PER_0_5KG = 2500f;
    private static final int DIMENSIONAL_FACTOR = 6000; // cm^3/kg

    @Override
    public float calculateFee(List<OrderItem> orderItems, DeliveryInfo deliveryInfo) throws ValidationException {
        if (orderItems == null || orderItems.isEmpty() || deliveryInfo == null) {
            throw new ValidationException("Order items and delivery information are required for volumetric shipping calculation.");
        }
         if (deliveryInfo.getDeliveryProvinceCity() == null || deliveryInfo.getDeliveryProvinceCity().trim().isEmpty()) {
            throw new ValidationException("Delivery province/city is required.");
        }

        float totalChargeableWeightKg = 0f;

        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            if (product == null) {
                throw new ValidationException("Product details missing for an order item.");
            }
            if (product.getDimensionsCm() == null || product.getDimensionsCm().trim().isEmpty()) {
                // Fallback to actual weight if dimensions are not available, or throw exception
                // For this example, let's assume dimensions are required for this strategy or have defaults.
                throw new ValidationException("Product dimensions are required for volumetric shipping for product: " + product.getTitle());
            }

            float actualItemWeightKg = product.getWeightKg();
            float dimensionalItemWeightKg = calculateDimensionalWeightForItem(product.getDimensionsCm());

            float chargeableItemWeightKg = Math.max(actualItemWeightKg, dimensionalItemWeightKg);
            totalChargeableWeightKg += chargeableItemWeightKg * item.getQuantity();
        }

        if (totalChargeableWeightKg <= 0) return 0f;

        float shippingFee;
        String province = deliveryInfo.getDeliveryProvinceCity().trim().toLowerCase();
        boolean isInnerCityHanoiOrHCM = province.contains("hanoi") || province.contains("ho chi minh");

        if (isInnerCityHanoiOrHCM) {
            shippingFee = HANOI_HCM_BASE_FEE_3KG;
            if (totalChargeableWeightKg > 3f) {
                float additionalWeight = totalChargeableWeightKg - 3f;
                shippingFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        } else {
            shippingFee = OTHER_PLACES_BASE_FEE_0_5KG; // For the first 0.5kg
            if (totalChargeableWeightKg > 0.5f) {
                float additionalWeight = totalChargeableWeightKg - 0.5f;
                shippingFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        }
        System.out.println("Volumetric Shipping: Chargeable Weight: " + totalChargeableWeightKg + "kg, Province: " + province + ", Fee: " + shippingFee);
        return shippingFee;
    }

    private float calculateDimensionalWeightForItem(String dimensionsCm) throws ValidationException {
        if (dimensionsCm == null || dimensionsCm.trim().isEmpty()) {
            return 0f; // Or handle as an error
        }
        // Assuming dimensions are "LxWxH" e.g., "30x20x10"
        String[] dims = dimensionsCm.split("[xX]");
        if (dims.length != 3) {
            throw new ValidationException("Invalid product dimensions format: " + dimensionsCm + ". Expected LxWxH.");
        }
        try {
            float length = Float.parseFloat(dims[0].trim());
            float width = Float.parseFloat(dims[1].trim());
            float height = Float.parseFloat(dims[2].trim());
            if (length <= 0 || width <= 0 || height <= 0) {
                throw new ValidationException("Product dimensions must be positive values.");
            }
            return (length * width * height) / DIMENSIONAL_FACTOR;
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid number in product dimensions: " + dimensionsCm, e);
        }
    }
}