package com.aims.core.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "CART")
public class Cart {

    @Id
    @Column(name = "cartSessionID", length = 100)
    private String cartSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID", referencedColumnName = "userID", nullable = true) // Cho phép NULL cho guest cart
    private UserAccount userAccount;

    @Column(name = "lastUpdated")
    private LocalDateTime lastUpdated;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    public Cart() {
    }

    public Cart(String cartSessionId, UserAccount userAccount, LocalDateTime lastUpdated) {
        this.cartSessionId = cartSessionId;
        this.userAccount = userAccount;
        this.lastUpdated = lastUpdated;
    }

    // Getters and Setters
    public String getCartSessionId() {
        return cartSessionId;
    }

    public void setCartSessionId(String cartSessionId) {
        this.cartSessionId = cartSessionId;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    // Helper methods for managing cart items
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cart cart = (Cart) o;
        return Objects.equals(cartSessionId, cart.cartSessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartSessionId);
    }

    @Override
    public String toString() {
        return "Cart{" +
               "cartSessionId='" + cartSessionId + '\'' +
               ", user=" + (userAccount != null ? userAccount.getUserId() : "guest") +
               ", lastUpdated=" + lastUpdated +
               ", itemCount=" + (items != null ? items.size() : 0) +
               '}';
    }
}