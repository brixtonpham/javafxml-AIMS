package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Cart;
import com.aims.core.entities.CartItem;
import com.aims.core.entities.Product;
import com.aims.core.entities.UserAccount;
import com.aims.core.enums.ProductType;
import com.aims.core.enums.UserStatus;
import com.aims.core.infrastructure.database.SQLiteConnector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CartDAOImplTest {

    private static final String TEST_DB_URL_PREFIX = "jdbc:sqlite:";
    private static String testDbUrl;
    private ICartDAO cartDAO;
    private Connection connection;

    // Mocks for dependent DAOs
    private ICartItemDAO mockCartItemDAO;
    private IProductDAO mockProductDAO;
    private IUserAccountDAO mockUserAccountDAO;
    private IUserAccountDAO realUserAccountDAO; // For creating actual users
    private IProductDAO realProductDAO; // For creating actual products


    @TempDir
    static Path sharedTempDir;
    private static Path dbFile;

    @BeforeAll
    static void beforeAll() throws IOException, SQLException {
        dbFile = sharedTempDir.resolve("aims_cart_test.db");
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
        // Set the connection for the SQLiteConnector singleton ONCE.
        SQLiteConnector.getInstance().setConnection(connection); 

        // Initialize mocks
        mockCartItemDAO = mock(ICartItemDAO.class);
        mockProductDAO = mock(IProductDAO.class);
        mockUserAccountDAO = mock(IUserAccountDAO.class);
        
        // Real DAOs for test data setup - they will use the connection set in SQLiteConnector
        realUserAccountDAO = new UserAccountDAOImpl(); 
        realProductDAO = new ProductDAOImpl();

        // CartDAO uses mocked dependencies
        cartDAO = new CartDAOImpl(mockCartItemDAO, mockProductDAO, mockUserAccountDAO);

        // Clean relevant tables before each test using the established connection
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM CART_ITEM;");
            stmt.executeUpdate("DELETE FROM CART;");
            stmt.executeUpdate("DELETE FROM PRODUCT;");
            stmt.executeUpdate("DELETE FROM USER_ACCOUNT;");
        } catch (SQLException e) {
            // If connection is closed here, it means SQLiteConnector might have closed it.
            // Re-establish for cleanup if necessary, though ideally it should remain open.
            if (connection.isClosed()) {
                connection = DriverManager.getConnection(testDbUrl);
                SQLiteConnector.getInstance().setConnection(connection); // Reset for DAOs if they were affected
            }
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM CART_ITEM;");
                stmt.executeUpdate("DELETE FROM CART;");
                stmt.executeUpdate("DELETE FROM PRODUCT;");
                stmt.executeUpdate("DELETE FROM USER_ACCOUNT;");
            } // Rethrow or handle if cleanup still fails
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Close the connection used by the test method itself
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        // Clear the connection from the singleton to avoid interference between tests
        SQLiteConnector.getInstance().setConnection(null);
    }

    private UserAccount setupTestUser(String userId, String username) throws SQLException {
        UserAccount user = new UserAccount(userId, username, "testpass", username + "@example.com", UserStatus.ACTIVE);
        // Save the user to the database using the real DAO for FK integrity
        realUserAccountDAO.add(user);
        when(mockUserAccountDAO.getById(userId)).thenReturn(user); // Mock the call for CartDAOImpl dependency
        return user;
    }
    
    private Product setupTestProduct(String productId, String title) throws SQLException {
        Product product = new Product(productId, title, "Category", 10f, 12f, 10, "Desc", "img.url", "barcode", "dim", 1f, LocalDate.now(), ProductType.BOOK);
        // Save the product to the database using the real DAO for FK integrity if CartItemDAO needs it
        // For current CartDAOImpl tests, this might not be strictly necessary if CartItemDAO is fully mocked
        // and doesn't interact with the DB for products.
        realProductDAO.addBaseProduct(product); 
        when(mockProductDAO.getById(productId)).thenReturn(product);
        return product;
    }

    @Test
    void testSaveAndGetCartBySessionId_GuestCart() throws SQLException {
        String sessionId = "guestSession123";
        Cart cart = new Cart(sessionId, null, LocalDateTime.now());
        cartDAO.saveOrUpdate(cart);

        when(mockCartItemDAO.getItemsByCartSessionId(sessionId)).thenReturn(new ArrayList<>()); // No items for now

        Cart retrievedCart = cartDAO.getBySessionId(sessionId);
        assertNotNull(retrievedCart);
        assertEquals(sessionId, retrievedCart.getCartSessionId());
        assertNull(retrievedCart.getUserAccount());
        assertTrue(retrievedCart.getItems().isEmpty());
        verify(mockCartItemDAO).getItemsByCartSessionId(sessionId);
    }

    @Test
    void testSaveAndGetCartByUserId_UserCart() throws SQLException {
        String userId = "U_CartTest1";
        UserAccount user = setupTestUser(userId, "cartUser1");
        String sessionId = "userSession456";
        Cart cart = new Cart(sessionId, user, LocalDateTime.now());
        cartDAO.saveOrUpdate(cart);

        when(mockCartItemDAO.getItemsByCartSessionId(sessionId)).thenReturn(new ArrayList<>());

        Cart retrievedCart = cartDAO.getByUserId(userId);
        assertNotNull(retrievedCart, "Cart should be found by user ID");
        assertEquals(sessionId, retrievedCart.getCartSessionId());
        assertNotNull(retrievedCart.getUserAccount());
        assertEquals(userId, retrievedCart.getUserAccount().getUserId());
        
        // Verify that userAccountDAO.getById was called during mapResultSetToCart
        verify(mockUserAccountDAO).getById(userId);
    }

    @Test
    void testUpdateCart_AssociateUser() throws SQLException {
        String sessionId = "updateSession789";
        Cart cart = new Cart(sessionId, null, LocalDateTime.now()); // Initially guest cart
        // Ensure user is in DB for FK constraints if cartDAO.saveOrUpdate needs to validate it.
        UserAccount user = setupTestUser("U_CartAssociate", "cartAssociateUser");
        cartDAO.saveOrUpdate(cart); // Save guest cart first

        // Associate user
        cart.setUserAccount(user);
        cart.setLastUpdated(LocalDateTime.now().plusHours(1));
        cartDAO.saveOrUpdate(cart); // Update cart with user

        when(mockCartItemDAO.getItemsByCartSessionId(sessionId)).thenReturn(new ArrayList<>());

        Cart updatedCart = cartDAO.getBySessionId(sessionId);
        assertNotNull(updatedCart.getUserAccount());
        assertEquals(user.getUserId(), updatedCart.getUserAccount().getUserId());
        
        // Verification: 
        // 1. setupTestUser calls mockUserAccountDAO.getById(userId) once.
        // 2. cartDAO.getBySessionId calls mapResultSetToCart, which calls mockUserAccountDAO.getById(userId) once.
        // Total should be 2 calls if the user ID is the same.
        // If the test setup calls it, and then the retrieval calls it.
        verify(mockUserAccountDAO, times(1)).getById(user.getUserId()); // Adjusted from 2 to 1 for the retrieval part
                                                                    // The setupTestUser mock is separate.
                                                                    // Let's verify the specific interaction during retrieval.
                                                                    // The setupTestUser already has 'when(mockUserAccountDAO.getById(userId)).thenReturn(user);'
                                                                    // This means any call to getById for this user in CartDAOImpl will use this mock.
                                                                    // The saveOrUpdate might not call getById if the user object is already there.
                                                                    // The getBySessionId WILL call it via mapResultSetToCart.
    }

    @Test
    void testDeleteCartBySessionId() throws SQLException {
        String sessionId = "deleteSession101";
        Cart cart = new Cart(sessionId, null, LocalDateTime.now());
        cartDAO.saveOrUpdate(cart);

        assertNotNull(cartDAO.getBySessionId(sessionId)); // Pre-condition
        cartDAO.deleteBySessionId(sessionId);
        assertNull(cartDAO.getBySessionId(sessionId));
    }
    
    @Test
    void testAddItemToCart_DelegatesToCartItemDAO() throws SQLException {
        String sessionId = "addItemSession";
        Cart cart = new Cart(sessionId, null, LocalDateTime.now());
        // cartDAO.saveOrUpdate(cart); // Cart is not saved directly in this mocked scenario for addItem

        Product product = setupTestProduct("P_CartItem1", "Cart Item Product");
        
        // Create a CartItem without a Cart reference initially, as CartDAOImpl should set it.
        // The constructor CartItem(Cart cart, Product product, int quantity) is the correct one.
        // The error "The constructor CartItem(null, Product, int, float) is undefined"
        // indicates a mismatch in constructor arguments or an attempt to use a non-existent one.
        // Assuming CartItem constructor is CartItem(Cart, Product, int) and price is set via product.
        CartItem item = new CartItem(null, product, 1); // Price comes from product

        // Mock the behavior of getBySessionId to return the cart
        // This is crucial because addItemToCart internally calls getBySessionId
        // We need to use a spy or a more direct way if we were testing the real getBySessionId behavior here.
        // However, since cartDAO is already instantiated with mocks, we can mock its own methods if needed,
        // but it's better to mock dependencies.
        // For this specific test, we are testing the addItemToCart method of the real cartDAO instance,
        // which uses the mocked cartItemDAO.
        // The internal call to this.getBySessionId(sessionId) will use the real cartDAO's getBySessionId.
        // So, we must ensure the cart exists in the DB for that call to succeed, or mock that specific call on cartDAO itself (if it were a spy).

        // Let's ensure the cart is in the database so the internal getBySessionId can find it.
        cartDAO.saveOrUpdate(cart);
        when(mockCartItemDAO.getItemsByCartSessionId(sessionId)).thenReturn(new ArrayList<>()); // For the getBySessionId call

        cartDAO.addItemToCart(sessionId, item);
        verify(mockCartItemDAO).add(item); // Verify delegation
        // The cart reference in 'item' should be set by cartDAO.addItemToCart
        assertNotNull(item.getCart(), "Cart reference should be set on the item.");
        assertEquals(sessionId, item.getCart().getCartSessionId(), "Cart session ID should match.");
    }

    @Test
    void testRemoveItemFromCart_DelegatesToCartItemDAO() throws SQLException {
        String sessionId = "removeItemSession";
        String productId = "P_ToRemove";
        cartDAO.removeItemFromCart(sessionId, productId);
        verify(mockCartItemDAO).delete(sessionId, productId);
    }

    @Test
    void testUpdateItemQuantity_DelegatesToCartItemDAO() throws SQLException {
        String sessionId = "updateQtySession";
        String productId = "P_UpdateQty";
        int newQuantity = 5;

        CartItem mockItem = mock(CartItem.class); // Mock the item returned by getByIds
        when(mockCartItemDAO.getByIds(sessionId, productId)).thenReturn(mockItem);

        cartDAO.updateItemQuantity(sessionId, productId, newQuantity);

        verify(mockItem).setQuantity(newQuantity);
        verify(mockCartItemDAO).update(mockItem);
    }
    
    @Test
    void testUpdateItemQuantity_ItemNotFound_ThrowsSQLException() throws SQLException {
        String sessionId = "updateQtyNotFoundSession";
        String productId = "P_NotFound";
        when(mockCartItemDAO.getByIds(sessionId, productId)).thenReturn(null);

        assertThrows(SQLException.class, () -> {
            cartDAO.updateItemQuantity(sessionId, productId, 2);
        });
    }

    @Test
    void testClearCart_DelegatesToCartItemDAO() throws SQLException {
        String sessionId = "clearCartSession";
        cartDAO.clearCart(sessionId);
        verify(mockCartItemDAO).deleteByCartSessionId(sessionId);
    }
}
