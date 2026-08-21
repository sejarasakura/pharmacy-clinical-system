package pharmacy_system.model.management_dss;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReportCriteria represents the transient criteria used to generate a Report.
 * Criteria are not persisted; the Report snapshot captures the data as it existed at generation time.
 */
public class ReportCriteria {
    private String reportType;              // Prescription, Dispensing, Inventory, UserAccess
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, String> filters;

    public ReportCriteria() {
        this.filters = new HashMap<>();
    }

    public ReportCriteria(String reportType, LocalDate startDate, LocalDate endDate, 
                          Map<String, String> filters) {
        this.reportType = reportType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.filters = filters != null ? filters : new HashMap<>();
    }

    // Getters
    public String getReportType() {
        return reportType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    // Setters
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters = filters;
    }

    /**
     * Validates that the date range is valid (startDate <= endDate).
     * @return true if the date range is valid, false otherwise
     */
    public boolean hasValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;  // No date range specified
        }
        return !startDate.isAfter(endDate);
    }

    /**
     * Validates the criteria for report generation.
     * @return a list of validation error messages; empty list if valid
     */
    public List<String> validateCriteria() {
        List<String> errors = new ArrayList<>();

        if (reportType == null || reportType.isBlank()) {
            errors.add("Report type is required");
        } else {
            // Validate supported report types
            if (!reportType.matches("^(Prescription|Dispensing|Inventory|UserAccess)$")) {
                errors.add("Unsupported report type: " + reportType);
            }
        }

        if (!hasValidDateRange()) {
            errors.add("Start date must not be after end date");
        }

        return errors;
    }
}
