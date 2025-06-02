package com.aims.core.application.dtos;

import java.util.List;

public class CartViewDTO {
    private String cartSessionId;
    private List<CartItemDTO> items;
    private float totalPriceExclVAT;
    private List<String> stockWarningMessages; // e.g., "Product X only has Y items in stock."

    public CartViewDTO() {
    }

    public CartViewDTO(String cartSessionId, List<CartItemDTO> items, float totalPriceExclVAT, List<String> stockWarningMessages) {
        this.cartSessionId = cartSessionId;
        this.items = items;
        this.totalPriceExclVAT = totalPriceExclVAT;
        this.stockWarningMessages = stockWarningMessages;
    }

    // Getters and Setters
    public String getCartSessionId() { return cartSessionId; }
    public void setCartSessionId(String cartSessionId) { this.cartSessionId = cartSessionId; }

    public List<CartItemDTO> getItems() { return items; }
    public void setItems(List<CartItemDTO> items) { this.items = items; }

    public float getTotalPriceExclVAT() { return totalPriceExclVAT; }
    public void setTotalPriceExclVAT(float totalPriceExclVAT) { this.totalPriceExclVAT = totalPriceExclVAT; }

    public List<String> getStockWarningMessages() { return stockWarningMessages; }
    public void setStockWarningMessages(List<String> stockWarningMessages) { this.stockWarningMessages = stockWarningMessages; }
}