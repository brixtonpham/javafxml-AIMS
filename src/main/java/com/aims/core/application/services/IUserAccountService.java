package com.aims.core.application.services;

import com.aims.core.entities.UserAccount;
import com.aims.core.entities.Role;
import com.aims.core.shared.exceptions.AuthenticationException;
import com.aims.core.shared.exceptions.AuthorizationException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Service interface for managing user accounts, roles, and authentication.
 * This includes operations for administrators and product managers.
 */
public interface IUserAccountService {

    // --- User Account Management (Admin) ---

    /**
     * Creates a new user account (typically Admin or Product Manager).
     *
     * @param userAccount The UserAccount object to create (password should be pre-hashed or handled internally).
     * @param assignRoles A set of role IDs to assign to the new user.
     * @param adminId The ID of the admin performing this action (for auditing/authorization).
     * @return The created UserAccount.
     * @throws SQLException If a database error occurs.
     * @throws ValidationException If user data is invalid (e.g., username/email exists) or roles are invalid.
     * @throws AuthorizationException If the performing admin does not have rights.
     */
    UserAccount createUser(UserAccount userAccount, Set<String> assignRoleIds, String adminId) throws SQLException, ValidationException, AuthorizationException;

    /**
     * Updates an existing user's information (e.g., email, status - not password).
     *
     * @param userAccount The UserAccount object with updated details.
     * @param adminId The ID of the admin performing this action.
     * @return The updated UserAccount.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the user is not found.
     * @throws ValidationException If updated data is invalid.
     * @throws AuthorizationException If the performing admin does not have rights.
     */
    UserAccount updateUser(UserAccount userAccount, String adminId) throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException;

    /**
     * Deletes a user account.
     *
     * @param userIdToDelete The ID of the user to delete.
     * @param adminId The ID of the admin performing this action.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the user to delete is not found.
     * @throws AuthorizationException If the performing admin does not have rights.
     * @throws ValidationException If trying to delete a critical system account (e.g., the last admin).
     */
    void deleteUser(String userIdToDelete, String adminId) throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException;

    /**
     * Retrieves a user account by its ID.
     *
     * @param userId The ID of the user.
     * @return The UserAccount object or null if not found.
     * @throws SQLException If a database error occurs.
     */
    UserAccount getUserById(String userId) throws SQLException;

    /**
     * Retrieves a user account by its username.
     *
     * @param username The username.
     * @return The UserAccount object or null if not found.
     * @throws SQLException If a database error occurs.
     */
    UserAccount getUserByUsername(String username) throws SQLException;

    /**
     * Retrieves a list of all user accounts.
     * (Consider pagination for large numbers of users).
     * @param adminId The ID of the admin requesting the list (for authorization).
     * @return A list of UserAccount objects.
     * @throws SQLException If a database error occurs.
     * @throws AuthorizationException If the performing admin does not have rights.
     */
    List<UserAccount> getAllUsers(String adminId) throws SQLException, AuthorizationException;

    /**
     * Blocks a user account.
     *
     * @param userIdToBlock The ID of the user to block.
     * @param adminId The ID of the admin performing the action.
     * @return The updated UserAccount with status BLOCKED.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the user is not found.
     * @throws AuthorizationException If the performing admin does not have rights.
     * @throws ValidationException If trying to block self or a critical system account.
     */
    UserAccount blockUser(String userIdToBlock, String adminId) throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException;

    /**
     * Unblocks a user account.
     *
     * @param userIdToUnblock The ID of the user to unblock.
     * @param adminId The ID of the admin performing the action.
     * @return The updated UserAccount with status ACTIVE.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the user is not found.
     * @throws AuthorizationException If the performing admin does not have rights.
     */
    UserAccount unblockUser(String userIdToUnblock, String adminId) throws SQLException, ResourceNotFoundException, AuthorizationException;

    /**
     * Resets the password for a user (typically by an admin).
     * The new password might be auto-generated and sent via notification.
     *
     * @param userIdToReset The ID of the user whose password is to be reset.
     * @param adminId The ID of the admin performing the action.
     * @param newPassword The new plain text password (service will hash it).
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the user is not found.
     * @throws AuthorizationException If the performing admin does not have rights.
     * @throws ValidationException If the new password is weak.
     */
    void resetPassword(String userIdToReset, String newPassword, String adminId) throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException;


    // --- User Self-Service ---

    /**
     * Allows a logged-in user (Admin or Product Manager) to change their own password.
     *
     * @param userId The ID of the user changing their password.
     * @param oldPassword The user's current plain text password (for verification).
     * @param newPassword The new plain text password.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the user is not found.
     * @throws AuthenticationException If the old password does not match.
     * @throws ValidationException If the new password is weak.
     */
    void changeOwnPassword(String userId, String oldPassword, String newPassword) throws SQLException, ResourceNotFoundException, AuthenticationException, ValidationException;


    // --- Role Management (Admin) ---

    /**
     * Assigns a role to a user.
     *
     * @param userId The ID of the user.
     * @param roleId The ID of the role to assign.
     * @param adminId The ID of the admin performing the action.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the user or role is not found.
     * @throws ValidationException If the user already has the role.
     * @throws AuthorizationException If the performing admin does not have rights.
     */
    void assignRoleToUser(String userId, String roleId, String adminId) throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException;

    /**
     * Removes a role from a user.
     *
     * @param userId The ID of the user.
     * @param roleId The ID of the role to remove.
     * @param adminId The ID of the admin performing the action.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the user or role assignment is not found.
     * @throws ValidationException If trying to remove a critical role (e.g., last admin role from a user).
     * @throws AuthorizationException If the performing admin does not have rights.
     */
    void removeRoleFromUser(String userId, String roleId, String adminId) throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException;

    /**
     * Retrieves all roles assigned to a specific user.
     *
     * @param userId The ID of the user.
     * @return A set of Role objects.
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the user is not found.
     */
    Set<Role> getUserRoles(String userId) throws SQLException, ResourceNotFoundException;

    /**
     * Retrieves all available roles in the system.
     * @param adminId The ID of the admin requesting this (for authorization).
     * @return A list of all Role objects.
     * @throws SQLException If a database error occurs.
     * @throws AuthorizationException If the requester is not authorized.
     */
    List<Role> getAllRoles(String adminId) throws SQLException, AuthorizationException;


    // --- Authentication (Admin, Product Manager) ---

    /**
     * Authenticates a user (Admin or Product Manager) based on username and password.
     *
     * @param username The username.
     * @param plainTextPassword The plain text password.
     * @return The authenticated UserAccount object if credentials are valid and user is active.
     * @throws AuthenticationException If authentication fails (wrong credentials, user not active/blocked).
     * @throws SQLException If a database error occurs.
     * @throws ResourceNotFoundException If the user is not found.
     */
    UserAccount login(String username, String plainTextPassword) throws AuthenticationException, SQLException, ResourceNotFoundException;

}