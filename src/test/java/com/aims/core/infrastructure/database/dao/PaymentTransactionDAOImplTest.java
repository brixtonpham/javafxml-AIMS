package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.entities.UserAccount;
import com.aims.core.enums.OrderStatus;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.enums.UserStatus;
import com.aims.core.enums.TransactionType;
import com.aims.core.infrastructure.database.SQLiteConnector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager; // Added for direct DriverManager usage
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PaymentTransactionDAOImplTest {

    private static IPaymentTransactionDAO paymentTransactionDAO;
    private static IUserAccountDAO userAccountDAO;
    private static IOrderEntityDAO orderEntityDAO;
    private static IPaymentMethodDAO paymentMethodDAO;

    private static UserAccount testUser;
    private static OrderEntity testOrder;
    private static PaymentMethod testPaymentMethod;

    @TempDir
    static Path tempDir;
    static Path dbFile;

    @BeforeAll
    static void setUpAll() throws SQLException, IOException {
        dbFile = tempDir.resolve("test_aims_transaction.db");
        // Ensure the db file is deleted before starting to guarantee a clean state for each full test run
        Files.deleteIfExists(dbFile);

        String localDbUrl = "jdbc:sqlite:" + dbFile.toAbsolutePath();
        System.setProperty("TEST_DB_URL", localDbUrl); // Set system property for SQLiteConnector's default path
        
        // Let SQLiteConnector create its own connection using the TEST_DB_URL
        // Don't manually create and set a connection - this causes conflicts

        // Initialize DAOs. They will get connections from SQLiteConnector (which now uses TEST_DB_URL).
        userAccountDAO = new UserAccountDAOImpl();
        IOrderItemDAO orderItemDAO = mock(IOrderItemDAO.class); 
        orderEntityDAO = new OrderEntityDAOImpl(orderItemDAO, userAccountDAO);
        ICardDetailsDAO cardDetailsDAO = mock(ICardDetailsDAO.class); 
        // Configure the mock to handle add() calls without throwing exceptions
        // Since our PaymentMethod will have null cardDetails, this shouldn't be called
        // But just in case, let's make it not throw an exception
        doNothing().when(cardDetailsDAO).add(any()); 
        paymentMethodDAO = new PaymentMethodDAOImpl(userAccountDAO, cardDetailsDAO);
        paymentTransactionDAO = new PaymentTransactionDAOImpl(orderEntityDAO, paymentMethodDAO);

        // Schema setup: get connection from SQLiteConnector
        try (Connection schemaConn = SQLiteConnector.getInstance().getConnection();
             Statement stmt = schemaConn.createStatement()) {
            String schemaSql = new String(Files.readAllBytes(Path.of("src/main/resources/migration/V1__create_tables.sql")));
            String cleanedSql = schemaSql.replaceAll("--.*\\\\R", "").replaceAll("/\\\\*[^*]*\\\\*+(?:[^/*][^*]*\\\\*+)*/", "");
            String[] statements = cleanedSql.split(";", -1);
            for (String s : statements) {
                String trimmedStatement = s.trim();
                if (!trimmedStatement.isEmpty()) {
                    stmt.executeUpdate(trimmedStatement);
                }
            }
        }

        // Add initial data using DAOs. Each DAO call will use the connection from SQLiteConnector.
        UserAccount initialUser = new UserAccount("userTrans", "transUser", "pass", "trans@example.com", UserStatus.ACTIVE);
        userAccountDAO.add(initialUser); 
        testUser = userAccountDAO.getById(initialUser.getUserId()); 
        assertNotNull(testUser, "Test user not found after add.");

        OrderEntity initialOrder = new OrderEntity();
        initialOrder.setOrderId("orderTrans1");
        initialOrder.setUserAccount(testUser); 
        initialOrder.setOrderDate(LocalDateTime.now());
        initialOrder.setOrderStatus(OrderStatus.PENDING_PROCESSING);
        initialOrder.setTotalAmountPaid(100.00f);
        orderEntityDAO.add(initialOrder); 
        testOrder = orderEntityDAO.getById(initialOrder.getOrderId()); 
        assertNotNull(testOrder, "Test order not found after add.");
        
        assertNotNull(testOrder.getUserAccount(), "User account in testOrder should be populated by OrderEntityDAOImpl.getById.");
        assertEquals(testUser.getUserId(), testOrder.getUserAccount().getUserId(), "User ID mismatch in fetched order.");


        PaymentMethod initialPaymentMethod = new PaymentMethod("pmTrans1", PaymentMethodType.CREDIT_CARD, testUser, true); 
        
        // DEBUG: Check if cardDetails is null as expected
        System.out.println("DEBUG: initialPaymentMethod.getCardDetails() = " + initialPaymentMethod.getCardDetails());
        
        try {
            paymentMethodDAO.add(initialPaymentMethod);
            System.out.println("DEBUG: PaymentMethod add() completed successfully");
        } catch (Exception e) {
            System.err.println("DEBUG: Exception during PaymentMethod add(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        // Force SQLiteConnector to commit any pending transactions
        try (Connection connCheck = SQLiteConnector.getInstance().getConnection()) {
            if (!connCheck.getAutoCommit()) {
                System.out.println("DEBUG: Connection is not in autocommit mode, forcing commit");
                connCheck.commit();
            }
            System.out.println("DEBUG: Connection AutoCommit: " + connCheck.getAutoCommit());
            System.out.println("DEBUG: Connection URL: " + connCheck.getMetaData().getURL());
        } 
        testPaymentMethod = paymentMethodDAO.getById(initialPaymentMethod.getPaymentMethodId()); 
        assertNotNull(testPaymentMethod, "Test payment method not found after add.");
        
        assertNotNull(testPaymentMethod.getUserAccount(), "User account in testPaymentMethod should be populated by PaymentMethodDAOImpl.getById.");
        assertEquals(testUser.getUserId(), testPaymentMethod.getUserAccount().getUserId(), "User ID mismatch in fetched payment method.");

        // Verification Queries: Use a NEW, DEDICATED connection for verification.
        try (Connection verificationConn = DriverManager.getConnection(localDbUrl)) { 
            // Verify User
            try (PreparedStatement pstmt = verificationConn.prepareStatement("SELECT COUNT(*) FROM USER_ACCOUNT WHERE userID = ?")) {
                pstmt.setString(1, testUser.getUserId());
                ResultSet rs = pstmt.executeQuery();
                assertTrue(rs.next(), "ResultSet expected for user verification query.");
                int count = rs.getInt(1);
                assertEquals(1, count, "Test user '" + testUser.getUserId() + "' not found in DB via direct query post-setup.");
            }

            // Verify Order
            try (PreparedStatement pstmt = verificationConn.prepareStatement("SELECT COUNT(*) FROM ORDER_ENTITY WHERE orderID = ?")) {
                pstmt.setString(1, testOrder.getOrderId());
                ResultSet rs = pstmt.executeQuery();
                assertTrue(rs.next(), "ResultSet expected for order verification query.");
                int count = rs.getInt(1);
                assertEquals(1, count, "Test order '" + testOrder.getOrderId() + "' not found in DB via direct query post-setup.");
            }

            // Verify PaymentMethod
            try (PreparedStatement pstmt = verificationConn.prepareStatement("SELECT COUNT(*) FROM PAYMENT_METHOD WHERE paymentMethodID = ?")) {
                pstmt.setString(1, testPaymentMethod.getPaymentMethodId());
                ResultSet rs = pstmt.executeQuery();
                assertTrue(rs.next(), "ResultSet expected for payment method verification query.");
                int count = rs.getInt(1);
                assertEquals(1, count, "Test payment method '" + testPaymentMethod.getPaymentMethodId() + "' not found in DB via direct query post-setup.");
            }
        } // verificationConn is closed here, testConnection remains open.
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        SQLiteConnector.getInstance().closeConnection(); // Close the connector's connection
        System.clearProperty("TEST_DB_URL"); // Clear system property if it was set
    }

    @BeforeEach
    void setUp() throws SQLException {
        // Clean up payment transactions before each test
        try (Connection conn = SQLiteConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM PAYMENT_TRANSACTION;");
        }
    }

    private PaymentTransaction createTestTransaction(String id, String status, TransactionType type) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(id);
        
        // Use the fully fetched entities from @BeforeAll
        transaction.setOrder(testOrder); // testOrder is fetched in @BeforeAll
        transaction.setPaymentMethod(testPaymentMethod); // testPaymentMethod is fetched in @BeforeAll
        
        transaction.setTransactionDateTime(LocalDateTime.now());
        // Assuming testOrder has totalAmountPaid correctly set and fetched.
        transaction.setAmount(testOrder.getTotalAmountPaid()); 
        transaction.setTransactionStatus(status);
        transaction.setTransactionType(type);
        transaction.setTransactionContent("Test content for " + id);
        return transaction;
    }

    @Test
    void testAddAndGetById() throws SQLException {
        PaymentTransaction transaction = createTestTransaction("trans1", "PENDING", TransactionType.PAYMENT);
        paymentTransactionDAO.add(transaction);

        PaymentTransaction fetched = paymentTransactionDAO.getById("trans1");
        assertNotNull(fetched);
        assertEquals("trans1", fetched.getTransactionId());
        assertNotNull(fetched.getOrder());
        assertEquals(testOrder.getOrderId(), fetched.getOrder().getOrderId());
        assertEquals(testPaymentMethod.getPaymentMethodId(), fetched.getPaymentMethod().getPaymentMethodId());
        assertEquals("PENDING", fetched.getTransactionStatus());
        assertEquals(100.00, fetched.getAmount(), 0.001);
        assertEquals(TransactionType.PAYMENT, fetched.getTransactionType());
    }

    @Test
    void testGetById_NotFound() throws SQLException {
        PaymentTransaction fetched = paymentTransactionDAO.getById("nonexistent");
        assertNull(fetched);
    }

    @Test
    void testGetAll() throws SQLException {
        PaymentTransaction transaction1 = createTestTransaction("trans_all1", "COMPLETED", TransactionType.PAYMENT);
        PaymentTransaction transaction2 = createTestTransaction("trans_all2", "PENDING", TransactionType.REFUND);
        paymentTransactionDAO.add(transaction1);
        paymentTransactionDAO.add(transaction2);

        List<PaymentTransaction> transactions = paymentTransactionDAO.getAll();
        assertEquals(2, transactions.size());
        assertTrue(transactions.stream().anyMatch(t -> t.getTransactionId().equals("trans_all1")));
        assertTrue(transactions.stream().anyMatch(t -> t.getTransactionId().equals("trans_all2")));
    }
    
    @Test
    void testGetAll_Empty() throws SQLException {
        List<PaymentTransaction> transactions = paymentTransactionDAO.getAll();
        assertTrue(transactions.isEmpty());
    }

    @Test
    void testGetByOrderId() throws SQLException {
        PaymentTransaction transaction1 = createTestTransaction("trans_order1_1", "SUCCESS", TransactionType.PAYMENT);
        paymentTransactionDAO.add(transaction1);

        UserAccount otherUser = new UserAccount("otherU", "otherU", "p", "o@e.com", UserStatus.ACTIVE);
        userAccountDAO.add(otherUser);
        OrderEntity otherOrder = new OrderEntity();
        otherOrder.setOrderId("otherOrder1");
        otherOrder.setUserAccount(otherUser);
        otherOrder.setOrderDate(LocalDateTime.now());
        otherOrder.setOrderStatus(OrderStatus.PENDING_PROCESSING);
        otherOrder.setTotalAmountPaid(50.0f);
        orderEntityDAO.add(otherOrder);
        
        PaymentMethod otherPm = new PaymentMethod("otherPM", PaymentMethodType.DOMESTIC_DEBIT_CARD, otherUser, true);
        paymentMethodDAO.add(otherPm);

        PaymentTransaction transaction2 = new PaymentTransaction();
        transaction2.setTransactionId("trans_order2_1");
        transaction2.setOrder(otherOrder);
        transaction2.setPaymentMethod(otherPm);
        transaction2.setTransactionDateTime(LocalDateTime.now());
        transaction2.setAmount(otherOrder.getTotalAmountPaid());
        transaction2.setTransactionStatus("PENDING");
        transaction2.setTransactionType(TransactionType.PAYMENT);
        transaction2.setTransactionContent("Other order content");
        paymentTransactionDAO.add(transaction2);


        List<PaymentTransaction> orderTransactions = paymentTransactionDAO.getByOrderId(testOrder.getOrderId());
        assertEquals(1, orderTransactions.size());
        assertEquals("trans_order1_1", orderTransactions.get(0).getTransactionId());
    }
    
    @Test
    void testGetByOrderId_NotFound() throws SQLException {
         List<PaymentTransaction> orderTransactions = paymentTransactionDAO.getByOrderId("non_existent_order");
         assertTrue(orderTransactions.isEmpty());
    }
    
    @Test
    void testGetByTransactionType() throws SQLException {
        PaymentTransaction transaction1 = createTestTransaction("trans_type1", "COMPLETED", TransactionType.PAYMENT);
        paymentTransactionDAO.add(transaction1);

        PaymentTransaction transaction2 = createTestTransaction("trans_type2", "PENDING", TransactionType.REFUND);
        paymentTransactionDAO.add(transaction2);
        
        PaymentTransaction transaction3 = createTestTransaction("trans_type3", "SUCCESS", TransactionType.PAYMENT);
        paymentTransactionDAO.add(transaction3);

        List<PaymentTransaction> paymentTransactions = paymentTransactionDAO.getByTransactionType(TransactionType.PAYMENT);
        assertEquals(2, paymentTransactions.size());
        assertTrue(paymentTransactions.stream().allMatch(t -> t.getTransactionType() == TransactionType.PAYMENT));
        assertTrue(paymentTransactions.stream().anyMatch(t -> t.getTransactionId().equals("trans_type1")));
        assertTrue(paymentTransactions.stream().anyMatch(t -> t.getTransactionId().equals("trans_type3")));


        List<PaymentTransaction> refundTransactions = paymentTransactionDAO.getByTransactionType(TransactionType.REFUND);
        assertEquals(1, refundTransactions.size());
        assertEquals("trans_type2", refundTransactions.get(0).getTransactionId());
    }

    @Test
    void testGetByDateRange() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        PaymentTransaction transaction1 = createTestTransaction("trans_date1", "SUCCESS", TransactionType.PAYMENT);
        transaction1.setTransactionDateTime(now.minusDays(1)); // Yesterday
        paymentTransactionDAO.add(transaction1);

        PaymentTransaction transaction2 = createTestTransaction("trans_date2", "PENDING", TransactionType.PAYMENT);
        transaction2.setTransactionDateTime(now); // Today
        paymentTransactionDAO.add(transaction2);

        PaymentTransaction transaction3 = createTestTransaction("trans_date3", "COMPLETED", TransactionType.REFUND);
        transaction3.setTransactionDateTime(now.plusDays(1)); // Tomorrow
        paymentTransactionDAO.add(transaction3);
        
        List<PaymentTransaction> todayTransactions = paymentTransactionDAO.getByDateRange(now.withHour(0).withMinute(0), now.withHour(23).withMinute(59));
        assertEquals(1, todayTransactions.size());
        assertEquals("trans_date2", todayTransactions.get(0).getTransactionId());

        List<PaymentTransaction> allThreeDays = paymentTransactionDAO.getByDateRange(now.minusDays(1).withHour(0).withMinute(0), now.plusDays(1).withHour(23).withMinute(59));
        assertEquals(3, allThreeDays.size());
    }
    
    @Test
    void testUpdateStatus() throws SQLException {
        PaymentTransaction transaction = createTestTransaction("trans_upd_stat", "PENDING", TransactionType.PAYMENT);
        paymentTransactionDAO.add(transaction);

        paymentTransactionDAO.updateStatus("trans_upd_stat", "SUCCESS", "ext_id_123");

        PaymentTransaction updated = paymentTransactionDAO.getById("trans_upd_stat");
        assertNotNull(updated);
        assertEquals("SUCCESS", updated.getTransactionStatus());
        assertEquals("ext_id_123", updated.getExternalTransactionId());
    }
}
