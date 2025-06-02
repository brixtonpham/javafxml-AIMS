package com.aims.core.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "INVOICE")
public class Invoice {

    @Id
    @Column(name = "invoiceID", length = 50) // Có thể dùng @GeneratedValue
    private String invoiceId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orderID", referencedColumnName = "orderID", nullable = false, unique = true)
    private OrderEntity orderEntity;

    @Column(name = "invoiceDate", nullable = false)
    private LocalDateTime invoiceDate;

    @Column(name = "invoicedTotalAmount", nullable = false)
    private float invoicedTotalAmount;

    // Constructors
    public Invoice() {
    }

    public Invoice(String invoiceId, OrderEntity orderEntity, LocalDateTime invoiceDate, float invoicedTotalAmount) {
        this.invoiceId = invoiceId;
        this.orderEntity = orderEntity;
        this.invoiceDate = invoiceDate;
        this.invoicedTotalAmount = invoicedTotalAmount;
    }

    // Getters and Setters
    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public OrderEntity getOrderEntity() {
        return orderEntity;
    }

    public void setOrderEntity(OrderEntity orderEntity) {
        this.orderEntity = orderEntity;
    }

    public LocalDateTime getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDateTime invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public float getInvoicedTotalAmount() {
        return invoicedTotalAmount;
    }

    public void setInvoicedTotalAmount(float invoicedTotalAmount) {
        this.invoicedTotalAmount = invoicedTotalAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invoice invoice = (Invoice) o;
        return Objects.equals(invoiceId, invoice.invoiceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoiceId);
    }

    @Override
    public String toString() {
        return "Invoice{" +
               "invoiceId='" + invoiceId + '\'' +
               ", orderId=" + (orderEntity != null ? orderEntity.getOrderId() : "null") +
               ", invoiceDate=" + invoiceDate +
               ", invoicedTotalAmount=" + invoicedTotalAmount +
               '}';
    }
}