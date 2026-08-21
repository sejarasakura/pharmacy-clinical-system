package pharmacy_system.storage.security_user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pharmacy_system.model.security_user.RolePermission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRolePermissionStorageTest {
    private RolePermissionStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryRolePermissionStorage();
    }

    @Test
    void testCreateAndFindById() {
        Set<String> permissions = Set.of("READ_PRESCRIPTION", "WRITE_PRESCRIPTION");
        RolePermission role = new RolePermission(0, "Doctor", permissions, true, 0);

        RolePermission created = storage.create(role);
        assertEquals(1, created.getRoleId());
        assertEquals(1, created.getVersion());

        Optional<RolePermission> found = storage.findById(created.getRoleId());
        assertTrue(found.isPresent());
        assertEquals("Doctor", found.get().getRoleName());
    }

    @Test
    void testFindByRoleName() {
        Set<String> permissions = Set.of("DISPENSE_MEDICATION");
        RolePermission role = new RolePermission(0, "Pharmacist", permissions, true, 0);
        storage.create(role);

        Optional<RolePermission> found = storage.findByRoleName("Pharmacist");
        assertTrue(found.isPresent());
        assertEquals("Pharmacist", found.get().getRoleName());
    }

    @Test
    void testUpdateWithVersionConflict() {
        Set<String> permissions = Set.of("READ_ONLY");
        RolePermission role = new RolePermission(0, "Patient", permissions, true, 0);
        RolePermission created = storage.create(role);

        Set<String> newPermissions = Set.of("READ_ONLY", "SUBMIT_FEEDBACK");
        RolePermission modified = new RolePermission(created.getRoleId(), "Patient", newPermissions, true, 0);

        boolean success = storage.update(modified, 0);
        assertFalse(success);
    }

    @Test
    void testUpdateWithCorrectVersion() {
        Set<String> permissions = Set.of("MANAGE_ACCOUNTS");
        RolePermission role = new RolePermission(0, "Administrator", permissions, true, 0);
        RolePermission created = storage.create(role);

        Set<String> newPermissions = Set.of("MANAGE_ACCOUNTS", "GENERATE_REPORTS", "VIEW_AUDIT_LOGS");
        RolePermission modified = new RolePermission(created.getRoleId(), "Administrator", newPermissions, true, 0);

        boolean success = storage.update(modified, 1);
        assertTrue(success);

        Optional<RolePermission> updated = storage.findById(created.getRoleId());
        assertEquals(3, updated.get().getPermissionCodes().size());
        assertEquals(2, updated.get().getVersion());
    }

    @Test
    void testAssignRoleToUser() {
        Set<String> permissions = Set.of("READ");
        RolePermission role = new RolePermission(0, "Viewer", permissions, true, 0);
        RolePermission created = storage.create(role);

        boolean assigned = storage.assignRoleToUser(100, created.getRoleId());
        assertTrue(assigned);

        // Assign same role again should return false
        boolean duplicate = storage.assignRoleToUser(100, created.getRoleId());
        assertFalse(duplicate);
    }

    @Test
    void testFindRolesByUserId() {
        Set<String> permissions1 = Set.of("READ_PRESCRIPTION");
        Set<String> permissions2 = Set.of("WRITE_PRESCRIPTION");
        RolePermission role1 = new RolePermission(0, "Doctor", permissions1, true, 0);
        RolePermission role2 = new RolePermission(0, "Admin", permissions2, true, 0);

        RolePermission created1 = storage.create(role1);
        RolePermission created2 = storage.create(role2);

        storage.assignRoleToUser(50, created1.getRoleId());
        storage.assignRoleToUser(50, created2.getRoleId());

        List<RolePermission> userRoles = storage.findRolesByUserId(50);
        assertEquals(2, userRoles.size());
    }

    @Test
    void testRemoveRoleFromUser() {
        Set<String> permissions = Set.of("READ");
        RolePermission role = new RolePermission(0, "Reader", permissions, true, 0);
        RolePermission created = storage.create(role);

        storage.assignRoleToUser(60, created.getRoleId());
        
        boolean removed = storage.removeRoleFromUser(60, created.getRoleId());
        assertTrue(removed);

        List<RolePermission> userRoles = storage.findRolesByUserId(60);
        assertEquals(0, userRoles.size());
    }

    @Test
    void testListAll() {
        Set<String> permissions1 = Set.of("READ");
        Set<String> permissions2 = Set.of("WRITE");
        storage.create(new RolePermission(0, "Role1", permissions1, true, 0));
        storage.create(new RolePermission(0, "Role2", permissions2, true, 0));

        List<RolePermission> all = storage.listAll();
        assertEquals(2, all.size());
    }
}
