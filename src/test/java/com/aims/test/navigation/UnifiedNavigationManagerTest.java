package com.aims.test.navigation;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.presentation.utils.UnifiedNavigationManager;
import com.aims.core.presentation.utils.MainLayoutControllerRegistry;
import com.aims.core.presentation.utils.OrderDataContextManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for UnifiedNavigationManager to verify Phase 2 implementation.
 * Tests the unified navigation architecture and order data preservation.
 */
public class UnifiedNavigationManagerTest {

    @Mock
    private MainLayoutController mockMainLayoutController;

    @Mock
    private javafx.scene.Node mockContentPane;

    @Mock
    private javafx.scene.layout.BorderPane mockMainContainer;

    @Mock
    private Object mockSourceController;

    private OrderEntity testOrder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Reset navigation statistics
        UnifiedNavigationManager.resetStatistics();
        
        // Reset registries
        MainLayoutControllerRegistry.reset();
        
        // Setup test order
        setupTestOrder();
        
        // Setup mock controller
        when(mockMainLayoutController.getContentPane()).thenReturn(mockContentPane);
        when(mockMainLayoutController.getMainContainer()).thenReturn(mockMainContainer);
    }

    private void setupTestOrder() {
        testOrder = new OrderEntity();
        testOrder.setOrderId("TEST_ORDER_001");
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setTotalAmountPaid(100.0f);
        
        // Add delivery info
        DeliveryInfo deliveryInfo = new DeliveryInfo();
        deliveryInfo.setRecipientName("Test User");
        deliveryInfo.setDeliveryAddress("123 Test St");
        deliveryInfo.setPhoneNumber("123-456-7890");
        testOrder.setDeliveryInfo(deliveryInfo);
        
        // Add order items
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem();
        item.setQuantity(1);
        item.setPriceAtTimeOfOrder(100.0f);
        items.add(item);
        testOrder.setOrderItems(items);
    }

    @Test
    @DisplayName("Should successfully navigate to order summary with valid order")
    void testNavigateToOrderSummarySuccess() {
        // Setup
        MainLayoutControllerRegistry.setInstance(mockMainLayoutController);
        when(mockMainLayoutController.loadContent(anyString())).thenReturn(mockSourceController);
        
        // Execute
        UnifiedNavigationManager.NavigationResult result = 
            UnifiedNavigationManager.navigateToOrderSummary(testOrder, mockSourceController);
        
        // Verify
        assertTrue(result.isSuccess(), "Navigation should succeed with valid order and controller");
        assertEquals(1.0, UnifiedNavigationManager.getSuccessRate(), 0.01, "Success rate should be 100%");
        
        // Verify order data was preserved
        String sessionId = UnifiedNavigationManager.getLastNavigationRequest().getSessionId();
        assertTrue(OrderDataContextManager.hasOrderData(sessionId), "Order data should be preserved");
    }

    @Test
    @DisplayName("Should successfully navigate to payment method with valid order")
    void testNavigateToPaymentMethodSuccess() {
        // Setup
        MainLayoutControllerRegistry.setInstance(mockMainLayoutController);
        when(mockMainLayoutController.loadContent(anyString())).thenReturn(mockSourceController);
        
        // Execute
        UnifiedNavigationManager.NavigationResult result = 
            UnifiedNavigationManager.navigateToPaymentMethod(testOrder, mockSourceController);
        
        // Verify
        assertTrue(result.isSuccess(), "Navigation should succeed with valid order and controller");
        
        // Verify MainLayoutController was used
        verify(mockMainLayoutController).loadContent("/com/aims/presentation/views/payment_method_screen.fxml");
        verify(mockMainLayoutController).setHeaderTitle("Payment Method");
    }

    @Test
    @DisplayName("Should handle navigation with null order gracefully")
    void testNavigateWithNullOrder() {
        // Setup
        MainLayoutControllerRegistry.setInstance(mockMainLayoutController);
        
        // Execute
        UnifiedNavigationManager.NavigationResult result = 
            UnifiedNavigationManager.navigateToOrderSummary(null, mockSourceController);
        
        // Verify
        assertEquals(UnifiedNavigationManager.NavigationResult.FAILED_CRITICAL, result, 
            "Navigation should fail critically with null order");
        assertEquals(0.0, UnifiedNavigationManager.getSuccessRate(), 0.01, "Success rate should be 0%");
    }

    @Test
    @DisplayName("Should handle navigation without MainLayoutController")
    void testNavigateWithoutMainLayoutController() {
        // Note: MainLayoutControllerRegistry is not set, so no controller available
        
        // Execute
        UnifiedNavigationManager.NavigationResult result = 
            UnifiedNavigationManager.navigateToPaymentMethod(testOrder, mockSourceController);
        
        // Verify - should use emergency navigation
        assertNotNull(result, "Navigation result should not be null");
        // Emergency navigation may succeed or fail depending on NavigationService availability
        assertTrue(result == UnifiedNavigationManager.NavigationResult.DATA_PRESERVED || 
                  result == UnifiedNavigationManager.NavigationResult.FAILED_CRITICAL,
                  "Should either preserve data or fail critically");
    }

    @Test
    @DisplayName("Should validate order requirements before navigation")
    void testOrderValidationBeforeNavigation() {
        // Setup order with missing delivery info for payment method
        OrderEntity invalidOrder = new OrderEntity();
        invalidOrder.setOrderId("INVALID_ORDER");
        invalidOrder.setTotalAmountPaid(0.0f); // Invalid amount
        
        MainLayoutControllerRegistry.setInstance(mockMainLayoutController);
        
        // Execute
        UnifiedNavigationManager.NavigationResult result = 
            UnifiedNavigationManager.navigateToPaymentMethod(invalidOrder, mockSourceController);
        
        // Verify
        assertEquals(UnifiedNavigationManager.NavigationResult.FAILED_CRITICAL, result,
            "Navigation should fail validation with invalid order");
    }

    @Test
    @DisplayName("Should preserve order data in session during navigation")
    void testOrderDataPreservation() {
        // Setup
        MainLayoutControllerRegistry.setInstance(mockMainLayoutController);
        when(mockMainLayoutController.loadContent(anyString())).thenReturn(mockSourceController);
        
        // Execute
        UnifiedNavigationManager.NavigationResult result = 
            UnifiedNavigationManager.navigateToOrderSummary(testOrder, mockSourceController);
        
        // Verify
        assertTrue(result.isSuccess(), "Navigation should succeed");
        
        // Check that order data was preserved
        UnifiedNavigationManager.NavigationRequest lastRequest = 
            UnifiedNavigationManager.getLastNavigationRequest();
        assertNotNull(lastRequest, "Navigation request should be recorded");
        
        String sessionId = lastRequest.getSessionId();
        assertTrue(OrderDataContextManager.hasOrderData(sessionId), 
            "Order data should be preserved in session");
        
        OrderEntity retrievedOrder = OrderDataContextManager.retrieveOrderData(sessionId);
        assertNotNull(retrievedOrder, "Should be able to retrieve preserved order data");
        assertEquals(testOrder.getOrderId(), retrievedOrder.getOrderId(), 
            "Retrieved order should match original");
    }

    @Test
    @DisplayName("Should handle controller loading failure gracefully")
    void testControllerLoadingFailure() {
        // Setup
        MainLayoutControllerRegistry.setInstance(mockMainLayoutController);
        when(mockMainLayoutController.loadContent(anyString())).thenReturn(null); // Simulate loading failure
        
        // Execute
        UnifiedNavigationManager.NavigationResult result = 
            UnifiedNavigationManager.navigateToOrderSummary(testOrder, mockSourceController);
        
        // Verify
        assertFalse(result.isSuccess(), "Navigation should fail when controller loading fails");
        assertEquals(UnifiedNavigationManager.NavigationResult.FAILED_RECOVERABLE, result,
            "Should return FAILED_RECOVERABLE for controller loading failure");
    }

    @Test
    @DisplayName("Should track navigation statistics correctly")
    void testNavigationStatistics() {
        // Setup
        MainLayoutControllerRegistry.setInstance(mockMainLayoutController);
        when(mockMainLayoutController.loadContent(anyString())).thenReturn(mockSourceController);
        
        // Execute multiple navigations
        UnifiedNavigationManager.navigateToOrderSummary(testOrder, mockSourceController); // Success
        UnifiedNavigationManager.navigateToPaymentMethod(testOrder, mockSourceController); // Success
        UnifiedNavigationManager.navigateToOrderSummary(null, mockSourceController); // Failure
        
        // Verify statistics
        assertEquals(66.67, UnifiedNavigationManager.getSuccessRate(), 0.1, 
            "Success rate should be 66.67% (2 success, 1 failure)");
        
        // Verify debug info contains statistics
        String debugInfo = UnifiedNavigationManager.getNavigationDebugInfo();
        assertNotNull(debugInfo, "Debug info should not be null");
        assertTrue(debugInfo.contains("Success Count"), "Debug info should contain success count");
        assertTrue(debugInfo.contains("Failure Count"), "Debug info should contain failure count");
    }

    @Test
    @DisplayName("Should generate unique session IDs for each navigation")
    void testUniqueSessionGeneration() {
        // Setup
        MainLayoutControllerRegistry.setInstance(mockMainLayoutController);
        when(mockMainLayoutController.loadContent(anyString())).thenReturn(mockSourceController);
        
        // Execute multiple navigations
        UnifiedNavigationManager.navigateToOrderSummary(testOrder, mockSourceController);
        String firstSessionId = UnifiedNavigationManager.getLastNavigationRequest().getSessionId();
        
        UnifiedNavigationManager.navigateToPaymentMethod(testOrder, mockSourceController);
        String secondSessionId = UnifiedNavigationManager.getLastNavigationRequest().getSessionId();
        
        // Verify
        assertNotNull(firstSessionId, "First session ID should not be null");
        assertNotNull(secondSessionId, "Second session ID should not be null");
        assertNotEquals(firstSessionId, secondSessionId, "Session IDs should be unique");
        
        assertTrue(firstSessionId.startsWith("NAV_"), "Session ID should have proper prefix");
        assertTrue(secondSessionId.startsWith("NAV_"), "Session ID should have proper prefix");
    }

    @Test
    @DisplayName("Should handle navigation request validation")
    void testNavigationRequestValidation() {
        // Test with empty order ID
        OrderEntity emptyOrderIdOrder = new OrderEntity();
        emptyOrderIdOrder.setOrderId(""); // Empty order ID
        
        // Execute
        UnifiedNavigationManager.NavigationResult result = 
            UnifiedNavigationManager.navigateToOrderSummary(emptyOrderIdOrder, mockSourceController);
        
        // Verify
        assertEquals(UnifiedNavigationManager.NavigationResult.FAILED_CRITICAL, result,
            "Navigation should fail validation with empty order ID");
    }

    @Test
    @DisplayName("Should provide comprehensive debug information")
    void testDebugInformation() {
        // Setup
        MainLayoutControllerRegistry.setInstance(mockMainLayoutController);
        when(mockMainLayoutController.loadContent(anyString())).thenReturn(mockSourceController);
        
        // Execute navigation
        UnifiedNavigationManager.navigateToOrderSummary(testOrder, mockSourceController);
        
        // Get debug info
        String debugInfo = UnifiedNavigationManager.getNavigationDebugInfo();
        
        // Verify debug info content
        assertNotNull(debugInfo, "Debug info should not be null");
        assertTrue(debugInfo.contains("UnifiedNavigationManager Debug Info"), 
            "Should contain manager name");
        assertTrue(debugInfo.contains("Last Request"), "Should contain last request info");
        assertTrue(debugInfo.contains("Last Result"), "Should contain last result info");
        assertTrue(debugInfo.contains("Success Rate"), "Should contain success rate");
        assertTrue(debugInfo.contains("MainLayoutController Available"), 
            "Should contain controller availability");
    }
}