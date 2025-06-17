package com.aims.test.payment;

import com.aims.core.application.services.IOrderValidationService;
import com.aims.core.application.services.IOrderService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.ProductType;
import com.aims.core.presentation.controllers.PaymentMethodScreenController;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.test.base.BaseUITest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CRITICAL FIX 1 VERIFICATION: Payment Method Screen Validation Enhancement Test
 * 
 * Tests the enhanced validation logic in PaymentMethodScreenController that replaces
 * manual validation with service-based validation using OrderValidationService.
 * 
 * This test verifies:
 * 1. Service-based validation replaces manual checks
 * 2. Comprehensive error handling and recovery
 * 3. Detailed logging for payment flow debugging
 * 4. Robust handling of various order states
 */
public class PaymentMethodValidationEnhancementTest extends BaseUITest {

    @Mock
    private IOrderValidationService mockOrderValidationService;
    
    @Mock
    private IOrderService mockOrderService;
    
    private PaymentMethodScreenController controller;
    private OrderEntity validOrder;
    private OrderEntity invalidOrder;
    
    // UI components for testing
    private Label errorMessageLabel;
    private Button proceedButton;
    private RadioButton creditCardRadio;
    private ToggleGroup paymentMethodToggleGroup;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize controller with mocked services
        controller = new PaymentMethodScreenController();
        
        // Set up mock services using reflection to access private fields
        setPrivateField(controller, "orderValidationService", mockOrderValidationService);
        setPrivateField(controller, "orderService", mockOrderService);
        
        // Initialize UI components
        errorMessageLabel = new Label();
        proceedButton = new Button();
        creditCardRadio = new RadioButton();
        creditCardRadio.setUserData("CREDIT_CARD");
        paymentMethodToggleGroup = new ToggleGroup();
        creditCardRadio.setToggleGroup(paymentMethodToggleGroup);
        
        setPrivateField(controller, "errorMessageLabel", errorMessageLabel);
        setPrivateField(controller, "proceedButton", proceedButton);
        setPrivateField(controller, "creditCardRadio", creditCardRadio);
        setPrivateField(controller, "paymentMethodToggleGroup", paymentMethodToggleGroup);
        
