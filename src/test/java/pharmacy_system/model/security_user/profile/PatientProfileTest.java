package pharmacy_system.model.security_user.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PatientProfile} as both self-service profile and
 * Patient_Business_Record.
 *
 * <p>Validates: Requirements 2.3, 2.4, 3.1, 3.4, 3.5, Property 12</p>
 */
class PatientProfileTest {

    @Test
    void newPatientProfileWithLinkedAccount() {
        PatientProfile profile = new PatientProfile(
                100L, "PAT123", "John Doe", "5551234567", "john@example.com",
                LocalDate.of(1980, 5, 15), "123 Main St", "Jane Doe (5559876543)");

        assertEquals(100L, profile.getUserId());
        assertEquals("PAT123", profile.getPatientIdentifier());
        assertEquals("John Doe", profile.getFullName());
        assertEquals("5551234567", profile.getPhoneNumber());
        assertEquals("john@example.com", profile.getContactEmail());
        assertEquals(LocalDate.of(1980, 5, 15), profile.getDateOfBirth());
        assertEquals("123 Main St", profile.getAddress());
        assertEquals("Jane Doe (5559876543)", profile.getEmergencyContact());
        assertTrue(profile.hasLinkedAccount());
    }

    @Test
    void newPatientProfileWithoutLinkedAccount() {
        PatientProfile profile = new PatientProfile(
                null, "PAT456", "Jane Smith", null, null, null, null, null);

        assertNull(profile.getUserId());
        assertEquals("PAT456", profile.getPatientIdentifier());
        assertEquals("Jane Smith", profile.getFullName());
        assertFalse(profile.hasLinkedAccount());
    }

    @Test
    void patientProfilePropertiesAreCorrectlySet() {
        Map<String, String> preferences = Map.of("theme", "light");
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        PatientProfile profile = new PatientProfile(
                1L, 100L, "PAT123", "John Doe", "5551234567", "john@example.com",
                "123 Main St", preferences, 2L, createdAt, updatedAt,
                LocalDate.of(1980, 5, 15), "Jane Doe");

        assertEquals(1L, profile.getProfileId());
        assertEquals(100L, profile.getUserId());
        assertEquals("PAT123", profile.getPatientIdentifier());
        assertEquals("John Doe", profile.getFullName());
        assertEquals("5551234567", profile.getPhoneNumber());
        assertEquals("john@example.com", profile.getContactEmail());
        assertEquals("123 Main St", profile.getAddress());
        assertEquals("light", profile.getPreferences().get("theme"));
        assertEquals(2L, profile.getVersion());
        assertEquals(createdAt, profile.getCreatedAt());
        assertEquals(updatedAt, profile.getUpdatedAt());
        assertEquals(LocalDate.of(1980, 5, 15), profile.getDateOfBirth());
        assertEquals("Jane Doe", profile.getEmergencyContact());
    }

