package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Role;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.UserRoleAssignment;
import com.aims.core.enums.UserStatus; // Assuming UserStatus enum for UserAccount mapping
import org.springframework.stereotype.Repository;
import com.aims.core.infrastructure.database.SQLiteConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class UserRoleAssignmentDAOImpl implements IUserRoleAssignmentDAO {

    private Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }

    @Override
    public void assignRoleToUser(String userId, String roleId) throws SQLException {
        String sql = "INSERT INTO USER_ROLE_ASSIGNMENT (userID, roleID) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, roleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Specific error code for UNIQUE constraint violation in SQLite is 19
            if (e.getErrorCode() == 19 && e.getMessage().toLowerCase().contains("unique constraint failed")) {
                // Log or handle the case where the assignment already exists,
                // For now, we re-throw a more specific message or just the original.
                // System.out.println("Assignment for userID " + userId + " and roleID " + roleId + " already exists.");
            }
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void removeRoleFromUser(String userId, String roleId) throws SQLException {
        String sql = "DELETE FROM USER_ROLE_ASSIGNMENT WHERE userID = ? AND roleID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, roleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public Set<Role> getRolesByUserId(String userId) throws SQLException {
        Set<Role> roles = new HashSet<>();
        String sql = "SELECT r.roleID, r.roleName FROM ROLE r " +
                     "JOIN USER_ROLE_ASSIGNMENT ura ON r.roleID = ura.roleID " +
                     "WHERE ura.userID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Role role = new Role();
                role.setRoleId(rs.getString("roleID"));
                role.setRoleName(rs.getString("roleName"));
                roles.add(role);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return roles;
    }

    @Override
    public Set<UserAccount> getUsersByRoleId(String roleId) throws SQLException {
        Set<UserAccount> users = new HashSet<>();
        String sql = "SELECT ua.userID, ua.username, ua.password_hash, ua.email, ua.user_status FROM USER_ACCOUNT ua " +
                     "JOIN USER_ROLE_ASSIGNMENT ura ON ua.userID = ura.userID " +
                     "WHERE ura.roleID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roleId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UserAccount user = new UserAccount();
                user.setUserId(rs.getString("userID"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setEmail(rs.getString("email"));
                user.setUserStatus(UserStatus.valueOf(rs.getString("user_status")));
                users.add(user);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return users;
    }

    @Override
    public boolean checkUserHasRole(String userId, String roleId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM USER_ROLE_ASSIGNMENT WHERE userID = ? AND roleID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, roleId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return false;
    }

    @Override
    public List<UserRoleAssignment> getAllAssignments() throws SQLException {
        List<UserRoleAssignment> assignments = new ArrayList<>();
        String sql = "SELECT ura.userID, ura.roleID, u.username, u.password_hash, u.email, u.user_status, r.roleName " +
                     "FROM USER_ROLE_ASSIGNMENT ura " +
                     "JOIN USER_ACCOUNT u ON ura.userID = u.userID " +
                     "JOIN ROLE r ON ura.roleID = r.roleID";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                UserRoleAssignment assignment = new UserRoleAssignment();
                
                UserAccount user = new UserAccount();
                user.setUserId(rs.getString("userID"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setEmail(rs.getString("email"));
                user.setUserStatus(UserStatus.valueOf(rs.getString("user_status")));
                assignment.setUserAccount(user); // Corrected method name

                Role role = new Role();
                role.setRoleId(rs.getString("roleID"));
                role.setRoleName(rs.getString("roleName"));
                assignment.setRole(role); // Corrected method name
                
                assignments.add(assignment);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e; // Re-throw the exception to be handled by the caller
        }
        return assignments;
    }
}