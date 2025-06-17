package com.aims.core.application.impl.strategies.payment;

import com.aims.core.application.services.strategies.IPaymentStrategy;
import com.aims.core.entities.OrderEntity;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IPaymentGatewayAdapter;

import java.util.Map;

public class DomesticCardPaymentStrategy implements IPaymentStrategy {

    private final IPaymentGatewayAdapter gatewayAdapter;

    public DomesticCardPaymentStrategy(IPaymentGatewayAdapter gatewayAdapter) {
        if (gatewayAdapter == null) {
            throw new IllegalArgumentException("IPaymentGatewayAdapter cannot be null for DomesticCardPaymentStrategy.");
        }
        this.gatewayAdapter = gatewayAdapter;
    }

    @Override
    public Map<String, String> processPayment(OrderEntity order, Map<String, Object> clientParams)
            throws PaymentException, ValidationException, ResourceNotFoundException {
        if (order == null) {
            throw new ValidationException("Order cannot be null for domestic card payment.");
        }
        if (clientParams == null || !clientParams.containsKey("bankCode") ||
            clientParams.get("bankCode") == null || ((String)clientParams.get("bankCode")).trim().isEmpty()) {
            throw new ValidationException("Parameter 'bankCode' is required for Domestic Card payment strategy.");
        }
        System.out.println("DomesticCardPaymentStrategy: Processing payment for order " + order.getOrderId() +
                          " with BankCode: " + clientParams.get("bankCode"));

        Map<String, Object> preparedParams = gatewayAdapter.preparePaymentParameters(order, null, null);

        // Merge/override with specific parameters for domestic cards
        preparedParams.putAll(clientParams);

        return gatewayAdapter.processPayment(preparedParams);
    }

    @Override
    public Map<String, String> processRefund(String originalGatewayTransactionId, OrderEntity order, float refundAmount, String reason)
            throws PaymentException, ValidationException, ResourceNotFoundException {
        if (order == null || originalGatewayTransactionId == null || originalGatewayTransactionId.trim().isEmpty()) {
            throw new ValidationException("Order and original gateway transaction ID are required for refund.");
        }
        System.out.println("DomesticCardPaymentStrategy: Processing refund for order " + order.getOrderId());

        Map<String, Object> preparedParams = gatewayAdapter.prepareRefundParameters(order, originalGatewayTransactionId, refundAmount, reason);
        return gatewayAdapter.processRefund(preparedParams);
    }

    @Override
    public PaymentMethodType getStrategyType() {
        return PaymentMethodType.DOMESTIC_DEBIT_CARD;
    }
}