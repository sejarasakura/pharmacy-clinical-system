package pharmacy_system.storage.clinical_prescription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pharmacy_system.model.clinical_prescription.Prescription;
import pharmacy_system.model.clinical_prescription.PrescriptionItem;
import pharmacy_system.model.clinical_prescription.PrescriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPrescriptionStorageTest {
    private PrescriptionStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryPrescriptionStorage();
    }

    @Test
    void testCreateAndFindById() {
        PrescriptionItem item = new PrescriptionItem(0, 1, "Aspirin", "500mg", "Twice daily", "After meals", 20);
        List<PrescriptionItem> items = List.of(item);
        Prescription rx = new Prescription(
                0, 10, 5, "Patient has fever", PrescriptionStatus.DRAFT,
                null, null, 0, null, LocalDateTime.now(), LocalDateTime.now(), 0, items
        );

        Prescription created = storage.create(rx);
        assertEquals(1, created.getPrescriptionId());
        assertEquals(1, created.getVersion());

        Optional<Prescription> found = storage.findById(created.getPrescriptionId());
        assertTrue(found.isPresent());
        assertEquals(10, found.get().getPatientId());
    }

    @Test
    void testFindByPatientId() {
        PrescriptionItem item = new PrescriptionItem(0, 1, "Amoxicillin", "250mg", "Three times daily", "With water", 30);
        Prescription rx1 = new Prescription(
                0, 20, 5, "Infection", PrescriptionStatus.ISSUED,
                LocalDateTime.now(), LocalDateTime.now(), 5, "Approved", LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        );
        Prescription rx2 = new Prescription(
                0, 20, 6, "Allergy", PrescriptionStatus.ISSUED,
                LocalDateTime.now(), LocalDateTime.now(), 6, "Approved", LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        );

        storage.create(rx1);
        storage.create(rx2);

        List<Prescription> patientRxs = storage.findByPatientId(20);
        assertEquals(2, patientRxs.size());
    }

    @Test
    void testFindByDoctorId() {
        PrescriptionItem item = new PrescriptionItem(0, 2, "Ibuprofen", "400mg", "Once daily", "Before bed", 10);
        Prescription rx1 = new Prescription(
                0, 30, 7, "Pain relief", PrescriptionStatus.DRAFT,
                null, null, 0, null, LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        );
        Prescription rx2 = new Prescription(
                0, 31, 7, "Inflammation", PrescriptionStatus.DRAFT,
                null, null, 0, null, LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        );

        storage.create(rx1);
        storage.create(rx2);

        List<Prescription> doctorRxs = storage.findByDoctorId(7);
        assertEquals(2, doctorRxs.size());
    }

    @Test
    void testUpdateWithVersionConflict() {
        PrescriptionItem item = new PrescriptionItem(0, 3, "Acetaminophen", "650mg", "Every 4-6 hours", "As needed", 15);
        Prescription rx = new Prescription(
                0, 40, 8, "Headache", PrescriptionStatus.DRAFT,
                null, null, 0, null, LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        );
        Prescription created = storage.create(rx);

        Prescription modified = new Prescription(
                created.getPrescriptionId(), 40, 8, "Severe headache", PrescriptionStatus.ISSUED,
                LocalDateTime.now(), LocalDateTime.now(), 8, "Approved", LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        );

        boolean success = storage.update(modified, 0);
        assertFalse(success);
    }

    @Test
    void testUpdateWithCorrectVersion() {
        PrescriptionItem item = new PrescriptionItem(0, 4, "Metformin", "500mg", "Twice daily", "With meals", 60);
        Prescription rx = new Prescription(
                0, 50, 9, "Diabetes management", PrescriptionStatus.DRAFT,
                null, null, 0, null, LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        );
        Prescription created = storage.create(rx);

        Prescription modified = new Prescription(
                created.getPrescriptionId(), 50, 9, "Diabetes management - updated", PrescriptionStatus.ISSUED,
                LocalDateTime.now(), LocalDateTime.now(), 9, "Approved", LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        );

        boolean success = storage.update(modified, 1);
        assertTrue(success);

        Optional<Prescription> updated = storage.findById(created.getPrescriptionId());
        assertEquals("Diabetes management - updated", updated.get().getClinicalNotes());
        assertEquals(2, updated.get().getVersion());
    }

    @Test
    void testDelete() {
        PrescriptionItem item = new PrescriptionItem(0, 5, "Lisinopril", "10mg", "Once daily", "Morning", 30);
        Prescription rx = new Prescription(
                0, 60, 10, "Blood pressure", PrescriptionStatus.DRAFT,
                null, null, 0, null, LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        );
        Prescription created = storage.create(rx);

        boolean deleted = storage.delete(created.getPrescriptionId());
        assertTrue(deleted);

        Optional<Prescription> found = storage.findById(created.getPrescriptionId());
        assertFalse(found.isPresent());
    }

    @Test
    void testListAll() {
        PrescriptionItem item = new PrescriptionItem(0, 6, "Vitamin D", "1000IU", "Once daily", "Anytime", 90);
        storage.create(new Prescription(
                0, 70, 11, "Supplement", PrescriptionStatus.DRAFT,
                null, null, 0, null, LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        ));
        storage.create(new Prescription(
                0, 71, 12, "Supplement", PrescriptionStatus.DRAFT,
                null, null, 0, null, LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        ));

        List<Prescription> all = storage.listAll();
        assertEquals(2, all.size());
    }

    @Test
    void testQueryForReport() {
        PrescriptionItem item = new PrescriptionItem(0, 7, "Omeprazole", "20mg", "Once daily", "Before breakfast", 30);
        LocalDateTime issuedAt = LocalDateTime.of(2024, 1, 15, 10, 0);
        Prescription rx = new Prescription(
                0, 80, 13, "GERD", PrescriptionStatus.ISSUED,
                issuedAt, issuedAt, 13, "Approved", LocalDateTime.now(), LocalDateTime.now(), 0, List.of(item)
        );
        storage.create(rx);

        var rows = storage.queryForReport(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), java.util.Map.of());
        assertEquals(1, rows.size());
        assertEquals("ISSUED", rows.get(0).get("status"));
    }
}
