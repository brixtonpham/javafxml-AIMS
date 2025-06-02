package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.Product;
import com.aims.core.enums.ProductType;
import com.aims.core.infrastructure.database.SQLiteConnector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class CartItemDAOImplTest {

    private static final String TEST_DB_URL_PREFIX = "jdbc:sqlite:";
    private static String testDbUrl;
    private ICartItemDAO cartItemDAO;
    private Connection connection;
    private IProductDAO mockProductDAO;
    private ICartDAO realCartDAO; // To set up Cart for FK constraints
    private IProductDAO realProductDAO; // To set up Product for FK constraints

    @TempDir
    static Path sharedTempDir;
    private static Path dbFile;

    private static int productBarcodeSuffix = 0; // Ensure unique barcodes for products

    @BeforeAll
    static void beforeAll() throws IOException, SQLException {
        dbFile = sharedTempDir.resolve("aims_cart_item_test.db");
        testDbUrl = TEST_DB_URL_PREFIX + dbFile.toAbsolutePath().toString();
        System.setProperty("TEST_DB_URL", testDbUrl);

        try (Connection conn = DriverManager.getConnection(testDbUrl)) {
            Path schemaScriptPath = Path.of("src/main/java/com/aims/core/infrastructure/database/scripts/V1__create_tables.sql");
            String schemaSql = Files.readString(schemaScriptPath);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("PRAGMA foreign_keys = ON;");
                stmt.executeUpdate(schemaSql);
            }
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(testDbUrl);
        SQLiteConnector.getInstance().setConnection(connection);

        mockProductDAO = Mockito.mock(IProductDAO.class);
        realProductDAO = new ProductDAOImpl(); // Uses the connection from SQLiteConnector
        realCartDAO = new CartDAOImpl(null, null, null); // Dependencies not strictly needed for basic cart setup

        cartItemDAO = new CartItemDAOImpl(mockProductDAO);

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM CART_ITEM;");
            stmt.executeUpdate("DELETE FROM CART;");
            stmt.executeUpdate("DELETE FROM PRODUCT;");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        SQLiteConnector.getInstance().setConnection(null);
    }

    private Cart setupTestCart(String sessionId) throws SQLException {
        Cart cart = new Cart(sessionId, null, LocalDateTime.now());
        realCartDAO.saveOrUpdate(cart);
        return cart;
    }

    private Product setupTestProduct(String productId, String title) throws SQLException {
        // Append a unique suffix to the barcode to avoid constraint violations
        String uniqueBarcode = "barcode_" + productId + "_" + (productBarcodeSuffix++);
        Product product = new Product(productId, title, "Category", 10f, 12f, 10, "Desc", "img.url", uniqueBarcode, "dim", 1f, LocalDate.now(), ProductType.BOOK);
        realProductDAO.addBaseProduct(product); // Add to DB for FK
        when(mockProductDAO.getById(productId)).thenReturn(product); // Mock for CartItemDAOImpl dependency
        return product;
    }

    @Test
    void testAddAndGetCartItem() throws SQLException {
        Cart cart = setupTestCart("session1");
        Product product = setupTestProduct("prod1", "Test Product 1");

        CartItem item = new CartItem(cart, product, 2);
        cartItemDAO.add(item);

        CartItem retrievedItem = cartItemDAO.getByIds(cart.getCartSessionId(), product.getProductId());
        assertNotNull(retrievedItem);
        assertEquals(cart.getCartSessionId(), retrievedItem.getCart().getCartSessionId());
        assertEquals(product.getProductId(), retrievedItem.getProduct().getProductId());
        assertEquals(2, retrievedItem.getQuantity());
    }

    @Test
    void testAdd_ExistingItem_UpdatesQuantity() throws SQLException {
        Cart cart = setupTestCart("session2");
        Product product = setupTestProduct("prod2", "Test Product 2");

        CartItem item1 = new CartItem(cart, product, 1);
        cartItemDAO.add(item1); // Add initially

        CartItem item2 = new CartItem(cart, product, 3); // Same item, new quantity
        cartItemDAO.add(item2); // This should update

        CartItem retrievedItem = cartItemDAO.getByIds(cart.getCartSessionId(), product.getProductId());
        assertNotNull(retrievedItem);
        assertEquals(3, retrievedItem.getQuantity(), "Quantity should be updated to the new value from the second add.");
    }


    @Test
    void testUpdateCartItem() throws SQLException {
        Cart cart = setupTestCart("session3");
        Product product = setupTestProduct("prod3", "Test Product 3");

        CartItem item = new CartItem(cart, product, 1);
        cartItemDAO.add(item);

        item.setQuantity(5);
        cartItemDAO.update(item);

        CartItem updatedItem = cartItemDAO.getByIds(cart.getCartSessionId(), product.getProductId());
        assertNotNull(updatedItem);
        assertEquals(5, updatedItem.getQuantity());
    }
    
    @Test
    void testUpdateCartItem_NotFound() throws SQLException {
        Cart cart = setupTestCart("sessionUpdateNotFound");
        Product product = setupTestProduct("prodUpdateNotFound", "Test Product Update Not Found");
        // Item not added to DAO

        CartItem itemToUpdate = new CartItem(cart, product, 1);
        // No exception is thrown by current implementation, it just does nothing if not found
        assertDoesNotThrow(() -> cartItemDAO.update(itemToUpdate));

        CartItem retrievedItem = cartItemDAO.getByIds(cart.getCartSessionId(), product.getProductId());
        assertNull(retrievedItem, "Item should not exist as it was never added.");
    }


    @Test
    void testDeleteCartItem() throws SQLException {
        Cart cart = setupTestCart("session4");
        Product product = setupTestProduct("prod4", "Test Product 4");

        CartItem item = new CartItem(cart, product, 1);
        cartItemDAO.add(item);
        assertNotNull(cartItemDAO.getByIds(cart.getCartSessionId(), product.getProductId()));

        cartItemDAO.delete(cart.getCartSessionId(), product.getProductId());
        assertNull(cartItemDAO.getByIds(cart.getCartSessionId(), product.getProductId()));
    }

    @Test
    void testGetItemsByCartSessionId() throws SQLException {
        Cart cart1 = setupTestCart("session5_cart1");
        Product product1 = setupTestProduct("prod5_1", "Product 5.1");
        Product product2 = setupTestProduct("prod5_2", "Product 5.2");

        Cart cart2 = setupTestCart("session5_cart2"); // Different cart
        Product product3 = setupTestProduct("prod5_3", "Product 5.3");


        CartItem item1_1 = new CartItem(cart1, product1, 1);
        CartItem item1_2 = new CartItem(cart1, product2, 2);
        cartItemDAO.add(item1_1);
        cartItemDAO.add(item1_2);

        CartItem item2_1 = new CartItem(cart2, product3, 3); // Item for a different cart
        cartItemDAO.add(item2_1);


        List<CartItem> itemsForCart1 = cartItemDAO.getItemsByCartSessionId(cart1.getCartSessionId());
        assertNotNull(itemsForCart1);
        assertEquals(2, itemsForCart1.size());
        assertTrue(itemsForCart1.stream().anyMatch(it -> it.getProduct().getProductId().equals(product1.getProductId()) && it.getQuantity() == 1));
        assertTrue(itemsForCart1.stream().anyMatch(it -> it.getProduct().getProductId().equals(product2.getProductId()) && it.getQuantity() == 2));

        List<CartItem> itemsForCart2 = cartItemDAO.getItemsByCartSessionId(cart2.getCartSessionId());
        assertNotNull(itemsForCart2);
        assertEquals(1, itemsForCart2.size());
        assertEquals(product3.getProductId(), itemsForCart2.get(0).getProduct().getProductId());
    }
    
    @Test
    void testGetItemsByCartSessionId_NoItems() throws SQLException {
        Cart cart = setupTestCart("sessionNoItems");
        List<CartItem> items = cartItemDAO.getItemsByCartSessionId(cart.getCartSessionId());
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    void testDeleteByCartSessionId() throws SQLException {
        Cart cart = setupTestCart("session6");
        Product product1 = setupTestProduct("prod6_1", "Product 6.1");
        Product product2 = setupTestProduct("prod6_2", "Product 6.2");

        cartItemDAO.add(new CartItem(cart, product1, 1));
        cartItemDAO.add(new CartItem(cart, product2, 3));

        assertEquals(2, cartItemDAO.getItemsByCartSessionId(cart.getCartSessionId()).size());

        cartItemDAO.deleteByCartSessionId(cart.getCartSessionId());
        assertTrue(cartItemDAO.getItemsByCartSessionId(cart.getCartSessionId()).isEmpty());
    }
    
    @Test
    void testGetByIds_NotFound() throws SQLException {
        setupTestCart("sessionGetNotFound"); // Ensure a cart exists
        CartItem retrievedItem = cartItemDAO.getByIds("sessionGetNotFound", "nonExistentProd");
        assertNull(retrievedItem);
    }
}
