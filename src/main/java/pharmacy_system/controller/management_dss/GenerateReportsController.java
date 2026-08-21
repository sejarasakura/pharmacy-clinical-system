package pharmacy_system.controller.management_dss;

import pharmacy_system.controller.common.SessionController;
import pharmacy_system.model.management_dss.Report;
import pharmacy_system.model.management_dss.ReportCriteria;
import pharmacy_system.storage.clinical_prescription.PrescriptionStorage;
import pharmacy_system.storage.management_dss.ReportStorage;
import pharmacy_system.storage.pharmacy_operations.DispenseStorage;
import pharmacy_system.storage.pharmacy_operations.InventoryStorage;
import pharmacy_system.storage.pharmacy_operations.MedicineStorage;
import pharmacy_system.storage.security_user.UserAccountStorage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pharmacy_system.view.common.Flash;

/**
 * Implements UCD-09 (Generate Reports) with read-only, cross-domain operational queries
 * and immutable report snapshot persistence (Algorithm 7).
 *
 * <p>Owns:
 * <ul>
 *   <li>Report generation from operational data using read-only queries across multiple domains
 *   <li>Report snapshot persistence as minified JSON (immutable after creation)
 *   <li>Report retrieval and PDF export from saved snapshots (not from live data)
 * </ul>
 *
 * <p>Must not own: any operational mutation, prescription authoring, stock changes, notification delivery.
 *
 * <p>Implements Algorithm 7 (Report Generation and Snapshot Persistence):
 * <ul>
 *   <li>Enforce Administrator authorisation before any operation
 *   <li>Validate criteria against the supported report types and date ranges
 *   <li>Query each operational storage read-only, building row data as it existed at generation time
 *   <li>Persist the snapshot as-is without subsequent modification
 *   <li>Export from the saved snapshot, never from live data
 * </ul>
 *
 * <p>Requirements:
 * <ul>
 *   <li>FR-050, FR-051, FR-052, FR-054: Report generation, snapshot persistence, immutability, export
 *   <li>SC-013, SC-014: Read-only cross-domain access, snapshot derivation, consistency
 * </ul>
 *
 * <p>Properties:
 * <ul>
 *   <li>Property 15: Report snapshot immutability (Validates Requirements 10.4)
 * </ul>
 */
@Controller
public class GenerateReportsController {

    private final SessionController session;
    private final PrescriptionStorage prescriptionStorage;
    private final DispenseStorage dispenseStorage;
    private final InventoryStorage inventoryStorage;
    private final MedicineStorage medicineStorage;
    private final UserAccountStorage userAccountStorage;
    private final ReportStorage reportStorage;

    /**
     * Constructs a GenerateReportsController with all required storage dependencies.
     *
     * @param session the authenticated session controller (enforces permission)
     * @param prescriptionStorage storage for Prescription queries
     * @param dispenseStorage storage for DispenseRecord queries
     * @param inventoryStorage storage for InventoryItem queries
     * @param medicineStorage storage for Medicine queries
     * @param userAccountStorage storage for UserAccount queries
     * @param reportStorage storage for Report persistence
     */
    public GenerateReportsController(
            SessionController session,
            PrescriptionStorage prescriptionStorage,
            DispenseStorage dispenseStorage,
            InventoryStorage inventoryStorage,
            MedicineStorage medicineStorage,
            UserAccountStorage userAccountStorage,
            ReportStorage reportStorage) {
        this.session = session;
        this.prescriptionStorage = prescriptionStorage;
        this.dispenseStorage = dispenseStorage;
        this.inventoryStorage = inventoryStorage;
        this.medicineStorage = medicineStorage;
        this.userAccountStorage = userAccountStorage;
        this.reportStorage = reportStorage;
    }

