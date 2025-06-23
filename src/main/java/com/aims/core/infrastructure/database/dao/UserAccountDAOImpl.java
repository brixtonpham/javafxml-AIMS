package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.UserAccount;
import com.aims.core.enums.UserStatus;
import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public class UserAccountDAOImpl implements IUserAccountDAO {

    private Connection getConnection() throws SQLException {
        Connection conn = SQLiteConnector.getInstance().getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Database connection is closed or unavailable.");
        }
        return conn;
    }

    private UserAccount mapResultSetToUserAccount(ResultSet rs) throws SQLException {
        UserAccount user = new UserAccount();
        user.setUserId(rs.getString("userID"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setUserStatus(UserStatus.valueOf(rs.getString("user_status")));
        return user;
    }

    @Override
    public UserAccount getById(String userId) throws SQLException {
        String sql = "SELECT * FROM USER_ACCOUNT WHERE userID = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUserAccount(rs);
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return null;
    }

    @Override
    public UserAccount getByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM USER_ACCOUNT WHERE username = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUserAccount(rs);
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return null;
    }

    @Override
    public UserAccount getByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM USER_ACCOUNT WHERE email = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUserAccount(rs);
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return null;
    }

    @Override
    public List<UserAccount> getAll() throws SQLException {
        List<UserAccount> users = new ArrayList<>();
        String sql = "SELECT * FROM USER_ACCOUNT";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapResultSetToUserAccount(rs));
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return users;
    }

    @Override
    public void add(UserAccount user) throws SQLException {
        String sql = "INSERT INTO USER_ACCOUNT (userID, username, password_hash, email, user_status) VALUES (?, ?, ?, ?, ?)";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUserId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getUserStatus().name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void update(UserAccount user) throws SQLException {
        String sql = "UPDATE USER_ACCOUNT SET username = ?, password_hash = ?, email = ?, user_status = ? WHERE userID = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getUserStatus().name());
            pstmt.setString(5, user.getUserId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void delete(String userId) throws SQLException {
        String sql = "DELETE FROM USER_ACCOUNT WHERE userID = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void updatePassword(String userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE USER_ACCOUNT SET password_hash = ? WHERE userID = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void updateStatus(String userId, UserStatus newStatus) throws SQLException {
        String sql = "UPDATE USER_ACCOUNT SET user_status = ? WHERE userID = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.name());
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    // Implementations for Role related methods if they are part of IUserAccountDAO
    // For example:
    // @Override
    // public Set<Role> getRolesForUser(String userId) throws SQLException { ... }
    @Override
    public Set<com.aims.core.entities.Role> getUserRoles(String userId) throws SQLException {
        Set<com.aims.core.entities.Role> roles = new java.util.HashSet<>();
        String sql = "SELECT r.* FROM ROLE r JOIN USER_ROLE_ASSIGNMENT ura ON r.roleID = ura.roleID WHERE ura.userID = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    com.aims.core.entities.Role role = new com.aims.core.entities.Role();
                    role.setRoleId(rs.getString("roleID"));
                    role.setRoleName(rs.getString("roleName")); // Corrected column name
                    roles.add(role);
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return roles;
    }

    @Override
    public void addUserRole(String userId, String roleId) throws SQLException {
        String sql = "INSERT INTO USER_ROLE_ASSIGNMENT (userID, roleID) VALUES (?, ?)";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, roleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void removeUserRole(String userId, String roleId) throws SQLException {
        String sql = "DELETE FROM USER_ROLE_ASSIGNMENT WHERE userID = ? AND roleID = ?";
        Connection conn = getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, roleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }
}