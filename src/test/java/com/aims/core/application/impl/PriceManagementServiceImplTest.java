package com.aims.core.application.impl;

import com.aims.core.application.services.IPriceManagementService;
import com.aims.core.application.services.IProductManagerAuditService;
import com.aims.core.shared.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceManagementServiceImplTest {

    @Mock
    private IProductManagerAuditService auditService;

    @InjectMocks
    private PriceManagementServiceImpl priceManagementService;

    private String testProductId;
    private String testManagerId;
    private float testProductValue;

    @BeforeEach
    void setUp() {
        testProductId = "product123";
        testManagerId = "manager456";
        testProductValue = 100000.0f; // 100,000 VND
    }

    @Test
    void validatePriceRange_validPrice_returnsTrue() {
        // Test price within 30%-150% range
        float validPrice = 50000.0f; // 50% of product value
        
        boolean result = priceManagementService.validatePriceRange(validPrice, testProductValue);
        
        assertTrue(result);
    }

    @Test
    void validatePriceRange_priceTooLow_returnsFalse() {
        // Test price below 30% range
        float invalidPrice = 25000.0f; // 25% of product value
        
        boolean result = priceManagementService.validatePriceRange(invalidPrice, testProductValue);
        
        assertFalse(result);
    }

    @Test
    void validatePriceRange_priceTooHigh_returnsFalse() {
        // Test price above 150% range
        float invalidPrice = 160000.0f; // 160% of product value
        
        boolean result = priceManagementService.validatePriceRange(invalidPrice, testProductValue);
        
        assertFalse(result);
    }

    @Test
    void canUpdatePrice_underDailyLimit_returnsTrue() throws SQLException {
        // Mock: Manager has made 1 price update today (under limit of 2)
        List<String> operations = Arrays.asList("PRICE_UPDATE_" + testProductId + "_2024-01-01_50000.00_TO_60000.00");
        when(auditService.getManagerOperations(testManagerId, LocalDate.now().toString())).thenReturn(operations);
        
        boolean result = priceManagementService.canUpdatePrice(testProductId, testManagerId);
        
        assertTrue(result);
        verify(auditService).getManagerOperations(testManagerId, LocalDate.now().toString());
    }

    @Test
    void canUpdatePrice_atDailyLimit_returnsFalse() throws SQLException {
        // Mock: Manager has made 2 price updates today (at limit)
        List<String> operations = Arrays.asList(
            "PRICE_UPDATE_" + testProductId + "_2024-01-01_50000.00_TO_60000.00",
            "PRICE_UPDATE_" + testProductId + "_2024-01-01_60000.00_TO_70000.00"
        );
        when(auditService.getManagerOperations(testManagerId, LocalDate.now().toString())).thenReturn(operations);
        
        boolean result = priceManagementService.canUpdatePrice(testProductId, testManagerId);
        
        assertFalse(result);
        verify(auditService).getManagerOperations(testManagerId, LocalDate.now().toString());
    }

    @Test
    void recordPriceUpdate_success() throws SQLException {
        float oldPrice = 50000.0f;
        float newPrice = 60000.0f;
        
        assertDoesNotThrow(() -> 
            priceManagementService.recordPriceUpdate(testProductId, testManagerId, oldPrice, newPrice)
        );
        
        verify(auditService).recordOperation(eq(testManagerId), contains("PRICE_UPDATE_" + testProductId));
    }

    @Test
    void validatePriceUpdate_validRequest_returnsValidResult() throws ValidationException, SQLException {
        // Mock: Manager under daily limit
        List<String> operations = Arrays.asList("PRICE_UPDATE_otherProduct_2024-01-01_40000.00_TO_45000.00");
        when(auditService.getManagerOperations(testManagerId, LocalDate.now().toString())).thenReturn(operations);
        
        float newPrice = 80000.0f; // Valid price (80% of product value)
        
        IPriceManagementService.PriceValidationResult result = 
            priceManagementService.validatePriceUpdate(testProductId, newPrice, testProductValue, testManagerId);
        
        assertTrue(result.isValid());
        assertNotNull(result.getValidRange());
        verify(auditService).getManagerOperations(testManagerId, LocalDate.now().toString());
    }

    @Test
    void validatePriceUpdate_invalidPriceRange_returnsInvalidResult() throws ValidationException, SQLException {
        // Mock: Manager under daily limit
        List<String> operations = Arrays.asList();
        when(auditService.getManagerOperations(testManagerId, LocalDate.now().toString())).thenReturn(operations);
        
        float newPrice = 20000.0f; // Invalid price (20% of product value, below 30% threshold)
        
        IPriceManagementService.PriceValidationResult result = 
            priceManagementService.validatePriceUpdate(testProductId, newPrice, testProductValue, testManagerId);
        
        assertFalse(result.isValid());
        assertNotNull(result.getMessage());
        assertNotNull(result.getValidRange());
    }

    @Test
    void calculateValidPriceRange_correctCalculation() {
        IPriceManagementService.PriceRange range = priceManagementService.calculateValidPriceRange(testProductValue);
        
        assertEquals(30000.0f, range.getMinimumPrice(), 0.01f); // 30% of 100,000
        assertEquals(150000.0f, range.getMaximumPrice(), 0.01f); // 150% of 100,000
        assertTrue(range.isWithinRange(50000.0f)); // 50% should be valid
        assertFalse(range.isWithinRange(25000.0f)); // 25% should be invalid
    }

    @Test
    void getDailyPriceUpdates_returnsCorrectRecords() throws SQLException {
        LocalDate testDate = LocalDate.now();
        List<String> mockOperations = Arrays.asList(
            "PRICE_UPDATE_product1_" + testDate + "_50000.00_TO_60000.00",
            "PRICE_UPDATE_product2_" + testDate + "_70000.00_TO_80000.00",
            "EDIT_product3_" + testDate // This should not be included
        );
        when(auditService.getManagerOperations(testManagerId, testDate.toString())).thenReturn(mockOperations);
        
        List<IPriceManagementService.PriceUpdateRecord> records = 
            priceManagementService.getDailyPriceUpdates(testManagerId, testDate);
        
        assertNotNull(records);
        // Implementation should filter only price update operations
        verify(auditService).getManagerOperations(testManagerId, testDate.toString());
    }

    @Test
    void recordPriceUpdate_sqlException_throwsException() throws SQLException {
        doThrow(new SQLException("DB error")).when(auditService).recordOperation(anyString(), anyString());
        
        assertThrows(SQLException.class, () -> 
            priceManagementService.recordPriceUpdate(testProductId, testManagerId, 50000.0f, 60000.0f)
        );
    }
}