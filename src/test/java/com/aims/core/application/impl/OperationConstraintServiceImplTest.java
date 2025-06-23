package com.aims.core.application.impl;

import com.aims.core.application.services.IOperationConstraintService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationConstraintServiceImplTest {

    @Mock
    private IProductManagerAuditService auditService;

    @InjectMocks
    private OperationConstraintServiceImpl operationConstraintService;

    private String testManagerId;
    private String testProductId;
    private List<String> testProductIds;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testManagerId = "manager123";
        testProductId = "product456";
        testProductIds = Arrays.asList("product456", "product789", "product012");
        testDate = LocalDate.now();
    }

    @Test
    void canAddProduct_alwaysReturnsTrue() {
        // Per problem statement: Unlimited additions allowed
        boolean result = operationConstraintService.canAddProduct(testManagerId);
        
        assertTrue(result);
    }

    @Test
    void canEditProduct_noActiveSession_underDailyLimit_returnsTrue() throws SQLException {
        // Mock: No active edit session and under daily limit
        List<String> operations = Arrays.asList(
            "EDIT_product1_2024-01-01",
            "DELETE_product2_2024-01-01"
        ); // 2 operations, under limit of 30
        when(auditService.getManagerOperations(testManagerId, testDate.toString())).thenReturn(operations);
        
        boolean result = operationConstraintService.canEditProduct(testManagerId, testProductId);
        
        assertTrue(result);
        verify(auditService).getManagerOperations(testManagerId, testDate.toString());
    }

    @Test
    void canEditProduct_hasActiveSession_returnsFalse() throws SQLException {
        // Setup active edit session
        assertDoesNotThrow(() -> 
            operationConstraintService.startEditSession(testManagerId, "otherProduct"));
        
        List<String> operations = Arrays.asList(); // Empty, under daily limit
        when(auditService.getManagerOperations(testManagerId, testDate.toString())).thenReturn(operations);
        
        boolean result = operationConstraintService.canEditProduct(testManagerId, testProductId);
        
        assertFalse(result);
    }

    @Test
    void canDeleteProducts_validQuantity_underDailyLimit_returnsTrue() throws SQLException {
        // Mock: 5 products to delete (under limit of 10), under daily operations limit
        List<String> productIds = Arrays.asList("p1", "p2", "p3", "p4", "p5");
        List<String> operations = Arrays.asList("EDIT_product1_2024-01-01"); // 1 operation, under limit
        when(auditService.getManagerOperations(testManagerId, testDate.toString())).thenReturn(operations);
        
        boolean result = operationConstraintService.canDeleteProducts(testManagerId, productIds);
        
        assertTrue(result);
        verify(auditService).getManagerOperations(testManagerId, testDate.toString());
    }

    @Test
    void canDeleteProducts_exceedsBulkLimit_returnsFalse() throws SQLException {
        // Mock: 15 products to delete (exceeds limit of 10)
        List<String> tooManyProducts = Arrays.asList(
            "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10", "p11", "p12", "p13", "p14", "p15"
        );
        
        boolean result = operationConstraintService.canDeleteProducts(testManagerId, tooManyProducts);
        
        assertFalse(result);
        // Should not even check daily operations if bulk limit is exceeded
        verify(auditService, never()).getManagerOperations(anyString(), anyString());
    }

    @Test
    void canDeleteProducts_exceedsDailyLimit_returnsFalse() throws SQLException {
        // Mock: At daily operations limit (30 operations)
        List<String> operations = Arrays.asList();
        for (int i = 0; i < 30; i++) {
            operations = Arrays.asList(operations.toArray(new String[0]));
            operations.add("EDIT_product" + i + "_2024-01-01");
        }
        when(auditService.getManagerOperations(testManagerId, testDate.toString())).thenReturn(operations);
        
        boolean result = operationConstraintService.canDeleteProducts(testManagerId, testProductIds);
        
        assertFalse(result);
        verify(auditService).getManagerOperations(testManagerId, testDate.toString());
    }

    @Test
    void canUpdatePrice_underDailyLimit_returnsTrue() throws SQLException {
        // Mock: Under daily operations limit
        List<String> operations = Arrays.asList("EDIT_product1_2024-01-01"); // 1 operation
        when(auditService.getManagerOperations(testManagerId, testDate.toString())).thenReturn(operations);
        
        boolean result = operationConstraintService.canUpdatePrice(testManagerId, testProductId);
        
        assertTrue(result);
        verify(auditService).getManagerOperations(testManagerId, testDate.toString());
    }

    @Test
    void getQuotaStatus_returnsCorrectStatus() throws SQLException {
        // Mock: Mixed operations
        List<String> operations = Arrays.asList(
            "ADD_product1_2024-01-01",
            "ADD_product2_2024-01-01",
            "EDIT_product3_2024-01-01",
            "DELETE_product4_2024-01-01",
            "PRICE_UPDATE_product5_2024-01-01_100.00_TO_120.00"
        );
        when(auditService.getManagerOperations(testManagerId, testDate.toString())).thenReturn(operations);
        
        IOperationConstraintService.OperationQuotaStatus status = 
            operationConstraintService.getQuotaStatus(testManagerId, testDate);
        
        assertNotNull(status);
        assertEquals(3, status.getDailyOperationsUsed()); // EDIT + DELETE + PRICE_UPDATE (ADD doesn't count)
        assertEquals(30, status.getDailyOperationsLimit());
        assertEquals(27, status.getDailyOperationsRemaining());
        assertEquals(2, status.getAdditionsToday());
        assertEquals(1, status.getEditsToday());
        assertEquals(1, status.getDeletionsToday());
        assertEquals(1, status.getPriceUpdatesTotal());
        assertFalse(status.hasActiveEditSession());
        verify(auditService).getManagerOperations(testManagerId, testDate.toString());
    }

    @Test
    void validateBulkOperation_addOperation_alwaysSucceeds() throws ValidationException, SQLException {
        List<String> productIds = Arrays.asList("p1", "p2", "p3");
        
        assertDoesNotThrow(() -> 
            operationConstraintService.validateBulkOperation(
                testManagerId, IOperationConstraintService.OperationType.ADD, productIds)
        );
    }

    @Test
    void validateBulkOperation_bulkDelete_exceedsLimit_throwsException() throws SQLException {
        // 15 products exceeds bulk delete limit of 10
        List<String> tooManyProducts = Arrays.asList(
            "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10", "p11", "p12", "p13", "p14", "p15"
        );
        
        ValidationException exception = assertThrows(ValidationException.class, () -> 
            operationConstraintService.validateBulkOperation(
                testManagerId, IOperationConstraintService.OperationType.BULK_DELETE, tooManyProducts)
        );
        
        assertTrue(exception.getMessage().contains("Cannot delete 15 products at once"));
    }

    @Test
    void validateSingleOperation_edit_hasActiveSession_throwsException() throws ValidationException, SQLException {
        // Setup active edit session
        assertDoesNotThrow(() -> 
            operationConstraintService.startEditSession(testManagerId, "otherProduct"));
        
        ValidationException exception = assertThrows(ValidationException.class, () -> 
            operationConstraintService.validateSingleOperation(
                testManagerId, IOperationConstraintService.OperationType.EDIT, testProductId)
        );
        
        assertTrue(exception.getMessage().contains("Cannot start edit"));
        assertTrue(exception.getMessage().contains("Active edit session exists"));
    }

    @Test
    void recordOperation_success() throws SQLException {
        List<String> productIds = Arrays.asList("product1", "product2");
        
        assertDoesNotThrow(() -> 
            operationConstraintService.recordOperation(
                testManagerId, IOperationConstraintService.OperationType.EDIT, productIds)
        );
        
        verify(auditService, times(2)).recordOperation(eq(testManagerId), contains("EDIT_"));
    }

    @Test
    void hasActiveEditSession_noSession_returnsFalse() throws SQLException {
        boolean hasSession = operationConstraintService.hasActiveEditSession(testManagerId);
        
        assertFalse(hasSession);
    }

    @Test
    void startEditSession_success() throws ValidationException, SQLException {
        assertDoesNotThrow(() -> 
            operationConstraintService.startEditSession(testManagerId, testProductId)
        );
        
        assertTrue(operationConstraintService.hasActiveEditSession(testManagerId));
        verify(auditService).recordOperation(eq(testManagerId), contains("EDIT_SESSION_START_"));
    }

    @Test
    void startEditSession_alreadyHasSession_throwsException() throws ValidationException, SQLException {
        // Start first session
        operationConstraintService.startEditSession(testManagerId, "product1");
        
        // Try to start second session
        ValidationException exception = assertThrows(ValidationException.class, () -> 
            operationConstraintService.startEditSession(testManagerId, "product2")
        );
        
        assertTrue(exception.getMessage().contains("already has an active edit session"));
    }

    @Test
    void endEditSession_success() throws ValidationException, SQLException {
        // Start session first
        operationConstraintService.startEditSession(testManagerId, testProductId);
        assertTrue(operationConstraintService.hasActiveEditSession(testManagerId));
        
        // End session
        assertDoesNotThrow(() -> 
            operationConstraintService.endEditSession(testManagerId, testProductId)
        );
        
        assertFalse(operationConstraintService.hasActiveEditSession(testManagerId));
        verify(auditService).recordOperation(eq(testManagerId), contains("EDIT_SESSION_END_"));
    }

    @Test
    void canUpdatePriceForProduct_underLimit_returnsTrue() throws SQLException {
        // Mock: Only 1 price update for this product today (under limit of 2)
        List<String> operations = Arrays.asList(
            "PRICE_UPDATE_" + testProductId + "_2024-01-01_100.00_TO_120.00",
            "PRICE_UPDATE_otherProduct_2024-01-01_200.00_TO_220.00" // Different product
        );
        when(auditService.getManagerOperations(testManagerId, testDate.toString())).thenReturn(operations);
        
        boolean result = ((OperationConstraintServiceImpl) operationConstraintService)
            .canUpdatePriceForProduct(testManagerId, testProductId);
        
        assertTrue(result);
        verify(auditService).getManagerOperations(testManagerId, testDate.toString());
    }

    @Test
    void canUpdatePriceForProduct_atLimit_returnsFalse() throws SQLException {
        // Mock: 2 price updates for this product today (at limit)
        List<String> operations = Arrays.asList(
            "PRICE_UPDATE_" + testProductId + "_2024-01-01_100.00_TO_120.00",
            "PRICE_UPDATE_" + testProductId + "_2024-01-01_120.00_TO_140.00"
        );
        when(auditService.getManagerOperations(testManagerId, testDate.toString())).thenReturn(operations);
        
        boolean result = ((OperationConstraintServiceImpl) operationConstraintService)
            .canUpdatePriceForProduct(testManagerId, testProductId);
        
        assertFalse(result);
        verify(auditService).getManagerOperations(testManagerId, testDate.toString());
    }

    @Test
    void recordPriceUpdate_success() throws SQLException {
        float oldPrice = 100000.0f;
        float newPrice = 120000.0f;
        
        assertDoesNotThrow(() -> 
            ((OperationConstraintServiceImpl) operationConstraintService)
                .recordPriceUpdate(testManagerId, testProductId, oldPrice, newPrice)
        );
        
        verify(auditService).recordOperation(eq(testManagerId), contains("PRICE_UPDATE_" + testProductId));
    }

    @Test
    void operationQuotaStatus_canPerformOperation_correctLogic() throws SQLException {
        // Mock status with some operations used
        List<String> operations = Arrays.asList(
            "EDIT_product1_2024-01-01",
            "DELETE_product2_2024-01-01"
        ); // 2 operations used
        when(auditService.getManagerOperations(testManagerId, testDate.toString())).thenReturn(operations);
        
        IOperationConstraintService.OperationQuotaStatus status = 
            operationConstraintService.getQuotaStatus(testManagerId, testDate);
        
        // Test various operation types
        assertTrue(status.canPerformOperation(IOperationConstraintService.OperationType.ADD, 5)); // Unlimited
        assertTrue(status.canPerformOperation(IOperationConstraintService.OperationType.EDIT, 1)); // No active session, under limit
        assertTrue(status.canPerformOperation(IOperationConstraintService.OperationType.DELETE, 5)); // Under both limits
        assertFalse(status.canPerformOperation(IOperationConstraintService.OperationType.DELETE, 15)); // Exceeds bulk limit
        assertTrue(status.canPerformOperation(IOperationConstraintService.OperationType.PRICE_UPDATE, 1)); // Under limit
    }
}