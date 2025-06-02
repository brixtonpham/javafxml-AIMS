package com.aims.core.application.impl;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.shared.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryCalculationServiceImplTest {

    private DeliveryCalculationServiceImpl deliveryService;
    private OrderEntity mockOrder;
    private DeliveryInfo mockDeliveryInfo;
    private Product mockProduct1;
    private Product mockProduct2;

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryCalculationServiceImpl();
        mockOrder = new OrderEntity();
        mockDeliveryInfo = new DeliveryInfo();
        mockOrder.setDeliveryInfo(mockDeliveryInfo);
        mockOrder.setOrderItems(new ArrayList<>());

        mockProduct1 = new Product();
        mockProduct1.setProductId("prod1");
        mockProduct1.setTitle("Product 1");
        mockProduct1.setWeightKg(1.0f); // 1kg
        mockProduct1.setPrice(50000f); // For free shipping check

        mockProduct2 = new Product();
        mockProduct2.setProductId("prod2");
        mockProduct2.setTitle("Product 2 Rush Eligible");
        mockProduct2.setWeightKg(2.5f); // 2.5kg
        mockProduct2.setPrice(60000f); // For free shipping check
    }

    private OrderItem createOrderItem(Product product, int quantity, float priceAtOrder, boolean rushEligible) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setPriceAtTimeOfOrder(priceAtOrder);
        item.setEligibleForRushDelivery(rushEligible); // Corrected: Matches OrderItem entity field
        return item;
    }

    // Tests for calculateShippingFee

    @Test
    void calculateShippingFee_nullOrder_throwsValidationException() {
        Exception exception = assertThrows(ValidationException.class, () -> {
            deliveryService.calculateShippingFee(null, false);
        });
        assertEquals("Order, order items, and delivery information are required for shipping calculation.", exception.getMessage());
    }

    @Test
    void calculateShippingFee_nullDeliveryInfo_throwsValidationException() {
        mockOrder.setDeliveryInfo(null);
        mockOrder.getOrderItems().add(createOrderItem(mockProduct1, 1, mockProduct1.getPrice(), false));
        Exception exception = assertThrows(ValidationException.class, () -> {
            deliveryService.calculateShippingFee(mockOrder, false);
        });
        assertEquals("Order, order items, and delivery information are required for shipping calculation.", exception.getMessage());
    }

    @Test
    void calculateShippingFee_emptyItems_throwsValidationException() {
        mockDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        mockDeliveryInfo.setDeliveryAddress("123 Hoan Kiem");
        Exception exception = assertThrows(ValidationException.class, () -> {
            deliveryService.calculateShippingFee(mockOrder, false);
        });
        assertEquals("Order, order items, and delivery information are required for shipping calculation.", exception.getMessage());
    }
    
    @Test
    void calculateShippingFee_standardDelivery_HanoiInnerCity_Under3kg() throws ValidationException {
        mockDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        mockDeliveryInfo.setDeliveryAddress("123 Hoan Kiem street"); // "hoan kiem" is inner city
        mockOrder.getOrderItems().add(createOrderItem(mockProduct1, 1, mockProduct1.getPrice(), false)); // 1kg
        float fee = deliveryService.calculateShippingFee(mockOrder, false);
        assertEquals(22000f, fee, 0.01f); // Base fee for Hanoi/HCM <= 3kg
    }

    @Test
    void calculateShippingFee_standardDelivery_HanoiInnerCity_Over3kg() throws ValidationException {
        mockDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        mockDeliveryInfo.setDeliveryAddress("123 Ba Dinh district"); // "ba dinh" is inner city
        mockOrder.getOrderItems().add(createOrderItem(mockProduct1, 4, mockProduct1.getPrice(), false)); // 4kg
        // 3kg base = 22000. Additional 1kg = 2 * 0.5kg increments = 2 * 2500 = 5000. Total = 27000
        // Order value = 4 * 50000 = 200000. Free shipping discount = 25000.
        // Fee = max(0, 27000 - 25000) = 2000.
        float fee = deliveryService.calculateShippingFee(mockOrder, false);
        assertEquals(2000f, fee, 0.01f);
    }
    
    @Test
    void calculateShippingFee_standardDelivery_OtherProvince_Under0_5kg() throws ValidationException {
        mockDeliveryInfo.setDeliveryProvinceCity("Da Nang");
        mockDeliveryInfo.setDeliveryAddress("123 Hai Chau");
        Product lightProduct = new Product();
        lightProduct.setWeightKg(0.4f);
        lightProduct.setPrice(10000f);
        mockOrder.getOrderItems().add(createOrderItem(lightProduct, 1, lightProduct.getPrice(), false)); // 0.4kg
        float fee = deliveryService.calculateShippingFee(mockOrder, false);
        assertEquals(30000f, fee, 0.01f); // Base fee for other places <= 0.5kg
    }

    @Test
    void calculateShippingFee_standardDelivery_OtherProvince_Over0_5kg() throws ValidationException {
        mockDeliveryInfo.setDeliveryProvinceCity("Can Tho");
        mockDeliveryInfo.setDeliveryAddress("123 Ninh Kieu");
        mockOrder.getOrderItems().add(createOrderItem(mockProduct1, 1, mockProduct1.getPrice(), false)); // 1kg
        // 0.5kg base = 30000. Additional 0.5kg = 1 * 0.5kg increment = 1 * 2500 = 2500. Total = 32500
        float fee = deliveryService.calculateShippingFee(mockOrder, false);
        assertEquals(30000f + 1 * 2500f, fee, 0.01f);
    }

    @Test
    void calculateShippingFee_standardDelivery_HanoiInnerCity_FreeShippingEligible() throws ValidationException {
        mockDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        mockDeliveryInfo.setDeliveryAddress("123 Dong Da central"); // "dong da" is inner city
        mockOrder.getOrderItems().add(createOrderItem(mockProduct1, 3, mockProduct1.getPrice(), false)); // 3kg, Value = 3 * 50000 = 150000 > 100000
        // Base fee for 3kg = 22000. Discount = 25000. Fee = max(0, 22000 - 25000) = 0
        float fee = deliveryService.calculateShippingFee(mockOrder, false);
        assertEquals(0f, fee, 0.01f);
    }
    
    @Test
    void calculateShippingFee_standardDelivery_HanoiInnerCity_PartialFreeShipping() throws ValidationException {
        mockDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        mockDeliveryInfo.setDeliveryAddress("123 Tay Ho lake view"); // "tay ho" is inner city
        Product heavyProduct = new Product();
        heavyProduct.setWeightKg(10f); // 10kg
        heavyProduct.setPrice(120000f); // Value > 100000
        mockOrder.getOrderItems().add(createOrderItem(heavyProduct, 1, heavyProduct.getPrice(), false));
        // Base fee for 10kg in Hanoi: 22000 (3kg) + ceil((10-3)/0.5)*2500 = 22000 + ceil(7/0.5)*2500 = 22000 + 14*2500 = 22000 + 35000 = 57000
        // Discount = 25000. Fee = 57000 - 25000 = 32000
        float fee = deliveryService.calculateShippingFee(mockOrder, false);
        assertEquals(57000f - 25000f, fee, 0.01f);
    }

    @Test
    void calculateShippingFee_rushOrder_HanoiInnerCity_EligibleItem() throws ValidationException {
        mockDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        mockDeliveryInfo.setDeliveryAddress("456 Cau Giay street"); // "cau giay" is inner city
        mockOrder.getOrderItems().add(createOrderItem(mockProduct2, 1, mockProduct2.getPrice(), true)); // 2.5kg, rush eligible
        // Base fee for 2.5kg in Hanoi = 22000. Rush surcharge = 1 * 10000. Total = 32000
        float fee = deliveryService.calculateShippingFee(mockOrder, true);
        assertEquals(22000f + 10000f, fee, 0.01f);
    }

    @Test
    void calculateShippingFee_rushOrder_HanoiInnerCity_MixOfItems() throws ValidationException {
        mockDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        mockDeliveryInfo.setDeliveryAddress("789 Thanh Xuan district"); // "thanh xuan" is inner city
        mockOrder.getOrderItems().add(createOrderItem(mockProduct1, 1, mockProduct1.getPrice(), false)); // 1kg, standard
        mockOrder.getOrderItems().add(createOrderItem(mockProduct2, 1, mockProduct2.getPrice(), true));  // 2.5kg, rush

        // Standard part: 1kg in Hanoi. Fee = 22000. Value = 50000 (no free shipping).
        // Rush part: 2.5kg in Hanoi. Fee = 22000 (base) + 10000 (surcharge) = 32000.
        // Total = 22000 + 32000 = 54000
        float fee = deliveryService.calculateShippingFee(mockOrder, true);
        assertEquals(22000f + (22000f + 10000f), fee, 0.01f);
    }
    
    @Test
    void calculateShippingFee_rushOrder_HanoiInnerCity_MixItems_StandardFreeShipping() throws ValidationException {
        mockDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        mockDeliveryInfo.setDeliveryAddress("789 Hoang Mai area"); // "hoang mai" is inner city
        Product standardProductExpensive = new Product();
        standardProductExpensive.setWeightKg(1f);
        standardProductExpensive.setPrice(150000f); // Qualifies for free shipping
        
        mockOrder.getOrderItems().add(createOrderItem(standardProductExpensive, 1, standardProductExpensive.getPrice(), false)); // 1kg, standard, free shipping
        mockOrder.getOrderItems().add(createOrderItem(mockProduct2, 1, mockProduct2.getPrice(), true));  // 2.5kg, rush

        // Standard part: 1kg in Hanoi. Base Fee = 22000. Value = 150000. Discount = 25000. Standard Fee = max(0, 22000-25000) = 0.
        // Rush part: 2.5kg in Hanoi. Fee = 22000 (base) + 10000 (surcharge) = 32000.
        // Total = 0 + 32000 = 32000
        float fee = deliveryService.calculateShippingFee(mockOrder, true);
        assertEquals(32000f, fee, 0.01f);
    }


    @Test
    void calculateShippingFee_rushOrder_NotEligibleAddress_throwsValidationException() {
        mockDeliveryInfo.setDeliveryProvinceCity("Da Nang"); // Not Hanoi
        mockDeliveryInfo.setDeliveryAddress("123 Son Tra");
        mockOrder.getOrderItems().add(createOrderItem(mockProduct2, 1, mockProduct2.getPrice(), true));
        Exception exception = assertThrows(ValidationException.class, () -> {
            deliveryService.calculateShippingFee(mockOrder, true);
        });
        assertTrue(exception.getMessage().contains("Delivery address is not eligible for rush order."));
    }
    
    @Test
    void calculateShippingFee_rushOrder_HanoiOuterDistrict_throwsValidationException() {
        mockDeliveryInfo.setDeliveryProvinceCity("Hanoi"); 
        mockDeliveryInfo.setDeliveryAddress("123 Soc Son town"); // Soc Son is not in HANOI_INNER_CITY_DISTRICTS
        mockOrder.getOrderItems().add(createOrderItem(mockProduct2, 1, mockProduct2.getPrice(), true));
        Exception exception = assertThrows(ValidationException.class, () -> {
            deliveryService.calculateShippingFee(mockOrder, true);
        });
        assertTrue(exception.getMessage().contains("Delivery address is not eligible for rush order."));
    }

    @Test
    void calculateShippingFee_rushOrder_NoRushEligibleItems_throwsValidationException() {
        mockDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        mockDeliveryInfo.setDeliveryAddress("123 Hoan Kiem");
        mockOrder.getOrderItems().add(createOrderItem(mockProduct1, 1, mockProduct1.getPrice(), false)); // Not rush eligible
        Exception exception = assertThrows(ValidationException.class, () -> {
            deliveryService.calculateShippingFee(mockOrder, true);
        });
        assertEquals("Rush order requested, but no items in the order are eligible for rush delivery.", exception.getMessage());
    }
    
    @Test
    void calculateShippingFee_productDetailsMissing_throwsValidationException() {
        mockDeliveryInfo.setDeliveryProvinceCity("Hanoi");
        mockDeliveryInfo.setDeliveryAddress("123 Hoan Kiem");
        OrderItem itemWithNullProduct = createOrderItem(null, 1, 1000f, false);
        mockOrder.getOrderItems().add(itemWithNullProduct);

        Exception exception = assertThrows(ValidationException.class, () -> {
            deliveryService.calculateShippingFee(mockOrder, false);
        });
        assertEquals("Product details missing for an order item during fee calculation.", exception.getMessage());
    }


    // Tests for getFreeShippingDiscount
    @ParameterizedTest
    @CsvSource({
            "50000, 0",      // Below threshold
            "100000, 0",     // At threshold (exclusive, so no discount)
            "100000.01, 25000",// Slightly above threshold
            "150000, 25000",   // Well above threshold
            "0, 0"           // Zero value
    })
    void getFreeShippingDiscount_variousValues(float totalValue, float expectedDiscount) {
        assertEquals(expectedDiscount, deliveryService.getFreeShippingDiscount(totalValue), 0.01f);
    }

    // Tests for isRushDeliveryAddressEligible
    @Test
    void isRushDeliveryAddressEligible_HanoiInnerCity_ReturnsTrue() {
        DeliveryInfo info = new DeliveryInfo();
        info.setDeliveryProvinceCity("Hanoi");
        info.setDeliveryAddress("123 Hoan Kiem District");
        assertTrue(deliveryService.isRushDeliveryAddressEligible(info));

        info.setDeliveryAddress("Some place in Ba Dinh");
        assertTrue(deliveryService.isRushDeliveryAddressEligible(info));
        
        info.setDeliveryAddress("long bien bridge area"); // "long bien"
        assertTrue(deliveryService.isRushDeliveryAddressEligible(info));
    }

    @Test
    void isRushDeliveryAddressEligible_HanoiOuterCity_ReturnsFalse() {
        DeliveryInfo info = new DeliveryInfo();
        info.setDeliveryProvinceCity("Hanoi");
        info.setDeliveryAddress("123 Soc Son Town"); // Soc Son is typically outer
        assertFalse(deliveryService.isRushDeliveryAddressEligible(info));
    }

    @Test
    void isRushDeliveryAddressEligible_OtherProvince_ReturnsFalse() {
        DeliveryInfo info = new DeliveryInfo();
        info.setDeliveryProvinceCity("Da Nang");
        info.setDeliveryAddress("123 Hai Chau");
        assertFalse(deliveryService.isRushDeliveryAddressEligible(info));
    }
    
    @Test
    void isRushDeliveryAddressEligible_NullDeliveryInfo_ReturnsFalse() {
        assertFalse(deliveryService.isRushDeliveryAddressEligible(null));
    }

    @Test
    void isRushDeliveryAddressEligible_NullProvince_ReturnsFalse() {
        DeliveryInfo info = new DeliveryInfo();
        info.setDeliveryAddress("123 Some Street");
        assertFalse(deliveryService.isRushDeliveryAddressEligible(info));
    }

    @Test
    void isRushDeliveryAddressEligible_NullAddress_ReturnsFalse() {
        DeliveryInfo info = new DeliveryInfo();
        info.setDeliveryProvinceCity("Hanoi");
        assertFalse(deliveryService.isRushDeliveryAddressEligible(info));
    }
    
    @Test
    void isRushDeliveryAddressEligible_CaseInsensitiveChecks() {
        DeliveryInfo info = new DeliveryInfo();
        info.setDeliveryProvinceCity("hAnOi");
        info.setDeliveryAddress("123 hOaN kIeM District");
        assertTrue(deliveryService.isRushDeliveryAddressEligible(info));
    }


    // Tests for calculateFeeWithDimensionalWeight
    @Test
    void calculateFeeWithDimensionalWeight_HanoiHCM_ActualWeightDominant() throws ValidationException {
        // Actual: 2kg, Dim: (10*10*10)/6000 = 0.167kg. Chargeable = 2kg.
        // Fee for 2kg in Hanoi/HCM = 22000 (base for <=3kg)
        float fee = deliveryService.calculateFeeWithDimensionalWeight(2f, 10f, 10f, 10f, "Hanoi", true);
        assertEquals(22000f, fee, 0.01f);
    }

    @Test
    void calculateFeeWithDimensionalWeight_HanoiHCM_DimensionalWeightDominant_Under3kg() throws ValidationException {
        // Actual: 1kg, Dim: (30*30*20)/6000 = 18000/6000 = 3kg. Chargeable = 3kg.
        // Fee for 3kg in Hanoi/HCM = 22000 (base for <=3kg)
        float fee = deliveryService.calculateFeeWithDimensionalWeight(1f, 30f, 30f, 20f, "Ho Chi Minh City", true);
        assertEquals(22000f, fee, 0.01f);
    }
    
    @Test
    void calculateFeeWithDimensionalWeight_HanoiHCM_DimensionalWeightDominant_Over3kg() throws ValidationException {
        // Actual: 1kg, Dim: (40*30*25)/6000 = 30000/6000 = 5kg. Chargeable = 5kg.
        // Fee for 5kg in Hanoi/HCM: 22000 (3kg) + ceil((5-3)/0.5)*2500 = 22000 + ceil(2/0.5)*2500 = 22000 + 4*2500 = 22000 + 10000 = 32000
        float fee = deliveryService.calculateFeeWithDimensionalWeight(1f, 40f, 30f, 25f, "Hanoi", true);
        assertEquals(32000f, fee, 0.01f);
    }

    @Test
    void calculateFeeWithDimensionalWeight_OtherProvince_ActualWeightDominant() throws ValidationException {
        // Actual: 0.4kg, Dim: (5*5*5)/6000 = 0.02kg. Chargeable = 0.4kg.
        // Fee for 0.4kg other: 30000 (base for <=0.5kg)
        float fee = deliveryService.calculateFeeWithDimensionalWeight(0.4f, 5f, 5f, 5f, "Da Nang", false);
        assertEquals(30000f, fee, 0.01f);
    }

    @Test
    void calculateFeeWithDimensionalWeight_OtherProvince_DimensionalWeightDominant_Under0_5kg() throws ValidationException {
        // Actual: 0.1kg, Dim: (20*10*10)/6000 = 2000/6000 = 0.333kg. Chargeable = 0.333kg.
        // Fee for 0.333kg other: 30000 (base for <= 0.5kg)
        float fee = deliveryService.calculateFeeWithDimensionalWeight(0.1f, 20f, 10f, 10f, "Can Tho", false);
        assertEquals(30000f, fee, 0.01f);
    }
    
    @Test
    void calculateFeeWithDimensionalWeight_OtherProvince_DimensionalWeightDominant_Over0_5kg() throws ValidationException {
        // Actual: 0.2kg, Dim: (30*20*15)/6000 = 9000/6000 = 1.5kg. Chargeable = 1.5kg.
        // Fee for 1.5kg other: 30000 (0.5kg) + ceil((1.5-0.5)/0.5)*2500 = 30000 + ceil(1/0.5)*2500 = 30000 + 2*2500 = 30000 + 5000 = 35000
        float fee = deliveryService.calculateFeeWithDimensionalWeight(0.2f, 30f, 20f, 15f, "Hai Phong", false);
        assertEquals(35000f, fee, 0.01f);
    }

    @ParameterizedTest
    @ValueSource(floats = {0f, -1f})
    void calculateFeeWithDimensionalWeight_InvalidWeightOrDimension_throwsValidationException(float invalidValue) {
        assertThrows(ValidationException.class, () -> {
            deliveryService.calculateFeeWithDimensionalWeight(invalidValue, 10f, 10f, 10f, "Hanoi", true);
        });
        assertThrows(ValidationException.class, () -> {
            deliveryService.calculateFeeWithDimensionalWeight(1f, invalidValue, 10f, 10f, "Hanoi", true);
        });
         assertThrows(ValidationException.class, () -> {
            deliveryService.calculateFeeWithDimensionalWeight(1f, 10f, invalidValue, 10f, "Hanoi", true);
        });
         assertThrows(ValidationException.class, () -> {
            deliveryService.calculateFeeWithDimensionalWeight(1f, 10f, 10f, invalidValue, "Hanoi", true);
        });
    }

    @Test
    void calculateFeeWithDimensionalWeight_NullProvince_throwsValidationException() {
         assertThrows(ValidationException.class, () -> {
            deliveryService.calculateFeeWithDimensionalWeight(1f, 10f, 10f, 10f, null, true);
        });
    }
     @Test
    void calculateFeeWithDimensionalWeight_EmptyProvince_throwsValidationException() {
         assertThrows(ValidationException.class, () -> {
            deliveryService.calculateFeeWithDimensionalWeight(1f, 10f, 10f, 10f, "   ", true);
        });
    }
}