    /**
     * Generates an operational report from the specified criteria and persists it
     * as an immutable snapshot.
     *
     * <p>Enforces:
     * <ul>
     *   <li>Administrator authorisation (Requirement 10.6)
     *   <li>Valid criteria (supported report type, valid date range) (Requirement 10.8)
     *   <li>Read-only cross-domain queries (Requirement 10.1, 10.2, SC-013)
     *   <li>Empty snapshot when no data matches (Requirement 10.5)
     * </ul>
     *
     * <p>Supported report types:
     * <ul>
     *   <li>Prescription: prescription data within a date range
     *   <li>Dispensing: dispensing transaction data within a date range
     *   <li>Inventory: current inventory snapshot (no date range; filters optional)
     *   <li>UserAccess: user account data (no date range; filters optional)
     * </ul>
     *
     * <p>Algorithm 7 implementation: validates criteria, performs read-only cross-domain
     * queries capturing data as it existed at generation time, builds the row list,
     * creates and persists the Report snapshot, and returns it.
     *
     * @param criteria the report criteria (report type, date range, filters)
     * @return the generated and persisted Report with assigned reportId and snapshot
     * @throws SessionController.SessionExpiredException if session has expired or no session established
     * @throws SessionController.InsufficientPermissionException if session lacks Administrator permission
     * @throws IllegalArgumentException if criteria are invalid (unsupported type, invalid dates, etc.)
     */
    public Report generateReport(ReportCriteria criteria) {
        // Enforce Administrator permission (Requirement 10.6)
        session.requirePermission("GENERATE_REPORTS");

        // Validate criteria (Requirement 10.8)
        List<String> validationErrors = criteria.validateCriteria();
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Invalid report criteria: " + validationErrors);
        }

        // Algorithm 7: Query operational storages read-only, capturing data as it existed at generation time
        List<Map<String, Object>> rows = queryOperationalData(criteria);

        // Create the Report snapshot (immutable after creation)
        Report report = new Report();
        report.setReportType(criteria.getReportType());
        report.setTitle(buildReportTitle(criteria));
        report.setCriteria(criteria);
        report.setGeneratedBy(session.getCurrentUserId());
        report.setGeneratedAt(LocalDateTime.now());
        report.setRowCount(rows.size());
        report.setData(rows);
        report.setVersion(0);  // Will be set to 1 by storage on creation

        // Persist the snapshot (Requirement 10.1, 10.2, 10.5, FR-052)
        Report persisted = reportStorage.create(report);

