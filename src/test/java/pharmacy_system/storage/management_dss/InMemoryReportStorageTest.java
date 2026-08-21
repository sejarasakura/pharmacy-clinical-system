package pharmacy_system.storage.management_dss;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pharmacy_system.model.management_dss.Report;
import pharmacy_system.model.management_dss.ReportCriteria;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryReportStorageTest {
    private ReportStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryReportStorage();
    }

    @Test
    void testCreateAndFindById() {
        ReportCriteria criteria = new ReportCriteria(
                "Prescription", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), new HashMap<>()
        );
        List<Map<String, Object>> data = List.of(
                Map.of("prescriptionId", 1L, "status", "ISSUED")
        );
        Report report = new Report(
                0, "Prescription", "January Prescriptions", criteria, 1, LocalDateTime.now(), 1, data, 0
        );

        Report created = storage.create(report);
        assertEquals(1, created.getReportId());
        assertEquals(1, created.getVersion());

        Optional<Report> found = storage.findById(created.getReportId());
        assertTrue(found.isPresent());
        assertEquals("Prescription", found.get().getReportType());
    }

    @Test
    void testListAll() {
        ReportCriteria criteria1 = new ReportCriteria(
                "Prescription", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), new HashMap<>()
        );
        ReportCriteria criteria2 = new ReportCriteria(
                "Dispensing", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), new HashMap<>()
        );

        storage.create(new Report(0, "Prescription", "Rx Report", criteria1, 1, LocalDateTime.now(), 0, List.of(), 0));
        storage.create(new Report(0, "Dispensing", "Dispense Report", criteria2, 1, LocalDateTime.now(), 0, List.of(), 0));

        List<Report> all = storage.listAll();
        assertEquals(2, all.size());
    }

    @Test
    void testListByType() {
        ReportCriteria criteria = new ReportCriteria(
                "Prescription", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), new HashMap<>()
        );

        storage.create(new Report(0, "Prescription", "Rx Report 1", criteria, 1, LocalDateTime.now(), 0, List.of(), 0));
        storage.create(new Report(0, "Prescription", "Rx Report 2", criteria, 1, LocalDateTime.now(), 0, List.of(), 0));
        storage.create(new Report(0, "Dispensing", "Dispense Report", criteria, 1, LocalDateTime.now(), 0, List.of(), 0));

        List<Report> prescriptionReports = storage.listByType("Prescription");
        assertEquals(2, prescriptionReports.size());
        assertTrue(prescriptionReports.stream().allMatch(r -> "Prescription".equals(r.getReportType())));

        List<Report> dispensingReports = storage.listByType("Dispensing");
        assertEquals(1, dispensingReports.size());
    }

    @Test
    void testListByDateRange() {
        ReportCriteria criteria = new ReportCriteria(
                "Prescription", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), new HashMap<>()
        );

        LocalDateTime jan15 = LocalDateTime.of(2024, 1, 15, 10, 0);
        LocalDateTime feb10 = LocalDateTime.of(2024, 2, 10, 10, 0);
        LocalDateTime mar05 = LocalDateTime.of(2024, 3, 5, 10, 0);

        storage.create(new Report(0, "Prescription", "Jan Report", criteria, 1, jan15, 0, List.of(), 0));
        storage.create(new Report(0, "Prescription", "Feb Report", criteria, 1, feb10, 0, List.of(), 0));
        storage.create(new Report(0, "Prescription", "Mar Report", criteria, 1, mar05, 0, List.of(), 0));

        List<Report> janFebReports = storage.listByDateRange(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 28)
        );
        assertEquals(2, janFebReports.size());
    }

    @Test
    void testListByGeneratedBy() {
        ReportCriteria criteria = new ReportCriteria(
                "Prescription", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), new HashMap<>()
        );

        storage.create(new Report(0, "Prescription", "Report by User 1", criteria, 1, LocalDateTime.now(), 0, List.of(), 0));
        storage.create(new Report(0, "Prescription", "Report by User 1 Again", criteria, 1, LocalDateTime.now(), 0, List.of(), 0));
        storage.create(new Report(0, "Dispensing", "Report by User 2", criteria, 2, LocalDateTime.now(), 0, List.of(), 0));

        List<Report> user1Reports = storage.listByGeneratedBy(1);
        assertEquals(2, user1Reports.size());
        assertTrue(user1Reports.stream().allMatch(r -> r.getGeneratedBy() == 1));

        List<Report> user2Reports = storage.listByGeneratedBy(2);
        assertEquals(1, user2Reports.size());
    }

    @Test
    void testDelete() {
        ReportCriteria criteria = new ReportCriteria(
                "Prescription", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), new HashMap<>()
        );
        Report report = new Report(
                0, "Prescription", "To Delete", criteria, 1, LocalDateTime.now(), 0, List.of(), 0
        );
        Report created = storage.create(report);

        boolean deleted = storage.delete(created.getReportId());
        assertTrue(deleted);

        Optional<Report> found = storage.findById(created.getReportId());
        assertFalse(found.isPresent());
    }

    @Test
    void testReportImmutability() {
        ReportCriteria criteria = new ReportCriteria(
                "Prescription", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), new HashMap<>()
        );
        List<Map<String, Object>> data = List.of(
                Map.of("prescriptionId", 1L, "status", "ISSUED"),
                Map.of("prescriptionId", 2L, "status", "ON_HOLD")
        );
        Report report = new Report(
                0, "Prescription", "Snapshot Report", criteria, 1, LocalDateTime.now(), 2, data, 0
        );

        Report created = storage.create(report);
        assertEquals(2, created.getRowCount());
        assertEquals(data.size(), created.getData().size());

        // Verify data was persisted as-is (snapshot preserved)
        Optional<Report> retrieved = storage.findById(created.getReportId());
        assertTrue(retrieved.isPresent());
        assertEquals(data.size(), retrieved.get().getData().size());
    }

    @Test
    void testMultipleReportTypesAndUsers() {
        ReportCriteria criteria = new ReportCriteria(
                "Prescription", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), new HashMap<>()
        );

        // Create reports of different types by different users
        storage.create(new Report(0, "Prescription", "Rx Report", criteria, 1, LocalDateTime.now(), 0, List.of(), 0));
        storage.create(new Report(0, "Dispensing", "Dispense Report", criteria, 2, LocalDateTime.now(), 0, List.of(), 0));
        storage.create(new Report(0, "Inventory", "Inv Report", criteria, 1, LocalDateTime.now(), 0, List.of(), 0));
        storage.create(new Report(0, "UserAccess", "Access Report", criteria, 2, LocalDateTime.now(), 0, List.of(), 0));

        // Verify queries work correctly
        assertEquals(2, storage.listByType("Prescription").size() + storage.listByType("Inventory").size());
        assertEquals(2, storage.listByGeneratedBy(1).size());
        assertEquals(2, storage.listByGeneratedBy(2).size());
        assertEquals(4, storage.listAll().size());
    }
}
