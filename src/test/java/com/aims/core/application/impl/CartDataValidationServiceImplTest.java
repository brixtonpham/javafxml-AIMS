package com.aims.core.application.impl;

import com.aims.core.application.dtos.CartValidationResult;
import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.Product;
import com.aims.core.entities.UserAccount;
import com.aims.core.enums.ProductType;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartDataValidationServiceImplTest {

    @Mock
    private IProductDAO productDAO;

    private CartDataValidationServiceImpl validationService;
    private Cart testCart;
    private Product testProduct1;
    private Product testProduct2;
    private CartItem testCartItem1;
    private CartItem testCartItem2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validationService = new CartDataValidationServiceImpl(productDAO);

        // Create test products
        testProduct1 = new Product("PROD001", "Test Book", "Books", 100.0f, 150.0f, 
                                  10, "Test description", "test-image.jpg", "123456789", 
                                  "20x15x2", 0.5f, LocalDate.now(), ProductType.BOOK);
        testProduct2 = new Product("PROD002", "Test CD", "Music", 80.0f, 120.0f, 
                                  5, "Test CD description", "test-cd.jpg", "987654321", 
                                  "14x12x1", 0.1f, LocalDate.now(), ProductType.CD);

        // Create test cart
        testCart = new Cart("test-session-123", null, LocalDateTime.now());
        
        // Create test cart items
        testCartItem1 = new CartItem(testCart, testProduct1, 2);
        testCartItem2 = new CartItem(testCart, testProduct2, 1);
        
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(testCartItem1);
        cartItems.add(testCartItem2);
        testCart.setItems(cartItems);
    }

    @Test
    void validateCartForOrderCreation_ValidCart_ReturnsSuccess() throws SQLException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);

        // Act
        CartValidationResult result = validationService.validateCartForOrderCreation(testCart);

        // Assert
        assertTrue(result.isValid());
        assertEquals(2, result.getTotalItemsValidated());
        assertEquals(2, result.getValidItemsCount());
        assertEquals(0, result.getInvalidItemsCount());
        assertFalse(result.hasStockIssues());
        assertFalse(result.hasIntegrityIssues());
    }

    @Test
    void validateCartForOrderCreation_NullCart_ReturnsFailure() throws SQLException {
        // Act
        CartValidationResult result = validationService.validateCartForOrderCreation(null);

        // Assert
        assertFalse(result.isValid());
        assertEquals(1, result.getValidationErrors().size());
        assertEquals("Cart cannot be null", result.getValidationErrors().get(0));
    }

    @Test
    void validateCartForOrderCreation_EmptyCart_ReturnsFailure() throws SQLException {
        // Arrange
        testCart.setItems(new ArrayList<>());

        // Act
        CartValidationResult result = validationService.validateCartForOrderCreation(testCart);

        // Assert
        assertFalse(result.isValid());
        assertEquals(1, result.getValidationErrors().size());
        assertEquals("Cart is empty", result.getValidationErrors().get(0));
    }

    @Test
    void validateCartItemForOrderConversion_ValidItem_ReturnsTrue() throws SQLException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);

        // Act
        boolean result = validationService.validateCartItemForOrderConversion(testCartItem1);

        // Assert
        assertTrue(result);
        verify(productDAO).getById("PROD001");
    }

    @Test
    void validateCartItemForOrderConversion_NullItem_ReturnsFalse() throws SQLException {
        // Act
        boolean result = validationService.validateCartItemForOrderConversion(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateCartItemForOrderConversion_NullProduct_ReturnsFalse() throws SQLException {
        // Arrange
        testCartItem1.setProduct(null);

        // Act
        boolean result = validationService.validateCartItemForOrderConversion(testCartItem1);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateCartItemForOrderConversion_InvalidQuantity_ReturnsFalse() throws SQLException {
        // Arrange
        testCartItem1.setQuantity(0);

        // Act
        boolean result = validationService.validateCartItemForOrderConversion(testCartItem1);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateCartItemForOrderConversion_ProductNotFound_ReturnsFalse() throws SQLException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(null);

        // Act
        boolean result = validationService.validateCartItemForOrderConversion(testCartItem1);

        // Assert
        assertFalse(result);
        verify(productDAO).getById("PROD001");
    }

    @Test
    void validateCartItemForOrderConversion_InsufficientStock_ReturnsFalse() throws SQLException {
        // Arrange
        testProduct1.setQuantityInStock(1); // Less than requested quantity of 2
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);

        // Act
        boolean result = validationService.validateCartItemForOrderConversion(testCartItem1);

        // Assert
        assertFalse(result);
        verify(productDAO).getById("PROD001");
    }

    @Test
    void enrichCartWithProductMetadata_ValidCart_EnrichesSuccessfully() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);

        // Act
        Cart enrichedCart = validationService.enrichCartWithProductMetadata(testCart);

        // Assert
        assertNotNull(enrichedCart);
        assertEquals(2, enrichedCart.getItems().size());
        verify(productDAO, times(2)).getById(anyString());
    }

    @Test
    void enrichCartWithProductMetadata_NullCart_ThrowsException() {
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            validationService.enrichCartWithProductMetadata(null);
        });
    }

    @Test
    void enrichCartWithProductMetadata_ProductNotFound_ThrowsException() throws SQLException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            validationService.enrichCartWithProductMetadata(testCart);
        });
        verify(productDAO).getById("PROD001");
    }

    @Test
    void validateStockAvailability_AllStockAvailable_ReturnsTrue() throws SQLException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);

        // Act
        boolean result = validationService.validateStockAvailability(testCart);

        // Assert
        assertTrue(result);
        verify(productDAO, times(2)).getById(anyString());
    }

    @Test
    void validateStockAvailability_InsufficientStock_ReturnsFalse() throws SQLException {
        // Arrange
        testProduct1.setQuantityInStock(1); // Less than requested quantity of 2
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);

        // Act
        boolean result = validationService.validateStockAvailability(testCart);

        // Assert
        assertFalse(result);
        verify(productDAO, times(2)).getById(anyString());
    }

    @Test
    void validateStockAvailability_NullCart_ReturnsFalse() throws SQLException {
        // Act
        boolean result = validationService.validateStockAvailability(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateCartDataIntegrity_ValidCart_ReturnsTrue() throws SQLException {
        // Act
        boolean result = validationService.validateCartDataIntegrity(testCart);

        // Assert
        assertTrue(result);
    }

    @Test
    void validateCartDataIntegrity_NullCart_ReturnsFalse() throws SQLException {
        // Act
        boolean result = validationService.validateCartDataIntegrity(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateCartDataIntegrity_NullSessionId_ReturnsFalse() throws SQLException {
        // Arrange
        testCart.setCartSessionId(null);

        // Act
        boolean result = validationService.validateCartDataIntegrity(testCart);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateCartDataIntegrity_EmptySessionId_ReturnsFalse() throws SQLException {
        // Arrange
        testCart.setCartSessionId("");

        // Act
        boolean result = validationService.validateCartDataIntegrity(testCart);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateCartDataIntegrity_NullItems_ReturnsFalse() throws SQLException {
        // Arrange
        testCart.setItems(null);

        // Act
        boolean result = validationService.validateCartDataIntegrity(testCart);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateProductMetadataCompleteness_CompleteMetadata_ReturnsTrue() throws SQLException {
        // Act
        boolean result = validationService.validateProductMetadataCompleteness(testCart);

        // Assert
        assertTrue(result);
    }

    @Test
    void validateProductMetadataCompleteness_IncompleteMetadata_ReturnsFalse() throws SQLException {
        // Arrange
        testProduct1.setDescription(null); // Missing description
        testProduct1.setImageUrl(""); // Empty image URL

        // Act
        boolean result = validationService.validateProductMetadataCompleteness(testCart);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateProductMetadataCompleteness_NullCart_ReturnsFalse() throws SQLException {
        // Act
        boolean result = validationService.validateProductMetadataCompleteness(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateCartForOrderCreation_DatabaseError_HandlesGracefully() throws SQLException {
        // Arrange
        when(productDAO.getById(anyString())).thenThrow(new SQLException("Database connection failed"));

        // Act
        CartValidationResult result = validationService.validateCartForOrderCreation(testCart);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getValidationErrors().size() > 0);
        assertTrue(result.getValidationErrors().get(0).contains("Database error"));
    }
}