package com.aims.core.application.dtos;

import com.aims.core.enums.UserStatus;
import java.util.Set;

public class UserUpdateDTO {
    private String userId; // To identify which user to update
    private String email;  // Field that can be updated
    private UserStatus status; // Field that can be updated
    private Set<String> roleIdsToAssign; // Optional: new roles to assign
    private Set<String> roleIdsToRemove; // Optional: roles to remove

    public UserUpdateDTO() {
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public Set<String> getRoleIdsToAssign() { return roleIdsToAssign; }
    public void setRoleIdsToAssign(Set<String> roleIdsToAssign) { this.roleIdsToAssign = roleIdsToAssign; }

    public Set<String> getRoleIdsToRemove() { return roleIdsToRemove; }
    public void setRoleIdsToRemove(Set<String> roleIdsToRemove) { this.roleIdsToRemove = roleIdsToRemove; }
}