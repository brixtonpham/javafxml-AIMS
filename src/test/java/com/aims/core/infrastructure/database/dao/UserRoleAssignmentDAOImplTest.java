package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Role;
import com.aims.core.entities.UserAccount;
import com.aims.core.enums.UserStatus;
import com.aims.core.infrastructure.database.SQLiteConnector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List; // Added for List
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserRoleAssignmentDAOImplTest {

    private static IUserRoleAssignmentDAO userRoleAssignmentDAO;
    private static IUserAccountDAO userAccountDAO;
    private static IRoleDAO roleDAO;

    @TempDir
    static Path tempDir;
    static Path dbFile;

    private static UserAccount testUser1;
    private static UserAccount testUser2;
    private static Role testRole1;
    private static Role testRole2;

    @BeforeAll
    static void setUpAll() throws SQLException, IOException {
        dbFile = tempDir.resolve("test_aims_user_role.db");
        String localDbUrl = "jdbc:sqlite:" + dbFile.toAbsolutePath();

        System.clearProperty("TEST_DB_URL");
        SQLiteConnector.getInstance().setConnection(null);
        System.setProperty("TEST_DB_URL", localDbUrl);

        userAccountDAO = new UserAccountDAOImpl(); // Assuming UserAccountDAOImpl has a default constructor
        roleDAO = new RoleDAOImpl(); // Assuming RoleDAOImpl has a default constructor
        userRoleAssignmentDAO = new UserRoleAssignmentDAOImpl();

        try (Connection conn = SQLiteConnector.getInstance().getConnection()) {
            String schemaSql = new String(Files.readAllBytes(Path.of("src/main/resources/migration/V1__create_tables.sql")));
            try (Statement stmt = conn.createStatement()) {
                String cleanedSql = schemaSql.replaceAll("--.*\\R", "").replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");
                String[] statements = cleanedSql.split(";", -1);
                for (String s : statements) {
                    String trimmedStatement = s.trim();
                    if (!trimmedStatement.isEmpty()) {
                        stmt.executeUpdate(trimmedStatement);
                    }
                }
            }

            // Create test users
            testUser1 = new UserAccount("userRoleAssign1", "userAssign1", "pass1", "assign1@example.com", UserStatus.ACTIVE);
            userAccountDAO.add(testUser1);
            testUser1 = userAccountDAO.getById(testUser1.getUserId()); // Re-fetch

            testUser2 = new UserAccount("userRoleAssign2", "userAssign2", "pass2", "assign2@example.com", UserStatus.ACTIVE);
            userAccountDAO.add(testUser2);
            testUser2 = userAccountDAO.getById(testUser2.getUserId()); // Re-fetch

            // Create test roles
            testRole1 = new Role("ROLE_ASSIGN_1", "Test Role Assign 1");
            roleDAO.add(testRole1);
            testRole1 = roleDAO.getById(testRole1.getRoleId()); // Re-fetch

            testRole2 = new Role("ROLE_ASSIGN_2", "Test Role Assign 2");
            roleDAO.add(testRole2);
            testRole2 = roleDAO.getById(testRole2.getRoleId()); // Re-fetch

            assertNotNull(testUser1, "Test User 1 setup failed.");
            assertNotNull(testUser2, "Test User 2 setup failed.");
            assertNotNull(testRole1, "Test Role 1 setup failed.");
            assertNotNull(testRole2, "Test Role 2 setup failed.");
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
        try (Connection conn = SQLiteConnector.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM USER_ROLE_ASSIGNMENT;");
        }
    }

    @Test
    void testAssignRoleToUserAndGetRolesByUserId() throws SQLException {
        userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole1.getRoleId());
        userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole2.getRoleId());

        Set<Role> roles = userRoleAssignmentDAO.getRolesByUserId(testUser1.getUserId());
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.stream().anyMatch(r -> r.getRoleId().equals(testRole1.getRoleId())));
        assertTrue(roles.stream().anyMatch(r -> r.getRoleId().equals(testRole2.getRoleId())));
    }

    @Test
    void testAssignRoleToUser_AlreadyExists() throws SQLException {
        userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole1.getRoleId());
        // Attempting to assign the same role again should not throw a unique constraint violation
        // if the DAO handles it (e.g., by ignoring or logging).
        // The current UserRoleAssignmentDAOImpl re-throws, so we expect an SQLException.
        // If it were to handle it silently, this test would need to change.
        assertThrows(SQLException.class, () -> {
            userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole1.getRoleId());
        });
    }
    
    @Test
    void testAssignRoleToUser_NonExistentUser() throws SQLException {
        // Expect foreign key constraint violation
        assertThrows(SQLException.class, () -> {
            userRoleAssignmentDAO.assignRoleToUser("nonExistentUser", testRole1.getRoleId());
        });
    }

    @Test
    void testAssignRoleToUser_NonExistentRole() throws SQLException {
        // Expect foreign key constraint violation
        assertThrows(SQLException.class, () -> {
            userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), "nonExistentRole");
        });
    }


    @Test
    void testRemoveRoleFromUser() throws SQLException {
        userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole1.getRoleId());
        userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole2.getRoleId());

        userRoleAssignmentDAO.removeRoleFromUser(testUser1.getUserId(), testRole1.getRoleId());

        Set<Role> roles = userRoleAssignmentDAO.getRolesByUserId(testUser1.getUserId());
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertTrue(roles.stream().anyMatch(r -> r.getRoleId().equals(testRole2.getRoleId())));
        assertFalse(roles.stream().anyMatch(r -> r.getRoleId().equals(testRole1.getRoleId())));
    }

    @Test
    void testRemoveRoleFromUser_NotAssigned() throws SQLException {
        // Removing a role that isn't assigned should not throw an error
        // and should not change the state of other assignments.
        userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole1.getRoleId());
        userRoleAssignmentDAO.removeRoleFromUser(testUser1.getUserId(), testRole2.getRoleId()); // testRole2 not assigned

        Set<Role> roles = userRoleAssignmentDAO.getRolesByUserId(testUser1.getUserId());
        assertEquals(1, roles.size());
        assertTrue(roles.stream().anyMatch(r -> r.getRoleId().equals(testRole1.getRoleId())));
    }

    @Test
    void testGetUsersByRoleId() throws SQLException {
        userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole1.getRoleId());
        userRoleAssignmentDAO.assignRoleToUser(testUser2.getUserId(), testRole1.getRoleId());
        userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole2.getRoleId()); // User1 also has Role2

        Set<UserAccount> users = userRoleAssignmentDAO.getUsersByRoleId(testRole1.getRoleId());
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.getUserId().equals(testUser1.getUserId())));
        assertTrue(users.stream().anyMatch(u -> u.getUserId().equals(testUser2.getUserId())));
    }
    
    @Test
    void testGetRolesByUserId_NoRolesAssigned() throws SQLException {
        Set<Role> roles = userRoleAssignmentDAO.getRolesByUserId(testUser1.getUserId());
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    void testGetUsersByRoleId_NoUsersAssigned() throws SQLException {
        Set<UserAccount> users = userRoleAssignmentDAO.getUsersByRoleId(testRole1.getRoleId());
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void testCheckUserHasRole() throws SQLException {
        userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole1.getRoleId());

        assertTrue(userRoleAssignmentDAO.checkUserHasRole(testUser1.getUserId(), testRole1.getRoleId()));
        assertFalse(userRoleAssignmentDAO.checkUserHasRole(testUser1.getUserId(), testRole2.getRoleId()));
        assertFalse(userRoleAssignmentDAO.checkUserHasRole(testUser2.getUserId(), testRole1.getRoleId()));
    }
    
    @Test
    void testCheckUserHasRole_UserNotExists() throws SQLException {
         assertFalse(userRoleAssignmentDAO.checkUserHasRole("nonExistentUser", testRole1.getRoleId()));
    }

    @Test
    void testCheckUserHasRole_RoleNotExists() throws SQLException {
         assertFalse(userRoleAssignmentDAO.checkUserHasRole(testUser1.getUserId(), "nonExistentRole"));
    }

    @Test
    void testGetAllAssignments() throws SQLException {
        // Assign some roles
        userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole1.getRoleId());
        userRoleAssignmentDAO.assignRoleToUser(testUser2.getUserId(), testRole2.getRoleId());
        userRoleAssignmentDAO.assignRoleToUser(testUser1.getUserId(), testRole2.getRoleId()); // User1 has two roles

        List<com.aims.core.entities.UserRoleAssignment> assignments = userRoleAssignmentDAO.getAllAssignments();
        assertNotNull(assignments);
        assertEquals(3, assignments.size(), "Should retrieve all three assignments.");

        // Check if the assignments contain the expected user-role pairs
        assertTrue(assignments.stream().anyMatch(a -> 
            a.getUserAccount().getUserId().equals(testUser1.getUserId()) && 
            a.getRole().getRoleId().equals(testRole1.getRoleId())),
            "Missing assignment: User1 - Role1");

        assertTrue(assignments.stream().anyMatch(a -> 
            a.getUserAccount().getUserId().equals(testUser2.getUserId()) && 
            a.getRole().getRoleId().equals(testRole2.getRoleId())),
            "Missing assignment: User2 - Role2");
        
        assertTrue(assignments.stream().anyMatch(a -> 
            a.getUserAccount().getUserId().equals(testUser1.getUserId()) && 
            a.getRole().getRoleId().equals(testRole2.getRoleId())),
            "Missing assignment: User1 - Role2");
    }

    @Test
    void testGetAllAssignments_NoAssignments() throws SQLException {
        List<com.aims.core.entities.UserRoleAssignment> assignments = userRoleAssignmentDAO.getAllAssignments();
        assertNotNull(assignments);
        assertTrue(assignments.isEmpty(), "Should return an empty list when no assignments exist.");
    }
}
