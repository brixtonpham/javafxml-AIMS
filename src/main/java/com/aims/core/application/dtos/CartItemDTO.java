package com.aims.core.application.dtos;

public class CartItemDTO {
    private String productId;
    private String title;
    private int quantity;
    private float unitPriceExclVAT; // Price of one unit, excluding VAT
    private float totalPriceExclVAT; // quantity * unitPriceExclVAT
    private String imageUrl;
    private int availableStock;
    private boolean stockSufficient; // True if quantity <= availableStock

    public CartItemDTO() {
    }

    public CartItemDTO(String productId, String title, int quantity, float unitPriceExclVAT, String imageUrl, int availableStock) {
        this.productId = productId;
        this.title = title;
        this.quantity = quantity;
        this.unitPriceExclVAT = unitPriceExclVAT;
        this.imageUrl = imageUrl;
        this.availableStock = availableStock;
        this.stockSufficient = quantity <= availableStock;
        this.totalPriceExclVAT = unitPriceExclVAT * quantity;
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public float getUnitPriceExclVAT() { return unitPriceExclVAT; }
    public void setUnitPriceExclVAT(float unitPriceExclVAT) { this.unitPriceExclVAT = unitPriceExclVAT; }

    public float getTotalPriceExclVAT() { return totalPriceExclVAT; }
    public void setTotalPriceExclVAT(float totalPriceExclVAT) { this.totalPriceExclVAT = totalPriceExclVAT; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getAvailableStock() { return availableStock; }
    public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }

    public boolean isStockSufficient() { return stockSufficient; }
    public void setStockSufficient(boolean stockSufficient) { this.stockSufficient = stockSufficient; }
}