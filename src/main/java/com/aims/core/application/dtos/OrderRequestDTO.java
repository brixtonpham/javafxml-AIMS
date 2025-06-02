package com.aims.core.application.dtos;

public class OrderRequestDTO {
    private String cartSessionId;
    private String userId; // Nullable if guest checkout

    public OrderRequestDTO() {
    }

    public OrderRequestDTO(String cartSessionId, String userId) {
        this.cartSessionId = cartSessionId;
        this.userId = userId;
    }

    // Getters and Setters
    public String getCartSessionId() { return cartSessionId; }
    public void setCartSessionId(String cartSessionId) { this.cartSessionId = cartSessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}