package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Role;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoleDAOImplTest {

    private static final String TEST_DB_URL_PREFIX = "jdbc:sqlite:";
    private static String testDbUrl;
    private IRoleDAO roleDAO;
    private Connection connection;

    @TempDir
    static Path sharedTempDir; // Shared temporary directory for all tests in this class

    private static Path dbFile;

    @BeforeAll
    static void beforeAll() throws IOException, SQLException {
        dbFile = sharedTempDir.resolve("aims_role_test.db");
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
        connection = DriverManager.getConnection(testDbUrl);
        SQLiteConnector.getInstance().setConnection(connection); // Inject test connection
        roleDAO = new RoleDAOImpl();

        // Clean relevant tables before each test
        try (Statement stmt = connection.createStatement()) {
            // Order matters if there are foreign key constraints without ON DELETE CASCADE from USER_ROLE_ASSIGNMENT to ROLE
            // Assuming USER_ROLE_ASSIGNMENT.roleID has ON DELETE CASCADE from V1__create_tables.sql
            stmt.executeUpdate("DELETE FROM USER_ROLE_ASSIGNMENT;"); // Clear assignments first
            stmt.executeUpdate("DELETE FROM ROLE;");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        SQLiteConnector.getInstance().setConnection(null); // Reset connection in SQLiteConnector
    }

    @Test
    void testAddAndGetRoleById() throws SQLException {
        Role role = new Role("R001", "ADMIN");
        roleDAO.add(role);

        Role retrievedRole = roleDAO.getById("R001");
        assertNotNull(retrievedRole);
        assertEquals("ADMIN", retrievedRole.getRoleName());
    }

    @Test
    void testGetRoleByName() throws SQLException {
        Role role = new Role("R002", "CUSTOMER");
        roleDAO.add(role);

        Role retrievedRole = roleDAO.getByName("CUSTOMER");
        assertNotNull(retrievedRole);
        assertEquals("R002", retrievedRole.getRoleId());
    }

    @Test
    void testGetAllRoles() throws SQLException {
        roleDAO.add(new Role("R003", "MANAGER"));
        roleDAO.add(new Role("R004", "STAFF"));

        List<Role> roles = roleDAO.getAll();
        assertEquals(2, roles.size());
    }

    @Test
    void testUpdateRole() throws SQLException {
        Role role = new Role("R005", "GUEST_USER");
        roleDAO.add(role);

        role.setRoleName("LIMITED_GUEST");
        roleDAO.update(role);

        Role updatedRole = roleDAO.getById("R005");
        assertEquals("LIMITED_GUEST", updatedRole.getRoleName());
    }

    @Test
    void testDeleteRole() throws SQLException {
        Role role = new Role("R006", "TEMP_ROLE");
        roleDAO.add(role);

        assertNotNull(roleDAO.getById("R006"));
        roleDAO.delete("R006");
        assertNull(roleDAO.getById("R006"));
    }

    @Test
    void testAddDuplicateRoleId_ShouldThrowSQLException() {
        Role role1 = new Role("R007", "UNIQUE_ID_TEST_1");
        try {
            roleDAO.add(role1);
        } catch (SQLException e) {
            fail("First add should succeed: " + e.getMessage());
        }

        Role role2 = new Role("R007", "UNIQUE_ID_TEST_2");
        assertThrows(SQLException.class, () -> {
            roleDAO.add(role2);
        }, "Adding a role with a duplicate ID should throw SQLException.");
    }

    @Test
    void testAddDuplicateRoleName_ShouldThrowSQLException() {
        Role role1 = new Role("R008", "DUPLICATE_NAME_TEST");
        try {
            roleDAO.add(role1);
        } catch (SQLException e) {
            fail("First add should succeed: " + e.getMessage());
        }

        Role role2 = new Role("R009", "DUPLICATE_NAME_TEST");
        assertThrows(SQLException.class, () -> {
            roleDAO.add(role2);
        }, "Adding a role with a duplicate name should throw SQLException.");
    }
    
    @Test
    void testUpdateNonExistentRole_ShouldThrowSQLExceptionOrNotUpdate() throws SQLException {
        Role nonExistentRole = new Role("R999", "NON_EXISTENT");
        // The DAO's update method throws SQLException if no rows are affected.
        assertThrows(SQLException.class, () -> {
            roleDAO.update(nonExistentRole);
        });
    }

    @Test
    void testDeleteNonExistentRole() throws SQLException {
        // The DAO's delete method doesn't throw an error if the role doesn't exist, it simply does nothing.
        // This behavior is acceptable for many use cases.
        assertDoesNotThrow(() -> {
            roleDAO.delete("R888");
        });
        assertNull(roleDAO.getById("R888"));
    }
}
