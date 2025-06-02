package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.UserAccount;
import com.aims.core.entities.Role; // Cần thiết nếu có phương thức liên quan đến Role
import com.aims.core.enums.UserStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface IUserAccountDAO {

    /**
     * Retrieves a UserAccount from the database by its ID.
     *
     * @param userId The ID of the user to retrieve.
     * @return The UserAccount object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    UserAccount getById(String userId) throws SQLException;

    /**
     * Retrieves a UserAccount from the database by its username.
     *
     * @param username The username of the user to retrieve.
     * @return The UserAccount object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    UserAccount getByUsername(String username) throws SQLException;

    /**
     * Retrieves a UserAccount from the database by its email.
     *
     * @param email The email of the user to retrieve.
     * @return The UserAccount object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    UserAccount getByEmail(String email) throws SQLException;

    /**
     * Retrieves all UserAccounts from the database.
     *
     * @return A list of all UserAccount objects.
     * @throws SQLException If a database access error occurs.
     */
    List<UserAccount> getAll() throws SQLException;

    /**
     * Adds a new UserAccount to the database.
     *
     * @param userAccount The UserAccount object to add.
     * @throws SQLException If a database access error occurs or if username/email already exists.
     */
    void add(UserAccount userAccount) throws SQLException;

    /**
     * Updates an existing UserAccount's information in the database.
     * This typically excludes password changes, which should have a dedicated method.
     *
     * @param userAccount The UserAccount object with updated information.
     * @throws SQLException If a database access error occurs.
     */
    void update(UserAccount userAccount) throws SQLException;

    /**
     * Updates the password for a given user.
     *
     * @param userId          The ID of the user whose password is to be updated.
     * @param newPasswordHash The new hashed password.
     * @throws SQLException If a database access error occurs.
     */
    void updatePassword(String userId, String newPasswordHash) throws SQLException;

    /**
     * Updates the status of a user (e.g., ACTIVE, BLOCKED).
     *
     * @param userId    The ID of the user whose status is to be updated.
     * @param newStatus The new status for the user.
     * @throws SQLException If a database access error occurs.
     */
    void updateStatus(String userId, UserStatus newStatus) throws SQLException;

    /**
     * Deletes a UserAccount from the database by its ID.
     *
     * @param userId The ID of the user to delete.
     * @throws SQLException If a database access error occurs.
     */
    void delete(String userId) throws SQLException;

    /**
     * Retrieves all roles associated with a specific user.
     *
     * @param userId The ID of the user.
     * @return A set of Role objects associated with the user.
     * @throws SQLException If a database access error occurs.
     */
    Set<Role> getUserRoles(String userId) throws SQLException;

    /**
     * Adds a role to a user.
     *
     * @param userId The ID of the user.
     * @param roleId The ID of the role to add.
     * @throws SQLException If a database access error occurs (e.g., user or role not found, or assignment already exists).
     */
    void addUserRole(String userId, String roleId) throws SQLException;

    /**
     * Removes a role from a user.
     *
     * @param userId The ID of the user.
     * @param roleId The ID of the role to remove.
     * @throws SQLException If a database access error occurs.
     */
    void removeUserRole(String userId, String roleId) throws SQLException;
}