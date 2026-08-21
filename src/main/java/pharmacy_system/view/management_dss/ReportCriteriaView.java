package pharmacy_system.view.management_dss;

import pharmacy_system.model.management_dss.ReportCriteria;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReportCriteriaView collects and validates report generation criteria from the user.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Displays a form for selecting report type, date range, and optional filters
 *   <li>Collects user input: report type, start date, end date, filters
 *   <li>Validates input before submission
 *   <li>Displays inline validation errors (Requirement 10.8 - invalid criteria state)
 *   <li>Delegates successful criteria submission to GenerateReportsView
 * </ul>
 *
 * <p>Supported Report Types:
 * <ul>
 *   <li>Prescription: prescription data within a date range
 *   <li>Dispensing: dispensing transaction data within a date range
 *   <li>Inventory: current inventory snapshot (no date range)
 *   <li>UserAccess: user account data (no date range)
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>Requirement 10.1, 10.2: Report generation with criteria
 *   <li>Requirement 10.8: Invalid criteria validation and error display
 * </ul>
 */
public class ReportCriteriaView {
    private final GenerateReportsView parentView;

    private String reportType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, String> filters;

    /**
     * Constructs a ReportCriteriaView with a reference to its parent GenerateReportsView.
     *
     * @param parentView the parent GenerateReportsView that this component is part of
     */
    public ReportCriteriaView(GenerateReportsView parentView) {
        this.parentView = parentView;
        this.filters = new HashMap<>();
    }

    /**
     * Displays the criteria collection form and waits for user input.
     *
     * <p>The form presents:
     * <ul>
     *   <li>Report type selector (dropdown: Prescription, Dispensing, Inventory, UserAccess)
     *   <li>Start date picker (required for Prescription and Dispensing)
     *   <li>End date picker (required for Prescription and Dispensing)
     *   <li>Optional filters panel (varies by report type)
     *   <li>Submit button
     *   <li>Cancel button (returns to main menu)
     * </ul>
     *
     * <p>The view blocks until the user submits or cancels. On submission, collectCriteria()
     * is called to gather the input and validate it.
     */
    public void collectCriteria() {
        // TODO: Display the criteria form UI (deferred to UI toolkit implementation)
        // Placeholder for UI rendering logic:
        // - Render report type dropdown
        // - Render date range inputs (conditionally visible based on report type)
        // - Render filters panel
        // - Bind submit/cancel handlers
    }

    /**
     * Submits the collected criteria for report generation.
     *
     * <p>Flow:
     * <ul>
     *   <li>Gathers current form input into a ReportCriteria object
     *   <li>Delegates to the parent GenerateReportsView to handle submission
     *   <li>Parent view will validate, generate, and display the report
     * </ul>
     */
    public void submitCriteria() {
        // Build ReportCriteria from the form input
        ReportCriteria criteria = new ReportCriteria(reportType, startDate, endDate, filters);

        // Delegate to parent view for generation
        parentView.handleCriteriaSubmission(criteria);
    }

    /**
     * Displays validation errors for invalid criteria.
     *
     * <p>Requirement 10.8: If criteria are unsupported or invalid, display error messages
     * without generating or persisting a report.
     *
     * <p>Error states include:
     * <ul>
     *   <li>Unsupported report type
     *   <li>Invalid date range (startDate > endDate)
     *   <li>Missing required fields for the selected report type
     * </ul>
     *
     * @param errors a list of validation error messages
     */
    public void showValidationError(List<String> errors) {
        // TODO: Display inline validation errors in the form
        // Placeholder for UI rendering logic:
        // - Show error banner with list of error messages
        // - Highlight invalid fields
        // - Keep form visible for correction
    }

    /**
     * Sets the report type from user input.
     *
     * @param reportType the selected report type (Prescription, Dispensing, Inventory, or UserAccess)
     */
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    /**
     * Sets the start date from user input.
     *
     * @param startDate the selected start date (may be null for report types that don't use dates)
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Sets the end date from user input.
     *
     * @param endDate the selected end date (may be null for report types that don't use dates)
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Sets or adds a filter to the criteria.
     *
     * @param key the filter key
     * @param value the filter value
     */
    public void setFilter(String key, String value) {
        this.filters.put(key, value);
    }

    /**
     * Gets the currently collected criteria from the form.
     *
     * <p>Useful for retrieving user input before submission.
     *
     * @return a ReportCriteria object with current form values
     */
    public ReportCriteria getCriteria() {
        return new ReportCriteria(reportType, startDate, endDate, filters);
    }
}
