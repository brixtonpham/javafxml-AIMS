package com.aims.core.application.impl;

import com.aims.core.application.services.IOrderValidationService;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.Product;
import com.aims.core.entities.UserAccount;
import com.aims.core.enums.OrderStatus;
import com.aims.core.infrastructure.database.dao.IOrderEntityDAO;
import com.aims.core.infrastructure.database.dao.IOrderItemDAO;
import com.aims.core.infrastructure.database.dao.IDeliveryInfoDAO;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import com.aims.core.infrastructure.database.dao.IInvoiceDAO;
import com.aims.core.infrastructure.database.dao.IPaymentTransactionDAO;
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

@DisplayName("Order Validation Service Tests")
public class OrderValidationServiceImplTest {

    @Mock
    private IOrderEntityDAO orderEntityDAO;
    
    @Mock
    private IOrderItemDAO orderItemDAO;
    
    @Mock
    private IDeliveryInfoDAO deliveryInfoDAO;
    
    @Mock
    private IProductDAO productDAO;
    
    @Mock
    private IUserAccountDAO userAccountDAO;
    
    @Mock
    private IInvoiceDAO invoiceDAO;
    
    @Mock
    private IPaymentTransactionDAO paymentTransactionDAO;

    private IOrderValidationService orderValidationService;
    private OrderEntity sampleOrder;
    private DeliveryInfo sampleDeliveryInfo;
    private OrderItem sampleOrderItem;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderValidationService = new OrderValidationServiceImpl(
            orderEntityDAO,
            orderItemDAO,
            deliveryInfoDAO,
            productDAO,
            userAccountDAO,
            invoiceDAO,
            paymentTransactionDAO
        );
        
