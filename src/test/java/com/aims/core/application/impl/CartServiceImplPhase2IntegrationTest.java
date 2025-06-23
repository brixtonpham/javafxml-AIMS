package com.aims.core.application.impl;

import com.aims.core.application.services.IStockValidationService;
import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.Product;
import com.aims.core.entities.UserAccount;
import com.aims.core.infrastructure.database.dao.ICartDAO;
import com.aims.core.infrastructure.database.dao.ICartItemDAO;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import com.aims.core.shared.exceptions.InventoryException;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 2 Integration Tests for CartServiceImpl with StockValidationService
 * Tests the integration between CartService and StockValidationService for real-time stock validation
 */
@DisplayName("CartServiceImpl Phase 2 - StockValidationService Integration Tests")
public class CartServiceImplPhase2IntegrationTest {

    @Mock
    private ICartDAO cartDAO;
    
    @Mock
    private ICartItemDAO cartItemDAO;
    
    @Mock
    private IProductDAO productDAO;
    
    @Mock
    private IUserAccountDAO userAccountDAO;
    
    @Mock
    private IStockValidationService stockValidationService;

    private CartServiceImpl cartService;
    private Product testProduct;
    private Cart testCart;
    private String testCartSessionId;
    private String testProductId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize CartServiceImpl with StockValidationService
        cartService = new CartServiceImpl(cartDAO, cartItemDAO, productDAO, userAccountDAO, stockValidationService);
        
        // Set up test data
        testCartSessionId = "test-cart-session-123";
        testProductId = "PROD-001";
        
        // Create test product
        testProduct = new Product();
        testProduct.setProductId(testProductId);
        testProduct.setTitle("Test Product");
        testProduct.setQuantityInStock(10);
        testProduct.setPrice(99.99f);
        
