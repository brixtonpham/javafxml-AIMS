package com.aims.test.payment;

import com.aims.core.presentation.controllers.PaymentProcessingScreenController;
import com.aims.core.entities.OrderEntity;
import javafx.application.HostServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for browser integration functionality in PaymentProcessingScreenController
 */
public class BrowserIntegrationTest {

    private PaymentProcessingScreenController controller;
    
    @Mock
    private HostServices mockHostServices;
    
    @Mock 
    private OrderEntity mockOrder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new PaymentProcessingScreenController();
    }

    @Test
    void testHostServicesInjection() {
        // Test that HostServices can be successfully injected
        controller.setHostServices(mockHostServices);
        
        // No direct way to verify injection, but should not throw exception
        assertDoesNotThrow(() -> controller.setHostServices(mockHostServices));
    }

    @Test
    void testWebViewModeToggle() {
        // Test WebView mode can be toggled
        controller.setUseWebView(true);
        controller.setUseWebView(false);
        
        // Should complete without throwing exceptions
        assertDoesNotThrow(() -> {
            controller.setUseWebView(true);
            controller.setUseWebView(false);
        });
    }

    @Test
    void testPaymentDataWithUrl() {
        // Test setting payment data with URL
        String testUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=100000";
        String transactionId = "TEST_TXN_123";
        
        when(mockOrder.getOrderId()).thenReturn("ORDER_123");
        
        assertDoesNotThrow(() -> {
            controller.setPaymentData(mockOrder, transactionId, testUrl);
        });
    }

    @Test
    void testPaymentDataWithoutUrl() {
        // Test setting payment data without URL (direct processing mode)
        String transactionId = "TEST_TXN_123";
        
        when(mockOrder.getOrderId()).thenReturn("ORDER_123");
        
        assertDoesNotThrow(() -> {
            controller.setPaymentData(mockOrder, transactionId, null);
        });
    }
}