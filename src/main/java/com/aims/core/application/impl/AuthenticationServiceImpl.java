package com.aims.core.application.impl; // Or com.aims.core.application.services.impl;

import com.aims.core.application.services.IAuthenticationService;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.Role;
import com.aims.core.enums.UserStatus;
import com.aims.core.enums.UserRole; // Assuming your UserRole enum has ADMIN, PRODUCT_MANAGER
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import com.aims.core.infrastructure.database.dao.IUserRoleAssignmentDAO;
import com.aims.core.shared.utils.PasswordUtils; // You need to create this utility
import com.aims.core.shared.exceptions.AuthenticationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;

import java.sql.SQLException;
import java.util.Set;
// import java.util.UUID; // If generating session tokens

public class AuthenticationServiceImpl implements IAuthenticationService {

    private final IUserAccountDAO userAccountDAO;
    private final IUserRoleAssignmentDAO userRoleAssignmentDAO;
    // private final ISessionManager sessionManager; // Optional: For more complex session management

    public AuthenticationServiceImpl(IUserAccountDAO userAccountDAO,
                                     IUserRoleAssignmentDAO userRoleAssignmentDAO
                                     /*, ISessionManager sessionManager */) {
        this.userAccountDAO = userAccountDAO;
        this.userRoleAssignmentDAO = userRoleAssignmentDAO;
        // this.sessionManager = sessionManager;
    }

    @Override
    public UserAccount login(String username, String plainTextPassword)
            throws AuthenticationException, SQLException, ResourceNotFoundException {
        if (username == null || username.trim().isEmpty() || plainTextPassword == null || plainTextPassword.isEmpty()) {
            throw new AuthenticationException("Username and password are required.");
        }

        UserAccount user = userAccountDAO.getByUsername(username);
        if (user == null) {
            // To prevent username enumeration, some systems always throw a generic "Invalid credentials"
            // instead of "User not found". For AIMS admin/manager login, "User not found" might be acceptable.
            throw new ResourceNotFoundException("User '" + username + "' not found.");
        }

        if (!PasswordUtils.verifyPassword(plainTextPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }

        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("User account is not active. Current status: " + user.getUserStatus());
        }

        // Check if user has an administrative or product manager role
        Set<Role> roles = userRoleAssignmentDAO.getRolesByUserId(user.getUserId());
        boolean isAuthorizedRole = roles.stream().anyMatch(role ->
                UserRole.ADMIN.name().equalsIgnoreCase(role.getRoleId()) ||
                UserRole.PRODUCT_MANAGER.name().equalsIgnoreCase(role.getRoleId())
        );

        if (!isAuthorizedRole) {
            throw new AuthenticationException("User does not have sufficient privileges for this application.");
        }

        // If using session management, create and return a session token/object here
        // String sessionId = sessionManager.createSession(user.getUserId(), roles);
        // return new UserSessionDTO(sessionId, user, roles); // Example
        System.out.println("User " + user.getUsername() + " logged in successfully.");
        return user; // Return the user account upon successful authentication
    }

    @Override
    public void logout(String sessionId) {
        // If using server-side session management:
        // sessionManager.invalidateSession(sessionId);
        System.out.println("User with session/token " + sessionId + " logged out.");
        // For a simple desktop app, this might just clear a global current user state.
    }

    @Override
    public UserAccount validateSession(String sessionId) throws AuthenticationException {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new AuthenticationException("Session ID/User ID is invalid.");
        }
        
        try {
            UserAccount user = userAccountDAO.getById(sessionId);
            if (user == null) {
                throw new AuthenticationException("Session is invalid - user not found.");
            }
            
            if (user.getUserStatus() != UserStatus.ACTIVE) {
                throw new AuthenticationException("User account is not active. Current status: " + user.getUserStatus());
            }
            
            // Validate that user still has required roles
            Set<Role> roles = userRoleAssignmentDAO.getRolesByUserId(user.getUserId());
            boolean hasValidRole = roles.stream().anyMatch(role ->
                    UserRole.ADMIN.name().equalsIgnoreCase(role.getRoleId()) ||
                    UserRole.PRODUCT_MANAGER.name().equalsIgnoreCase(role.getRoleId())
            );
            
            if (!hasValidRole) {
                throw new AuthenticationException("User no longer has sufficient privileges for this application.");
            }
            
            System.out.println("Session validated successfully for user: " + user.getUsername());
            return user;
            
        } catch (SQLException e) {
            System.err.println("Database error during session validation: " + e.getMessage());
            throw new AuthenticationException("Error validating session: " + e.getMessage(), e);
        }
    }

    @Override
    public UserAccount getCurrentAuthenticatedUser(String sessionId) throws AuthenticationException {
        // This would typically call validateSession or get data from a session manager.
        return validateSession(sessionId); // Simple passthrough for now
    }
}