package com.aims.test.payment;

import com.aims.core.application.impl.strategies.payment.CreditCardPaymentStrategy;
import com.aims.core.application.impl.strategies.payment.DomesticCardPaymentStrategy;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.CardDetails;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IPaymentGatewayAdapter;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Payment Strategy Tests")
public class PaymentStrategyTest {

    @Mock
    private IPaymentGatewayAdapter mockGatewayAdapter;

    private CreditCardPaymentStrategy creditCardStrategy;
    private DomesticCardPaymentStrategy domesticCardStrategy;
    private OrderEntity testOrder;
    private PaymentMethod testPaymentMethod;
    private CardDetails testCard;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        creditCardStrategy = new CreditCardPaymentStrategy(mockGatewayAdapter);
        domesticCardStrategy = new DomesticCardPaymentStrategy(mockGatewayAdapter);
        
        // Create test order
        testOrder = new OrderEntity();
        testOrder.setOrderId("TEST_ORDER_123");
        testOrder.setTotalAmountPaid(100.0f);
        testOrder.setOrderDate(LocalDateTime.now());
        
        // Create test payment method
        testPaymentMethod = new PaymentMethod();
        testPaymentMethod.setPaymentMethodId("TEST_PM_123");
        testPaymentMethod.setMethodType(PaymentMethodType.CREDIT_CARD);
        
        // Create test card details
        testCard = new CardDetails();
        testCard.setCardNumberMasked("************1234");
        testCard.setCardholderName("TEST USER");
        testCard.setExpiryDateMMYY("12/25");
    }

    @Test
    @DisplayName("Credit Card Strategy - Should process payment successfully")
    void testCreditCardStrategy_ProcessPayment_Success() throws PaymentException, ValidationException, ResourceNotFoundException {
        // Arrange
        Map<String, Object> expectedGatewayParams = new HashMap<>();
        expectedGatewayParams.put("gateway_amount", "10000");
        expectedGatewayParams.put("gateway_transaction_ref", "TEST_TXN_123");
        
        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("paymentUrl", "https://gateway.test/payment/123");
        expectedResult.put("gateway_transaction_ref", "TEST_TXN_123");
        
        when(mockGatewayAdapter.preparePaymentParameters(any(), any(), any()))
            .thenReturn(expectedGatewayParams);
        when(mockGatewayAdapter.processPayment(any()))
            .thenReturn(expectedResult);
        
        Map<String, Object> clientParams = new HashMap<>();
        
        // Act
        Map<String, String> result = creditCardStrategy.processPayment(testOrder, clientParams);
        
        // Assert
        assertNotNull(result);
        assertEquals("https://gateway.test/payment/123", result.get("paymentUrl"));
        assertEquals("TEST_TXN_123", result.get("gateway_transaction_ref"));
        
        verify(mockGatewayAdapter).preparePaymentParameters(testOrder, testPaymentMethod, testCard);
        verify(mockGatewayAdapter).processPayment(argThat(params -> 
            params.containsKey("payment_method") && 
            "CREDIT_CARD".equals(params.get("payment_method"))
        ));
    }

    @Test
    @DisplayName("Credit Card Strategy - Should handle null order")
    void testCreditCardStrategy_ProcessPayment_NullOrder() {
        // Arrange
        Map<String, Object> clientParams = new HashMap<>();
        
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            creditCardStrategy.processPayment(null, clientParams);
        });
        
        verifyNoInteractions(mockGatewayAdapter);
    }

    @Test
    @DisplayName("Domestic Card Strategy - Should process payment successfully")
    void testDomesticCardStrategy_ProcessPayment_Success() throws PaymentException, ValidationException, ResourceNotFoundException {
        // Arrange
        Map<String, Object> expectedGatewayParams = new HashMap<>();
        expectedGatewayParams.put("gateway_amount", "10000");
        expectedGatewayParams.put("gateway_transaction_ref", "TEST_TXN_123");
        
        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("paymentUrl", "https://gateway.test/payment/123");
        expectedResult.put("gateway_transaction_ref", "TEST_TXN_123");
        
        when(mockGatewayAdapter.preparePaymentParameters(any(), any(), any()))
            .thenReturn(expectedGatewayParams);
        when(mockGatewayAdapter.processPayment(any()))
            .thenReturn(expectedResult);
        
        Map<String, Object> clientParams = new HashMap<>();
        clientParams.put("bank_code", "NCB");
        
        // Act
        Map<String, String> result = domesticCardStrategy.processPayment(testOrder, clientParams);
        
        // Assert
        assertNotNull(result);
        assertEquals("https://gateway.test/payment/123", result.get("paymentUrl"));
        assertEquals("TEST_TXN_123", result.get("gateway_transaction_ref"));
        
        verify(mockGatewayAdapter).preparePaymentParameters(testOrder, testPaymentMethod, testCard);
        verify(mockGatewayAdapter).processPayment(argThat(params -> 
            params.containsKey("payment_method") && 
            "DOMESTIC_CARD".equals(params.get("payment_method")) &&
            params.containsKey("bank_code") &&
            "NCB".equals(params.get("bank_code"))
        ));
    }

    @Test
    @DisplayName("Domestic Card Strategy - Should require bank code")
    void testDomesticCardStrategy_ProcessPayment_MissingBankCode() {
        // Arrange
        Map<String, Object> clientParams = new HashMap<>();
        // No bank_code provided
        
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            domesticCardStrategy.processPayment(testOrder, clientParams);
        });
        
        verifyNoInteractions(mockGatewayAdapter);
    }

    @Test
    @DisplayName("Domestic Card Strategy - Should require non-empty bank code")
    void testDomesticCardStrategy_ProcessPayment_EmptyBankCode() {
        // Arrange
        Map<String, Object> clientParams = new HashMap<>();
        clientParams.put("bank_code", ""); // Empty bank code
        
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            domesticCardStrategy.processPayment(testOrder, clientParams);
        });
        
        verifyNoInteractions(mockGatewayAdapter);
    }

    @Test
    @DisplayName("Credit Card Strategy - Should process refund successfully")
    void testCreditCardStrategy_ProcessRefund_Success() throws PaymentException, ValidationException, ResourceNotFoundException {
        // Arrange
        String originalTransactionId = "ORIGINAL_TXN_123";
        float refundAmount = 50.0f;
        String reason = "Customer request";
        
        Map<String, Object> expectedRefundParams = new HashMap<>();
        expectedRefundParams.put("gateway_refund_amount", "5000");
        expectedRefundParams.put("gateway_original_transaction_id", originalTransactionId);
        
        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("refund_status", "SUCCESS");
        expectedResult.put("refund_transaction_id", "REFUND_123");
        
        when(mockGatewayAdapter.prepareRefundParameters(any(), eq(originalTransactionId), eq(refundAmount), eq(reason)))
            .thenReturn(expectedRefundParams);
        when(mockGatewayAdapter.processRefund(any()))
            .thenReturn(expectedResult);
        
        // Act
        Map<String, String> result = creditCardStrategy.processRefund(originalTransactionId, testOrder, refundAmount, reason);
        
        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.get("refund_status"));
        assertEquals("REFUND_123", result.get("refund_transaction_id"));
        
        verify(mockGatewayAdapter).prepareRefundParameters(testOrder, originalTransactionId, refundAmount, reason);
        verify(mockGatewayAdapter).processRefund(expectedRefundParams);
    }

    @Test
    @DisplayName("Should reject null gateway adapter in constructor")
    void testStrategy_NullGatewayAdapter() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new CreditCardPaymentStrategy(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new DomesticCardPaymentStrategy(null);
        });
    }
}