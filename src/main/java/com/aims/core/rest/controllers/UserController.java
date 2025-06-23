package com.aims.core.rest.controllers;

import com.aims.core.application.services.IUserAccountService;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.Role;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.exceptions.AuthenticationException;
import com.aims.core.shared.exceptions.AuthorizationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * REST controller for user account management operations
 */
@RestController
@RequestMapping("/api/users")

public class UserController extends BaseController {

    private final IUserAccountService userAccountService;

    public UserController() {
        this.userAccountService = ServiceFactory.getUserAccountService();
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserAccount>> getUserById(@PathVariable String userId) {
        try {
            UserAccount user = userAccountService.getUserById(userId);
            if (user == null) {
                return error("User not found", HttpStatus.NOT_FOUND);
            }
            return success(user, "User retrieved successfully");
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while retrieving user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<UserAccount>> getUserByUsername(@PathVariable String username) {
        try {
            UserAccount user = userAccountService.getUserByUsername(username);
            if (user == null) {
                return error("User not found", HttpStatus.NOT_FOUND);
            }
            return success(user, "User retrieved successfully");
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while retrieving user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all users (admin only)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<UserAccount>>> getAllUsers(
            @RequestParam String adminId) {
        try {
            List<UserAccount> users = userAccountService.getAllUsers(adminId);
            return success(users, "Users retrieved successfully");
        } catch (AuthorizationException e) {
            return error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while retrieving users: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create new user (admin only)
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<UserAccount>> createUser(@RequestBody CreateUserRequest request) {
        try {
            UserAccount user = userAccountService.createUser(
                request.getUserAccount(), 
                request.getAssignRoleIds(), 
                request.getAdminId()
            );
            return success(user, "User created successfully");
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("User creation failed", errors);
        } catch (AuthorizationException e) {
            return error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while creating user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update user (admin only)
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserAccount>> updateUser(
            @PathVariable String userId,
            @RequestBody UpdateUserRequest request) {
        try {
            // Set the user ID in the request object
            request.getUserAccount().setUserId(userId);
            UserAccount user = userAccountService.updateUser(request.getUserAccount(), request.getAdminId());
            return success(user, "User updated successfully");
        } catch (ResourceNotFoundException e) {
            return error("User not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("User update failed", errors);
        } catch (AuthorizationException e) {
            return error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while updating user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete user (admin only)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @PathVariable String userId,
            @RequestParam String adminId) {
        try {
            userAccountService.deleteUser(userId, adminId);
            return success("User deleted", "User deleted successfully");
        } catch (ResourceNotFoundException e) {
            return error("User not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("User deletion failed", errors);
        } catch (AuthorizationException e) {
            return error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while deleting user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Block user (admin only)
     */
    @PostMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<UserAccount>> blockUser(
            @PathVariable String userId,
            @RequestBody BlockUserRequest request) {
        try {
            UserAccount user = userAccountService.blockUser(userId, request.getAdminId());
            return success(user, "User blocked successfully");
        } catch (ResourceNotFoundException e) {
            return error("User not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("User blocking failed", errors);
        } catch (AuthorizationException e) {
            return error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while blocking user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Unblock user (admin only)
     */
    @PostMapping("/{userId}/unblock")
    public ResponseEntity<ApiResponse<UserAccount>> unblockUser(
            @PathVariable String userId,
            @RequestBody UnblockUserRequest request) {
        try {
            UserAccount user = userAccountService.unblockUser(userId, request.getAdminId());
            return success(user, "User unblocked successfully");
        } catch (ResourceNotFoundException e) {
            return error("User not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (AuthorizationException e) {
            return error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while unblocking user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reset user password (admin only)
     */
    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @PathVariable String userId,
            @RequestBody ResetPasswordRequest request) {
        try {
            userAccountService.resetPassword(userId, request.getNewPassword(), request.getAdminId());
            return success("Password reset", "Password reset successfully");
        } catch (ResourceNotFoundException e) {
            return error("User not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Password reset failed", errors);
        } catch (AuthorizationException e) {
            return error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while resetting password: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Change own password
     */
    @PostMapping("/{userId}/change-password")
    public ResponseEntity<ApiResponse<String>> changeOwnPassword(
            @PathVariable String userId,
            @RequestBody ChangePasswordRequest request) {
        try {
            userAccountService.changeOwnPassword(userId, request.getOldPassword(), request.getNewPassword());
            return success("Password changed", "Password changed successfully");
        } catch (ResourceNotFoundException e) {
            return error("User not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (AuthenticationException e) {
            return error("Authentication failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Password change failed", errors);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while changing password: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Assign role to user (admin only)
     */
    @PostMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<String>> assignRoleToUser(
            @PathVariable String userId,
            @PathVariable String roleId,
            @RequestParam String adminId) {
        try {
            userAccountService.assignRoleToUser(userId, roleId, adminId);
            return success("Role assigned", "Role assigned successfully");
        } catch (ResourceNotFoundException e) {
            return error("User or role not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Role assignment failed", errors);
        } catch (AuthorizationException e) {
            return error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while assigning role: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Remove role from user (admin only)
     */
    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<String>> removeRoleFromUser(
            @PathVariable String userId,
            @PathVariable String roleId,
            @RequestParam String adminId) {
        try {
            userAccountService.removeRoleFromUser(userId, roleId, adminId);
            return success("Role removed", "Role removed successfully");
        } catch (ResourceNotFoundException e) {
            return error("User or role assignment not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            Map<String, String> errors = new HashMap<>();
            errors.put("validation", e.getMessage());
            return validationError("Role removal failed", errors);
        } catch (AuthorizationException e) {
            return error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while removing role: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get user roles
     */
    @GetMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<Set<Role>>> getUserRoles(@PathVariable String userId) {
        try {
            Set<Role> roles = userAccountService.getUserRoles(userId);
            return success(roles, "User roles retrieved successfully");
        } catch (ResourceNotFoundException e) {
            return error("User not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while retrieving user roles: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all available roles (admin only)
     */
    @GetMapping("/roles/all")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles(@RequestParam String adminId) {
        try {
            List<Role> roles = userAccountService.getAllRoles(adminId);
            return success(roles, "Roles retrieved successfully");
        } catch (AuthorizationException e) {
            return error("Access denied: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred while retrieving roles: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * User login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserAccount>> login(@RequestBody LoginRequest request) {
        try {
            UserAccount user = userAccountService.login(request.getUsername(), request.getPassword());
            return success(user, "Login successful");
        } catch (AuthenticationException e) {
            return error("Authentication failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (ResourceNotFoundException e) {
            return error("User not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (SQLException e) {
            return error("Database error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("An error occurred during login: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Request DTOs
    public static class CreateUserRequest {
        private UserAccount userAccount;
        private Set<String> assignRoleIds;
        private String adminId;

        public UserAccount getUserAccount() { return userAccount; }
        public void setUserAccount(UserAccount userAccount) { this.userAccount = userAccount; }

        public Set<String> getAssignRoleIds() { return assignRoleIds; }
        public void setAssignRoleIds(Set<String> assignRoleIds) { this.assignRoleIds = assignRoleIds; }

        public String getAdminId() { return adminId; }
        public void setAdminId(String adminId) { this.adminId = adminId; }
    }

    public static class UpdateUserRequest {
        private UserAccount userAccount;
        private String adminId;

        public UserAccount getUserAccount() { return userAccount; }
        public void setUserAccount(UserAccount userAccount) { this.userAccount = userAccount; }

        public String getAdminId() { return adminId; }
        public void setAdminId(String adminId) { this.adminId = adminId; }
    }

    public static class BlockUserRequest {
        private String adminId;

        public String getAdminId() { return adminId; }
        public void setAdminId(String adminId) { this.adminId = adminId; }
    }

    public static class UnblockUserRequest {
        private String adminId;

        public String getAdminId() { return adminId; }
        public void setAdminId(String adminId) { this.adminId = adminId; }
    }

    public static class ResetPasswordRequest {
        private String newPassword;
        private String adminId;

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

        public String getAdminId() { return adminId; }
        public void setAdminId(String adminId) { this.adminId = adminId; }
    }

    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}