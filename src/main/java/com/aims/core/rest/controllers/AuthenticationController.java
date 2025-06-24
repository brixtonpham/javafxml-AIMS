package com.aims.core.rest.controllers;

import com.aims.core.application.services.IUserAccountService;
import com.aims.core.entities.UserAccount;
import com.aims.core.shared.ServiceFactory;
import com.aims.core.shared.exceptions.AuthenticationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class AuthenticationController extends BaseController {
    
    private final IUserAccountService userAccountService;
    
    public AuthenticationController() {
        this.userAccountService = ServiceFactory.getUserAccountService();
    }
    
    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        try {
            UserAccount user = userAccountService.login(request.getUsername(), request.getPassword());
            
            // Create response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", mapUserToResponse(user));
            responseData.put("token", generateToken(user)); // You'll need to implement JWT token generation
            responseData.put("expiresAt", java.time.LocalDateTime.now().plusHours(24).toString());
            
            return success(responseData, "Login successful");
            
        } catch (AuthenticationException e) {
            return error("Invalid credentials", org.springframework.http.HttpStatus.UNAUTHORIZED);
        } catch (SQLException e) {
            return error("Database error occurred", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ResourceNotFoundException e) {
            return error("User not found", org.springframework.http.HttpStatus.NOT_FOUND);
        }
    }
    
    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // In a real implementation, you'd invalidate the JWT token
        return success(null, "Logout successful");
    }
    
    /**
     * Get current user endpoint
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        try {
            // In a real implementation, you'd get user ID from JWT token
            String userId = getCurrentUserId(); // You'll need to implement this
            UserAccount user = userAccountService.getUserById(userId);
            
            return success(mapUserToResponse(user), "User retrieved successfully");
            
        } catch (SQLException e) {
            return error("Database error occurred", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return error("Unauthorized", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
    }
    
    /**
     * Validate session endpoint
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateSession() {
        try {
            // In a real implementation, you'd validate JWT token
            String userId = getCurrentUserId();
            UserAccount user = userAccountService.getUserById(userId);
            
            return success(mapUserToResponse(user), "Session valid");
            
        } catch (Exception e) {
            return error("Invalid session", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
    }
    
    /**
     * Change password endpoint
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            String userId = getCurrentUserId();
            userAccountService.changeOwnPassword(userId, request.getCurrentPassword(), request.getNewPassword());
            
            return success(null, "Password changed successfully");
            
        } catch (AuthenticationException e) {
            return error("Current password is incorrect", org.springframework.http.HttpStatus.UNAUTHORIZED);
        } catch (ValidationException e) {
            return error(e.getMessage(), org.springframework.http.HttpStatus.BAD_REQUEST);
        } catch (SQLException e) {
            return error("Database error occurred", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ResourceNotFoundException e) {
            return error("User not found", org.springframework.http.HttpStatus.NOT_FOUND);
        }
    }
    
    // Helper methods
    private Map<String, Object> mapUserToResponse(UserAccount user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getUserId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("fullName", user.getUsername()); // Use username as fallback for fullName
        userMap.put("status", user.getUserStatus().toString());
        userMap.put("roles", user.getRoleAssignments()); // Return role assignments
        userMap.put("createdAt", java.time.LocalDateTime.now().toString()); // Placeholder for createdAt
        userMap.put("lastLoginAt", java.time.LocalDateTime.now().toString()); // Placeholder for lastLoginAt
        return userMap;
    }
    
    private String generateToken(UserAccount user) {
        // TODO: Implement JWT token generation
        // For now, return a placeholder
        return "jwt_token_placeholder_" + user.getUserId();
    }
    
    private String getCurrentUserId() {
        // TODO: Extract user ID from JWT token in Authorization header
        // For now, return a placeholder
        return "current_user_id";
    }
    
    // Request DTOs
    public static class LoginRequest {
        private String username;
        private String password;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}