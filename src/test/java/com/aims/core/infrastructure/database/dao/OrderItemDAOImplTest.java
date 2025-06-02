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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemDAOImplTest {

    private IOrderItemDAO orderItemDAO;
    private IProductDAO productDAO;
    private IUserAccountDAO userAccountDAO;
    private IOrderEntityDAO orderEntityDAO; // To create prerequisite orders
    private Connection connection;

    private UserAccount testUser;
    private Product testProduct1;
    private Product testProduct2;
    private OrderEntity testOrder;

    @TempDir
    static Path sharedTempDir;
    private static Path dbFile;
    private static String testDbUrl;
    private static final String TEST_DB_URL_PREFIX = "jdbc:sqlite:";

    @BeforeAll
    static void beforeAll() throws IOException, SQLException {
        dbFile = sharedTempDir.resolve("aims_orderitem_test.db");
        testDbUrl = TEST_DB_URL_PREFIX + dbFile.toAbsolutePath().toString();
        System.setProperty("TEST_DB_URL", testDbUrl);

        try (Connection conn = DriverManager.getConnection(testDbUrl)) {
            Path schemaScriptPath = Path.of("src/main/java/com/aims/core/infrastructure/database/scripts/V1__create_tables.sql");
            String schemaSql = Files.readString(schemaScriptPath);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("PRAGMA foreign_keys = ON;");
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
        connection = DriverManager.getConnection(testDbUrl);
        SQLiteConnector.getInstance().setConnection(connection);

        productDAO = new ProductDAOImpl();
        userAccountDAO = new UserAccountDAOImpl();
        // OrderItemDAO needs ProductDAO. OrderEntityDAO needs OrderItemDAO and UserAccountDAO.
        orderItemDAO = new OrderItemDAOImpl(productDAO);
        orderEntityDAO = new OrderEntityDAOImpl(orderItemDAO, userAccountDAO);


        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM ORDER_ITEM;");
            stmt.executeUpdate("DELETE FROM ORDER_ENTITY;");
            stmt.executeUpdate("DELETE FROM DVD;");
            stmt.executeUpdate("DELETE FROM CD;");
            stmt.executeUpdate("DELETE FROM BOOK;");
            stmt.executeUpdate("DELETE FROM PRODUCT;");
            stmt.executeUpdate("DELETE FROM USER_ACCOUNT;");
        }

        // Setup prerequisite data
        testUser = new UserAccount("user001", "testuser", "passhash", "user@example.com", UserStatus.ACTIVE);
        userAccountDAO.add(testUser);
        testUser = userAccountDAO.getById("user001"); // re-fetch

        testProduct1 = new Product("prod001", "Test Product 1", "Category A", 100.0f, 120.0f, 10,
                "/img1.jpg", "Desc 1", "barcode1", "dim1", 1.0f, LocalDate.now(), ProductType.OTHER);
        productDAO.addBaseProduct(testProduct1);
        // If testProduct1 were a Book, CD, or DVD, add specific details:
        // e.g. if (testProduct1 instanceof Book) productDAO.addBookDetails((Book) testProduct1);
        testProduct1 = productDAO.getById("prod001"); // re-fetch

        testProduct2 = new Product("prod002", "Test Product 2", "Category B", 50.0f, 60.0f, 5,
                "/img2.jpg", "Desc 2", "barcode2", "dim2", 0.5f, LocalDate.now(), ProductType.OTHER);
        productDAO.addBaseProduct(testProduct2);
        testProduct2 = productDAO.getById("prod002"); // re-fetch


        testOrder = new OrderEntity("order001", testUser, LocalDateTime.now(), OrderStatus.PENDING_PROCESSING,
                0,0,0,0); // Prices will be based on items
        orderEntityDAO.add(testOrder);
        testOrder = orderEntityDAO.getById("order001"); // re-fetch
        assertNotNull(testOrder, "Test order setup failed.");
        assertNotNull(testProduct1, "Test product 1 setup failed.");
        assertNotNull(testProduct2, "Test product 2 setup failed.");
    }

    @AfterEach
    void tearDown() throws SQLException {
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
                connection.close();
            }
        }
        SQLiteConnector.getInstance().setConnection(null);
    }

    @Test
    void testAddAndGetByIds() throws SQLException {
        OrderItem item = new OrderItem();
        item.setOrderEntity(testOrder);
        item.setProduct(testProduct1);
        item.setQuantity(2);
        item.setPriceAtTimeOfOrder(testProduct1.getPrice());
        item.setEligibleForRushDelivery(true);

        orderItemDAO.add(item);

        OrderItem fetchedItem = orderItemDAO.getByIds(testOrder.getOrderId(), testProduct1.getProductId());
        assertNotNull(fetchedItem);
        assertEquals(testOrder.getOrderId(), fetchedItem.getOrderEntity().getOrderId());
        assertEquals(testProduct1.getProductId(), fetchedItem.getProduct().getProductId());
        assertEquals(2, fetchedItem.getQuantity());
        assertEquals(testProduct1.getPrice(), fetchedItem.getPriceAtTimeOfOrder(), 0.001);
        assertTrue(fetchedItem.isEligibleForRushDelivery());
    }

    @Test
    void testGetItemsByOrderId() throws SQLException {
        OrderItem item1 = new OrderItem();
        item1.setOrderEntity(testOrder);
        item1.setProduct(testProduct1);
        item1.setQuantity(1);
        item1.setPriceAtTimeOfOrder(testProduct1.getPrice());
        item1.setEligibleForRushDelivery(false);
        orderItemDAO.add(item1);

        OrderItem item2 = new OrderItem();
        item2.setOrderEntity(testOrder);
        item2.setProduct(testProduct2);
        item2.setQuantity(3);
        item2.setPriceAtTimeOfOrder(testProduct2.getPrice());
        item2.setEligibleForRushDelivery(true);
        orderItemDAO.add(item2);

        List<OrderItem> fetchedItems = orderItemDAO.getItemsByOrderId(testOrder.getOrderId());
        assertNotNull(fetchedItems);
        assertEquals(2, fetchedItems.size());

        // Check if both items are present (order might not be guaranteed)
        boolean foundItem1 = fetchedItems.stream().anyMatch(i -> i.getProduct().getProductId().equals(testProduct1.getProductId()));
        boolean foundItem2 = fetchedItems.stream().anyMatch(i -> i.getProduct().getProductId().equals(testProduct2.getProductId()));
        assertTrue(foundItem1, "TestProduct1 not found in fetched items for order.");
        assertTrue(foundItem2, "TestProduct2 not found in fetched items for order.");
    }

    @Test
    void testUpdate() throws SQLException {
        OrderItem item = new OrderItem();
        item.setOrderEntity(testOrder);
        item.setProduct(testProduct1);
        item.setQuantity(1);
        item.setPriceAtTimeOfOrder(testProduct1.getPrice());
        item.setEligibleForRushDelivery(false);
        orderItemDAO.add(item);

        OrderItem fetchedItem = orderItemDAO.getByIds(testOrder.getOrderId(), testProduct1.getProductId());
        assertNotNull(fetchedItem);

        fetchedItem.setQuantity(5);
        fetchedItem.setPriceAtTimeOfOrder(115.0f); // Price at time of order might be fixed, but testing update
        fetchedItem.setEligibleForRushDelivery(true);
        orderItemDAO.update(fetchedItem);

        OrderItem updatedItem = orderItemDAO.getByIds(testOrder.getOrderId(), testProduct1.getProductId());
        assertNotNull(updatedItem);
        assertEquals(5, updatedItem.getQuantity());
        assertEquals(115.0f, updatedItem.getPriceAtTimeOfOrder(), 0.001);
        assertTrue(updatedItem.isEligibleForRushDelivery());
    }

    @Test
    void testDelete() throws SQLException {
        OrderItem item = new OrderItem();
        item.setOrderEntity(testOrder);
        item.setProduct(testProduct1);
        item.setQuantity(1);
        item.setPriceAtTimeOfOrder(testProduct1.getPrice());
        orderItemDAO.add(item);

        assertNotNull(orderItemDAO.getByIds(testOrder.getOrderId(), testProduct1.getProductId()));
        orderItemDAO.delete(testOrder.getOrderId(), testProduct1.getProductId());
        assertNull(orderItemDAO.getByIds(testOrder.getOrderId(), testProduct1.getProductId()));
    }

    @Test
    void testDeleteByOrderId() throws SQLException {
        OrderItem item1 = new OrderItem();
        item1.setOrderEntity(testOrder);
        item1.setProduct(testProduct1);
        item1.setQuantity(1);
        item1.setPriceAtTimeOfOrder(testProduct1.getPrice());
        orderItemDAO.add(item1);

        OrderItem item2 = new OrderItem();
        item2.setOrderEntity(testOrder);
        item2.setProduct(testProduct2);
        item2.setQuantity(2);
        item2.setPriceAtTimeOfOrder(testProduct2.getPrice());
        orderItemDAO.add(item2);

        assertEquals(2, orderItemDAO.getItemsByOrderId(testOrder.getOrderId()).size());
        orderItemDAO.deleteByOrderId(testOrder.getOrderId());
        assertTrue(orderItemDAO.getItemsByOrderId(testOrder.getOrderId()).isEmpty());
    }

    @Test
    void addOrderItem_NonExistentOrder_ShouldFailDueToForeignKeyConstraint() throws SQLException {
        OrderEntity nonExistentOrder = new OrderEntity();
        nonExistentOrder.setOrderId("NON_EXISTENT_ORDER_ID_12345");
        // Do not add this order to the DB

        OrderItem item = new OrderItem();
        item.setOrderEntity(nonExistentOrder); // Associate with non-existent order
        item.setProduct(testProduct1);
        item.setQuantity(1);
        item.setPriceAtTimeOfOrder(testProduct1.getPrice());

        // Expecting an SQLException due to foreign key violation (orderID in ORDER_ITEM must exist in ORDER_ENTITY)
        assertThrows(SQLException.class, () -> {
            orderItemDAO.add(item);
        }, "Adding an OrderItem for a non-existent OrderEntity should throw SQLException due to FK constraint.");
    }

    @Test
    void addOrderItem_NonExistentProduct_ShouldFailDueToForeignKeyConstraint() throws SQLException {
        Product nonExistentProduct = new Product();
        nonExistentProduct.setProductId("NON_EXISTENT_PROD_ID_67890");
        // Do not add this product to the DB

        OrderItem item = new OrderItem();
        item.setOrderEntity(testOrder);
        item.setProduct(nonExistentProduct); // Associate with non-existent product
        item.setQuantity(1);
        item.setPriceAtTimeOfOrder(99.99f); // Arbitrary price

        // Expecting an SQLException due to foreign key violation (productID in ORDER_ITEM must exist in PRODUCT)
        assertThrows(SQLException.class, () -> {
            orderItemDAO.add(item);
        }, "Adding an OrderItem for a non-existent Product should throw SQLException due to FK constraint.");
    }
}
