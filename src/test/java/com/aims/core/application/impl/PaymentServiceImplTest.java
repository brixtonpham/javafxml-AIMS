package com.aims.core.application.impl;

import com.aims.core.entities.CardDetails;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.enums.TransactionType;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;
import com.aims.core.infrastructure.database.dao.ICardDetailsDAO;
import com.aims.core.infrastructure.database.dao.IOrderEntityDAO;
import com.aims.core.infrastructure.database.dao.IPaymentMethodDAO;
import com.aims.core.infrastructure.database.dao.IPaymentTransactionDAO;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {

    @Mock
    private IPaymentTransactionDAO paymentTransactionDAO;

    @Mock
    private IPaymentMethodDAO paymentMethodDAO;

    @Mock
    private ICardDetailsDAO cardDetailsDAO;

    @Mock
    private IVNPayAdapter vnPayAdapter;

    @Mock
    private IOrderEntityDAO orderDAO; // Added for refund tests

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private OrderEntity sampleOrder;
    private PaymentMethod samplePaymentMethod;
    private CardDetails sampleCardDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentService.setOrderDAO(orderDAO); // Manually inject orderDAO

        sampleOrder = new OrderEntity();
        sampleOrder.setOrderId("ORD-" + UUID.randomUUID().toString());
        sampleOrder.setTotalAmountPaid(100.0f);

        samplePaymentMethod = new PaymentMethod();
        samplePaymentMethod.setPaymentMethodId("PM-" + UUID.randomUUID().toString());
        samplePaymentMethod.setMethodType(PaymentMethodType.CREDIT_CARD);

        sampleCardDetails = new CardDetails();
        // Corrected: Set PaymentMethod object, which in turn sets the paymentMethodId
        sampleCardDetails.setPaymentMethod(samplePaymentMethod); 
        // sampleCardDetails.setCardId("CARD-" + UUID.randomUUID().toString()); // This field does not exist
        // sampleCardDetails.setPaymentMethodId(samplePaymentMethod.getPaymentMethodId()); // Set via setPaymentMethod
        sampleCardDetails.setCardNumberMasked("************5678"); // Corrected field name
        sampleCardDetails.setCardholderName("Test User");
        sampleCardDetails.setExpiryDateMMYY("12/25");
        sampleCardDetails.setIssuingBank("Test Bank");
    }

    @Test
    void processPayment_success() throws SQLException, PaymentException, ValidationException, ResourceNotFoundException {
        when(paymentMethodDAO.getById(samplePaymentMethod.getPaymentMethodId())).thenReturn(samplePaymentMethod);
        when(cardDetailsDAO.getByPaymentMethodId(samplePaymentMethod.getPaymentMethodId())).thenReturn(sampleCardDetails);
        when(vnPayAdapter.preparePaymentParameters(any(OrderEntity.class), any(PaymentMethod.class), any(CardDetails.class)))
                .thenReturn(new HashMap<>());
        Map<String, String> gatewayResponse = new HashMap<>();
        gatewayResponse.put("vnp_ResponseCode", "00");
        gatewayResponse.put("vnp_TransactionNo", "VNP12345");
        when(vnPayAdapter.processPayment(anyMap())).thenReturn(gatewayResponse);
        doNothing().when(paymentTransactionDAO).add(any(PaymentTransaction.class));
        doNothing().when(paymentTransactionDAO).updateStatus(anyString(), anyString(), anyString());

        PaymentTransaction transaction = paymentService.processPayment(sampleOrder, samplePaymentMethod.getPaymentMethodId());

        assertNotNull(transaction);
        assertEquals("SUCCESS", transaction.getTransactionStatus());
        assertEquals("VNP12345", transaction.getExternalTransactionId());
        verify(paymentTransactionDAO, times(1)).add(any(PaymentTransaction.class));
        verify(paymentTransactionDAO, times(1)).updateStatus(anyString(), eq("SUCCESS"), eq("VNP12345"));
    }

    @Test
    void processPayment_gatewayFailure_throwsPaymentException() throws SQLException, PaymentException, ValidationException, ResourceNotFoundException {
        when(paymentMethodDAO.getById(samplePaymentMethod.getPaymentMethodId())).thenReturn(samplePaymentMethod);
        when(cardDetailsDAO.getByPaymentMethodId(samplePaymentMethod.getPaymentMethodId())).thenReturn(sampleCardDetails);
        when(vnPayAdapter.preparePaymentParameters(any(OrderEntity.class), any(PaymentMethod.class), any(CardDetails.class)))
                .thenReturn(new HashMap<>());
        Map<String, String> gatewayResponse = new HashMap<>();
        gatewayResponse.put("vnp_ResponseCode", "99"); // Failure code
        gatewayResponse.put("vnp_Message", "Gateway Error");
        when(vnPayAdapter.processPayment(anyMap())).thenReturn(gatewayResponse);
        doNothing().when(paymentTransactionDAO).add(any(PaymentTransaction.class));
        doNothing().when(paymentTransactionDAO).updateStatus(anyString(), anyString(), anyString());

        PaymentException exception = assertThrows(PaymentException.class, () ->
                paymentService.processPayment(sampleOrder, samplePaymentMethod.getPaymentMethodId()));

        assertTrue(exception.getMessage().contains("Gateway Error"));
        verify(paymentTransactionDAO, times(1)).add(any(PaymentTransaction.class));
        verify(paymentTransactionDAO, times(1)).updateStatus(anyString(), eq("FAILED"), isNull());
    }

    @Test
    void processPayment_paymentMethodNotFound_throwsResourceNotFoundException() throws SQLException {
        when(paymentMethodDAO.getById(anyString())).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.processPayment(sampleOrder, "invalid-pm-id"));
    }

    @Test
    void processRefund_success() throws SQLException, PaymentException, ValidationException, ResourceNotFoundException {
        String originalGatewayTxnId = "VNP12345";
        float refundAmount = 50.0f;
        String reason = "Item returned";

        when(orderDAO.getById(sampleOrder.getOrderId())).thenReturn(sampleOrder);
        when(vnPayAdapter.prepareRefundParameters(any(OrderEntity.class), anyString(), anyFloat(), anyString()))
                .thenReturn(new HashMap<>());
        Map<String, String> gatewayResponse = new HashMap<>();
        gatewayResponse.put("vnp_ResponseCode", "00");
        gatewayResponse.put("vnp_TransactionNo", "VNP_REF_67890");
        when(vnPayAdapter.processRefund(anyMap())).thenReturn(gatewayResponse);
        doNothing().when(paymentTransactionDAO).add(any(PaymentTransaction.class));
        doNothing().when(paymentTransactionDAO).updateStatus(anyString(), anyString(), anyString());

        PaymentTransaction refundTransaction = paymentService.processRefund(sampleOrder.getOrderId(), originalGatewayTxnId, refundAmount, reason);

        assertNotNull(refundTransaction);
        assertEquals("SUCCESS", refundTransaction.getTransactionStatus());
        assertEquals("VNP_REF_67890", refundTransaction.getExternalTransactionId());
        assertEquals(TransactionType.REFUND, refundTransaction.getTransactionType());
        assertEquals(refundAmount, refundTransaction.getAmount());
        verify(paymentTransactionDAO, times(1)).add(any(PaymentTransaction.class));
        verify(paymentTransactionDAO, times(1)).updateStatus(anyString(), eq("SUCCESS"), eq("VNP_REF_67890"));
    }

    @Test
    void processRefund_orderNotFound_throwsResourceNotFoundException() throws SQLException {
        when(orderDAO.getById(anyString())).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.processRefund("invalid-order-id", "VNP12345", 50.0f, "reason"));
    }

    @Test
    void checkPaymentStatus_success() throws SQLException, PaymentException, ResourceNotFoundException, ValidationException {
        PaymentTransaction existingTransaction = new PaymentTransaction();
        existingTransaction.setTransactionId("PAY-" + UUID.randomUUID().toString());
        existingTransaction.setOrder(sampleOrder);
        existingTransaction.setExternalTransactionId("VNP12345");
        existingTransaction.setTransactionDateTime(LocalDateTime.now().minusHours(1));

        when(paymentTransactionDAO.getById(existingTransaction.getTransactionId())).thenReturn(existingTransaction);
        Map<String, String> gatewayStatusResponse = new HashMap<>();
        gatewayStatusResponse.put("vnp_ResponseCode", "00");
        gatewayStatusResponse.put("vnp_Message", "Success");
        gatewayStatusResponse.put("vnp_TransactionNo", "VNP12345");

        when(vnPayAdapter.queryTransactionStatus(anyString(), anyString(), any(LocalDateTime.class)))
                .thenReturn(gatewayStatusResponse);
        doNothing().when(paymentTransactionDAO).updateStatus(anyString(), anyString(), anyString());

        PaymentTransaction updatedTransaction = paymentService.checkPaymentStatus(existingTransaction.getTransactionId(), existingTransaction.getExternalTransactionId());

        assertNotNull(updatedTransaction);
        assertEquals("SUCCESS", updatedTransaction.getTransactionStatus());
        verify(paymentTransactionDAO, times(1)).updateStatus(existingTransaction.getTransactionId(), "SUCCESS", "VNP12345");
    }

    @Test
    void checkPaymentStatus_transactionNotFound_throwsResourceNotFoundException() throws SQLException {
        when(paymentTransactionDAO.getById(anyString())).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.checkPaymentStatus("invalid-txn-id", "VNP12345"));
    }
}
