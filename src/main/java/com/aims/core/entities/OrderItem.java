package com.aims.core.entities;

import com.aims.core.entities.pks.OrderItemId;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "ORDER_ITEM")
@IdClass(OrderItemId.class)
public class OrderItem {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderID", referencedColumnName = "orderID")
    private OrderEntity orderEntity;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productID", referencedColumnName = "productID")
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "priceAtTimeOfOrder", nullable = false)
    private float priceAtTimeOfOrder;

    @Column(name = "isEligibleForRushDelivery")
    private boolean eligibleForRushDelivery; // Sửa tên cho đúng chuẩn Java naming

    public OrderItem() {
    }

    public OrderItem(OrderEntity orderEntity, Product product, int quantity, float priceAtTimeOfOrder, boolean eligibleForRushDelivery) {
        this.orderEntity = orderEntity;
        this.product = product;
        this.quantity = quantity;
        this.priceAtTimeOfOrder = priceAtTimeOfOrder;
        this.eligibleForRushDelivery = eligibleForRushDelivery;
    }

    // Getters and Setters
    public OrderEntity getOrderEntity() {
        return orderEntity;
    }

    public void setOrderEntity(OrderEntity orderEntity) {
        this.orderEntity = orderEntity;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public float getPriceAtTimeOfOrder() {
        return priceAtTimeOfOrder;
    }

    public void setPriceAtTimeOfOrder(float priceAtTimeOfOrder) {
        this.priceAtTimeOfOrder = priceAtTimeOfOrder;
    }

    public boolean isEligibleForRushDelivery() {
        return eligibleForRushDelivery;
    }

    public void setEligibleForRushDelivery(boolean eligibleForRushDelivery) {
        this.eligibleForRushDelivery = eligibleForRushDelivery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(orderEntity, orderItem.orderEntity) &&
               Objects.equals(product, orderItem.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderEntity, product);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
               "orderId=" + (orderEntity != null ? orderEntity.getOrderId() : "null") +
               ", productId=" + (product != null ? product.getProductId() : "null") +
               ", quantity=" + quantity +
               ", priceAtTimeOfOrder=" + priceAtTimeOfOrder +
               '}';
    }
}