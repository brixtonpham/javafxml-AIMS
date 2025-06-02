package com.aims.core.application.impl.strategies.payment;

import com.aims.core.application.services.strategies.IPaymentStrategy;
import com.aims.core.entities.OrderEntity;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;

import java.util.Map;

public class DomesticCardVNPayStrategy implements IPaymentStrategy {

    private final IVNPayAdapter vnPayAdapter;

    public DomesticCardVNPayStrategy(IVNPayAdapter vnPayAdapter) {
        if (vnPayAdapter == null) {
            throw new IllegalArgumentException("IVNPayAdapter cannot be null for DomesticCardVNPayStrategy.");
        }
        this.vnPayAdapter = vnPayAdapter;
    }

    @Override
    public Map<String, String> processPayment(OrderEntity order, Map<String, Object> clientParams)
            throws PaymentException, ValidationException, ResourceNotFoundException {
        if (order == null) {
            throw new ValidationException("Order cannot be null for domestic card payment.");
        }
        if (clientParams == null || !clientParams.containsKey("vnp_BankCode") ||
            clientParams.get("vnp_BankCode") == null || ((String)clientParams.get("vnp_BankCode")).trim().isEmpty()) {
            throw new ValidationException("Parameter 'vnp_BankCode' is required for Domestic Card payment strategy.");
        }
        System.out.println("DomesticCardVNPayStrategy: Processing payment for order " + order.getOrderId() +
                           " with BankCode: " + clientParams.get("vnp_BankCode"));


        Map<String, Object> preparedVnpParams = vnPayAdapter.preparePaymentParameters(order, null, null);

        // Merge/override with specific parameters for domestic cards
        preparedVnpParams.putAll(clientParams); // This will include vnp_BankCode and potentially client IP

        return vnPayAdapter.processPayment(preparedVnpParams);
    }

    @Override
    public Map<String, String> processRefund(String originalGatewayTransactionId, OrderEntity order, float refundAmount, String reason)
            throws PaymentException, ValidationException, ResourceNotFoundException {
        if (order == null || originalGatewayTransactionId == null || originalGatewayTransactionId.trim().isEmpty()) {
            throw new ValidationException("Order and original gateway transaction ID are required for refund.");
        }
        System.out.println("DomesticCardVNPayStrategy: Processing refund for order " + order.getOrderId());

        Map<String, Object> preparedVnpParams = vnPayAdapter.prepareRefundParameters(order, originalGatewayTransactionId, refundAmount, reason);
        return vnPayAdapter.processRefund(preparedVnpParams);
    }

    @Override
    public PaymentMethodType getStrategyType() {
        return PaymentMethodType.DOMESTIC_DEBIT_CARD;
    }
}