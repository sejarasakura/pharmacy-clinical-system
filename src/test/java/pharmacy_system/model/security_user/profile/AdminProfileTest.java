package pharmacy_system.model.security_user.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AdminProfile}.
 *
 * <p>Validates: Requirements 2.3, 2.4, Property 12</p>
 */
class AdminProfileTest {

    @Test
    void newAdminProfileHasCorrectDefaults() {
        AdminProfile profile = new AdminProfile(100L, "Admin User", "ADM001", "IT Department");

        assertEquals(100L, profile.getUserId());
        assertEquals("Admin User", profile.getFullName());
        assertEquals("ADM001", profile.getStaffId());
        assertEquals("IT Department", profile.getDepartment());
        assertTrue(profile.hasLinkedAccount());
    }

    @Test
    void adminProfilePropertiesAreCorrectlySet() {
        Map<String, String> preferences = Map.of("theme", "admin");
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        AdminProfile profile = new AdminProfile(
                1L, 100L, "Admin User", "5551234567", "admin@example.com",
                "123 Admin Bldg", preferences, 2L, createdAt, updatedAt,
                "ADM001", "IT Department");

        assertEquals(1L, profile.getProfileId());
        assertEquals(100L, profile.getUserId());
        assertEquals("Admin User", profile.getFullName());
        assertEquals("5551234567", profile.getPhoneNumber());
        assertEquals("admin@example.com", profile.getContactEmail());
        assertEquals("123 Admin Bldg", profile.getAddress());
        assertEquals("admin", profile.getPreferences().get("theme"));
        assertEquals(2L, profile.getVersion());
        assertEquals(createdAt, profile.getCreatedAt());
        assertEquals(updatedAt, profile.getUpdatedAt());
        assertEquals("ADM001", profile.getStaffId());
        assertEquals("IT Department", profile.getDepartment());
    }

    @Test
    void validateProfileDataAcceptsValidAdminProfile() {
        AdminProfile profile = new AdminProfile(
                1L, 100L, "Admin User", "5551234567", "admin@example.com",
                "123 Admin Bldg", Map.of("pref", "value"), 1L,
                LocalDateTime.now(), LocalDateTime.now(),
                "ADM001", "IT Department");

        List<String> errors = profile.validateProfileData();
        assertTrue(errors.isEmpty());
        assertTrue(profile.hasValidProfileData());
    }

    @Test
    void validateProfileDataRejectsLongStaffId() {
        String longStaffId = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        AdminProfile profile = new AdminProfile(100L, "Admin User", longStaffId, "IT Department");
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("staffId must not exceed " + UserProfile.MAX_TEXT_LENGTH + " characters"));
    }

    @Test
    void validateProfileDataRejectsLongDepartment() {
        String longDept = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        AdminProfile profile = new AdminProfile(100L, "Admin User", "ADM001", longDept);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("department must not exceed " + UserProfile.MAX_TEXT_LENGTH + " characters"));
    }

    @Test
    void applyChangesRejectsStaffId() {
        AdminProfile profile = new AdminProfile(100L, "Admin User", "ADM001", "IT Department");
        Map<String, Object> changes = new HashMap<>();
        changes.put("department", "HR Department");
        changes.put("staffId", "NEW001"); // Restricted field

        RestrictedFieldException exception = assertThrows(RestrictedFieldException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("staffId"));
        assertEquals("IT Department", profile.getDepartment()); // No changes applied
    }

    @Test
    void applyChangesAcceptsDepartment() {
        AdminProfile profile = new AdminProfile(100L, "Admin User", "ADM001", "IT Department");
        long initialVersion = profile.getVersion();

        Map<String, Object> changes = new HashMap<>();
        changes.put("department", "HR Department");
        changes.put("fullName", "Administrator");

        profile.applyChanges(changes);

        assertEquals("HR Department", profile.getDepartment());
        assertEquals("Administrator", profile.getFullName());
        assertEquals("ADM001", profile.getStaffId()); // Unchanged
        assertTrue(profile.getVersion() > initialVersion);
    }

    @Test
    void applyChangesValidatesDepartmentType() {
        AdminProfile profile = new AdminProfile(100L, "Admin User", "ADM001", "IT Department");
        Map<String, Object> changes = new HashMap<>();
        changes.put("department", 123); // Wrong type

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("department must be a String"));
    }

    @Test
    void adminProfileInheritsBaseValidation() {
        // Test that AdminProfile also validates base fields
        AdminProfile profile = new AdminProfile(
                100L, null, "ADM001", "IT Department"); // Missing fullName
        List<String> errors = profile.validateProfileData();
        assertTrue(errors.stream().anyMatch(e -> e.contains("fullName is required")));
    }

    @Test
    void adminProfileCanUpdateBaseFields() {
        AdminProfile profile = new AdminProfile(100L, "Admin User", "ADM001", "IT Department");
        Map<String, Object> changes = new HashMap<>();
        changes.put("phoneNumber", "9876543210");
        changes.put("contactEmail", "new@example.com");
        changes.put("address", "456 New St");

        profile.applyChanges(changes);

        assertEquals("9876543210", profile.getPhoneNumber());
        assertEquals("new@example.com", profile.getContactEmail());
        assertEquals("456 New St", profile.getAddress());
        assertEquals("Admin User", profile.getFullName()); // Unchanged
        assertEquals("ADM001", profile.getStaffId()); // Unchanged
        assertEquals("IT Department", profile.getDepartment()); // Unchanged
    }

    @Test
    void staffIdCanBeNull() {
        AdminProfile profile = new AdminProfile(100L, "Admin User", null, "IT Department");
        assertNull(profile.getStaffId());
        List<String> errors = profile.validateProfileData();
        assertTrue(errors.isEmpty()); // staffId is optional in validation
    }

    @Test
    void departmentCanBeNull() {
        AdminProfile profile = new AdminProfile(100L, "Admin User", "ADM001", null);
        assertNull(profile.getDepartment());
        List<String> errors = profile.validateProfileData();
        assertTrue(errors.isEmpty()); // department is optional in validation
    }
}