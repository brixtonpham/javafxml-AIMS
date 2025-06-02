package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Invoice;
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

public class InvoiceDAOImplTest {

    private static final String TEST_DB_URL_PREFIX = "jdbc:sqlite:";
    private static String testDbUrl;
    private IInvoiceDAO invoiceDAO;
    private IOrderEntityDAO orderEntityDAO;
    private IUserAccountDAO userAccountDAO;
    private IOrderItemDAO orderItemDAO;
    private IProductDAO productDAO;
    private Connection connection;

    @TempDir
    static Path sharedTempDir;
    private static Path dbFile;

    @BeforeAll
    static void beforeAll() throws IOException, SQLException {
        dbFile = sharedTempDir.resolve("aims_invoice_test.db");
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
        productDAO = new ProductDAOImpl();
        orderItemDAO = new OrderItemDAOImpl(productDAO);
        orderEntityDAO = new OrderEntityDAOImpl(orderItemDAO, userAccountDAO);
        invoiceDAO = new InvoiceDAOImpl(); // Corrected: InvoiceDAOImpl has a no-arg constructor

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM INVOICE;");
            stmt.executeUpdate("DELETE FROM ORDER_ITEM;");
            stmt.executeUpdate("DELETE FROM ORDER_ENTITY;");
            stmt.executeUpdate("DELETE FROM USER_ACCOUNT;");
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
        order.setOrderStatus(OrderStatus.PENDING_PROCESSING);
        order.setTotalProductPriceExclVAT(100.0f);
        order.setTotalProductPriceInclVAT(120.0f);
        order.setCalculatedDeliveryFee(10.0f);
        order.setTotalAmountPaid(130.0f);
        order.setOrderItems(new ArrayList<>());
        orderEntityDAO.add(order);
        return order;
    }

    @Test
    void testAddAndGetInvoiceById() throws SQLException {
        UserAccount user = setupTestUser("U_Inv1");
        OrderEntity order = setupTestOrder("O_Inv1", user);
        String invoiceId = "Inv1";

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setOrderEntity(order);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setInvoicedTotalAmount(order.getTotalAmountPaid()); // Corrected method name

        invoiceDAO.add(invoice);

        Invoice retrievedInvoice = invoiceDAO.getById(invoiceId);
        assertNotNull(retrievedInvoice);
        assertEquals(order.getOrderId(), retrievedInvoice.getOrderEntity().getOrderId());
        assertEquals(invoice.getInvoicedTotalAmount(), retrievedInvoice.getInvoicedTotalAmount()); // Corrected method name
        assertEquals(invoice.getInvoiceDate().withNano(0), retrievedInvoice.getInvoiceDate().withNano(0));
    }

    @Test
    void testGetInvoiceByOrderId() throws SQLException {
        UserAccount user = setupTestUser("U_Inv2");
        OrderEntity order = setupTestOrder("O_Inv2", user);
        String invoiceId = "Inv2";

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setOrderEntity(order);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setInvoicedTotalAmount(order.getTotalAmountPaid()); // Corrected method name
        invoiceDAO.add(invoice);

        Invoice retrievedInvoice = invoiceDAO.getByOrderId(order.getOrderId());
        assertNotNull(retrievedInvoice);
        assertEquals(invoiceId, retrievedInvoice.getInvoiceId());
    }
    
    @Test
    void testAddInvoice_DuplicateOrderId_ThrowsException() throws SQLException {
        UserAccount user = setupTestUser("U_Inv_DupOrder");
        OrderEntity order = setupTestOrder("O_Inv_DupOrder", user);

        Invoice invoice1 = new Invoice();
        invoice1.setInvoiceId("Inv_Dup1");
        invoice1.setOrderEntity(order);
        invoice1.setInvoiceDate(LocalDateTime.now());
        invoice1.setInvoicedTotalAmount(100f); // Corrected method name
        invoiceDAO.add(invoice1);

        Invoice invoice2 = new Invoice();
        invoice2.setInvoiceId("Inv_Dup2");
        invoice2.setOrderEntity(order); // Same order
        invoice2.setInvoiceDate(LocalDateTime.now().plusHours(1));
        invoice2.setInvoicedTotalAmount(150f); // Corrected method name
        
        // Assuming INVOICE.orderID has a UNIQUE constraint
        SQLException exception = assertThrows(SQLException.class, () -> invoiceDAO.add(invoice2));
        // assertTrue(exception.getMessage().contains("UNIQUE constraint failed: INVOICE.orderID")); // Original assertion
        assertTrue(exception.getMessage().contains("Invoice for order ID '" + order.getOrderId() + "' already exists.")); // Adjusted assertion
    }


    @Test
    void testUpdateInvoice() throws SQLException {
        UserAccount user = setupTestUser("U_Inv3");
        OrderEntity order = setupTestOrder("O_Inv3", user);
        String invoiceId = "Inv3";

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setOrderEntity(order);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setInvoicedTotalAmount(200.0f); // Corrected method name
        invoiceDAO.add(invoice);

        LocalDateTime newDate = LocalDateTime.now().plusDays(1);
        float newTotal = 250.0f;
        invoice.setInvoiceDate(newDate);
        invoice.setInvoicedTotalAmount(newTotal); // Corrected method name
        invoiceDAO.update(invoice);

        Invoice updatedInvoice = invoiceDAO.getById(invoiceId);
        assertNotNull(updatedInvoice);
        assertEquals(newTotal, updatedInvoice.getInvoicedTotalAmount()); // Corrected method name
        assertEquals(newDate.withNano(0), updatedInvoice.getInvoiceDate().withNano(0));
    }
    
    @Test
    void testUpdateInvoice_NotFound() throws SQLException {
        Invoice nonExistentInvoice = new Invoice();
        nonExistentInvoice.setInvoiceId("Inv_NonExistent");
        OrderEntity dummyOrder = new OrderEntity();
        dummyOrder.setOrderId("O_Dummy"); // Needs a valid order if FK is checked, but DAO might not fetch it for update
        nonExistentInvoice.setOrderEntity(dummyOrder);
        nonExistentInvoice.setInvoiceDate(LocalDateTime.now());
        nonExistentInvoice.setInvoicedTotalAmount(50f); // Corrected method name

        SQLException exception = assertThrows(SQLException.class, () -> invoiceDAO.update(nonExistentInvoice));
        // assertTrue(exception.getMessage().contains("Updating invoice failed, no record found for ID: Inv_NonExistent")); // Original assertion
        assertTrue(exception.getMessage().contains("Updating invoice failed, no matching record found for ID: " + nonExistentInvoice.getInvoiceId())); // Adjusted assertion
    }


    @Test
    void testDeleteInvoice() throws SQLException {
        UserAccount user = setupTestUser("U_Inv4");
        OrderEntity order = setupTestOrder("O_Inv4", user);
        String invoiceId = "Inv4";

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setOrderEntity(order);
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setInvoicedTotalAmount(300.0f); // Corrected method name
        invoiceDAO.add(invoice);

        assertNotNull(invoiceDAO.getById(invoiceId));
        invoiceDAO.deleteById(invoiceId); // Corrected method name
        assertNull(invoiceDAO.getById(invoiceId));
    }

    @Test
    void testGetById_NotFound() throws SQLException {
        assertNull(invoiceDAO.getById("Inv_NonExistent"));
    }

    @Test
    void testGetByOrderId_NotFound() throws SQLException {
        assertNull(invoiceDAO.getByOrderId("O_NonExistent"));
    }
}
