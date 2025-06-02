package com.aims.core.infrastructure.database.dao;

import com.aims.core.entities.Role;
import java.sql.SQLException;
import java.util.List;

public interface IRoleDAO {

    /**
     * Retrieves a Role from the database by its ID.
     *
     * @param roleId The ID of the role to retrieve.
     * @return The Role object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    Role getById(String roleId) throws SQLException;

    /**
     * Retrieves a Role from the database by its name.
     *
     * @param roleName The name of the role to retrieve.
     * @return The Role object if found, otherwise null.
     * @throws SQLException If a database access error occurs.
     */
    Role getByName(String roleName) throws SQLException;

    /**
     * Retrieves all Roles from the database.
     *
     * @return A list of all Role objects.
     * @throws SQLException If a database access error occurs.
     */
    List<Role> getAll() throws SQLException;

    /**
     * Adds a new Role to the database.
     *
     * @param role The Role object to add.
     * @throws SQLException If a database access error occurs or if roleId/roleName already exists.
     */
    void add(Role role) throws SQLException;

    /**
     * Updates an existing Role's information in the database.
     * Typically, only the roleName might be updatable if roleId is a fixed identifier.
     *
     * @param role The Role object with updated information.
     * @throws SQLException If a database access error occurs.
     */
    void update(Role role) throws SQLException;

    /**
     * Deletes a Role from the database by its ID.
     * Note: Consider database constraints. If roles are in use (assigned to users),
     * deletion might fail or require cascading actions defined in the database schema
     * or handled by the service layer (e.g., unassigning users first).
     *
     * @param roleId The ID of the role to delete.
     * @throws SQLException If a database access error occurs.
     */
    void delete(String roleId) throws SQLException;
}