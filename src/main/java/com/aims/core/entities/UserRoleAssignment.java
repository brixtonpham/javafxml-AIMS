package com.aims.core.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "USER_ROLE_ASSIGNMENT")
public class UserRoleAssignment implements Serializable {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID", referencedColumnName = "userID")
    private UserAccount userAccount;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleID", referencedColumnName = "roleID")
    private Role role;

    public UserRoleAssignment() {}

    public UserRoleAssignment(UserAccount userAccount, Role role) {
        this.userAccount = userAccount;
        this.role = role;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRoleAssignment that = (UserRoleAssignment) o;
        return Objects.equals(userAccount, that.userAccount) &&
               Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userAccount, role);
    }
}
