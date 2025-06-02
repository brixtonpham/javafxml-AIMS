package com.aims.core.entities;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "CARD_DETAILS")
public class CardDetails {

    @Id
    @Column(name = "paymentMethodID", length = 50) // Đây cũng là khóa ngoại trỏ tới PaymentMethod
    private String paymentMethodId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Sử dụng MapsId để chỉ định rằng paymentMethodId cũng là khóa chính và liên kết với PaymentMethod
    @JoinColumn(name = "paymentMethodID")
    private PaymentMethod paymentMethod;

    @Column(name = "cardholderName", nullable = false, length = 255)
    private String cardholderName;

    @Column(name = "cardNumber_masked", nullable = false, length = 25) // e.g., "************1234"
    private String cardNumberMasked;

    @Column(name = "expiryDate_MMYY", nullable = false, length = 5) // e.g., "12/28"
    private String expiryDateMMYY;

    @Column(name = "validFromDate_MMYY", length = 5) // e.g., "12/23", for Domestic Card, nullable
    private String validFromDateMMYY;

    @Column(name = "issuingBank", length = 255)
    private String issuingBank;

    // Có thể thêm CardType.java (enum) nếu cần phân biệt rõ hơn trong logic code
    // @Enumerated(EnumType.STRING)
    // @Column(name = "cardType", length = 50)
    // private CardType cardType;


    public CardDetails() {
    }

    public CardDetails(PaymentMethod paymentMethod, String cardholderName, String cardNumberMasked, String expiryDateMMYY, String validFromDateMMYY, String issuingBank) {
        this.paymentMethod = paymentMethod;
        this.paymentMethodId = (paymentMethod != null) ? paymentMethod.getPaymentMethodId() : null;
        this.cardholderName = cardholderName;
        this.cardNumberMasked = cardNumberMasked;
        this.expiryDateMMYY = expiryDateMMYY;
        this.validFromDateMMYY = validFromDateMMYY;
        this.issuingBank = issuingBank;
    }


    // Getters and Setters
    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    // Setter for paymentMethodId is typically not needed when using @MapsId
    // as it's derived from the associated paymentMethod.

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        this.paymentMethodId = (paymentMethod != null) ? paymentMethod.getPaymentMethodId() : null;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public String getCardNumberMasked() {
        return cardNumberMasked;
    }

    public void setCardNumberMasked(String cardNumberMasked) {
        this.cardNumberMasked = cardNumberMasked;
    }

    public String getExpiryDateMMYY() {
        return expiryDateMMYY;
    }

    public void setExpiryDateMMYY(String expiryDateMMYY) {
        this.expiryDateMMYY = expiryDateMMYY;
    }

    public String getValidFromDateMMYY() {
        return validFromDateMMYY;
    }

    public void setValidFromDateMMYY(String validFromDateMMYY) {
        this.validFromDateMMYY = validFromDateMMYY;
    }

    public String getIssuingBank() {
        return issuingBank;
    }

    public void setIssuingBank(String issuingBank) {
        this.issuingBank = issuingBank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardDetails that = (CardDetails) o;
        return Objects.equals(paymentMethodId, that.paymentMethodId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentMethodId);
    }

    @Override
    public String toString() {
        return "CardDetails{" +
               "paymentMethodId='" + paymentMethodId + '\'' +
               ", cardholderName='" + cardholderName + '\'' +
               ", cardNumberMasked='" + cardNumberMasked + '\'' +
               ", expiryDateMMYY='" + expiryDateMMYY + '\'' +
               '}';
    }
}