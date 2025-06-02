package com.aims.core.entities.pks;

import java.io.Serializable;
import java.util.Objects;

public class UserRoleAssignmentId implements Serializable {
    private String userAccount; // Tên trường phải khớp với tên trường entity UserAccount trong UserRoleAssignment
    private String role;      // Tên trường phải khớp với tên trường entity Role trong UserRoleAssignment

    public UserRoleAssignmentId() {
    }

    public UserRoleAssignmentId(String userId, String roleId) {
        this.userAccount = userId;
        this.role = roleId;
    }

    // Getters, Setters, equals, hashCode
    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRoleAssignmentId that = (UserRoleAssignmentId) o;
        return Objects.equals(userAccount, that.userAccount) &&
               Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userAccount, role);
    }
}
