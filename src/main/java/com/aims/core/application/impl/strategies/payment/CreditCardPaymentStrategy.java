package com.aims.core.application.impl.strategies.payment;

import com.aims.core.application.services.strategies.IPaymentStrategy;
import com.aims.core.entities.OrderEntity;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IPaymentGatewayAdapter;

import java.util.Map;

public class CreditCardPaymentStrategy implements IPaymentStrategy {

    private final IPaymentGatewayAdapter gatewayAdapter;

    public CreditCardPaymentStrategy(IPaymentGatewayAdapter gatewayAdapter) {
        if (gatewayAdapter == null) {
            throw new IllegalArgumentException("IPaymentGatewayAdapter cannot be null for CreditCardPaymentStrategy.");
        }
        this.gatewayAdapter = gatewayAdapter;
    }

    @Override
    public Map<String, String> processPayment(OrderEntity order, Map<String, Object> clientParams)
            throws PaymentException, ValidationException, ResourceNotFoundException {
        if (order == null) {
            throw new ValidationException("Order cannot be null for credit card payment.");
        }
        System.out.println("CreditCardPaymentStrategy: Processing payment for order " + order.getOrderId());

        Map<String, Object> preparedParams = gatewayAdapter.preparePaymentParameters(order, null, null);

        // Merge any specific client parameters if necessary
        if (clientParams != null) {
            preparedParams.putAll(clientParams);
        }

        return gatewayAdapter.processPayment(preparedParams);
    }

    @Override
    public Map<String, String> processRefund(String originalGatewayTransactionId, OrderEntity order, float refundAmount, String reason)
            throws PaymentException, ValidationException, ResourceNotFoundException {
        if (order == null || originalGatewayTransactionId == null || originalGatewayTransactionId.trim().isEmpty()) {
            throw new ValidationException("Order and original gateway transaction ID are required for refund.");
        }
        System.out.println("CreditCardPaymentStrategy: Processing refund for order " + order.getOrderId());

        Map<String, Object> preparedParams = gatewayAdapter.prepareRefundParameters(order, originalGatewayTransactionId, refundAmount, reason);
        return gatewayAdapter.processRefund(preparedParams);
    }

    @Override
    public PaymentMethodType getStrategyType() {
        return PaymentMethodType.CREDIT_CARD;
    }
}