package com.aims.test.data;

import com.aims.core.entities.*;
import com.aims.core.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test Data Factory for Cart-to-Order Integration Testing
 * 
 * Provides comprehensive test data creation methods for various
 * cart-to-order testing scenarios including different product types,
 * delivery options, and error conditions.
 */
public class CartToOrderTestDataFactory {
    
    /**
     * Creates test cart with various product types (Books, CDs, DVDs, LPs)
     */
    public static Cart createTestCartWithAllProductTypes(String sessionId) {
        Cart cart = new Cart(sessionId, null, LocalDateTime.now());
        List<CartItem> items = new ArrayList<>();
        
        // Add Book
        Book book = createTestBook("book_001", "Clean Code", 45000f);
        CartItem bookItem = new CartItem(cart, book, 2);
        items.add(bookItem);
        
        // Add CD
        CD cd = createTestCD("cd_001", "Abbey Road", 35000f);
        CartItem cdItem = new CartItem(cart, cd, 1);
        items.add(cdItem);
        
        // Add DVD
        DVD dvd = createTestDVD("dvd_001", "The Matrix", 25000f);
        CartItem dvdItem = new CartItem(cart, dvd, 1);
        items.add(dvdItem);
        
        // Add LP
        LP lp = createTestLP("lp_001", "Dark Side of the Moon", 55000f);
        CartItem lpItem = new CartItem(cart, lp, 1);
        items.add(lpItem);
        
        cart.setItems(items);
        return cart;
    }
    
    /**
     * Creates large test cart for performance testing
     */
    public static Cart createLargeTestCart(String sessionId, int itemCount) {
        Cart cart = new Cart(sessionId, null, LocalDateTime.now());
        List<CartItem> items = new ArrayList<>();
        
        for (int i = 1; i <= itemCount; i++) {
            Product product = createTestProduct("test_product_" + i, "Test Product " + i, 20000f + (i * 1000));
            CartItem item = new CartItem(cart, product, 1);
            items.add(item);
        }
        
        cart.setItems(items);
        return cart;
    }
    
    /**
     * Creates test cart with rush delivery eligible items
     */
    public static Cart createRushDeliveryEligibleCart(String sessionId) {
        Cart cart = new Cart(sessionId, null, LocalDateTime.now());
        List<CartItem> items = new ArrayList<>();
        
        // Add rush eligible book
        Book rushBook = createTestBook("book_rush_001", "Programming Pearls", 40000f);
        rushBook.setWeightKg(0.3f); // Light enough for rush delivery
        CartItem bookItem = new CartItem(cart, rushBook, 1);
        items.add(bookItem);
        
        // Add rush eligible CD
        CD rushCD = createTestCD("cd_rush_001", "Rush Greatest Hits", 30000f);
        rushCD.setWeightKg(0.1f); // Light enough for rush delivery
        CartItem cdItem = new CartItem(cart, rushCD, 1);
        items.add(cdItem);
        
        cart.setItems(items);
        return cart;
    }
    
