package com.aims.core.entities;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "ROLE")
public class Role {

    @Id
    @Column(name = "roleID", length = 50) // Hoặc dùng @GeneratedValue nếu muốn ID tự tăng
    private String roleId; // e.g., "ADMIN", "PRODUCT_MANAGER", "CUSTOMER"

    @Column(name = "roleName", nullable = false, unique = true, length = 100)
    private String roleName; // e.g., "Administrator", "Product Manager", "Customer"
    
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<UserRoleAssignment> userAssignments = new HashSet<>();


    public Role() {
    }

    public Role(String roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
    }

    // Getters and Setters
    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public Set<UserRoleAssignment> getUserAssignments() {
        return userAssignments;
    }

    public void setUserAssignments(Set<UserRoleAssignment> userAssignments) {
        this.userAssignments = userAssignments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(roleId, role.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId);
    }

    @Override
    public String toString() {
        return "Role{" +
               "roleId='" + roleId + '\'' +
               ", roleName='" + roleName + '\'' +
               '}';
    }
}