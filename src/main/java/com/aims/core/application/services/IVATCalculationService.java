package com.aims.core.application.services;

import com.aims.core.entities.OrderItem;

import java.util.List;

/**
 * Service interface for VAT calculation operations.
 * Implements 10% VAT calculations per problem statement lines 22-23.
 * Handles display vs storage price conversions and delivery fee calculations.
 */
public interface IVATCalculationService {

    /**
     * Calculates VAT-inclusive price for display purposes.
     * Per problem statement lines 22-23: 10% VAT rate.
     *
     * @param basePrice The base price excluding VAT
     * @return The price including 10% VAT
     */
    float calculateVATInclusivePrice(float basePrice);

    /**
     * Extracts the base price from a VAT-inclusive price.
     * Used when storing prices that were entered with VAT included.
     *
     * @param vatInclusivePrice The price including VAT
     * @return The base price excluding VAT
     */
    float extractBasePrice(float vatInclusivePrice);

    /**
     * Calculates comprehensive order pricing with VAT breakdown.
     * Provides detailed pricing information for invoices and order summaries.
     *
     * @param items List of order items
     * @return OrderPriceBreakdown with detailed VAT calculations
     */
    OrderPriceBreakdown calculateOrderPricing(List<OrderItem> items);

    /**
     * Determines if an item type is VAT exempt.
     * Per business rules: Delivery fees are VAT exempt.
     *
     * @param itemType The type of item to check
     * @return true if VAT exempt, false otherwise
     */
    boolean isVATExempt(ItemType itemType);

    /**
     * Calculates the VAT amount for a given base price.
     *
     * @param basePrice The base price excluding VAT
     * @return The VAT amount (10% of base price)
     */
    float calculateVATAmount(float basePrice);

    /**
     * Calculates order pricing including delivery fees with proper VAT handling.
     * Delivery fees are VAT exempt per business rules.
     *
     * @param items List of order items
     * @param deliveryFee The delivery fee (VAT exempt)
     * @return OrderPriceBreakdown with delivery fees properly handled
     */
    OrderPriceBreakdown calculateOrderPricingWithDelivery(List<OrderItem> items, float deliveryFee);

    /**
     * Validates VAT calculations for accuracy.
     * Ensures calculations are accurate to 2 decimal places per requirements.
     *
     * @param basePrice The base price
     * @param vatInclusivePrice The calculated VAT-inclusive price
     * @return true if calculation is accurate within tolerance
     */
    boolean validateVATCalculation(float basePrice, float vatInclusivePrice);

    /**
     * Rounds a price to 2 decimal places for VAT calculations.
     *
     * @param price The price to round
     * @return The price rounded to 2 decimal places
     */
    float roundToTwoDecimals(float price);

    /**
     * Enum for item types to determine VAT exemption.
     */
    public enum ItemType {
        PRODUCT,
        DELIVERY_FEE,
        SERVICE_FEE,
        DISCOUNT
    }

    /**
     * Comprehensive pricing breakdown for orders including VAT details.
     */
    public static class OrderPriceBreakdown {
        private final float subtotalExclVAT;
        private final float totalVATAmount;
        private final float subtotalInclVAT;
        private final float deliveryFee;
        private final float totalAmount;
        private final List<ItemPriceBreakdown> itemBreakdowns;

        public OrderPriceBreakdown(float subtotalExclVAT, float totalVATAmount, float subtotalInclVAT,
                                 float deliveryFee, float totalAmount, List<ItemPriceBreakdown> itemBreakdowns) {
            this.subtotalExclVAT = subtotalExclVAT;
            this.totalVATAmount = totalVATAmount;
            this.subtotalInclVAT = subtotalInclVAT;
            this.deliveryFee = deliveryFee;
            this.totalAmount = totalAmount;
            this.itemBreakdowns = itemBreakdowns;
        }

        // Getters
        public float getSubtotalExclVAT() { return subtotalExclVAT; }
        public float getTotalVATAmount() { return totalVATAmount; }
        public float getSubtotalInclVAT() { return subtotalInclVAT; }
        public float getDeliveryFee() { return deliveryFee; }
        public float getTotalAmount() { return totalAmount; }
        public List<ItemPriceBreakdown> getItemBreakdowns() { return itemBreakdowns; }
    }

    /**
     * Price breakdown for individual items.
     */
    public static class ItemPriceBreakdown {
        private final String productId;
        private final String productTitle;
        private final int quantity;
        private final float unitPriceExclVAT;
        private final float unitVATAmount;
        private final float unitPriceInclVAT;
        private final float totalPriceExclVAT;
        private final float totalVATAmount;
        private final float totalPriceInclVAT;

        public ItemPriceBreakdown(String productId, String productTitle, int quantity,
                                float unitPriceExclVAT, float unitVATAmount, float unitPriceInclVAT,
                                float totalPriceExclVAT, float totalVATAmount, float totalPriceInclVAT) {
            this.productId = productId;
            this.productTitle = productTitle;
            this.quantity = quantity;
            this.unitPriceExclVAT = unitPriceExclVAT;
            this.unitVATAmount = unitVATAmount;
            this.unitPriceInclVAT = unitPriceInclVAT;
            this.totalPriceExclVAT = totalPriceExclVAT;
            this.totalVATAmount = totalVATAmount;
            this.totalPriceInclVAT = totalPriceInclVAT;
        }

        // Getters
        public String getProductId() { return productId; }
        public String getProductTitle() { return productTitle; }
        public int getQuantity() { return quantity; }
        public float getUnitPriceExclVAT() { return unitPriceExclVAT; }
        public float getUnitVATAmount() { return unitVATAmount; }
        public float getUnitPriceInclVAT() { return unitPriceInclVAT; }
        public float getTotalPriceExclVAT() { return totalPriceExclVAT; }
        public float getTotalVATAmount() { return totalVATAmount; }
        public float getTotalPriceInclVAT() { return totalPriceInclVAT; }
    }
}