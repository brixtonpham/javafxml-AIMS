package com.aims.core.application.dtos;

import com.aims.core.enums.UserStatus;
import java.util.Set;

public class UserRegistrationDTO {
    private String userId;
    private String username;
    private String password; // Plain text
    private String email;
    private Set<String> roleIds; // e.g., Set.of("ADMIN", "PRODUCT_MANAGER")
    private UserStatus status; // Optional, can default to ACTIVE in service

    public UserRegistrationDTO() {
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Set<String> getRoleIds() { return roleIds; }
    public void setRoleIds(Set<String> roleIds) { this.roleIds = roleIds; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
}