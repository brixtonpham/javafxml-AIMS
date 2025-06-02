package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Role;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.UserRoleAssignment; // Assuming this entity exists

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface IUserRoleAssignmentDAO {

    /**
     * Assigns a role to a user.
     *
     * @param userId The ID of the user.
     * @param roleId The ID of the role.
     * @throws SQLException If a database access error occurs or if the assignment already exists.
     */
    void assignRoleToUser(String userId, String roleId) throws SQLException;

    /**
     * Removes a role assignment from a user.
     *
     * @param userId The ID of the user.
     * @param roleId The ID of the role.
     * @throws SQLException If a database access error occurs.
     */
    void removeRoleFromUser(String userId, String roleId) throws SQLException;

    /**
     * Retrieves all roles assigned to a specific user.
     *
     * @param userId The ID of the user.
     * @return A Set of Role objects assigned to the user.
     * @throws SQLException If a database access error occurs.
     */
    Set<Role> getRolesByUserId(String userId) throws SQLException;

    /**
     * Retrieves all users assigned to a specific role.
     *
     * @param roleId The ID of the role.
     * @return A Set of UserAccount objects assigned to the role.
     * @throws SQLException If a database access error occurs.
     */
    Set<UserAccount> getUsersByRoleId(String roleId) throws SQLException;

    /**
     * Checks if a specific user has a specific role.
     *
     * @param userId The ID of the user.
     * @param roleId The ID of the role.
     * @return true if the user has the role, false otherwise.
     * @throws SQLException If a database access error occurs.
     */
    boolean checkUserHasRole(String userId, String roleId) throws SQLException;

    /**
     * Retrieves all UserRoleAssignment records.
     *
     * @return A list of all UserRoleAssignment objects.
     * @throws SQLException If a database access error occurs.
     */
    List<UserRoleAssignment> getAllAssignments() throws SQLException;
}