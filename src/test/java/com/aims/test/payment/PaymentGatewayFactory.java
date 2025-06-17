package com.aims.test.payment;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.aims.core.infrastructure.adapters.external.payment_gateway.IPaymentGatewayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.StubPaymentGatewayAdapter;

public class PaymentGatewayFactory {
    
    public static IPaymentGatewayAdapter createConfiguredAdapter() {
        Properties props = loadPaymentGatewayConfig();
        String activeGateway = props.getProperty("payment.gateway.active", "STUB");
        
        switch (activeGateway.toUpperCase()) {
            case "VNPAY":
                return new VNPayAdapter(props);
            // Add cases for other gateways as they are implemented
            default:
                System.out.println("Using stub payment gateway adapter for testing");
                return new StubPaymentGatewayAdapter();
        }
    }
    
    private static Properties loadPaymentGatewayConfig() {
        Properties props = new Properties();
        try {
            props.load(PaymentGatewayFactory.class.getClassLoader()
                    .getResourceAsStream("payment_gateway_config.properties"));
        } catch (IOException e) {
            System.err.println("Warning: Could not load payment gateway configuration. Using defaults.");
        }
        return props;
    }
}