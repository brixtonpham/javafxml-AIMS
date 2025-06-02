package com.aims.core.entities;

import com.aims.core.enums.TransactionType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "PAYMENT_TRANSACTION")
public class PaymentTransaction {

    @Id
    @Column(name = "transactionID", length = 50)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderID", referencedColumnName = "orderID", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paymentMethodID", referencedColumnName = "paymentMethodID") // Can be null if payment failed before method association
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "transactionType", nullable = false, length = 20)
    private TransactionType transactionType; // e.g., PAYMENT, REFUND

    @Column(name = "externalTransactionID", length = 100) // ID from VNPay or other gateway
    private String externalTransactionId;

    @Column(name = "transaction_status", nullable = false, length = 50)
    private String transactionStatus; // e.g., "SUCCESS", "FAILED", "PENDING"

    @Column(name = "transactionDateTime", nullable = false)
    private LocalDateTime transactionDateTime;

    @Column(name = "amount", nullable = false)
    private float amount;

    @Lob
    @Column(name = "transactionContent")
    private String transactionContent; // e.g., Details or message from payment gateway

    public PaymentTransaction() {
    }

    public PaymentTransaction(String transactionId, OrderEntity order, PaymentMethod paymentMethod, TransactionType transactionType, String externalTransactionId, String transactionStatus, LocalDateTime transactionDateTime, float amount, String transactionContent) {
        this.transactionId = transactionId;
        this.order = order;
        this.paymentMethod = paymentMethod;
        this.transactionType = transactionType;
        this.externalTransactionId = externalTransactionId;
        this.transactionStatus = transactionStatus;
        this.transactionDateTime = transactionDateTime;
        this.amount = amount;
        this.transactionContent = transactionContent;
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public void setExternalTransactionId(String externalTransactionId) {
        this.externalTransactionId = externalTransactionId;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public LocalDateTime getTransactionDateTime() {
        return transactionDateTime;
    }

    public void setTransactionDateTime(LocalDateTime transactionDateTime) {
        this.transactionDateTime = transactionDateTime;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public String getTransactionContent() {
        return transactionContent;
    }

    public void setTransactionContent(String transactionContent) {
        this.transactionContent = transactionContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentTransaction that = (PaymentTransaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "PaymentTransaction{" +
               "transactionId='" + transactionId + '\'' +
               ", orderId=" + (order != null ? order.getOrderId() : "null") +
               ", type=" + transactionType +
               ", status='" + transactionStatus + '\'' +
               ", amount=" + amount +
               '}';
    }
}