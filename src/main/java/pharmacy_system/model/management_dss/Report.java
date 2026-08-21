package pharmacy_system.model.management_dss;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Report entity representing a persisted report snapshot.
 * Reports are immutable read-only snapshots generated at a specific point in time.
 * The data is stored as a minified JSON snapshot and never modified after creation.
 */
public class Report {
    private long reportId;
    private String reportType;           // Prescription, Dispensing, Inventory, UserAccess
    private String title;
    private ReportCriteria criteria;
    private long generatedBy;
    private LocalDateTime generatedAt;
    private int rowCount;
    private List<Map<String, Object>> data;  // minified JSON snapshot
    private long version;

    public Report() {
    }

    public Report(long reportId, String reportType, String title, ReportCriteria criteria, 
                  long generatedBy, LocalDateTime generatedAt, int rowCount, 
                  List<Map<String, Object>> data, long version) {
        this.reportId = reportId;
        this.reportType = reportType;
        this.title = title;
        this.criteria = criteria;
        this.generatedBy = generatedBy;
        this.generatedAt = generatedAt;
        this.rowCount = rowCount;
        this.data = data;
        this.version = version;
    }

    // Getters
    public long getReportId() {
        return reportId;
    }

    public String getReportType() {
        return reportType;
    }

    public String getTitle() {
        return title;
    }

    public ReportCriteria getCriteria() {
        return criteria;
    }

    public long getGeneratedBy() {
        return generatedBy;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public int getRowCount() {
        return rowCount;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public long getVersion() {
        return version;
    }

    // Setters
    public void setReportId(long reportId) {
        this.reportId = reportId;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCriteria(ReportCriteria criteria) {
        this.criteria = criteria;
    }

    public void setGeneratedBy(long generatedBy) {
        this.generatedBy = generatedBy;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * Checks if the report contains any data.
     * @return true if the report contains no rows, false otherwise
     */
    public boolean isEmpty() {
        return rowCount == 0 || (data != null && data.isEmpty());
    }

    /**
     * Generates an export of the report in the specified format.
     * The export is derived solely from the stored snapshot data.
     * @param format the export format (e.g., "PDF", "CSV")
     * @return the exported bytes
     */
    public byte[] generateExport(String format) {
        // Deferred: PDF generation library
        // For now, return a placeholder
        return new byte[0];
    }
}
