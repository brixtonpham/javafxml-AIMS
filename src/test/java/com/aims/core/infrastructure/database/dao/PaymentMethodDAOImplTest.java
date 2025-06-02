package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.PaymentMethod;
import com.aims.core.entities.UserAccount;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.enums.UserStatus;
import com.aims.core.infrastructure.database.SQLiteConnector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PaymentMethodDAOImplTest {

    private static IPaymentMethodDAO paymentMethodDAO;
    private static IUserAccountDAO userAccountDAO; 
    private static ICardDetailsDAO cardDetailsDAO; 
    private static UserAccount testUser;

    @TempDir
    static Path tempDir;
    static Path dbFile;

    @BeforeAll
    static void setUpAll() throws SQLException, IOException {
        dbFile = tempDir.resolve("test_aims_payment_method.db"); // Unique DB file name
        String localDbUrl = "jdbc:sqlite:" + dbFile.toAbsolutePath();

        // Use SQLiteConnector for connection management
        System.clearProperty("TEST_DB_URL"); 
        SQLiteConnector.getInstance().setConnection(null); // Reset any existing connection
        System.setProperty("TEST_DB_URL", localDbUrl);
        
        userAccountDAO = new UserAccountDAOImpl(); 
        cardDetailsDAO = mock(ICardDetailsDAO.class); 
        paymentMethodDAO = new PaymentMethodDAOImpl(userAccountDAO, cardDetailsDAO);

        try (Connection conn = SQLiteConnector.getInstance().getConnection()) { // Get connection from SQLiteConnector
            String schemaSql = new String(Files.readAllBytes(Path.of("src/main/resources/migration/V1__create_tables.sql"))); // Corrected schema path
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("PRAGMA foreign_keys = ON;");
                String cleanedSql = schemaSql.replaceAll("--.*\\\\R", "").replaceAll("/\\\\*[^*]*\\\\*+(?:[^/*][^*]*\\\\*+)*/", "");
                String[] statements = cleanedSql.split(";", -1); 
                for (String s : statements) {
                    String trimmedStatement = s.trim();
                    if (!trimmedStatement.isEmpty()) {
                        stmt.executeUpdate(trimmedStatement);
                    }
                }
            }
            // Setup a test user for foreign key constraints
            testUser = new UserAccount("pm_user1", "pm_testuser", "password123", "pm_test@example.com", UserStatus.ACTIVE);
            userAccountDAO.add(testUser);
        }
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        Connection connToClose = SQLiteConnector.getInstance().getConnection();
        if (connToClose != null && !connToClose.isClosed()) {
            connToClose.close();
        }
        SQLiteConnector.getInstance().setConnection(null);
        System.clearProperty("TEST_DB_URL");
    }

    @BeforeEach
    void setUp() throws SQLException {
        try (Connection conn = SQLiteConnector.getInstance().getConnection(); // Get connection from SQLiteConnector
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM PAYMENT_METHOD;");
        }
    }

    @Test
    void testAddAndGetById() throws SQLException {
        PaymentMethod method = new PaymentMethod("pm_123", PaymentMethodType.CREDIT_CARD, testUser, false);
        paymentMethodDAO.add(method);

        PaymentMethod fetchedMethod = paymentMethodDAO.getById("pm_123");
        assertNotNull(fetchedMethod);
        assertEquals("pm_123", fetchedMethod.getPaymentMethodId());
        assertEquals(PaymentMethodType.CREDIT_CARD, fetchedMethod.getMethodType());
        assertNotNull(fetchedMethod.getUserAccount());
        assertEquals(testUser.getUserId(), fetchedMethod.getUserAccount().getUserId());
        assertFalse(fetchedMethod.isDefault());
    }

    @Test
    void testGetById_NotFound() throws SQLException {
        PaymentMethod fetchedMethod = paymentMethodDAO.getById("non_existent_id");
        assertNull(fetchedMethod);
    }

    @Test
    void testGetAll() throws SQLException {
        PaymentMethod method1 = new PaymentMethod("pm_001", PaymentMethodType.CREDIT_CARD, testUser, true);
        PaymentMethod method2 = new PaymentMethod("pm_002", PaymentMethodType.DOMESTIC_DEBIT_CARD, testUser, false); // Changed to DOMESTIC_DEBIT_CARD
        paymentMethodDAO.add(method1);
        paymentMethodDAO.add(method2);

        List<PaymentMethod> methods = paymentMethodDAO.getAll();
        assertNotNull(methods);
        assertEquals(2, methods.size());

        assertTrue(methods.stream().anyMatch(pm -> pm.getPaymentMethodId().equals("pm_001")));
        assertTrue(methods.stream().anyMatch(pm -> pm.getPaymentMethodId().equals("pm_002")));
    }

    @Test
    void testGetAll_Empty() throws SQLException {
        List<PaymentMethod> methods = paymentMethodDAO.getAll();
        assertNotNull(methods);
        assertTrue(methods.isEmpty());
    }

    @Test
    void testUpdate() throws SQLException {
        PaymentMethod method = new PaymentMethod("pm_789", PaymentMethodType.DOMESTIC_DEBIT_CARD, testUser, false);
        paymentMethodDAO.add(method);

        method.setMethodType(PaymentMethodType.CREDIT_CARD);
        method.setDefault(true);
        paymentMethodDAO.update(method);

        PaymentMethod updatedMethod = paymentMethodDAO.getById("pm_789");
        assertNotNull(updatedMethod);
        assertEquals(PaymentMethodType.CREDIT_CARD, updatedMethod.getMethodType());
        assertTrue(updatedMethod.isDefault());
    }

    @Test
    void testUpdate_NotFound() throws SQLException { 
        PaymentMethod method = new PaymentMethod("pm_non_existent", PaymentMethodType.OTHER, testUser, false); // Changed to OTHER
        assertDoesNotThrow(() -> paymentMethodDAO.update(method));
        assertNull(paymentMethodDAO.getById("pm_non_existent"));
    }


    @Test
    void testDelete() throws SQLException {
        PaymentMethod method = new PaymentMethod("pm_to_delete", PaymentMethodType.OTHER, testUser, false); // Changed to OTHER
        paymentMethodDAO.add(method);

        assertNotNull(paymentMethodDAO.getById("pm_to_delete"));
        paymentMethodDAO.delete("pm_to_delete");
        assertNull(paymentMethodDAO.getById("pm_to_delete"));
    }

    @Test
    void testDelete_NotFound() throws SQLException { 
        assertDoesNotThrow(() -> paymentMethodDAO.delete("pm_non_existent_delete"));
    }
}
