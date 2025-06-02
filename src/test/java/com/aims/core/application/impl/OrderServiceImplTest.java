package com.aims.core.application.impl;

import com.aims.core.application.services.*;
import com.aims.core.entities.*;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.ProductType;
import com.aims.core.enums.TransactionType;
import com.aims.core.enums.UserStatus;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.shared.exceptions.*;
import com.aims.core.shared.utils.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    @Mock
    private IOrderEntityDAO orderDAO;
    @Mock
    private IOrderItemDAO orderItemDAO;
    @Mock
    private IDeliveryInfoDAO deliveryInfoDAO;
    @Mock
    private IInvoiceDAO invoiceDAO;
    @Mock
    private IProductDAO productDAO;
    @Mock
    private IProductService productService;
    @Mock
    private ICartService cartService;
    @Mock
    private IPaymentService paymentService;
    @Mock
    private IDeliveryCalculationService deliveryCalculationService;
    @Mock
    private INotificationService notificationService;
    @Mock
    private IUserAccountDAO userAccountDAO;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Cart mockCart;
    private Product mockProduct1;
    private Product mockProduct2;
    private UserAccount mockUser;
    private OrderEntity mockOrder;
    private DeliveryInfo mockDeliveryInfoInput;
    private PaymentTransaction mockPaymentTransaction;
    private Invoice mockInvoice;

    private static final float VAT_RATE = 0.10f;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);

        mockUser = new UserAccount("user1", "testuser", "password", "user@example.com", UserStatus.ACTIVE);
        when(userAccountDAO.getById("user1")).thenReturn(mockUser);

        mockProduct1 = new Product("prod1", "Book 1", "Books", 100f, 50f, 10, "Desc1", "img1.jpg", "barcode1", "10x10x2", 0.5f, LocalDate.now(), ProductType.BOOK);
        mockProduct2 = new Product("prod2", "CD 1", "Music", 80f, 40f, 5, "Desc2", "img2.jpg", "barcode2", "12x12x1", 0.2f, LocalDate.now(), ProductType.CD);

        when(productDAO.getById("prod1")).thenReturn(mockProduct1);
        when(productDAO.getById("prod2")).thenReturn(mockProduct2);

        mockCart = new Cart("cartSession1", mockUser, LocalDateTime.now());
        CartItem cartItem1 = new CartItem(mockCart, mockProduct1, 2);
        CartItem cartItem2 = new CartItem(mockCart, mockProduct2, 1);
        mockCart.setItems(List.of(cartItem1, cartItem2));

        mockOrder = new OrderEntity();
        mockOrder.setOrderId("order1");
        mockOrder.setUserAccount(mockUser);
        mockOrder.setOrderDate(LocalDateTime.now());
        mockOrder.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);
        mockOrder.setTotalProductPriceExclVAT( (mockProduct1.getPrice() * 2) + (mockProduct2.getPrice() * 1) ); // 100 + 40 = 140
        mockOrder.setTotalProductPriceInclVAT(mockOrder.getTotalProductPriceExclVAT() * (1 + VAT_RATE)); // 140 * 1.1 = 154

        OrderItem orderItem1 = new OrderItem(mockOrder, mockProduct1, 2, mockProduct1.getPrice(), true);
        OrderItem orderItem2 = new OrderItem(mockOrder, mockProduct2, 1, mockProduct2.getPrice(), false);
        mockOrder.setOrderItems(new ArrayList<>(List.of(orderItem1, orderItem2))); // Mutable list

        mockDeliveryInfoInput = new DeliveryInfo();
        mockDeliveryInfoInput.setRecipientName("Test Recipient");
        mockDeliveryInfoInput.setPhoneNumber("1234567890");
        mockDeliveryInfoInput.setDeliveryAddress("123 Test St");
        mockDeliveryInfoInput.setDeliveryProvinceCity("Hanoi");
        mockDeliveryInfoInput.setEmail("test@example.com");

        mockPaymentTransaction = new PaymentTransaction("txn1", mockOrder, null, TransactionType.PAYMENT, "extTxn1", "SUCCESS", LocalDateTime.now(), 174f, "Payment successful");
        mockInvoice = new Invoice("inv1", mockOrder, LocalDateTime.now(), 174f);

        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        when(orderItemDAO.getItemsByOrderId("order1")).thenReturn(mockOrder.getOrderItems());
        when(deliveryInfoDAO.getByOrderId("order1")).thenReturn(mockDeliveryInfoInput);
        when(invoiceDAO.getByOrderId("order1")).thenReturn(mockInvoice);
    }

    @Test
    void initiateOrderFromCart_success() throws SQLException, ResourceNotFoundException, InventoryException, ValidationException {
        when(cartService.getCart("cartSession1")).thenReturn(mockCart);
        when(productDAO.getById("prod1")).thenReturn(mockProduct1); 
        when(productDAO.getById("prod2")).thenReturn(mockProduct2);
        doNothing().when(orderDAO).add(any(OrderEntity.class));
        doNothing().when(orderItemDAO).add(any(OrderItem.class));

        OrderEntity createdOrder = orderService.initiateOrderFromCart("cartSession1", "user1");

        assertNotNull(createdOrder);
        assertTrue(createdOrder.getOrderId().startsWith("ORD-"));
        assertEquals(OrderStatus.PENDING_DELIVERY_INFO, createdOrder.getOrderStatus());
        assertEquals(mockUser, createdOrder.getUserAccount());
        assertEquals(2, createdOrder.getOrderItems().size());
        assertEquals(140f, createdOrder.getTotalProductPriceExclVAT(), 0.01f);
        assertEquals(154f, createdOrder.getTotalProductPriceInclVAT(), 0.01f);

        verify(orderDAO, times(1)).add(any(OrderEntity.class));
        verify(orderItemDAO, times(2)).add(any(OrderItem.class));
    }

    @Test
    void initiateOrderFromCart_emptyCart_throwsResourceNotFound() throws SQLException {
        Cart emptyCart = new Cart("emptyCartSess", null, LocalDateTime.now());
        emptyCart.setItems(Collections.emptyList());
        when(cartService.getCart("emptyCartSess")).thenReturn(emptyCart);

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.initiateOrderFromCart("emptyCartSess", null);
        });
    }
    
    @Test
    void initiateOrderFromCart_productNotFound_throwsResourceNotFound() throws SQLException, ResourceNotFoundException {
        when(cartService.getCart("cartSession1")).thenReturn(mockCart);
        when(productDAO.getById("prod1")).thenReturn(null); // Product 1 not found

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.initiateOrderFromCart("cartSession1", "user1");
        });
    }

    @Test
    void initiateOrderFromCart_insufficientStock_throwsInventoryException() throws SQLException, ResourceNotFoundException {
        mockProduct1.setQuantityInStock(1); // Only 1 in stock, cart wants 2
        when(cartService.getCart("cartSession1")).thenReturn(mockCart);
        when(productDAO.getById("prod1")).thenReturn(mockProduct1);

        assertThrows(InventoryException.class, () -> {
            orderService.initiateOrderFromCart("cartSession1", "user1");
        });
        mockProduct1.setQuantityInStock(10); // Reset stock
    }


    @Test
    void setDeliveryInformation_success_standardOrder() throws SQLException, ResourceNotFoundException, ValidationException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        when(deliveryCalculationService.calculateShippingFee(any(OrderEntity.class), eq(false))).thenReturn(20f);
        when(deliveryInfoDAO.getByOrderId("order1")).thenReturn(null); // No existing delivery info
        doNothing().when(deliveryInfoDAO).add(any(DeliveryInfo.class));
        doNothing().when(orderDAO).update(any(OrderEntity.class));

        OrderEntity updatedOrder = orderService.setDeliveryInformation("order1", mockDeliveryInfoInput, false);

        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.PENDING_PAYMENT, updatedOrder.getOrderStatus());
        assertEquals(20f, updatedOrder.getCalculatedDeliveryFee(), 0.01f);
        assertEquals(mockOrder.getTotalProductPriceInclVAT() + 20f, updatedOrder.getTotalAmountPaid(), 0.01f);
        assertNotNull(updatedOrder.getDeliveryInfo());
        assertEquals("STANDARD", updatedOrder.getDeliveryInfo().getDeliveryMethodChosen());

        verify(deliveryInfoDAO, times(1)).add(any(DeliveryInfo.class));
        verify(orderDAO, times(1)).update(any(OrderEntity.class));
    }

    @Test
    void setDeliveryInformation_success_rushOrderEligible() throws SQLException, ResourceNotFoundException, ValidationException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);
        mockOrder.getOrderItems().get(0).setEligibleForRushDelivery(true);

        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        when(deliveryCalculationService.isRushDeliveryAddressEligible(any(DeliveryInfo.class))).thenReturn(true);
        when(deliveryCalculationService.calculateShippingFee(any(OrderEntity.class), eq(true))).thenReturn(50f); 
        when(deliveryInfoDAO.getByOrderId("order1")).thenReturn(null);
        doNothing().when(deliveryInfoDAO).add(any(DeliveryInfo.class));
        doNothing().when(orderDAO).update(any(OrderEntity.class));

        OrderEntity updatedOrder = orderService.setDeliveryInformation("order1", mockDeliveryInfoInput, true);

        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.PENDING_PAYMENT, updatedOrder.getOrderStatus());
        assertEquals(50f, updatedOrder.getCalculatedDeliveryFee(), 0.01f);
        assertEquals("RUSH_DELIVERY", updatedOrder.getDeliveryInfo().getDeliveryMethodChosen());
        verify(deliveryInfoDAO, times(1)).add(any(DeliveryInfo.class));
        verify(orderDAO, times(1)).update(any(OrderEntity.class));
    }
    
    @Test
    void setDeliveryInformation_rushOrder_addressNotEligible_throwsValidationException() throws SQLException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        when(deliveryCalculationService.isRushDeliveryAddressEligible(any(DeliveryInfo.class))).thenReturn(false);

        assertThrows(ValidationException.class, () -> {
            orderService.setDeliveryInformation("order1", mockDeliveryInfoInput, true);
        });
    }

    @Test
    void setDeliveryInformation_rushOrder_noItemsEligible_throwsValidationException() throws SQLException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);
        mockOrder.getOrderItems().forEach(item -> item.setEligibleForRushDelivery(false)); 
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        when(deliveryCalculationService.isRushDeliveryAddressEligible(any(DeliveryInfo.class))).thenReturn(true); 

        assertThrows(ValidationException.class, () -> {
            orderService.setDeliveryInformation("order1", mockDeliveryInfoInput, true);
        });
    }
    
    @Test
    void setDeliveryInformation_orderNotFound_throwsResourceNotFound() throws SQLException {
        when(orderDAO.getById("unknownOrder")).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.setDeliveryInformation("unknownOrder", mockDeliveryInfoInput, false);
        });
    }

    @Test
    void setDeliveryInformation_invalidOrderStatus_throwsValidationException() throws SQLException {
        mockOrder.setOrderStatus(OrderStatus.SHIPPING); 
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        assertThrows(ValidationException.class, () -> {
            orderService.setDeliveryInformation("order1", mockDeliveryInfoInput, false);
        });
    }
    
    @Test
    void setDeliveryInformation_nullDeliveryInfo_throwsValidationException() throws SQLException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        assertThrows(ValidationException.class, () -> {
            orderService.setDeliveryInformation("order1", null, false);
        });
    }

    @Test
    void calculateShippingFee_success() throws SQLException, ResourceNotFoundException, ValidationException {
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        when(deliveryCalculationService.calculateShippingFee(mockOrder, false)).thenReturn(25.50f);

        float fee = orderService.calculateShippingFee("order1", mockDeliveryInfoInput, false);
        assertEquals(25.50f, fee, 0.01f);
    }

    @Test
    void processPayment_success() throws SQLException, ResourceNotFoundException, ValidationException, PaymentException, InventoryException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        mockOrder.setDeliveryInfo(mockDeliveryInfoInput); 
        mockOrder.setCalculatedDeliveryFee(20f);
        mockOrder.setTotalAmountPaid(mockOrder.getTotalProductPriceInclVAT() + 20f); // 154 + 20 = 174

        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        when(productDAO.getById("prod1")).thenReturn(mockProduct1); 
        when(productDAO.getById("prod2")).thenReturn(mockProduct2);
        when(paymentService.processPayment(eq(mockOrder), eq("pm_method1"))).thenReturn(mockPaymentTransaction);
        // Corrected: productService.updateProductStock returns Product
        when(productService.updateProductStock(eq("prod1"), eq(-2))).thenReturn(mockProduct1);
        when(productService.updateProductStock(eq("prod2"), eq(-1))).thenReturn(mockProduct2);
        doNothing().when(invoiceDAO).add(any(Invoice.class));
        doNothing().when(orderDAO).update(any(OrderEntity.class)); 
        doNothing().when(notificationService).sendOrderConfirmationEmail(any(OrderEntity.class), any(Invoice.class), any(PaymentTransaction.class));

        OrderEntity processedOrder = orderService.processPayment("order1", "pm_method1");

        assertNotNull(processedOrder);
        assertEquals(OrderStatus.PENDING_PROCESSING, processedOrder.getOrderStatus());
        assertNotNull(processedOrder.getInvoice());
        assertTrue(processedOrder.getInvoice().getInvoiceId().startsWith("INV-"));
        assertEquals(mockPaymentTransaction.getAmount(), processedOrder.getInvoice().getInvoicedTotalAmount());

        verify(productService, times(1)).updateProductStock("prod1", -2);
        verify(productService, times(1)).updateProductStock("prod2", -1);
        verify(invoiceDAO, times(1)).add(any(Invoice.class));
        verify(orderDAO, times(1)).update(processedOrder);
        verify(notificationService, times(1)).sendOrderConfirmationEmail(eq(processedOrder), any(Invoice.class), eq(mockPaymentTransaction));
    }

    @Test
    void processPayment_orderNotPendingPayment_throwsValidationException() throws SQLException {
        mockOrder.setOrderStatus(OrderStatus.SHIPPING); 
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        assertThrows(ValidationException.class, () -> {
            orderService.processPayment("order1", "pm_method1");
        });
    }
    
    @Test
    void processPayment_deliveryInfoNotSet_throwsValidationException() throws SQLException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        mockOrder.setDeliveryInfo(null); 
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        assertThrows(ValidationException.class, () -> {
            orderService.processPayment("order1", "pm_method1");
        });
    }

    @Test
    void processPayment_inventoryChanged_throwsInventoryException() throws SQLException, ResourceNotFoundException, PaymentException, ValidationException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        mockOrder.setDeliveryInfo(mockDeliveryInfoInput);
        mockProduct1.setQuantityInStock(0); 

        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        when(productDAO.getById("prod1")).thenReturn(mockProduct1); 

        assertThrows(InventoryException.class, () -> {
            orderService.processPayment("order1", "pm_method1");
        });
        verify(orderDAO, times(1)).updateStatus("order1", OrderStatus.PENDING_DELIVERY_INFO); 
        mockProduct1.setQuantityInStock(10); 
    }

    @Test
    void processPayment_paymentFailed_throwsPaymentException_updatesStatus() throws SQLException, ResourceNotFoundException, ValidationException, PaymentException, InventoryException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        mockOrder.setDeliveryInfo(mockDeliveryInfoInput);
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        when(productDAO.getById(anyString())).thenReturn(mockProduct1); 
        when(paymentService.processPayment(any(OrderEntity.class), anyString())).thenThrow(new PaymentException("Gateway error"));

        assertThrows(PaymentException.class, () -> {
            orderService.processPayment("order1", "pm_method1");
        });
        verify(orderDAO, times(1)).updateStatus("order1", OrderStatus.PAYMENT_FAILED);
    }
    
    @Test
    void processPayment_stockUpdateFailed_throwsInventoryException() throws SQLException, ResourceNotFoundException, ValidationException, PaymentException, InventoryException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        mockOrder.setDeliveryInfo(mockDeliveryInfoInput);
        mockOrder.setCalculatedDeliveryFee(20f);
        mockOrder.setTotalAmountPaid(mockOrder.getTotalProductPriceInclVAT() + 20f);

        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        when(productDAO.getById(anyString())).thenReturn(mockProduct1); 
        when(paymentService.processPayment(eq(mockOrder), eq("pm_method1"))).thenReturn(mockPaymentTransaction); 
        doThrow(new ResourceNotFoundException("Product not found for stock update")).when(productService).updateProductStock(eq("prod1"), anyInt()); 

        assertThrows(InventoryException.class, () -> {
            orderService.processPayment("order1", "pm_method1");
        });
        verify(orderDAO, times(1)).updateStatus(eq("order1"), eq(OrderStatus.PAYMENT_FAILED)); 
    }


    @Test
    void getOrderDetails_success() throws SQLException, ResourceNotFoundException {
        OrderEntity foundOrder = orderService.getOrderDetails("order1");
        assertNotNull(foundOrder);
        assertEquals("order1", foundOrder.getOrderId());
        assertNotNull(foundOrder.getOrderItems());
        assertFalse(foundOrder.getOrderItems().isEmpty());
        assertNotNull(foundOrder.getDeliveryInfo());
        assertNotNull(foundOrder.getInvoice());
    }
    
    @Test
    void getOrderDetails_orderNotFound_throwsResourceNotFound() throws SQLException {
        when(orderDAO.getById("unknownOrder")).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrderDetails("unknownOrder");
        });
    }

    @Test
    void getOrdersByUserId_success() throws SQLException {
        List<OrderEntity> userOrders = List.of(mockOrder);
        when(orderDAO.getByUserId("user1")).thenReturn(userOrders);
        List<OrderEntity> foundOrders = orderService.getOrdersByUserId("user1");
        assertEquals(1, foundOrders.size());
        assertEquals("order1", foundOrders.get(0).getOrderId());
    }

    @Test
    void cancelOrder_success_pendingProcessing_noPayment() throws SQLException, ResourceNotFoundException, ValidationException, PaymentException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PROCESSING);
        mockOrder.setUserAccount(mockUser); 
        mockOrder.setPaymentTransactions(new ArrayList<>()); 
        when(orderDAO.getById("order1")).thenReturn(mockOrder); 
        when(orderItemDAO.getItemsByOrderId("order1")).thenReturn(mockOrder.getOrderItems());
        when(deliveryInfoDAO.getByOrderId("order1")).thenReturn(mockDeliveryInfoInput);

        // Corrected: productService.updateProductStock returns Product
        // Assuming the products from mockOrder.getOrderItems() are mockProduct1 and mockProduct2
        when(productService.updateProductStock(eq(mockProduct1.getProductId()), eq(mockOrder.getOrderItems().get(0).getQuantity()))).thenReturn(mockProduct1);
        when(productService.updateProductStock(eq(mockProduct2.getProductId()), eq(mockOrder.getOrderItems().get(1).getQuantity()))).thenReturn(mockProduct2);
        doNothing().when(orderDAO).updateStatus("order1", OrderStatus.CANCELLED);
        doNothing().when(notificationService).sendOrderCancellationNotification(any(OrderEntity.class), isNull());

        OrderEntity cancelledOrder = orderService.cancelOrder("order1", "user1");

        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getOrderStatus());
        verify(productService, times(mockOrder.getOrderItems().size())).updateProductStock(anyString(), anyInt()); 
        verify(paymentService, never()).processRefund(anyString(), anyString(), anyFloat(), anyString());
        verify(notificationService).sendOrderCancellationNotification(cancelledOrder, null);
    }

    @Test
    void cancelOrder_success_withRefund() throws SQLException, ResourceNotFoundException, ValidationException, PaymentException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PROCESSING);
        mockOrder.setUserAccount(mockUser);
        PaymentTransaction successfulPayment = new PaymentTransaction("pt1", mockOrder, null, TransactionType.PAYMENT, "extId1", "SUCCESS", LocalDateTime.now(), 174f, "Paid");
        mockOrder.setPaymentTransactions(List.of(successfulPayment)); 

        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        when(orderItemDAO.getItemsByOrderId("order1")).thenReturn(mockOrder.getOrderItems());
        when(deliveryInfoDAO.getByOrderId("order1")).thenReturn(mockDeliveryInfoInput);
        when(invoiceDAO.getByOrderId("order1")).thenReturn(mockInvoice); 

        PaymentTransaction refundTxn = new PaymentTransaction("ref1", mockOrder, null, TransactionType.REFUND, "extRef1", "SUCCESS", LocalDateTime.now(), 174f, "Refunded");
        when(paymentService.processRefund(eq("order1"), eq("extId1"), eq(174f), anyString())).thenReturn(refundTxn);
        // Corrected: productService.updateProductStock returns Product
        when(productService.updateProductStock(eq(mockProduct1.getProductId()), eq(mockOrder.getOrderItems().get(0).getQuantity()))).thenReturn(mockProduct1);
        when(productService.updateProductStock(eq(mockProduct2.getProductId()), eq(mockOrder.getOrderItems().get(1).getQuantity()))).thenReturn(mockProduct2);
        doNothing().when(orderDAO).updateStatus("order1", OrderStatus.CANCELLED);
        doNothing().when(notificationService).sendOrderCancellationNotification(any(OrderEntity.class), any(PaymentTransaction.class));

        OrderEntity cancelledOrder = orderService.cancelOrder("order1", "user1");

        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getOrderStatus());
        verify(paymentService).processRefund(eq("order1"), eq("extId1"), eq(174f), anyString());
        verify(productService, times(mockOrder.getOrderItems().size())).updateProductStock(anyString(), anyInt());
        verify(notificationService).sendOrderCancellationNotification(cancelledOrder, refundTxn);
    }
    
    @Test
    void cancelOrder_unauthorizedUser_throwsValidationException() throws SQLException, ResourceNotFoundException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PROCESSING);
        mockOrder.setUserAccount(mockUser); 
        when(orderDAO.getById("order1")).thenReturn(mockOrder); 

        assertThrows(ValidationException.class, () -> {
            orderService.cancelOrder("order1", "user2"); 
        });
    }

    @Test
    void cancelOrder_invalidStatus_throwsValidationException() throws SQLException, ResourceNotFoundException {
        mockOrder.setOrderStatus(OrderStatus.SHIPPING); 
        mockOrder.setUserAccount(mockUser);
        when(orderDAO.getById("order1")).thenReturn(mockOrder);

        assertThrows(ValidationException.class, () -> {
            orderService.cancelOrder("order1", "user1");
        });
    }

    @Test
    void getOrdersByStatusForManager_success() throws SQLException {
        List<OrderEntity> pendingOrders = List.of(mockOrder);
        when(orderDAO.getByStatus(OrderStatus.PENDING_PROCESSING)).thenReturn(pendingOrders);

        SearchResult<OrderEntity> result = orderService.getOrdersByStatusForManager(OrderStatus.PENDING_PROCESSING, 1, 10);
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getResults().size());
        assertEquals(mockOrder, result.getResults().get(0));
    }
    
    @Test
    void getOrdersByStatusForManager_pagination() throws SQLException {
        List<OrderEntity> orders = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            OrderEntity o = new OrderEntity();
            o.setOrderId("order" + i);
            o.setOrderStatus(OrderStatus.APPROVED);
            orders.add(o);
        }
        when(orderDAO.getByStatus(OrderStatus.APPROVED)).thenReturn(orders);

        SearchResult<OrderEntity> result1 = orderService.getOrdersByStatusForManager(OrderStatus.APPROVED, 1, 10);
        assertEquals(15, result1.getTotalCount());
        assertEquals(10, result1.getResults().size());
        assertEquals("order0", result1.getResults().get(0).getOrderId());

        SearchResult<OrderEntity> result2 = orderService.getOrdersByStatusForManager(OrderStatus.APPROVED, 2, 10);
        assertEquals(15, result2.getTotalCount());
        assertEquals(5, result2.getResults().size());
        assertEquals("order10", result2.getResults().get(0).getOrderId());
    }


    @Test
    void approveOrder_success() throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PROCESSING);
        when(orderDAO.getById("order1")).thenReturn(mockOrder); 
        when(productDAO.getById(anyString())).thenReturn(mockProduct1); 
        mockProduct1.setQuantityInStock(100); 

        doNothing().when(orderDAO).updateStatus("order1", OrderStatus.APPROVED);
        doNothing().when(notificationService).sendOrderStatusUpdateNotification(any(OrderEntity.class), anyString(), anyString(), anyString());

        OrderEntity approvedOrder = orderService.approveOrder("order1", "manager1");

        assertEquals(OrderStatus.APPROVED, approvedOrder.getOrderStatus());
        verify(orderDAO).updateStatus("order1", OrderStatus.APPROVED);
        verify(notificationService).sendOrderStatusUpdateNotification(eq(approvedOrder), eq(OrderStatus.PENDING_PROCESSING.name()), eq(OrderStatus.APPROVED.name()), anyString());
    }
    
    @Test
    void approveOrder_invalidStatus_throwsValidationException() throws SQLException, ResourceNotFoundException {
        mockOrder.setOrderStatus(OrderStatus.APPROVED); 
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        assertThrows(ValidationException.class, () -> {
            orderService.approveOrder("order1", "manager1");
        });
    }

    @Test
    void approveOrder_insufficientStock_throwsInventoryException() throws SQLException, ResourceNotFoundException, ValidationException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PROCESSING);
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        mockProduct1.setQuantityInStock(1); 
        when(productDAO.getById("prod1")).thenReturn(mockProduct1);

        assertThrows(InventoryException.class, () -> {
            orderService.approveOrder("order1", "manager1");
        });
        verify(orderDAO).updateStatus("order1", OrderStatus.REJECTED); 
    }


    @Test
    void rejectOrder_success_noPayment() throws SQLException, ResourceNotFoundException, ValidationException, PaymentException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PROCESSING);
        mockOrder.setPaymentTransactions(new ArrayList<>()); 
        when(orderDAO.getById("order1")).thenReturn(mockOrder);

        doNothing().when(orderDAO).updateStatus("order1", OrderStatus.REJECTED);
        doNothing().when(notificationService).sendOrderStatusUpdateNotification(any(OrderEntity.class), anyString(), anyString(), anyString());

        OrderEntity rejectedOrder = orderService.rejectOrder("order1", "manager1", "Reason for rejection");

        assertEquals(OrderStatus.REJECTED, rejectedOrder.getOrderStatus());
        verify(paymentService, never()).processRefund(anyString(), anyString(), anyFloat(), anyString());
        verify(productService, never()).updateProductStock(anyString(), anyInt()); 
        verify(orderDAO).updateStatus("order1", OrderStatus.REJECTED);
        verify(notificationService).sendOrderStatusUpdateNotification(eq(rejectedOrder), eq(OrderStatus.PENDING_PROCESSING.name()), eq(OrderStatus.REJECTED.name()), anyString());
    }

    @Test
    void rejectOrder_success_withRefundAndStockRestore() throws SQLException, ResourceNotFoundException, ValidationException, PaymentException {
        mockOrder.setOrderStatus(OrderStatus.PENDING_PROCESSING);
        PaymentTransaction successfulPayment = new PaymentTransaction("pt1", mockOrder, null, TransactionType.PAYMENT, "extId1", "SUCCESS", LocalDateTime.now(), 174f, "Paid");
        mockOrder.setPaymentTransactions(List.of(successfulPayment)); 

        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        PaymentTransaction refundTxn = new PaymentTransaction("ref1", mockOrder, null, TransactionType.REFUND, "extRef1", "SUCCESS", LocalDateTime.now(), 174f, "Refunded");
        when(paymentService.processRefund(eq("order1"), eq("extId1"), eq(174f), anyString())).thenReturn(refundTxn);
        // Corrected: productService.updateProductStock returns Product
        when(productService.updateProductStock(eq(mockProduct1.getProductId()), eq(mockOrder.getOrderItems().get(0).getQuantity()))).thenReturn(mockProduct1);
        when(productService.updateProductStock(eq(mockProduct2.getProductId()), eq(mockOrder.getOrderItems().get(1).getQuantity()))).thenReturn(mockProduct2);
        doNothing().when(orderDAO).updateStatus("order1", OrderStatus.REJECTED);
        doNothing().when(notificationService).sendOrderStatusUpdateNotification(any(OrderEntity.class), anyString(), anyString(), anyString());

        OrderEntity rejectedOrder = orderService.rejectOrder("order1", "manager1", "Item unavailable");

        assertEquals(OrderStatus.REJECTED, rejectedOrder.getOrderStatus());
        verify(paymentService).processRefund(eq("order1"), eq("extId1"), eq(174f), anyString());
        verify(productService, times(mockOrder.getOrderItems().size())).updateProductStock(anyString(), anyInt()); 
        verify(orderDAO).updateStatus("order1", OrderStatus.REJECTED);
        verify(notificationService).sendOrderStatusUpdateNotification(eq(rejectedOrder), eq(OrderStatus.PENDING_PROCESSING.name()), eq(OrderStatus.REJECTED.name()), anyString());
    }
    
    @Test
    void rejectOrder_invalidStatus_throwsValidationException() throws SQLException, ResourceNotFoundException {
        mockOrder.setOrderStatus(OrderStatus.SHIPPING); 
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        assertThrows(ValidationException.class, () -> {
            orderService.rejectOrder("order1", "manager1", "Too late");
        });
    }


    @Test
    void updateOrderStatus_success() throws SQLException, ResourceNotFoundException, ValidationException {
        mockOrder.setOrderStatus(OrderStatus.APPROVED);
        when(orderDAO.getById("order1")).thenReturn(mockOrder);
        doNothing().when(orderDAO).updateStatus("order1", OrderStatus.SHIPPING);
        doNothing().when(notificationService).sendOrderStatusUpdateNotification(any(OrderEntity.class), anyString(), anyString(), anyString());

        OrderEntity updatedOrder = orderService.updateOrderStatus("order1", OrderStatus.SHIPPING, "adminUser");

        assertEquals(OrderStatus.SHIPPING, updatedOrder.getOrderStatus());
        verify(orderDAO).updateStatus("order1", OrderStatus.SHIPPING);
        verify(notificationService).sendOrderStatusUpdateNotification(eq(updatedOrder), eq(OrderStatus.APPROVED.name()), eq(OrderStatus.SHIPPING.name()), anyString());
    }
    
    @Test
    void updateOrderStatus_sameStatus_noAction() throws SQLException, ResourceNotFoundException, ValidationException {
        mockOrder.setOrderStatus(OrderStatus.APPROVED);
        when(orderDAO.getById("order1")).thenReturn(mockOrder);

        OrderEntity updatedOrder = orderService.updateOrderStatus("order1", OrderStatus.APPROVED, "adminUser");
        
        assertEquals(OrderStatus.APPROVED, updatedOrder.getOrderStatus()); 
        verify(orderDAO, never()).updateStatus(anyString(), any(OrderStatus.class)); 
        verify(notificationService, never()).sendOrderStatusUpdateNotification(any(), any(), any(), any()); 
    }
}