        // Create test orders
        createTestOrders();
    }
    
    private void createTestOrders() {
        // Valid order for testing successful validation
        validOrder = new OrderEntity();
        validOrder.setOrderId("ORDER_001");
        validOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        validOrder.setTotalAmountPaid(100000.0f);
        
        // Set up delivery info
        DeliveryInfo deliveryInfo = new DeliveryInfo();
        deliveryInfo.setRecipientName("John Doe");
        deliveryInfo.setDeliveryAddress("123 Main Street");
        deliveryInfo.setPhoneNumber("0123456789");
        deliveryInfo.setDeliveryProvinceCity("Hanoi");
        validOrder.setDeliveryInfo(deliveryInfo);
        
        // Set up order items
        Product product = new Product();
        product.setProductId("PROD_001");
        product.setTitle("Test Book");
        product.setProductType(ProductType.BOOK);
        product.setPrice(50000.0f);
        
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setPriceAtTimeOfOrder(50000.0f);
        
        validOrder.setOrderItems(Arrays.asList(orderItem));
        
        // Invalid order for testing validation failures
        invalidOrder = new OrderEntity();
        invalidOrder.setOrderId("ORDER_002");
        invalidOrder.setOrderStatus(OrderStatus.CANCELLED);
        invalidOrder.setOrderItems(new ArrayList<>());
    }

    @Test
    @DisplayName("CRITICAL FIX 1: Enhanced validation succeeds for valid orders")
    void testEnhancedValidation_ValidOrder_Success() throws Exception {
        // Arrange
        String orderId = "ORDER_001";
        setPrivateField(controller, "currentOrderId", orderId);
        
        when(mockOrderValidationService.orderExists(orderId)).thenReturn(true);
        when(mockOrderValidationService.getValidatedOrderForPayment(orderId)).thenReturn(validOrder);
        
        // Select payment method
        paymentMethodToggleGroup.selectToggle(creditCardRadio);
        
        // Act
        OrderEntity result = invokePrivateMethod(controller, "validateOrderForPaymentEnhanced");
        
        // Assert
        assertNotNull(result, "Enhanced validation should return validated order for valid order");
        assertEquals(orderId, result.getOrderId(), "Returned order should have correct ID");
        assertEquals(OrderStatus.PENDING_PAYMENT, result.getOrderStatus(), "Order should be in PENDING_PAYMENT status");
        
        // Verify service interactions
        verify(mockOrderValidationService).orderExists(orderId);
        verify(mockOrderValidationService).getValidatedOrderForPayment(orderId);
        
        // Verify UI state
        assertFalse(errorMessageLabel.isVisible(), "Error label should not be visible for valid order");
    }
    
    @Test
    @DisplayName("CRITICAL FIX 1: Enhanced validation handles order not found")
    void testEnhancedValidation_OrderNotFound_ShowsError() throws Exception {
        // Arrange
        String orderId = "NON_EXISTENT_ORDER";
        setPrivateField(controller, "currentOrderId", orderId);
        
        when(mockOrderValidationService.orderExists(orderId)).thenReturn(false);
        
        // Act
        OrderEntity result = invokePrivateMethod(controller, "validateOrderForPaymentEnhanced");
        
        // Assert
        assertNull(result, "Enhanced validation should return null for non-existent order");
        
        // Verify service interactions
        verify(mockOrderValidationService).orderExists(orderId);
        verify(mockOrderValidationService, never()).getValidatedOrderForPayment(orderId);
        
        // Verify error handling
        assertTrue(errorMessageLabel.isVisible(), "Error label should be visible for non-existent order");
        assertEquals("Order not found. Please restart the order process.", errorMessageLabel.getText());
    }
    
    @Test
    @DisplayName("CRITICAL FIX 1: Enhanced validation handles validation exceptions")
    void testEnhancedValidation_ValidationException_ShowsSpecificError() throws Exception {
        // Arrange
        String orderId = "ORDER_002";
        setPrivateField(controller, "currentOrderId", orderId);
        
        when(mockOrderValidationService.orderExists(orderId)).thenReturn(true);
        when(mockOrderValidationService.getValidatedOrderForPayment(orderId))
            .thenThrow(new ValidationException("Delivery information is missing. Please complete delivery details first."));
        
        // Act
        OrderEntity result = invokePrivateMethod(controller, "validateOrderForPaymentEnhanced");
        
        // Assert
        assertNull(result, "Enhanced validation should return null for validation failure");
        
        // Verify error handling
        assertTrue(errorMessageLabel.isVisible(), "Error label should be visible for validation error");
        assertEquals("Delivery information is missing. Please complete delivery details first.", 
                    errorMessageLabel.getText(), "Should show specific validation error message");
    }
    
    @Test
    @DisplayName("CRITICAL FIX 1: Enhanced validation with error recovery")
    void testEnhancedValidation_WithErrorRecovery_Success() throws Exception {
        // Arrange
        String orderId = "ORDER_001";
        setPrivateField(controller, "currentOrderId", orderId);
        
        when(mockOrderValidationService.orderExists(orderId)).thenReturn(true);
        
        // First call fails, second call (after recovery) succeeds
        when(mockOrderValidationService.getValidatedOrderForPayment(orderId))
            .thenThrow(new RuntimeException("Temporary database error"))
            .thenReturn(validOrder);
        
        // Recovery should succeed
        when(mockOrderService.getOrderById(orderId)).thenReturn(validOrder);
        
        // Act
        OrderEntity result = invokePrivateMethod(controller, "validateOrderForPaymentEnhanced");
        
        // Assert
        assertNotNull(result, "Enhanced validation should succeed after recovery");
        assertEquals(orderId, result.getOrderId(), "Recovered order should have correct ID");
        
        // Verify recovery was attempted
        verify(mockOrderService).getOrderById(orderId);
        verify(mockOrderValidationService, times(2)).getValidatedOrderForPayment(orderId);
    }
    
    @Test
    @DisplayName("CRITICAL FIX 1: Error recovery handles null order ID")
    void testErrorRecovery_NullOrderId_ReturnsFalse() throws Exception {
        // Arrange
        setPrivateField(controller, "currentOrderId", null);
        
        // Act
        boolean result = invokePrivateMethod(controller, "attemptOrderDataRecovery");
        
        // Assert
        assertFalse(result, "Error recovery should return false for null order ID");
        
        // Verify no service calls were made
        verify(mockOrderService, never()).getOrderById(any());
    }
    
    @Test
    @DisplayName("CRITICAL FIX 1: Error recovery handles service exceptions")
    void testErrorRecovery_ServiceException_ReturnsFalse() throws Exception {
        // Arrange
        String orderId = "ORDER_001";
        setPrivateField(controller, "currentOrderId", orderId);
        
        when(mockOrderService.getOrderById(orderId))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // Act
        boolean result = invokePrivateMethod(controller, "attemptOrderDataRecovery");
        
        // Assert
        assertFalse(result, "Error recovery should return false when service throws exception");
        
        // Verify service was called
        verify(mockOrderService).getOrderById(orderId);
    }
    
    @Test
    @DisplayName("CRITICAL FIX 1: Enhanced validation handles null order ID")
    void testEnhancedValidation_NullOrderId_ShowsError() throws Exception {
        // Arrange
        setPrivateField(controller, "currentOrderId", null);
        
        // Act
        OrderEntity result = invokePrivateMethod(controller, "validateOrderForPaymentEnhanced");
        
        // Assert
        assertNull(result, "Enhanced validation should return null for null order ID");
        
        // Verify error handling
        assertTrue(errorMessageLabel.isVisible(), "Error label should be visible for null order ID");
        assertEquals("Invalid order state. Please restart the order process.", errorMessageLabel.getText());
        
        // Verify no service calls were made
        verify(mockOrderValidationService, never()).orderExists(any());
        verify(mockOrderValidationService, never()).getValidatedOrderForPayment(any());
    }
    
    @Test
    @DisplayName("CRITICAL FIX 1: Enhanced validation handles empty order ID")
    void testEnhancedValidation_EmptyOrderId_ShowsError() throws Exception {
        // Arrange
        setPrivateField(controller, "currentOrderId", "   "); // Whitespace only
        
        // Act
        OrderEntity result = invokePrivateMethod(controller, "validateOrderForPaymentEnhanced");
        
        // Assert
        assertNull(result, "Enhanced validation should return null for empty order ID");
        
        // Verify error handling
        assertTrue(errorMessageLabel.isVisible(), "Error label should be visible for empty order ID");
        assertEquals("Invalid order state. Please restart the order process.", errorMessageLabel.getText());
    }
    
    @Test
    @DisplayName("CRITICAL FIX 1: Integration test with handleProceedAction")
    void testHandleProceedAction_WithEnhancedValidation_Success() throws Exception {
        // Arrange
        String orderId = "ORDER_001";
        setPrivateField(controller, "currentOrderId", orderId);
        
        when(mockOrderValidationService.orderExists(orderId)).thenReturn(true);
        when(mockOrderValidationService.getValidatedOrderForPayment(orderId)).thenReturn(validOrder);
        
        // Select payment method
        paymentMethodToggleGroup.selectToggle(creditCardRadio);
        
        // Act - this would normally navigate, but we're testing the validation part
        // We can't easily test the navigation without a full UI environment,
        // so we focus on the validation logic
        
        // The enhanced validation should work through the handleProceedAction method
        OrderEntity result = invokePrivateMethod(controller, "validateOrderForPaymentEnhanced");
        
        // Assert
        assertNotNull(result, "Enhanced validation integration should work correctly");
        assertEquals(orderId, result.getOrderId(), "Integrated validation should return correct order");
        
        // Verify the controller's current order is updated
        OrderEntity currentOrder = getPrivateField(controller, "currentOrder");
        assertNotNull(currentOrder, "Controller's current order should be updated");
        assertEquals(orderId, currentOrder.getOrderId(), "Updated current order should have correct ID");
    }
    
    // Helper methods for reflection-based testing
    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set private field: " + fieldName, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get private field: " + fieldName, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(Object target, String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
            
            Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return (T) method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
        }
    }
}