        // Create test cart
        testCart = new Cart(testCartSessionId, null, LocalDateTime.now());
        testCart.setItems(new ArrayList<>());
    }

    @Test
    @DisplayName("Should successfully add item to cart when stock validation passes")
    void testAddItemToCart_StockValidationPasses_Success() throws Exception {
        // Arrange
        int quantityToAdd = 5;
        
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                true, testProductId, "Test Product", quantityToAdd, 
                10, 0, 10, "Stock available", "STOCK_OK"
            );
        
        when(productDAO.getById(testProductId)).thenReturn(testProduct);
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);
        when(stockValidationService.validateProductStock(testProductId, quantityToAdd))
            .thenReturn(validationResult);
        when(cartDAO.saveOrUpdate(any(Cart.class))).thenReturn(testCart);
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);

        // Act
        Cart result = cartService.addItemToCart(testCartSessionId, testProductId, quantityToAdd);

        // Assert
        assertNotNull(result);
        verify(stockValidationService).validateProductStock(testProductId, quantityToAdd);
        verify(cartItemDAO).add(any(CartItem.class));
        verify(cartDAO).saveOrUpdate(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw InventoryException when stock validation fails for addItemToCart")
    void testAddItemToCart_StockValidationFails_ThrowsInventoryException() throws Exception {
        // Arrange
        int quantityToAdd = 15; // More than available stock
        
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                false, testProductId, "Test Product", quantityToAdd, 
                10, 0, 10, "Insufficient stock", "INSUFFICIENT_STOCK"
            );
        
        when(productDAO.getById(testProductId)).thenReturn(testProduct);
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);
        when(stockValidationService.validateProductStock(testProductId, quantityToAdd))
            .thenReturn(validationResult);

        // Act & Assert
        InventoryException exception = assertThrows(InventoryException.class, () -> {
            cartService.addItemToCart(testCartSessionId, testProductId, quantityToAdd);
        });
        
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        assertTrue(exception.getMessage().contains("Test Product"));
        verify(stockValidationService).validateProductStock(testProductId, quantityToAdd);
        verify(cartItemDAO, never()).add(any(CartItem.class));
    }

    @Test
    @DisplayName("Should use StockValidationService for existing item quantity update in addItemToCart")
    void testAddItemToCart_ExistingItem_UsesStockValidationService() throws Exception {
        // Arrange
        int existingQuantity = 3;
        int quantityToAdd = 2;
        int totalRequired = existingQuantity + quantityToAdd;
        
        // Create existing cart item
        CartItem existingItem = new CartItem(testCart, testProduct, existingQuantity);
        testCart.getItems().add(existingItem);
        
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                true, testProductId, "Test Product", totalRequired, 
                10, 0, 10, "Stock available", "STOCK_OK"
            );
        
        when(productDAO.getById(testProductId)).thenReturn(testProduct);
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);
        when(stockValidationService.validateProductStock(testProductId, totalRequired))
            .thenReturn(validationResult);
        when(cartDAO.saveOrUpdate(any(Cart.class))).thenReturn(testCart);
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);

        // Act
        Cart result = cartService.addItemToCart(testCartSessionId, testProductId, quantityToAdd);

        // Assert
        assertNotNull(result);
        verify(stockValidationService).validateProductStock(testProductId, totalRequired);
        verify(cartItemDAO).update(any(CartItem.class));
        assertEquals(totalRequired, existingItem.getQuantity());
    }

    @Test
    @DisplayName("Should successfully update item quantity when stock validation passes")
    void testUpdateItemQuantity_StockValidationPasses_Success() throws Exception {
        // Arrange
        int newQuantity = 7;
        CartItem existingItem = new CartItem(testCart, testProduct, 3);
        testCart.getItems().add(existingItem);
        
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                true, testProductId, "Test Product", newQuantity, 
                10, 0, 10, "Stock available", "STOCK_OK"
            );
        
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);
        when(productDAO.getById(testProductId)).thenReturn(testProduct);
        when(stockValidationService.validateProductStock(testProductId, newQuantity))
            .thenReturn(validationResult);
        when(cartDAO.saveOrUpdate(any(Cart.class))).thenReturn(testCart);
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);

        // Act
        Cart result = cartService.updateItemQuantity(testCartSessionId, testProductId, newQuantity);

        // Assert
        assertNotNull(result);
        verify(stockValidationService).validateProductStock(testProductId, newQuantity);
        verify(cartItemDAO).update(any(CartItem.class));
        assertEquals(newQuantity, existingItem.getQuantity());
    }

    @Test
    @DisplayName("Should throw InventoryException when stock validation fails for updateItemQuantity")
    void testUpdateItemQuantity_StockValidationFails_ThrowsInventoryException() throws Exception {
        // Arrange
        int newQuantity = 15; // More than available stock
        CartItem existingItem = new CartItem(testCart, testProduct, 3);
        testCart.getItems().add(existingItem);
        
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                false, testProductId, "Test Product", newQuantity, 
                10, 0, 10, "Insufficient stock", "INSUFFICIENT_STOCK"
            );
        
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);
        when(productDAO.getById(testProductId)).thenReturn(testProduct);
        when(stockValidationService.validateProductStock(testProductId, newQuantity))
            .thenReturn(validationResult);

        // Act & Assert
        InventoryException exception = assertThrows(InventoryException.class, () -> {
            cartService.updateItemQuantity(testCartSessionId, testProductId, newQuantity);
        });
        
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        assertTrue(exception.getMessage().contains("Test Product"));
        verify(stockValidationService).validateProductStock(testProductId, newQuantity);
        verify(cartItemDAO, never()).update(any(CartItem.class));
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException from StockValidationService")
    void testAddItemToCart_ProductNotFoundInStockValidation_ThrowsInventoryException() throws Exception {
        // Arrange
        int quantityToAdd = 5;
        
        when(productDAO.getById(testProductId)).thenReturn(testProduct);
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);
        when(stockValidationService.validateProductStock(testProductId, quantityToAdd))
            .thenThrow(new ResourceNotFoundException("Product not found in stock validation"));

        // Act & Assert
        InventoryException exception = assertThrows(InventoryException.class, () -> {
            cartService.addItemToCart(testCartSessionId, testProductId, quantityToAdd);
        });
        
        assertTrue(exception.getMessage().contains("no longer available"));
        verify(stockValidationService).validateProductStock(testProductId, quantityToAdd);
        verify(cartItemDAO, never()).add(any(CartItem.class));
    }

    @Test
    @DisplayName("Should handle SQLException from StockValidationService")
    void testAddItemToCart_SQLException_PropagatesSQLException() throws Exception {
        // Arrange
        int quantityToAdd = 5;
        
        when(productDAO.getById(testProductId)).thenReturn(testProduct);
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);
        when(stockValidationService.validateProductStock(testProductId, quantityToAdd))
            .thenThrow(new SQLException("Database connection error"));

        // Act & Assert
        SQLException exception = assertThrows(SQLException.class, () -> {
            cartService.addItemToCart(testCartSessionId, testProductId, quantityToAdd);
        });
        
        assertTrue(exception.getMessage().contains("Failed to add item to cart"));
        verify(stockValidationService).validateProductStock(testProductId, quantityToAdd);
        verify(cartItemDAO, never()).add(any(CartItem.class));
    }

    @Test
    @DisplayName("Should provide enhanced error messages from StockValidationService")
    void testStockValidationService_EnhancedErrorMessages() throws Exception {
        // Arrange
        int quantityToAdd = 12;
        String detailedMessage = "Stock reserved for other orders. Only 8 units available for immediate purchase.";
        
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                false, testProductId, "Test Product", quantityToAdd, 
                10, 2, 8, detailedMessage, "STOCK_RESERVED"
            );
        
        when(productDAO.getById(testProductId)).thenReturn(testProduct);
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);
        when(stockValidationService.validateProductStock(testProductId, quantityToAdd))
            .thenReturn(validationResult);

        // Act & Assert
        InventoryException exception = assertThrows(InventoryException.class, () -> {
            cartService.addItemToCart(testCartSessionId, testProductId, quantityToAdd);
        });
        
        // Verify enhanced error message includes StockValidationService details
        assertTrue(exception.getMessage().contains(detailedMessage));
        assertTrue(exception.getMessage().contains("available in stock: 8"));
        verify(stockValidationService).validateProductStock(testProductId, quantityToAdd);
    }

    @Test
    @DisplayName("Should correctly handle cart-aware validation with existing items")
    void testCartAwareValidation_WithExistingItems_CorrectTotalCalculation() throws Exception {
        // Arrange - Cart already has 4 items, trying to add 3 more (total 7)
        int existingQuantity = 4;
        int quantityToAdd = 3;
        int totalRequired = existingQuantity + quantityToAdd;
        
        CartItem existingItem = new CartItem(testCart, testProduct, existingQuantity);
        testCart.getItems().add(existingItem);
        
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                true, testProductId, "Test Product", totalRequired, 
                10, 0, 10, "Stock available", "STOCK_OK"
            );
        
        when(productDAO.getById(testProductId)).thenReturn(testProduct);
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);
        when(stockValidationService.validateProductStock(testProductId, totalRequired))
            .thenReturn(validationResult);
        when(cartDAO.saveOrUpdate(any(Cart.class))).thenReturn(testCart);
        when(cartDAO.getBySessionId(testCartSessionId)).thenReturn(testCart);

        // Act
        Cart result = cartService.addItemToCart(testCartSessionId, testProductId, quantityToAdd);

        // Assert
        assertNotNull(result);
        // Verify that validation was called with the total required quantity (4 + 3 = 7)
        verify(stockValidationService).validateProductStock(testProductId, 7);
        verify(cartItemDAO).update(any(CartItem.class));
        assertEquals(7, existingItem.getQuantity());
    }
}