package com.aims.core.entities;

import com.aims.core.enums.OrderStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "ORDER_ENTITY") // ORDER là từ khóa trong SQL, nên đổi tên bảng
public class OrderEntity {

    @Id
    @Column(name = "orderID", length = 50)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID", referencedColumnName = "userID", nullable = true) // Cho phép NULL cho guest
    private UserAccount userAccount;

    @Column(name = "orderDate", nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Column(name = "totalProductPriceExclVAT")
    private float totalProductPriceExclVAT;

    @Column(name = "totalProductPriceInclVAT")
    private float totalProductPriceInclVAT;

    @Column(name = "calculatedDeliveryFee")
    private float calculatedDeliveryFee;

    @Column(name = "totalAmountPaid")
    private float totalAmountPaid;

    @OneToMany(mappedBy = "orderEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    // Một Order có một DeliveryInfo
    @OneToOne(mappedBy = "orderEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private DeliveryInfo deliveryInfo;

    // Một Order có một Invoice
    @OneToOne(mappedBy = "orderEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Invoice invoice;
    
    // Một Order có thể có nhiều PaymentTransaction (ví dụ: thanh toán, hoàn tiền)
    @OneToMany(mappedBy = "orderEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentTransaction> paymentTransactions;

    public OrderEntity() {
    }

    public OrderEntity(String orderId, UserAccount userAccount, LocalDateTime orderDate, OrderStatus orderStatus, float totalProductPriceExclVAT, float totalProductPriceInclVAT, float calculatedDeliveryFee, float totalAmountPaid) {
        this.orderId = orderId;
        this.userAccount = userAccount;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.totalProductPriceExclVAT = totalProductPriceExclVAT;
        this.totalProductPriceInclVAT = totalProductPriceInclVAT;
        this.calculatedDeliveryFee = calculatedDeliveryFee;
        this.totalAmountPaid = totalAmountPaid;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public float getTotalProductPriceExclVAT() {
        return totalProductPriceExclVAT;
    }

    public void setTotalProductPriceExclVAT(float totalProductPriceExclVAT) {
        this.totalProductPriceExclVAT = totalProductPriceExclVAT;
    }

    public float getTotalProductPriceInclVAT() {
        return totalProductPriceInclVAT;
    }

    public void setTotalProductPriceInclVAT(float totalProductPriceInclVAT) {
        this.totalProductPriceInclVAT = totalProductPriceInclVAT;
    }

    public float getCalculatedDeliveryFee() {
        return calculatedDeliveryFee;
    }

    public void setCalculatedDeliveryFee(float calculatedDeliveryFee) {
        this.calculatedDeliveryFee = calculatedDeliveryFee;
    }

    public float getTotalAmountPaid() {
        return totalAmountPaid;
    }

    public void setTotalAmountPaid(float totalAmountPaid) {
        this.totalAmountPaid = totalAmountPaid;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
    
    public DeliveryInfo getDeliveryInfo() {
        return deliveryInfo;
    }

    public void setDeliveryInfo(DeliveryInfo deliveryInfo) {
        if (deliveryInfo != null) {
            deliveryInfo.setOrderEntity(this);
        }
        this.deliveryInfo = deliveryInfo;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        if (invoice != null) {
            invoice.setOrderEntity(this);
        }
        this.invoice = invoice;
    }
    
    public List<PaymentTransaction> getPaymentTransactions() {
        return paymentTransactions;
    }

    public void setPaymentTransactions(List<PaymentTransaction> paymentTransactions) {
        this.paymentTransactions = paymentTransactions;
    }

    // Helper methods
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrderEntity(this);
    }

    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrderEntity(null);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderEntity that = (OrderEntity) o;
        return Objects.equals(orderId, that.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return "OrderEntity{" +
               "orderId='" + orderId + '\'' +
               ", user=" + (userAccount != null ? userAccount.getUserId() : "guest") +
               ", orderDate=" + orderDate +
               ", orderStatus=" + orderStatus +
               ", totalAmountPaid=" + totalAmountPaid +
               '}';
    }
}