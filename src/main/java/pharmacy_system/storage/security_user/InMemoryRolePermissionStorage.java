package pharmacy_system.storage.security_user;

import pharmacy_system.model.security_user.RolePermission;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of RolePermissionStorage using HashMap.
 * Thread-safe with atomic ID generation and version-checked updates.
 * Maintains user-role associations as a join table.
 */
public class InMemoryRolePermissionStorage implements RolePermissionStorage {
    private final Map<Long, RolePermission> store = new ConcurrentHashMap<>();
    private final Map<String, Long> roleNameIndex = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> userRoleAssignments = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public RolePermission create(RolePermission rolePermission) {
        long roleId = idGenerator.getAndIncrement();
        RolePermission created = new RolePermission(
                roleId,
                rolePermission.getRoleName(),
                rolePermission.getPermissionCodes(),
                rolePermission.isActive(),
                1 // version
        );
        store.put(roleId, created);
        roleNameIndex.put(rolePermission.getRoleName(), roleId);
        return created;
    }

    @Override
    public Optional<RolePermission> findById(long roleId) {
        return Optional.ofNullable(store.get(roleId));
    }

    @Override
    public Optional<RolePermission> findByRoleName(String roleName) {
        Long roleId = roleNameIndex.get(roleName);
        if (roleId == null) return Optional.empty();
        return Optional.ofNullable(store.get(roleId));
    }

    @Override
    public boolean update(RolePermission rolePermission, long expectedVersion) {
        RolePermission existing = store.get(rolePermission.getRoleId());
        if (existing == null) {
            throw new IllegalArgumentException("RolePermission not found: " + rolePermission.getRoleId());
        }
        if (existing.getVersion() != expectedVersion) {
            return false; // version conflict
        }
        RolePermission updated = new RolePermission(
                rolePermission.getRoleId(),
                rolePermission.getRoleName(),
                rolePermission.getPermissionCodes(),
                rolePermission.isActive(),
                expectedVersion + 1 // increment version
        );
        store.put(rolePermission.getRoleId(), updated);
        // Update name index if changed
        if (!existing.getRoleName().equals(rolePermission.getRoleName())) {
            roleNameIndex.remove(existing.getRoleName());
            roleNameIndex.put(rolePermission.getRoleName(), rolePermission.getRoleId());
        }
        return true;
    }

    @Override
    public boolean delete(long roleId) {
        RolePermission rolePermission = store.remove(roleId);
        if (rolePermission == null) return false;
        roleNameIndex.remove(rolePermission.getRoleName());
        // Remove all user-role assignments with this role
        userRoleAssignments.values().forEach(roles -> roles.remove(roleId));
        return true;
    }

    @Override
    public List<RolePermission> listAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<RolePermission> findRolesByUserId(long userId) {
        Set<Long> roleIds = userRoleAssignments.getOrDefault(userId, Collections.emptySet());
        return roleIds.stream()
                .map(roleId -> store.get(roleId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean assignRoleToUser(long userId, long roleId) {
        if (!store.containsKey(roleId)) {
            return false; // role does not exist
        }
        Set<Long> roles = userRoleAssignments.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        return roles.add(roleId);
    }

    @Override
    public boolean removeRoleFromUser(long userId, long roleId) {
        Set<Long> roles = userRoleAssignments.get(userId);
        if (roles == null) return false;
        boolean removed = roles.remove(roleId);
        if (roles.isEmpty()) {
            userRoleAssignments.remove(userId);
        }
        return removed;
    }
}
