package com.aims.core.application.impl;

import com.aims.core.entities.*;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.ProductType;
import com.aims.core.infrastructure.database.dao.*;
import com.aims.core.application.services.*;
import com.aims.core.shared.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for enhanced cart-to-order conversion functionality
 */
class EnhancedCartToOrderConversionTest {

    @Mock private IOrderEntityDAO orderDAO;
    @Mock private IOrderItemDAO orderItemDAO;
    @Mock private IDeliveryInfoDAO deliveryInfoDAO;
    @Mock private IInvoiceDAO invoiceDAO;
    @Mock private IProductDAO productDAO;
    @Mock private IProductService productService;
    @Mock private ICartService cartService;
    @Mock private IPaymentService paymentService;
    @Mock private IDeliveryCalculationService deliveryCalculationService;
    @Mock private INotificationService notificationService;
    @Mock private IUserAccountDAO userAccountDAO;
    @Mock private IOrderDataLoaderService orderDataLoaderService;

    private OrderServiceImpl orderService;
    private CartServiceImpl cartServiceImpl;
    private Cart testCart;
    private Product testProduct1;
    private Product testProduct2;
    private UserAccount testUser;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        
        // Initialize order service
        orderService = new OrderServiceImpl(
            orderDAO, orderItemDAO, deliveryInfoDAO, invoiceDAO, productDAO,
            productService, cartService, paymentService, deliveryCalculationService,
            notificationService, userAccountDAO, orderDataLoaderService
        );

        // Create test user
        testUser = new UserAccount();
        testUser.setUserId("user123");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Create test products with complete metadata
        testProduct1 = new Product();
        testProduct1.setProductId("BOOK001");
        testProduct1.setTitle("Clean Code");
        testProduct1.setCategory("Programming");
        testProduct1.setPrice(299.0f);
        testProduct1.setQuantityInStock(50);
        testProduct1.setDescription("A comprehensive guide to writing clean, maintainable code");
        testProduct1.setImageUrl("/images/books/clean_code.jpg");
        testProduct1.setBarcode("978-0132350884");
        testProduct1.setDimensionsCm("23x18x3");
        testProduct1.setWeightKg(0.8f);
        testProduct1.setEntryDate(LocalDate.now());
        testProduct1.setProductType(ProductType.BOOK);

        testProduct2 = new Product();
        testProduct2.setProductId("CD001");
        testProduct2.setTitle("Abbey Road");
        testProduct2.setCategory("Rock");
        testProduct2.setPrice(199.0f);
        testProduct2.setQuantityInStock(25);
        testProduct2.setDescription("Classic album by The Beatles");
        testProduct2.setImageUrl("/images/cds/abbey_road.jpg");
        testProduct2.setBarcode("094638241928");
        testProduct2.setDimensionsCm("14x12x1");
        testProduct2.setWeightKg(0.1f);
        testProduct2.setEntryDate(LocalDate.now());
        testProduct2.setProductType(ProductType.CD);

        // Create test cart with items
        testCart = new Cart("cart-session-123", testUser, LocalDateTime.now());
        List<CartItem> cartItems = new ArrayList<>();
        
        CartItem item1 = new CartItem(testCart, testProduct1, 2);
        CartItem item2 = new CartItem(testCart, testProduct2, 1);
        
        cartItems.add(item1);
        cartItems.add(item2);
        testCart.setItems(cartItems);

        // Mock cart service to return complete cart data
        when(cartService.getCart("cart-session-123")).thenReturn(testCart);
        
        // Mock product DAO to return complete product data
        when(productDAO.getById("BOOK001")).thenReturn(testProduct1);
        when(productDAO.getById("CD001")).thenReturn(testProduct2);
        
        // Mock user DAO
        when(userAccountDAO.getById("user123")).thenReturn(testUser);
        
