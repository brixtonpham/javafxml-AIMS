package com.aims.core.application.impl;

import com.aims.core.application.dtos.*;
import com.aims.core.application.services.IOrderDataLoaderService;
import com.aims.core.entities.*;
import com.aims.core.enums.*;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for OrderDataLoaderServiceImpl
 */
class OrderDataLoaderServiceImplTest {

    @Mock
    private IOrderEntityDAO orderEntityDAO;
    @Mock
    private IOrderItemDAO orderItemDAO;
    @Mock
    private IDeliveryInfoDAO deliveryInfoDAO;
    @Mock
    private IInvoiceDAO invoiceDAO;
    @Mock
    private IPaymentTransactionDAO paymentTransactionDAO;
    @Mock
    private IUserAccountDAO userAccountDAO;
    @Mock
    private IProductDAO productDAO;

    private IOrderDataLoaderService orderDataLoaderService;
    private OrderEntity testOrder;
    private UserAccount testUser;
    private List<OrderItem> testOrderItems;
    private DeliveryInfo testDeliveryInfo;
    private Invoice testInvoice;
    private List<PaymentTransaction> testPaymentTransactions;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        orderDataLoaderService = new OrderDataLoaderServiceImpl(
            orderEntityDAO,
            orderItemDAO,
            deliveryInfoDAO,
            invoiceDAO,
            paymentTransactionDAO,
            userAccountDAO,
            productDAO
        );

