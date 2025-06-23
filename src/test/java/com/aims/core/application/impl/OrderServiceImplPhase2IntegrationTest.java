package com.aims.core.application.impl;

import com.aims.core.application.services.*;
import com.aims.core.entities.*;
import com.aims.core.enums.OrderStatus;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.shared.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl with Phase 2 service integrations
 * Tests the integration of StockValidationService and OrderStateManagementService
 */
public class OrderServiceImplPhase2IntegrationTest {

    @Mock private IOrderEntityDAO orderEntityDAO;
    @Mock private IOrderItemDAO orderItemDAO;
    @Mock private IDeliveryInfoDAO deliveryInfoDAO;
    @Mock private IInvoiceDAO invoiceDAO;
    @Mock private IProductDAO productDAO;
    @Mock private IProductService productService;
    @Mock private ICartService cartService;
    @Mock private IPaymentService paymentService;
    @Mock private IDeliveryCalculationService deliveryCalculationService;
    @Mock private INotificationService notificationService;
    @Mock private IUserAccountDAO userAccountDAO;
    @Mock private IOrderDataLoaderService orderDataLoaderService;
    @Mock private IStockValidationService stockValidationService;
    @Mock private IOrderStateManagementService orderStateManagementService;

    private OrderServiceImpl orderService;
    private OrderEntity testOrder;
    private Product testProduct;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        orderService = new OrderServiceImpl(
            orderEntityDAO, orderItemDAO, deliveryInfoDAO, invoiceDAO, productDAO,
            productService, cartService, paymentService, deliveryCalculationService,
            notificationService, userAccountDAO, orderDataLoaderService,
            stockValidationService, orderStateManagementService
        );

        // Setup test data
        testProduct = new Product();
        testProduct.setProductId("TEST-PROD-001");
        testProduct.setTitle("Test Product");
        testProduct.setPrice(100.0f);
        testProduct.setQuantityInStock(10);

        testOrder = new OrderEntity();
        testOrder.setOrderId("TEST-ORDER-001");
        testOrder.setOrderStatus(OrderStatus.PENDING_PROCESSING);
        testOrder.setOrderDate(LocalDateTime.now());

        testOrderItem = new OrderItem();
        testOrderItem.setProduct(testProduct);
        testOrderItem.setQuantity(2);
        testOrderItem.setPriceAtTimeOfOrder(100.0f);

