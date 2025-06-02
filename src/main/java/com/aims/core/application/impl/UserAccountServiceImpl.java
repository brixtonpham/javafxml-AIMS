package com.aims.core.application.impl; // Or com.aims.core.application.services.impl;

import com.aims.core.application.services.IUserAccountService;
import com.aims.core.application.services.INotificationService;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.Role;
import com.aims.core.enums.UserStatus;
import com.aims.core.enums.UserRole; // Assuming UserRole enum for default roles if needed
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import com.aims.core.infrastructure.database.dao.IRoleDAO;
import com.aims.core.infrastructure.database.dao.IUserRoleAssignmentDAO;
import com.aims.core.shared.utils.PasswordUtils; // You need to create this utility
import com.aims.core.shared.exceptions.AuthenticationException;
import com.aims.core.shared.exceptions.AuthorizationException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserAccountServiceImpl implements IUserAccountService {

    private final IUserAccountDAO userAccountDAO;
    private final IRoleDAO roleDAO;
    private final IUserRoleAssignmentDAO userRoleAssignmentDAO;
    private final INotificationService notificationService;
    // private final PasswordUtils passwordUtils; // Injected or static access

    public UserAccountServiceImpl(IUserAccountDAO userAccountDAO,
                                  IRoleDAO roleDAO,
                                  IUserRoleAssignmentDAO userRoleAssignmentDAO,
                                  INotificationService notificationService
                                  /*, PasswordUtils passwordUtils */) {
        this.userAccountDAO = userAccountDAO;
        this.roleDAO = roleDAO;
        this.userRoleAssignmentDAO = userRoleAssignmentDAO;
        this.notificationService = notificationService;
        // this.passwordUtils = passwordUtils;
    }

    @Override
    public UserAccount createUser(UserAccount userAccount, Set<String> assignRoleIds, String adminId)
            throws SQLException, ValidationException, AuthorizationException {
        // TODO: Implement authorization check: Does adminId have rights to create users?

        if (userAccount == null || userAccount.getUsername() == null || userAccount.getEmail() == null || userAccount.getPasswordHash() == null) {
            throw new ValidationException("Username, email, and password (hash) are required to create a user.");
        }
        if (userAccountDAO.getByUsername(userAccount.getUsername()) != null) {
            throw new ValidationException("Username '" + userAccount.getUsername() + "' already exists.");
        }
        if (userAccountDAO.getByEmail(userAccount.getEmail()) != null) {
            throw new ValidationException("Email '" + userAccount.getEmail() + "' already exists.");
        }

        // Assuming password in userAccount.getPasswordHash() is already plain text from input
        // Service should be responsible for hashing it.
        String plainPassword = userAccount.getPasswordHash(); // Temporary: Assume DTO passes plain password here
        userAccount.setPasswordHash(PasswordUtils.hashPassword(plainPassword)); // Use your hashing utility

        if (userAccount.getUserId() == null || userAccount.getUserId().trim().isEmpty()) {
            userAccount.setUserId("USR-" + UUID.randomUUID().toString());
        }
        if (userAccount.getUserStatus() == null) {
            userAccount.setUserStatus(UserStatus.ACTIVE); // Default status
        }

        // // START TRANSACTION
        try {
            userAccountDAO.add(userAccount);

            if (assignRoleIds != null && !assignRoleIds.isEmpty()) {
                for (String roleId : assignRoleIds) {
                    Role role = roleDAO.getById(roleId);
                    if (role == null) {
                        // // ROLLBACK TRANSACTION
                        throw new ValidationException("Role with ID '" + roleId + "' not found. Cannot assign to user.");
                    }
                    userRoleAssignmentDAO.assignRoleToUser(userAccount.getUserId(), roleId);
                }
            }
            // // COMMIT TRANSACTION
        } catch (SQLException e) {
            // // ROLLBACK TRANSACTION
            throw e;
        }

        // Optionally send a welcome/activation email
        // notificationService.sendWelcomeEmail(userAccount, plainPassword); // If sending plain password is part of req. (not recommended)
        return userAccount;
    }

    @Override
    public UserAccount updateUser(UserAccount userAccount, String adminId)
            throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException {
        // TODO: Implement authorization check for adminId

        UserAccount existingUser = userAccountDAO.getById(userAccount.getUserId());
        if (existingUser == null) {
            throw new ResourceNotFoundException("User with ID " + userAccount.getUserId() + " not found.");
        }

        // Prevent changing username or password hash directly through this method
        // Password change should use dedicated methods. Username change might be disallowed or have specific rules.
        if (!existingUser.getUsername().equals(userAccount.getUsername())) {
            // Check if new username is taken if allowed to change
            if (userAccountDAO.getByUsername(userAccount.getUsername()) != null) {
                 throw new ValidationException("New username '" + userAccount.getUsername() + "' already taken.");
            }
        }
        if (!existingUser.getEmail().equals(userAccount.getEmail())) {
            if (userAccountDAO.getByEmail(userAccount.getEmail()) != null) {
                throw new ValidationException("New email '" + userAccount.getEmail() + "' already taken.");
            }
        }


        // Only update allowed fields: email, status.
        existingUser.setEmail(userAccount.getEmail()); // Assuming email can be updated
        if (userAccount.getUserStatus() != null) { // Only update status if provided
             existingUser.setUserStatus(userAccount.getUserStatus());
        }
        // Do NOT update password hash here. Use changePassword/resetPassword.
        // existingUser.setPasswordHash(userAccount.getPasswordHash());

        userAccountDAO.update(existingUser); // DAO update should only update fields it's designed to
        return existingUser;
    }

    @Override
    public void deleteUser(String userIdToDelete, String adminId)
            throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException {
        // TODO: Implement authorization check for adminId
        // TODO: Add logic to prevent deleting critical accounts (e.g., the last admin)

        UserAccount user = userAccountDAO.getById(userIdToDelete);
        if (user == null) {
            throw new ResourceNotFoundException("User with ID " + userIdToDelete + " not found.");
        }
        // Business rule: Cannot delete self?
        if (userIdToDelete.equals(adminId)) {
            throw new ValidationException("Cannot delete your own account via this method.");
        }

        // ON DELETE CASCADE on USER_ROLE_ASSIGNMENT will remove assignments.
        // ON DELETE SET NULL on CART/ORDER_ENTITY will handle those.
        userAccountDAO.delete(userIdToDelete);
        // notificationService.sendAccountDeletionNotification(user);
    }

    @Override
    public UserAccount getUserById(String userId) throws SQLException {
        return userAccountDAO.getById(userId);
    }

    @Override
    public UserAccount getUserByUsername(String username) throws SQLException {
        return userAccountDAO.getByUsername(username);
    }

    @Override
    public List<UserAccount> getAllUsers(String adminId) throws SQLException, AuthorizationException {
        // TODO: Implement authorization check for adminId
        return userAccountDAO.getAll();
    }

    @Override
    public UserAccount blockUser(String userIdToBlock, String adminId)
            throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException {
        // TODO: Implement authorization check for adminId
        if (userIdToBlock.equals(adminId)) {
            throw new ValidationException("Admin cannot block their own account.");
        }
        UserAccount user = userAccountDAO.getById(userIdToBlock);
        if (user == null) {
            throw new ResourceNotFoundException("User with ID " + userIdToBlock + " not found.");
        }
        UserStatus oldStatus = user.getUserStatus();
        userAccountDAO.updateStatus(userIdToBlock, UserStatus.SUSPENDED); // Using SUSPENDED for blocked
        user.setUserStatus(UserStatus.SUSPENDED); // Update in-memory object
        notificationService.sendUserStatusChangeNotification(user, oldStatus.name(), UserStatus.SUSPENDED.name(), "Account blocked by admin " + adminId);
        return user;
    }

    @Override
    public UserAccount unblockUser(String userIdToUnblock, String adminId)
            throws SQLException, ResourceNotFoundException, AuthorizationException {
        // TODO: Implement authorization check for adminId
        UserAccount user = userAccountDAO.getById(userIdToUnblock);
        if (user == null) {
            throw new ResourceNotFoundException("User with ID " + userIdToUnblock + " not found.");
        }
        UserStatus oldStatus = user.getUserStatus();
        userAccountDAO.updateStatus(userIdToUnblock, UserStatus.ACTIVE);
        user.setUserStatus(UserStatus.ACTIVE);
        notificationService.sendUserStatusChangeNotification(user, oldStatus.name(), UserStatus.ACTIVE.name(), "Account unblocked by admin " + adminId);
        return user;
    }

    @Override
    public void resetPassword(String userIdToReset, String newPassword, String adminId)
            throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException {
        // TODO: Implement authorization check for adminId
        UserAccount user = userAccountDAO.getById(userIdToReset);
        if (user == null) {
            throw new ResourceNotFoundException("User with ID " + userIdToReset + " not found.");
        }
        if (newPassword == null || newPassword.trim().length() < 8) { // Example: Basic password policy
            throw new ValidationException("New password must be at least 8 characters long.");
        }
        String newPasswordHash = PasswordUtils.hashPassword(newPassword);
        userAccountDAO.updatePassword(userIdToReset, newPasswordHash);
        notificationService.sendPasswordResetNotification(user, newPassword); // Sending newPassword in email is a security risk, better to send a reset link or one-time code.
    }

    @Override
    public void changeOwnPassword(String userId, String oldPassword, String newPassword)
            throws SQLException, ResourceNotFoundException, AuthenticationException, ValidationException {
        UserAccount user = userAccountDAO.getById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User with ID " + userId + " not found.");
        }
        if (!PasswordUtils.verifyPassword(oldPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Incorrect old password.");
        }
        if (newPassword == null || newPassword.trim().length() < 8) { // Example: Basic password policy
            throw new ValidationException("New password must be at least 8 characters long.");
        }
        if (PasswordUtils.verifyPassword(newPassword, user.getPasswordHash())) {
            throw new ValidationException("New password cannot be the same as the old password.");
        }

        String newPasswordHash = PasswordUtils.hashPassword(newPassword);
        userAccountDAO.updatePassword(userId, newPasswordHash);
        notificationService.sendPasswordChangedNotification(user);
    }

    @Override
    public void assignRoleToUser(String userId, String roleId, String adminId)
            throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException {
        // TODO: Implement authorization check for adminId
        UserAccount user = userAccountDAO.getById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User with ID " + userId + " not found.");
        }
        Role role = roleDAO.getById(roleId);
        if (role == null) {
            throw new ResourceNotFoundException("Role with ID " + roleId + " not found.");
        }
        if (userRoleAssignmentDAO.checkUserHasRole(userId, roleId)) {
            throw new ValidationException("User " + userId + " already has role " + roleId);
        }
        userRoleAssignmentDAO.assignRoleToUser(userId, roleId);
    }

    @Override
    public void removeRoleFromUser(String userId, String roleId, String adminId)
            throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException {
        // TODO: Implement authorization check for adminId
        // TODO: Add logic to prevent removing critical roles (e.g., last admin role from the system if applicable)
        UserAccount user = userAccountDAO.getById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User with ID " + userId + " not found.");
        }
        Role role = roleDAO.getById(roleId); // Check if role exists
        if (role == null) {
            throw new ResourceNotFoundException("Role with ID " + roleId + " not found.");
        }
        if (!userRoleAssignmentDAO.checkUserHasRole(userId, roleId)) {
            throw new ValidationException("User " + userId + " does not have role " + roleId);
        }
        userRoleAssignmentDAO.removeRoleFromUser(userId, roleId);
    }

    @Override
    public Set<Role> getUserRoles(String userId) throws SQLException, ResourceNotFoundException {
        UserAccount user = userAccountDAO.getById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User with ID " + userId + " not found.");
        }
        return userRoleAssignmentDAO.getRolesByUserId(userId);
    }

    @Override
    public List<Role> getAllRoles(String adminId) throws SQLException, AuthorizationException {
        // TODO: Implement authorization check for adminId
        return roleDAO.getAll();
    }

    @Override
    public UserAccount login(String username, String plainTextPassword)
            throws AuthenticationException, SQLException, ResourceNotFoundException {
        if (username == null || username.trim().isEmpty() || plainTextPassword == null || plainTextPassword.isEmpty()) {
            throw new AuthenticationException("Username and password are required.");
        }
        UserAccount user = userAccountDAO.getByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User '" + username + "' not found."); // Or AuthenticationException for security
        }
        if (!PasswordUtils.verifyPassword(plainTextPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("User account is not active. Status: " + user.getUserStatus());
        }

        // Check if user has an administrative role (ADMIN or PRODUCT_MANAGER)
        Set<Role> roles = userRoleAssignmentDAO.getRolesByUserId(user.getUserId());
        boolean isPrivileged = roles.stream().anyMatch(role ->
            role.getRoleId().equalsIgnoreCase(UserRole.ADMIN.name()) || // Assuming UserRole enum has ADMIN
            role.getRoleId().equalsIgnoreCase(UserRole.PRODUCT_MANAGER.name()) // Assuming UserRole enum has PRODUCT_MANAGER
        );

        if (!isPrivileged) {
            throw new AuthenticationException("User does not have sufficient privileges to log in to this system.");
        }

        return user;
    }
}