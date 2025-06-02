package com.aims.core.entities.pks;

import java.io.Serializable;
import java.util.Objects;

public class CartItemId implements Serializable {
    private String cart;    // Tên trường phải khớp với tên trường entity Cart trong CartItem
    private String product; // Tên trường phải khớp với tên trường entity Product trong CartItem

    public CartItemId() {
    }

    public CartItemId(String cartSessionId, String productId) {
        this.cart = cartSessionId;
        this.product = productId;
    }

    // Getters, Setters, equals, hashCode
    public String getCart() {
        return cart;
    }

    public void setCart(String cart) {
        this.cart = cart;
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
        CartItemId that = (CartItemId) o;
        return Objects.equals(cart, that.cart) &&
               Objects.equals(product, that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cart, product);
    }
}