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
 * Unit tests for {@link DoctorProfile}.
 *
 * <p>Validates: Requirements 2.3, 2.4, 3.4, 3.5, Property 12</p>
 */
class DoctorProfileTest {

    @Test
    void newDoctorProfileHasCorrectDefaults() {
        DoctorProfile profile = new DoctorProfile(100L, "Dr. Smith", "MED123456", "Cardiology");

        assertEquals(100L, profile.getUserId());
        assertEquals("Dr. Smith", profile.getFullName());
        assertEquals("MED123456", profile.getMedicalRegistrationNo());
        assertEquals("Cardiology", profile.getSpeciality());
        assertTrue(profile.hasLinkedAccount());
    }

    @Test
    void doctorProfilePropertiesAreCorrectlySet() {
        Map<String, String> preferences = Map.of("theme", "dark");
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        DoctorProfile profile = new DoctorProfile(
                1L, 100L, "Dr. Smith", "5551234567", "smith@example.com",
                "123 Medical Center", preferences, 2L, createdAt, updatedAt,
                "MED123456", "Cardiology");

        assertEquals(1L, profile.getProfileId());
        assertEquals(100L, profile.getUserId());
        assertEquals("Dr. Smith", profile.getFullName());
        assertEquals("5551234567", profile.getPhoneNumber());
        assertEquals("smith@example.com", profile.getContactEmail());
        assertEquals("123 Medical Center", profile.getAddress());
        assertEquals("dark", profile.getPreferences().get("theme"));
        assertEquals(2L, profile.getVersion());
        assertEquals(createdAt, profile.getCreatedAt());
        assertEquals(updatedAt, profile.getUpdatedAt());
        assertEquals("MED123456", profile.getMedicalRegistrationNo());
        assertEquals("Cardiology", profile.getSpeciality());
    }

    @Test
    void validateProfileDataRejectsMissingMedicalRegistrationNo() {
        DoctorProfile profile = new DoctorProfile(
                100L, "Dr. Smith", null, "Cardiology");
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("medicalRegistrationNo is required"));
    }

    @Test
    void validateProfileDataRejectsLongMedicalRegistrationNo() {
        String longRegNo = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        DoctorProfile profile = new DoctorProfile(
                100L, "Dr. Smith", longRegNo, "Cardiology");
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("medicalRegistrationNo must not exceed " + UserProfile.MAX_TEXT_LENGTH + " characters"));
    }

    @Test
    void validateProfileDataRejectsLongSpeciality() {
        String longSpeciality = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        DoctorProfile profile = new DoctorProfile(
                100L, "Dr. Smith", "MED123456", longSpeciality);
        List<String> errors = profile.validateProfileData();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("speciality must not exceed " + UserProfile.MAX_TEXT_LENGTH + " characters"));
    }

    @Test
    void validateProfileDataAcceptsValidDoctorProfile() {
        DoctorProfile profile = new DoctorProfile(
                1L, 100L, "Dr. Smith", "5551234567", "smith@example.com",
                "123 Medical Center", Map.of("pref", "value"), 1L,
                LocalDateTime.now(), LocalDateTime.now(),
                "MED123456", "Cardiology");

        List<String> errors = profile.validateProfileData();
        assertTrue(errors.isEmpty());
        assertTrue(profile.hasValidProfileData());
    }

    @Test
    void applyChangesRejectsMedicalRegistrationNo() {
        DoctorProfile profile = new DoctorProfile(100L, "Dr. Smith", "MED123456", "Cardiology");
        Map<String, Object> changes = new HashMap<>();
        changes.put("speciality", "Neurology");
        changes.put("medicalRegistrationNo", "NEW123456"); // Restricted field

        RestrictedFieldException exception = assertThrows(RestrictedFieldException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("medicalRegistrationNo"));
        assertEquals("Cardiology", profile.getSpeciality()); // No changes applied
    }

    @Test
    void applyChangesAcceptsSpeciality() {
        DoctorProfile profile = new DoctorProfile(100L, "Dr. Smith", "MED123456", "Cardiology");
        long initialVersion = profile.getVersion();

        Map<String, Object> changes = new HashMap<>();
        changes.put("speciality", "Neurology");
        changes.put("fullName", "Dr. Johnson");

        profile.applyChanges(changes);

        assertEquals("Neurology", profile.getSpeciality());
        assertEquals("Dr. Johnson", profile.getFullName());
        assertEquals("MED123456", profile.getMedicalRegistrationNo()); // Unchanged
        assertTrue(profile.getVersion() > initialVersion);
    }

    @Test
    void applyChangesValidatesSpecialityType() {
        DoctorProfile profile = new DoctorProfile(100L, "Dr. Smith", "MED123456", "Cardiology");
        Map<String, Object> changes = new HashMap<>();
        changes.put("speciality", 123); // Wrong type

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> profile.applyChanges(changes));
        assertTrue(exception.getMessage().contains("speciality must be a String"));
    }

    @Test
    void validateProfessionalDetailsReturnsTrueForValidRegistration() {
        DoctorProfile profile = new DoctorProfile(100L, "Dr. Smith", "MED123456", "Cardiology");
        assertTrue(profile.validateProfessionalDetails());
    }

    @Test
    void validateProfessionalDetailsReturnsFalseForBlankRegistration() {
        DoctorProfile profile = new DoctorProfile(100L, "Dr. Smith", "", "Cardiology");
        assertFalse(profile.validateProfessionalDetails());

        profile = new DoctorProfile(100L, "Dr. Smith", null, "Cardiology");
        assertFalse(profile.validateProfessionalDetails());
    }

    @Test
    void validateProfessionalDetailsReturnsFalseForLongRegistration() {
        String longRegNo = "A".repeat(UserProfile.MAX_TEXT_LENGTH + 1);
        DoctorProfile profile = new DoctorProfile(100L, "Dr. Smith", longRegNo, "Cardiology");
        assertFalse(profile.validateProfessionalDetails());
    }

    @Test
    void doctorProfileInheritsBaseValidation() {
        // Test that DoctorProfile also validates base fields
        DoctorProfile profile = new DoctorProfile(
                100L, null, "MED123456", "Cardiology"); // Missing fullName
        List<String> errors = profile.validateProfileData();
        assertTrue(errors.stream().anyMatch(e -> e.contains("fullName is required")));
    }

    @Test
    void doctorProfileCanUpdateBaseFields() {
        DoctorProfile profile = new DoctorProfile(100L, "Dr. Smith", "MED123456", "Cardiology");
        Map<String, Object> changes = new HashMap<>();
        changes.put("phoneNumber", "9876543210");
        changes.put("contactEmail", "new@example.com");
        changes.put("address", "456 New St");

        profile.applyChanges(changes);

        assertEquals("9876543210", profile.getPhoneNumber());
        assertEquals("new@example.com", profile.getContactEmail());
        assertEquals("456 New St", profile.getAddress());
        assertEquals("Dr. Smith", profile.getFullName()); // Unchanged
        assertEquals("MED123456", profile.getMedicalRegistrationNo()); // Unchanged
        assertEquals("Cardiology", profile.getSpeciality()); // Unchanged
    }
}