    @Test
    void validateProfileDataRejectsMissingPatientIdentifier() {
        PatientProfile profile = new PatientProfile(
                100L, null, "John Doe", null, null, null, null, null);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("patientIdentifier is required"));
    }

    @Test
    void validateProfileDataRejectsLongPatientIdentifier() {
        String longId = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        PatientProfile profile = new PatientProfile(
                100L, longId, "John Doe", null, null, null, null, null);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("patientIdentifier must not exceed " + UserProfile.MAX_TEXT_LENGTH + " characters"));
    }

    @Test
    void validateProfileDataRejectsFutureDateOfBirth() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        PatientProfile profile = new PatientProfile(
                100L, "PAT123", "John Doe", null, null, futureDate, null, null);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("dateOfBirth must not be later than the current date"));
    }

    @Test
    void validateProfileDataRejectsLongEmergencyContact() {
        String longContact = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        PatientProfile profile = new PatientProfile(
                100L, "PAT123", "John Doe", null, null, null, null, longContact);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("emergencyContact must not exceed " + UserProfile.MAX_TEXT_LENGTH + " characters"));
    }

    @Test
    void validateProfileDataAcceptsValidPatientProfile() {
        PatientProfile profile = new PatientProfile(
                1L, 100L, "PAT123", "John Doe", "5551234567", "john@example.com",
                "123 Main St", Map.of("pref", "value"), 1L,
                LocalDateTime.now(), LocalDateTime.now(),
                LocalDate.of(1980, 5, 15), "Jane Doe");

        List<String> errors = profile.validateProfileData();
        assertTrue(errors.isEmpty());
        assertTrue(profile.hasValidProfileData());
    }

    @Test
    void applyChangesRejectsPatientIdentifier() {
        PatientProfile profile = new PatientProfile(
                100L, "PAT123", "John Doe", null, null, null, null, null);
        Map<String, Object> changes = new HashMap<>();
        changes.put("emergencyContact", "New Contact");
        changes.put("patientIdentifier", "NEW123"); // Restricted field

        RestrictedFieldException exception = assertThrows(RestrictedFieldException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("patientIdentifier"));
        assertNull(profile.getEmergencyContact()); // No changes applied
    }

    @Test
    void applyChangesAcceptsDateOfBirthAndEmergencyContact() {
        PatientProfile profile = new PatientProfile(
                100L, "PAT123", "John Doe", null, null, null, null, null);
        long initialVersion = profile.getVersion();
        LocalDate newDateOfBirth = LocalDate.of(1990, 6, 20);

        Map<String, Object> changes = new HashMap<>();
        changes.put("dateOfBirth", newDateOfBirth);
        changes.put("emergencyContact", "Jane Doe");
        changes.put("fullName", "John Smith");

        profile.applyChanges(changes);

        assertEquals(newDateOfBirth, profile.getDateOfBirth());
        assertEquals("Jane Doe", profile.getEmergencyContact());
        assertEquals("John Smith", profile.getFullName());
        assertEquals("PAT123", profile.getPatientIdentifier()); // Unchanged
        assertTrue(profile.getVersion() > initialVersion);
    }

    @Test
    void applyChangesValidatesDateOfBirthType() {
        PatientProfile profile = new PatientProfile(
                100L, "PAT123", "John Doe", null, null, null, null, null);
        Map<String, Object> changes = new HashMap<>();
        changes.put("dateOfBirth", "not-a-date"); // Wrong type

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("dateOfBirth must be a LocalDate"));
    }

    @Test
    void calculateAgeReturnsCorrectAge() {
        LocalDate dateOfBirth = LocalDate.now().minusYears(30).minusMonths(2);
        PatientProfile profile = new PatientProfile(
                100L, "PAT123", "John Doe", null, null, dateOfBirth, null, null);

        int expectedAge = Period.between(dateOfBirth, LocalDate.now()).getYears();
        assertEquals(expectedAge, profile.calculateAge());
    }

    @Test
    void calculateAgeReturnsNegativeOneForUnknownDateOfBirth() {
        PatientProfile profile = new PatientProfile(
                100L, "PAT123", "John Doe", null, null, null, null, null);
        assertEquals(-1, profile.calculateAge());
    }

    @Test
    void patientProfileInheritsBaseValidation() {
        // Test that PatientProfile also validates base fields
        PatientProfile profile = new PatientProfile(
                100L, "PAT123", null, "invalid-phone", "invalid-email", null, null, null);
        List<String> errors = profile.validateProfileData();
        assertTrue(errors.stream().anyMatch(e -> e.contains("fullName is required")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("phoneNumber must contain")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("contactEmail must be")));
    }

    @Test
    void patientProfileCanUpdateBaseFields() {
        PatientProfile profile = new PatientProfile(
                100L, "PAT123", "John Doe", null, null, null, null, null);
        Map<String, Object> changes = new HashMap<>();
        changes.put("phoneNumber", "9876543210");
        changes.put("contactEmail", "new@example.com");
        changes.put("address", "456 New St");

        profile.applyChanges(changes);

        assertEquals("9876543210", profile.getPhoneNumber());
        assertEquals("new@example.com", profile.getContactEmail());
        assertEquals("456 New St", profile.getAddress());
        assertEquals("John Doe", profile.getFullName()); // Unchanged
        assertEquals("PAT123", profile.getPatientIdentifier()); // Unchanged
        assertNull(profile.getDateOfBirth()); // Unchanged
        assertNull(profile.getEmergencyContact()); // Unchanged
    }

    @Test
    void patientProfileBusinessRecordCanExistWithoutLinkedAccount() {
        // This is a key requirement: Patient_Business_Record may exist without linked UserAccount
        PatientProfile profile = new PatientProfile(
                null, "PAT999", "Clinical Patient", null, null, LocalDate.of(1975, 3, 10), null, "Emergency");

        assertNull(profile.getUserId());
        assertEquals("PAT999", profile.getPatientIdentifier());
        assertEquals("Clinical Patient", profile.getFullName());
        assertEquals(LocalDate.of(1975, 3, 10), profile.getDateOfBirth());
        assertEquals("Emergency", profile.getEmergencyContact());
        assertFalse(profile.hasLinkedAccount());
        assertTrue(profile.hasValidProfileData());
    }
}