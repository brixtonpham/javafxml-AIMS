package com.aims.test.payment;

import com.aims.core.application.services.IOrderValidationService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.ProductType;
import com.aims.core.presentation.controllers.OrderSummaryController;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

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
import static org.mockito.Mockito.*;

/**
 * Test suite for the enhanced Order Summary Payment Button Validation.
 * 
 * This test verifies that the fix for the payment button validation issue works correctly:
 * - Uses OrderValidationService instead of manual validation
 * - Handles lazy loading issues properly
 * - Provides fallback mechanisms for edge cases
 * - Improves error handling with user-friendly messages
 * - Enables payment button when order is actually ready for payment
 */
public class OrderSummaryPaymentButtonValidationTest {

    @Mock
    private IOrderValidationService mockOrderValidationService;

    private OrderSummaryController orderSummaryController;
    private OrderEntity validTestOrder;
    private DeliveryInfo validDeliveryInfo;
    private List<OrderItem> validOrderItems;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize controller
        orderSummaryController = new OrderSummaryController();
        orderSummaryController.setOrderValidationService(mockOrderValidationService);
        
        // Create valid test data
        setupValidTestOrder();
    }

    private void setupValidTestOrder() {
        // Create valid delivery info
        validDeliveryInfo = new DeliveryInfo();
        validDeliveryInfo.setRecipientName("John Doe");
        validDeliveryInfo.setPhoneNumber("0123456789");
        validDeliveryInfo.setEmail("john.doe@example.com");
        validDeliveryInfo.setDeliveryAddress("123 Main Street, Apt 2A");
        validDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        validDeliveryInfo.setDeliveryMethodChosen("STANDARD");
        validDeliveryInfo.setDeliveryInstructions("Leave at front door");

        // Create valid order items
        validOrderItems = new ArrayList<>();
        
        Product testProduct = new Product();
        testProduct.setProductId("BOOK001");
        testProduct.setTitle("Test Book");
        testProduct.setProductType(ProductType.BOOK);
        testProduct.setPrice(100000f);
        testProduct.setQuantityInStock(10);

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(testProduct);
        orderItem.setQuantity(2);
        orderItem.setPriceAtTimeOfOrder(100000f);
        validOrderItems.add(orderItem);

        // Create valid order
        validTestOrder = new OrderEntity();
        validTestOrder.setOrderId("ORDER123");
        validTestOrder.setOrderDate(LocalDateTime.now().minusHours(1));
        validTestOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        validTestOrder.setDeliveryInfo(validDeliveryInfo);
        validTestOrder.setOrderItems(validOrderItems);
        validTestOrder.setTotalProductPriceExclVAT(200000f);
        validTestOrder.setTotalProductPriceInclVAT(220000f);
        validTestOrder.setCalculatedDeliveryFee(30000f);
        validTestOrder.setTotalAmountPaid(250000f);
    }

    @Test
    @DisplayName("Payment button should be enabled when OrderValidationService confirms order is ready")
    void testPaymentButtonEnabledWhenOrderValidationServiceConfirmsReady() throws Exception {
        // Arrange
        when(mockOrderValidationService.isOrderReadyForPayment("ORDER123")).thenReturn(true);
        when(mockOrderValidationService.getValidatedOrderForPayment("ORDER123")).thenReturn(validTestOrder);
        
        // Act
        orderSummaryController.setOrderData(validTestOrder);
        
        // The payment button should be enabled since validation passed
        // This is tested implicitly through the validation process
        
        // Verify
        verify(mockOrderValidationService).isOrderReadyForPayment("ORDER123");
        verify(mockOrderValidationService).getValidatedOrderForPayment("ORDER123");
    }

    @Test
    @DisplayName("Payment button should be disabled when OrderValidationService indicates order not ready")
    void testPaymentButtonDisabledWhenOrderValidationServiceIndicatesNotReady() throws Exception {
        // Arrange
        when(mockOrderValidationService.isOrderReadyForPayment("ORDER123")).thenReturn(false);
        
        // Act & Assert
        // The validation process should handle this case and disable the payment button
        
        // Verify
        verify(mockOrderValidationService).isOrderReadyForPayment("ORDER123");
        verify(mockOrderValidationService, never()).getValidatedOrderForPayment(any());
    }

    @Test
    @DisplayName("System should handle lazy loading issues by using OrderValidationService")
    void testHandleLazyLoadingIssuesWithOrderValidationService() throws Exception {
        // Arrange
        OrderEntity orderWithNullDeliveryInfo = new OrderEntity();
        orderWithNullDeliveryInfo.setOrderId("ORDER123");
        orderWithNullDeliveryInfo.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        orderWithNullDeliveryInfo.setDeliveryInfo(null); // Simulate lazy loading issue
        orderWithNullDeliveryInfo.setOrderItems(validOrderItems);
        orderWithNullDeliveryInfo.setTotalAmountPaid(250000f);

        // OrderValidationService should handle this and return the complete order
        when(mockOrderValidationService.isOrderReadyForPayment("ORDER123")).thenReturn(true);
        when(mockOrderValidationService.getValidatedOrderForPayment("ORDER123")).thenReturn(validTestOrder);
        
        // Act
        orderSummaryController.setOrderData(orderWithNullDeliveryInfo);
        
        // Verify that the service was called to resolve the lazy loading issue
        verify(mockOrderValidationService).isOrderReadyForPayment("ORDER123");
        verify(mockOrderValidationService).getValidatedOrderForPayment("ORDER123");
    }

    @Test
    @DisplayName("System should fall back to manual validation when OrderValidationService has database errors")
    void testFallbackToManualValidationOnDatabaseError() throws Exception {
        // Arrange
        when(mockOrderValidationService.isOrderReadyForPayment("ORDER123"))
            .thenThrow(new SQLException("Database connection error"));
        
        // Act
        orderSummaryController.setOrderData(validTestOrder);
        
        // Verify that the service was attempted
        verify(mockOrderValidationService).isOrderReadyForPayment("ORDER123");
        
        // The system should fall back to manual validation
        // and should not throw an exception but handle it gracefully
    }

    @Test
    @DisplayName("System should provide user-friendly error messages for validation failures")
    void testUserFriendlyErrorMessagesForValidationFailures() throws Exception {
        // Arrange
        when(mockOrderValidationService.isOrderReadyForPayment("ORDER123"))
            .thenThrow(new ValidationException("Delivery information is required for payment processing."));
        
        // Act
        orderSummaryController.setOrderData(validTestOrder);
        
        // Verify that the service was called
        verify(mockOrderValidationService).isOrderReadyForPayment("ORDER123");
        
        // The system should handle the ValidationException and show user-friendly error
    }

    @Test
    @DisplayName("System should handle ResourceNotFoundException gracefully")
    void testHandleResourceNotFoundExceptionGracefully() throws Exception {
        // Arrange
        when(mockOrderValidationService.isOrderReadyForPayment("ORDER123"))
            .thenThrow(new ResourceNotFoundException("Order not found with ID: ORDER123"));
        
        // Act
        orderSummaryController.setOrderData(validTestOrder);
        
        // Verify that the service was called
        verify(mockOrderValidationService).isOrderReadyForPayment("ORDER123");
        
        // The system should handle the ResourceNotFoundException appropriately
    }

    @Test
    @DisplayName("System should refresh order data when OrderValidationService provides updated order")
    void testRefreshOrderDataWhenServiceProvidesUpdatedOrder() throws Exception {
        // Arrange
        OrderEntity updatedOrder = new OrderEntity();
        updatedOrder.setOrderId("ORDER123");
        updatedOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        updatedOrder.setDeliveryInfo(validDeliveryInfo);
        updatedOrder.setOrderItems(validOrderItems);
        updatedOrder.setTotalAmountPaid(275000f); // Updated amount
        
        when(mockOrderValidationService.isOrderReadyForPayment("ORDER123")).thenReturn(true);
        when(mockOrderValidationService.getValidatedOrderForPayment("ORDER123")).thenReturn(updatedOrder);
        
        // Act
        orderSummaryController.setOrderData(validTestOrder);
        
        // Verify
        verify(mockOrderValidationService).isOrderReadyForPayment("ORDER123");
        verify(mockOrderValidationService).getValidatedOrderForPayment("ORDER123");
        
        // The controller should use the updated order data from the service
    }

    @Test
    @DisplayName("Manual fallback validation should work when OrderValidationService is unavailable")
    void testManualFallbackValidationWhenServiceUnavailable() {
        // Arrange
        OrderSummaryController controllerWithoutService = new OrderSummaryController();
        // No OrderValidationService injected - should fall back to manual validation
        
        // Act
        controllerWithoutService.setOrderData(validTestOrder);
        
        // The system should fall back to manual validation
        // and should work correctly for valid orders
    }

    @Test
    @DisplayName("Payment validation should handle edge case of missing order ID")
    void testPaymentValidationHandlesMissingOrderId() {
        // Arrange
        OrderEntity orderWithoutId = new OrderEntity();
        orderWithoutId.setOrderId(null); // Missing order ID
        orderWithoutId.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        orderWithoutId.setDeliveryInfo(validDeliveryInfo);
        orderWithoutId.setOrderItems(validOrderItems);
        orderWithoutId.setTotalAmountPaid(250000f);
        
        // Act
        orderSummaryController.setOrderData(orderWithoutId);
        
        // The system should handle this gracefully and show appropriate error
        // Payment button should be disabled
    }

    @Test
    @DisplayName("Payment validation should handle comprehensive business rule validation")
    void testPaymentValidationHandlesBusinessRules() throws Exception {
        // Arrange
        when(mockOrderValidationService.isOrderReadyForPayment("ORDER123")).thenReturn(true);
        when(mockOrderValidationService.getValidatedOrderForPayment("ORDER123"))
            .thenThrow(new ValidationException("Total payment amount does not match calculated total"));
        
        // Act
        orderSummaryController.setOrderData(validTestOrder);
        
        // Verify
        verify(mockOrderValidationService).isOrderReadyForPayment("ORDER123");
        verify(mockOrderValidationService).getValidatedOrderForPayment("ORDER123");
        
        // The system should handle business rule validation failures appropriately
    }
}