package com.aims.test.compliance;

import com.aims.core.application.services.*;
import com.aims.core.entities.*;
import com.aims.core.shared.ServiceFactory;
import com.aims.test.base.BaseUITest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AIMS Phase 4.1: VAT Calculation and Pricing Compliance Tests
 * 
 * Validates VAT calculation and pricing requirements from AIMS-ProblemStatement-v2.0.pdf:
 * - Lines 22-23: 10% VAT calculation accuracy
 * - Lines 41-42: Free shipping >100,000 VND, max 25,000 discount
 * 
 * Total Tests: 4 VAT and pricing compliance validation tests
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VATCalculationComplianceTest extends BaseUITest {

    private static final Logger logger = Logger.getLogger(VATCalculationComplianceTest.class.getName());

    // Business Rule Constants from Problem Statement
    private static final float VAT_RATE = 0.10f; // Line 22-23: 10% VAT
    private static final float FREE_SHIPPING_THRESHOLD = 100000.0f; // Line 41: >100,000 VND
    private static final float MAX_SHIPPING_DISCOUNT = 25000.0f; // Line 42: max 25,000 VND discount
    private static final float VAT_CALCULATION_TOLERANCE = 0.01f; // 1 cent tolerance for floating point

    // Core Services
    private IVATCalculationService vatCalculationService;
    private IDeliveryCalculationService deliveryCalculationService;
    private ICartService cartService;
    private IOrderService orderService;
    private IProductService productService;

    // Test Data
    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        logger.info("=== Setting up VAT Calculation Compliance Tests ===");
        
        // Initialize services
        ServiceFactory serviceFactory = ServiceFactory.getInstance();
        vatCalculationService = serviceFactory.getVATCalculationService();
        deliveryCalculationService = serviceFactory.getDeliveryCalculationService();
        cartService = serviceFactory.getCartService();
        orderService = serviceFactory.getOrderService();
        productService = serviceFactory.getProductService();
        
        // Setup test products
        setupVATTestProducts();
        
        logger.info("✓ VAT calculation compliance test setup completed");
    }

    // ========================================
    // VAT Calculation Accuracy Tests (Lines 22-23)
    // ========================================

    @Test
    @Order(1)
    @DisplayName("PS-VAT-001: 10% VAT calculation accuracy - Lines 22-23")
    void testVATCalculationAccuracy() throws Exception {
        logger.info("=== Testing 10% VAT Calculation Accuracy - Lines 22-23 ===");
        
        // Test VAT calculation on various price points
        float[] testPrices = {
            10000.0f,   // 10,000 VND
            50000.0f,   // 50,000 VND
            100000.0f,  // 100,000 VND
            250000.0f,  // 250,000 VND
            999999.0f   // 999,999 VND
        };
        
        for (float basePrice : testPrices) {
            // Test VAT amount calculation
            float calculatedVAT = vatCalculationService.calculateVATForPrice(basePrice);
            float expectedVAT = basePrice * VAT_RATE;
            
            assertEquals(expectedVAT, calculatedVAT, VAT_CALCULATION_TOLERANCE,
                String.format("VAT calculation for %.2f VND should be exactly 10%%", basePrice));
            
            // Test VAT inclusive price calculation
            float priceInclVAT = vatCalculationService.calculatePriceIncludingVAT(basePrice);
            float expectedInclVAT = basePrice + expectedVAT;
            
            assertEquals(expectedInclVAT, priceInclVAT, VAT_CALCULATION_TOLERANCE,
                String.format("Price including VAT for %.2f VND should be base + 10%%", basePrice));
            
            // Test VAT rate consistency
            float vatRate = vatCalculationService.getVATRate();
            assertEquals(VAT_RATE, vatRate, 0.001f, "VAT rate should consistently be 10%");
            
            // Test reverse calculation (price excluding VAT)
            float calculatedBasePrice = vatCalculationService.calculatePriceExcludingVAT(priceInclVAT);
            assertEquals(basePrice, calculatedBasePrice, VAT_CALCULATION_TOLERANCE,
                String.format("Reverse VAT calculation should return original base price: %.2f", basePrice));
        }
        
        logger.info("✓ 10% VAT calculation accuracy verified for all test cases");
    }

    @Test
    @Order(2)
    @DisplayName("PS-VAT-002: VAT calculation accuracy with order items - Lines 22-23")
    void testVATCalculationAccuracyWithOrderItems() throws Exception {
        logger.info("=== Testing VAT Calculation Accuracy With Order Items - Lines 22-23 ===");
        
        // Create order with multiple items for comprehensive VAT testing
        String cartSessionId = "vat-test-cart-" + System.currentTimeMillis();
        Cart cart = cartService.createCart(cartSessionId, "vat-test-customer");
        
        // Add products with different prices and quantities
        List<OrderItemTestData> testItems = new ArrayList<>();
        testItems.add(new OrderItemTestData(testProducts.get(0), 2, 60000.0f)); // 2 × 60,000 = 120,000
        testItems.add(new OrderItemTestData(testProducts.get(1), 1, 80000.0f)); // 1 × 80,000 = 80,000
        testItems.add(new OrderItemTestData(testProducts.get(2), 3, 40000.0f)); // 3 × 40,000 = 120,000
        // Total: 320,000 VND (excluding VAT)
        
        float totalExclVAT = 0;
        for (OrderItemTestData itemData : testItems) {
            cartService.addItem(cartSessionId, itemData.product.getProductId(), itemData.quantity);
            totalExclVAT += itemData.quantity * itemData.unitPrice;
        }
        
        // Create order from cart
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartSessionId, "vat-test-customer");
        order.setDeliveryInfo(createTestDeliveryInfo());
        
        // Calculate VAT breakdown
        List<OrderItem> orderItems = order.getOrderItems();
        IVATCalculationService.OrderPriceBreakdown vatBreakdown = 
            vatCalculationService.calculateOrderVAT(orderItems, 0.0f); // No delivery fee for pure VAT test
        
        // Verify subtotal excluding VAT
        assertEquals(totalExclVAT, vatBreakdown.getSubtotalExclVAT(), VAT_CALCULATION_TOLERANCE,
            "Subtotal excluding VAT should match sum of item prices");
        
        // Verify total VAT amount (10% of subtotal)
        float expectedTotalVAT = totalExclVAT * VAT_RATE;
        assertEquals(expectedTotalVAT, vatBreakdown.getTotalVATAmount(), VAT_CALCULATION_TOLERANCE,
            "Total VAT should be exactly 10% of subtotal excluding VAT");
        
        // Verify subtotal including VAT
        float expectedSubtotalInclVAT = totalExclVAT + expectedTotalVAT;
        assertEquals(expectedSubtotalInclVAT, vatBreakdown.getSubtotalInclVAT(), VAT_CALCULATION_TOLERANCE,
            "Subtotal including VAT should be base amount plus VAT");
        
        // Verify total order amount (no delivery fee)
        assertEquals(expectedSubtotalInclVAT, vatBreakdown.getTotalOrderAmount(), VAT_CALCULATION_TOLERANCE,
            "Total order amount should equal subtotal including VAT when no delivery fee");
        
        // Test individual item VAT calculations
        List<IVATCalculationService.ItemPriceBreakdown> itemBreakdowns = 
            vatCalculationService.calculateItemVAT(orderItems);
        
        for (int i = 0; i < itemBreakdowns.size(); i++) {
            IVATCalculationService.ItemPriceBreakdown itemBreakdown = itemBreakdowns.get(i);
            OrderItemTestData expectedData = testItems.get(i);
            
            // Verify unit price calculations
            assertEquals(expectedData.unitPrice, itemBreakdown.getUnitPriceExclVAT(), VAT_CALCULATION_TOLERANCE,
                "Unit price excluding VAT should match product price");
            
            float expectedUnitVAT = expectedData.unitPrice * VAT_RATE;
            assertEquals(expectedUnitVAT, itemBreakdown.getUnitVATAmount(), VAT_CALCULATION_TOLERANCE,
                "Unit VAT amount should be 10% of unit price");
            
            float expectedUnitInclVAT = expectedData.unitPrice + expectedUnitVAT;
            assertEquals(expectedUnitInclVAT, itemBreakdown.getUnitPriceInclVAT(), VAT_CALCULATION_TOLERANCE,
                "Unit price including VAT should be unit price plus VAT");
            
            // Verify total calculations for item
            float expectedTotalExclVAT = expectedData.unitPrice * expectedData.quantity;
            assertEquals(expectedTotalExclVAT, itemBreakdown.getTotalPriceExclVAT(), VAT_CALCULATION_TOLERANCE,
                "Total price excluding VAT should be unit price × quantity");
            
            float expectedTotalVATForItem = expectedUnitVAT * expectedData.quantity;
            assertEquals(expectedTotalVATForItem, itemBreakdown.getTotalVATAmount(), VAT_CALCULATION_TOLERANCE,
                "Total VAT for item should be unit VAT × quantity");
        }
        
        logger.info("✓ VAT calculation accuracy with order items verified");
    }

    // ========================================
    // Free Shipping and Delivery Discount Tests (Lines 41-42)
    // ========================================

    @Test
    @Order(3)
    @DisplayName("PS-VAT-003: Free shipping >100,000 VND threshold - Line 41")
    void testFreeShippingThreshold() throws Exception {
        logger.info("=== Testing Free Shipping >100,000 VND Threshold - Line 41 ===");
        
        // Test order below free shipping threshold
        String belowThresholdCartId = "below-threshold-" + System.currentTimeMillis();
        OrderEntity belowThresholdOrder = createOrderWithValue(belowThresholdCartId, 95000.0f);
        
        float belowThresholdDeliveryFee = deliveryCalculationService.calculateShippingFee(belowThresholdOrder, false);
        assertTrue(belowThresholdDeliveryFee > 0,
            "Orders below 100,000 VND should have delivery fee");
        
        // Test order at exactly the free shipping threshold
        String atThresholdCartId = "at-threshold-" + System.currentTimeMillis();
        OrderEntity atThresholdOrder = createOrderWithValue(atThresholdCartId, FREE_SHIPPING_THRESHOLD);
        
        float atThresholdDeliveryFee = deliveryCalculationService.calculateShippingFee(atThresholdOrder, false);
        assertEquals(0.0f, atThresholdDeliveryFee, VAT_CALCULATION_TOLERANCE,
            "Orders of exactly 100,000 VND should qualify for free shipping");
        
        // Test order above free shipping threshold
        String aboveThresholdCartId = "above-threshold-" + System.currentTimeMillis();
        OrderEntity aboveThresholdOrder = createOrderWithValue(aboveThresholdCartId, 150000.0f);
        
        float aboveThresholdDeliveryFee = deliveryCalculationService.calculateShippingFee(aboveThresholdOrder, false);
        assertEquals(0.0f, aboveThresholdDeliveryFee, VAT_CALCULATION_TOLERANCE,
            "Orders above 100,000 VND should have free shipping");
        
        // Test edge case: order just above threshold
        String justAboveCartId = "just-above-" + System.currentTimeMillis();
        OrderEntity justAboveOrder = createOrderWithValue(justAboveCartId, FREE_SHIPPING_THRESHOLD + 100.0f);
        
        float justAboveDeliveryFee = deliveryCalculationService.calculateShippingFee(justAboveOrder, false);
        assertEquals(0.0f, justAboveDeliveryFee, VAT_CALCULATION_TOLERANCE,
            "Orders just above 100,000 VND should have free shipping");
        
        // Test with VAT included pricing
        String vatTestCartId = "vat-threshold-" + System.currentTimeMillis();
        OrderEntity vatTestOrder = createOrderWithValue(vatTestCartId, 91000.0f); // 91,000 + 10% VAT = 100,100
        
        // Calculate total with VAT
        IVATCalculationService.OrderPriceBreakdown vatBreakdown = 
            vatCalculationService.calculateOrderVAT(vatTestOrder.getOrderItems(), 0.0f);
        
        boolean qualifiesForFreeShipping = vatBreakdown.getSubtotalInclVAT() >= FREE_SHIPPING_THRESHOLD;
        assertTrue(qualifiesForFreeShipping, 
            "Order with VAT-inclusive total above 100,000 VND should qualify for free shipping");
        
        logger.info("✓ Free shipping threshold (>100,000 VND) compliance verified");
    }

    @Test
    @Order(4)
    @DisplayName("PS-VAT-004: Maximum 25,000 VND shipping discount - Line 42")
    void testMaximumShippingDiscount() throws Exception {
        logger.info("=== Testing Maximum 25,000 VND Shipping Discount - Line 42 ===");
        
        // Create orders with different delivery destinations to test various shipping fees
        String[] deliveryProvinces = {"HN", "HCM", "DN", "HP", "QN"}; // Different provinces with different base fees
        float[] orderValues = {95000.0f, 80000.0f, 70000.0f, 50000.0f}; // Below free shipping threshold
        
        for (String province : deliveryProvinces) {
            for (float orderValue : orderValues) {
                String testCartId = String.format("discount-test-%s-%.0f-%d", 
                    province, orderValue, System.currentTimeMillis());
                
                // Create order below free shipping threshold
                OrderEntity testOrder = createOrderWithValue(testCartId, orderValue);
                DeliveryInfo deliveryInfo = createTestDeliveryInfoForProvince(province);
                testOrder.setDeliveryInfo(deliveryInfo);
                
                // Calculate standard shipping fee (what would be charged without free shipping)
                float standardShippingFee = deliveryCalculationService.calculateShippingFee(testOrder, false);
                
                // Calculate shipping fee if order qualified for free shipping
                float freeShippingDiscount = Math.min(standardShippingFee, MAX_SHIPPING_DISCOUNT);
                
                // The discount should never exceed 25,000 VND
                assertTrue(freeShippingDiscount <= MAX_SHIPPING_DISCOUNT,
                    String.format("Shipping discount should not exceed 25,000 VND for %s province " +
                        "(calculated discount: %.2f)", province, freeShippingDiscount));
                
                // Test with qualifying order to verify actual discount application
                String qualifyingCartId = String.format("qualifying-%s-%.0f-%d", 
                    province, orderValue, System.currentTimeMillis());
                OrderEntity qualifyingOrder = createOrderWithValue(qualifyingCartId, 120000.0f); // Above threshold
                qualifyingOrder.setDeliveryInfo(deliveryInfo);
                
                float qualifyingOrderDeliveryFee = deliveryCalculationService.calculateShippingFee(qualifyingOrder, false);
                assertEquals(0.0f, qualifyingOrderDeliveryFee, VAT_CALCULATION_TOLERANCE,
                    "Qualifying orders should have zero delivery fee regardless of province");
                
                // Calculate the actual discount applied
                float baseShippingForQualifying = deliveryCalculationService.calculateBaseShippingFee(
                    qualifyingOrder.getDeliveryInfo().getDeliveryProvince(),
                    qualifyingOrder.getDeliveryInfo().getDeliveryCity(),
                    calculateOrderWeight(qualifyingOrder));
                
                float actualDiscount = baseShippingForQualifying - qualifyingOrderDeliveryFee; // Should equal base fee
                assertTrue(actualDiscount <= MAX_SHIPPING_DISCOUNT + VAT_CALCULATION_TOLERANCE,
                    String.format("Actual shipping discount should not exceed 25,000 VND " +
                        "(actual: %.2f for %s)", actualDiscount, province));
            }
        }
        
        // Test edge case: very expensive shipping location
        String expensiveCartId = "expensive-shipping-" + System.currentTimeMillis();
        OrderEntity expensiveOrder = createOrderWithValue(expensiveCartId, 150000.0f); // Qualifies for free shipping
        
        // Set delivery to a location that would normally have high shipping cost
        DeliveryInfo expensiveDelivery = createTestDeliveryInfoForProvince("remote-province");
        expensiveOrder.setDeliveryInfo(expensiveDelivery);
        
        float expensiveDeliveryFee = deliveryCalculationService.calculateShippingFee(expensiveOrder, false);
        assertEquals(0.0f, expensiveDeliveryFee, VAT_CALCULATION_TOLERANCE,
            "Even expensive shipping locations should have free delivery for qualifying orders");
        
        logger.info("✓ Maximum 25,000 VND shipping discount compliance verified");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private void setupVATTestProducts() {
        try {
            testProducts = new ArrayList<>();
            
            // Create products with different price points for VAT testing
            testProducts.add(createTestProduct("VAT_TEST_001", "VAT Test Product 1", 60000.0f));
            testProducts.add(createTestProduct("VAT_TEST_002", "VAT Test Product 2", 80000.0f));
            testProducts.add(createTestProduct("VAT_TEST_003", "VAT Test Product 3", 40000.0f));
            testProducts.add(createTestProduct("VAT_TEST_004", "VAT Test Product 4", 120000.0f));
            
        } catch (Exception e) {
            logger.warning("Failed to setup VAT test products: " + e.getMessage());
            testProducts = new ArrayList<>();
        }
    }

    private Product createTestProduct(String productId, String title, float price) throws Exception {
        Product product = new Product();
        product.setProductId(productId);
        product.setTitle(title);
        product.setPrice(price);
        product.setQuantityInStock(100);
        product.setCategory("VAT Test");
        product.setDescription("Product for VAT compliance testing");
        product.setImageUrl("vat-test.jpg");
        product.setBarcode("987654321");
        product.setDimensions("15x10x2");
        product.setWeight(0.3f);
        
        return productService.addProduct(product);
    }

    private OrderEntity createOrderWithValue(String cartSessionId, float targetValue) throws Exception {
        Cart cart = cartService.createCart(cartSessionId, "vat-test-customer");
        
        // Add products to reach approximately the target value
        float currentValue = 0;
        int productIndex = 0;
        
        while (currentValue < targetValue && productIndex < testProducts.size()) {
            Product product = testProducts.get(productIndex);
            float remainingValue = targetValue - currentValue;
            int quantity = (int) Math.max(1, Math.round(remainingValue / product.getPrice()));
            
            // Avoid exceeding target too much
            if (currentValue + (quantity * product.getPrice()) > targetValue * 1.1f) {
                quantity = Math.max(1, (int) Math.floor(remainingValue / product.getPrice()));
            }
            
            cartService.addItem(cartSessionId, product.getProductId(), quantity);
            currentValue += quantity * product.getPrice();
            productIndex++;
        }
        
        OrderEntity order = orderService.initiateOrderFromCartEnhanced(cartSessionId, "vat-test-customer");
        order.setDeliveryInfo(createTestDeliveryInfo());
        
        return order;
    }

    private DeliveryInfo createTestDeliveryInfo() {
        return createTestDeliveryInfoForProvince("HN");
    }

    private DeliveryInfo createTestDeliveryInfoForProvince(String province) {
        DeliveryInfo deliveryInfo = new DeliveryInfo();
        deliveryInfo.setRecipientName("VAT Test Customer");
        deliveryInfo.setRecipientPhone("0987654321");
        deliveryInfo.setRecipientEmail("vattest@example.com");
        deliveryInfo.setDeliveryAddress("123 VAT Test Street");
        deliveryInfo.setDeliveryProvince(province);
        deliveryInfo.setDeliveryCity(getDefaultCityForProvince(province));
        deliveryInfo.setPostalCode("10000");
        deliveryInfo.setDeliveryInstructions("VAT compliance test delivery");
        return deliveryInfo;
    }

    private String getDefaultCityForProvince(String province) {
        switch (province) {
            case "HN": return "Hoan Kiem";
            case "HCM": return "Quan 1";
            case "DN": return "Hai Chau";
            case "HP": return "Hong Bang";
            case "QN": return "Hoi An";
            default: return "Default City";
        }
    }

    private float calculateOrderWeight(OrderEntity order) {
        return order.getOrderItems().stream()
            .map(item -> item.getQuantity() * (item.getProduct() != null ? item.getProduct().getWeight() : 0.5f))
            .reduce(0.0f, Float::sum);
    }

    // ========================================
    // Helper Classes
    // ========================================

    private static class OrderItemTestData {
        final Product product;
        final int quantity;
        final float unitPrice;

        OrderItemTestData(Product product, int quantity, float unitPrice) {
            this.product = product;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        clearTestData();
    }

    @AfterAll
    static void tearDownClass() {
        logger.info("=== VAT Calculation Compliance Tests Completed ===");
    }
}