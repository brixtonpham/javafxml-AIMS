package com.aims.core.application.impl; // Or com.aims.core.application.services.impl;

import com.aims.core.application.services.IDeliveryCalculationService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.shared.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DeliveryCalculationServiceImpl implements IDeliveryCalculationService {

    private static final float HANOI_HCM_BASE_FEE_FIRST_3KG = 22000f;
    private static final float OTHER_PLACES_BASE_FEE_FIRST_0_5KG = 30000f;
    private static final float ADDITIONAL_FEE_PER_0_5KG = 2500f;
    private static final float RUSH_DELIVERY_SURCHARGE_PER_ITEM = 10000f;
    private static final float FREE_SHIPPING_THRESHOLD_VALUE = 100000f;
    private static final float MAX_FREE_SHIPPING_DISCOUNT = 25000f;
    private static final int DIMENSIONAL_WEIGHT_FACTOR = 6000; // cm^3/kg

    // In a real application, these would be more robustly defined, possibly from a configuration or database.
    private static final List<String> HANOI_INNER_CITY_DISTRICTS = List.of(
            "hoan kiem", "ba dinh", "dong da", "hai ba trung", "cau giay",
            "thanh xuan", "tay ho", "hoang mai", "long bien" 
            // Add other relevant districts
    );
    // HCM inner city districts would be similar if needed for other rules, currently rush is Hanoi only.

    public DeliveryCalculationServiceImpl() {
        // Constructor
    }

    @Override
    public float calculateShippingFee(OrderEntity order, boolean isRushOrderRequested) throws ValidationException {
        if (order == null || order.getOrderItems() == null || order.getOrderItems().isEmpty() || order.getDeliveryInfo() == null) {
            throw new ValidationException("Order, order items, and delivery information are required for shipping calculation.");
        }

        DeliveryInfo deliveryInfo = order.getDeliveryInfo();
        List<OrderItem> allItems = order.getOrderItems();
        float totalShippingFee = 0f;

        List<OrderItem> standardDeliveryItems = new ArrayList<>();
        List<OrderItem> rushDeliveryItems = new ArrayList<>();

        boolean isRushAddressEligible = isRushDeliveryAddressEligible(deliveryInfo);

        if (isRushOrderRequested) {
            if (!isRushAddressEligible) {
                throw new ValidationException("Delivery address is not eligible for rush order. " +
                                            "Rush order is only available for inner city Hanoi districts.");
            }
            for (OrderItem item : allItems) {
                // Assuming OrderItem has a way to know if the product itself is eligible for rush.
                // For now, let's use the flag on OrderItem set during order initiation.
                if (item.isEligibleForRushDelivery()) { // This flag should be set based on product properties
                    rushDeliveryItems.add(item);
                } else {
                    standardDeliveryItems.add(item);
                }
            }
            if (rushDeliveryItems.isEmpty() && !allItems.isEmpty()) {
                 throw new ValidationException("Rush order requested, but no items in the order are eligible for rush delivery.");
            }
        } else {
            standardDeliveryItems.addAll(allItems);
        }

        // Calculate fee for standard delivery items
        if (!standardDeliveryItems.isEmpty()) {
            float standardItemsFee = calculateFeeForItemGroup(standardDeliveryItems, deliveryInfo, false);
            float standardItemsValueExclVAT = 0;
            for(OrderItem item : standardDeliveryItems) {
                standardItemsValueExclVAT += item.getPriceAtTimeOfOrder() * item.getQuantity();
            }
            float discount = getFreeShippingDiscount(standardItemsValueExclVAT);
            totalShippingFee += Math.max(0, standardItemsFee - discount);
        }

        // Calculate fee for rush delivery items
        if (!rushDeliveryItems.isEmpty()) {
            float rushItemsFee = calculateFeeForItemGroup(rushDeliveryItems, deliveryInfo, true);
            totalShippingFee += rushItemsFee;
        }

        return totalShippingFee;
    }

    private float calculateFeeForItemGroup(List<OrderItem> items, DeliveryInfo deliveryInfo, boolean isRushGroup) throws ValidationException {
        if (items.isEmpty()) {
            return 0f;
        }

        float totalWeightKg = 0f;
        for (OrderItem item : items) {
            Product product = item.getProduct();
            if (product == null) {
                throw new ValidationException("Product details missing for an order item during fee calculation.");
            }
            // Using actual weight for now. Volumetric logic would be integrated here if active.
            // For the future requirement, you'd call calculateFeeWithDimensionalWeight for each item's package
            // or for the combined package. The problem statement implies per-item calculation of chargeable weight.
            totalWeightKg += product.getWeightKg() * item.getQuantity();
        }

        if (totalWeightKg <= 0 && !items.isEmpty()) {
             // Or handle as free if it's a digital product with no weight, though problem says physical media
            System.err.println("Warning: Total weight for item group is zero or negative. Fee might be incorrect.");
            return 0f;
        }


        float baseFee;
        String province = deliveryInfo.getDeliveryProvinceCity().trim().toLowerCase();
        String address = deliveryInfo.getDeliveryAddress().trim().toLowerCase(); // For more specific district checks

        // Simplified check for Hanoi/HCM inner city. A more robust solution would use district lists.
        boolean isHanoiInner = province.contains("hanoi") && HANOI_INNER_CITY_DISTRICTS.stream().anyMatch(address::contains);
        boolean isHCMInner = province.contains("ho chi minh"); // Simplified, add district check if needed

        if (isHanoiInner || isHCMInner) {
            baseFee = HANOI_HCM_BASE_FEE_FIRST_3KG;
            if (totalWeightKg > 3f) {
                float additionalWeight = totalWeightKg - 3f;
                baseFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        } else {
            baseFee = OTHER_PLACES_BASE_FEE_FIRST_0_5KG; // For the first 0.5kg
            if (totalWeightKg > 0.5f) {
                float additionalWeight = totalWeightKg - 0.5f;
                baseFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        }

        if (isRushGroup) {
            // Add rush surcharge per item line in the rush group
            baseFee += items.size() * RUSH_DELIVERY_SURCHARGE_PER_ITEM;
        }
        return baseFee;
    }


    @Override
    public float getFreeShippingDiscount(float totalValueNonRushItemsExclVAT) {
        if (totalValueNonRushItemsExclVAT > FREE_SHIPPING_THRESHOLD_VALUE) {
            return MAX_FREE_SHIPPING_DISCOUNT; // Max discount is 25,000 VND
        }
        return 0f;
    }

    @Override
    public boolean isRushDeliveryAddressEligible(DeliveryInfo deliveryInfo) {
        if (deliveryInfo == null || deliveryInfo.getDeliveryProvinceCity() == null || deliveryInfo.getDeliveryAddress() == null) {
            return false;
        }
        String province = deliveryInfo.getDeliveryProvinceCity().trim().toLowerCase();
        String address = deliveryInfo.getDeliveryAddress().trim().toLowerCase();

        if (province.contains("hanoi")) {
            // Check against predefined list of inner city districts
            return HANOI_INNER_CITY_DISTRICTS.stream().anyMatch(district -> address.contains(district.toLowerCase()));
        }
        return false;
    }

    @Override
    public float calculateFeeWithDimensionalWeight(
            float actualWeightKg,
            float lengthCm,
            float widthCm,
            float heightCm,
            String deliveryProvinceCity,
            boolean isInnerCityHanoiOrHCM // This boolean simplifies location check for this specific method
    ) throws ValidationException {

        if (actualWeightKg <= 0 || lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) {
            throw new ValidationException("Actual weight and dimensions must be positive for dimensional weight calculation.");
        }
        if (deliveryProvinceCity == null || deliveryProvinceCity.trim().isEmpty()){
            throw new ValidationException("Delivery province/city is required.");
        }

        float dimensionalWeightKg = (lengthCm * widthCm * heightCm) / DIMENSIONAL_WEIGHT_FACTOR;
        float chargeableWeightKg = Math.max(actualWeightKg, dimensionalWeightKg);

        float shippingFee;
        if (isInnerCityHanoiOrHCM) {
            shippingFee = HANOI_HCM_BASE_FEE_FIRST_3KG;
            if (chargeableWeightKg > 3f) {
                float additionalWeight = chargeableWeightKg - 3f;
                shippingFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        } else {
            shippingFee = OTHER_PLACES_BASE_FEE_FIRST_0_5KG; // For the first 0.5kg
            if (chargeableWeightKg > 0.5f) {
                float additionalWeight = chargeableWeightKg - 0.5f;
                shippingFee += Math.ceil(additionalWeight / 0.5f) * ADDITIONAL_FEE_PER_0_5KG;
            }
        }
        return shippingFee;
    }
}