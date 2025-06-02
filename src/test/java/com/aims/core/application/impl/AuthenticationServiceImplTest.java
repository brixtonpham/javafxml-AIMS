package com.aims.core.application.impl;

import com.aims.core.application.services.IAuthenticationService;
import com.aims.core.entities.UserAccount;
import com.aims.core.entities.Role;
import com.aims.core.enums.UserStatus;
import com.aims.core.enums.UserRole;
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import com.aims.core.infrastructure.database.dao.IUserRoleAssignmentDAO;
import com.aims.core.shared.utils.PasswordUtils;
import com.aims.core.shared.exceptions.AuthenticationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private IUserAccountDAO userAccountDAO;

    @Mock
    private IUserRoleAssignmentDAO userRoleAssignmentDAO;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private UserAccount mockUser;
    private Set<Role> mockRoles;

    @BeforeEach
    void setUp() {
        mockUser = new UserAccount();
        mockUser.setUserId("user123");
        mockUser.setUsername("testuser");
        mockUser.setPasswordHash(PasswordUtils.hashPassword("password123")); // Assume PasswordUtils is testable or use a fixed hash
        mockUser.setUserStatus(UserStatus.ACTIVE);

        mockRoles = new HashSet<>();
        Role adminRole = new Role();
        adminRole.setRoleId(UserRole.ADMIN.name());
        adminRole.setRoleName("Administrator");
        mockRoles.add(adminRole);
    }

    @Test
    void login_success() throws AuthenticationException, SQLException, ResourceNotFoundException {
        try (MockedStatic<PasswordUtils> mockedPasswordUtils = Mockito.mockStatic(PasswordUtils.class)) {
            when(userAccountDAO.getByUsername("testuser")).thenReturn(mockUser);
            mockedPasswordUtils.when(() -> PasswordUtils.verifyPassword("password123", mockUser.getPasswordHash())).thenReturn(true);
            when(userRoleAssignmentDAO.getRolesByUserId("user123")).thenReturn(mockRoles);

            UserAccount loggedInUser = authenticationService.login("testuser", "password123");

            assertNotNull(loggedInUser);
            assertEquals("testuser", loggedInUser.getUsername());
            verify(userAccountDAO).getByUsername("testuser");
            verify(userRoleAssignmentDAO).getRolesByUserId("user123");
        }
    }

    @Test
    void login_userNotFound() throws SQLException {
        when(userAccountDAO.getByUsername("unknownuser")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            authenticationService.login("unknownuser", "password");
        });
        verify(userAccountDAO).getByUsername("unknownuser");
    }

    @Test
    void login_invalidPassword() throws SQLException {
        try (MockedStatic<PasswordUtils> mockedPasswordUtils = Mockito.mockStatic(PasswordUtils.class)) {
            when(userAccountDAO.getByUsername("testuser")).thenReturn(mockUser);
            mockedPasswordUtils.when(() -> PasswordUtils.verifyPassword("wrongpassword", mockUser.getPasswordHash())).thenReturn(false);

            assertThrows(AuthenticationException.class, () -> {
                authenticationService.login("testuser", "wrongpassword");
            });
            verify(userAccountDAO).getByUsername("testuser");
        }
    }

    @Test
    void login_userNotActive() throws SQLException {
        mockUser.setUserStatus(UserStatus.INACTIVE);
        when(userAccountDAO.getByUsername("testuser")).thenReturn(mockUser);
         try (MockedStatic<PasswordUtils> mockedPasswordUtils = Mockito.mockStatic(PasswordUtils.class)) {
            mockedPasswordUtils.when(() -> PasswordUtils.verifyPassword("password123", mockUser.getPasswordHash())).thenReturn(true);

            assertThrows(AuthenticationException.class, () -> {
                authenticationService.login("testuser", "password123");
            });
            verify(userAccountDAO).getByUsername("testuser");
        }
    }

    @Test
    void login_insufficientPrivileges() throws SQLException {
        Set<Role> insufficientRoles = new HashSet<>();
        Role userRole = new Role();
        userRole.setRoleId(UserRole.CUSTOMER.name()); // Assuming CUSTOMER is not an admin/manager role
        insufficientRoles.add(userRole);

        try (MockedStatic<PasswordUtils> mockedPasswordUtils = Mockito.mockStatic(PasswordUtils.class)) {
            when(userAccountDAO.getByUsername("testuser")).thenReturn(mockUser);
            mockedPasswordUtils.when(() -> PasswordUtils.verifyPassword("password123", mockUser.getPasswordHash())).thenReturn(true);
            when(userRoleAssignmentDAO.getRolesByUserId("user123")).thenReturn(insufficientRoles);

            assertThrows(AuthenticationException.class, () -> {
                authenticationService.login("testuser", "password123");
            });
            verify(userAccountDAO).getByUsername("testuser");
            verify(userRoleAssignmentDAO).getRolesByUserId("user123");
        }
    }
    
    @Test
    void login_nullUsername() {
        assertThrows(AuthenticationException.class, () -> {
            authenticationService.login(null, "password");
        });
    }

    @Test
    void login_emptyPassword() {
        assertThrows(AuthenticationException.class, () -> {
            authenticationService.login("testuser", "");
        });
    }

    @Test
    void logout_success() {
        // Simple logout, may not have much to assert if it just prints a message
        // or clears a static variable not controlled by this service instance.
        // If it involved a session manager mock, we'd verify interactions.
        assertDoesNotThrow(() -> authenticationService.logout("session123"));
    }

    @Test
    void validateSession_success() throws SQLException, AuthenticationException {
        when(userAccountDAO.getById("user123")).thenReturn(mockUser);
        UserAccount validatedUser = authenticationService.validateSession("user123");
        assertNotNull(validatedUser);
        assertEquals("user123", validatedUser.getUserId());
        verify(userAccountDAO).getById("user123");
    }

    @Test
    void validateSession_userNotFound() throws SQLException {
        when(userAccountDAO.getById("unknownSession")).thenReturn(null);
        assertThrows(AuthenticationException.class, () -> {
            authenticationService.validateSession("unknownSession");
        });
        verify(userAccountDAO).getById("unknownSession");
    }
    
    @Test
    void validateSession_userNotActive() throws SQLException {
        mockUser.setUserStatus(UserStatus.INACTIVE);
        when(userAccountDAO.getById("user123")).thenReturn(mockUser);
        assertThrows(AuthenticationException.class, () -> {
            authenticationService.validateSession("user123");
        });
        verify(userAccountDAO).getById("user123");
    }

    @Test
    void validateSession_sqlException() throws SQLException {
        when(userAccountDAO.getById(anyString())).thenThrow(new SQLException("DB error"));
        assertThrows(AuthenticationException.class, () -> {
            authenticationService.validateSession("user123");
        });
    }
    
    @Test
    void validateSession_nullSessionId() {
        assertThrows(AuthenticationException.class, () -> {
            authenticationService.validateSession(null);
        });
    }

    @Test
    void getCurrentAuthenticatedUser_callsValidate() throws AuthenticationException, SQLException {
        // This test assumes getCurrentAuthenticatedUser directly calls validateSession.
        // We can spy on the service to verify this internal call.
        AuthenticationServiceImpl spyService = Mockito.spy(authenticationService);
        when(userAccountDAO.getById("user123")).thenReturn(mockUser); // Needed for the validateSession call

        spyService.getCurrentAuthenticatedUser("user123");
        verify(spyService).validateSession("user123"); // Verifies that validateSession was called by getCurrentAuthenticatedUser
    }
}
