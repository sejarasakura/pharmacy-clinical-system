package pharmacy_system.model.security_user;

import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 * Represents an operational role and its associated permission set.
 * One of four roles: Patient, Doctor, Pharmacist, Administrator.
 * Each role carries a fixed set of permission codes that are evaluated at runtime.
 * 
 * [UCD-04, UCD-06]
 */
public class RolePermission {
    private long roleId;
    private String roleName;  // Patient, Doctor, Pharmacist, Administrator
    private String description;
    private Set<String> permissionCodes;
    private boolean active;
    private long version;

    // Constructor
    public RolePermission(long roleId, String roleName, Set<String> permissionCodes, boolean active) {
        this(roleId, roleName, null, permissionCodes, active, 0L);
    }

    public RolePermission(long roleId, String roleName, Set<String> permissionCodes, boolean active, long version) {
        this(roleId, roleName, null, permissionCodes, active, version);
    }

    public RolePermission(long roleId, String roleName, String description,
                          Set<String> permissionCodes, boolean active) {
        this(roleId, roleName, description, permissionCodes, active, 0L);
    }

    public RolePermission(String roleName, Set<String> permissionCodes, boolean active) {
        this(0L, roleName, null, permissionCodes, active, 0L);
    }

    private RolePermission(long roleId, String roleName, String description,
                           Set<String> permissionCodes, boolean active, long version) {
        this.roleId = roleId;
        this.roleName = Objects.requireNonNull(roleName, "roleName must not be null");
        this.description = description;
        this.permissionCodes = new HashSet<>(Objects.requireNonNull(permissionCodes,
                "permissionCodes must not be null"));
        this.active = active;
        this.version = version;
    }

    // Getters
    public long getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() { return description; }

    public long getVersion() { return version; }

    public Set<String> getPermissionCodes() {
        return new HashSet<>(permissionCodes);
    }

    public boolean isActive() {
        return active;
    }

    // Business logic
    /**
     * Check if this role has a specific permission.
     * @param permissionCode the permission to check
     * @return true if this role includes the permission, false otherwise
     */
    public boolean hasPermission(String permissionCode) {
        if (!active || permissionCode == null) {
            return false;
        }
        return permissionCodes.contains(permissionCode);
    }

    // Setters
    public void setActive(boolean active) {
        this.active = active;
    }

    public void addPermission(String permissionCode) {
        if (permissionCode != null && !permissionCode.isBlank()) {
            permissionCodes.add(permissionCode);
        }
    }

    public void removePermission(String permissionCode) {
        permissionCodes.remove(permissionCode);
    }

    @Override
    public String toString() {
        return "RolePermission{" +
                "roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", active=" + active +
                ", permissionCount=" + permissionCodes.size() +
                '}';
    }
}
