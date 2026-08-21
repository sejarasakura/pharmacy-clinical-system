package pharmacy_system.model.management_dss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Report Tests")
class ReportTest {

    @Test
    @DisplayName("constructor initializes all fields correctly")
    void constructor_initializesAllFieldsCorrectly() {
        long reportId = 1L;
        String reportType = "Prescription";
        String title = "Prescription Report";
        ReportCriteria criteria = new ReportCriteria("Prescription", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null);
        long generatedBy = 100L;
        LocalDateTime generatedAt = LocalDateTime.now();
        int rowCount = 5;
        List<Map<String, Object>> data = createSampleData(5);

        Report report = new Report(reportId, reportType, title, criteria, generatedBy, generatedAt, rowCount, data, 1L);

        assertEquals(reportId, report.getReportId());
        assertEquals(reportType, report.getReportType());
        assertEquals(title, report.getTitle());
        assertEquals(criteria, report.getCriteria());
        assertEquals(generatedBy, report.getGeneratedBy());
        assertEquals(generatedAt, report.getGeneratedAt());
        assertEquals(rowCount, report.getRowCount());
        assertEquals(data, report.getData());
        assertEquals(1L, report.getVersion());
    }

    @Test
    @DisplayName("default constructor creates empty report")
    void defaultConstructor_createsEmptyReport() {
        Report report = new Report();

        assertEquals(0L, report.getReportId());
        assertNull(report.getReportType());
        assertNull(report.getTitle());
        assertNull(report.getCriteria());
        assertEquals(0L, report.getGeneratedBy());
        assertNull(report.getGeneratedAt());
        assertEquals(0, report.getRowCount());
        assertNull(report.getData());
        assertEquals(0L, report.getVersion());
    }

    // isEmpty() tests
    @Test
    @DisplayName("isEmpty returns true when rowCount is 0")
    void isEmpty_rowCountZero_returnsTrue() {
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   0, Collections.emptyList(), 1L);

