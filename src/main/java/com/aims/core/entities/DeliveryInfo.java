package com.aims.core.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DELIVERY_INFO")
public class DeliveryInfo {
    
    @Id
    @Column(name = "deliveryInfoID", length = 50)
    private String deliveryInfoId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderID", referencedColumnName = "orderID")
    private OrderEntity orderEntity;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "phoneNumber", nullable = false)
    private String phoneNumber;
    
    @Column(name = "streetAddress", nullable = false)
    private String streetAddress;
    
    @Column(name = "district")
    private String district;
    
    @Column(name = "city", nullable = false)
    private String city;
    
    @Column(name = "postalCode")
    private String postalCode;
    
    @Column(name = "deliveryAddress", nullable = false)
    private String deliveryAddress;
    
    @Column(name = "deliveryProvinceCity", nullable = false)
    private String deliveryProvinceCity;
    
    @Column(name = "recipientName", nullable = false)
    private String recipientName;
    
    @Column(name = "recipientPhone", nullable = false)
    private String recipientPhone;
    
    @Column(name = "deliveryInstructions")
    private String deliveryInstructions;
    
    @Column(name = "deliveryMethodChosen", nullable = false)
    private String deliveryMethodChosen;
    
    @Column(name = "requestedRushDeliveryTime")
    private LocalDateTime requestedRushDeliveryTime;
    
    @Column(name = "rushDelivery")
    private boolean rushDelivery;

    public DeliveryInfo() {
    }

    // Core delivery info getters and setters
    public String getDeliveryInfoId() {
        return deliveryInfoId;
    }

    public void setDeliveryInfoId(String deliveryInfoId) {
        this.deliveryInfoId = deliveryInfoId;
    }

    public OrderEntity getOrderEntity() {
        return orderEntity;
    }

    public void setOrderEntity(OrderEntity orderEntity) {
        this.orderEntity = orderEntity;
    }

    // Contact info getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // Address components getters and setters
    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    // Legacy address fields getters and setters
    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
        // Also update the street address for new format compatibility
        this.streetAddress = deliveryAddress;
    }

    public String getDeliveryProvinceCity() {
        return deliveryProvinceCity;
    }

    public void setDeliveryProvinceCity(String deliveryProvinceCity) {
        this.deliveryProvinceCity = deliveryProvinceCity;
        // Also update the city for new format compatibility
        this.city = deliveryProvinceCity;
    }

    // Recipient info getters and setters
    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    // Delivery method and instructions getters and setters
    public String getDeliveryInstructions() {
        return deliveryInstructions;
    }

    public void setDeliveryInstructions(String deliveryInstructions) {
        this.deliveryInstructions = deliveryInstructions;
    }

    public String getDeliveryMethodChosen() {
        return deliveryMethodChosen;
    }

    public void setDeliveryMethodChosen(String deliveryMethodChosen) {
        this.deliveryMethodChosen = deliveryMethodChosen;
    }

    public LocalDateTime getRequestedRushDeliveryTime() {
        return requestedRushDeliveryTime;
    }

    public void setRequestedRushDeliveryTime(LocalDateTime requestedRushDeliveryTime) {
        this.requestedRushDeliveryTime = requestedRushDeliveryTime;
    }

    public boolean isRushDelivery() {
        return rushDelivery;
    }

    public void setRushDelivery(boolean rushDelivery) {
        this.rushDelivery = rushDelivery;
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (streetAddress != null) {
            sb.append(streetAddress);
        } else if (deliveryAddress != null) {
            sb.append(deliveryAddress);
        }
        
        if (district != null && !district.trim().isEmpty()) {
            sb.append(", ").append(district);
        }
        
        String cityOrProvince = city != null ? city : deliveryProvinceCity;
        if (cityOrProvince != null && !cityOrProvince.trim().isEmpty()) {
            sb.append(", ").append(cityOrProvince);
        }
        
        if (postalCode != null && !postalCode.trim().isEmpty()) {
            sb.append(" ").append(postalCode);
        }
        return sb.toString();
    }
}