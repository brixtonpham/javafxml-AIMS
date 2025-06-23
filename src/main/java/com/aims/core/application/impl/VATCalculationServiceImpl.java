package com.aims.core.application.impl;

import com.aims.core.application.services.IVATCalculationService;
import com.aims.core.entities.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of IVATCalculationService.
 * Handles 10% VAT calculations per problem statement lines 22-23.
 */
@Service
public class VATCalculationServiceImpl implements IVATCalculationService {

    // Business rule constants from problem statement lines 22-23
    private static final float VAT_RATE = 0.10f; // 10% VAT
    private static final float VAT_MULTIPLIER = 1.0f + VAT_RATE; // 1.10 for inclusive calculations
    private static final float CALCULATION_TOLERANCE = 0.01f; // 1 cent tolerance for validation

    @Override
    public float calculateVATInclusivePrice(float basePrice) {
        if (basePrice <= 0) {
            return 0;
        }
        
        float vatInclusivePrice = basePrice * VAT_MULTIPLIER;
        return roundToTwoDecimals(vatInclusivePrice);
    }

    @Override
    public float extractBasePrice(float vatInclusivePrice) {
        if (vatInclusivePrice <= 0) {
            return 0;
        }
        
        float basePrice = vatInclusivePrice / VAT_MULTIPLIER;
        return roundToTwoDecimals(basePrice);
    }

    @Override
    public OrderPriceBreakdown calculateOrderPricing(List<OrderItem> items) {
        return calculateOrderPricingWithDelivery(items, 0.0f);
    }

    @Override
    public boolean isVATExempt(ItemType itemType) {
        // Per business rules: Delivery fees are VAT exempt
        return itemType == ItemType.DELIVERY_FEE;
    }

    @Override
    public float calculateVATAmount(float basePrice) {
        if (basePrice <= 0) {
            return 0;
        }
        
        float vatAmount = basePrice * VAT_RATE;
        return roundToTwoDecimals(vatAmount);
    }

    @Override
    public OrderPriceBreakdown calculateOrderPricingWithDelivery(List<OrderItem> items, float deliveryFee) {
        List<ItemPriceBreakdown> itemBreakdowns = new ArrayList<>();
        float subtotalExclVAT = 0;
        float totalVATAmount = 0;

        // Calculate pricing for each item
        for (OrderItem item : items) {
            if (item.getProduct() == null) {
                continue; // Skip items without product data
            }

            String productId = item.getProduct().getId();
            String productTitle = item.getProduct().getTitle();
            int quantity = item.getQuantity();
            
            // Assuming stored prices are VAT-exclusive
            float unitPriceExclVAT = item.getProduct().getPrice();
            float unitVATAmount = calculateVATAmount(unitPriceExclVAT);
            float unitPriceInclVAT = calculateVATInclusivePrice(unitPriceExclVAT);
            
            float totalPriceExclVAT = roundToTwoDecimals(unitPriceExclVAT * quantity);
            float totalVATForItem = roundToTwoDecimals(unitVATAmount * quantity);
            float totalPriceInclVAT = roundToTwoDecimals(unitPriceInclVAT * quantity);

            ItemPriceBreakdown itemBreakdown = new ItemPriceBreakdown(
                productId, productTitle, quantity,
                unitPriceExclVAT, unitVATAmount, unitPriceInclVAT,
                totalPriceExclVAT, totalVATForItem, totalPriceInclVAT
            );

            itemBreakdowns.add(itemBreakdown);
            subtotalExclVAT += totalPriceExclVAT;
            totalVATAmount += totalVATForItem;
        }

        // Round subtotals
        subtotalExclVAT = roundToTwoDecimals(subtotalExclVAT);
        totalVATAmount = roundToTwoDecimals(totalVATAmount);
        float subtotalInclVAT = roundToTwoDecimals(subtotalExclVAT + totalVATAmount);
        
        // Delivery fee is VAT exempt, so add it directly to total
        float deliveryFeeRounded = roundToTwoDecimals(deliveryFee);
        float totalAmount = roundToTwoDecimals(subtotalInclVAT + deliveryFeeRounded);

        return new OrderPriceBreakdown(
            subtotalExclVAT, totalVATAmount, subtotalInclVAT,
            deliveryFeeRounded, totalAmount, itemBreakdowns
        );
    }