        assertTrue(report.isEmpty());
    }

    @Test
    @DisplayName("isEmpty returns true when data is empty list")
    void isEmpty_emptyDataList_returnsTrue() {
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   0, new ArrayList<>(), 1L);

        assertTrue(report.isEmpty());
    }

    @Test
    @DisplayName("isEmpty returns false when rowCount is greater than 0")
    void isEmpty_rowCountGreaterThanZero_returnsFalse() {
        List<Map<String, Object>> data = createSampleData(3);
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   3, data, 1L);

        assertFalse(report.isEmpty());
    }

    @Test
    @DisplayName("isEmpty returns false when data has records")
    void isEmpty_dataHasRecords_returnsFalse() {
        List<Map<String, Object>> data = createSampleData(5);
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   5, data, 1L);

        assertFalse(report.isEmpty());
    }

    @Test
    @DisplayName("isEmpty returns true when data is null and rowCount is 0")
    void isEmpty_dataNull_rowCountZero_returnsTrue() {
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   0, null, 1L);

        assertTrue(report.isEmpty());
    }

    // Snapshot immutability and export tests
    @Test
    @DisplayName("generateExport derives output only from stored snapshot")
    void generateExport_derivesFromStoredSnapshot() {
        LocalDateTime generatedTime = LocalDateTime.of(2024, 1, 15, 10, 30);
        List<Map<String, Object>> snapshotData = createSampleData(3);
        Report report = new Report(1L, "Prescription", "Title", null, 100L, generatedTime, 
                                   3, snapshotData, 1L);

        byte[] export = report.generateExport("PDF");

        // Export should be generated from snapshot, not null even if deferred
        assertNotNull(export);
    }

    @Test
    @DisplayName("generateExport handles different format types")
    void generateExport_handlesDifferentFormats() {
        List<Map<String, Object>> data = createSampleData(2);
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   2, data, 1L);

        byte[] pdfExport = report.generateExport("PDF");
        byte[] csvExport = report.generateExport("CSV");
        byte[] jsonExport = report.generateExport("JSON");

        assertNotNull(pdfExport);
        assertNotNull(csvExport);
        assertNotNull(jsonExport);
    }

    @Test
    @DisplayName("generateExport does not modify stored snapshot")
    void generateExport_doesNotModifySnapshot() {
        List<Map<String, Object>> data = createSampleData(3);
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   3, data, 1L);

        // Make a copy to verify later
        List<Map<String, Object>> originalSnapshot = new ArrayList<>(data);

        // Call export multiple times
        report.generateExport("PDF");
        report.generateExport("CSV");
        report.generateExport("PDF");

        // Verify snapshot is unchanged
        assertEquals(3, report.getRowCount());
        assertEquals(originalSnapshot.size(), report.getData().size());
    }

    @Test
    @DisplayName("generateExport on empty report returns export")
    void generateExport_emptyReport_returnsExport() {
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   0, Collections.emptyList(), 1L);

        byte[] export = report.generateExport("PDF");

        assertNotNull(export);
        assertTrue(report.isEmpty());
    }

    // Snapshot persistence and data integrity tests
    @Test
    @DisplayName("stored data reflects generation time snapshot")
    void storedData_reflectsGenerationTimeSnapshot() {
        LocalDateTime generatedAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        List<Map<String, Object>> snapshotData = createSampleData(3);
        Report report = new Report(1L, "Prescription", "Title", null, 100L, generatedAt, 
                                   3, snapshotData, 1L);

        // Verify snapshot is captured at generation time
        assertEquals(generatedAt, report.getGeneratedAt());
        assertEquals(3, report.getRowCount());
        assertEquals(snapshotData, report.getData());
    }

    @Test
    @DisplayName("data snapshot contains expected structure")
    void dataSnapshot_containsExpectedStructure() {
        List<Map<String, Object>> data = createSampleData(2);
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   2, data, 1L);

        List<Map<String, Object>> retrieved = report.getData();

        assertEquals(2, retrieved.size());
        assertTrue(retrieved.get(0).containsKey("id"));
        assertTrue(retrieved.get(0).containsKey("value"));
    }

    @Test
    @DisplayName("rowCount matches data size for consistency")
    void rowCount_matchesDataSize() {
        List<Map<String, Object>> data = createSampleData(5);
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   5, data, 1L);

        assertEquals(report.getRowCount(), report.getData().size());
    }

    @Test
    @DisplayName("rowCount is zero for empty snapshot")
    void rowCount_zeroForEmptySnapshot() {
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   0, Collections.emptyList(), 1L);

        assertEquals(0, report.getRowCount());
        assertTrue(report.getData().isEmpty());
    }

    // Setters and getters tests
    @Test
    @DisplayName("setters update fields correctly")
    void setters_updateFieldsCorrectly() {
        Report report = new Report();
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> data = createSampleData(2);

        report.setReportId(1L);
        report.setReportType("Prescription");
        report.setTitle("Prescription Report");
        report.setGeneratedBy(100L);
        report.setGeneratedAt(now);
        report.setRowCount(2);
        report.setData(data);
        report.setVersion(1L);

        assertEquals(1L, report.getReportId());
        assertEquals("Prescription", report.getReportType());
        assertEquals("Prescription Report", report.getTitle());
        assertEquals(100L, report.getGeneratedBy());
        assertEquals(now, report.getGeneratedAt());
        assertEquals(2, report.getRowCount());
        assertEquals(data, report.getData());
        assertEquals(1L, report.getVersion());
    }

    @Test
    @DisplayName("criteria setter updates correctly")
    void criteriaSetterAndGetter_workCorrectly() {
        Report report = new Report();
        ReportCriteria criteria = new ReportCriteria("Prescription", LocalDate.now(), LocalDate.now(), null);

        report.setCriteria(criteria);

        assertEquals(criteria, report.getCriteria());
    }

    @Test
    @DisplayName("version field tracks report mutations")
    void versionField_tracksReportMutations() {
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   0, null, 0L);

        assertEquals(0L, report.getVersion());

        report.setVersion(1L);
        assertEquals(1L, report.getVersion());

        report.setVersion(2L);
        assertEquals(2L, report.getVersion());
    }

    // Edge cases and constraint validations
    @Test
    @DisplayName("large rowCount is handled correctly")
    void largeRowCount_handledCorrectly() {
        List<Map<String, Object>> data = createSampleData(1000);
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   1000, data, 1L);

        assertEquals(1000, report.getRowCount());
        assertFalse(report.isEmpty());
    }

    @Test
    @DisplayName("report with null data and rowCount mismatch")
    void reportWithNullData_andRowCountMismatch() {
        Report report = new Report(1L, "Prescription", "Title", null, 100L, LocalDateTime.now(), 
                                   5, null, 1L);

        // isEmpty uses rowCount == 0 check first, then checks data
        assertFalse(report.isEmpty()); // rowCount is 5
    }

    @Test
    @DisplayName("multiple reports with different data remain independent")
    void multipleReports_remainIndependent() {
        List<Map<String, Object>> data1 = createSampleData(3);
        List<Map<String, Object>> data2 = createSampleData(2);

        Report report1 = new Report(1L, "Prescription", "Report 1", null, 100L, LocalDateTime.now(), 
                                    3, data1, 1L);
        Report report2 = new Report(2L, "Dispensing", "Report 2", null, 200L, LocalDateTime.now(), 
                                    2, data2, 1L);

        assertEquals(3, report1.getRowCount());
        assertEquals(2, report2.getRowCount());

        // Modify one report's data via setter doesn't affect the other
        report1.setData(data2);
        assertEquals(3, report1.getRowCount()); // rowCount stays the same unless explicitly set
        assertEquals(2, report2.getRowCount());
    }

    @Test
    @DisplayName("report snapshot persists across multiple getter calls")
    void snapshotPersists_acrossMultipleGetterCalls() {
        LocalDateTime generatedAt = LocalDateTime.now();
        List<Map<String, Object>> data = createSampleData(3);
        Report report = new Report(1L, "Prescription", "Title", null, 100L, generatedAt, 
                                   3, data, 1L);

        // Call getters multiple times
        assertEquals(generatedAt, report.getGeneratedAt());
        assertEquals(generatedAt, report.getGeneratedAt());
        assertEquals(3, report.getRowCount());
        assertEquals(3, report.getRowCount());

        // Snapshot should remain unchanged
        assertEquals(3, report.getData().size());
    }

    // Helper method to create sample data
    private List<Map<String, Object>> createSampleData(int count) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", (long) i);
            row.put("value", "Record " + i);
            row.put("timestamp", LocalDateTime.now());
            data.add(row);
        }
        return data;
    }
}
