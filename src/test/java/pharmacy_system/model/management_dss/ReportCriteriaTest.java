package pharmacy_system.model.management_dss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReportCriteria Tests")
class ReportCriteriaTest {

    @Test
    @DisplayName("constructor with all fields initializes correctly")
    void constructor_withAllFields_initializesCorrectly() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        Map<String, String> filters = new HashMap<>();
        filters.put("doctorId", "123");

        ReportCriteria criteria = new ReportCriteria("Prescription", start, end, filters);

        assertEquals("Prescription", criteria.getReportType());
        assertEquals(start, criteria.getStartDate());
        assertEquals(end, criteria.getEndDate());
        assertEquals(1, criteria.getFilters().size());
        assertEquals("123", criteria.getFilters().get("doctorId"));
    }

    @Test
    @DisplayName("constructor with null filters creates empty filter map")
    void constructor_withNullFilters_createsEmptyFilterMap() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        ReportCriteria criteria = new ReportCriteria("Prescription", start, end, null);

        assertNotNull(criteria.getFilters());
        assertTrue(criteria.getFilters().isEmpty());
    }

    @Test
    @DisplayName("default constructor creates empty criteria")
    void defaultConstructor_createsEmptyCriteria() {
        ReportCriteria criteria = new ReportCriteria();

        assertNull(criteria.getReportType());
        assertNull(criteria.getStartDate());
        assertNull(criteria.getEndDate());
        assertNotNull(criteria.getFilters());
        assertTrue(criteria.getFilters().isEmpty());
    }

    // hasValidDateRange() tests
    @Test
    @DisplayName("hasValidDateRange returns true when dates are equal")
    void hasValidDateRange_dateRangeEqual_returnsTrue() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        ReportCriteria criteria = new ReportCriteria("Prescription", date, date, null);

        assertTrue(criteria.hasValidDateRange());
    }

    @Test
    @DisplayName("hasValidDateRange returns true when startDate is before endDate")
    void hasValidDateRange_startBeforeEnd_returnsTrue() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        ReportCriteria criteria = new ReportCriteria("Prescription", start, end, null);

        assertTrue(criteria.hasValidDateRange());
    }

    @Test
    @DisplayName("hasValidDateRange returns false when startDate is after endDate")
    void hasValidDateRange_startAfterEnd_returnsFalse() {
        LocalDate start = LocalDate.of(2024, 1, 31);
        LocalDate end = LocalDate.of(2024, 1, 1);
        ReportCriteria criteria = new ReportCriteria("Prescription", start, end, null);

        assertFalse(criteria.hasValidDateRange());
    }

    @Test
    @DisplayName("hasValidDateRange returns true when startDate is null")
    void hasValidDateRange_startDateNull_returnsTrue() {
        LocalDate end = LocalDate.of(2024, 1, 31);
        ReportCriteria criteria = new ReportCriteria("Prescription", null, end, null);

        assertTrue(criteria.hasValidDateRange());
    }

    @Test
    @DisplayName("hasValidDateRange returns true when endDate is null")
    void hasValidDateRange_endDateNull_returnsTrue() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        ReportCriteria criteria = new ReportCriteria("Prescription", start, null, null);

        assertTrue(criteria.hasValidDateRange());
    }

    @Test
    @DisplayName("hasValidDateRange returns true when both dates are null")
    void hasValidDateRange_bothDatesNull_returnsTrue() {
        ReportCriteria criteria = new ReportCriteria("Prescription", null, null, null);

        assertTrue(criteria.hasValidDateRange());
    }

    // validateCriteria() tests
    @Test
    @DisplayName("validateCriteria returns no errors for valid Prescription report")
    void validateCriteria_validPrescriptionReport_returnsNoErrors() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        ReportCriteria criteria = new ReportCriteria("Prescription", start, end, null);

        List<String> errors = criteria.validateCriteria();

        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("validateCriteria returns no errors for valid Dispensing report")
    void validateCriteria_validDispensingReport_returnsNoErrors() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        ReportCriteria criteria = new ReportCriteria("Dispensing", start, end, null);

        List<String> errors = criteria.validateCriteria();

        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("validateCriteria returns no errors for valid Inventory report")
    void validateCriteria_validInventoryReport_returnsNoErrors() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        ReportCriteria criteria = new ReportCriteria("Inventory", start, end, null);

        List<String> errors = criteria.validateCriteria();

        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("validateCriteria returns no errors for valid UserAccess report")
    void validateCriteria_validUserAccessReport_returnsNoErrors() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        ReportCriteria criteria = new ReportCriteria("UserAccess", start, end, null);

        List<String> errors = criteria.validateCriteria();

        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("validateCriteria returns error when reportType is null")
    void validateCriteria_reportTypeNull_returnsError() {
        ReportCriteria criteria = new ReportCriteria(null, LocalDate.now(), LocalDate.now(), null);

        List<String> errors = criteria.validateCriteria();

        assertTrue(errors.stream().anyMatch(e -> e.contains("Report type is required")));
    }

    @Test
    @DisplayName("validateCriteria returns error when reportType is blank")
    void validateCriteria_reportTypeBlank_returnsError() {
        ReportCriteria criteria = new ReportCriteria("   ", LocalDate.now(), LocalDate.now(), null);

        List<String> errors = criteria.validateCriteria();

        assertTrue(errors.stream().anyMatch(e -> e.contains("Report type is required")));
    }

    @Test
    @DisplayName("validateCriteria returns error for unsupported report type")
    void validateCriteria_unsupportedReportType_returnsError() {
        ReportCriteria criteria = new ReportCriteria("InvalidReport", LocalDate.now(), LocalDate.now(), null);

        List<String> errors = criteria.validateCriteria();

        assertTrue(errors.stream().anyMatch(e -> e.contains("Unsupported report type")));
    }

    @Test
    @DisplayName("validateCriteria returns error for invalid date range")
    void validateCriteria_invalidDateRange_returnsError() {
        LocalDate start = LocalDate.of(2024, 1, 31);
        LocalDate end = LocalDate.of(2024, 1, 1);
        ReportCriteria criteria = new ReportCriteria("Prescription", start, end, null);

        List<String> errors = criteria.validateCriteria();

        assertTrue(errors.stream().anyMatch(e -> e.contains("Start date must not be after end date")));
    }

    @Test
    @DisplayName("validateCriteria returns multiple errors when multiple validations fail")
    void validateCriteria_multipleErrors_returnsAllErrors() {
        LocalDate start = LocalDate.of(2024, 1, 31);
        LocalDate end = LocalDate.of(2024, 1, 1);
        ReportCriteria criteria = new ReportCriteria("InvalidType", start, end, null);

        List<String> errors = criteria.validateCriteria();

        assertEquals(2, errors.size());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Unsupported report type")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Start date must not be after end date")));
    }

    @Test
    @DisplayName("setters update fields correctly")
    void setters_updateFieldsCorrectly() {
        ReportCriteria criteria = new ReportCriteria();
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        Map<String, String> filters = new HashMap<>();
        filters.put("key", "value");

        criteria.setReportType("Prescription");
        criteria.setStartDate(start);
        criteria.setEndDate(end);
        criteria.setFilters(filters);

        assertEquals("Prescription", criteria.getReportType());
        assertEquals(start, criteria.getStartDate());
        assertEquals(end, criteria.getEndDate());
        assertEquals(filters, criteria.getFilters());
    }
}
