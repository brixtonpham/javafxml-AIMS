package com.aims.core.entities.pks;

import java.io.Serializable;
import java.util.Objects;

public class OrderItemId implements Serializable {
    private String orderEntity; // Tên trường phải khớp với tên trường entity OrderEntity trong OrderItem
    private String product;    // Tên trường phải khớp với tên trường entity Product trong OrderItem

    public OrderItemId() {
    }

    public OrderItemId(String orderId, String productId) {
        this.orderEntity = orderId;
        this.product = productId;
    }

    // Getters, Setters, equals, hashCode
    public String getOrderEntity() {
        return orderEntity;
    }

    public void setOrderEntity(String orderEntity) {
        this.orderEntity = orderEntity;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItemId that = (OrderItemId) o;
        return Objects.equals(orderEntity, that.orderEntity) &&
               Objects.equals(product, that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderEntity, product);
    }
}