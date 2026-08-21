package pharmacy_system.model.security_user.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Unit tests for base {@link UserProfile} functionality.
 *
 * <p>Validates: Requirements 2.3, 2.4, Property 12</p>
 */
class UserProfileTest {

    private static class TestProfile extends UserProfile {
        TestProfile(Long userId, String fullName) {
            super(userId, fullName);
        }

        TestProfile(
                long profileId,
                Long userId,
                String fullName,
                String phoneNumber,
                String contactEmail,
                String address,
                Map<String, String> preferences,
                long version,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
            super(profileId, userId, fullName, phoneNumber, contactEmail, address, preferences, version, createdAt, updatedAt);
        }
    }

    @Test
    void newProfileHasDefaultValues() {
        TestProfile profile = new TestProfile(100L, "John Doe");

        assertEquals(0L, profile.getProfileId());
        assertEquals(100L, profile.getUserId());
        assertEquals("John Doe", profile.getFullName());
        assertNull(profile.getPhoneNumber());
        assertNull(profile.getContactEmail());
        assertNull(profile.getAddress());
        assertTrue(profile.getPreferences().isEmpty());
        assertEquals(0L, profile.getVersion());
        assertNotNull(profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());
        assertTrue(profile.hasLinkedAccount());
    }

    @Test
    void profileWithoutUserIdIsBusinessRecordOnly() {
        TestProfile profile = new TestProfile(null, "Patient Business Record");

        assertNull(profile.getUserId());
        assertFalse(profile.hasLinkedAccount());
    }

    @Test
    void validateProfileDataAcceptsValidProfile() {
        TestProfile profile = new TestProfile(
                1L, 100L, "John Doe", "1234567890", "john@example.com",
                "123 Main St", Map.of("theme", "dark"), 1L, LocalDateTime.now(),
                LocalDateTime.now());

        List<String> errors = profile.validateProfileData();
        assertTrue(errors.isEmpty());
        assertTrue(profile.hasValidProfileData());
    }

