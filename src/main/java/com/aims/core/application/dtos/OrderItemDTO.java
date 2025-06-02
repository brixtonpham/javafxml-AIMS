package com.aims.core.application.dtos;

public class OrderItemDTO {
    private String productId;
    private String title;
    private int quantity;
    private float priceAtTimeOfOrder; // Price when order was placed (excl. VAT)
    private float totalPriceAtTimeOfOrder; // quantity * priceAtTimeOfOrder
    private String imageUrl;
    private boolean eligibleForRushDelivery;

    public OrderItemDTO() {
    }

    public OrderItemDTO(String productId, String title, int quantity, float priceAtTimeOfOrder, String imageUrl, boolean eligibleForRushDelivery) {
        this.productId = productId;
        this.title = title;
        this.quantity = quantity;
        this.priceAtTimeOfOrder = priceAtTimeOfOrder;
        this.totalPriceAtTimeOfOrder = priceAtTimeOfOrder * quantity;
        this.imageUrl = imageUrl;
        this.eligibleForRushDelivery = eligibleForRushDelivery;
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public float getPriceAtTimeOfOrder() { return priceAtTimeOfOrder; }
    public void setPriceAtTimeOfOrder(float priceAtTimeOfOrder) { this.priceAtTimeOfOrder = priceAtTimeOfOrder; }

    public float getTotalPriceAtTimeOfOrder() { return totalPriceAtTimeOfOrder; }
    public void setTotalPriceAtTimeOfOrder(float totalPriceAtTimeOfOrder) { this.totalPriceAtTimeOfOrder = totalPriceAtTimeOfOrder; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isEligibleForRushDelivery() { return eligibleForRushDelivery; }
    public void setEligibleForRushDelivery(boolean eligibleForRushDelivery) { this.eligibleForRushDelivery = eligibleForRushDelivery; }
}