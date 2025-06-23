package com.aims.core.application.impl;

import com.aims.core.application.services.IStockReservationService;
import com.aims.core.entities.Product;
import com.aims.core.infrastructure.database.dao.IProductDAO;
import com.aims.core.shared.exceptions.InventoryException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for StockReservationServiceImpl
 * Tests stock reservation functionality, expiration handling, and cleanup mechanisms
 */
@ExtendWith(MockitoExtension.class)
class StockReservationServiceImplTest {

    @Mock
    private IProductDAO productDAO;
    
    private IStockReservationService stockReservationService;
    
    private Product testProduct1;
    private Product testProduct2;
    private Product testProductLowStock;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stockReservationService = new StockReservationServiceImpl(productDAO);
        
        setupTestData();
    }
    
    private void setupTestData() {
        // Test Product 1 - Normal stock
        testProduct1 = new Product();
        testProduct1.setProductId("PROD001");
        testProduct1.setTitle("Test Product 1");
        testProduct1.setQuantityInStock(20);
        testProduct1.setPrice(50000f);
        
        // Test Product 2 - Different stock level
        testProduct2 = new Product();
        testProduct2.setProductId("PROD002");
        testProduct2.setTitle("Test Product 2");
        testProduct2.setQuantityInStock(10);
        testProduct2.setPrice(75000f);
        
        // Test Product - Low stock
        testProductLowStock = new Product();
        testProductLowStock.setProductId("PROD003");
        testProductLowStock.setTitle("Low Stock Product");
        testProductLowStock.setQuantityInStock(3);
        testProductLowStock.setPrice(100000f);
    }
    
    // ========== reserveStock() Tests ==========
    
    @Test
    void reserveStock_ValidRequest_ReturnsTrue() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        
        // Act
        boolean result = stockReservationService.reserveStock("PROD001", 5, "RES001", 15);
        
        // Assert
        assertTrue(result);
        verify(productDAO).getById("PROD001");
    }
    
    @Test
    void reserveStock_InsufficientStock_ReturnsFalse() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD003")).thenReturn(testProductLowStock);
        
        // Act
        boolean result = stockReservationService.reserveStock("PROD003", 5, "RES001", 15);
        
        // Assert
        assertFalse(result);
        verify(productDAO).getById("PROD003");
    }
    
    @Test
    void reserveStock_ExactAvailableQuantity_ReturnsTrue() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD003")).thenReturn(testProductLowStock);
        
        // Act
        boolean result = stockReservationService.reserveStock("PROD003", 3, "RES001", 15);
        
        // Assert
        assertTrue(result);
        verify(productDAO).getById("PROD003");
    }
    
    @Test
    void reserveStock_MultipleReservationsSameProduct_ReducesAvailableStock() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        
        // Act & Assert
        assertTrue(stockReservationService.reserveStock("PROD001", 8, "RES001", 15));
        assertTrue(stockReservationService.reserveStock("PROD001", 7, "RES002", 15));
        assertFalse(stockReservationService.reserveStock("PROD001", 6, "RES003", 15)); // 20 - 8 - 7 = 5 < 6
        
        verify(productDAO, times(3)).getById("PROD001");
    }
    
    @Test
    void reserveStock_DefaultTimeout_UsesDefaultValue() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        
        // Act
        boolean result = stockReservationService.reserveStock("PROD001", 5, "RES001", 0);
        
        // Assert
        assertTrue(result);
        verify(productDAO).getById("PROD001");
    }
    
    @Test
    void reserveStock_NullProductId_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
            stockReservationService.reserveStock(null, 5, "RES001", 15));
        
        verifyNoInteractions(productDAO);
    }
    
    @Test
    void reserveStock_EmptyProductId_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
            stockReservationService.reserveStock("", 5, "RES001", 15));
        
        verifyNoInteractions(productDAO);
    }
    
    @Test
    void reserveStock_ZeroQuantity_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
            stockReservationService.reserveStock("PROD001", 0, "RES001", 15));
        
        verifyNoInteractions(productDAO);
    }
    
    @Test
    void reserveStock_NegativeQuantity_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
            stockReservationService.reserveStock("PROD001", -5, "RES001", 15));
        
        verifyNoInteractions(productDAO);
    }
    
    @Test
    void reserveStock_NullReservationId_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
            stockReservationService.reserveStock("PROD001", 5, null, 15));
        
        verifyNoInteractions(productDAO);
    }
    
    @Test
    void reserveStock_EmptyReservationId_ThrowsValidationException() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> 
            stockReservationService.reserveStock("PROD001", 5, "", 15));
        
        verifyNoInteractions(productDAO);
    }
    
    @Test
    void reserveStock_ProductNotFound_ThrowsResourceNotFoundException() throws SQLException {
        // Arrange
        when(productDAO.getById("INVALID_PROD")).thenReturn(null);
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            stockReservationService.reserveStock("INVALID_PROD", 5, "RES001", 15));
        
        verify(productDAO).getById("INVALID_PROD");
    }
    
    // ========== confirmReservation() Tests ==========
    
    @Test
    void confirmReservation_ValidReservation_UpdatesStock() throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        stockReservationService.reserveStock("PROD001", 5, "RES001", 15);
        
        // Act
        stockReservationService.confirmReservation("RES001");
        
        // Assert
        verify(productDAO).updateStock("PROD001", 15); // 20 - 5 = 15
    }
    
    @Test
    void confirmReservation_NonExistentReservation_ThrowsInventoryException() {
        // Act & Assert
        assertThrows(InventoryException.class, () -> 
            stockReservationService.confirmReservation("NONEXISTENT"));
        
        verifyNoInteractions(productDAO);
    }
    
    @Test
    void confirmReservation_NullReservationId_ThrowsInventoryException() {
        // Act & Assert
        assertThrows(InventoryException.class, () -> 
            stockReservationService.confirmReservation(null));
        
        verifyNoInteractions(productDAO);
    }
    
    @Test
    void confirmReservation_EmptyReservationId_ThrowsInventoryException() {
        // Act & Assert
        assertThrows(InventoryException.class, () -> 
            stockReservationService.confirmReservation(""));
        
        verifyNoInteractions(productDAO);
    }
    
    @Test
    void confirmReservation_ExpiredReservation_ThrowsInventoryException() throws SQLException, ResourceNotFoundException, ValidationException, InterruptedException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        stockReservationService.reserveStock("PROD001", 5, "RES001", 1); // 1 minute timeout
        
        // Wait for expiration (simulate with very short timeout)
        Thread.sleep(70000); // Wait just over 1 minute
        
        // Act & Assert
        assertThrows(InventoryException.class, () -> 
            stockReservationService.confirmReservation("RES001"));
    }
    
    @Test
    void confirmReservation_ProductNoLongerExists_ThrowsInventoryException() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        stockReservationService.reserveStock("PROD001", 5, "RES001", 15);
        
        // Product gets deleted after reservation
        when(productDAO.getById("PROD001")).thenReturn(null);
        
        // Act & Assert
        assertThrows(InventoryException.class, () -> 
            stockReservationService.confirmReservation("RES001"));
    }
    
    @Test
    void confirmReservation_InsufficientActualStock_ThrowsInventoryException() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        stockReservationService.reserveStock("PROD001", 5, "RES001", 15);
        
        // Simulate stock being reduced externally
        testProduct1.setQuantityInStock(3); // Less than reserved quantity
        
        // Act & Assert
        assertThrows(InventoryException.class, () -> 
            stockReservationService.confirmReservation("RES001"));
    }
    
    // ========== releaseReservation() Tests ==========
    
    @Test
    void releaseReservation_ValidReservation_RemovesReservation() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        stockReservationService.reserveStock("PROD001", 5, "RES001", 15);
        
        // Verify stock is reserved
        assertEquals(15, stockReservationService.getAvailableStock("PROD001"));
        
        // Act
        stockReservationService.releaseReservation("RES001");
        
        // Assert - Stock should be available again
        assertEquals(20, stockReservationService.getAvailableStock("PROD001"));
    }
    
    @Test
    void releaseReservation_NonExistentReservation_DoesNothing() throws SQLException {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> stockReservationService.releaseReservation("NONEXISTENT"));
        
        verifyNoInteractions(productDAO);
    }
    
    @Test
    void releaseReservation_NullReservationId_DoesNothing() throws SQLException {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> stockReservationService.releaseReservation(null));
        
        verifyNoInteractions(productDAO);
    }
    
    @Test
    void releaseReservation_EmptyReservationId_DoesNothing() throws SQLException {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> stockReservationService.releaseReservation(""));
        
        verifyNoInteractions(productDAO);
    }
    
    // ========== isStockAvailable() Tests ==========
    
    @Test
    void isStockAvailable_SufficientStock_ReturnsTrue() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        
        // Act
        boolean result = stockReservationService.isStockAvailable("PROD001", 15);
        
        // Assert
        assertTrue(result);
        verify(productDAO).getById("PROD001");
    }
    
    @Test
    void isStockAvailable_InsufficientStock_ReturnsFalse() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        
        // Act
        boolean result = stockReservationService.isStockAvailable("PROD001", 25);
        
        // Assert
        assertFalse(result);
        verify(productDAO).getById("PROD001");
    }
    
    @Test
    void isStockAvailable_ExactStock_ReturnsTrue() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        
        // Act
        boolean result = stockReservationService.isStockAvailable("PROD001", 20);
        
        // Assert
        assertTrue(result);
        verify(productDAO).getById("PROD001");
    }
    
    @Test
    void isStockAvailable_WithReservations_ConsidersReservedStock() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        stockReservationService.reserveStock("PROD001", 10, "RES001", 15);
        
        // Act
        boolean sufficientForFive = stockReservationService.isStockAvailable("PROD001", 5);
        boolean sufficientForFifteen = stockReservationService.isStockAvailable("PROD001", 15);
        
        // Assert
        assertTrue(sufficientForFive); // 20 - 10 = 10 >= 5
        assertFalse(sufficientForFifteen); // 20 - 10 = 10 < 15
    }
    
    @Test
    void isStockAvailable_ProductNotFound_ThrowsResourceNotFoundException() throws SQLException {
        // Arrange
        when(productDAO.getById("INVALID_PROD")).thenReturn(null);
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            stockReservationService.isStockAvailable("INVALID_PROD", 5));
        
        verify(productDAO).getById("INVALID_PROD");
    }
    
    // ========== getAvailableStock() Tests ==========
    
    @Test
    void getAvailableStock_NoReservations_ReturnsActualStock() throws SQLException, ResourceNotFoundException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        
        // Act
        int availableStock = stockReservationService.getAvailableStock("PROD001");
        
        // Assert
        assertEquals(20, availableStock);
        verify(productDAO).getById("PROD001");
    }
    
    @Test
    void getAvailableStock_WithReservations_ReturnsReducedStock() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        stockReservationService.reserveStock("PROD001", 7, "RES001", 15);
        
        // Act
        int availableStock = stockReservationService.getAvailableStock("PROD001");
        
        // Assert
        assertEquals(13, availableStock); // 20 - 7 = 13
    }
    
    @Test
    void getAvailableStock_MultipleReservations_ReturnsCorrectStock() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        stockReservationService.reserveStock("PROD001", 5, "RES001", 15);
        stockReservationService.reserveStock("PROD001", 3, "RES002", 15);
        
        // Act
        int availableStock = stockReservationService.getAvailableStock("PROD001");
        
        // Assert
        assertEquals(12, availableStock); // 20 - 5 - 3 = 12
    }
    
    @Test
    void getAvailableStock_ProductNotFound_ThrowsResourceNotFoundException() throws SQLException {
        // Arrange
        when(productDAO.getById("INVALID_PROD")).thenReturn(null);
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            stockReservationService.getAvailableStock("INVALID_PROD"));
        
        verify(productDAO).getById("INVALID_PROD");
    }
    
    // ========== cleanupExpiredReservations() Tests ==========
    
    @Test
    void cleanupExpiredReservations_NoReservations_ReturnsZero() throws SQLException {
        // Act
        int cleanedUp = stockReservationService.cleanupExpiredReservations();
        
        // Assert
        assertEquals(0, cleanedUp);
    }
    
    @Test
    void cleanupExpiredReservations_NoExpiredReservations_ReturnsZero() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        stockReservationService.reserveStock("PROD001", 5, "RES001", 60); // 60 minutes timeout
        
        // Act
        int cleanedUp = stockReservationService.cleanupExpiredReservations();
        
        // Assert
        assertEquals(0, cleanedUp);
    }
    
    @Test
    void cleanupExpiredReservations_HasExpiredReservations_ReturnsCount() throws SQLException, ResourceNotFoundException, ValidationException, InterruptedException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);
        
        // Create reservations with very short timeout
        stockReservationService.reserveStock("PROD001", 5, "RES001", 1); // 1 minute
        stockReservationService.reserveStock("PROD002", 3, "RES002", 1); // 1 minute
        
        // Wait for expiration
        Thread.sleep(70000); // Wait over 1 minute
        
        // Act
        int cleanedUp = stockReservationService.cleanupExpiredReservations();
        
        // Assert
        assertEquals(2, cleanedUp);
    }
    
    // ========== getActiveReservations() Tests ==========
    
    @Test
    void getActiveReservations_NoReservations_ReturnsEmptyMap() throws SQLException {
        // Act
        Map<String, String> activeReservations = stockReservationService.getActiveReservations();
        
        // Assert
        assertTrue(activeReservations.isEmpty());
    }
    
    @Test
    void getActiveReservations_HasReservations_ReturnsReservationDetails() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        when(productDAO.getById("PROD002")).thenReturn(testProduct2);
        stockReservationService.reserveStock("PROD001", 5, "RES001", 15);
        stockReservationService.reserveStock("PROD002", 3, "RES002", 30);
        
        // Act
        Map<String, String> activeReservations = stockReservationService.getActiveReservations();
        
        // Assert
        assertEquals(2, activeReservations.size());
        assertTrue(activeReservations.containsKey("RES001"));
        assertTrue(activeReservations.containsKey("RES002"));
        
        String res1Details = activeReservations.get("RES001");
        assertTrue(res1Details.contains("PROD001"));
        assertTrue(res1Details.contains("5"));
        
        String res2Details = activeReservations.get("RES002");
        assertTrue(res2Details.contains("PROD002"));
        assertTrue(res2Details.contains("3"));
    }
    
    @Test
    void getActiveReservations_AfterConfirmation_RemovesReservation() throws SQLException, ResourceNotFoundException, ValidationException, InventoryException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        stockReservationService.reserveStock("PROD001", 5, "RES001", 15);
        
        // Verify reservation exists
        assertEquals(1, stockReservationService.getActiveReservations().size());
        
        // Act
        stockReservationService.confirmReservation("RES001");
        
        // Assert
        assertTrue(stockReservationService.getActiveReservations().isEmpty());
    }
    
    @Test
    void getActiveReservations_AfterRelease_RemovesReservation() throws SQLException, ResourceNotFoundException, ValidationException {
        // Arrange
        when(productDAO.getById("PROD001")).thenReturn(testProduct1);
        stockReservationService.reserveStock("PROD001", 5, "RES001", 15);
        
        // Verify reservation exists
        assertEquals(1, stockReservationService.getActiveReservations().size());
        
        // Act
        stockReservationService.releaseReservation("RES001");
        
        // Assert
        assertTrue(stockReservationService.getActiveReservations().isEmpty());
    }
}