    @Test
    void validateProfileDataRejectsMissingFullName() {
        TestProfile profile = new TestProfile(100L, null);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("fullName is required"));
    }

    @Test
    void validateProfileDataRejectsLongFullName() {
        String longName = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        TestProfile profile = new TestProfile(100L, longName);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("fullName must not exceed " + UserProfile.MAX_TEXT_LENGTH + " characters"));
    }

    @Test
    void validateProfileDataRejectsInvalidPhoneNumber() {
        TestProfile profile = new TestProfile(
                1L, 100L, "John Doe", "not-a-phone", "john@example.com",
                null, null, 0L, null, null);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("phoneNumber must contain 7 to 15 digits"));
    }

    @Test
    void validateProfileDataRejectsInvalidEmail() {
        TestProfile profile = new TestProfile(
                1L, 100L, "John Doe", "1234567890", "invalid-email",
                null, null, 0L, null, null);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("contactEmail must be of the form local@domain"));
    }

    @Test
    void validateProfileDataRejectsLongAddress() {
        String longAddress = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        TestProfile profile = new TestProfile(
                1L, 100L, "John Doe", null, null, longAddress, null, 0L, null, null);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("address must not exceed " + UserProfile.MAX_TEXT_LENGTH + " characters"));
    }

    @Test
    void applyChangesRejectsRestrictedFields() {
        TestProfile profile = new TestProfile(100L, "John Doe");
        Map<String, Object> changes = new HashMap<>();
        changes.put("fullName", "Jane Doe");
        changes.put("role", "Administrator"); // Restricted
        changes.put("accountStatus", "ACTIVE"); // Restricted

        RestrictedFieldException exception = assertThrows(RestrictedFieldException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("role"));
        assertTrue(exception.getMessage().contains("accountStatus"));
        assertEquals("John Doe", profile.getFullName()); // No changes applied
    }

    @Test
    void applyChangesRejectsUnknownFields() {
        TestProfile profile = new TestProfile(100L, "John Doe");
        Map<String, Object> changes = new HashMap<>();
        changes.put("fullName", "Jane Doe");
        changes.put("unknownField", "value");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("unknownField"));
        assertEquals("John Doe", profile.getFullName()); // No changes applied
    }

    @Test
    void applyChangesAcceptsValidEditableFields() {
        TestProfile profile = new TestProfile(100L, "John Doe");
        long initialVersion = profile.getVersion();
        LocalDateTime initialUpdatedAt = profile.getUpdatedAt();

        Map<String, Object> changes = new HashMap<>();
        changes.put("fullName", "Jane Doe");
        changes.put("phoneNumber", "9876543210");
        changes.put("contactEmail", "jane@example.com");
        changes.put("address", "456 Oak St");
        changes.put("preferences", Map.of("language", "en", "notifications", "enabled"));

        profile.applyChanges(changes);

        assertEquals("Jane Doe", profile.getFullName());
        assertEquals("9876543210", profile.getPhoneNumber());
        assertEquals("jane@example.com", profile.getContactEmail());
        assertEquals("456 Oak St", profile.getAddress());
        assertEquals("en", profile.getPreferences().get("language"));
        assertEquals("enabled", profile.getPreferences().get("notifications"));
        assertTrue(profile.getVersion() > initialVersion);
        assertTrue(profile.getUpdatedAt().isAfter(initialUpdatedAt));
    }

    @Test
    void applyChangesValidatesValueTypes() {
        TestProfile profile = new TestProfile(100L, "John Doe");
        Map<String, Object> changes = new HashMap<>();
        changes.put("phoneNumber", 1234567890); // Wrong type: Integer instead of String

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("phoneNumber must be a String"));
    }

    @Test
    void applyPreferencesValidatesMapTypes() {
        TestProfile profile = new TestProfile(100L, "John Doe");
        Map<String, Object> changes = new HashMap<>();
        Map<Object, Object> invalidPrefs = new HashMap<>();
        invalidPrefs.put(123, "value"); // Key is not String
        changes.put("preferences", invalidPrefs);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("preferences keys must be Strings"));
    }

    @Test
    void directMutationMethodsWork() {
        TestProfile profile = new TestProfile(100L, "John Doe");
        long initialVersion = profile.getVersion();

        profile.updateContact("5551234567", "john@newdomain.com");
        assertEquals("5551234567", profile.getPhoneNumber());
        assertEquals("john@newdomain.com", profile.getContactEmail());
        assertTrue(profile.getVersion() > initialVersion);

        initialVersion = profile.getVersion();
        profile.updateAddress("789 Pine Rd");
        assertEquals("789 Pine Rd", profile.getAddress());
        assertTrue(profile.getVersion() > initialVersion);

        initialVersion = profile.getVersion();
        profile.updatePreference("theme", "light");
        assertEquals("light", profile.getPreferences().get("theme"));
        assertTrue(profile.getVersion() > initialVersion);
    }

    @Test
    void emptyChangesMapDoesNothing() {
        TestProfile profile = new TestProfile(100L, "John Doe");
        long initialVersion = profile.getVersion();

        profile.applyChanges(Map.of());

        assertEquals("John Doe", profile.getFullName());
        assertEquals(initialVersion, profile.getVersion());
    }

    @Test
    void preferencesAreImmutable() {
        TestProfile profile = new TestProfile(100L, "John Doe");
        Map<String, String> preferences = new HashMap<>();
        preferences.put("key", "value");
        TestProfile profileWithPrefs = new TestProfile(
                1L, 100L, "John Doe", null, null, null, preferences, 0L, null, null);

        Map<String, String> retrieved = profileWithPrefs.getPreferences();
        assertEquals("value", retrieved.get("key"));

        // Attempt to modify should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> retrieved.put("another", "value2"));
        assertFalse(profileWithPrefs.getPreferences().containsKey("another"));
    }
}