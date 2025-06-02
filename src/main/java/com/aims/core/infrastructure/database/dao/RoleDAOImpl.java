package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Role;
import com.aims.core.infrastructure.database.SQLiteConnector;
import com.aims.core.infrastructure.database.dao.IRoleDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoleDAOImpl implements IRoleDAO {

    private Connection getConnection() throws SQLException {
        return SQLiteConnector.getInstance().getConnection();
    }

    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setRoleId(rs.getString("roleID"));
        role.setRoleName(rs.getString("roleName"));
        // If Role entity has a list of UserRoleAssignments and you want to eager load them,
        // you would query and populate that here, though it's often done in the service layer
        // or via separate DAO calls to avoid N+1 problems with simple DAOs.
        return role;
    }

    @Override
    public Role getById(String roleId) throws SQLException {
        String sql = "SELECT * FROM ROLE WHERE roleID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roleId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToRole(rs);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e; // Re-throw to allow service layer to handle
        }
        return null;
    }

    @Override
    public Role getByName(String roleName) throws SQLException {
        String sql = "SELECT * FROM ROLE WHERE roleName = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roleName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToRole(rs);
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return null;
    }

    @Override
    public List<Role> getAll() throws SQLException {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM ROLE";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                roles.add(mapResultSetToRole(rs));
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
        return roles;
    }

    @Override
    public void add(Role role) throws SQLException {
        String sql = "INSERT INTO ROLE (roleID, roleName) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role.getRoleId());
            pstmt.setString(2, role.getRoleName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            // Check for SQLite specific error code for UNIQUE constraint violation (e.g., 19 for SQLITE_CONSTRAINT)
            // if (e.getErrorCode() == 19 && e.getMessage().contains("UNIQUE constraint failed: ROLE.roleID")) {
            // throw new SQLException("Role ID '" + role.getRoleId() + "' already exists.", e.getSQLState(), e.getErrorCode(), e);
            // } else if (e.getErrorCode() == 19 && e.getMessage().contains("UNIQUE constraint failed: ROLE.roleName")) {
            // throw new SQLException("Role Name '" + role.getRoleName() + "' already exists.", e.getSQLState(), e.getErrorCode(), e);
            // }
            throw e;
        }
    }

    @Override
    public void update(Role role) throws SQLException {
        // Assuming roleID is the primary key and cannot be changed.
        // Only roleName can be updated.
        String sql = "UPDATE ROLE SET roleName = ? WHERE roleID = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role.getRoleName());
            pstmt.setString(2, role.getRoleId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating role failed, no rows affected. RoleID '" + role.getRoleId() + "' might not exist.");
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }

    @Override
    public void delete(String roleId) throws SQLException {
        // Deleting a role might be restricted if it's assigned to users.
        // The USER_ROLE_ASSIGNMENT table has ON DELETE CASCADE for roleID,
        // so assignments will be deleted. If ON DELETE RESTRICT was used, this would fail.
        String sql = "DELETE FROM ROLE WHERE roleID = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            // Optional: If you didn't have ON DELETE CASCADE for USER_ROLE_ASSIGNMENT.roleID
            // you might want to delete assignments first within a transaction.
            // String deleteAssignmentsSql = "DELETE FROM USER_ROLE_ASSIGNMENT WHERE roleID = ?";
            // try (PreparedStatement pstmtAssignments = conn.prepareStatement(deleteAssignmentsSql)) {
            //     pstmtAssignments.setString(1, roleId);
            //     pstmtAssignments.executeUpdate();
            // }

            try (PreparedStatement pstmtRole = conn.prepareStatement(sql)) {
                pstmtRole.setString(1, roleId);
                int affectedRows = pstmtRole.executeUpdate();
                if (affectedRows == 0) {
                    // This could mean the role didn't exist, which might not be an error in all cases.
                    // For now, we don't throw an error here, but logging could be useful.
                    // System.out.println("Role with ID '" + roleId + "' not found for deletion or already deleted.");
                }
            }
        } catch (SQLException e) {
            SQLiteConnector.printSQLException(e);
            throw e;
        }
    }
}