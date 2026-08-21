package pharmacy_system.storage.security_user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pharmacy_system.model.security_user.profile.PatientProfile;
import pharmacy_system.model.security_user.profile.DoctorProfile;
import pharmacy_system.model.security_user.profile.UserProfile;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserProfileStorageTest {
    private UserProfileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryUserProfileStorage();
    }

    @Test
    void testCreatePatientProfile() {
        PatientProfile profile = new PatientProfile(
                0, 1, "John Patient", "555-1234", "john@example.com", "123 Main St",
                LocalDate.of(1990, 5, 15), "555-5678", java.util.Map.of(), 0
        );

        UserProfile created = storage.create(profile);
        assertTrue(created.getProfileId() > 0);
        assertEquals(1, created.getVersion());
    }

    @Test
    void testFindByUserIdAndType() {
        PatientProfile profile = new PatientProfile(
                0, 2, "Jane Patient", "555-2345", "jane@example.com", "456 Oak Ave",
                LocalDate.of(1985, 3, 20), "555-6789", java.util.Map.of(), 0
        );
        UserProfile created = storage.create(profile);

        Optional<PatientProfile> found = storage.findByUserIdAndType(2, PatientProfile.class);
        assertTrue(found.isPresent());
        assertEquals("Jane Patient", found.get().getFullName());
    }

    @Test
    void testFindByUserId() {
        DoctorProfile profile = new DoctorProfile(
                0, 3, "Dr. Smith", "555-3456", "smith@example.com", "789 Medical Plaza",
                "MD12345", java.util.Map.of(), 0
        );
        UserProfile created = storage.create(profile);

        Optional<UserProfile> found = storage.findByUserId(3);
        assertTrue(found.isPresent());
        assertEquals("Dr. Smith", found.get().getFullName());
    }

    @Test
    void testFindById() {
        PatientProfile profile = new PatientProfile(
                0, 4, "Bob Patient", "555-4567", "bob@example.com", "321 Elm St",
                LocalDate.of(1995, 7, 10), "555-7890", java.util.Map.of(), 0
        );
        UserProfile created = storage.create(profile);

        Optional<UserProfile> found = storage.findById(created.getProfileId());
        assertTrue(found.isPresent());
        assertEquals(4, found.get().getUserId());
    }

    @Test
    void testUpdateWithVersionConflict() {
        PatientProfile profile = new PatientProfile(
                0, 5, "Alice Patient", "555-5678", "alice@example.com", "654 Pine Rd",
                LocalDate.of(1988, 11, 25), "555-8901", java.util.Map.of(), 0
        );
        UserProfile created = storage.create(profile);

        PatientProfile modified = new PatientProfile(
                created.getProfileId(), 5, "Alice Patient Modified", "555-5678", "alice_new@example.com",
                "654 Pine Rd", LocalDate.of(1988, 11, 25), "555-8901", java.util.Map.of(), 0
        );

        boolean success = storage.update(modified, 0);
        assertFalse(success);
    }

    @Test
    void testUpdateWithCorrectVersion() {
        PatientProfile profile = new PatientProfile(
                0, 6, "Charlie Patient", "555-6789", "charlie@example.com", "987 Maple Ln",
                LocalDate.of(1992, 2, 14), "555-9012", java.util.Map.of(), 0
        );
        UserProfile created = storage.create(profile);

        PatientProfile modified = new PatientProfile(
                created.getProfileId(), 6, "Charles Patient", "555-6789", "charles@example.com",
                "987 Maple Ln", LocalDate.of(1992, 2, 14), "555-9012", java.util.Map.of(), 0
        );

        boolean success = storage.update(modified, 1);
        assertTrue(success);

        Optional<UserProfile> updated = storage.findById(created.getProfileId());
        assertTrue(updated.isPresent());
        assertEquals(2, updated.get().getVersion());
    }

    @Test
    void testDelete() {
        DoctorProfile profile = new DoctorProfile(
                0, 7, "Dr. Johnson", "555-7890", "johnson@example.com", "111 Health Center",
                "MD67890", java.util.Map.of(), 0
        );
        UserProfile created = storage.create(profile);

        boolean deleted = storage.delete(created.getProfileId());
        assertTrue(deleted);

        Optional<UserProfile> found = storage.findById(created.getProfileId());
        assertFalse(found.isPresent());
    }

    @Test
    void testListByType() {
        PatientProfile patient1 = new PatientProfile(
                0, 8, "Patient One", "555-8901", "p1@example.com", "111 Patient St",
                LocalDate.of(1990, 1, 1), "555-0001", java.util.Map.of(), 0
        );
        PatientProfile patient2 = new PatientProfile(
                0, 9, "Patient Two", "555-9012", "p2@example.com", "222 Patient St",
                LocalDate.of(1991, 1, 1), "555-0002", java.util.Map.of(), 0
        );
        DoctorProfile doctor = new DoctorProfile(
                0, 10, "Dr. Three", "555-0123", "d3@example.com", "333 Doctor Plaza",
                "MD00003", java.util.Map.of(), 0
        );

        storage.create(patient1);
        storage.create(patient2);
        storage.create(doctor);

        var patients = storage.listByType(PatientProfile.class);
        assertEquals(2, patients.size());

        var doctors = storage.listByType(DoctorProfile.class);
        assertEquals(1, doctors.size());
    }

    @Test
    void testListAll() {
        storage.create(new PatientProfile(
                0, 11, "P1", "555-1111", "p1@test.com", "1 St",
                LocalDate.of(1990, 1, 1), "555-1111", java.util.Map.of(), 0
        ));
        storage.create(new DoctorProfile(
                0, 12, "D1", "555-2222", "d1@test.com", "2 Blvd",
                "MD00001", java.util.Map.of(), 0
        ));

        var all = storage.listAll();
        assertEquals(2, all.size());
    }
}
