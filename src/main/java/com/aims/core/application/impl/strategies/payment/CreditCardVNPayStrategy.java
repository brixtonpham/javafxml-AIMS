package com.aims.core.application.impl.strategies.payment;

import com.aims.core.application.services.strategies.IPaymentStrategy;
import com.aims.core.entities.OrderEntity;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;

import java.util.Map;

public class CreditCardVNPayStrategy implements IPaymentStrategy {

    private final IVNPayAdapter vnPayAdapter;

    public CreditCardVNPayStrategy(IVNPayAdapter vnPayAdapter) {
        if (vnPayAdapter == null) {
            throw new IllegalArgumentException("IVNPayAdapter cannot be null for CreditCardVNPayStrategy.");
        }
        this.vnPayAdapter = vnPayAdapter;
    }

    @Override
    public Map<String, String> processPayment(OrderEntity order, Map<String, Object> clientParams)
            throws PaymentException, ValidationException, ResourceNotFoundException {
        if (order == null) {
            throw new ValidationException("Order cannot be null for credit card payment.");
        }
        System.out.println("CreditCardVNPayStrategy: Processing payment for order " + order.getOrderId());

        // The IVNPayAdapter's preparePaymentParameters should ideally handle creating the
        // full parameter set for VNPay based on the order and potentially a PaymentMethod entity.
        // ClientParams might contain things like client's IP address.
        // Here, we pass null for PaymentMethod and CardDetails as this strategy is specific
        // and the adapter will know it's for a general card payment scenario.
        Map<String, Object> preparedVnpParams = vnPayAdapter.preparePaymentParameters(order, null, null);

        // Merge any specific client parameters if necessary
        if (clientParams != null) {
            preparedVnpParams.putAll(clientParams); // e.g., client IP from controller
        }
        // Ensure vnp_BankCode is appropriate for international cards if not set by preparePaymentParameters
        preparedVnpParams.putIfAbsent("vnp_BankCode", "INTCARD"); // Or leave empty for VNPay selection

        return vnPayAdapter.processPayment(preparedVnpParams);
    }

    @Override
    public Map<String, String> processRefund(String originalGatewayTransactionId, OrderEntity order, float refundAmount, String reason)
            throws PaymentException, ValidationException, ResourceNotFoundException {
        if (order == null || originalGatewayTransactionId == null || originalGatewayTransactionId.trim().isEmpty()) {
            throw new ValidationException("Order and original gateway transaction ID are required for refund.");
        }
        System.out.println("CreditCardVNPayStrategy: Processing refund for order " + order.getOrderId());

        Map<String, Object> preparedVnpParams = vnPayAdapter.prepareRefundParameters(order, originalGatewayTransactionId, refundAmount, reason);
        return vnPayAdapter.processRefund(preparedVnpParams);
    }

    @Override
    public PaymentMethodType getStrategyType() {
        return PaymentMethodType.CREDIT_CARD;
    }
}