package com.aims.core.infrastructure.adapters.external.payment_gateway;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.CardDetails;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;

import java.time.LocalDateTime;
import java.util.Map;

public interface IVNPayAdapter {

    Map<String, Object> preparePaymentParameters(OrderEntity order, PaymentMethod paymentMethod, CardDetails cardDetails) throws ValidationException;

    Map<String, Object> prepareRefundParameters(OrderEntity order, String originalGatewayTransactionId, float refundAmount, String reason) throws ValidationException;

    Map<String, String> processPayment(Map<String, Object> paymentParams) throws PaymentException;

    Map<String, String> processRefund(Map<String, Object> refundParams) throws PaymentException;

    Map<String, String> queryTransactionStatus(String vnpTransactionRef, String aimsOrderId, LocalDateTime originalTransactionDate) throws PaymentException;

    boolean validateResponseSignature(Map<String, String> responseParams);
}