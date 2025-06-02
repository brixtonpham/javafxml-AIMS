package com.aims.core.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "DELIVERY_INFO")
public class DeliveryInfo {

    @Id
    @Column(name = "deliveryInfoID", length = 50) // Có thể dùng @GeneratedValue nếu muốn tự tăng
    private String deliveryInfoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false) // optional = false vì DeliveryInfo luôn thuộc về 1 Order
    @JoinColumn(name = "orderID", referencedColumnName = "orderID", nullable = false, unique = true)
    private OrderEntity orderEntity;

    @Column(name = "recipientName", nullable = false, length = 255)
    private String recipientName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phoneNumber", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "deliveryProvinceCity", nullable = false, length = 100)
    private String deliveryProvinceCity;

    @Column(name = "deliveryAddress", nullable = false, length = 500)
    private String deliveryAddress;

    @Lob
    @Column(name = "deliveryInstructions")
    private String deliveryInstructions;

    @Column(name = "deliveryMethodChosen", length = 100)
    private String deliveryMethodChosen; // e.g., "STANDARD", "RUSH_DELIVERY"

    @Column(name = "requestedRushDeliveryTime")
    private LocalDateTime requestedRushDeliveryTime; // Nullable if not rush delivery

    public DeliveryInfo() {
    }

    public DeliveryInfo(String deliveryInfoId, OrderEntity orderEntity, String recipientName, String email, String phoneNumber, String deliveryProvinceCity, String deliveryAddress, String deliveryInstructions, String deliveryMethodChosen, LocalDateTime requestedRushDeliveryTime) {
        this.deliveryInfoId = deliveryInfoId;
        this.orderEntity = orderEntity;
        this.recipientName = recipientName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.deliveryProvinceCity = deliveryProvinceCity;
        this.deliveryAddress = deliveryAddress;
        this.deliveryInstructions = deliveryInstructions;
        this.deliveryMethodChosen = deliveryMethodChosen;
        this.requestedRushDeliveryTime = requestedRushDeliveryTime;
    }

    // Getters and Setters
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

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

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

    public String getDeliveryProvinceCity() {
        return deliveryProvinceCity;
    }

    public void setDeliveryProvinceCity(String deliveryProvinceCity) {
        this.deliveryProvinceCity = deliveryProvinceCity;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryInfo that = (DeliveryInfo) o;
        return Objects.equals(deliveryInfoId, that.deliveryInfoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveryInfoId);
    }

    @Override
    public String toString() {
        return "DeliveryInfo{" +
               "deliveryInfoId='" + deliveryInfoId + '\'' +
               ", orderId=" + (orderEntity != null ? orderEntity.getOrderId() : "null") +
               ", recipientName='" + recipientName + '\'' +
               ", deliveryAddress='" + deliveryAddress + '\'' +
               '}';
    }
}