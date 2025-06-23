package com.aims.core.application.impl;

import com.aims.core.application.services.IProductManagerAuditService;
import com.aims.core.application.services.IStockValidationService;
import com.aims.core.entities.Product;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.shared.exceptions.InventoryException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Phase 2 Integration Test for ProductServiceImpl with StockValidationService.
 * Tests the enhanced inventory management capabilities and stock validation integration.
 */
@DisplayName("ProductService Phase 2 - StockValidationService Integration Tests")
public class ProductServiceImplPhase2IntegrationTest {

    @Mock
    private IProductDAO productDAO;
    
    @Mock
    private IProductManagerAuditService auditService;
    
    @Mock
    private IStockValidationService stockValidationService;
    
    private ProductServiceImpl productService;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productService = new ProductServiceImpl(productDAO, auditService, stockValidationService);
        
        // Create test product
        testProduct = new Product("PROD001", "Test Product", "Electronics", 100.0f, 150.0f, 
                                 20, "Test description", "test.jpg", "12345", 
                                 "10x10x10", 1.0f, null, null);
        testProduct.setVersion(1L);
    }

    @Test
    @DisplayName("Should validate stock before reducing inventory")
    void updateProductStock_reduceStock_shouldValidateWithStockService() throws Exception {
        // Arrange
        int stockReduction = -5;
        when(productDAO.getById("PROD001")).thenReturn(testProduct);
        
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                true, "PROD001", "Test Product", 5, 20, 0, 20, 
                "Stock validation passed", "STOCK_AVAILABLE"
            );
        when(stockValidationService.validateProductStock("PROD001", 5)).thenReturn(validationResult);
        when(stockValidationService.isStockCriticallyLow("PROD001", 10)).thenReturn(false);
        
        // Act
        Product result = productService.updateProductStock("PROD001", stockReduction);
        
        // Assert
        verify(stockValidationService).validateProductStock("PROD001", 5);
        verify(stockValidationService).isStockCriticallyLow("PROD001", 10);
        verify(productDAO).updateStockWithVersion("PROD001", 15, 1L);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should reject stock reduction when validation fails")
    void updateProductStock_insufficientStock_shouldThrowInventoryException() throws Exception {
        // Arrange
        int stockReduction = -25; // More than available
        when(productDAO.getById("PROD001")).thenReturn(testProduct);
        
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                false, "PROD001", "Test Product", 25, 20, 5, 15, 
                "Insufficient stock", "INSUFFICIENT_STOCK"
            );
        when(stockValidationService.validateProductStock("PROD001", 25)).thenReturn(validationResult);
        
        // Act & Assert
        InventoryException exception = assertThrows(InventoryException.class, () -> {
            productService.updateProductStock("PROD001", stockReduction);
        });
        
        assertTrue(exception.getMessage().contains("Stock validation failed"));
        verify(stockValidationService).validateProductStock("PROD001", 25);
        verify(productDAO, never()).updateStockWithVersion(anyString(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("Should not validate for stock increases")
    void updateProductStock_increaseStock_shouldSkipValidation() throws Exception {
        // Arrange
        int stockIncrease = 10;
        when(productDAO.getById("PROD001")).thenReturn(testProduct);
        when(stockValidationService.isStockCriticallyLow("PROD001", 10)).thenReturn(false);
        
        // Act
        Product result = productService.updateProductStock("PROD001", stockIncrease);
        
        // Assert
        verify(stockValidationService, never()).validateProductStock(anyString(), anyInt());
        verify(stockValidationService).isStockCriticallyLow("PROD001", 10);
        verify(productDAO).updateStockWithVersion("PROD001", 30, 1L);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should warn about critically low stock")
    void updateProductStock_resultingInLowStock_shouldLogWarning() throws Exception {
        // Arrange
        testProduct.setQuantityInStock(12); // Will result in 2 after reduction
        int stockReduction = -10;
        when(productDAO.getById("PROD001")).thenReturn(testProduct);
        
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                true, "PROD001", "Test Product", 10, 12, 0, 12, 
                "Stock validation passed", "STOCK_AVAILABLE"
            );
        when(stockValidationService.validateProductStock("PROD001", 10)).thenReturn(validationResult);
        when(stockValidationService.isStockCriticallyLow("PROD001", 10)).thenReturn(true);
        
        // Act
        Product result = productService.updateProductStock("PROD001", stockReduction);
        
        // Assert
        verify(stockValidationService).isStockCriticallyLow("PROD001", 10);
        verify(productDAO).updateStockWithVersion("PROD001", 2, 1L);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should get comprehensive stock information")
    void getProductStockInfo_shouldReturnDetailedStockInfo() throws Exception {
        // Arrange
        IStockValidationService.StockInfo expectedStockInfo = 
            new IStockValidationService.StockInfo(
                "PROD001", "Test Product", 20, 5, 15, true, false, 10
            );
        when(stockValidationService.getStockInfo("PROD001")).thenReturn(expectedStockInfo);
        
        // Act
        IStockValidationService.StockInfo result = productService.getProductStockInfo("PROD001");
        
        // Assert
        assertNotNull(result);
        assertEquals("PROD001", result.getProductId());
        assertEquals(20, result.getActualStock());
        assertEquals(15, result.getAvailableStock());
        verify(stockValidationService).getStockInfo("PROD001");
    }

    @Test
    @DisplayName("Should validate stock availability correctly")
    void hasRawStock_sufficientStock_shouldReturnTrue() throws Exception {
        // Arrange
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                true, "PROD001", "Test Product", 10, 20, 0, 20, 
                "Stock available", "STOCK_AVAILABLE"
            );
        when(stockValidationService.validateProductStock("PROD001", 10)).thenReturn(validationResult);
        
        // Act
        boolean result = productService.hasRawStock("PROD001", 10);
        
        // Assert
        assertTrue(result);
        verify(stockValidationService).validateProductStock("PROD001", 10);
    }

    @Test
    @DisplayName("Should validate stock availability correctly for insufficient stock")
    void hasRawStock_insufficientStock_shouldReturnFalse() throws Exception {
        // Arrange
        IStockValidationService.StockValidationResult validationResult = 
            new IStockValidationService.StockValidationResult(
                false, "PROD001", "Test Product", 25, 20, 5, 15, 
                "Insufficient stock", "INSUFFICIENT_STOCK"
            );
        when(stockValidationService.validateProductStock("PROD001", 25)).thenReturn(validationResult);
        
        // Act
        boolean result = productService.hasRawStock("PROD001", 25);
        
        // Assert
        assertFalse(result);
        verify(stockValidationService).validateProductStock("PROD001", 25);
    }

    @Test
    @DisplayName("Should get available stock considering reservations")
    void getAvailableStock_shouldReturnAvailableQuantity() throws Exception {
        // Arrange
        IStockValidationService.StockInfo stockInfo = 
            new IStockValidationService.StockInfo(
                "PROD001", "Test Product", 20, 3, 17, true, false, 10
            );
        when(stockValidationService.getStockInfo("PROD001")).thenReturn(stockInfo);
        
        // Act
        int availableStock = productService.getAvailableStock("PROD001");
        
        // Assert
        assertEquals(17, availableStock);
        verify(stockValidationService).getStockInfo("PROD001");
    }

    @Test
    @DisplayName("Should check critical stock levels")
    void isStockCriticallyLow_withCustomThreshold_shouldUseDelegateService() throws Exception {
        // Arrange
        when(stockValidationService.isStockCriticallyLow("PROD001", 5)).thenReturn(true);
        
        // Act
        boolean result = productService.isStockCriticallyLow("PROD001", 5);
        
        // Assert
        assertTrue(result);
        verify(stockValidationService).isStockCriticallyLow("PROD001", 5);
    }

    @Test
    @DisplayName("Should validate bulk stock operations")
    void validateBulkProductStock_shouldProcessMultipleProducts() throws Exception {
        // Arrange
        Map<String, Integer> productStockMap = new HashMap<>();
        productStockMap.put("PROD001", 5);
        productStockMap.put("PROD002", 3);
        
        Product product2 = new Product("PROD002", "Test Product 2", "Electronics", 80.0f, 120.0f, 
                                      15, "Test description 2", "test2.jpg", "67890", 
                                      "8x8x8", 0.8f, null, null);
        
        when(productDAO.getById("PROD001")).thenReturn(testProduct);
        when(productDAO.getById("PROD002")).thenReturn(product2);
        
        IStockValidationService.BulkStockValidationResult expectedResult = 
            mock(IStockValidationService.BulkStockValidationResult.class);
        when(stockValidationService.validateBulkStock(any())).thenReturn(expectedResult);
        
        // Act
        IStockValidationService.BulkStockValidationResult result = 
            productService.validateBulkProductStock(productStockMap);
        
        // Assert
        assertNotNull(result);
        verify(productDAO).getById("PROD001");
        verify(productDAO).getById("PROD002");
        verify(stockValidationService).validateBulkStock(any());
    }

    @Test
    @DisplayName("Should handle empty bulk validation gracefully")
    void validateBulkProductStock_emptyMap_shouldReturnEmptyResult() throws Exception {
        // Arrange
        Map<String, Integer> emptyMap = new HashMap<>();
        
        // Act
        IStockValidationService.BulkStockValidationResult result = 
            productService.validateBulkProductStock(emptyMap);
        
        // Assert
        assertTrue(result.isAllValid());
        assertEquals(0, result.getTotalProductsChecked());
        assertEquals("No products to validate", result.getOverallMessage());
    }

    @Test
    @DisplayName("Should handle validation service errors gracefully during stock updates")
    void updateProductStock_validationServiceError_shouldContinueWithWarning() throws Exception {
        // Arrange
        int stockReduction = -5;
        when(productDAO.getById("PROD001")).thenReturn(testProduct);
        when(stockValidationService.validateProductStock("PROD001", 5))
            .thenThrow(new ResourceNotFoundException("Service error"));
        
        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            productService.updateProductStock("PROD001", stockReduction);
        });
        
        assertEquals("Service error", exception.getMessage());
        verify(productDAO, never()).updateStockWithVersion(anyString(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("Should handle critical stock check errors gracefully")
    void updateProductStock_criticalStockCheckError_shouldContinueOperation() throws Exception {
        // Arrange
        int stockIncrease = 5;
        when(productDAO.getById("PROD001")).thenReturn(testProduct);
        when(stockValidationService.isStockCriticallyLow("PROD001", 10))
            .thenThrow(new ResourceNotFoundException("Product not found for critical check"));
        
        // Act
        Product result = productService.updateProductStock("PROD001", stockIncrease);
        
        // Assert - operation should continue despite critical stock check failure
        assertNotNull(result);
        verify(productDAO).updateStockWithVersion("PROD001", 25, 1L);
    }
}