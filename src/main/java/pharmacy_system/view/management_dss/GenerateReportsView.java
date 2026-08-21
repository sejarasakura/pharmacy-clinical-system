package pharmacy_system.view.management_dss;

import pharmacy_system.controller.management_dss.GenerateReportsController;
import pharmacy_system.model.management_dss.Report;
import pharmacy_system.model.management_dss.ReportCriteria;

/**
 * GenerateReportsView is the primary boundary for UCD-09 (Generate Reports).
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Provides a user interface for generating new reports with criteria input
 *   <li>Displays previously generated reports with options for export
 *   <li>Surfaces error states: invalid criteria, not found, generation failure, export failure
 *   <li>Surfaces empty-result state when report data matches no records (Requirement 10.5)
 *   <li>Wires user actions to GenerateReportsController
 * </ul>
 *
 * <p>Structure:
 * <ul>
 *   <li>Internally composes ReportCriteriaView (criteria collection UI)
 *   <li>Internally composes ReportResultView (result display and export UI)
 *   <li>Routes generation and export requests to GenerateReportsController
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>FR-050, FR-051, FR-052, FR-054: Report generation, persistence, immutability, export
 *   <li>Requirement 10.1, 10.3, 10.4, 10.5: Report generation, retrieval, export, empty results
 * </ul>
 */
public class GenerateReportsView {
    private final GenerateReportsController controller;
    private final ReportCriteriaView criteriaView;
    private final ReportResultView resultView;

    private Report currentReport;

    /**
     * Constructs a GenerateReportsView with its component views and controller.
     *
     * @param controller the GenerateReportsController to handle report operations
     * @param criteriaView the view for collecting report criteria input
     * @param resultView the view for displaying and exporting report results
     */
    public GenerateReportsView(
            GenerateReportsController controller,
            ReportCriteriaView criteriaView,
            ReportResultView resultView) {
        this.controller = controller;
        this.criteriaView = criteriaView;
        this.resultView = resultView;
    }

    /**
     * Opens the reports interface, presenting the criteria form or list of prior reports.
     *
     * <p>This is the entry point for UCD-09. The view displays the report criteria form,
     * allowing the user to select a report type, date range, and filters.
     */
    public void openReports() {
        showCriteriaForm();
    }

    /**
     * Displays the report criteria form, allowing the user to input generation criteria.
     *
     * <p>The criteria form collects:
     * <ul>
     *   <li>Report type (Prescription, Dispensing, Inventory, UserAccess)
     *   <li>Start date and end date (for time-series reports)
     *   <li>Filters (optional, report-type-specific)
     * </ul>
     */
    public void showCriteriaForm() {
        // Delegate to the criteria view to display the form
        criteriaView.collectCriteria();
    }

    /**
     * Handles criteria submission: validates, generates, and displays the report.
     *
     * <p>Flow:
     * <ul>
     *   <li>User submits criteria from ReportCriteriaView
     *   <li>This method receives the submitted ReportCriteria
     *   <li>Calls GenerateReportsController.generateReport()
     *   <li>On success: stores the report and displays it via ReportResultView
     *   <li>On invalid criteria: displays inline validation errors in ReportCriteriaView
     *   <li>On generation failure: displays error via showGenerationError()
     * </ul>
     *
     * @param criteria the report criteria submitted by the user
     */
    public void handleCriteriaSubmission(ReportCriteria criteria) {
        try {
            // Call the controller to generate and persist the report
            Report report = controller.generateReport(criteria);
            this.currentReport = report;

            // Display the generated report
            showReport(report);
        } catch (IllegalArgumentException e) {
            // Invalid criteria (unsupported report type, invalid date range, etc.)
            // Requirement 10.8: reject and display validation errors
            criteriaView.showValidationError(java.util.List.of(e.getMessage()));
        } catch (Exception e) {
            // Other generation failure (authorisation, persistence, etc.)
            showGenerationError("Failed to generate report: " + e.getMessage());
        }
    }

    /**
     * Displays a successfully generated report.
     *
     * <p>Delegates to ReportResultView to render the report. If the report is empty
     * (no data matched the criteria, Requirement 10.5), ReportResultView shows an
     * empty-result state instead.
     *
     * @param report the Report to display
     */
    public void showReport(Report report) {
        this.currentReport = report;
        if (report.isEmpty()) {
            // Requirement 10.5: empty result state
            resultView.displayNoDataResult();
        } else {
            // Normal report display
            resultView.displayReport(report);
        }
    }

    /**
     * Displays a generation error (invalid criteria, unauthorised, etc.).
     *
     * <p>Shown when:
     * <ul>
     *   <li>Criteria validation fails (Requirement 10.8)
     *   <li>Session expired or insufficient permission (Requirement 10.6)
     *   <li>Report generation or persistence fails (FR-050, FR-051)
     * </ul>
     *
     * @param message a human-readable error message
     */
    public void showGenerationError(String message) {
        // Display the error to the user; may include option to retry or return to criteria form
        resultView.showExportError();
    }

    /**
     * Handles an export request from the user.
     *
     * <p>Flow:
     * <ul>
     *   <li>User requests export from ReportResultView (e.g., "Export as PDF")
     *   <li>This method calls controller.exportReport() to generate the export bytes
     *   <li>On success: provides the export via resultView.provideExport()
     *   <li>On failure: displays error via showExportError()
     * </ul>
     *
     * @param format the export format (e.g., "PDF")
     */
    public void handleExportRequest(String format) {
        if (currentReport == null) {
            showGenerationError("No report to export");
            return;
        }

        try {
            // Call the controller to export the report snapshot
            byte[] exportData = controller.exportReport(currentReport, format);
            resultView.provideExport(exportData);
        } catch (Exception e) {
            // Export failure (report not found, snapshot unavailable, etc.)
            // Requirement 10.7: not-found state
            resultView.showExportError();
        }
    }

    /**
     * Handles retrieval of a previously generated report by ID.
     *
     * <p>Flow:
     * <ul>
     *   <li>User selects a report from a list
     *   <li>This method calls controller.retrieveReport() to fetch it
     *   <li>On success: displays the report
     *   <li>On not found: displays not-found error
     * </ul>
     *
     * @param reportId the ID of the report to retrieve
     */
    public void handleReportRetrieval(long reportId) {
        try {
            var reportOptional = controller.retrieveReport(reportId);
            if (reportOptional.isPresent()) {
                Report report = reportOptional.get();
                this.currentReport = report;
                showReport(report);
            } else {
                // Requirement 10.7: not-found state
                resultView.showExportError();
            }
        } catch (Exception e) {
            showGenerationError("Failed to retrieve report: " + e.getMessage());
        }
    }

    /**
     * Gets the currently displayed report.
     *
     * @return the Report currently shown in the view, or null if none is displayed
     */
    public Report getCurrentReport() {
        return currentReport;
    }
}
