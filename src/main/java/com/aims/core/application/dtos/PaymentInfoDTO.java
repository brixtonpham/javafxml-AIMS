package com.aims.core.application.dtos;

import com.aims.core.enums.PaymentMethodType;

public class PaymentInfoDTO {
    private PaymentMethodType paymentMethodType; // CREDIT_CARD, DOMESTIC_DEBIT_CARD
    private String cardholderName; // Cho Credit Card
    private String cardNumber;     // Cho Credit Card (CHỈ TRUYỀN, KHÔNG LƯU)
    private String expiryDateMMYY; // Cho Credit Card (MM/YY)
    private String cvv;            // Cho Credit Card (CHỈ TRUYỀN, KHÔNG LƯU)
    private String bankCode;       // Cho Domestic Card (mã ngân hàng VNPay)
    // issuingBank, validFromDateMMYY có thể lấy từ PaymentMethod entity nếu đã lưu,
    // hoặc thêm vào đây nếu là one-time input.

    public PaymentInfoDTO() {
    }

    // Getters and Setters
    public PaymentMethodType getPaymentMethodType() { return paymentMethodType; }
    public void setPaymentMethodType(PaymentMethodType paymentMethodType) { this.paymentMethodType = paymentMethodType; }

    public String getCardholderName() { return cardholderName; }
    public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getExpiryDateMMYY() { return expiryDateMMYY; }
    public void setExpiryDateMMYY(String expiryDateMMYY) { this.expiryDateMMYY = expiryDateMMYY; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
}