        // Mock successful persistence operations
        doNothing().when(orderDAO).add(any(OrderEntity.class));
        doNothing().when(orderItemDAO).add(any(OrderItem.class));
    }

    @Test
    void testEnhancedCartToOrderConversion_SuccessfulConversion() throws ValidationException, SQLException {
        // Act
        OrderEntity result = orderService.initiateOrderFromCartEnhanced("cart-session-123", "user123");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getOrderId());
        assertTrue(result.getOrderId().startsWith("ORD-"));
        assertEquals(OrderStatus.PENDING_DELIVERY_INFO, result.getOrderStatus());
        assertEquals(testUser, result.getUserAccount());
        assertNotNull(result.getOrderItems());
        assertEquals(2, result.getOrderItems().size());

        // Verify pricing calculations
        float expectedTotalExclVAT = (299.0f * 2) + (199.0f * 1); // 797.0f
        float expectedTotalInclVAT = expectedTotalExclVAT * 1.10f; // 876.7f
        assertEquals(expectedTotalExclVAT, result.getTotalProductPriceExclVAT(), 0.01f);
        assertEquals(expectedTotalInclVAT, result.getTotalProductPriceInclVAT(), 0.01f);

        // Verify order items have complete product data
        OrderItem bookItem = result.getOrderItems().stream()
            .filter(item -> item.getProduct().getProductId().equals("BOOK001"))
            .findFirst().orElse(null);
        assertNotNull(bookItem);
        assertEquals(2, bookItem.getQuantity());
        assertEquals(299.0f, bookItem.getPriceAtTimeOfOrder(), 0.01f);
        assertFalse(bookItem.isEligibleForRushDelivery()); // Books not eligible for rush

        OrderItem cdItem = result.getOrderItems().stream()
            .filter(item -> item.getProduct().getProductId().equals("CD001"))
            .findFirst().orElse(null);
        assertNotNull(cdItem);
        assertEquals(1, cdItem.getQuantity());
        assertEquals(199.0f, cdItem.getPriceAtTimeOfOrder(), 0.01f);
        assertTrue(cdItem.isEligibleForRushDelivery()); // CDs eligible for rush

        // Verify database operations
        verify(orderDAO, times(1)).add(any(OrderEntity.class));
        verify(orderItemDAO, times(2)).add(any(OrderItem.class));
        verify(productDAO, times(2)).getById(anyString());
    }

    @Test
    void testEnhancedCartToOrderConversion_NullCartSessionId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderService.initiateOrderFromCartEnhanced(null, "user123");
        });
        
        assertTrue(exception.getMessage().contains("Invalid cart session ID"));
    }

    @Test
    void testEnhancedCartToOrderConversion_EmptyCartSessionId() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderService.initiateOrderFromCartEnhanced("", "user123");
        });
        
        assertTrue(exception.getMessage().contains("Invalid cart session ID"));
    }

    @Test
    void testEnhancedCartToOrderConversion_CartNotFound() throws SQLException {
        // Arrange
        when(cartService.getCart("nonexistent-cart")).thenReturn(null);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderService.initiateOrderFromCartEnhanced("nonexistent-cart", "user123");
        });
        
        assertTrue(exception.getMessage().contains("Enhanced order creation failed"));
    }

    @Test
    void testEnhancedCartToOrderConversion_EmptyCart() throws SQLException {
        // Arrange
        Cart emptyCart = new Cart("empty-cart", testUser, LocalDateTime.now());
        emptyCart.setItems(new ArrayList<>());
        when(cartService.getCart("empty-cart")).thenReturn(emptyCart);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderService.initiateOrderFromCartEnhanced("empty-cart", "user123");
        });
        
        assertTrue(exception.getMessage().contains("Enhanced order creation failed"));
    }

    @Test
    void testEnhancedCartToOrderConversion_ProductNotFound() throws SQLException {
        // Arrange
        when(productDAO.getById("BOOK001")).thenReturn(null); // Product not found

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderService.initiateOrderFromCartEnhanced("cart-session-123", "user123");
        });
        
        assertTrue(exception.getMessage().contains("Enhanced order creation failed"));
    }

    @Test
    void testEnhancedCartToOrderConversion_InsufficientStock() throws SQLException {
        // Arrange
        testProduct1.setQuantityInStock(1); // Less than requested quantity of 2
        when(productDAO.getById("BOOK001")).thenReturn(testProduct1);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderService.initiateOrderFromCartEnhanced("cart-session-123", "user123");
        });
        
        assertTrue(exception.getMessage().contains("Enhanced order creation failed"));
    }

    @Test
    void testEnhancedCartToOrderConversion_UserNotFound() throws SQLException {
        // Arrange
        when(userAccountDAO.getById("nonexistent-user")).thenReturn(null);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderService.initiateOrderFromCartEnhanced("cart-session-123", "nonexistent-user");
        });
        
        assertTrue(exception.getMessage().contains("Enhanced order creation failed"));
    }

    @Test
    void testEnhancedCartToOrderConversion_GuestUser() throws ValidationException, SQLException {
        // Act - Guest checkout (null userId)
        OrderEntity result = orderService.initiateOrderFromCartEnhanced("cart-session-123", null);

        // Assert
        assertNotNull(result);
        assertNull(result.getUserAccount()); // Guest user should be null
        assertEquals(OrderStatus.PENDING_DELIVERY_INFO, result.getOrderStatus());
        assertNotNull(result.getOrderItems());
        assertEquals(2, result.getOrderItems().size());

        // Verify database operations
        verify(orderDAO, times(1)).add(any(OrderEntity.class));
        verify(orderItemDAO, times(2)).add(any(OrderItem.class));
        verify(userAccountDAO, never()).getById(anyString()); // No user lookup for guest
    }

    @Test
    void testEnhancedCartToOrderConversion_DatabaseError() throws SQLException {
        // Arrange
        doThrow(new SQLException("Database connection failed")).when(orderDAO).add(any(OrderEntity.class));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            orderService.initiateOrderFromCartEnhanced("cart-session-123", "user123");
        });
        
        assertTrue(exception.getMessage().contains("Enhanced order creation failed"));
    }

    @Test
    void testEnhancedCartToOrderConversion_RushEligibilityDetermination() throws ValidationException, SQLException {
        // Arrange - Create products with different rush eligibility
        Product heavyProduct = new Product();
        heavyProduct.setProductId("HEAVY001");
        heavyProduct.setTitle("Heavy Equipment Manual");
        heavyProduct.setPrice(500.0f);
        heavyProduct.setQuantityInStock(5);
        heavyProduct.setWeightKg(15.0f); // Over 10kg - not eligible for rush
        heavyProduct.setProductType(ProductType.DVD);
        heavyProduct.setDescription("Heavy manual");
        heavyProduct.setImageUrl("/images/manual.jpg");
        heavyProduct.setDimensionsCm("30x25x5");

        CartItem heavyItem = new CartItem(testCart, heavyProduct, 1);
        testCart.getItems().add(heavyItem);

        when(productDAO.getById("HEAVY001")).thenReturn(heavyProduct);

        // Act
        OrderEntity result = orderService.initiateOrderFromCartEnhanced("cart-session-123", "user123");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getOrderItems().size());

        // Verify rush eligibility
        OrderItem heavyOrderItem = result.getOrderItems().stream()
            .filter(item -> item.getProduct().getProductId().equals("HEAVY001"))
            .findFirst().orElse(null);
        assertNotNull(heavyOrderItem);
        assertFalse(heavyOrderItem.isEligibleForRushDelivery()); // Heavy items not eligible
    }

    @Test
    void testEnhancedCartToOrderConversion_CompleteProductMetadataPreserved() throws ValidationException, SQLException {
        // Act
        OrderEntity result = orderService.initiateOrderFromCartEnhanced("cart-session-123", "user123");

        // Assert - Verify all product metadata is preserved
        OrderItem bookItem = result.getOrderItems().stream()
            .filter(item -> item.getProduct().getProductId().equals("BOOK001"))
            .findFirst().orElse(null);
        assertNotNull(bookItem);
        
        Product preservedProduct = bookItem.getProduct();
        assertEquals("Clean Code", preservedProduct.getTitle());
        assertEquals("Programming", preservedProduct.getCategory());
        assertEquals("A comprehensive guide to writing clean, maintainable code", preservedProduct.getDescription());
        assertEquals("/images/books/clean_code.jpg", preservedProduct.getImageUrl());
        assertEquals("978-0132350884", preservedProduct.getBarcode());
        assertEquals("23x18x3", preservedProduct.getDimensionsCm());
        assertEquals(0.8f, preservedProduct.getWeightKg(), 0.01f);
        assertEquals(ProductType.BOOK, preservedProduct.getProductType());
    }
}