        return persisted;
    }

    /**
     * Persists an existing Report snapshot.
     *
     * <p>This method is typically called after generateReport() has created and persisted
     * a Report. In this design, persistence happens as part of generateReport(), so this
     * method is provided for API consistency and future extensibility.
     *
     * @param report the Report to persist
     * @return the persisted Report with assigned reportId if not already set
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public Report persistReport(Report report) {
        // Enforce Administrator permission
        session.requirePermission("GENERATE_REPORTS");

        // If the report has not yet been persisted (reportId == 0), create it
        if (report.getReportId() == 0) {
            return reportStorage.create(report);
        }

        // Report already persisted
        return report;
    }

    /**
     * Exports a previously generated Report to the specified format.
     *
     * <p>The export is derived solely from the saved Report snapshot (Requirement 10.4, FR-054).
     * Live operational data is not queried or used.
     *
     * <p>Enforces:
     * <ul>
     *   <li>Administrator authorisation
     *   <li>Report must exist (Requirement 10.7)
     *   <li>Snapshot must be available
     *   <li>Export is deterministic (always produces the same output for the same snapshot)
     * </ul>
     *
     * @param report the Report to export (must be a persisted report with valid data)
     * @param format the export format (e.g., "PDF", "CSV")
     * @return the exported bytes
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     * @throws IllegalArgumentException if the report is not persisted or has no snapshot
     */
    public byte[] exportReport(Report report, String format) {
        // Enforce Administrator permission
        session.requirePermission("GENERATE_REPORTS");

        // Verify the report exists and has a valid snapshot
        if (report == null || report.getReportId() == 0) {
            throw new IllegalArgumentException("Report not found or not persisted");
        }

        if (report.getData() == null) {
            throw new IllegalArgumentException("Report snapshot unavailable");
        }

        // Algorithm 7: Export from the saved snapshot, not from live data (Requirement 10.4, SC-014)
        return report.generateExport(format);
    }

    /**
     * Retrieves a previously generated Report by its ID.
     *
     * <p>Enforces Administrator authorisation.
     *
     * @param reportId the report ID
     * @return Optional containing the Report if found and accessible, empty otherwise
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public Optional<Report> retrieveReport(long reportId) {
        // Enforce Administrator permission
        session.requirePermission("GENERATE_REPORTS");

        return reportStorage.findById(reportId);
    }

    /**
     * Lists all generated Reports.
     *
     * @return a list of all Reports
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public List<Report> listReports() {
        // Enforce Administrator permission
        session.requirePermission("GENERATE_REPORTS");

        return reportStorage.listAll();
    }

    /**
     * Lists all Reports of a specific type.
     *
     * @param reportType the report type filter (e.g., "Prescription", "Dispensing")
     * @return a list of Reports matching the type
     * @throws SessionController.SessionExpiredException if session has expired
     * @throws SessionController.InsufficientPermissionException if session lacks permission
     */
    public List<Report> listReportsByType(String reportType) {
        // Enforce Administrator permission
        session.requirePermission("GENERATE_REPORTS");

        return reportStorage.listByType(reportType);
    }

    /**
     * Queries operational storage read-only to build report rows.
     *
     * <p>Based on the report type, delegates to the appropriate storage's queryForReport method,
     * capturing data as it existed at the time of this call. No operational data is mutated.
     *
     * <p>Supports:
     * <ul>
     *   <li>Prescription: queries PrescriptionStorage by date range
     *   <li>Dispensing: queries DispenseStorage by date range
     *   <li>Inventory: queries InventoryStorage with filters
     *   <li>UserAccess: queries UserAccountStorage with filters
     * </ul>
     *
     * @param criteria the report criteria
     * @return a list of Maps representing the report rows
     */
    private List<Map<String, Object>> queryOperationalData(ReportCriteria criteria) {
        String reportType = criteria.getReportType();

        return switch (reportType) {
            case "Prescription" -> {
                // Prescription Report: query by date range (Requirement 10.1)
                yield prescriptionStorage.queryForReport(
                        criteria.getStartDate(),
                        criteria.getEndDate(),
                        criteria.getFilters()
                );
            }
            case "Dispensing" -> {
                // Dispensing Report: query by date range (Requirement 10.2)
                yield dispenseStorage.queryForReport(
                        criteria.getStartDate(),
                        criteria.getEndDate(),
                        criteria.getFilters()
                );
            }
            case "Inventory" -> {
                // Inventory Report: query with filters only (no date range)
                yield inventoryStorage.queryForReport(criteria.getFilters());
            }
            case "UserAccess" -> {
                // UserAccess Report: query with filters only (no date range)
                yield userAccountStorage.queryForReport(criteria.getFilters());
            }
            default ->
                // Should not reach here if validateCriteria passed
                new ArrayList<>();
        };
    }

    /**
     * Builds a human-readable title for the report based on its criteria.
     *
     * @param criteria the report criteria
     * @return a report title
     */
    private String buildReportTitle(ReportCriteria criteria) {
        String baseTitle = switch (criteria.getReportType()) {
            case "Prescription" -> "Prescription Report";
            case "Dispensing" -> "Dispensing Report";
            case "Inventory" -> "Inventory Report";
            case "UserAccess" -> "User Access Report";
            default -> "Report";
        };

        if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
            return baseTitle + " (" + criteria.getStartDate() + " to " + criteria.getEndDate() + ")";
        }

        return baseTitle;
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("reports", listReports());
        reportPage(model, "Reports");
        return "reports/list";
    }

    @GetMapping("/admin/reports/new")
    public String criteria(@RequestParam(required = false) String type, Model model) {
        session.requirePermission("GENERATE_REPORTS");
        model.addAttribute("reportType", type);
        reportPage(model, "Report criteria");
        return "reports/criteria";
    }

    @PostMapping("/admin/reports/generate")
    public String generate(@RequestParam String reportType,
                           @RequestParam(required = false) LocalDate startDate,
                           @RequestParam(required = false) LocalDate endDate,
                           Model model, RedirectAttributes redirect) {
        ReportCriteria criteria = new ReportCriteria(reportType, startDate, endDate, Map.of());
        if (!criteria.hasValidDateRange()) {
            model.addAttribute("reportType", reportType);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("dateError", "Start date must not be after end date.");
            reportPage(model, "Report criteria");
            return "reports/criteria";
        }
        Report report = generateReport(criteria);
        redirect.addFlashAttribute("flash", new Flash("success", "Report snapshot generated."));
        return "redirect:/admin/reports/" + report.getReportId();
    }

    @GetMapping("/admin/reports/{id}")
    public String report(@PathVariable long id, Model model) {
        Optional<Report> report = retrieveReport(id);
        if (report.isEmpty()) return "errors/not-found";
        model.addAttribute("report", report.get());
        reportPage(model, report.get().getTitle());
        return "reports/result";
    }

    @GetMapping("/admin/reports/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable long id) {
        Optional<Report> report = retrieveReport(id);
        if (report.isEmpty()) return ResponseEntity.notFound().build();
        byte[] data = exportReport(report.get(), "PDF");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    private void reportPage(Model model, String title) {
        model.addAttribute("title", title);
        model.addAttribute("breadcrumb", "Reports / " + title);
    }
}
