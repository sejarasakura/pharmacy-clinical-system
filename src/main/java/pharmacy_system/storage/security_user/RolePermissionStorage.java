package pharmacy_system.storage.security_user;

import pharmacy_system.model.security_user.RolePermission;
import java.util.List;
import java.util.Optional;

/**
 * Storage contract for RolePermission entities.
 * Supports CRUD operations and user-role join queries.
 * Enables role and permission lookups and user-role assignments.
 */
public interface RolePermissionStorage {

    /**
     * Creates and persists a new RolePermission.
     * @param rolePermission the RolePermission to create (must have roleName and permissionCodes set)
     * @return the created RolePermission with assigned roleId and version set to 1
     */
    RolePermission create(RolePermission rolePermission);

    /**
     * Retrieves a RolePermission by its unique ID.
     * @param roleId the role ID
     * @return Optional containing the RolePermission if found, empty otherwise
     */
    Optional<RolePermission> findById(long roleId);

    /**
     * Retrieves a RolePermission by role name (e.g., "Patient", "Doctor", "Pharmacist", "Administrator").
     * @param roleName the role name to search for
     * @return Optional containing the RolePermission if found, empty otherwise
     */
    Optional<RolePermission> findByRoleName(String roleName);

    /**
     * Updates an existing RolePermission with optimistic concurrency control.
     * @param rolePermission the updated RolePermission (must have version set to the current value)
     * @param expectedVersion the version at the time of the last read (must match current to succeed)
     * @return true if update succeeds, false if version conflict detected
     * @throws IllegalArgumentException if the role permission does not exist
     */
    boolean update(RolePermission rolePermission, long expectedVersion);

    /**
     * Deletes a RolePermission by ID.
     * @param roleId the role ID to delete
     * @return true if deletion succeeds, false if the role permission does not exist
     */
    boolean delete(long roleId);

    /**
     * Retrieves all RolePermissions.
     * @return a list of all RolePermissions in the system
     */
    List<RolePermission> listAll();

    /**
     * Retrieves all RolePermissions assigned to a specific user (join query).
     * @param userId the user ID to query
     * @return a list of RolePermissions assigned to this user
     */
    List<RolePermission> findRolesByUserId(long userId);

    /**
     * Assigns a role to a user (creates a user-role association).
     * @param userId the user ID
     * @param roleId the role ID to assign
     * @return true if the assignment succeeds, false if already assigned
     */
    boolean assignRoleToUser(long userId, long roleId);

    /**
     * Removes a role from a user (deletes a user-role association).
     * @param userId the user ID
     * @param roleId the role ID to remove
     * @return true if removal succeeds, false if the assignment does not exist
     */
    boolean removeRoleFromUser(long userId, long roleId);
}
