package com.aims.core.application.impl;

import com.aims.core.application.services.IVATCalculationService;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VATCalculationServiceImplTest {

    @InjectMocks
    private VATCalculationServiceImpl vatCalculationService;

    private Product mockProduct;
    private OrderItem mockOrderItem;
    private List<OrderItem> mockOrderItems;
    private float testDeliveryFee;

    @BeforeEach
    void setUp() {
        // Create mock product
        mockProduct = new Product();
        mockProduct.setProductId("product123");
        mockProduct.setTitle("Test Product");
        mockProduct.setPrice(100000.0f); // 100,000 VND (excluding VAT)

        // Create mock order item
        mockOrderItem = new OrderItem();
        mockOrderItem.setProduct(mockProduct);
        mockOrderItem.setQuantity(2);
        mockOrderItem.setItemPrice(100000.0f); // Price excluding VAT

        mockOrderItems = Arrays.asList(mockOrderItem);
        testDeliveryFee = 20000.0f; // 20,000 VND
    }

    @Test
    void calculateVATForPrice_standardRate() {
        float basePrice = 100000.0f;
        
        float vatAmount = vatCalculationService.calculateVATForPrice(basePrice);
        
        assertEquals(10000.0f, vatAmount, 0.01f); // 10% of 100,000 = 10,000
    }

    @Test
    void calculatePriceIncludingVAT_correctCalculation() {
        float basePriceExclVAT = 100000.0f;
        
        float priceInclVAT = vatCalculationService.calculatePriceIncludingVAT(basePriceExclVAT);
        
        assertEquals(110000.0f, priceInclVAT, 0.01f); // 100,000 + 10% = 110,000
    }

    @Test
    void getVATRate_returnsCorrectRate() {
        float vatRate = vatCalculationService.getVATRate();
        
        assertEquals(0.10f, vatRate, 0.001f); // 10% VAT rate
    }

    @Test
    void calculatePriceExcludingVAT_correctCalculation() {
        float priceInclVAT = 110000.0f;
        
        float priceExclVAT = vatCalculationService.calculatePriceExcludingVAT(priceInclVAT);
        
        assertEquals(100000.0f, priceExclVAT, 0.01f); // 110,000 / 1.1 = 100,000
    }

    @Test
    void calculateVATForDeliveryFee_correctCalculation() {
        float deliveryFee = 20000.0f;
        
        float deliveryVAT = vatCalculationService.calculateVATForDeliveryFee(deliveryFee);
        
        assertEquals(2000.0f, deliveryVAT, 0.01f); // 10% of 20,000 = 2,000
    }

    @Test
    void calculateOrderVAT_withDeliveryFee() {
        IVATCalculationService.OrderPriceBreakdown breakdown = 
            vatCalculationService.calculateOrderVAT(mockOrderItems, testDeliveryFee);
        
        assertNotNull(breakdown);
        assertEquals(200000.0f, breakdown.getSubtotalExclVAT(), 0.01f); // 2 * 100,000
        assertEquals(22000.0f, breakdown.getTotalVATAmount(), 0.01f); // VAT on products + delivery
        assertEquals(222000.0f, breakdown.getSubtotalInclVAT(), 0.01f); // Including VAT
        assertEquals(242000.0f, breakdown.getTotalOrderAmount(), 0.01f); // Including delivery with VAT
    }

    @Test
    void calculateOrderVAT_withoutDeliveryFee() {
        IVATCalculationService.OrderPriceBreakdown breakdown = 
            vatCalculationService.calculateOrderVAT(mockOrderItems, 0.0f);
        
        assertNotNull(breakdown);
        assertEquals(200000.0f, breakdown.getSubtotalExclVAT(), 0.01f); // 2 * 100,000
        assertEquals(20000.0f, breakdown.getTotalVATAmount(), 0.01f); // VAT on products only
        assertEquals(220000.0f, breakdown.getSubtotalInclVAT(), 0.01f); // Including VAT
        assertEquals(220000.0f, breakdown.getTotalOrderAmount(), 0.01f); // No delivery fee
    }

    @Test
    void calculateItemVAT_singleItem() {
        List<IVATCalculationService.ItemPriceBreakdown> itemBreakdowns = 
            vatCalculationService.calculateItemVAT(mockOrderItems);
        
        assertNotNull(itemBreakdowns);
        assertEquals(1, itemBreakdowns.size());
        
        IVATCalculationService.ItemPriceBreakdown itemBreakdown = itemBreakdowns.get(0);
        assertEquals("product123", itemBreakdown.getProductId());
        assertEquals("Test Product", itemBreakdown.getProductTitle());
        assertEquals(2, itemBreakdown.getQuantity());
        assertEquals(100000.0f, itemBreakdown.getUnitPriceExclVAT(), 0.01f);
        assertEquals(110000.0f, itemBreakdown.getUnitPriceInclVAT(), 0.01f);
        assertEquals(200000.0f, itemBreakdown.getTotalPriceExclVAT(), 0.01f);
        assertEquals(220000.0f, itemBreakdown.getTotalPriceInclVAT(), 0.01f);
        assertEquals(20000.0f, itemBreakdown.getTotalVATAmount(), 0.01f);
    }

    @Test
    void calculateTotalVATAmount_multipleItems() {
        // Add second item
        Product secondProduct = new Product();
        secondProduct.setProductId("product456");
        secondProduct.setPrice(50000.0f);
        
        OrderItem secondItem = new OrderItem();
        secondItem.setProduct(secondProduct);
        secondItem.setQuantity(1);
        secondItem.setItemPrice(50000.0f);
        
        List<OrderItem> multipleItems = Arrays.asList(mockOrderItem, secondItem);
        
        float totalVAT = vatCalculationService.calculateTotalVATAmount(multipleItems);
        
        assertEquals(25000.0f, totalVAT, 0.01f); // (2*100,000 + 1*50,000) * 10% = 25,000
    }

    @Test
    void calculateVATBreakdown_detailedBreakdown() {
        float basePrice = 100000.0f;
        
        VATCalculationServiceImpl.VATBreakdown breakdown = 
            ((VATCalculationServiceImpl) vatCalculationService).calculateVATBreakdown(basePrice);
        
        assertNotNull(breakdown);
        assertEquals(100000.0f, breakdown.getBasePrice(), 0.01f);
        assertEquals(10000.0f, breakdown.getVatAmount(), 0.01f);
        assertEquals(110000.0f, breakdown.getTotalPrice(), 0.01f);
        assertEquals(0.10f, breakdown.getVatRate(), 0.001f);
    }

    @Test
    void validateOrderVATCalculations_validBreakdown_returnsTrue() {
        IVATCalculationService.OrderPriceBreakdown validBreakdown = 
            new IVATCalculationService.OrderPriceBreakdown(
                100000.0f, // subtotalExclVAT
                10000.0f,  // totalVATAmount (10%)
                110000.0f, // subtotalInclVAT
                20000.0f,  // deliveryFee
                2000.0f,   // deliveryVAT (10%)
                132000.0f  // totalOrderAmount
            );
        
        boolean isValid = ((VATCalculationServiceImpl) vatCalculationService)
            .validateOrderVATCalculations(validBreakdown);
        
        assertTrue(isValid);
    }

    @Test
    void validateOrderVATCalculations_invalidBreakdown_returnsFalse() {
        IVATCalculationService.OrderPriceBreakdown invalidBreakdown = 
            new IVATCalculationService.OrderPriceBreakdown(
                100000.0f, // subtotalExclVAT
                5000.0f,   // totalVATAmount (incorrect - should be 10,000)
                110000.0f, // subtotalInclVAT
                20000.0f,  // deliveryFee
                2000.0f,   // deliveryVAT
                132000.0f  // totalOrderAmount
            );
        
        boolean isValid = ((VATCalculationServiceImpl) vatCalculationService)
            .validateOrderVATCalculations(invalidBreakdown);
        
        assertFalse(isValid);
    }

    @Test
    void calculateVATForPrice_zeroPrice_returnsZero() {
        float vatAmount = vatCalculationService.calculateVATForPrice(0.0f);
        
        assertEquals(0.0f, vatAmount, 0.01f);
    }

    @Test
    void calculateOrderVAT_emptyOrderItems_returnsZeroBreakdown() {
        List<OrderItem> emptyItems = Arrays.asList();
        
        IVATCalculationService.OrderPriceBreakdown breakdown = 
            vatCalculationService.calculateOrderVAT(emptyItems, testDeliveryFee);
        
        assertNotNull(breakdown);
        assertEquals(0.0f, breakdown.getSubtotalExclVAT(), 0.01f);
        assertEquals(2000.0f, breakdown.getTotalVATAmount(), 0.01f); // Only delivery VAT
        assertEquals(0.0f, breakdown.getSubtotalInclVAT(), 0.01f);
        assertEquals(22000.0f, breakdown.getTotalOrderAmount(), 0.01f); // Only delivery with VAT
    }

    @Test
    void calculateVATForPrice_negativePrice_returnsZero() {
        float vatAmount = vatCalculationService.calculateVATForPrice(-1000.0f);
        
        assertEquals(0.0f, vatAmount, 0.01f);
    }
}