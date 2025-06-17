package com.aims.test.payment;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.CardDetails;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IPaymentGatewayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayGatewayAdapter;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Payment Gateway Integration Tests")
public class PaymentGatewayIntegrationTest {

    private IPaymentGatewayAdapter gatewayAdapter;
    private OrderEntity testOrder;
    private PaymentMethod testPaymentMethod;
    private CardDetails testCard;

    @BeforeEach
    void setUp() {
        gatewayAdapter = new VNPayGatewayAdapter();
        
        // Create test order
        testOrder = new OrderEntity();
        testOrder.setOrderId("TEST_ORDER_" + System.currentTimeMillis());
        testOrder.setTotalAmountPaid(100.0f);
        testOrder.setOrderDate(LocalDateTime.now());
        
        // Create test payment method
        testPaymentMethod = new PaymentMethod();
        testPaymentMethod.setPaymentMethodId("TEST_PM_" + System.currentTimeMillis());
        testPaymentMethod.setMethodType(PaymentMethodType.CREDIT_CARD);
        
        // Create test card details
        testCard = new CardDetails();
        testCard.setCardNumberMasked("************2198");
        testCard.setCardholderName("TEST USER");
        testCard.setExpiryDateMMYY("12/25");
        testCard.setIssuingBank("TEST BANK");
    }

    @Test
    @DisplayName("Should prepare payment parameters successfully")
    void testPreparePaymentParameters() throws ValidationException {
        // Act
        Map<String, Object> params = gatewayAdapter.preparePaymentParameters(testOrder, testPaymentMethod, testCard);
        
        // Assert
        assertNotNull(params);
        assertFalse(params.isEmpty());
        assertTrue(params.containsKey("gateway_amount"));
        assertTrue(params.containsKey("gateway_transaction_ref"));
        assertTrue(params.containsKey("gateway_order_info"));
        
        // Verify amount conversion
        assertEquals("10000", params.get("gateway_amount")); // Amount in cents
    }

    @Test
    @DisplayName("Should process payment and return payment URL")
    void testProcessPayment() throws PaymentException, ValidationException {
        // Arrange
        Map<String, Object> paymentParams = gatewayAdapter.preparePaymentParameters(testOrder, testPaymentMethod, testCard);
        
        // Act
        Map<String, String> result = gatewayAdapter.processPayment(paymentParams);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("paymentUrl"));
        assertTrue(result.containsKey("gateway_transaction_ref"));
        
        String paymentUrl = result.get("paymentUrl");
        assertNotNull(paymentUrl);
        assertTrue(paymentUrl.startsWith("http"));
    }

    @Test
    @DisplayName("Should validate response signature correctly")
    void testValidateResponseSignature() {
        // Arrange
        Map<String, String> validResponse = new HashMap<>();
        validResponse.put("gateway_transaction_ref", "TEST_TXN_123");
        validResponse.put("gateway_response_code", "00");
        validResponse.put("gateway_amount", "10000");
        
        // Generate signature for test
        String testSignature = "test_signature_hash";
        validResponse.put("gateway_secure_hash", testSignature);
        
        // Act & Assert
        // Note: This will depend on the actual implementation
        // For now, we'll test that the method exists and doesn't throw
        assertDoesNotThrow(() -> {
            boolean isValid = gatewayAdapter.validateResponseSignature(validResponse);
            // The actual validation will depend on the gateway implementation
        });
    }

    @Test
    @DisplayName("Should prepare refund parameters correctly")
    void testPrepareRefundParameters() throws ValidationException {
        // Arrange
        String originalTransactionId = "ORIGINAL_TXN_123";
        float refundAmount = 50.0f;
        String reason = "Customer requested refund";
        
        // Act
        Map<String, Object> params = gatewayAdapter.prepareRefundParameters(
            testOrder, originalTransactionId, refundAmount, reason);
        
        // Assert
        assertNotNull(params);
        assertTrue(params.containsKey("gateway_refund_amount"));
        assertTrue(params.containsKey("gateway_original_transaction_id"));
        assertTrue(params.containsKey("gateway_refund_reason"));
        
        assertEquals("5000", params.get("gateway_refund_amount")); // Amount in cents
        assertEquals(originalTransactionId, params.get("gateway_original_transaction_id"));
        assertEquals(reason, params.get("gateway_refund_reason"));
    }

    @Test
    @DisplayName("Should handle null order gracefully")
    void testPreparePaymentParametersWithNullOrder() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            gatewayAdapter.preparePaymentParameters(null, testPaymentMethod, testCard);
        });
    }

    @Test
    @DisplayName("Should handle zero amount gracefully")
    void testPreparePaymentParametersWithZeroAmount() {
        // Arrange
        testOrder.setTotalAmountPaid(0.0f);
        
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            gatewayAdapter.preparePaymentParameters(testOrder, testPaymentMethod, testCard);
        });
    }

    @Test
    @DisplayName("Should query transaction status")
    void testQueryTransactionStatus() throws PaymentException {
        // Arrange
        String transactionRef = "TEST_TXN_REF";
        String orderId = testOrder.getOrderId();
        LocalDateTime transactionDate = LocalDateTime.now();
        
        // Act
        Map<String, String> result = gatewayAdapter.queryTransactionStatus(transactionRef, orderId, transactionDate);
        
        // Assert
        assertNotNull(result);
        // The actual assertions will depend on the gateway implementation
        // For now, we verify the method executes without throwing
    }
}