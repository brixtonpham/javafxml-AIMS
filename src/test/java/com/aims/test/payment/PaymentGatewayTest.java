package com.aims.test.payment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IPaymentGatewayAdapter;
import com.aims.core.application.impl.PaymentServiceImpl;
import com.aims.core.infrastructure.database.dao.*;

public class PaymentGatewayTest {
    private PaymentServiceImpl paymentService;
    private IPaymentGatewayAdapter gatewayAdapter;
    private IOrderEntityDAO orderDAO;
    private IPaymentMethodDAO paymentMethodDAO;
    private IPaymentTransactionDAO paymentTransactionDAO;
    private ICardDetailsDAO cardDetailsDAO;

    @BeforeEach
    void setUp() {
        // Initialize DAOs
        IUserAccountDAO userAccountDAO = new UserAccountDAOImpl();
        IProductDAO productDAO = new ProductDAOImpl();
        IOrderItemDAO orderItemDAO = new OrderItemDAOImpl(productDAO);
        cardDetailsDAO = new CardDetailsDAOImpl();
        
        orderDAO = new OrderEntityDAOImpl(orderItemDAO, userAccountDAO);
        paymentMethodDAO = new PaymentMethodDAOImpl(userAccountDAO, cardDetailsDAO);
        paymentTransactionDAO = new PaymentTransactionDAOImpl(orderDAO, paymentMethodDAO);
        
        // Get the configured gateway adapter
        gatewayAdapter = PaymentGatewayFactory.createConfiguredAdapter();
        
        // Initialize PaymentService
        paymentService = new PaymentServiceImpl(
            paymentTransactionDAO,
            paymentMethodDAO,
            cardDetailsDAO,
            gatewayAdapter
        );
        paymentService.setOrderDAO(orderDAO);
    }

    @Test
    void testPaymentGatewayConfiguration() {
        assertNotNull(gatewayAdapter, "Payment Gateway Adapter should not be null");
        System.out.println("✓ Payment Gateway Adapter configured successfully");
    }

    @Test
    void testProcessCreditCardPayment() throws Exception {
        // Create test order
        OrderEntity testOrder = TestDataFactory.createTestOrder();
        orderDAO.save(testOrder);

        // Create test payment method
        PaymentMethod paymentMethod = createTestPaymentMethod(PaymentMethodType.CREDIT_CARD);
        paymentMethodDAO.save(paymentMethod);

        // Process payment
        PaymentTransaction transaction = paymentService.processPayment(testOrder, paymentMethod.getPaymentMethodId());
        
        assertNotNull(transaction, "Transaction should not be null");
        assertNotNull(transaction.getTransactionId(), "Transaction ID should not be null");
        assertNotNull(transaction.getExternalTransactionId(), "External Transaction ID should not be null");
        assertEquals("SUCCESS", transaction.getTransactionStatus(), "Transaction status should be SUCCESS");
    }

    @Test
    void testProcessDomesticCardPayment() throws Exception {
        // Create test order
        OrderEntity testOrder = TestDataFactory.createTestOrder();
        orderDAO.save(testOrder);

        // Create test payment method
        PaymentMethod paymentMethod = createTestPaymentMethod(PaymentMethodType.DOMESTIC_DEBIT_CARD);
        paymentMethodDAO.save(paymentMethod);

        // Create test parameters including bank code
        Map<String, Object> clientParams = new HashMap<>();
        clientParams.put("bankCode", "TEST_BANK");

        // Process payment
        PaymentTransaction transaction = paymentService.processPaymentWithParams(
            testOrder, 
            paymentMethod.getPaymentMethodId(),
            clientParams
        );
        
        assertNotNull(transaction, "Transaction should not be null");
        assertNotNull(transaction.getTransactionId(), "Transaction ID should not be null");
        assertNotNull(transaction.getExternalTransactionId(), "External Transaction ID should not be null");
        assertEquals("SUCCESS", transaction.getTransactionStatus(), "Transaction status should be SUCCESS");
    }

    private PaymentMethod createTestPaymentMethod(PaymentMethodType type) {
        PaymentMethod method = new PaymentMethod();
        method.setPaymentMethodId("TEST_" + UUID.randomUUID().toString());
        method.setMethodType(type);
        method.setUserAccount(null); // Temporary method for testing
        method.setDefault(false);
        return method;
    }
}