        // Setup sample entities
        setupSampleEntities();
    }

    private void setupSampleEntities() {
        // Sample product
        sampleProduct = new Product();
        sampleProduct.setProductId("PROD-001");
        sampleProduct.setTitle("Sample Product");
        sampleProduct.setPrice(100.0f);
        sampleProduct.setQuantityInStock(10);

        // Sample delivery info
        sampleDeliveryInfo = new DeliveryInfo();
        sampleDeliveryInfo.setDeliveryInfoId("DINFO-001");
        sampleDeliveryInfo.setRecipientName("John Doe");
        sampleDeliveryInfo.setPhoneNumber("1234567890");
        sampleDeliveryInfo.setDeliveryAddress("123 Test Street");
        sampleDeliveryInfo.setEmail("john@example.com");

        // Sample order item
        sampleOrderItem = new OrderItem();
        sampleOrderItem.setProduct(sampleProduct);
        sampleOrderItem.setQuantity(2);
        sampleOrderItem.setPriceAtTimeOfOrder(100.0f);

        // Sample order
        sampleOrder = new OrderEntity();
        sampleOrder.setOrderId("ORD-001");
        sampleOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        sampleOrder.setOrderDate(LocalDateTime.now());
        sampleOrder.setTotalProductPriceExclVAT(200.0f);
        sampleOrder.setTotalProductPriceInclVAT(220.0f);
        sampleOrder.setCalculatedDeliveryFee(25.0f);
        sampleOrder.setTotalAmountPaid(245.0f);
        sampleOrder.setDeliveryInfo(sampleDeliveryInfo);
        
        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(sampleOrderItem);
        sampleOrder.setOrderItems(orderItems);
        
        // Set up bidirectional relationships
        sampleDeliveryInfo.setOrderEntity(sampleOrder);
        sampleOrderItem.setOrderEntity(sampleOrder);
    }

    @Test
    @DisplayName("Should return true when order is ready for payment")
    void testIsOrderReadyForPayment_ValidOrder_ReturnsTrue() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act
        boolean result = orderValidationService.isOrderReadyForPayment("ORD-001");

        // Assert
        assertTrue(result);
        verify(orderEntityDAO).getById("ORD-001");
    }

    @Test
    @DisplayName("Should return false when order status is not PENDING_PAYMENT")
    void testIsOrderReadyForPayment_InvalidStatus_ReturnsFalse() throws SQLException, ResourceNotFoundException {
        // Arrange
        sampleOrder.setOrderStatus(OrderStatus.SHIPPING);
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act
        boolean result = orderValidationService.isOrderReadyForPayment("ORD-001");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when order not found")
    void testIsOrderReadyForPayment_OrderNotFound_ThrowsException() throws SQLException {
        // Arrange
        when(orderEntityDAO.getById("ORD-001")).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderValidationService.isOrderReadyForPayment("ORD-001");
        });
    }

    @Test
    @DisplayName("Should return validated order when order is valid for payment")
    void testGetValidatedOrderForPayment_ValidOrder_ReturnsOrder() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act
        OrderEntity result = orderValidationService.getValidatedOrderForPayment("ORD-001");

        // Assert
        assertNotNull(result);
        assertEquals("ORD-001", result.getOrderId());
        assertEquals(OrderStatus.PENDING_PAYMENT, result.getOrderStatus());
        verify(orderEntityDAO).getById("ORD-001");
    }

    @Test
    @DisplayName("Should throw ValidationException when order has no delivery info")
    void testGetValidatedOrderForPayment_NoDeliveryInfo_ThrowsException() throws SQLException {
        // Arrange
        sampleOrder.setDeliveryInfo(null);
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
        
        assertTrue(exception.getMessage().contains("Delivery information is required"));
    }

    @Test
    @DisplayName("Should throw ValidationException when delivery info is incomplete")
    void testGetValidatedOrderForPayment_IncompleteDeliveryInfo_ThrowsException() throws SQLException {
        // Arrange
        sampleDeliveryInfo.setRecipientName(""); // Invalid empty name
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
        
        assertTrue(exception.getMessage().contains("Complete delivery information"));
    }

    @Test
    @DisplayName("Should return true when order integrity is valid")
    void testValidateOrderIntegrity_ValidOrder_ReturnsTrue() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);
        when(productDAO.getById("PROD-001")).thenReturn(sampleProduct);

        // Act
        boolean result = orderValidationService.validateOrderIntegrity("ORD-001");

        // Assert
        assertTrue(result);
        verify(orderEntityDAO).getById("ORD-001");
        verify(productDAO).getById("PROD-001");
    }

    @Test
    @DisplayName("Should return false when product stock is insufficient")
    void testValidateOrderIntegrity_InsufficientStock_ReturnsFalse() throws SQLException, ResourceNotFoundException {
        // Arrange
        sampleProduct.setQuantityInStock(1); // Less than order quantity (2)
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);
        when(productDAO.getById("PROD-001")).thenReturn(sampleProduct);

        // Act
        boolean result = orderValidationService.validateOrderIntegrity("ORD-001");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return true when order exists")
    void testOrderExists_ExistingOrder_ReturnsTrue() throws SQLException {
        // Arrange
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act
        boolean result = orderValidationService.orderExists("ORD-001");

        // Assert
        assertTrue(result);
        verify(orderEntityDAO).getById("ORD-001");
    }

    @Test
    @DisplayName("Should return false when order does not exist")
    void testOrderExists_NonExistentOrder_ReturnsFalse() throws SQLException {
        // Arrange
        when(orderEntityDAO.getById("ORD-001")).thenReturn(null);

        // Act
        boolean result = orderValidationService.orderExists("ORD-001");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should validate business rules successfully for valid order")
    void testValidateOrderBusinessRules_ValidOrder_NoException() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            orderValidationService.validateOrderBusinessRules(sampleOrder);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when order has no items")
    void testValidateOrderBusinessRules_NoItems_ThrowsException() {
        // Arrange
        sampleOrder.setOrderItems(new ArrayList<>());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderValidationService.validateOrderBusinessRules(sampleOrder);
        });
        
        assertTrue(exception.getMessage().contains("must contain at least one item"));
    }

    @Test
    @DisplayName("Should throw ValidationException when order item has zero quantity")
    void testValidateOrderBusinessRules_ZeroQuantity_ThrowsException() {
        // Arrange
        sampleOrderItem.setQuantity(0);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderValidationService.validateOrderBusinessRules(sampleOrder);
        });
        
        assertTrue(exception.getMessage().contains("positive quantities"));
    }

    @Test
    @DisplayName("Should throw ValidationException when total amounts are inconsistent")
    void testValidateOrderBusinessRules_InconsistentAmounts_ThrowsException() {
        // Arrange
        sampleOrder.setTotalAmountPaid(999.0f); // Inconsistent with calculated total

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderValidationService.validateOrderBusinessRules(sampleOrder);
        });
        
        assertTrue(exception.getMessage().contains("does not match calculated total"));
    }

    // =================== DELIVERY ADDRESS VALIDATION TESTS ===================

    @Test
    @DisplayName("Should accept valid short addresses with numbers")
    void testValidateDeliveryAddress_ValidShortWithNumbers_Success() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        sampleDeliveryInfo.setDeliveryAddress("123 Main"); // 8 characters, has numbers
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
    }

    @Test
    @DisplayName("Should accept valid short addresses with street abbreviations")
    void testValidateDeliveryAddress_ValidShortWithStreetAbbrev_Success() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        sampleDeliveryInfo.setDeliveryAddress("5th Ave"); // 7 characters, has street abbreviation
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
    }

    @Test
    @DisplayName("Should accept valid short addresses with PO Box")
    void testValidateDeliveryAddress_ValidShortWithPOBox_Success() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        sampleDeliveryInfo.setDeliveryAddress("PO Box 1"); // 8 characters, is PO Box
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
    }

    @Test
    @DisplayName("Should accept valid short addresses with unit designations")
    void testValidateDeliveryAddress_ValidShortWithUnit_Success() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        sampleDeliveryInfo.setDeliveryAddress("Unit 5A"); // 7 characters, has unit designation
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
    }

    @Test
    @DisplayName("Should accept addresses exactly 10 characters or longer")
    void testValidateDeliveryAddress_TenCharactersOrLonger_Success() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        sampleDeliveryInfo.setDeliveryAddress("1234567890"); // exactly 10 characters
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });

        // Test longer address
        sampleDeliveryInfo.setDeliveryAddress("123 Very Long Street Name"); // much longer
        assertDoesNotThrow(() -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
    }

    @Test
    @DisplayName("Should reject addresses shorter than 5 characters")
    void testValidateDeliveryAddress_TooShort_ThrowsException() throws SQLException {
        // Arrange
        sampleDeliveryInfo.setDeliveryAddress("ABC"); // 3 characters - too short
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
        
        assertTrue(exception.getMessage().contains("Address too short"));
        assertTrue(exception.getMessage().contains("street number, street name, and city/district"));
    }

    @Test
    @DisplayName("Should reject invalid short addresses without essential components")
    void testValidateDeliveryAddress_InvalidShort_ThrowsException() throws SQLException {
        // Arrange
        sampleDeliveryInfo.setDeliveryAddress("Home"); // 4 characters, no numbers/street info
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
        
        assertTrue(exception.getMessage().contains("Address too short"));

        // Test another invalid short address
        sampleDeliveryInfo.setDeliveryAddress("Here"); // 4 characters, no useful info
        exception = assertThrows(ValidationException.class, () -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
        assertTrue(exception.getMessage().contains("Address too short"));
    }

    @Test
    @DisplayName("Should reject short addresses (5-9 chars) without essential components")
    void testValidateDeliveryAddress_ShortWithoutComponents_ThrowsException() throws SQLException {
        // Arrange
        sampleDeliveryInfo.setDeliveryAddress("MyPlace"); // 7 characters, no numbers/street components
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
        
        assertTrue(exception.getMessage().contains("Please provide more address details"));
        assertTrue(exception.getMessage().contains("street number, street name"));
        assertTrue(exception.getMessage().contains("Example: '123 Main Street, Apt 2A'"));
    }

    @Test
    @DisplayName("Should provide enhanced error message for invalid phone numbers")
    void testValidateDeliveryAddress_InvalidPhoneNumber_EnhancedErrorMessage() throws SQLException {
        // Arrange
        sampleDeliveryInfo.setPhoneNumber("abc123"); // Invalid phone format
        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderValidationService.getValidatedOrderForPayment("ORD-001");
        });
        
        assertTrue(exception.getMessage().contains("Please provide a valid phone number (10-15 digits)"));
        assertTrue(exception.getMessage().contains("Example: 0123456789 or +84 123 456 789"));
    }

    @Test
    @DisplayName("Should handle case insensitive validation for short addresses")
    void testValidateDeliveryAddress_CaseInsensitive_Success() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange - test various case combinations
        String[] validAddresses = {
            "PO BOX 1",    // uppercase PO Box
            "po box 2",    // lowercase PO Box
            "P.O. Box 3",  // mixed case P.O.
            "UNIT 4A",     // uppercase unit
            "apt 5b",      // lowercase apt
            "123 ST",      // uppercase street abbrev
            "456 ave"      // lowercase avenue abbrev
        };

        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert - All should pass
        for (String address : validAddresses) {
            sampleDeliveryInfo.setDeliveryAddress(address);
            assertDoesNotThrow(() -> {
                orderValidationService.getValidatedOrderForPayment("ORD-001");
            }, "Address should be valid: " + address);
        }
    }

    @Test
    @DisplayName("Should validate real-world short address examples")
    void testValidateDeliveryAddress_RealWorldExamples_Success() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange - test real-world short but valid addresses
        String[] realWorldAddresses = {
            "123 Main St",     // Classic short address
            "5th Ave",         // Avenue abbreviation
            "PO Box 1",        // Post office box
            "Unit 5A",         // Apartment unit
            "Suite 2B",        // Office suite
            "Apt #3C",         // Apartment with hash
            "789 Rd",          // Road abbreviation
            "45 Dr",           // Drive abbreviation
            "12 Ct",           // Court abbreviation
            "8 Pl"             // Place abbreviation
        };

        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert - All should pass
        for (String address : realWorldAddresses) {
            sampleDeliveryInfo.setDeliveryAddress(address);
            assertDoesNotThrow(() -> {
                orderValidationService.getValidatedOrderForPayment("ORD-001");
            }, "Real-world address should be valid: " + address);
        }
    }

    @Test
    @DisplayName("Should reject clearly insufficient short addresses")
    void testValidateDeliveryAddress_ClearlyInsufficient_ThrowsException() throws SQLException {
        // Arrange - test clearly insufficient addresses
        String[] insufficientAddresses = {
            "Home",      // Too vague
            "Here",      // Too vague
            "Office",    // Too vague
            "School",    // Too vague
            "Work"       // Too vague
        };

        when(orderEntityDAO.getById("ORD-001")).thenReturn(sampleOrder);

        // Act & Assert - All should fail appropriately
        for (String address : insufficientAddresses) {
            sampleDeliveryInfo.setDeliveryAddress(address);
            ValidationException exception = assertThrows(ValidationException.class, () -> {
                orderValidationService.getValidatedOrderForPayment("ORD-001");
            }, "Address should be rejected as insufficient: " + address);
            
            // Check that appropriate error message is provided
            assertTrue(exception.getMessage().contains("Address too short") ||
                      exception.getMessage().contains("Please provide more address details"),
                      "Should provide helpful error message for: " + address);
        }
    }
}