package pharmacy_system.model.security_user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RolePermission} permission checking.
 *
 * <p>Validates: Requirements 1.1, 1.3, 2.1</p>
 */
class RolePermissionTest {

    @Test
    void hasPermissionReturnsTrueWhenActiveAndContainsCode() {
        RolePermission role = new RolePermission(1L, "Doctor", "Doctor role", Set.of("PRESCRIPTION_MANAGE", "PATIENT_VIEW"), true);

        assertTrue(role.hasPermission("PRESCRIPTION_MANAGE"));
        assertTrue(role.hasPermission("PATIENT_VIEW"));
    }

    @Test
    void hasPermissionReturnsFalseWhenInactive() {
        RolePermission role = new RolePermission(1L, "Doctor", "Doctor role", Set.of("PRESCRIPTION_MANAGE"), false);

        assertFalse(role.hasPermission("PRESCRIPTION_MANAGE"));
        assertFalse(role.hasPermission("NONEXISTENT"));
    }

    @Test
    void hasPermissionReturnsFalseWhenPermissionCodeNotInSet() {
        RolePermission role = new RolePermission(1L, "Pharmacist", "Pharmacist role", Set.of("DISPENSE_MEDICATION"), true);

        assertTrue(role.hasPermission("DISPENSE_MEDICATION"));
        assertFalse(role.hasPermission("PRESCRIPTION_MANAGE"));
        assertFalse(role.hasPermission(""));
        assertFalse(role.hasPermission(null));
    }

    @Test
    void rolePropertiesAreCorrectlySet() {
        Set<String> permissions = Set.of("CREATE_REPORT", "VIEW_REPORT");
        RolePermission role = new RolePermission(42L, "Administrator", "System administrator", permissions, true);

        assertEquals(42L, role.getRoleId());
        assertEquals("Administrator", role.getRoleName());
        assertEquals("System administrator", role.getDescription());
        assertEquals(permissions, role.getPermissionCodes());
        assertTrue(role.isActive());
        assertNotNull(role.getPermissionCodes());
    }

    @Test
    void constructorWithRoleNameOnly() {
        RolePermission role = new RolePermission("Patient", Set.of("VIEW_OWN_PRESCRIPTIONS"), true);

        assertEquals(0L, role.getRoleId());
        assertEquals("Patient", role.getRoleName());
        assertTrue(role.hasPermission("VIEW_OWN_PRESCRIPTIONS"));
        assertFalse(role.hasPermission("OTHER_PERMISSION"));
    }

    @Test
    void roleNameCannotBeNull() {
        try {
            new RolePermission(1L, null, "Test", Set.of("TEST"), true);
        } catch (NullPointerException e) {
            assertEquals("roleName must not be null", e.getMessage());
        }
    }

    @Test
    void permissionCodesCannotBeNull() {
        try {
            new RolePermission(1L, "Test", "Test", null, true);
        } catch (NullPointerException e) {
            assertEquals("permissionCodes must not be null", e.getMessage());
        }
    }
}