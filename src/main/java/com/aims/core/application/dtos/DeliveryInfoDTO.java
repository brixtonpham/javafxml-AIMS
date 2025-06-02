package com.aims.core.application.dtos;

import java.time.LocalDateTime;

public class DeliveryInfoDTO {
    private String recipientName;
    private String email;
    private String phoneNumber;
    private String deliveryProvinceCity;
    private String deliveryAddress;
    private String deliveryInstructions; // Optional
    private boolean rushOrder; // To indicate if customer requests rush delivery
    private LocalDateTime requestedRushDeliveryTime; // Optional, only if rushOrder is true

    public DeliveryInfoDTO() {
    }

    // Getters and Setters
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDeliveryProvinceCity() { return deliveryProvinceCity; }
    public void setDeliveryProvinceCity(String deliveryProvinceCity) { this.deliveryProvinceCity = deliveryProvinceCity; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getDeliveryInstructions() { return deliveryInstructions; }
    public void setDeliveryInstructions(String deliveryInstructions) { this.deliveryInstructions = deliveryInstructions; }

    public boolean isRushOrder() { return rushOrder; }
    public void setRushOrder(boolean rushOrder) { this.rushOrder = rushOrder; }

    public LocalDateTime getRequestedRushDeliveryTime() { return requestedRushDeliveryTime; }
    public void setRequestedRushDeliveryTime(LocalDateTime requestedRushDeliveryTime) { this.requestedRushDeliveryTime = requestedRushDeliveryTime; }
}