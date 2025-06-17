package com.aims.core.infrastructure.adapters.external.payment_gateway;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.CardDetails;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;

public class StubPaymentGatewayAdapter implements IPaymentGatewayAdapter {

    private static final Map<String, String> FIELD_MAPPINGS = new HashMap<>();
    static {
        FIELD_MAPPINGS.put("amount", "amount");
        FIELD_MAPPINGS.put("orderInfo", "description");
        FIELD_MAPPINGS.put("transactionRef", "transactionId");
        FIELD_MAPPINGS.put("responseCode", "status");
    }

    @Override
    public Map<String, Object> preparePaymentParameters(OrderEntity order, PaymentMethod paymentMethod, CardDetails cardDetails) throws ValidationException {
        if (order == null) {
            throw new ValidationException("Order cannot be null");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("orderInfo", "Payment for order " + order.getOrderId());
        params.put("amount", String.valueOf(order.getTotalAmountPaid()));
        params.put("transactionRef", "STUB_" + UUID.randomUUID().toString());
        return params;
    }

    @Override
    public Map<String, Object> prepareRefundParameters(OrderEntity order, String originalTransactionId, float refundAmount, String reason) throws ValidationException {
        if (order == null || originalTransactionId == null) {
            throw new ValidationException("Order and original transaction ID are required for refund");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("refundInfo", reason);
        params.put("amount", String.valueOf(refundAmount));
        params.put("originalTransactionId", originalTransactionId);
        params.put("orderId", order.getOrderId());
        return params;
    }

    @Override
    public Map<String, String> processPayment(Map<String, Object> paymentParams) throws PaymentException {
        // Simulate successful payment processing
        Map<String, String> response = new HashMap<>();
        response.put("responseCode", "00");
        response.put("message", "Success");
        response.put("transactionId", "STUB_TX_" + UUID.randomUUID().toString());
        return response;
    }

    @Override
    public Map<String, String> processRefund(Map<String, Object> refundParams) throws PaymentException {
        // Simulate successful refund processing
        Map<String, String> response = new HashMap<>();
        response.put("responseCode", "00");
        response.put("message", "Refund processed successfully");
        response.put("refundId", "STUB_REFUND_" + UUID.randomUUID().toString());
        return response;
    }

    @Override
    public Map<String, String> queryTransactionStatus(String gatewayTransactionRef, String aimsOrderId, LocalDateTime originalTransactionDate) throws PaymentException {
        // Simulate transaction query
        Map<String, String> response = new HashMap<>();
        response.put("responseCode", "00");
        response.put("status", "SUCCESS");
        response.put("message", "Transaction completed successfully");
        return response;
    }

    @Override
    public boolean validateResponseSignature(Map<String, String> responseParams) {
        // Stub implementation always returns true for testing
        return true;
    }

    @Override
    public String getGatewayType() {
        return "STUB";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String mapResponseCodeToStatus(String gatewayResponseCode) {
        switch (gatewayResponseCode) {
            case "00":
                return "SUCCESS";
            case "01":
                return "PENDING";
            case "02":
                return "CANCELLED";
            default:
                return "FAILED";
        }
    }

    @Override
    public Map<String, String> getFieldMappings() {
        return new HashMap<>(FIELD_MAPPINGS);
    }
}