        setupTestData();
    }

    private void setupTestData() {
        // Create test user
        testUser = new UserAccount();
        testUser.setUserId("USER001");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setUserStatus(UserStatus.ACTIVE);

        // Create test order
        testOrder = new OrderEntity();
        testOrder.setOrderId("ORD001");
        testOrder.setUserAccount(testUser);
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        testOrder.setTotalProductPriceExclVAT(100.0f);
        testOrder.setTotalProductPriceInclVAT(110.0f);
        testOrder.setCalculatedDeliveryFee(10.0f);
        testOrder.setTotalAmountPaid(120.0f);

        // Create test products and order items
        Product testProduct1 = new Product();
        testProduct1.setProductId("PROD001");
        testProduct1.setTitle("Test Product 1");
        testProduct1.setPrice(50.0f);
        testProduct1.setImageUrl("test1.jpg");
        testProduct1.setProductType(ProductType.BOOK);

        Product testProduct2 = new Product();
        testProduct2.setProductId("PROD002");
        testProduct2.setTitle("Test Product 2");
        testProduct2.setPrice(50.0f);
        testProduct2.setImageUrl("test2.jpg");
        testProduct2.setProductType(ProductType.DVD);

        OrderItem item1 = new OrderItem(testOrder, testProduct1, 1, 50.0f, false);
        OrderItem item2 = new OrderItem(testOrder, testProduct2, 1, 50.0f, true);
        
        testOrderItems = List.of(item1, item2);

        // Create test delivery info
        testDeliveryInfo = new DeliveryInfo();
        testDeliveryInfo.setDeliveryInfoId("DINFO001");
        testDeliveryInfo.setOrderEntity(testOrder);
        testDeliveryInfo.setRecipientName("Test Recipient");
        testDeliveryInfo.setEmail("recipient@example.com");
        testDeliveryInfo.setPhoneNumber("1234567890");
        testDeliveryInfo.setDeliveryProvinceCity("Test City");
        testDeliveryInfo.setDeliveryAddress("123 Test Street");
        testDeliveryInfo.setDeliveryMethodChosen("STANDARD");

        // Create test invoice
        testInvoice = new Invoice();
        testInvoice.setInvoiceId("INV001");
        testInvoice.setOrderEntity(testOrder);
        testInvoice.setInvoiceDate(LocalDateTime.now());
        testInvoice.setInvoicedTotalAmount(120.0f);

        // Create test payment transactions
        PaymentMethod testPaymentMethod = new PaymentMethod();
        testPaymentMethod.setPaymentMethodId("PM001");
        testPaymentMethod.setMethodType(PaymentMethodType.CREDIT_CARD);

        PaymentTransaction testTransaction = new PaymentTransaction();
        testTransaction.setTransactionId("TXN001");
        testTransaction.setOrder(testOrder);
        testTransaction.setPaymentMethod(testPaymentMethod);
        testTransaction.setTransactionType(TransactionType.PAYMENT);
        testTransaction.setTransactionStatus("SUCCESS");
        testTransaction.setTransactionDateTime(LocalDateTime.now());
        testTransaction.setAmount(120.0f);

        testPaymentTransactions = List.of(testTransaction);

        // Set up relationships
        testOrder.setOrderItems(testOrderItems);
        testOrder.setDeliveryInfo(testDeliveryInfo);
        testOrder.setInvoice(testInvoice);
        testOrder.setPaymentTransactions(testPaymentTransactions);
    }

    @Test
    @DisplayName("Should load complete order data successfully")
    void testLoadCompleteOrderData_Success() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(orderEntityDAO.getById("ORD001")).thenReturn(testOrder);
        when(orderItemDAO.getItemsByOrderId("ORD001")).thenReturn(testOrderItems);
        when(deliveryInfoDAO.getByOrderId("ORD001")).thenReturn(testDeliveryInfo);
        when(invoiceDAO.getByOrderId("ORD001")).thenReturn(testInvoice);
        when(paymentTransactionDAO.getByOrderId("ORD001")).thenReturn(testPaymentTransactions);

        // Act
        OrderEntity result = orderDataLoaderService.loadCompleteOrderData("ORD001");

        // Assert
        assertNotNull(result);
        assertEquals("ORD001", result.getOrderId());
        assertNotNull(result.getOrderItems());
        assertEquals(2, result.getOrderItems().size());
        assertNotNull(result.getDeliveryInfo());
        assertNotNull(result.getInvoice());
        assertNotNull(result.getPaymentTransactions());
        assertEquals(1, result.getPaymentTransactions().size());

        // Verify all DAOs were called
        verify(orderEntityDAO).getById("ORD001");
        verify(orderItemDAO).getItemsByOrderId("ORD001");
        verify(deliveryInfoDAO).getByOrderId("ORD001");
        verify(invoiceDAO).getByOrderId("ORD001");
        verify(paymentTransactionDAO).getByOrderId("ORD001");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when order not found")
    void testLoadCompleteOrderData_OrderNotFound() throws SQLException {
        // Arrange
        when(orderEntityDAO.getById("NONEXISTENT")).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderDataLoaderService.loadCompleteOrderData("NONEXISTENT");
        });

        verify(orderEntityDAO).getById("NONEXISTENT");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for null order ID")
    void testLoadCompleteOrderData_NullOrderId() {
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderDataLoaderService.loadCompleteOrderData(null);
        });
    }

    @Test
    @DisplayName("Should create OrderSummaryDTO successfully")
    void testCreateOrderSummaryDTO_Success() throws ValidationException {
        // Act
        OrderSummaryDTO result = orderDataLoaderService.createOrderSummaryDTO(testOrder);

        // Assert
        assertNotNull(result);
        assertEquals("ORD001", result.orderId());
        assertEquals(2, result.items().size());
        assertEquals(100.0f, result.totalProductPriceExclVAT());
        assertEquals(110.0f, result.totalProductPriceInclVAT());
        assertEquals(10.0f, result.deliveryFee());
        assertEquals(120.0f, result.totalAmountToBePaid());
        assertEquals("PENDING_PAYMENT", result.orderStatus());
        assertNotNull(result.customer());
        assertEquals("testuser", result.customer().fullName());
        assertNotNull(result.paymentSummary());
        assertEquals(10.0f, result.vatAmount()); // 110 - 100
        assertFalse(result.hasRushDelivery());
        assertTrue(result.isPaid());
    }

    @Test
    @DisplayName("Should throw ValidationException for null order in DTO creation")
    void testCreateOrderSummaryDTO_NullOrder() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            orderDataLoaderService.createOrderSummaryDTO(null);
        });
    }

    @Test
    @DisplayName("Should validate order data completeness correctly")
    void testValidateOrderDataCompleteness_CompleteOrder() {
        // Act
        boolean result = orderDataLoaderService.validateOrderDataCompleteness(testOrder);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should detect incomplete order data")
    void testValidateOrderDataCompleteness_IncompleteOrder() {
        // Arrange - create order with missing items
        OrderEntity incompleteOrder = new OrderEntity();
        incompleteOrder.setOrderId("ORD002");
        incompleteOrder.setOrderDate(LocalDateTime.now());
        incompleteOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        incompleteOrder.setOrderItems(new ArrayList<>()); // Empty items

        // Act
        boolean result = orderDataLoaderService.validateOrderDataCompleteness(incompleteOrder);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for null order validation")
    void testValidateOrderDataCompleteness_NullOrder() {
        // Act
        boolean result = orderDataLoaderService.validateOrderDataCompleteness(null);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should load order with fallbacks successfully")
    void testLoadOrderWithFallbacks_Success() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(orderEntityDAO.getById("ORD001")).thenReturn(testOrder);
        when(orderItemDAO.getItemsByOrderId("ORD001")).thenReturn(testOrderItems);
        when(deliveryInfoDAO.getByOrderId("ORD001")).thenReturn(testDeliveryInfo);
        when(invoiceDAO.getByOrderId("ORD001")).thenReturn(testInvoice);
        when(paymentTransactionDAO.getByOrderId("ORD001")).thenReturn(testPaymentTransactions);

        // Act
        OrderEntity result = orderDataLoaderService.loadOrderWithFallbacks("ORD001");

        // Assert
        assertNotNull(result);
        assertEquals("ORD001", result.getOrderId());
        assertNotNull(result.getOrderItems());
        assertNotNull(result.getDeliveryInfo());
    }

    @Test
    @DisplayName("Should handle partial loading with fallbacks when complete loading fails")
    void testLoadOrderWithFallbacks_PartialLoadingWithFallbacks() throws SQLException, ResourceNotFoundException {
        // Arrange - simulate partial failure
        when(orderEntityDAO.getById("ORD001")).thenReturn(testOrder);
        when(orderItemDAO.getItemsByOrderId("ORD001")).thenThrow(new SQLException("DB Error"));
        when(deliveryInfoDAO.getByOrderId("ORD001")).thenReturn(testDeliveryInfo);
        when(invoiceDAO.getByOrderId("ORD001")).thenReturn(testInvoice);
        when(paymentTransactionDAO.getByOrderId("ORD001")).thenReturn(testPaymentTransactions);

        // Act
        OrderEntity result = orderDataLoaderService.loadOrderWithFallbacks("ORD001");

        // Assert
        assertNotNull(result);
        assertEquals("ORD001", result.getOrderId());
        assertNotNull(result.getOrderItems()); // Should be empty list as fallback
        assertTrue(result.getOrderItems().isEmpty());
    }

    @Test
    @DisplayName("Should refresh order relationships successfully")
    void testRefreshOrderRelationships_Success() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(orderItemDAO.getItemsByOrderId("ORD001")).thenReturn(testOrderItems);
        when(deliveryInfoDAO.getByOrderId("ORD001")).thenReturn(testDeliveryInfo);
        when(invoiceDAO.getByOrderId("ORD001")).thenReturn(testInvoice);
        when(paymentTransactionDAO.getByOrderId("ORD001")).thenReturn(testPaymentTransactions);

        // Act
        OrderEntity result = orderDataLoaderService.refreshOrderRelationships(testOrder);

        // Assert
        assertNotNull(result);
        assertEquals(testOrder, result);
        verify(orderItemDAO).getItemsByOrderId("ORD001");
        verify(deliveryInfoDAO).getByOrderId("ORD001");
        verify(invoiceDAO).getByOrderId("ORD001");
        verify(paymentTransactionDAO).getByOrderId("ORD001");
    }

    @Test
    @DisplayName("Should validate lazy loading initialization correctly")
    void testValidateLazyLoadingInitialization_Success() {
        // Act
        boolean result = orderDataLoaderService.validateLazyLoadingInitialization(testOrder);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for null order in lazy loading validation")
    void testValidateLazyLoadingInitialization_NullOrder() {
        // Act
        boolean result = orderDataLoaderService.validateLazyLoadingInitialization(null);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should create rush delivery details for rush orders")
    void testCreateOrderSummaryDTO_WithRushDelivery() throws ValidationException {
        // Arrange - set up rush delivery
        testDeliveryInfo.setDeliveryMethodChosen("RUSH_DELIVERY");
        testDeliveryInfo.setRequestedRushDeliveryTime(LocalDateTime.now().plusDays(1));

        // Act
        OrderSummaryDTO result = orderDataLoaderService.createOrderSummaryDTO(testOrder);

        // Assert
        assertNotNull(result);
        assertNotNull(result.rushDeliveryDetails());
        assertTrue(result.hasRushDelivery());
        assertEquals(1, result.rushDeliveryDetails().rushItems().size()); // Only one item is rush-eligible
    }

    @Test
    @DisplayName("Should handle guest orders without user account")
    void testCreateOrderSummaryDTO_GuestOrder() throws ValidationException {
        // Arrange - remove user account
        testOrder.setUserAccount(null);

        // Act
        OrderSummaryDTO result = orderDataLoaderService.createOrderSummaryDTO(testOrder);

        // Assert
        assertNotNull(result);
        assertNotNull(result.customer());
        assertTrue(result.customer().isGuest());
        assertEquals("Guest User", result.customer().fullName());
    }

    @Test
    @DisplayName("Should handle orders without payment transactions")
    void testCreateOrderSummaryDTO_NoPaymentTransactions() throws ValidationException {
        // Arrange - remove payment transactions
        testOrder.setPaymentTransactions(new ArrayList<>());

        // Act
        OrderSummaryDTO result = orderDataLoaderService.createOrderSummaryDTO(testOrder);

        // Assert
        assertNotNull(result);
        assertNotNull(result.paymentSummary());
        assertEquals("PENDING", result.paymentSummary().paymentStatus());
        assertFalse(result.isPaid());
    }

    @Test
    @DisplayName("Should handle database errors gracefully in complete loading")
    void testLoadCompleteOrderData_DatabaseError() throws SQLException {
        // Arrange
        when(orderEntityDAO.getById("ORD001")).thenThrow(new SQLException("Database connection failed"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderDataLoaderService.loadCompleteOrderData("ORD001");
        });
    }

    @Test
    @DisplayName("Should apply fallbacks for missing data fields")
    void testLoadOrderWithFallbacks_AppliesFallbacks() throws SQLException, ResourceNotFoundException {
        // Arrange - create order with missing/invalid data
        OrderEntity incompleteOrder = new OrderEntity();
        incompleteOrder.setOrderId("ORD003");
        incompleteOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        incompleteOrder.setTotalProductPriceExclVAT(-10.0f); // Invalid negative value
        incompleteOrder.setOrderDate(null); // Missing date

        when(orderEntityDAO.getById("ORD003")).thenReturn(incompleteOrder);
        when(orderItemDAO.getItemsByOrderId("ORD003")).thenReturn(new ArrayList<>());
        when(deliveryInfoDAO.getByOrderId("ORD003")).thenReturn(null);
        when(invoiceDAO.getByOrderId("ORD003")).thenReturn(null);
        when(paymentTransactionDAO.getByOrderId("ORD003")).thenReturn(new ArrayList<>());

        // Act
        OrderEntity result = orderDataLoaderService.loadOrderWithFallbacks("ORD003");

        // Assert
        assertNotNull(result);
        assertEquals("ORD003", result.getOrderId());
        assertNotNull(result.getOrderDate()); // Should be set to current time as fallback
        assertEquals(0.0f, result.getTotalProductPriceExclVAT()); // Should be corrected to 0
        assertNotNull(result.getOrderItems()); // Should be empty list
        assertNotNull(result.getPaymentTransactions()); // Should be empty list
    }
}