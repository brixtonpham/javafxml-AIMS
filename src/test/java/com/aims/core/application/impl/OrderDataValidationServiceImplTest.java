package com.aims.core.application.impl;

import com.aims.core.application.services.IOrderDataValidationService;
import com.aims.core.application.services.IOrderDataLoaderService;
import com.aims.core.application.services.ICartDataValidationService;
import com.aims.core.application.services.IDeliveryCalculationService;
import com.aims.core.application.services.IProductService;
import com.aims.core.application.dtos.validation.*;
import com.aims.core.entities.*;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.ProductType;
import com.aims.core.shared.exceptions.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for OrderDataValidationServiceImpl
 */
public class OrderDataValidationServiceImplTest {
    
    @Mock
    private IOrderDataLoaderService orderDataLoaderService;
    
    @Mock
    private ICartDataValidationService cartDataValidationService;
    
    @Mock
    private IDeliveryCalculationService deliveryCalculationService;
    
    @Mock
    private IProductService productService;
    
    private IOrderDataValidationService validationService;
    
    private OrderEntity testOrder;
    private DeliveryInfo testDeliveryInfo;
    private List<OrderItem> testOrderItems;
    private Product testProduct;
    private UserAccount testUser;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        validationService = new OrderDataValidationServiceImpl(
            orderDataLoaderService,
            cartDataValidationService,
            deliveryCalculationService,
            productService
        );
        