    /**
     * Creates test order entity with complete data
     */
    public static OrderEntity createCompleteTestOrder(String orderId) {
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId != null ? orderId : generateOrderId());
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING_DELIVERY_INFO);
        order.setTotalProductPriceExclVAT(150000f);
        order.setTotalProductPriceInclVAT(165000f);
        order.setOrderItems(createTestOrderItems(order));
        
        return order;
    }
    
    /**
     * Creates test order with missing data for validation testing
     */
    public static OrderEntity createIncompleteTestOrder(String orderId, List<String> missingComponents) {
        OrderEntity order = createCompleteTestOrder(orderId);
        
        // Remove components based on missing list
        for (String missing : missingComponents) {
            switch (missing.toLowerCase()) {
                case "orderitems":
                    order.setOrderItems(null);
                    break;
                case "deliveryinfo":
                    order.setDeliveryInfo(null);
                    break;
                case "orderdate":
                    order.setOrderDate(null);
                    break;
                case "pricing":
                    order.setTotalProductPriceExclVAT(0f);
                    order.setTotalProductPriceInclVAT(0f);
                    break;
            }
        }
        
        return order;
    }
    
    /**
     * Creates test delivery info for various scenarios
     */
    public static DeliveryInfo createTestDeliveryInfo(String scenario) {
        DeliveryInfo deliveryInfo = new DeliveryInfo();
        
        switch (scenario.toUpperCase()) {
            case "STANDARD_DELIVERY":
                deliveryInfo.setRecipientName("John Doe");
                deliveryInfo.setDeliveryAddress("123 Test Street, Test City, Test Province");
                deliveryInfo.setRecipientPhone("0123456789");
                deliveryInfo.setDeliveryInstructions("Standard delivery instructions");
                deliveryInfo.setRushDelivery(false);
                break;
                
            case "RUSH_DELIVERY":
                deliveryInfo.setRecipientName("Jane Smith");
                deliveryInfo.setDeliveryAddress("456 Rush Avenue, Rush City, Rush Province");
                deliveryInfo.setRecipientPhone("0987654321");
                deliveryInfo.setDeliveryInstructions("Rush delivery - please deliver quickly");
                deliveryInfo.setRushDelivery(true);
                break;
                
            case "INVALID_DELIVERY":
                deliveryInfo.setRecipientName(""); // Invalid empty name
                deliveryInfo.setDeliveryAddress("");
                deliveryInfo.setRecipientPhone("");
                break;
                
            default:
                deliveryInfo.setRecipientName("Default User");
                deliveryInfo.setDeliveryAddress("Default Address");
                deliveryInfo.setRecipientPhone("0000000000");
                break;
        }
        
        return deliveryInfo;
    }
    
    /**
     * Creates test data for error scenarios
     */
    public static TestErrorScenario createErrorScenario(String errorType) {
        TestErrorScenario scenario = new TestErrorScenario();
        scenario.setErrorType(errorType);
        
        switch (errorType.toUpperCase()) {
            case "OUT_OF_STOCK":
                scenario.setDescription("Product out of stock");
                scenario.setExpectedException("InventoryException");
                scenario.setTestData(createOutOfStockProduct());
                break;
                
            case "INVALID_CART":
                scenario.setDescription("Invalid cart session");
                scenario.setExpectedException("ResourceNotFoundException");
                scenario.setTestData("non-existent-cart-session");
                break;
                
            case "VALIDATION_ERROR":
                scenario.setDescription("Data validation failure");
                scenario.setExpectedException("ValidationException");
                scenario.setTestData(createInvalidDeliveryInfo());
                break;
                
            default:
                scenario.setDescription("Generic error scenario");
                scenario.setExpectedException("Exception");
                break;
        }
        
        return scenario;
    }
    
    // Private helper methods
    
    private static Book createTestBook(String productId, String title, float price) {
        Book book = new Book();
        book.setProductId(productId);
        book.setTitle(title);
        book.setPrice(price);
        book.setCategory("Book");
        book.setQuantityInStock(50);
        book.setWeightKg(0.5f);
        book.setImageUrl("/images/books/" + productId + ".jpg");
        book.setAuthors("Test Author");
        book.setCoverType("Paperback");
        book.setPublisher("Test Publisher");
        book.setPublicationDate(java.time.LocalDate.parse("2023-01-01"));
        book.setLanguage("English");
        book.setBookGenre("Programming");
        return book;
    }
    
    private static CD createTestCD(String productId, String title, float price) {
        CD cd = new CD();
        cd.setProductId(productId);
        cd.setTitle(title);
        cd.setPrice(price);
        cd.setCategory("CD");
        cd.setQuantityInStock(30);
        cd.setWeightKg(0.2f);
        cd.setImageUrl("/images/cds/" + productId + ".jpg");
        cd.setArtists("Test Artist");
        cd.setRecordLabel("Test Records");
        cd.setCdGenre("Rock");
        cd.setTracklist("Track 1, Track 2, Track 3");
        cd.setReleaseDate(java.time.LocalDate.parse("2023-01-01"));
        return cd;
    }
    
    private static DVD createTestDVD(String productId, String title, float price) {
        DVD dvd = new DVD();
        dvd.setProductId(productId);
        dvd.setTitle(title);
        dvd.setPrice(price);
        dvd.setCategory("DVD");
        dvd.setQuantityInStock(20);
        dvd.setWeightKg(0.15f);
        dvd.setImageUrl("/images/dvds/" + productId + ".jpg");
        dvd.setDirector("Test Director");
        dvd.setRuntimeMinutes(120);
        dvd.setStudio("Test Studio");
        dvd.setSubtitles("English");
        dvd.setDvdReleaseDate(java.time.LocalDate.parse("2023-01-01"));
        dvd.setDvdGenre("Action");
        return dvd;
    }
    
    private static LP createTestLP(String productId, String title, float price) {
        LP lp = new LP();
        lp.setProductId(productId);
        lp.setTitle(title);
        lp.setPrice(price);
        lp.setCategory("LP");
        lp.setQuantityInStock(15);
        lp.setWeightKg(0.3f);
        lp.setImageUrl("/images/lps/" + productId + ".jpg");
        lp.setArtists("Test LP Artist");
        lp.setRecordLabel("Test LP Records");
        lp.setGenre("Progressive Rock");
        lp.setTracklist("Side A: Track 1, Track 2; Side B: Track 3, Track 4");
        lp.setReleaseDate(java.time.LocalDate.parse("2023-01-01"));
        return lp;
    }
    
    private static Product createTestProduct(String productId, String title, float price) {
        Product product = new Product();
        product.setProductId(productId);
        product.setTitle(title);
        product.setPrice(price);
        product.setCategory("General");
        product.setQuantityInStock(100);
        product.setWeightKg(0.5f);
        product.setImageUrl("/images/products/" + productId + ".jpg");
        return product;
    }
    
    private static List<OrderItem> createTestOrderItems(OrderEntity order) {
        List<OrderItem> items = new ArrayList<>();
        
        Product product1 = createTestProduct("order_product_1", "Order Test Product 1", 50000f);
        OrderItem item1 = new OrderItem(order, product1, 2, 50000f, true);
        items.add(item1);
        
        Product product2 = createTestProduct("order_product_2", "Order Test Product 2", 25000f);
        OrderItem item2 = new OrderItem(order, product2, 1, 25000f, false);
        items.add(item2);
        
        return items;
    }
    
    private static String generateOrderId() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private static Product createOutOfStockProduct() {
        Product product = createTestProduct("out_of_stock_001", "Out of Stock Product", 30000f);
        product.setQuantityInStock(0);
        return product;
    }
    
    private static DeliveryInfo createInvalidDeliveryInfo() {
        DeliveryInfo deliveryInfo = new DeliveryInfo();
        deliveryInfo.setRecipientName(""); // Invalid empty name
        deliveryInfo.setDeliveryAddress(""); // Invalid empty address
        deliveryInfo.setRecipientPhone(""); // Invalid empty phone
        return deliveryInfo;
    }
    
    /**
     * Helper class for error scenario testing
     */
    public static class TestErrorScenario {
        private String errorType;
        private String description;
        private String expectedException;
        private Object testData;
        
        // Getters and setters
        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getExpectedException() { return expectedException; }
        public void setExpectedException(String expectedException) { this.expectedException = expectedException; }
        
        public Object getTestData() { return testData; }
        public void setTestData(Object testData) { this.testData = testData; }
    }
}