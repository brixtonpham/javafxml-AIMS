package com.aims.core.entities;

import com.aims.core.enums.PaymentMethodType;
import jakarta.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "PAYMENT_METHOD")
public class PaymentMethod {

    @Id
    @Column(name = "paymentMethodID", length = 50)
    private String paymentMethodId;

    @Enumerated(EnumType.STRING)
    @Column(name = "methodType", nullable = false, length = 50)
    private PaymentMethodType methodType; // e.g., CREDIT_CARD, DOMESTIC_DEBIT_CARD

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID", referencedColumnName = "userID") // For saved methods by a user
    private UserAccount userAccount;

    @Column(name = "isDefault")
    private boolean isDefault = false;

    // Relationship to CardDetails (one PaymentMethod can have one CardDetails if it's a card type)
    @OneToOne(mappedBy = "paymentMethod", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private CardDetails cardDetails;

    // Relationship to PaymentTransaction
    @OneToMany(mappedBy = "paymentMethod", fetch = FetchType.LAZY)
    private List<PaymentTransaction> transactions;

    public PaymentMethod() {
    }

    public PaymentMethod(String paymentMethodId, PaymentMethodType methodType, UserAccount userAccount, boolean isDefault) {
        this.paymentMethodId = paymentMethodId;
        this.methodType = methodType;
        this.userAccount = userAccount;
        this.isDefault = isDefault;
    }

    // Getters and Setters
    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public PaymentMethodType getMethodType() {
        return methodType;
    }

    public void setMethodType(PaymentMethodType methodType) {
        this.methodType = methodType;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public CardDetails getCardDetails() {
        return cardDetails;
    }

    public void setCardDetails(CardDetails cardDetails) {
        this.cardDetails = cardDetails;
        if (cardDetails != null) {
            cardDetails.setPaymentMethod(this);
        }
    }

    public List<PaymentTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<PaymentTransaction> transactions) {
        this.transactions = transactions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentMethod that = (PaymentMethod) o;
        return Objects.equals(paymentMethodId, that.paymentMethodId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentMethodId);
    }

    @Override
    public String toString() {
        return "PaymentMethod{" +
               "paymentMethodId='" + paymentMethodId + '\'' +
               ", methodType=" + methodType +
               ", user=" + (userAccount != null ? userAccount.getUserId() : "N/A") +
               ", isDefault=" + isDefault +
               '}';
    }
}