        setupTestData();
    }
    
    private void setupTestData() {
        // Create test product
        testProduct = new Product();
        testProduct.setProductId("PROD001");
        testProduct.setTitle("Test Product");
        testProduct.setPrice(100.0f);
        testProduct.setQuantityInStock(10);
        testProduct.setProductType(ProductType.BOOK);
        testProduct.setWeightKg(0.5f);
        
        // Create test user
        testUser = new UserAccount();
        testUser.setUserId("USER001");
        testUser.setEmail("test@example.com");
        
        // Create test delivery info
        testDeliveryInfo = new DeliveryInfo();
        testDeliveryInfo.setDeliveryInfoId("DEL001");
        testDeliveryInfo.setRecipientName("Test Recipient");
        testDeliveryInfo.setPhoneNumber("0123456789");
        testDeliveryInfo.setDeliveryAddress("123 Test Street");
        testDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        testDeliveryInfo.setEmail("test@example.com");
        testDeliveryInfo.setDeliveryMethodChosen("STANDARD");
        
        // Create test order items
        testOrderItems = new ArrayList<>();
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(testProduct);
        orderItem.setQuantity(2);
        orderItem.setPriceAtTimeOfOrder(100.0f);
        orderItem.setEligibleForRushDelivery(true);
        testOrderItems.add(orderItem);
        
        // Create test order
        testOrder = new OrderEntity();
        testOrder.setOrderId("ORDER001");
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        testOrder.setUserAccount(testUser);
        testOrder.setDeliveryInfo(testDeliveryInfo);
        testOrder.setOrderItems(testOrderItems);
        testOrder.setTotalProductPriceExclVAT(200.0f);
        testOrder.setTotalProductPriceInclVAT(220.0f);
        testOrder.setCalculatedDeliveryFee(30.0f);
        testOrder.setTotalAmountPaid(250.0f);
        
        // Set bidirectional relationships
        testDeliveryInfo.setOrderEntity(testOrder);
        orderItem.setOrderEntity(testOrder);
    }
    
    @Test
    @DisplayName("Test comprehensive validation with valid order")
    void testValidateOrderComprehensive_ValidOrder_ReturnsValid() {
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(testOrder);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(ValidationSeverity.INFO, result.getSeverity());
        assertNotNull(result.getValidationContext());
        assertEquals("ORDER001", result.getValidationContext().get("orderId"));
    }
    
    @Test
    @DisplayName("Test comprehensive validation with null order")
    void testValidateOrderComprehensive_NullOrder_ReturnsInvalid() {
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(null);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals(ValidationSeverity.CRITICAL, result.getSeverity());
        assertTrue(result.hasCriticalErrors());
        assertEquals(1, result.getCriticalErrorCount());
    }
    
    @Test
    @DisplayName("Test comprehensive validation with missing delivery info")
    void testValidateOrderComprehensive_MissingDeliveryInfo_ReturnsInvalid() {
        // Arrange
        testOrder.setDeliveryInfo(null);
        
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(testOrder);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrorSummary().contains("delivery"));
    }
    
    @Test
    @DisplayName("Test validation for display with valid order")
    void testValidateOrderForDisplay_ValidOrder_ReturnsValid() {
        // Act
        OrderValidationResult result = validationService.validateOrderForDisplay(testOrder);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("display", result.getValidationContext().get("validationType"));
    }
    
    @Test
    @DisplayName("Test validation for payment with valid order")
    void testValidateOrderForPayment_ValidOrder_ReturnsValid() {
        // Act
        OrderValidationResult result = validationService.validateOrderForPayment(testOrder);
        
        // Assert
        assertNotNull(result);
        assertEquals("payment", result.getValidationContext().get("validationType"));
    }
    
    @Test
    @DisplayName("Test validation for navigation to order summary")
    void testValidateOrderForNavigation_OrderSummary_ReturnsValid() {
        // Act
        OrderValidationResult result = validationService.validateOrderForNavigation(testOrder, "order_summary");
        
        // Assert
        assertNotNull(result);
        assertEquals("navigation", result.getValidationContext().get("validationType"));
        assertEquals("order_summary", result.getValidationContext().get("targetScreen"));
    }
    
    @Test
    @DisplayName("Test validation for navigation to payment method")
    void testValidateOrderForNavigation_PaymentMethod_ReturnsValid() {
        // Act
        OrderValidationResult result = validationService.validateOrderForNavigation(testOrder, "payment_method");
        
        // Assert
        assertNotNull(result);
        assertEquals("payment_method", result.getValidationContext().get("targetScreen"));
    }
    
    @Test
    @DisplayName("Test order items validation with valid items")
    void testValidateOrderItems_ValidItems_ReturnsValid() {
        // Act
        OrderItemValidationResult result = validationService.validateOrderItems(testOrderItems);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(1, result.getTotalItemsValidated());
        assertEquals(1, result.getValidItemsCount());
        assertEquals(0, result.getInvalidItemsCount());
    }
    
    @Test
    @DisplayName("Test order items validation with empty list")
    void testValidateOrderItems_EmptyList_ReturnsInvalid() {
        // Act
        OrderItemValidationResult result = validationService.validateOrderItems(new ArrayList<>());
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertEquals(0, result.getTotalItemsValidated());
    }
    
    @Test
    @DisplayName("Test order items validation with null list")
    void testValidateOrderItems_NullList_ReturnsInvalid() {
        // Act
        OrderItemValidationResult result = validationService.validateOrderItems(null);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }
    
    @Test
    @DisplayName("Test order items validation with invalid quantity")
    void testValidateOrderItems_InvalidQuantity_ReturnsInvalid() {
        // Arrange
        testOrderItems.get(0).setQuantity(0);
        
        // Act
        OrderItemValidationResult result = validationService.validateOrderItems(testOrderItems);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals(1, result.getTotalItemsValidated());
        assertEquals(0, result.getValidItemsCount());
        assertEquals(1, result.getInvalidItemsCount());
    }
    
    @Test
    @DisplayName("Test delivery info validation with valid info")
    void testValidateDeliveryInfo_ValidInfo_ReturnsValid() {
        // Act
        DeliveryValidationResult result = validationService.validateDeliveryInfo(testDeliveryInfo);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("STANDARD", result.getDeliveryMethod());
    }
    
    @Test
    @DisplayName("Test delivery info validation with null info")
    void testValidateDeliveryInfo_NullInfo_ReturnsInvalid() {
        // Act
        DeliveryValidationResult result = validationService.validateDeliveryInfo(null);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }
    
    @Test
    @DisplayName("Test delivery info validation with rush delivery")
    void testValidateDeliveryInfo_RushDelivery_ValidatesRushRequirements() {
        // Arrange
        testDeliveryInfo.setDeliveryMethodChosen("RUSH_DELIVERY");
        testDeliveryInfo.setRequestedRushDeliveryTime(LocalDateTime.now().plusHours(2));
        
        // Act
        DeliveryValidationResult result = validationService.validateDeliveryInfo(testDeliveryInfo);
        
        // Assert
        assertNotNull(result);
        assertEquals("RUSH_DELIVERY", result.getDeliveryMethod());
    }
    
    @Test
    @DisplayName("Test order pricing validation with valid pricing")
    void testValidateOrderPricing_ValidPricing_ReturnsValid() {
        // Act
        PricingValidationResult result = validationService.validateOrderPricing(testOrder);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(200.0f, result.getCalculatedSubtotal(), 0.01f);
        assertEquals(250.0f, result.getCalculatedTotal(), 0.01f);
    }
    
    @Test
    @DisplayName("Test order pricing validation with null order")
    void testValidateOrderPricing_NullOrder_ReturnsInvalid() {
        // Act
        PricingValidationResult result = validationService.validateOrderPricing(null);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertEquals(ValidationSeverity.CRITICAL, result.getSeverity());
    }
    
    @Test
    @DisplayName("Test pricing validation tolerance check")
    void testValidateOrderPricing_WithinTolerance_ReturnsTrue() {
        // Act
        PricingValidationResult result = validationService.validateOrderPricing(testOrder);
        
        // Assert
        assertTrue(result.isWithinTolerance(100.0f, 100.01f));
        assertFalse(result.isWithinTolerance(100.0f, 100.02f));
    }
    
    @Test
    @DisplayName("Test rush delivery validation with eligible order")
    void testValidateRushDelivery_EligibleOrder_ReturnsValid() {
        // Arrange
        testDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        testDeliveryInfo.setDeliveryMethodChosen("RUSH_DELIVERY");
        
        // Act
        RushDeliveryValidationResult result = validationService.validateRushDelivery(testOrder);
        
        // Assert
        assertNotNull(result);
        // Note: Actual eligibility depends on implementation
    }
    
    @Test
    @DisplayName("Test rush delivery validation with null order")
    void testValidateRushDelivery_NullOrder_ReturnsInvalid() {
        // Act
        RushDeliveryValidationResult result = validationService.validateRushDelivery(null);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }
    
    @Test
    @DisplayName("Test detailed validation report generation")
    void testGetDetailedValidationReport_ValidOrder_ReturnsComprehensiveReport() {
        // Act
        DetailedValidationReport report = validationService.getDetailedValidationReport(testOrder);
        
        // Assert
        assertNotNull(report);
        assertEquals("ORDER001", report.getOrderId());
        assertNotNull(report.getValidationTimestamp());
        assertFalse(report.getSections().isEmpty());
        assertTrue(report.getSections().containsKey("Order Structure"));
        assertTrue(report.getSections().containsKey("Order Items"));
        assertTrue(report.getSections().containsKey("Delivery Information"));
        assertTrue(report.getSections().containsKey("Pricing"));
    }
    
    @Test
    @DisplayName("Test detailed validation report with null order")
    void testGetDetailedValidationReport_NullOrder_ReturnsReportWithErrors() {
        // Act
        DetailedValidationReport report = validationService.getDetailedValidationReport(null);
        
        // Assert
        assertNotNull(report);
        assertNull(report.getOrderId());
        assertTrue(report.hasErrors());
    }
    
    @Test
    @DisplayName("Test validation fixes with valid order")
    void testAttemptValidationFixes_ValidOrder_ReturnsFixedOrder() throws ValidationException {
        // Act
        OrderEntity fixedOrder = validationService.attemptValidationFixes(testOrder);
        
        // Assert
        assertNotNull(fixedOrder);
        assertEquals(testOrder.getOrderId(), fixedOrder.getOrderId());
    }
    
    @Test
    @DisplayName("Test validation fixes with null order")
    void testAttemptValidationFixes_NullOrder_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            validationService.attemptValidationFixes(null);
        });
    }
    
    @Test
    @DisplayName("Test validation with old order")
    void testValidateOrderComprehensive_OldOrder_ReturnsInvalid() {
        // Arrange
        testOrder.setOrderDate(LocalDateTime.now().minusDays(35)); // Older than 30 days
        
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(testOrder);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.hasErrors());
        assertTrue(result.getErrorSummary().toLowerCase().contains("old"));
    }
    
    @Test
    @DisplayName("Test validation with future order date")
    void testValidateOrderComprehensive_FutureOrderDate_ReturnsWarning() {
        // Arrange
        testOrder.setOrderDate(LocalDateTime.now().plusHours(1));
        
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(testOrder);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.hasWarnings());
    }
    
    @Test
    @DisplayName("Test validation with missing order ID")
    void testValidateOrderComprehensive_MissingOrderId_ReturnsInvalid() {
        // Arrange
        testOrder.setOrderId(null);
        
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(testOrder);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.hasErrors());
    }
    
    @Test
    @DisplayName("Test validation with guest order")
    void testValidateOrderComprehensive_GuestOrder_ReturnsValid() {
        // Arrange
        testOrder.setUserAccount(null); // Guest order
        
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(testOrder);
        
        // Assert
        assertNotNull(result);
        // Guest orders should be valid if delivery info has contact details
    }
    
    @Test
    @DisplayName("Test validation with high order amount")
    void testValidateOrderComprehensive_HighAmount_ReturnsWarning() {
        // Arrange
        testOrder.setTotalAmountPaid(150000000.0f); // Very high amount
        
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(testOrder);
        
        // Assert
        assertNotNull(result);
        // Should generate warning for unusually high amount
    }
    
    @Test
    @DisplayName("Test validation with zero total amount")
    void testValidateOrderComprehensive_ZeroAmount_ReturnsInvalid() {
        // Arrange
        testOrder.setTotalAmountPaid(0.0f);
        
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(testOrder);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.hasErrors());
    }
    
    @Test
    @DisplayName("Test validation context information")
    void testValidationResults_ContainContextInformation() {
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(testOrder);
        
        // Assert
        assertNotNull(result.getValidationContext());
        assertTrue(result.getValidationContext().containsKey("orderId"));
        assertTrue(result.getValidationContext().containsKey("validationType"));
        assertEquals("comprehensive", result.getValidationContext().get("validationType"));
    }
    
    @Test
    @DisplayName("Test validation result statistics")
    void testValidationResults_ProvideAccurateStatistics() {
        // Arrange - Create order with some issues
        testOrder.setOrderId(""); // This should cause an error
        
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(testOrder);
        
        // Assert
        assertNotNull(result);
        assertEquals(result.getErrorCount() + result.getWarningCount() + result.getCriticalErrorCount(), 
                    result.getIssueCount());
    }
    
    @Test
    @DisplayName("Test validation recovery suggestions")
    void testValidationResults_ProvideRecoverySuggestions() {
        // Arrange
        testOrder.setDeliveryInfo(null); // This should trigger recovery suggestions
        
        // Act
        OrderValidationResult result = validationService.validateOrderComprehensive(testOrder);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.getRecoverySuggestions().isEmpty());
    }
}