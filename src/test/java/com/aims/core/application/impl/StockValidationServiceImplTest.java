package com.aims.core.application.impl;

import com.aims.core.application.services.IStockReservationService;
import com.aims.core.application.services.IStockValidationService;
import com.aims.core.application.services.IStockValidationService.*;
import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockValidationServiceImplTest {

    @Mock
    private IProductDAO productDAO;
    
    @Mock
    private IStockReservationService stockReservationService;
    
    private IStockValidationService stockValidationService;
    
    private Product testProduct1;
    private Product testProduct2;
    private CartItem testCartItem1;
    private CartItem testCartItem2;
    private Cart testCart;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stockValidationService = new StockValidationServiceImpl(productDAO, stockReservationService);
        
        setupTestData();
    }
    
    private void setupTestData() {
        // Test Product 1
        testProduct1 = new Product();
        testProduct1.setProductId("PROD001");
        testProduct1.setTitle("Test Product 1");
        testProduct1.setQuantityInStock(20);
        testProduct1.setPrice(50000f);
        
        // Test Product 2
        testProduct2 = new Product();
        testProduct2.setProductId("PROD002");
        testProduct2.setTitle("Test Product 2");
        testProduct2.setQuantityInStock(5);
        testProduct2.setPrice(75000f);
        
        // Test Cart Items
        testCartItem1 = new CartItem();
        testCartItem1.setProduct(testProduct1);
        testCartItem1.setQuantity(3);
        
        testCartItem2 = new CartItem();
        testCartItem2.setProduct(testProduct2);
        testCartItem2.setQuantity(2);
        
        // Test Cart
        testCart = new Cart();
        testCart.setCartSessionId("TEST_CART_001");
        testCart.setItems(Arrays.asList(testCartItem1, testCartItem2));
    }
    
    @Test
    void validateProductStock_ValidRequest_ReturnsValidResult() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(15);
        
        // Act
        StockValidationResult result = stockValidationService.validateProductStock("PROD001", 10);
        
        // Assert
        assertTrue(result.isValid());
        assertEquals("PROD001", result.getProductId());
        assertEquals("Test Product 1", result.getProductTitle());
        assertEquals(10, result.getRequestedQuantity());
        assertEquals(20, result.getActualStock());
        assertEquals(5, result.getReservedStock()); // 20 - 15
        assertEquals(15, result.getAvailableStock());
        assertEquals("STOCK_AVAILABLE", result.getReasonCode());
        assertEquals(0, result.getShortfallQuantity());
    }
    
    @Test
    void validateProductStock_InsufficientStock_ReturnsInvalidResult() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(5);
        
        // Act
        StockValidationResult result = stockValidationService.validateProductStock("PROD001", 10);
        
        // Assert
        assertFalse(result.isValid());
        assertEquals("PROD001", result.getProductId());
        assertEquals(10, result.getRequestedQuantity());
        assertEquals(5, result.getAvailableStock());
        assertEquals("INSUFFICIENT_STOCK", result.getReasonCode());
        assertEquals(5, result.getShortfallQuantity()); // 10 - 5
        assertTrue(result.getMessage().contains("Insufficient stock"));
    }
    
    @Test
    void validateProductStock_ProductNotFound_ThrowsResourceNotFoundException() throws SQLException {
        // Arrange
        when(productDAO.getById("INVALID_PROD")).thenReturn(null);
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            stockValidationService.validateProductStock("INVALID_PROD", 5));
        
        verify(productDAO).getById("INVALID_PROD");
        verifyNoInteractions(stockReservationService);
    }
    
    @Test
    void validateProductStock_NullProductId_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
            stockValidationService.validateProductStock(null, 5));
        
        verifyNoInteractions(productDAO);
        verifyNoInteractions(stockReservationService);
    }
    
    @Test
    void validateProductStock_EmptyProductId_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
            stockValidationService.validateProductStock("", 5));
        
        verifyNoInteractions(productDAO);
        verifyNoInteractions(stockReservationService);
    }
    
    @Test
    void validateProductStock_ZeroQuantity_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
            stockValidationService.validateProductStock("PROD001", 0));
        
        verifyNoInteractions(productDAO);
        verifyNoInteractions(stockReservationService);
    }
    
    @Test
    void validateProductStock_NegativeQuantity_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
            stockValidationService.validateProductStock("PROD001", -5));
        
        verifyNoInteractions(productDAO);
        verifyNoInteractions(stockReservationService);
    }
    
    @Test
    void validateBulkStock_AllItemsValid_ReturnsValidResult() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(15);
        when(stockReservationService.getAvailableStock("PROD002")).thenReturn(5);
        
        List<CartItem> items = Arrays.asList(testCartItem1, testCartItem2);
        
        // Act
        BulkStockValidationResult result = stockValidationService.validateBulkStock(items);
        
        // Assert
        assertTrue(result.isAllValid());
        assertEquals(2, result.getTotalProductsChecked());
        assertEquals(0, result.getTotalFailedProducts());
        assertEquals(2, result.getIndividualResults().size());
        assertTrue(result.getFailedValidations().isEmpty());
        assertFalse(result.hasFailures());
        assertEquals("All items passed stock validation", result.getOverallMessage());
    }
    
    @Test
    void validateBulkStock_SomeItemsInvalid_ReturnsPartiallyValidResult() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(15); // Valid: 3 requested, 15 available
        when(stockReservationService.getAvailableStock("PROD002")).thenReturn(1);  // Invalid: 2 requested, 1 available
        
        List<CartItem> items = Arrays.asList(testCartItem1, testCartItem2);
        
        // Act
        BulkStockValidationResult result = stockValidationService.validateBulkStock(items);
        
        // Assert
        assertFalse(result.isAllValid());
        assertEquals(2, result.getTotalProductsChecked());
        assertEquals(1, result.getTotalFailedProducts());
        assertEquals(2, result.getIndividualResults().size());
        assertEquals(1, result.getFailedValidations().size());
        assertTrue(result.hasFailures());
        assertEquals("1 out of 2 items failed stock validation", result.getOverallMessage());
        
        // Check the failed validation
        StockValidationResult failedResult = result.getFailedValidations().get(0);
        assertEquals("PROD002", failedResult.getProductId());
        assertFalse(failedResult.isValid());
    }
    
    @Test
    void validateBulkStock_EmptyList_ReturnsValidResult() throws SQLException {
        // Act
        BulkStockValidationResult result = stockValidationService.validateBulkStock(new ArrayList<>());
        
        // Assert
        assertTrue(result.isAllValid());
        assertEquals(0, result.getTotalProductsChecked());
        assertEquals(0, result.getTotalFailedProducts());
        assertTrue(result.getIndividualResults().isEmpty());
        assertTrue(result.getFailedValidations().isEmpty());
        assertEquals("No items to validate", result.getOverallMessage());
        
        verifyNoInteractions(productDAO);
        verifyNoInteractions(stockReservationService);
    }
    
    @Test
    void validateBulkStock_NullList_ReturnsValidResult() throws SQLException {
        // Act
        BulkStockValidationResult result = stockValidationService.validateBulkStock(null);
        
        // Assert
        assertTrue(result.isAllValid());
        assertEquals(0, result.getTotalProductsChecked());
        assertEquals("No items to validate", result.getOverallMessage());
        
        verifyNoInteractions(productDAO);
        verifyNoInteractions(stockReservationService);
    }
    
    @Test
    void validateCartStock_ValidCart_ReturnsValidResult() throws SQLException, ValidationException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(15);
        when(stockReservationService.getAvailableStock("PROD002")).thenReturn(5);
        
        // Act
        CartStockValidationResult result = stockValidationService.validateCartStock(testCart);
        
        // Assert
        assertTrue(result.isValid());
        assertEquals("TEST_CART_001", result.getCartSessionId());
        assertEquals(5, result.getTotalItemCount()); // 3 + 2
        assertEquals(275000f, result.getTotalCartValue(), 0.01f); // (50000*3) + (75000*2)
        assertTrue(result.getBulkValidationResult().isAllValid());
        assertTrue(result.getCartValidationMessage().contains("Cart validation passed"));
    }
    
    @Test
    void validateCartStock_InvalidCart_ReturnsInvalidResult() throws SQLException, ValidationException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(15);
        when(stockReservationService.getAvailableStock("PROD002")).thenReturn(1); // Insufficient for 2 requested
        
        // Act
        CartStockValidationResult result = stockValidationService.validateCartStock(testCart);
        
        // Assert
        assertFalse(result.isValid());
        assertEquals("TEST_CART_001", result.getCartSessionId());
        assertFalse(result.getBulkValidationResult().isAllValid());
        assertTrue(result.getCartValidationMessage().contains("Cart validation failed"));
        assertTrue(result.getCartValidationMessage().contains("1 items have stock issues"));
    }
    
    @Test
    void validateCartStock_NullCart_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
            stockValidationService.validateCartStock(null));
        
        verifyNoInteractions(productDAO);
        verifyNoInteractions(stockReservationService);
    }
    
    @Test
    void validateOrderItemsStock_ValidOrderItems_ReturnsValidResult() throws SQLException, ResourceNotFoundException {
        // Arrange
        OrderItem orderItem1 = new OrderItem();
        orderItem1.setProduct(testProduct1);
        orderItem1.setQuantity(3);
        
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setProduct(testProduct2);
        orderItem2.setQuantity(2);
        
        List<OrderItem> orderItems = Arrays.asList(orderItem1, orderItem2);
        
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(15);
        when(stockReservationService.getAvailableStock("PROD002")).thenReturn(5);
        
        // Act
        BulkStockValidationResult result = stockValidationService.validateOrderItemsStock(orderItems);
        
        // Assert
        assertTrue(result.isAllValid());
        assertEquals(2, result.getTotalProductsChecked());
        assertEquals(0, result.getTotalFailedProducts());
    }
    
    @Test
    void getStockInfo_ValidProduct_ReturnsStockInfo() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(15);
        
        // Act
        StockInfo stockInfo = stockValidationService.getStockInfo("PROD001");
        
        // Assert
        assertEquals("PROD001", stockInfo.getProductId());
        assertEquals("Test Product 1", stockInfo.getProductTitle());
        assertEquals(20, stockInfo.getActualStock());
        assertEquals(5, stockInfo.getReservedStock()); // 20 - 15
        assertEquals(15, stockInfo.getAvailableStock());
        assertTrue(stockInfo.isInStock());
        assertFalse(stockInfo.isLowStock()); // 20 > 10 (default threshold)
        assertEquals(10, stockInfo.getLowStockThreshold());
    }
    
    @Test
    void getStockInfo_LowStockProduct_ReturnsLowStockInfo() throws SQLException, ResourceNotFoundException {
        // Arrange
        testProduct1.setQuantityInStock(8); // Below default threshold of 10
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(8);
        
        // Act
        StockInfo stockInfo = stockValidationService.getStockInfo("PROD001");
        
        // Assert
        assertTrue(stockInfo.isLowStock());
        assertTrue(stockInfo.isInStock());
        assertEquals(8, stockInfo.getActualStock());
    }
    
    @Test
    void getStockInfo_OutOfStockProduct_ReturnsOutOfStockInfo() throws SQLException, ResourceNotFoundException {
        // Arrange
        testProduct1.setQuantityInStock(5);
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(0); // All reserved
        
        // Act
        StockInfo stockInfo = stockValidationService.getStockInfo("PROD001");
        
        // Assert
        assertFalse(stockInfo.isInStock());
        assertTrue(stockInfo.isLowStock());
        assertEquals(0, stockInfo.getAvailableStock());
        assertEquals(5, stockInfo.getReservedStock());
    }
    
    @Test
    void getBulkStockInfo_ValidProductIds_ReturnsStockInfoMap() throws SQLException, ResourceNotFoundException {
        // Arrange
        List<String> productIds = Arrays.asList("PROD001", "PROD002");
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(15);
        when(stockReservationService.getAvailableStock("PROD002")).thenReturn(3);
        
        // Act
        Map<String, StockInfo> stockInfoMap = stockValidationService.getBulkStockInfo(productIds);
        
        // Assert
        assertEquals(2, stockInfoMap.size());
        assertTrue(stockInfoMap.containsKey("PROD001"));
        assertTrue(stockInfoMap.containsKey("PROD002"));
        
        StockInfo stockInfo1 = stockInfoMap.get("PROD001");
        assertEquals("Test Product 1", stockInfo1.getProductTitle());
        assertEquals(15, stockInfo1.getAvailableStock());
        
        StockInfo stockInfo2 = stockInfoMap.get("PROD002");
        assertEquals("Test Product 2", stockInfo2.getProductTitle());
        assertEquals(3, stockInfo2.getAvailableStock());
    }
    
    @Test
    void getBulkStockInfo_ProductNotFound_ReturnsEmptyStockInfo() throws SQLException, ResourceNotFoundException {
        // Arrange
        List<String> productIds = Arrays.asList("PROD001", "INVALID_PROD");
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("INVALID_PROD")).thenReturn(null);
        when(stockReservationService.getAvailableStock("PROD001")).thenReturn(15);
        
        // Act
        Map<String, StockInfo> stockInfoMap = stockValidationService.getBulkStockInfo(productIds);
        
        // Assert
        assertEquals(2, stockInfoMap.size());
        
        StockInfo validStockInfo = stockInfoMap.get("PROD001");
        assertEquals("Test Product 1", validStockInfo.getProductTitle());
        
        StockInfo invalidStockInfo = stockInfoMap.get("INVALID_PROD");
        assertEquals("Product Not Found", invalidStockInfo.getProductTitle());
        assertEquals(0, invalidStockInfo.getActualStock());
        assertFalse(invalidStockInfo.isInStock());
    }
    
    @Test
    void generateInsufficientStockNotification_AllValid_ReturnsSuccessNotification() {
        // Arrange
        List<StockValidationResult> validResults = Arrays.asList(
            new StockValidationResult(true, "PROD001", "Product 1", 3, 20, 5, 15, "Valid", "STOCK_AVAILABLE")
        );
        BulkStockValidationResult validationResult = new BulkStockValidationResult(
            true, validResults, new ArrayList<>(), 1, 0, "All valid"
        );
        
        // Act
        InsufficientStockNotification notification = stockValidationService.generateInsufficientStockNotification(validationResult);
        
        // Assert
        assertEquals("Stock Validation Passed", notification.getTitle());
        assertTrue(notification.getMessage().contains("All items are available"));
        assertTrue(notification.getProductMessages().isEmpty());
        assertTrue(notification.getSuggestedActions().isEmpty());
        assertTrue(notification.canProceedWithAvailableStock());
    }
    
    @Test
    void generateInsufficientStockNotification_SomeInvalid_ReturnsFailureNotification() {
        // Arrange
        List<StockValidationResult> failedResults = Arrays.asList(
            new StockValidationResult(false, "PROD001", "Product 1", 10, 20, 5, 5, "Insufficient", "INSUFFICIENT_STOCK"),
            new StockValidationResult(false, "PROD002", "Product 2", 3, 5, 5, 0, "Out of stock", "INSUFFICIENT_STOCK")
        );
        BulkStockValidationResult validationResult = new BulkStockValidationResult(
            false, failedResults, failedResults, 2, 2, "Some failed"
        );
        
        // Act
        InsufficientStockNotification notification = stockValidationService.generateInsufficientStockNotification(validationResult);
        
        // Assert
        assertEquals("Insufficient Stock", notification.getTitle());
        assertTrue(notification.getMessage().contains("2 product(s) in your cart"));
        assertEquals(2, notification.getProductMessages().size());
        assertTrue(notification.getProductMessages().get(0).contains("Product 1: Requested 10, but only 5 available"));
        assertTrue(notification.getProductMessages().get(1).contains("Product 2: Requested 3, but only 0 available"));
        assertEquals(2, notification.getSuggestedActions().size());
        assertTrue(notification.getSuggestedActions().get("PROD001").contains("Reduce quantity to 5"));
        assertTrue(notification.getSuggestedActions().get("PROD002").contains("Remove from cart - out of stock"));
        assertFalse(notification.canProceedWithAvailableStock()); // One product is completely out of stock
    }
    
    @Test
    void isStockCriticallyLow_BelowThreshold_ReturnsTrue() throws SQLException, ResourceNotFoundException {
        // Arrange
        testProduct1.setQuantityInStock(5);
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        
        // Act
        boolean isCriticallyLow = stockValidationService.isStockCriticallyLow("PROD001", 10);
        
        // Assert
        assertTrue(isCriticallyLow);
    }
    
    @Test
    void isStockCriticallyLow_AboveThreshold_ReturnsFalse() throws SQLException, ResourceNotFoundException {
        // Arrange
        testProduct1.setQuantityInStock(15);
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        
        // Act
        boolean isCriticallyLow = stockValidationService.isStockCriticallyLow("PROD001", 10);
        
        // Assert
        assertFalse(isCriticallyLow);
    }
    
    @Test
    void isStockCriticallyLow_ExactlyAtThreshold_ReturnsTrue() throws SQLException, ResourceNotFoundException {
        // Arrange
        testProduct1.setQuantityInStock(10);
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        
        // Act
        boolean isCriticallyLow = stockValidationService.isStockCriticallyLow("PROD001", 10);
        
        // Assert
        assertTrue(isCriticallyLow);
    }
    
    @Test
    void isStockCriticallyLow_NegativeThreshold_UsesDefaultThreshold() throws SQLException, ResourceNotFoundException {
        // Arrange
        testProduct1.setQuantityInStock(5);
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        
        // Act
        boolean isCriticallyLow = stockValidationService.isStockCriticallyLow("PROD001", -5);
        
        // Assert
        assertTrue(isCriticallyLow); // 5 <= 10 (default threshold)
    }
}