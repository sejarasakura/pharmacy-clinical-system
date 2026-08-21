package pharmacy_system.storage.pharmacy_operations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pharmacy_system.model.pharmacy_operations.Medicine;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryMedicineStorageTest {
    private MedicineStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryMedicineStorage();
    }

    @Test
    void testCreateAndFindById() {
        Medicine med = new Medicine(
                0, "ASPIRIN500", "Aspirin", "Acetylsalicylic Acid", "Tablet", "500mg", "mg", "Pain relief", true, 0
        );

        Medicine created = storage.create(med);
        assertEquals(1, created.getMedicineId());
        assertEquals(1, created.getVersion());

        Optional<Medicine> found = storage.findById(created.getMedicineId());
        assertTrue(found.isPresent());
        assertEquals("Aspirin", found.get().getMedicineName());
    }

    @Test
    void testFindByCode() {
        Medicine med = new Medicine(
                0, "AMOX250", "Amoxicillin", "Amoxycillin trihydrate", "Capsule", "250mg", "mg", "Antibiotic", true, 0
        );
        storage.create(med);

        Optional<Medicine> found = storage.findByCode("AMOX250");
        assertTrue(found.isPresent());
        assertEquals("Amoxicillin", found.get().getMedicineName());
    }

    @Test
    void testUpdateWithVersionConflict() {
        Medicine med = new Medicine(
                0, "IBUPROFEN400", "Ibuprofen", "Ibuprofen", "Tablet", "400mg", "mg", "Anti-inflammatory", true, 0
        );
        Medicine created = storage.create(med);

        Medicine modified = new Medicine(
                created.getMedicineId(), "IBUPROFEN400", "Ibuprofen Updated", "Ibuprofen", "Tablet", "400mg", "mg", "Anti-inflammatory", true, 0
        );

        boolean success = storage.update(modified, 0);
        assertFalse(success);
    }

    @Test
    void testUpdateWithCorrectVersion() {
        Medicine med = new Medicine(
                0, "METFORMIN500", "Metformin", "Metformin hydrochloride", "Tablet", "500mg", "mg", "Diabetes", true, 0
        );
        Medicine created = storage.create(med);

        Medicine modified = new Medicine(
                created.getMedicineId(), "METFORMIN500", "Metformin HCL", "Metformin hydrochloride", "Tablet", "500mg", "mg", "Diabetes", true, 0
        );

        boolean success = storage.update(modified, 1);
        assertTrue(success);

        Optional<Medicine> updated = storage.findById(created.getMedicineId());
        assertEquals("Metformin HCL", updated.get().getMedicineName());
        assertEquals(2, updated.get().getVersion());
    }

    @Test
    void testDelete() {
        Medicine med = new Medicine(
                0, "PARACET650", "Paracetamol", "Paracetamol", "Tablet", "650mg", "mg", "Painkiller", true, 0
        );
        Medicine created = storage.create(med);

        boolean deleted = storage.delete(created.getMedicineId());
        assertTrue(deleted);

        Optional<Medicine> found = storage.findById(created.getMedicineId());
        assertFalse(found.isPresent());
    }

    @Test
    void testListAll() {
        storage.create(new Medicine(0, "CODE1", "Medicine 1", "Generic 1", "Tablet", "10mg", "mg", "Desc 1", true, 0));
        storage.create(new Medicine(0, "CODE2", "Medicine 2", "Generic 2", "Capsule", "20mg", "mg", "Desc 2", true, 0));
        storage.create(new Medicine(0, "CODE3", "Medicine 3", "Generic 3", "Liquid", "50ml", "ml", "Desc 3", false, 0));

        List<Medicine> all = storage.listAll();
        assertEquals(3, all.size());
    }

    @Test
    void testListActive() {
        storage.create(new Medicine(0, "ACTIVE1", "Active Medicine 1", "Generic", "Tablet", "10mg", "mg", "Active", true, 0));
        storage.create(new Medicine(0, "ACTIVE2", "Active Medicine 2", "Generic", "Capsule", "20mg", "mg", "Active", true, 0));
        storage.create(new Medicine(0, "INACTIVE", "Inactive Medicine", "Generic", "Tablet", "30mg", "mg", "Inactive", false, 0));

        List<Medicine> active = storage.listActive();
        assertEquals(2, active.size());
        assertTrue(active.stream().allMatch(Medicine::isActive));
    }

    @Test
    void testQueryForReport() {
        storage.create(new Medicine(0, "RPT001", "Report Medicine", "Generic", "Tablet", "100mg", "mg", "For report", true, 0));

        var rows = storage.queryForReport(java.util.Map.of());
        assertEquals(1, rows.size());
        assertEquals("Report Medicine", rows.get(0).get("medicineName"));
        assertTrue((Boolean) rows.get(0).get("active"));
    }

    @Test
    void testCodeIndexUpdatesOnCodeChange() {
        Medicine med = storage.create(new Medicine(
                0, "OLD_CODE", "Medicine", "Generic", "Tablet", "50mg", "mg", "Desc", true, 0
        ));

        // Should find by old code
        assertTrue(storage.findByCode("OLD_CODE").isPresent());

        // Update with new code
        Medicine updated = storage.findById(med.getMedicineId()).get();
        Medicine withNewCode = new Medicine(
                updated.getMedicineId(), "NEW_CODE", "Medicine", "Generic", "Tablet", "50mg", "mg", "Desc", true, 0
        );
        storage.update(withNewCode, 1);

        // Old code should no longer find it
        assertFalse(storage.findByCode("OLD_CODE").isPresent());

        // New code should find it
        assertTrue(storage.findByCode("NEW_CODE").isPresent());
    }
}
