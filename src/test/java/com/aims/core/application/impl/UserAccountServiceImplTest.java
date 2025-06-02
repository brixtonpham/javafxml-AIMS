package com.aims.core.application.impl;

import com.aims.core.application.services.INotificationService;
import com.aims.core.entities.Role;
import com.aims.core.entities.UserAccount;
import com.aims.core.enums.UserRole;
import com.aims.core.enums.UserStatus;
import com.aims.core.infrastructure.database.dao.IRoleDAO;
import com.aims.core.infrastructure.database.dao.IUserAccountDAO;
import com.aims.core.infrastructure.database.dao.IUserRoleAssignmentDAO;
import com.aims.core.shared.exceptions.AuthenticationException;
import com.aims.core.shared.exceptions.AuthorizationException;
import com.aims.core.shared.exceptions.ResourceNotFoundException;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.core.shared.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserAccountServiceImplTest {

    @Mock
    private IUserAccountDAO userAccountDAO;

    @Mock
    private IRoleDAO roleDAO;

    @Mock
    private IUserRoleAssignmentDAO userRoleAssignmentDAO;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private UserAccountServiceImpl userAccountService;

    private UserAccount sampleUser;
    private Role sampleRole;
    private String adminId = "admin-" + UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sampleUser = new UserAccount();
        sampleUser.setUserId("USR-" + UUID.randomUUID().toString());
        sampleUser.setUsername("testuser");
        sampleUser.setEmail("testuser@example.com");
        sampleUser.setPasswordHash("password123"); // Plain text for creation, will be hashed
        sampleUser.setUserStatus(UserStatus.ACTIVE);

        sampleRole = new Role();
        sampleRole.setRoleId(UserRole.CUSTOMER.name()); // Assuming CUSTOMER is a valid UserRole
        sampleRole.setRoleName("Customer");
    }

    @Test
    void createUser_success() throws SQLException, ValidationException, AuthorizationException {
        when(userAccountDAO.getByUsername(anyString())).thenReturn(null);
        when(userAccountDAO.getByEmail(anyString())).thenReturn(null);
        when(roleDAO.getById(anyString())).thenReturn(sampleRole);
        doNothing().when(userAccountDAO).add(any(UserAccount.class));
        doNothing().when(userRoleAssignmentDAO).assignRoleToUser(anyString(), anyString());

        Set<String> rolesToAssign = new HashSet<>();
        rolesToAssign.add(sampleRole.getRoleId());

        UserAccount createdUser = userAccountService.createUser(sampleUser, rolesToAssign, adminId);

        assertNotNull(createdUser);
        assertEquals(sampleUser.getUsername(), createdUser.getUsername());
        assertTrue(PasswordUtils.verifyPassword("password123", createdUser.getPasswordHash()));
        verify(userAccountDAO, times(1)).add(any(UserAccount.class));
        verify(userRoleAssignmentDAO, times(1)).assignRoleToUser(createdUser.getUserId(), sampleRole.getRoleId());
    }

    @Test
    void createUser_usernameExists_throwsValidationException() throws SQLException {
        when(userAccountDAO.getByUsername("testuser")).thenReturn(sampleUser);
        assertThrows(ValidationException.class, () -> userAccountService.createUser(sampleUser, Collections.emptySet(), adminId));
    }

    @Test
    void createUser_emailExists_throwsValidationException() throws SQLException {
        when(userAccountDAO.getByUsername(anyString())).thenReturn(null);
        when(userAccountDAO.getByEmail("testuser@example.com")).thenReturn(sampleUser);
        assertThrows(ValidationException.class, () -> userAccountService.createUser(sampleUser, Collections.emptySet(), adminId));
    }
     @Test
    void updateUser_success() throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException {
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        when(userAccountDAO.getByEmail(anyString())).thenReturn(null); // New email is not taken
        doNothing().when(userAccountDAO).update(any(UserAccount.class));

        UserAccount updatedDetails = new UserAccount();
        updatedDetails.setUserId(sampleUser.getUserId());
        updatedDetails.setUsername(sampleUser.getUsername()); // Username not changed
        updatedDetails.setEmail("newemail@example.com");
        updatedDetails.setUserStatus(UserStatus.SUSPENDED);


        UserAccount result = userAccountService.updateUser(updatedDetails, adminId);

        assertNotNull(result);
        assertEquals("newemail@example.com", result.getEmail());
        assertEquals(UserStatus.SUSPENDED, result.getUserStatus());
        verify(userAccountDAO, times(1)).update(any(UserAccount.class));
    }

    @Test
    void updateUser_userNotFound_throwsResourceNotFoundException() throws SQLException {
        when(userAccountDAO.getById(anyString())).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> userAccountService.updateUser(sampleUser, adminId));
    }


    @Test
    void deleteUser_success() throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException {
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        doNothing().when(userAccountDAO).delete(anyString());

        userAccountService.deleteUser(sampleUser.getUserId(), adminId);

        verify(userAccountDAO, times(1)).delete(sampleUser.getUserId());
    }


    @Test
    void getUserById_success() throws SQLException {
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        UserAccount foundUser = userAccountService.getUserById(sampleUser.getUserId());
        assertNotNull(foundUser);
        assertEquals(sampleUser.getUserId(), foundUser.getUserId());
    }

    @Test
    void getUserByUsername_success() throws SQLException {
        when(userAccountDAO.getByUsername(sampleUser.getUsername())).thenReturn(sampleUser);
        UserAccount foundUser = userAccountService.getUserByUsername(sampleUser.getUsername());
        assertNotNull(foundUser);
        assertEquals(sampleUser.getUsername(), foundUser.getUsername());
    }

    @Test
    void blockUser_success() throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException {
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        doNothing().when(userAccountDAO).updateStatus(anyString(), eq(UserStatus.SUSPENDED));

        UserAccount blockedUser = userAccountService.blockUser(sampleUser.getUserId(), adminId);

        assertNotNull(blockedUser);
        assertEquals(UserStatus.SUSPENDED, blockedUser.getUserStatus());
        verify(userAccountDAO, times(1)).updateStatus(sampleUser.getUserId(), UserStatus.SUSPENDED);
        verify(notificationService, times(1)).sendUserStatusChangeNotification(any(UserAccount.class), eq("ACTIVE"), eq("SUSPENDED"), anyString());
    }


    @Test
    void unblockUser_success() throws SQLException, ResourceNotFoundException, AuthorizationException {
        sampleUser.setUserStatus(UserStatus.SUSPENDED); // Assume user is initially blocked
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        doNothing().when(userAccountDAO).updateStatus(anyString(), eq(UserStatus.ACTIVE));

        UserAccount unblockedUser = userAccountService.unblockUser(sampleUser.getUserId(), adminId);

        assertNotNull(unblockedUser);
        assertEquals(UserStatus.ACTIVE, unblockedUser.getUserStatus());
        verify(userAccountDAO, times(1)).updateStatus(sampleUser.getUserId(), UserStatus.ACTIVE);
        verify(notificationService, times(1)).sendUserStatusChangeNotification(any(UserAccount.class), eq("SUSPENDED"), eq("ACTIVE"), anyString());
    }

    @Test
    void resetPassword_success() throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException {
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        doNothing().when(userAccountDAO).updatePassword(anyString(), anyString());

        userAccountService.resetPassword(sampleUser.getUserId(), "newStrongPassword123", adminId);

        verify(userAccountDAO, times(1)).updatePassword(eq(sampleUser.getUserId()), anyString());
        verify(notificationService, times(1)).sendPasswordResetNotification(any(UserAccount.class), eq("newStrongPassword123"));
    }


    @Test
    void changeOwnPassword_success() throws SQLException, ResourceNotFoundException, AuthenticationException, ValidationException {
        // Simulate current password hash
        String oldPassword = "password123";
        sampleUser.setPasswordHash(PasswordUtils.hashPassword(oldPassword));
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        doNothing().when(userAccountDAO).updatePassword(anyString(), anyString());

        userAccountService.changeOwnPassword(sampleUser.getUserId(), oldPassword, "newSecurePassword456");

        verify(userAccountDAO, times(1)).updatePassword(eq(sampleUser.getUserId()), anyString());
        verify(notificationService, times(1)).sendPasswordChangedNotification(any(UserAccount.class));
    }

    @Test
    void changeOwnPassword_incorrectOldPassword_throwsAuthenticationException() throws SQLException {
        sampleUser.setPasswordHash(PasswordUtils.hashPassword("correctOldPassword"));
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);

        assertThrows(AuthenticationException.class, () ->
                userAccountService.changeOwnPassword(sampleUser.getUserId(), "wrongOldPassword", "newSecurePassword456")
        );
    }

    @Test
    void assignRoleToUser_success() throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException {
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        when(roleDAO.getById(sampleRole.getRoleId())).thenReturn(sampleRole);
        when(userRoleAssignmentDAO.checkUserHasRole(sampleUser.getUserId(), sampleRole.getRoleId())).thenReturn(false); // User does not have the role yet
        doNothing().when(userRoleAssignmentDAO).assignRoleToUser(anyString(), anyString());

        userAccountService.assignRoleToUser(sampleUser.getUserId(), sampleRole.getRoleId(), adminId);

        verify(userRoleAssignmentDAO, times(1)).assignRoleToUser(sampleUser.getUserId(), sampleRole.getRoleId());
    }

    @Test
    void assignRoleToUser_alreadyHasRole_throwsValidationException() throws SQLException {
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        when(roleDAO.getById(sampleRole.getRoleId())).thenReturn(sampleRole);
        when(userRoleAssignmentDAO.checkUserHasRole(sampleUser.getUserId(), sampleRole.getRoleId())).thenReturn(true); // User already has the role

        assertThrows(ValidationException.class, () ->
                userAccountService.assignRoleToUser(sampleUser.getUserId(), sampleRole.getRoleId(), adminId)
        );
    }


    @Test
    void removeRoleFromUser_success() throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException {
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        when(roleDAO.getById(sampleRole.getRoleId())).thenReturn(sampleRole);
        when(userRoleAssignmentDAO.checkUserHasRole(sampleUser.getUserId(), sampleRole.getRoleId())).thenReturn(true); // User has the role
        doNothing().when(userRoleAssignmentDAO).removeRoleFromUser(anyString(), anyString());

        userAccountService.removeRoleFromUser(sampleUser.getUserId(), sampleRole.getRoleId(), adminId);

        verify(userRoleAssignmentDAO, times(1)).removeRoleFromUser(sampleUser.getUserId(), sampleRole.getRoleId());
    }

    @Test
    void removeRoleFromUser_doesNotHaveRole_throwsValidationException() throws SQLException {
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        when(roleDAO.getById(sampleRole.getRoleId())).thenReturn(sampleRole);
        when(userRoleAssignmentDAO.checkUserHasRole(sampleUser.getUserId(), sampleRole.getRoleId())).thenReturn(false); // User does not have the role

        assertThrows(ValidationException.class, () ->
                userAccountService.removeRoleFromUser(sampleUser.getUserId(), sampleRole.getRoleId(), adminId)
        );
    }

    @Test
    void getUserRoles_success() throws SQLException, ResourceNotFoundException {
        Set<Role> roles = new HashSet<>();
        roles.add(sampleRole);
        when(userAccountDAO.getById(sampleUser.getUserId())).thenReturn(sampleUser);
        when(userRoleAssignmentDAO.getRolesByUserId(sampleUser.getUserId())).thenReturn(roles);

        Set<Role> resultRoles = userAccountService.getUserRoles(sampleUser.getUserId());

        assertNotNull(resultRoles);
        assertEquals(1, resultRoles.size());
        assertTrue(resultRoles.contains(sampleRole));
    }

    @Test
    void login_success_adminRole() throws AuthenticationException, SQLException, ResourceNotFoundException {
        sampleUser.setPasswordHash(PasswordUtils.hashPassword("password123"));
        when(userAccountDAO.getByUsername("testuser")).thenReturn(sampleUser);

        Role adminRole = new Role();
        adminRole.setRoleId(UserRole.ADMIN.name());
        adminRole.setRoleName("Admin");
        Set<Role> roles = new HashSet<>(Collections.singletonList(adminRole));
        when(userRoleAssignmentDAO.getRolesByUserId(sampleUser.getUserId())).thenReturn(roles);

        UserAccount loggedInUser = userAccountService.login("testuser", "password123");

        assertNotNull(loggedInUser);
        assertEquals(sampleUser.getUserId(), loggedInUser.getUserId());
    }

    @Test
    void login_success_productManagerRole() throws AuthenticationException, SQLException, ResourceNotFoundException {
        sampleUser.setPasswordHash(PasswordUtils.hashPassword("password123"));
        when(userAccountDAO.getByUsername("testuser")).thenReturn(sampleUser);

        Role pmRole = new Role();
        pmRole.setRoleId(UserRole.PRODUCT_MANAGER.name());
        pmRole.setRoleName("Product Manager");
        Set<Role> roles = new HashSet<>(Collections.singletonList(pmRole));
        when(userRoleAssignmentDAO.getRolesByUserId(sampleUser.getUserId())).thenReturn(roles);

        UserAccount loggedInUser = userAccountService.login("testuser", "password123");

        assertNotNull(loggedInUser);
        assertEquals(sampleUser.getUserId(), loggedInUser.getUserId());
    }


    @Test
    void login_invalidPassword_throwsAuthenticationException() throws SQLException {
        sampleUser.setPasswordHash(PasswordUtils.hashPassword("correctPassword"));
        when(userAccountDAO.getByUsername("testuser")).thenReturn(sampleUser);

        assertThrows(AuthenticationException.class, () ->
                userAccountService.login("testuser", "wrongPassword")
        );
    }

    @Test
    void login_userNotFound_throwsResourceNotFoundException() throws SQLException {
        when(userAccountDAO.getByUsername("unknownuser")).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () ->
                userAccountService.login("unknownuser", "password123")
        );
    }

    @Test
    void login_userNotActive_throwsAuthenticationException() throws SQLException {
        sampleUser.setUserStatus(UserStatus.SUSPENDED);
        sampleUser.setPasswordHash(PasswordUtils.hashPassword("password123"));
        when(userAccountDAO.getByUsername("testuser")).thenReturn(sampleUser);

        assertThrows(AuthenticationException.class, () ->
                userAccountService.login("testuser", "password123")
        );
    }

    @Test
    void login_noPrivilegedRole_throwsAuthenticationException() throws SQLException {
        sampleUser.setPasswordHash(PasswordUtils.hashPassword("password123"));
        when(userAccountDAO.getByUsername("testuser")).thenReturn(sampleUser);

        Role customerRole = new Role();
        customerRole.setRoleId(UserRole.CUSTOMER.name()); // Assuming CUSTOMER is not ADMIN or PRODUCT_MANAGER
        customerRole.setRoleName("Customer");
        Set<Role> roles = new HashSet<>(Collections.singletonList(customerRole));
        when(userRoleAssignmentDAO.getRolesByUserId(sampleUser.getUserId())).thenReturn(roles);

        assertThrows(AuthenticationException.class, () ->
                userAccountService.login("testuser", "password123")
        );
    }
}