    @Override
    public boolean validateVATCalculation(float basePrice, float vatInclusivePrice) {
        if (basePrice <= 0) {
            return vatInclusivePrice == 0;
        }
        
        float expectedVATInclusivePrice = calculateVATInclusivePrice(basePrice);
        float difference = Math.abs(vatInclusivePrice - expectedVATInclusivePrice);
        
        return difference <= CALCULATION_TOLERANCE;
    }

    @Override
    public float roundToTwoDecimals(float price) {
        if (Float.isNaN(price) || Float.isInfinite(price)) {
            return 0;
        }
        
        BigDecimal bd = new BigDecimal(Float.toString(price));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    /**
     * Calculates VAT breakdown for a single price.
     * Useful for detailed tax reporting.
     *
     * @param basePrice The base price excluding VAT
     * @return VATBreakdown object with detailed calculations
     */
    public VATBreakdown calculateVATBreakdown(float basePrice) {
        float vatAmount = calculateVATAmount(basePrice);
        float totalPrice = calculateVATInclusivePrice(basePrice);
        
        return new VATBreakdown(basePrice, vatAmount, totalPrice, VAT_RATE);
    }

    /**
     * Validates an entire order's VAT calculations for accuracy.
     *
     * @param orderBreakdown The order price breakdown to validate
     * @return true if all calculations are accurate
     */
    public boolean validateOrderVATCalculations(OrderPriceBreakdown orderBreakdown) {
        // Validate subtotal calculations
        float expectedSubtotalInclVAT = orderBreakdown.getSubtotalExclVAT() + orderBreakdown.getTotalVATAmount();
        if (Math.abs(orderBreakdown.getSubtotalInclVAT() - expectedSubtotalInclVAT) > CALCULATION_TOLERANCE) {
            return false;
        }
        
        // Validate total amount
        float expectedTotalAmount = orderBreakdown.getSubtotalInclVAT() + orderBreakdown.getDeliveryFee();
        if (Math.abs(orderBreakdown.getTotalAmount() - expectedTotalAmount) > CALCULATION_TOLERANCE) {
            return false;
        }
        
        // Validate individual item calculations
        for (ItemPriceBreakdown item : orderBreakdown.getItemBreakdowns()) {
            if (!validateItemVATCalculations(item)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Validates VAT calculations for a single item.
     *
     * @param item The item breakdown to validate
     * @return true if calculations are accurate
     */
    private boolean validateItemVATCalculations(ItemPriceBreakdown item) {
        // Validate unit price calculations
        float expectedUnitVAT = calculateVATAmount(item.getUnitPriceExclVAT());
        if (Math.abs(item.getUnitVATAmount() - expectedUnitVAT) > CALCULATION_TOLERANCE) {
            return false;
        }
        
        float expectedUnitInclVAT = calculateVATInclusivePrice(item.getUnitPriceExclVAT());
        if (Math.abs(item.getUnitPriceInclVAT() - expectedUnitInclVAT) > CALCULATION_TOLERANCE) {
            return false;
        }
        
        // Validate total calculations
        float expectedTotalExclVAT = item.getUnitPriceExclVAT() * item.getQuantity();
        if (Math.abs(item.getTotalPriceExclVAT() - expectedTotalExclVAT) > CALCULATION_TOLERANCE) {
            return false;
        }
        
        float expectedTotalVAT = item.getUnitVATAmount() * item.getQuantity();
        if (Math.abs(item.getTotalVATAmount() - expectedTotalVAT) > CALCULATION_TOLERANCE) {
            return false;
        }
        
        return true;
    }

    /**
     * Helper class for detailed VAT breakdown.
     */
    public static class VATBreakdown {
        private final float basePrice;
        private final float vatAmount;
        private final float totalPrice;
        private final float vatRate;

        public VATBreakdown(float basePrice, float vatAmount, float totalPrice, float vatRate) {
            this.basePrice = basePrice;
            this.vatAmount = vatAmount;
            this.totalPrice = totalPrice;
            this.vatRate = vatRate;
        }

        // Getters
        public float getBasePrice() { return basePrice; }
        public float getVatAmount() { return vatAmount; }
        public float getTotalPrice() { return totalPrice; }
        public float getVatRate() { return vatRate; }
        public float getVatPercentage() { return vatRate * 100; }
    }
}