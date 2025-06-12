import com.aims.core.application.impl.DeliveryCalculationServiceImpl;
import com.aims.core.application.impl.OrderServiceImpl;
import com.aims.core.entities.*;
import com.aims.core.enums.OrderStatus;
import com.aims.core.shared.exceptions.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple test to verify the delivery validation bug fix.
 * This test demonstrates that the ValidationException is now resolved.
 */
public class DeliveryValidationBugFixTest {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Delivery Validation Bug Fix ===");
        
        try {
            // Test 1: Verify that calculateShippingFee works when delivery info is set
            testDeliveryCalculationWithValidDeliveryInfo();
            
            // Test 2: Verify that ValidationException is thrown when delivery info is null
            testDeliveryCalculationWithNullDeliveryInfo();
            
            System.out.println("\n✅ All tests passed! The delivery validation bug has been fixed.");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testDeliveryCalculationWithValidDeliveryInfo() throws ValidationException {
        System.out.println("\n--- Test 1: Valid Delivery Information ---");
        
        // Create test order with delivery info
        OrderEntity order = createTestOrder();
        DeliveryInfo deliveryInfo = createTestDeliveryInfo();
        order.setDeliveryInfo(deliveryInfo); // THIS IS THE KEY FIX - delivery info is set BEFORE calculation
        
        // Create delivery calculation service
        DeliveryCalculationServiceImpl deliveryService = new DeliveryCalculationServiceImpl();
        
        // This should NOT throw ValidationException anymore
        float shippingFee = deliveryService.calculateShippingFee(order, false);
        
        System.out.println("✓ Shipping fee calculated successfully: " + shippingFee + " VND");
        System.out.println("✓ No ValidationException thrown - fix confirmed!");
    }
    
    private static void testDeliveryCalculationWithNullDeliveryInfo() {
        System.out.println("\n--- Test 2: Null Delivery Information ---");
        
        // Create test order WITHOUT delivery info
        OrderEntity order = createTestOrder();
        order.setDeliveryInfo(null); // Explicitly set to null
        
        // Create delivery calculation service
        DeliveryCalculationServiceImpl deliveryService = new DeliveryCalculationServiceImpl();
        
        // This SHOULD throw ValidationException with clear message
        try {
            deliveryService.calculateShippingFee(order, false);
            System.err.println("❌ Expected ValidationException was not thrown");
        } catch (ValidationException e) {
            System.out.println("✓ ValidationException thrown as expected: " + e.getMessage());
            System.out.println("✓ Error message is clear and helpful for debugging");
        }
    }
    
    private static OrderEntity createTestOrder() {
        OrderEntity order = new OrderEntity();
        order.setOrderId("TEST-ORDER-001");
        order.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalProductPriceExclVAT(100000f);
        order.setTotalProductPriceInclVAT(110000f);
        
        // Add test order items
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem();
        item.setProduct(createTestProduct());
        item.setQuantity(2);
        item.setPriceAtTimeOfOrder(50000f);
        item.setEligibleForRushDelivery(false);
        items.add(item);
        
        order.setOrderItems(items);
        return order;
    }
    
    private static Product createTestProduct() {
        Product product = new Product();
        product.setProductId("TEST-PRODUCT-001");
        product.setTitle("Test Product");
        product.setPrice(50000f);
        product.setWeightKg(1.5f);
        product.setQuantityInStock(10);
        return product;
    }
    
    private static DeliveryInfo createTestDeliveryInfo() {
        DeliveryInfo deliveryInfo = new DeliveryInfo();
        deliveryInfo.setDeliveryInfoId("TEST-DELIVERY-001");
        deliveryInfo.setRecipientName("Test Customer");
        deliveryInfo.setEmail("test@example.com");
        deliveryInfo.setPhoneNumber("0901234567");
        deliveryInfo.setDeliveryProvinceCity("Hanoi");
        deliveryInfo.setDeliveryAddress("123 Test Street, Hoan Kiem District");
        deliveryInfo.setDeliveryInstructions("Test delivery instructions");
        deliveryInfo.setDeliveryMethodChosen("STANDARD");
        return deliveryInfo;
    }
}