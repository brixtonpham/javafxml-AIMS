package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.CardDetails;
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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class CardDetailsDAOImplTest {

    private static final String TEST_DB_URL_PREFIX = "jdbc:sqlite:";
    private static String testDbUrl;
    private ICardDetailsDAO cardDetailsDAO;
    private IPaymentMethodDAO paymentMethodDAO; // For setting up prerequisite PaymentMethod
    private IUserAccountDAO userAccountDAO; // For setting up prerequisite UserAccount for PaymentMethod
    private Connection connection;

    @TempDir
    static Path sharedTempDir;
    private static Path dbFile;

    @BeforeAll
    static void beforeAll() throws IOException, SQLException {
        dbFile = sharedTempDir.resolve("aims_card_details_test.db");
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
        // PaymentMethodDAOImpl requires IUserAccountDAO and ICardDetailsDAO
        // We can pass the DAO under test (cardDetailsDAO) if it's already instantiated,
        // or a new instance if PaymentMethodDAOImpl doesn't modify it in a way that affects these tests.
        // For simplicity in setup, creating a new CardDetailsDAOImpl for PaymentMethodDAOImpl dependency.
        ICardDetailsDAO dependentCardDetailsDAO = new CardDetailsDAOImpl();
        paymentMethodDAO = new PaymentMethodDAOImpl(userAccountDAO, dependentCardDetailsDAO); 
        cardDetailsDAO = new CardDetailsDAOImpl(); // DAO under test

        // Clean relevant tables before each test
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM CARD_DETAILS;");
            stmt.executeUpdate("DELETE FROM PAYMENT_METHOD;");
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
        UserAccount user = new UserAccount(userId, "testuser" + userId, "password", "user@example.com", UserStatus.ACTIVE);
        userAccountDAO.add(user);
        return user;
    }

    private PaymentMethod setupTestPaymentMethod(String pmId, UserAccount user) throws SQLException {
        // Constructor from PaymentMethod.java: public PaymentMethod(String paymentMethodId, PaymentMethodType methodType, UserAccount userAccount, boolean isDefault)
        PaymentMethod pm = new PaymentMethod(pmId, PaymentMethodType.CREDIT_CARD, user, false /*isDefault*/);
        paymentMethodDAO.add(pm);
        return pm;
    }

    @Test
    void testAddAndGetCardDetails() throws SQLException {
        UserAccount user = setupTestUser("U_CD1");
        PaymentMethod pm = setupTestPaymentMethod("PM_CD1", user);

        CardDetails details = new CardDetails();
        details.setPaymentMethod(pm); 
        details.setCardholderName("John Doe");
        details.setCardNumberMasked("************1234");
        details.setExpiryDateMMYY("12/25");
        details.setIssuingBank("Test Bank"); // Now correct

        cardDetailsDAO.add(details);

        CardDetails retrievedDetails = cardDetailsDAO.getByPaymentMethodId(pm.getPaymentMethodId());
        assertNotNull(retrievedDetails);
        assertEquals("John Doe", retrievedDetails.getCardholderName());
        assertEquals("************1234", retrievedDetails.getCardNumberMasked());
        assertEquals("12/25", retrievedDetails.getExpiryDateMMYY());
        assertEquals("Test Bank", retrievedDetails.getIssuingBank()); // Now correct
        assertNotNull(retrievedDetails.getPaymentMethod());
        assertEquals(pm.getPaymentMethodId(), retrievedDetails.getPaymentMethod().getPaymentMethodId());
    }

    @Test
    void testAddCardDetails_NullValidFromDate() throws SQLException {
        UserAccount user = setupTestUser("U_CD_NullDate");
        PaymentMethod pm = setupTestPaymentMethod("PM_CD_NullDate", user);

        CardDetails details = new CardDetails();
        details.setPaymentMethod(pm);
        details.setCardholderName("Jane Doe");
        details.setCardNumberMasked("************5678");
        details.setExpiryDateMMYY("11/26");
        details.setValidFromDateMMYY(null); 
        details.setIssuingBank("Another Bank"); // Now correct

        cardDetailsDAO.add(details);

        CardDetails retrievedDetails = cardDetailsDAO.getByPaymentMethodId(pm.getPaymentMethodId());
        assertNotNull(retrievedDetails);
        assertEquals("Jane Doe", retrievedDetails.getCardholderName());
        assertNull(retrievedDetails.getValidFromDateMMYY());
    }
    
    @Test
    void testAddCardDetails_DuplicateThrowsException() throws SQLException {
        UserAccount user = setupTestUser("U_CD_Dup");
        PaymentMethod pm = setupTestPaymentMethod("PM_CD_Dup", user);

        CardDetails details1 = new CardDetails();
        details1.setPaymentMethod(pm);
        details1.setCardholderName("Duplicate Test");
        details1.setCardNumberMasked("************0000");
        details1.setExpiryDateMMYY("01/23");
        details1.setIssuingBank("Dup Bank"); // Now correct
        cardDetailsDAO.add(details1);

        CardDetails details2 = new CardDetails();
        details2.setPaymentMethod(pm); 
        details2.setCardholderName("Another Duplicate");
        details2.setCardNumberMasked("************1111");
        details2.setExpiryDateMMYY("02/24");
        details2.setIssuingBank("Dup Bank Again"); // Now correct

        SQLException exception = assertThrows(SQLException.class, () -> cardDetailsDAO.add(details2));
        assertTrue(exception.getMessage().contains("already exists"));
    }


    @Test
    void testUpdateCardDetails() throws SQLException {
        UserAccount user = setupTestUser("U_CD2");
        PaymentMethod pm = setupTestPaymentMethod("PM_CD2", user);

        CardDetails details = new CardDetails();
        details.setPaymentMethod(pm);
        details.setCardholderName("Initial Name");
        details.setCardNumberMasked("************1111");
        details.setExpiryDateMMYY("01/24");
        details.setValidFromDateMMYY("01/22");
        details.setIssuingBank("Initial Bank"); // Now correct
        cardDetailsDAO.add(details);

        details.setCardholderName("Updated Name");
        details.setExpiryDateMMYY("02/25");
        details.setValidFromDateMMYY(null); // Test update to null
        cardDetailsDAO.update(details);

        CardDetails updatedDetails = cardDetailsDAO.getByPaymentMethodId(pm.getPaymentMethodId());
        assertNotNull(updatedDetails);
        assertEquals("Updated Name", updatedDetails.getCardholderName());
        assertEquals("02/25", updatedDetails.getExpiryDateMMYY());
        assertNull(updatedDetails.getValidFromDateMMYY());
    }
    
    @Test
    void testUpdateCardDetails_NotFound() throws SQLException {
        // UserAccount user = setupTestUser("U_CD_UpNotFound"); // Unused variable
        // PaymentMethod pm = setupTestPaymentMethod("PM_CD_UpNotFound", user); // PM exists
        // No CardDetails added for this PM

        CardDetails detailsToUpdate = new CardDetails();
        PaymentMethod nonExistentPm = new PaymentMethod(); // Create a PM that doesn't exist in CARD_DETAILS
        nonExistentPm.setPaymentMethodId("PM_NON_EXISTENT_CD"); 
        detailsToUpdate.setPaymentMethod(nonExistentPm);
        detailsToUpdate.setCardholderName("Ghost User");
        detailsToUpdate.setCardNumberMasked("************9999");
        detailsToUpdate.setExpiryDateMMYY("12/99");

        SQLException exception = assertThrows(SQLException.class, () -> cardDetailsDAO.update(detailsToUpdate));
        assertTrue(exception.getMessage().contains("Updating card details failed, no record found"));
    }


    @Test
    void testDeleteCardDetails() throws SQLException {
        UserAccount user = setupTestUser("U_CD3");
        PaymentMethod pm = setupTestPaymentMethod("PM_CD3", user);

        CardDetails details = new CardDetails();
        details.setPaymentMethod(pm);
        details.setCardholderName("To Be Deleted");
        details.setCardNumberMasked("************2222");
        details.setExpiryDateMMYY("03/26");
        details.setIssuingBank("Delete Bank"); // Now correct
        cardDetailsDAO.add(details);

        assertNotNull(cardDetailsDAO.getByPaymentMethodId(pm.getPaymentMethodId()));
        cardDetailsDAO.delete(pm.getPaymentMethodId());
        assertNull(cardDetailsDAO.getByPaymentMethodId(pm.getPaymentMethodId()));
    }

    @Test
    void testGetByPaymentMethodId_NotFound() throws SQLException {
        assertNull(cardDetailsDAO.getByPaymentMethodId("PM_NON_EXISTENT"));
    }
}
