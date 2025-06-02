package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.OrderItem;
import com.aims.core.entities.Product;
import com.aims.core.entities.UserAccount;
import com.aims.core.enums.OrderStatus;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderEntityDAOImplTest {

    private IOrderEntityDAO orderEntityDAO;
    private IUserAccountDAO userAccountDAO;
    private IProductDAO productDAO;
    private IOrderItemDAO orderItemDAO;
    private Connection connection; // This will be the connection managed by the test

    private UserAccount testUser;
    private Product testProduct;

    @TempDir
    static Path sharedTempDir; // Added
    private static Path dbFile; // Added
    private static String testDbUrl; // Added
    private static final String TEST_DB_URL_PREFIX = "jdbc:sqlite:"; // Added

    @BeforeAll // Added
    static void beforeAll() throws IOException, SQLException {
        dbFile = sharedTempDir.resolve("aims_order_entity_test.db");
        testDbUrl = TEST_DB_URL_PREFIX + dbFile.toAbsolutePath().toString();
        System.setProperty("TEST_DB_URL", testDbUrl); // Important for SQLiteConnector if it falls back

        // Initialize schema
        try (Connection conn = DriverManager.getConnection(testDbUrl)) {
            Path schemaScriptPath = Path.of("src/main/java/com/aims/core/infrastructure/database/scripts/V1__create_tables.sql");
            String schemaSql = Files.readString(schemaScriptPath);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("PRAGMA foreign_keys = ON;"); // Enable FKs for this connection
                // Execute schema statements one by one if they are ; separated
                for (String sqlStatement : schemaSql.split(";")) {
                    if (!sqlStatement.trim().isEmpty()) {
                        stmt.executeUpdate(sqlStatement);
                    }
                }
            }
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        // Each test gets a fresh connection to the temp DB
        connection = DriverManager.getConnection(testDbUrl);
        // Set this connection for the SQLiteConnector to use
        SQLiteConnector.getInstance().setConnection(connection);

        // DAOs will now use the connection set in SQLiteConnector
        userAccountDAO = new UserAccountDAOImpl();
        productDAO = new ProductDAOImpl();
        orderItemDAO = new OrderItemDAOImpl(productDAO);
        orderEntityDAO = new OrderEntityDAOImpl(orderItemDAO, userAccountDAO);

        // Clean up tables using the test-managed connection
        try (Statement stmt = connection.createStatement()) { // Use local connection for cleanup
            stmt.executeUpdate("DELETE FROM ORDER_ITEM;");
            stmt.executeUpdate("DELETE FROM ORDER_ENTITY;");
            stmt.executeUpdate("DELETE FROM DVD;"); // Clear specific product type tables first
            stmt.executeUpdate("DELETE FROM CD;");
            stmt.executeUpdate("DELETE FROM BOOK;");
            stmt.executeUpdate("DELETE FROM PRODUCT;");
            stmt.executeUpdate("DELETE FROM USER_ACCOUNT;");
        }

        // Setup common test data
        testUser = new UserAccount();
        testUser.setUserId("testuser001");
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hashedpassword");
        testUser.setEmail("testuser@example.com");
        testUser.setUserStatus(UserStatus.ACTIVE);
        userAccountDAO.add(testUser);
        testUser = userAccountDAO.getById("testuser001"); // Re-fetch to ensure it's in DB state

        testProduct = new Product();
        testProduct.setProductId("prod001");
        testProduct.setTitle("Test Product");
        testProduct.setDescription("A product for testing");
        testProduct.setPrice(100.0f);
        testProduct.setQuantityInStock(10);
        testProduct.setImageUrl("/path/to/image.jpg");
        testProduct.setProductType(ProductType.OTHER); // Default to OTHER if not a specific type
        testProduct.setEntryDate(LocalDate.now());
        testProduct.setWeightKg(1.5f);
        testProduct.setCategory("General Category");
        productDAO.addBaseProduct(testProduct); // Add base product
        // If it were a BOOK, CD, or DVD, add specific details too
        testProduct = productDAO.getById("prod001"); // Re-fetch
        assertNotNull(testProduct, "Test product setup failed, product not found after adding.");
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Clean up tables again to be safe, using the test-managed connection
        if (connection != null && !connection.isClosed()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM ORDER_ITEM;");
                stmt.executeUpdate("DELETE FROM ORDER_ENTITY;");
                stmt.executeUpdate("DELETE FROM DVD;");
                stmt.executeUpdate("DELETE FROM CD;");
                stmt.executeUpdate("DELETE FROM BOOK;");
                stmt.executeUpdate("DELETE FROM PRODUCT;");
                stmt.executeUpdate("DELETE FROM USER_ACCOUNT;");
            } finally {
                connection.close(); // Close the test-managed connection
            }
        }
        // Advise SQLiteConnector that the connection is no longer valid
        SQLiteConnector.getInstance().setConnection(null);
    }

    private OrderItem createTestOrderItem(OrderEntity order, Product product, int quantity, float pricePerUnit) {
        OrderItem item = new OrderItem();
        item.setOrderEntity(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setPriceAtTimeOfOrder(pricePerUnit);
        item.setEligibleForRushDelivery(false);
        return item;
    }

    @Test
    void testAddAndGetById() throws SQLException {
        OrderEntity order = new OrderEntity("ORD001", testUser, LocalDateTime.now(), OrderStatus.PENDING_PROCESSING, 100.0f, 110.0f, 10.0f, 120.0f);
        OrderItem item = createTestOrderItem(order, testProduct, 1, testProduct.getPrice());
        List<OrderItem> items = new ArrayList<>();
        items.add(item);
        order.setOrderItems(items);

        orderEntityDAO.add(order);
        orderItemDAO.add(item); 

        OrderEntity fetchedOrder = orderEntityDAO.getById("ORD001");
        assertNotNull(fetchedOrder);
        assertEquals(order.getOrderId(), fetchedOrder.getOrderId());
        assertNotNull(fetchedOrder.getUserAccount(), "User account in fetched order should not be null");
        assertEquals(testUser.getUserId(), fetchedOrder.getUserAccount().getUserId());
        assertEquals(OrderStatus.PENDING_PROCESSING, fetchedOrder.getOrderStatus());
        assertNotNull(fetchedOrder.getOrderItems(), "Order items should not be null");
        assertEquals(1, fetchedOrder.getOrderItems().size());
        assertNotNull(fetchedOrder.getOrderItems().get(0).getProduct(), "Product in order item should not be null");
        assertEquals(testProduct.getProductId(), fetchedOrder.getOrderItems().get(0).getProduct().getProductId());
    }

    @Test
    void testGetAll() throws SQLException {
        OrderEntity order1 = new OrderEntity("ORD002", testUser, LocalDateTime.now(), OrderStatus.PENDING_PROCESSING, 100f, 110f, 5.0f, 115.0f);
        OrderItem item1 = createTestOrderItem(order1, testProduct, 1, testProduct.getPrice());
        order1.setOrderItems(new ArrayList<>(List.of(item1)));
        orderEntityDAO.add(order1);
        orderItemDAO.add(item1);


        OrderEntity order2 = new OrderEntity("ORD003", testUser, LocalDateTime.now(), OrderStatus.SHIPPING, 200f, 220f, 10.0f, 230.0f);
        OrderItem item2 = createTestOrderItem(order2, testProduct, 2, testProduct.getPrice());
        order2.setOrderItems(new ArrayList<>(List.of(item2)));
        orderEntityDAO.add(order2);
        orderItemDAO.add(item2);

        List<OrderEntity> orders = orderEntityDAO.getAll();
        assertNotNull(orders);
        assertEquals(2, orders.size());
    }

    @Test
    void testUpdate() throws SQLException {
        OrderEntity order = new OrderEntity("ORD004", testUser, LocalDateTime.now(), OrderStatus.PENDING_PROCESSING, 100f, 110f, 5.0f, 115.0f);
        List<OrderItem> items = new ArrayList<>();
        OrderItem oi = createTestOrderItem(order, testProduct, 1, testProduct.getPrice());
        items.add(oi);
        order.setOrderItems(items);
        orderEntityDAO.add(order);
        orderItemDAO.add(oi);

        OrderEntity fetchedOrder = orderEntityDAO.getById("ORD004");
        assertNotNull(fetchedOrder);
        fetchedOrder.setOrderStatus(OrderStatus.DELIVERED);
        fetchedOrder.setCalculatedDeliveryFee(15.0f);

        orderEntityDAO.update(fetchedOrder);

        OrderEntity updatedOrder = orderEntityDAO.getById("ORD004");
        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.DELIVERED, updatedOrder.getOrderStatus());
        assertEquals(15.0f, updatedOrder.getCalculatedDeliveryFee(), 0.001);
    }

    @Test
    void testDelete() throws SQLException {
        OrderEntity order = new OrderEntity("ORD005", testUser, LocalDateTime.now(), OrderStatus.PENDING_PROCESSING, 100f, 110f, 5.0f, 115.0f);
        OrderItem item = createTestOrderItem(order, testProduct, 1, testProduct.getPrice());
        order.setOrderItems(new ArrayList<>(List.of(item)));
        orderEntityDAO.add(order);
        orderItemDAO.add(item);


        assertNotNull(orderEntityDAO.getById("ORD005"));
        // Deleting an order should ideally also delete its items via cascade or DAO logic.
        // For this test, we only check if the order is deleted.
        // If OrderItems are not cascaded, they might remain, which is a separate concern.
        orderEntityDAO.delete("ORD005"); 
        assertNull(orderEntityDAO.getById("ORD005"));
    }

    @Test
    void testGetOrdersByUser() throws SQLException {
        UserAccount localAnotherUser = new UserAccount(); 
        localAnotherUser.setUserId("anotheruser002");
        localAnotherUser.setUsername("anotheruser");
        localAnotherUser.setPasswordHash("password123");
        localAnotherUser.setEmail("another@example.com");
        localAnotherUser.setUserStatus(UserStatus.ACTIVE);
        userAccountDAO.add(localAnotherUser);
        final UserAccount retrievedAnotherUser = userAccountDAO.getById("anotheruser002"); 
        assertNotNull(retrievedAnotherUser, "Setup for anotherUser failed.");

        OrderEntity order1 = new OrderEntity("ORD006", testUser, LocalDateTime.now(), OrderStatus.PENDING_PROCESSING, 100f, 110f, 5.0f, 115.0f);
        OrderItem item1 = createTestOrderItem(order1, testProduct, 1, testProduct.getPrice());
        order1.setOrderItems(new ArrayList<>(List.of(item1)));
        orderEntityDAO.add(order1);
        orderItemDAO.add(item1);

        OrderEntity order2 = new OrderEntity("ORD007", retrievedAnotherUser, LocalDateTime.now(), OrderStatus.SHIPPING, 200f, 220f, 10.0f, 230.0f);
        OrderItem item2 = createTestOrderItem(order2, testProduct, 2, testProduct.getPrice());
        order2.setOrderItems(new ArrayList<>(List.of(item2)));
        orderEntityDAO.add(order2);
        orderItemDAO.add(item2);

        OrderEntity order3 = new OrderEntity("ORD008", testUser, LocalDateTime.now().minusDays(1), OrderStatus.DELIVERED, 150f, 165f, 7.0f, 172.0f);
        OrderItem item3 = createTestOrderItem(order3, testProduct, 1, testProduct.getPrice());
        order3.setOrderItems(new ArrayList<>(List.of(item3)));
        orderEntityDAO.add(order3);
        orderItemDAO.add(item3);

        List<OrderEntity> userOrders = orderEntityDAO.getByUserId(testUser.getUserId());
        assertNotNull(userOrders);
        assertEquals(2, userOrders.size());
        assertTrue(userOrders.stream().allMatch(o -> o.getUserAccount().getUserId().equals(testUser.getUserId())));

        List<OrderEntity> anotherUserOrders = orderEntityDAO.getByUserId(retrievedAnotherUser.getUserId());
        assertNotNull(anotherUserOrders);
        assertEquals(1, anotherUserOrders.size());
        assertTrue(anotherUserOrders.stream().allMatch(o -> o.getUserAccount().getUserId().equals(retrievedAnotherUser.getUserId())));
    }

    @Test
    void testGetOrdersByStatus() throws SQLException {
        OrderEntity order1 = new OrderEntity("ORD009", testUser, LocalDateTime.now(), OrderStatus.PENDING_PROCESSING, 100f,110f,10f,120f);
        OrderItem item1 = createTestOrderItem(order1, testProduct, 1, testProduct.getPrice());
        order1.setOrderItems(new ArrayList<>(List.of(item1)));
        orderEntityDAO.add(order1);
        orderItemDAO.add(item1);

        OrderEntity order2 = new OrderEntity("ORD010", testUser, LocalDateTime.now(), OrderStatus.SHIPPING, 200f,220f,10f,230f);
        OrderItem item2 = createTestOrderItem(order2, testProduct, 1, testProduct.getPrice());
        order2.setOrderItems(new ArrayList<>(List.of(item2)));
        orderEntityDAO.add(order2);
        orderItemDAO.add(item2);

        OrderEntity order3 = new OrderEntity("ORD011", testUser, LocalDateTime.now(), OrderStatus.PENDING_PROCESSING, 150f,165f,10f,175f);
        OrderItem item3 = createTestOrderItem(order3, testProduct, 1, testProduct.getPrice());
        order3.setOrderItems(new ArrayList<>(List.of(item3)));
        orderEntityDAO.add(order3);
        orderItemDAO.add(item3);

        List<OrderEntity> pendingOrders = orderEntityDAO.getByStatus(OrderStatus.PENDING_PROCESSING);
        assertNotNull(pendingOrders);
        assertEquals(2, pendingOrders.size());
        assertTrue(pendingOrders.stream().allMatch(o -> o.getOrderStatus() == OrderStatus.PENDING_PROCESSING));

        List<OrderEntity> shippedOrders = orderEntityDAO.getByStatus(OrderStatus.SHIPPING);
        assertNotNull(shippedOrders);
        assertEquals(1, shippedOrders.size());
        assertTrue(shippedOrders.stream().allMatch(o -> o.getOrderStatus() == OrderStatus.SHIPPING));
    }

    @Test
    void addOrderWithNoItems_ShouldPassWithCurrentDao() {
        OrderEntity order = new OrderEntity("ORD012", testUser, LocalDateTime.now(), OrderStatus.PENDING_PROCESSING, 0f, 0f, 0f, 0f);
        // No items added to order.setOrderItems()
        // No orderItemDAO.add() called for any items of this order
        try {
            orderEntityDAO.add(order);
            OrderEntity fetched = orderEntityDAO.getById("ORD012");
            assertNotNull(fetched);
            // OrderItems list is initialized in OrderEntity constructor, so it won't be null.
            // It will be empty if no items are added via setOrderItems or addOrderItem.
            // The OrderEntityDAOImpl.getById loads items using orderItemDAO.getItemsByOrderId.
            // If no items were persisted for this order, getItemsByOrderId should return an empty list.
            assertNotNull(fetched.getOrderItems(), "Order items list should not be null even if empty.");
            assertTrue(fetched.getOrderItems().isEmpty(), "Order items should be empty as none were added.");
        } catch (SQLException e) {
            fail("Adding an order with no items (but otherwise valid) should not cause SQLException with current DAO: " + e.getMessage());
        }
    }

    @Test
    void updateNonExistentOrder_ShouldNotErrorButAffectZeroRows() throws SQLException {
        OrderEntity order = new OrderEntity("ORD_NON_EXISTENT_9999", testUser, LocalDateTime.now(), OrderStatus.PENDING_PROCESSING, 10.0f, 11.0f, 1.0f, 12.0f);
        // Do not add this order to the database.
        // order.setOrderItems(...) // Not strictly needed as it won't be added.

        orderEntityDAO.update(order); // Attempt to update a non-existent order
        assertNull(orderEntityDAO.getById("ORD_NON_EXISTENT_9999"), "Order should still not exist after attempting update on non-existent ID.");
    }
}
