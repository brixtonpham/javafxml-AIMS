package com.aims.core.application.impl;

import com.aims.core.entities.Cart;
import com.aims.core.entities.Product;
import com.aims.core.entities.CD;
import com.aims.core.infrastructure.database.dao.ICartDAO;
import com.aims.core.infrastructure.database.dao.ICartItemDAO;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.InventoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for CartServiceImpl to verify FK constraint bug fixes
 * 
 * CRITICAL TEST: Ensures cart session ID consistency to prevent FK constraint violations
 */
public class CartServiceImplTest {

    @Mock
    private ICartDAO cartDAO;
    
    @Mock
    private ICartItemDAO cartItemDAO;
    
    @Mock
    private IProductDAO productDAO;
    
    @Mock
    private IUserAccountDAO userAccountDAO;

    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cartService = new CartServiceImpl(cartDAO, cartItemDAO, productDAO, userAccountDAO);
    }

    @Test
    void testAddItemToCart_NewCart_ConsistentSessionId() throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        // Arrange
        String cartSessionId = "test-session-123";
        String productId = "cd_001";
        int quantity = 1;
        
        // Mock product (Abbey Road CD)
        Product abbeyRoad = new CD();
        abbeyRoad.setProductId(productId);
        abbeyRoad.setTitle("Abbey Road");
        abbeyRoad.setPrice(19.99f);
        abbeyRoad.setQuantityInStock(15);
        
        // Mock DAO responses
        when(productDAO.getById(productId)).thenReturn(abbeyRoad);
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(null); // No existing cart
        
        // Mock cart creation and save
        doAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            // Verify that the cart has the correct session ID
            assertEquals(cartSessionId, cart.getCartSessionId());
            return null;
        }).when(cartDAO).saveOrUpdate(any(Cart.class));
        
        // Mock cart retrieval after creation
        Cart mockCart = new Cart(cartSessionId, null, LocalDateTime.now());
        when(cartDAO.getBySessionId(cartSessionId)).thenReturn(mockCart);

        // Act
        Cart result = cartService.addItemToCart(cartSessionId, productId, quantity);

        // Assert
        assertNotNull(result);
        assertEquals(cartSessionId, result.getCartSessionId());
        
        // Verify that cart was saved with correct session ID
        verify(cartDAO, atLeastOnce()).saveOrUpdate(argThat(cart -> 
            cartSessionId.equals(cart.getCartSessionId())
        ));
        
        // Verify that CartItem was added with correct cart reference
        verify(cartItemDAO).add(argThat(cartItem -> 
            cartItem.getCart() != null && 
            cartSessionId.equals(cartItem.getCart().getCartSessionId())
        ));
    }

    @Test
    void testAddItemToCart_InsufficientStock_ThrowsInventoryException() throws SQLException {
        // Arrange
        String cartSessionId = "test-session-456";
        String productId = "cd_001";
        int requestedQuantity = 20; // More than available
        
        Product abbeyRoad = new CD();
        abbeyRoad.setProductId(productId);
        abbeyRoad.setTitle("Abbey Road");
        abbeyRoad.setQuantityInStock(15); // Less than requested
        
        when(productDAO.getById(productId)).thenReturn(abbeyRoad);

        // Act & Assert
        InventoryException exception = assertThrows(InventoryException.class, () -> {
            cartService.addItemToCart(cartSessionId, productId, requestedQuantity);
        });
        
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        assertTrue(exception.getMessage().contains("Abbey Road"));
    }

    @Test
    void testAddItemToCart_ProductNotFound_ThrowsResourceNotFoundException() throws SQLException {
        // Arrange
        String cartSessionId = "test-session-789";
        String nonExistentProductId = "invalid_product";
        int quantity = 1;
        
        when(productDAO.getById(nonExistentProductId)).thenReturn(null);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cartService.addItemToCart(cartSessionId, nonExistentProductId, quantity);
        });
        
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testAddItemToCart_InvalidQuantity_ThrowsValidationException() {
        // Arrange
        String cartSessionId = "test-session-000";
        String productId = "cd_001";
        int invalidQuantity = 0; // Invalid quantity

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            cartService.addItemToCart(cartSessionId, productId, invalidQuantity);
        });
        
        assertTrue(exception.getMessage().contains("must be positive"));
    }
}
