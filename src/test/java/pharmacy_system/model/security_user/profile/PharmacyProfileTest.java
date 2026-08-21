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
 * Unit tests for {@link PharmacyProfile}.
 *
 * <p>Validates: Requirements 2.3, 2.4, Property 12</p>
 */
class PharmacyProfileTest {

    @Test
    void newPharmacyProfileHasCorrectDefaults() {
        PharmacyProfile profile = new PharmacyProfile(100L, "Jane Pharmacist", "PHARM123456", "Main Pharmacy");

        assertEquals(100L, profile.getUserId());
        assertEquals("Jane Pharmacist", profile.getFullName());
        assertEquals("PHARM123456", profile.getPharmacistRegistrationNo());
        assertEquals("Main Pharmacy", profile.getPharmacyUnit());
        assertTrue(profile.hasLinkedAccount());
    }

    @Test
    void pharmacyProfilePropertiesAreCorrectlySet() {
        Map<String, String> preferences = Map.of("theme", "blue");
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        PharmacyProfile profile = new PharmacyProfile(
                1L, 100L, "Jane Pharmacist", "5551234567", "jane@example.com",
                "123 Pharmacy St", preferences, 2L, createdAt, updatedAt,
                "PHARM123456", "Main Pharmacy");

        assertEquals(1L, profile.getProfileId());
        assertEquals(100L, profile.getUserId());
        assertEquals("Jane Pharmacist", profile.getFullName());
        assertEquals("5551234567", profile.getPhoneNumber());
        assertEquals("jane@example.com", profile.getContactEmail());
        assertEquals("123 Pharmacy St", profile.getAddress());
        assertEquals("blue", profile.getPreferences().get("theme"));
        assertEquals(2L, profile.getVersion());
        assertEquals(createdAt, profile.getCreatedAt());
        assertEquals(updatedAt, profile.getUpdatedAt());
        assertEquals("PHARM123456", profile.getPharmacistRegistrationNo());
        assertEquals("Main Pharmacy", profile.getPharmacyUnit());
    }

    @Test
    void validateProfileDataRejectsMissingPharmacistRegistrationNo() {
        PharmacyProfile profile = new PharmacyProfile(
                100L, "Jane Pharmacist", null, "Main Pharmacy");
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("pharmacistRegistrationNo is required"));
    }

    @Test
    void validateProfileDataRejectsLongPharmacistRegistrationNo() {
        String longRegNo = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        PharmacyProfile profile = new PharmacyProfile(
                100L, "Jane Pharmacist", longRegNo, "Main Pharmacy");
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("pharmacistRegistrationNo must not exceed " + UserProfile.MAX_TEXT_LENGTH + " characters"));
    }

    @Test
    void validateProfileDataRejectsLongPharmacyUnit() {
        String longUnit = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        PharmacyProfile profile = new PharmacyProfile(
                100L, "Jane Pharmacist", "PHARM123456", longUnit);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("pharmacyUnit must not exceed " + UserProfile.MAX_TEXT_LENGTH + " characters"));
    }

    @Test
    void validateProfileDataAcceptsValidPharmacyProfile() {
        PharmacyProfile profile = new PharmacyProfile(
                1L, 100L, "Jane Pharmacist", "5551234567", "jane@example.com",
                "123 Pharmacy St", Map.of("pref", "value"), 1L,
                LocalDateTime.now(), LocalDateTime.now(),
                "PHARM123456", "Main Pharmacy");

        List<String> errors = profile.validateProfileData();
        assertTrue(errors.isEmpty());
        assertTrue(profile.hasValidProfileData());
    }

    @Test
    void applyChangesRejectsPharmacistRegistrationNo() {
        PharmacyProfile profile = new PharmacyProfile(
                100L, "Jane Pharmacist", "PHARM123456", "Main Pharmacy");
        Map<String, Object> changes = new HashMap<>();
        changes.put("pharmacyUnit", "Emergency Pharmacy");
        changes.put("pharmacistRegistrationNo", "NEW123456"); // Restricted field

        RestrictedFieldException exception = assertThrows(RestrictedFieldException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("pharmacistRegistrationNo"));
        assertEquals("Main Pharmacy", profile.getPharmacyUnit()); // No changes applied
    }

    @Test
    void applyChangesAcceptsPharmacyUnit() {
        PharmacyProfile profile = new PharmacyProfile(
                100L, "Jane Pharmacist", "PHARM123456", "Main Pharmacy");
        long initialVersion = profile.getVersion();

        Map<String, Object> changes = new HashMap<>();
        changes.put("pharmacyUnit", "Emergency Pharmacy");
        changes.put("fullName", "Jane Smith");

        profile.applyChanges(changes);

        assertEquals("Emergency Pharmacy", profile.getPharmacyUnit());
        assertEquals("Jane Smith", profile.getFullName());
        assertEquals("PHARM123456", profile.getPharmacistRegistrationNo()); // Unchanged
        assertTrue(profile.getVersion() > initialVersion);
    }

    @Test
    void applyChangesValidatesPharmacyUnitType() {
        PharmacyProfile profile = new PharmacyProfile(
                100L, "Jane Pharmacist", "PHARM123456", "Main Pharmacy");
        Map<String, Object> changes = new HashMap<>();
        changes.put("pharmacyUnit", 123); // Wrong type

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("pharmacyUnit must be a String"));
    }

    @Test
    void validateProfessionalDetailsReturnsTrueForValidRegistration() {
        PharmacyProfile profile = new PharmacyProfile(
                100L, "Jane Pharmacist", "PHARM123456", "Main Pharmacy");
        assertTrue(profile.validateProfessionalDetails());
    }

    @Test
    void validateProfessionalDetailsReturnsFalseForBlankRegistration() {
        PharmacyProfile profile = new PharmacyProfile(
                100L, "Jane Pharmacist", "", "Main Pharmacy");
        assertFalse(profile.validateProfessionalDetails());

        profile = new PharmacyProfile(100L, "Jane Pharmacist", null, "Main Pharmacy");
        assertFalse(profile.validateProfessionalDetails());
    }

    @Test
    void validateProfessionalDetailsReturnsFalseForLongRegistration() {
        String longRegNo = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        PharmacyProfile profile = new PharmacyProfile(
                100L, "Jane Pharmacist", longRegNo, "Main Pharmacy");
        assertFalse(profile.validateProfessionalDetails());
    }

    @Test
    void pharmacyProfileInheritsBaseValidation() {
        // Test that PharmacyProfile also validates base fields
        PharmacyProfile profile = new PharmacyProfile(
                100L, null, "PHARM123456", "Main Pharmacy"); // Missing fullName
        List<String> errors = profile.validateProfileData();
        assertTrue(errors.stream().anyMatch(e -> e.contains("fullName is required")));
    }

    @Test
    void pharmacyProfileCanUpdateBaseFields() {
        PharmacyProfile profile = new PharmacyProfile(
                100L, "Jane Pharmacist", "PHARM123456", "Main Pharmacy");
        Map<String, Object> changes = new HashMap<>();
        changes.put("phoneNumber", "9876543210");
        changes.put("contactEmail", "new@example.com");
        changes.put("address", "456 New St");

        profile.applyChanges(changes);

        assertEquals("9876543210", profile.getPhoneNumber());
        assertEquals("new@example.com", profile.getContactEmail());
        assertEquals("456 New St", profile.getAddress());
        assertEquals("Jane Pharmacist", profile.getFullName()); // Unchanged
        assertEquals("PHARM123456", profile.getPharmacistRegistrationNo()); // Unchanged
        assertEquals("Main Pharmacy", profile.getPharmacyUnit()); // Unchanged
    }
}