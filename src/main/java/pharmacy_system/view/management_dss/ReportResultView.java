package pharmacy_system.view.management_dss;

import pharmacy_system.model.management_dss.Report;

/**
 * ReportResultView displays generated reports and handles export operations.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Displays a generated report in tabular or summary format
 *   <li>Surfaces the empty-result state when a report contains no data (Requirement 10.5)
 *   <li>Provides export options (PDF, CSV, etc.)
 *   <li>Surfaces the not-found state when a report cannot be retrieved (Requirement 10.7)
 *   <li>Surfaces export failure state when export generation fails
 *   <li>Delegates user actions back to GenerateReportsView
 * </ul>
 *
 * <p>Report Display States:
 * <ul>
 *   <li>Normal: Report contains data; display table/summary with export button
 *   <li>Empty: Report persisted but no data matched criteria (Requirement 10.5)
 *   <li>Not Found: Requested report does not exist or snapshot is unavailable (Requirement 10.7)
 *   <li>Export Error: Export generation failed
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>Requirement 10.3: Report retrieval and display
 *   <li>Requirement 10.4: PDF export from saved snapshot only
 *   <li>Requirement 10.5: Empty-result state
 *   <li>Requirement 10.7: Not-found state
 * </ul>
 */
public class ReportResultView {
    private final GenerateReportsView parentView;

    private Report displayedReport;

    /**
     * Constructs a ReportResultView with a reference to its parent GenerateReportsView.
     *
     * @param parentView the parent GenerateReportsView that this component is part of
     */
    public ReportResultView(GenerateReportsView parentView) {
        this.parentView = parentView;
    }

    /**
     * Displays a successfully generated report.
     *
     * <p>The report is displayed in a tabular format with:
     * <ul>
     *   <li>Report title and metadata (type, generated at, row count)
     *   <li>Data rows from the snapshot
     *   <li>Export button (PDF, CSV)
     *   <li>Return to criteria button (to generate another report)
     * </ul>
     *
     * <p>Requirement 10.3: Displays stored Report content identical to the persisted snapshot.
     * Requirement 10.4: Export button is available for PDF export from the saved snapshot.
     *
     * @param report the Report to display (must not be null)
     */
    public void displayReport(Report report) {
        this.displayedReport = report;

        // TODO: Render the report display UI
        // Placeholder for UI rendering logic:
        // - Display report title: report.getTitle()
        // - Display metadata: report.getReportType(), report.getGeneratedAt(), report.getRowCount()
        // - Display data table: iterate report.getData() and render rows
        // - Show export button with format selector (PDF, CSV)
        // - Show "Back to Criteria" button to return to generation form
    }

    /**
     * Displays the empty-result state when a report contains no data.
     *
     * <p>Requirement 10.5: If no operational data matches the selected Report_Criteria,
     * the System SHALL persist a Report snapshot containing zero records and return that
     * empty Report result without modifying operational records.
     *
     * <p>The empty-result state shows:
     * <ul>
     *   <li>An empty result message (e.g., "No records match your criteria")
     *   <li>The criteria that were used (to allow refinement)
     *   <li>Option to modify criteria and retry
     *   <li>Option to return to criteria form
     * </ul>
     */
    public void displayNoDataResult() {
        // TODO: Render the empty-result state UI
        // Placeholder for UI rendering logic:
        // - Display message: "No records match your search criteria"
        // - Display the criteria used for reference
        // - Show "Modify Criteria" button (to adjust and retry)
        // - Show "Back to Criteria" button (to clear and start over)
    }

    /**
     * Handles a user export request and delegates to the parent view.
     *
     * <p>Called when the user clicks an export button (e.g., "Export as PDF").
     *
     * @param format the export format (e.g., "PDF", "CSV")
     */
    public void requestExport(String format) {
        // Delegate to parent view to handle the export
        parentView.handleExportRequest(format);
    }

    /**
     * Provides the exported report data to the user.
     *
     * <p>Requirement 10.4: Export is derived solely from the saved Report snapshot,
     * not from live operational data. The exported content is identical to the stored
     * Report content.
     *
     * <p>The export bytes are typically handed to the OS for file download/save.
     *
     * @param exportData the export bytes (e.g., PDF or CSV content)
     */
    public void provideExport(byte[] exportData) {
        // TODO: Provide the export to the user (file download/save dialog)
        // Placeholder for UI rendering logic:
        // - Trigger file save dialog with appropriate file extension
        // - Use report metadata to suggest a filename (e.g., "Prescription_Report_2025-01-15.pdf")
        // - Write exportData to the file
    }

    /**
     * Displays an error state for export or not-found scenarios.
     *
     * <p>Error scenarios include:
     * <ul>
     *   <li>Requirement 10.7: Report not found or snapshot unavailable
     *   <li>Export generation failure
     *   <li>File save failure
     * </ul>
     *
     * <p>Error message depends on the context (set by parent view).
     */
    public void showExportError() {
        // TODO: Display error state UI
        // Placeholder for UI rendering logic:
        // - Display error banner with message (varies by context)
        // - For not-found: "Report not found or snapshot unavailable"
        // - For export failure: "Failed to export report"
        // - Show "Back to Criteria" button to return to generation form
        // - Show "Back to List" button to view other reports (if applicable)
    }

    /**
     * Gets the currently displayed report.
     *
     * @return the Report currently displayed, or null if none is displayed
     */
    public Report getDisplayedReport() {
        return displayedReport;
    }
}
