package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Role;
import com.aims.core.entities.UserAccount;
import com.aims.core.enums.UserStatus;
import com.aims.core.infrastructure.database.SQLiteConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class UserAccountDAOImplTest {

    private static final String TEST_DB_URL_PREFIX = "jdbc:sqlite:";
    private static String testDbUrl;
    private IUserAccountDAO userAccountDAO;
    private IRoleDAO roleDAO; // For setting up roles
    private Connection connection;

    @TempDir
    static Path sharedTempDir; // Shared temporary directory for all tests in this class

    private static Path dbFile;

    @BeforeAll
    static void beforeAll() throws IOException, SQLException {
        // Setup a single database file for all tests in this class for efficiency
        dbFile = sharedTempDir.resolve("aims_user_test.db");
        testDbUrl = TEST_DB_URL_PREFIX + dbFile.toAbsolutePath().toString();
        System.setProperty("TEST_DB_URL", testDbUrl); // For SQLiteConnector

        // Initialize schema once
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
        // Each test gets a fresh connection and DAO instance
        // The database file itself is not recreated for each test, but cleaned up
        connection = DriverManager.getConnection(testDbUrl);
        SQLiteConnector.getInstance().setConnection(connection); // Inject test connection

        userAccountDAO = new UserAccountDAOImpl();
        roleDAO = new RoleDAOImpl(); // Assuming RoleDAOImpl exists and works

        // Clean relevant tables before each test
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM USER_ROLE_ASSIGNMENT;");
            stmt.executeUpdate("DELETE FROM USER_ACCOUNT;");
            stmt.executeUpdate("DELETE FROM ROLE;"); // Roles might be pre-populated or managed by tests
        }
        // Pre-populate common roles if needed for tests
        try {
            roleDAO.add(new Role("R001", "CUSTOMER"));
            roleDAO.add(new Role("R002", "ADMIN"));
        } catch (SQLException e) {
            // Ignore if roles already exist (e.g. if cleaning failed or not thorough)
            if (!e.getMessage().contains("UNIQUE constraint failed")) { // Crude check, improve if needed
                throw e;
            }
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        SQLiteConnector.getInstance().setConnection(null); // Reset connection in SQLiteConnector
    }

    private UserAccount createTestUser(String id, String username, String email) {
        return new UserAccount(id, username, "password123hash", email, UserStatus.ACTIVE);
    }

    @Test
    void testAddAndGetUserById() throws SQLException {
        UserAccount user = createTestUser("U001", "testuser1", "user1@example.com");
        userAccountDAO.add(user);

        UserAccount retrievedUser = userAccountDAO.getById("U001");
        assertNotNull(retrievedUser);
        assertEquals("testuser1", retrievedUser.getUsername());
        assertEquals("user1@example.com", retrievedUser.getEmail());
        assertEquals(UserStatus.ACTIVE, retrievedUser.getUserStatus());
    }

    @Test
    void testGetUserByUsername() throws SQLException {
        UserAccount user = createTestUser("U002", "testuser2", "user2@example.com");
        userAccountDAO.add(user);

        UserAccount retrievedUser = userAccountDAO.getByUsername("testuser2");
        assertNotNull(retrievedUser);
        assertEquals("U002", retrievedUser.getUserId());
    }

    @Test
    void testGetUserByEmail() throws SQLException {
        UserAccount user = createTestUser("U003", "testuser3", "user3@example.com");
        userAccountDAO.add(user);

        UserAccount retrievedUser = userAccountDAO.getByEmail("user3@example.com");
        assertNotNull(retrievedUser);
        assertEquals("U003", retrievedUser.getUserId());
    }

    @Test
    void testGetAllUsers() throws SQLException {
        userAccountDAO.add(createTestUser("U004", "user4", "user4@example.com"));
        userAccountDAO.add(createTestUser("U005", "user5", "user5@example.com"));

        List<UserAccount> users = userAccountDAO.getAll();
        assertEquals(2, users.size());
    }

    @Test
    void testUpdateUser() throws SQLException {
        UserAccount user = createTestUser("U006", "user6", "user6@example.com");
        userAccountDAO.add(user);

        user.setEmail("updated_user6@example.com");
        user.setUserStatus(UserStatus.SUSPENDED);
        userAccountDAO.update(user);

        UserAccount updatedUser = userAccountDAO.getById("U006");
        assertEquals("updated_user6@example.com", updatedUser.getEmail());
        assertEquals(UserStatus.SUSPENDED, updatedUser.getUserStatus());
    }

    @Test
    void testUpdatePassword() throws SQLException {
        UserAccount user = createTestUser("U007", "user7", "user7@example.com");
        userAccountDAO.add(user);

        String newPasswordHash = "newSecurePasswordHash";
        userAccountDAO.updatePassword("U007", newPasswordHash);

        UserAccount updatedUser = userAccountDAO.getById("U007");
        assertEquals(newPasswordHash, updatedUser.getPasswordHash());
    }
    
    @Test
    void testUpdateStatus() throws SQLException {
        UserAccount user = createTestUser("U008", "user8", "user8@example.com");
        userAccountDAO.add(user);

        userAccountDAO.updateStatus("U008", UserStatus.DELETED);

        UserAccount updatedUser = userAccountDAO.getById("U008");
        assertEquals(UserStatus.DELETED, updatedUser.getUserStatus());
    }

    @Test
    void testDeleteUser() throws SQLException {
        UserAccount user = createTestUser("U009", "user9", "user9@example.com");
        userAccountDAO.add(user);
        userAccountDAO.addUserRole("U009", "R001"); // Add a role assignment

        assertNotNull(userAccountDAO.getById("U009"));
        userAccountDAO.delete("U009");
        assertNull(userAccountDAO.getById("U009"));
        assertTrue(userAccountDAO.getUserRoles("U009").isEmpty(), "Roles should be deleted by cascade or DAO logic");
    }

    @Test
    void testAddAndGetUserRoles() throws SQLException {
        UserAccount user = createTestUser("U010", "user10_roles", "user10@example.com");
        userAccountDAO.add(user);

        userAccountDAO.addUserRole("U010", "R001"); // CUSTOMER
        userAccountDAO.addUserRole("U010", "R002"); // ADMIN

        Set<Role> roles = userAccountDAO.getUserRoles("U010");
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.stream().anyMatch(r -> "CUSTOMER".equals(r.getRoleName())));
        assertTrue(roles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleName())));
    }

    @Test
    void testRemoveUserRole() throws SQLException {
        UserAccount user = createTestUser("U011", "user11_roles_remove", "user11@example.com");
        userAccountDAO.add(user);
        userAccountDAO.addUserRole("U011", "R001");
        userAccountDAO.addUserRole("U011", "R002");

        userAccountDAO.removeUserRole("U011", "R001");

        Set<Role> roles = userAccountDAO.getUserRoles("U011");
        assertEquals(1, roles.size());
        assertTrue(roles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleName())));
        assertFalse(roles.stream().anyMatch(r -> "CUSTOMER".equals(r.getRoleName())));
    }
    
    @Test
    void testAddExistingUserRole_ShouldThrowExceptionOrIgnore() throws SQLException {
        UserAccount user = createTestUser("U012", "user12_duplicate_role", "user12@example.com");
        userAccountDAO.add(user);
        userAccountDAO.addUserRole("U012", "R001");

        // Adding the same role again. SQLite default behavior for UNIQUE constraint is to throw SQLException.
        // The DAO should either catch this or let it propagate.
        assertThrows(SQLException.class, () -> {
            userAccountDAO.addUserRole("U012", "R001");
        }, "Adding a duplicate role assignment should throw SQLException due to UNIQUE constraint.");
    }
}
