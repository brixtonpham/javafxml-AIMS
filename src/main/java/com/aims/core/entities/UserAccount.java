package com.aims.core.entities;

import com.aims.core.enums.UserStatus;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "USER_ACCOUNT")
public class UserAccount {

    @Id
    @Column(name = "userID", length = 50)
    private String userId;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 20)
    private UserStatus userStatus;

    // Relationship to UserRoleAssignment (one user can have many role assignments)
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<UserRoleAssignment> roleAssignments = new HashSet<>();

    // Relationship to Cart (one user can have one active cart, or many historical - simplified to one for now)
    // If a user can have only one cart, this can be @OneToOne. If many, then @OneToMany
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Cart> carts;


    // Relationship to OrderEntity (one user can place many orders)
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderEntity> orders;

    // Relationship to PaymentMethod (one user can save many payment methods)
    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentMethod> paymentMethods;


    public UserAccount() {
    }

    public UserAccount(String userId, String username, String passwordHash, String email, UserStatus userStatus) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.userStatus = userStatus;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public Set<UserRoleAssignment> getRoleAssignments() {
        return roleAssignments;
    }

    public void setRoleAssignments(Set<UserRoleAssignment> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public List<Cart> getCarts() {
        return carts;
    }

    public void setCarts(List<Cart> carts) {
        this.carts = carts;
    }

    public List<OrderEntity> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderEntity> orders) {
        this.orders = orders;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    // Helper methods to manage roles easily
    public void addRole(Role role) {
        UserRoleAssignment assignment = new UserRoleAssignment(this, role);
        this.roleAssignments.add(assignment);
        // role.getUserAssignments().add(assignment); // If bidirectional needed in Role
    }

    public void removeRole(Role role) {
        UserRoleAssignment toRemove = null;
        for (UserRoleAssignment assignment : this.roleAssignments) {
            if (assignment.getRole().equals(role) && assignment.getUserAccount().equals(this)) {
                toRemove = assignment;
                break;
            }
        }
        if (toRemove != null) {
            this.roleAssignments.remove(toRemove);
            // toRemove.getRole().getUserAssignments().remove(toRemove); // If bidirectional
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAccount that = (UserAccount) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "UserAccount{" +
               "userId='" + userId + '\'' +
               ", username='" + username + '\'' +
               ", email='" + email + '\'' +
               ", userStatus=" + userStatus +
               '}';
    }
}