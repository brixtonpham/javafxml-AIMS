package com.aims.core.application.services;

import com.aims.core.entities.UserAccount;
import com.aims.core.shared.exceptions.AuthenticationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
// import com.aims.core.dtos.UserCredentialsDTO; // DTO for login credentials
// import com.aims.core.dtos.UserSessionDTO; // DTO to represent an authenticated session

import java.sql.SQLException;

/**
 * Service interface for handling user authentication and session management.
 */
public interface IAuthenticationService {

    /**
     * Authenticates a user (Admin or Product Manager) based on username and password.
     * Verifies credentials, checks user status, and ensures the user has an appropriate role
     * for accessing administrative/managerial functions.
     *
     * @param username The username provided for login.
     * @param plainTextPassword The plain text password provided for login.
     * @return UserAccount object representing the authenticated user if successful.
     * @throws AuthenticationException If authentication fails due to invalid credentials,
     * inactive/blocked account, or insufficient privileges.
     * @throws SQLException If a database error occurs during user retrieval.
     * @throws ResourceNotFoundException If the username does not exist.
     */
    UserAccount login(String username, String plainTextPassword) throws AuthenticationException, SQLException, ResourceNotFoundException;

    /**
     * Logs out the currently authenticated user.
     * This might involve invalidating a session token or clearing session data.
     *
     * @param sessionId The ID of the session to invalidate (if session management is implemented).
     * This could also be a userId if sessions are tied directly to users and not tokens.
     */
    void logout(String sessionId); // The implementation details will depend on session management strategy

    /**
     * Validates an existing session or token to confirm if the user is still authenticated.
     *
     * @param sessionId The session identifier or token to validate.
     * @return UserAccount object if the session is valid and active, otherwise null or throws AuthenticationException.
     * @throws AuthenticationException if the session is invalid or expired.
     */
    UserAccount validateSession(String sessionId) throws AuthenticationException;

    /**
     * Retrieves the currently authenticated user based on session information.
     * This is a convenience method that might use validateSession internally.
     *
     * @param sessionId The session identifier.
     * @return The UserAccount of the currently authenticated user, or null if no valid session.
     * @throws AuthenticationException if the session is invalid or expired.
     */
    UserAccount getCurrentAuthenticatedUser(String sessionId) throws AuthenticationException;
}