package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.UserAccount;
import com.aims.core.enums.OrderStatus;
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
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class DeliveryInfoDAOImplTest {

    private static final String TEST_DB_URL_PREFIX = "jdbc:sqlite:";
    private static String testDbUrl;
    private IDeliveryInfoDAO deliveryInfoDAO;
    private IOrderEntityDAO orderEntityDAO; // For setting up prerequisite OrderEntity
    private IUserAccountDAO userAccountDAO; // For OrderEntity prerequisite
    private IOrderItemDAO orderItemDAO; // For OrderEntityDAOImpl dependency
    private IProductDAO productDAO; // For OrderItemDAOImpl dependency
    private Connection connection;

    @TempDir
    static Path sharedTempDir;
    private static Path dbFile;

    @BeforeAll
    static void beforeAll() throws IOException, SQLException {
        dbFile = sharedTempDir.resolve("aims_delivery_info_test.db");
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

        userAccountDAO = new UserAccountDAOImpl();
        productDAO = new ProductDAOImpl(); // Instantiate ProductDAOImpl
        orderItemDAO = new OrderItemDAOImpl(productDAO); // Instantiate OrderItemDAOImpl with ProductDAO
        orderEntityDAO = new OrderEntityDAOImpl(orderItemDAO, userAccountDAO); // Corrected constructor call
        deliveryInfoDAO = new DeliveryInfoDAOImpl();

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM DELIVERY_INFO;");
            stmt.executeUpdate("DELETE FROM ORDER_ITEM;"); // Clear order items if they are linked
            stmt.executeUpdate("DELETE FROM ORDER_ENTITY;"); // Corrected table name
            stmt.executeUpdate("DELETE FROM USER_ACCOUNT;");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        SQLiteConnector.getInstance().setConnection(null);
    }

    private UserAccount setupTestUser(String userId) throws SQLException {
        UserAccount user = new UserAccount(userId, "user_" + userId, "password", userId + "@example.com", UserStatus.ACTIVE);
        userAccountDAO.add(user);
        return user;
    }

    private OrderEntity setupTestOrder(String orderId, UserAccount user) throws SQLException {
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        order.setUserAccount(user);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING_PROCESSING); // Corrected Enum value
        order.setTotalProductPriceExclVAT(90.0f);
        order.setTotalProductPriceInclVAT(100.0f);
        order.setCalculatedDeliveryFee(10.0f);
        order.setTotalAmountPaid(110.0f); // Example value
        order.setOrderItems(new ArrayList<>()); // Initialize items list
        orderEntityDAO.add(order); // Add order to DB
        return order;
    }

    @Test
    void testAddAndGetDeliveryInfoById() throws SQLException {
        UserAccount user = setupTestUser("U_DI1");
        OrderEntity order = setupTestOrder("O_DI1", user);
        String deliveryInfoId = "DI_1";

        DeliveryInfo info = new DeliveryInfo();
        info.setDeliveryInfoId(deliveryInfoId);
        info.setOrderEntity(order);
        info.setRecipientName("John Recipient");
        info.setEmail("john.recipient@example.com");
        info.setPhoneNumber("1234567890"); // Ensure phone number is set
        info.setDeliveryProvinceCity("Test City");
        info.setDeliveryAddress("123 Test Street"); // Ensure address is set
        info.setDeliveryMethodChosen("Standard");
        info.setRequestedRushDeliveryTime(LocalDateTime.now().plusDays(1));

        deliveryInfoDAO.add(info);

        DeliveryInfo retrievedInfo = deliveryInfoDAO.getById(deliveryInfoId);
        assertNotNull(retrievedInfo);
        assertEquals(order.getOrderId(), retrievedInfo.getOrderEntity().getOrderId());
        assertEquals("John Recipient", retrievedInfo.getRecipientName());
        assertEquals(info.getRequestedRushDeliveryTime().withNano(0), retrievedInfo.getRequestedRushDeliveryTime().withNano(0)); // Compare without nanos for LocalDateTime from DB
    }

    @Test
    void testAddAndGetDeliveryInfoByOrderId() throws SQLException {
        UserAccount user = setupTestUser("U_DI2");
        OrderEntity order = setupTestOrder("O_DI2", user);

        DeliveryInfo info = new DeliveryInfo();
        info.setDeliveryInfoId("DI_2");
        info.setOrderEntity(order);
        info.setRecipientName("Jane Receiver");
        info.setEmail("jane.receiver@example.com");
        info.setPhoneNumber("0987654321"); // Ensure phone number is set
        info.setDeliveryProvinceCity("Another City");
        info.setDeliveryAddress("456 Sample Avenue"); // Ensure address is set
        info.setDeliveryMethodChosen("Express");
        deliveryInfoDAO.add(info);

        DeliveryInfo retrievedInfo = deliveryInfoDAO.getByOrderId(order.getOrderId());
        assertNotNull(retrievedInfo);
        assertEquals("DI_2", retrievedInfo.getDeliveryInfoId());
        assertEquals("Jane Receiver", retrievedInfo.getRecipientName());
    }
    
    @Test
    void testAddDeliveryInfo_DuplicateOrderId_ThrowsException() throws SQLException {
        UserAccount user = setupTestUser("U_DI_DupOrder");
        OrderEntity order = setupTestOrder("O_DI_DupOrder", user);

        DeliveryInfo info1 = new DeliveryInfo();
        info1.setDeliveryInfoId("DI_Dup1");
        info1.setOrderEntity(order);
        info1.setRecipientName("Recipient One");
        info1.setEmail("recipient.one@example.com");
        info1.setPhoneNumber("111222333"); // Ensure phone number is set
        info1.setDeliveryProvinceCity("City One");
        info1.setDeliveryAddress("789 First Street"); // Ensure address is set
        info1.setDeliveryMethodChosen("Standard");
        deliveryInfoDAO.add(info1);

        DeliveryInfo info2 = new DeliveryInfo();
        info2.setDeliveryInfoId("DI_Dup2");
        info2.setOrderEntity(order); // Same order ID
        info2.setRecipientName("Recipient Two");
        info2.setEmail("recipient.two@example.com");
        info2.setPhoneNumber("444555666"); // Ensure phone number is set
        info2.setDeliveryProvinceCity("City Two");
        info2.setDeliveryAddress("101 Second Avenue"); // Ensure address is set
        info2.setDeliveryMethodChosen("Express");

        // The current DeliveryInfoDAOImpl add method checks for unique orderID constraint from DB
        SQLException exception = assertThrows(SQLException.class, () -> deliveryInfoDAO.add(info2));
        assertTrue(exception.getMessage().contains("already exists") || exception.getMessage().contains("UNIQUE constraint failed: DELIVERY_INFO.orderID"));
    }


    @Test
    void testUpdateDeliveryInfo() throws SQLException {
        UserAccount user = setupTestUser("U_DI_Upd");
        OrderEntity order = setupTestOrder("O_DI_Upd", user);
        String deliveryInfoId = "DI_Upd1";

        DeliveryInfo info = new DeliveryInfo();
        info.setDeliveryInfoId(deliveryInfoId);
        info.setOrderEntity(order);
        info.setRecipientName("Initial Recipient");
        info.setEmail("initial.recipient@example.com");
        info.setPhoneNumber("777888999"); // Ensure phone number is set
        info.setDeliveryProvinceCity("Initial City");
        info.setDeliveryAddress("321 Initial Road"); // Ensure address is set
        info.setDeliveryMethodChosen("Standard");
        info.setRequestedRushDeliveryTime(null);
        deliveryInfoDAO.add(info);

        info.setRecipientName("Updated Recipient");
        info.setDeliveryAddress("Updated Address, New City");
        LocalDateTime newRushTime = LocalDateTime.now().plusHours(5);
        info.setRequestedRushDeliveryTime(newRushTime);
        deliveryInfoDAO.update(info);

        DeliveryInfo updatedInfo = deliveryInfoDAO.getById(deliveryInfoId);
        assertNotNull(updatedInfo);
        assertEquals("Updated Recipient", updatedInfo.getRecipientName());
        assertEquals("Updated Address, New City", updatedInfo.getDeliveryAddress());
        assertNotNull(updatedInfo.getRequestedRushDeliveryTime());
        assertEquals(newRushTime.withNano(0), updatedInfo.getRequestedRushDeliveryTime().withNano(0));
    }
    
    @Test
    void testUpdateDeliveryInfo_NotFound() throws SQLException {
        UserAccount user = setupTestUser("U_DI_UpNotFound");
        OrderEntity order = setupTestOrder("O_DI_UpNotFound", user);

        DeliveryInfo nonExistentInfo = new DeliveryInfo();
        nonExistentInfo.setDeliveryInfoId("DI_NON_EXISTENT");
        nonExistentInfo.setOrderEntity(order); // Valid order, but DI_ID is not in DB
        nonExistentInfo.setRecipientName("Ghost");

        SQLException exception = assertThrows(SQLException.class, () -> deliveryInfoDAO.update(nonExistentInfo));
        assertTrue(exception.getMessage().contains("Updating delivery info failed, no matching record found"));
    }


    @Test
    void testDeleteDeliveryInfoById() throws SQLException {
        UserAccount user = setupTestUser("U_DI_DelId");
        OrderEntity order = setupTestOrder("O_DI_DelId", user);
        String deliveryInfoId = "DI_DelId1";

        DeliveryInfo info = new DeliveryInfo();
        info.setDeliveryInfoId(deliveryInfoId);
        info.setOrderEntity(order);
        info.setRecipientName("To Delete Recipient");
        info.setEmail("delete.recipient@example.com");
        info.setPhoneNumber("123123123"); // Ensure phone number is set
        info.setDeliveryProvinceCity("Delete City");
        info.setDeliveryAddress("654 Delete Way"); // Ensure address is set
        info.setDeliveryMethodChosen("Standard");
        deliveryInfoDAO.add(info);

        assertNotNull(deliveryInfoDAO.getById(deliveryInfoId));
        deliveryInfoDAO.deleteById(deliveryInfoId);
        assertNull(deliveryInfoDAO.getById(deliveryInfoId));
    }

    @Test
    void testDeleteDeliveryInfoByOrderId() throws SQLException {
        UserAccount user = setupTestUser("U_DI_DelOrd");
        OrderEntity order = setupTestOrder("O_DI_DelOrd", user);
        String deliveryInfoId = "DI_DelOrd1";

        DeliveryInfo info = new DeliveryInfo();
        info.setDeliveryInfoId(deliveryInfoId);
        info.setOrderEntity(order);
        info.setRecipientName("To Delete By Order Recipient");
        info.setEmail("delete.order.recipient@example.com");
        info.setPhoneNumber("456456456"); // Ensure phone number is set
        info.setDeliveryProvinceCity("Delete Order City");
        info.setDeliveryAddress("987 Order Path"); // Ensure address is set
        info.setDeliveryMethodChosen("Express");
        deliveryInfoDAO.add(info);

        assertNotNull(deliveryInfoDAO.getByOrderId(order.getOrderId()));
        deliveryInfoDAO.deleteByOrderId(order.getOrderId());
        assertNull(deliveryInfoDAO.getByOrderId(order.getOrderId()));
        assertNull(deliveryInfoDAO.getById(deliveryInfoId)); // Also should be null if deleted by orderId
    }
    
    @Test
    void testGetById_NotFound() throws SQLException {
        assertNull(deliveryInfoDAO.getById("DI_NON_EXISTENT_GET"));
    }

    @Test
    void testGetByOrderId_NotFound() throws SQLException {
        assertNull(deliveryInfoDAO.getByOrderId("O_NON_EXISTENT_GET"));
    }
}
