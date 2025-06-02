package com.aims.core.entities;

import com.aims.core.entities.pks.CartItemId;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "CART_ITEM")
@IdClass(CartItemId.class) // Sử dụng IdClass cho khóa phức hợp
public class CartItem {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartSessionID", referencedColumnName = "cartSessionID")
    private Cart cart;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productID", referencedColumnName = "productID")
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public CartItem() {
    }

    public CartItem(Cart cart, Product product, int quantity) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
    }

    // Getters and Setters
    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return Objects.equals(cart, cartItem.cart) &&
               Objects.equals(product, cartItem.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cart, product);
    }

    @Override
    public String toString() {
        return "CartItem{" +
               "cartSessionId=" + (cart != null ? cart.getCartSessionId() : "null") +
               ", productId=" + (product != null ? product.getProductId() : "null") +
               ", quantity=" + quantity +
               '}';
    }
}