        testOrder.setOrderItems(Arrays.asList(testOrderItem));
    }

    @Test
    @DisplayName("Approve Order - Success with Phase 2 Integration")
    void testApproveOrder_Success_WithPhase2Integration() throws Exception {
        // Arrange
        String orderId = "TEST-ORDER-001";
        String managerId = "MANAGER-001";
        
        IOrderStateManagementService.OrderApprovalResult approvalResult = 
            mock(IOrderStateManagementService.OrderApprovalResult.class);
        when(approvalResult.isSuccess()).thenReturn(true);
        when(approvalResult.getMessage()).thenReturn("Order approved successfully");
        
        when(orderStateManagementService.approveOrder(eq(orderId), eq(managerId), anyString()))
            .thenReturn(approvalResult);

        // Act & Assert
        assertDoesNotThrow(() -> {
            orderService.approveOrder(orderId, managerId);
        });

        // Verify that OrderStateManagementService was called
        verify(orderStateManagementService, times(1))
            .approveOrder(eq(orderId), eq(managerId), anyString());
    }

    @Test
    @DisplayName("Approve Order - Failure with Phase 2 Integration")
    void testApproveOrder_Failure_WithPhase2Integration() throws Exception {
        // Arrange
        String orderId = "TEST-ORDER-001";
        String managerId = "MANAGER-001";
        
        IOrderStateManagementService.OrderApprovalResult approvalResult = 
            mock(IOrderStateManagementService.OrderApprovalResult.class);
        when(approvalResult.isSuccess()).thenReturn(false);
        when(approvalResult.getMessage()).thenReturn("Insufficient stock");
        when(approvalResult.getValidationIssues()).thenReturn(Arrays.asList("Product TEST-PROD-001: Insufficient stock"));
        
        when(orderStateManagementService.approveOrder(eq(orderId), eq(managerId), anyString()))
            .thenReturn(approvalResult);

        // Act & Assert
        OrderException exception = assertThrows(OrderException.class, () -> {
            orderService.approveOrder(orderId, managerId);
        });

        assertTrue(exception.getMessage().contains("Order approval failed"));
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    @DisplayName("Reject Order - Success with Phase 2 Integration")
    void testRejectOrder_Success_WithPhase2Integration() throws Exception {
        // Arrange
        String orderId = "TEST-ORDER-001";
        String managerId = "MANAGER-001";
        String reason = "Quality issues";
        
        IOrderStateManagementService.StateTransitionResult rejectionResult = 
            mock(IOrderStateManagementService.StateTransitionResult.class);
        when(rejectionResult.isSuccess()).thenReturn(true);
        when(rejectionResult.getMessage()).thenReturn("Order rejected successfully");
        
        when(orderStateManagementService.rejectOrder(eq(orderId), eq(managerId), anyString(), eq(reason)))
            .thenReturn(rejectionResult);

        // Act & Assert
        assertDoesNotThrow(() -> {
            orderService.rejectOrder(orderId, managerId, reason);
        });

        // Verify that OrderStateManagementService was called
        verify(orderStateManagementService, times(1))
            .rejectOrder(eq(orderId), eq(managerId), anyString(), eq(reason));
    }

    @Test
    @DisplayName("Stock Validation Integration - Cart to Order Conversion")
    void testStockValidation_CartToOrderConversion() throws Exception {
        // Arrange
        String cartSessionId = "cart-123";
        String userId = "user-123";
        
        Cart testCart = new Cart();
        testCart.setCartSessionId(cartSessionId);
        
        CartItem cartItem = new CartItem();
        cartItem.setProduct(testProduct);
        cartItem.setQuantity(2);
        testCart.setItems(Arrays.asList(cartItem));
        
        UserAccount testUser = new UserAccount();
        testUser.setUserId(userId);
        
        IStockValidationService.StockValidationResult stockResult = 
            mock(IStockValidationService.StockValidationResult.class);
        when(stockResult.isValid()).thenReturn(true);
        when(stockResult.getValidationMessage()).thenReturn("Stock validation passed");
        
        when(cartService.getCart(cartSessionId)).thenReturn(testCart);
        when(userAccountDAO.getById(userId)).thenReturn(testUser);
        when(productDAO.getById("TEST-PROD-001")).thenReturn(testProduct);
        when(stockValidationService.validateProductStock("TEST-PROD-001", 2))
            .thenReturn(stockResult);

        // Act & Assert
        assertDoesNotThrow(() -> {
            orderService.initiateOrderFromCart(cartSessionId, userId);
        });

        // Verify that StockValidationService was called
        verify(stockValidationService, times(1))
            .validateProductStock("TEST-PROD-001", 2);
    }

    @Test
    @DisplayName("Stock Validation Integration - Insufficient Stock")
    void testStockValidation_InsufficientStock() throws Exception {
        // Arrange
        String cartSessionId = "cart-123";
        String userId = "user-123";
        
        Cart testCart = new Cart();
        testCart.setCartSessionId(cartSessionId);
        
        CartItem cartItem = new CartItem();
        cartItem.setProduct(testProduct);
        cartItem.setQuantity(15); // More than available stock
        testCart.setItems(Arrays.asList(cartItem));
        
        UserAccount testUser = new UserAccount();
        testUser.setUserId(userId);
        
        IStockValidationService.StockValidationResult stockResult = 
            mock(IStockValidationService.StockValidationResult.class);
        when(stockResult.isValid()).thenReturn(false);
        when(stockResult.getValidationMessage()).thenReturn("Insufficient stock: requested 15, available 10");
        
        when(cartService.getCart(cartSessionId)).thenReturn(testCart);
        when(userAccountDAO.getById(userId)).thenReturn(testUser);
        when(productDAO.getById("TEST-PROD-001")).thenReturn(testProduct);
        when(stockValidationService.validateProductStock("TEST-PROD-001", 15))
            .thenReturn(stockResult);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderService.initiateOrderFromCart(cartSessionId, userId);
        });

        assertTrue(exception.getMessage().contains("Stock validation failed"));
        verify(stockValidationService, times(1))
            .validateProductStock("TEST-PROD-001", 15);
    }

    @Test
    @DisplayName("Bulk Stock Validation Integration - Payment Processing")
    void testBulkStockValidation_PaymentProcessing() throws Exception {
        // Arrange
        String orderId = "TEST-ORDER-001";
        String paymentMethodId = "pm-123";
        
        testOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        testOrder.setDeliveryInfo(new DeliveryInfo());
        testOrder.setCalculatedDeliveryFee(20.0f);
        testOrder.setTotalAmountPaid(220.0f);
        
        IStockValidationService.BulkStockValidationResult bulkResult = 
            mock(IStockValidationService.BulkStockValidationResult.class);
        when(bulkResult.isAllValid()).thenReturn(true);
        
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setTransactionStatus("SUCCESS");
        
        when(orderEntityDAO.getById(orderId)).thenReturn(testOrder);
        when(stockValidationService.validateOrderItemsStock(any()))
            .thenReturn(bulkResult);
        when(paymentService.processPayment(testOrder, paymentMethodId))
            .thenReturn(paymentTransaction);

        // Act & Assert
        assertDoesNotThrow(() -> {
            orderService.processOrderPayment(orderId, paymentMethodId);
        });

        // Verify that bulk stock validation was called
        verify(stockValidationService, times(1))
            .validateOrderItemsStock(any());
    }
}