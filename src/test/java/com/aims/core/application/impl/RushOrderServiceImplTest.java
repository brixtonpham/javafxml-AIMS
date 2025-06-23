package com.aims.core.application.impl;

import com.aims.core.application.services.IRushOrderService;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.shared.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RushOrderServiceImplTest {

    @InjectMocks
    private RushOrderServiceImpl rushOrderService;

    private DeliveryInfo validHanoiDeliveryInfo;
    private DeliveryInfo invalidDeliveryInfo;
    private OrderItem mockOrderItem;
    private List<OrderItem> mockOrderItems;

    @BeforeEach
    void setUp() {
        // Valid Hanoi delivery info
        validHanoiDeliveryInfo = new DeliveryInfo();
        validHanoiDeliveryInfo.setCity("Hà Nội");
        validHanoiDeliveryInfo.setDistrict("Ba Đình");
        validHanoiDeliveryInfo.setDeliveryAddress("123 Test Street");
        validHanoiDeliveryInfo.setRecipientName("Test Recipient");
        validHanoiDeliveryInfo.setRecipientPhone("0123456789");

        // Invalid delivery info (not in Hanoi)
        invalidDeliveryInfo = new DeliveryInfo();
        invalidDeliveryInfo.setCity("Ho Chi Minh City");
        invalidDeliveryInfo.setDistrict("District 1");
        invalidDeliveryInfo.setDeliveryAddress("456 Other Street");
        invalidDeliveryInfo.setRecipientName("Other Recipient");
        invalidDeliveryInfo.setRecipientPhone("0987654321");

        // Mock product and order item
        Product mockProduct = new Product();
        mockProduct.setProductId("product123");
        mockProduct.setTitle("Test Product");
        mockProduct.setPrice(100000.0f);

        mockOrderItem = new OrderItem();
        mockOrderItem.setProduct(mockProduct);
        mockOrderItem.setQuantity(2);
        mockOrderItem.setItemPrice(100000.0f);

        mockOrderItems = Arrays.asList(mockOrderItem);
    }

    @Test
    void validateRushOrderEligibility_validHanoiAddress_returnsEligible() throws ValidationException {
        IRushOrderService.RushOrderEligibilityResult result = 
            rushOrderService.validateRushOrderEligibility(validHanoiDeliveryInfo);
        
        assertTrue(result.isEligible());
        assertEquals("ELIGIBLE", result.getReasonCode());
        assertNotNull(result.getEligibleDistricts());
        assertTrue(result.getEligibleDistricts().contains("Ba Đình"));
    }

    @Test
    void validateRushOrderEligibility_invalidCity_returnsNotEligible() throws ValidationException {
        IRushOrderService.RushOrderEligibilityResult result = 
            rushOrderService.validateRushOrderEligibility(invalidDeliveryInfo);
        
        assertFalse(result.isEligible());
        assertEquals("CITY_NOT_ELIGIBLE", result.getReasonCode());
        assertTrue(result.getMessage().contains("Rush delivery is only available in Hanoi"));
    }

    @Test
    void validateRushOrderEligibility_hanoiButInvalidDistrict_returnsNotEligible() throws ValidationException {
        DeliveryInfo invalidDistrictInfo = new DeliveryInfo();
        invalidDistrictInfo.setCity("Hà Nội");
        invalidDistrictInfo.setDistrict("Invalid District");
        invalidDistrictInfo.setDeliveryAddress("123 Test Street");
        
        IRushOrderService.RushOrderEligibilityResult result = 
            rushOrderService.validateRushOrderEligibility(invalidDistrictInfo);
        
        assertFalse(result.isEligible());
        assertEquals("DISTRICT_NOT_ELIGIBLE", result.getReasonCode());
        assertTrue(result.getMessage().contains("District 'Invalid District' is not eligible"));
    }

    @Test
    void validateRushOrderEligibility_missingDistrict_returnsNotEligible() throws ValidationException {
        DeliveryInfo missingDistrictInfo = new DeliveryInfo();
        missingDistrictInfo.setCity("Hà Nội");
        missingDistrictInfo.setDistrict(null);
        missingDistrictInfo.setDeliveryAddress("123 Test Street");
        
        IRushOrderService.RushOrderEligibilityResult result = 
            rushOrderService.validateRushOrderEligibility(missingDistrictInfo);
        
        assertFalse(result.isEligible());
        assertEquals("MISSING_DISTRICT", result.getReasonCode());
        assertTrue(result.getMessage().contains("District information is required"));
    }

    @Test
    void validateRushOrderEligibility_nullDeliveryInfo_throwsException() {
        assertThrows(ValidationException.class, () -> 
            rushOrderService.validateRushOrderEligibility(null)
        );
    }

    @Test
    void validateOrderItemsForRushDelivery_validItems_returnsEligible() throws ValidationException {
        IRushOrderService.RushOrderEligibilityResult result = 
            rushOrderService.validateOrderItemsForRushDelivery(mockOrderItems);
        
        assertTrue(result.isEligible());
        assertEquals("ELIGIBLE", result.getReasonCode());
        assertTrue(result.getMessage().contains("All order items are eligible"));
    }

    @Test
    void validateOrderItemsForRushDelivery_nullItems_throwsException() {
        assertThrows(ValidationException.class, () -> 
            rushOrderService.validateOrderItemsForRushDelivery(null)
        );
    }

    @Test
    void validateOrderItemsForRushDelivery_emptyItems_throwsException() {
        assertThrows(ValidationException.class, () -> 
            rushOrderService.validateOrderItemsForRushDelivery(Arrays.asList())
        );
    }

    @Test
    void calculateRushDeliveryFee_validOrder_returnsCorrectFee() throws ValidationException, SQLException {
        float rushFee = rushOrderService.calculateRushDeliveryFee(mockOrderItems, validHanoiDeliveryInfo);
        
        assertTrue(rushFee > 0);
        // Should include base fee + weight-based fee
        // For Ba Đình (central district), expect discount multiplier
        assertTrue(rushFee >= 50000.0f); // At least base fee
    }

    @Test
    void calculateRushDeliveryFee_nullItems_throwsException() {
        assertThrows(ValidationException.class, () -> 
            rushOrderService.calculateRushDeliveryFee(null, validHanoiDeliveryInfo)
        );
    }

    @Test
    void calculateRushDeliveryFee_emptyItems_throwsException() {
        assertThrows(ValidationException.class, () -> 
            rushOrderService.calculateRushDeliveryFee(Arrays.asList(), validHanoiDeliveryInfo)
        );
    }

    @Test
    void validateCompleteRushOrder_validOrder_returnsValid() throws ValidationException, SQLException {
        IRushOrderService.RushOrderValidationResult result = 
            rushOrderService.validateCompleteRushOrder(mockOrderItems, validHanoiDeliveryInfo);
        
        assertTrue(result.isValid());
        assertTrue(result.getAddressEligibility().isEligible());
        assertTrue(result.getItemEligibility().isEligible());
        assertTrue(result.getRushDeliveryFee() > 0);
        assertNotNull(result.getTimeEstimate());
        assertTrue(result.getMessage().contains("Rush order is valid"));
    }

    @Test
    void validateCompleteRushOrder_invalidAddress_returnsInvalid() throws ValidationException, SQLException {
        IRushOrderService.RushOrderValidationResult result = 
            rushOrderService.validateCompleteRushOrder(mockOrderItems, invalidDeliveryInfo);
        
        assertFalse(result.isValid());
        assertFalse(result.getAddressEligibility().isEligible());
        assertTrue(result.getItemEligibility().isEligible()); // Items are still valid
        assertEquals(0.0f, result.getRushDeliveryFee(), 0.01f); // No fee for invalid order
        assertNull(result.getTimeEstimate()); // No time estimate for invalid order
        assertTrue(result.getMessage().contains("Rush order validation failed"));
    }

    @Test
    void isDistrictEligibleForRushDelivery_validDistricts_returnsTrue() {
        assertTrue(rushOrderService.isDistrictEligibleForRushDelivery("Ba Đình", "Hà Nội"));
        assertTrue(rushOrderService.isDistrictEligibleForRushDelivery("Hoàn Kiếm", "Hà Nội"));
        assertTrue(rushOrderService.isDistrictEligibleForRushDelivery("Cầu Giấy", "Hà Nội"));
        assertTrue(rushOrderService.isDistrictEligibleForRushDelivery("Thanh Xuân", "Hà Nội"));
    }

    @Test
    void isDistrictEligibleForRushDelivery_invalidDistrict_returnsFalse() {
        assertFalse(rushOrderService.isDistrictEligibleForRushDelivery("Invalid District", "Hà Nội"));
        assertFalse(rushOrderService.isDistrictEligibleForRushDelivery("District 1", "Ho Chi Minh City"));
    }

    @Test
    void isDistrictEligibleForRushDelivery_nullValues_returnsFalse() {
        assertFalse(rushOrderService.isDistrictEligibleForRushDelivery(null, "Hà Nội"));
        assertFalse(rushOrderService.isDistrictEligibleForRushDelivery("Ba Đình", null));
        assertFalse(rushOrderService.isDistrictEligibleForRushDelivery(null, null));
    }

    @Test
    void isDistrictEligibleForRushDelivery_caseInsensitive_returnsTrue() {
        assertTrue(rushOrderService.isDistrictEligibleForRushDelivery("ba đình", "hanoi"));
        assertTrue(rushOrderService.isDistrictEligibleForRushDelivery("BA ĐÌNH", "HÀ NỘI"));
        assertTrue(rushOrderService.isDistrictEligibleForRushDelivery("Ba Đình", "hà nội"));
    }

    @Test
    void getEligibleRushDeliveryDistricts_returnsHanoiDistricts() {
        List<String> eligibleDistricts = rushOrderService.getEligibleRushDeliveryDistricts();
        
        assertNotNull(eligibleDistricts);
        assertFalse(eligibleDistricts.isEmpty());
        assertTrue(eligibleDistricts.contains("Ba Đình"));
        assertTrue(eligibleDistricts.contains("Hoàn Kiếm"));
        assertTrue(eligibleDistricts.contains("Cầu Giấy"));
        assertTrue(eligibleDistricts.contains("Thanh Xuân"));
        assertTrue(eligibleDistricts.contains("Hà Đông"));
    }

    @Test
    void calculateRushDeliveryTime_returnsValidEstimate() {
        IRushOrderService.RushDeliveryTimeEstimate estimate = 
            rushOrderService.calculateRushDeliveryTime("Ba Đình");
        
        assertNotNull(estimate);
        assertEquals(3, estimate.getEstimatedHours()); // 3-hour delivery
        assertNotNull(estimate.getTimeWindow());
        assertEquals("18:00", estimate.getCutoffTime());
        // availableToday depends on current time, so we just check it's not null
        assertNotNull(estimate.getTimeWindow());
    }

    @Test
    void calculateRushDeliveryFee_differentDistricts_differentFees() throws ValidationException, SQLException {
        // Test central district (discount)
        DeliveryInfo centralDistrictInfo = new DeliveryInfo();
        centralDistrictInfo.setCity("Hà Nội");
        centralDistrictInfo.setDistrict("Hoàn Kiếm");
        centralDistrictInfo.setDeliveryAddress("123 Test Street");
        
        float centralFee = rushOrderService.calculateRushDeliveryFee(mockOrderItems, centralDistrictInfo);
        
        // Test outer district (surcharge)
        DeliveryInfo outerDistrictInfo = new DeliveryInfo();
        outerDistrictInfo.setCity("Hà Nội");
        outerDistrictInfo.setDistrict("Tây Hồ");
        outerDistrictInfo.setDeliveryAddress("456 Test Street");
        
        float outerFee = rushOrderService.calculateRushDeliveryFee(mockOrderItems, outerDistrictInfo);
        
        // Outer districts should have higher fees than central districts
        assertTrue(outerFee > centralFee);
    }

    @Test
    void calculateRushDeliveryFee_roundedToThousands() throws ValidationException, SQLException {
        float rushFee = rushOrderService.calculateRushDeliveryFee(mockOrderItems, validHanoiDeliveryInfo);
        
        // Fee should be rounded to nearest 1000 VND
        assertEquals(0, rushFee % 1000, 0.01f);
    }

    @Test
    void validateCompleteRushOrder_includesAllValidationResults() throws ValidationException, SQLException {
        IRushOrderService.RushOrderValidationResult result = 
            rushOrderService.validateCompleteRushOrder(mockOrderItems, validHanoiDeliveryInfo);
        
        // Should include all validation components
        assertNotNull(result.getAddressEligibility());
        assertNotNull(result.getItemEligibility());
        assertTrue(result.getRushDeliveryFee() > 0);
        assertNotNull(result.getTimeEstimate());
        
        // All components should be valid for this test case
        assertTrue(result.getAddressEligibility().isEligible());
        assertTrue(result.getItemEligibility().isEligible());
        assertTrue(result.isValid());
    }
}