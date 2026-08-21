package pharmacy_system.storage.management_dss;

import pharmacy_system.model.management_dss.Report;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ReportStorage using HashMap.
 * Thread-safe with atomic ID generation.
 * Reports are immutable snapshots stored as generated; no updates occur after creation.
 */
public class InMemoryReportStorage implements ReportStorage {
    private final Map<Long, Report> store = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> typeIndex = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> userIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Report create(Report report) {
        long reportId = idGenerator.getAndIncrement();
        Report created = new Report(
                reportId,
                report.getReportType(),
                report.getTitle(),
                report.getCriteria(),
                report.getGeneratedBy(),
                report.getGeneratedAt(),
                report.getRowCount(),
                report.getData(),
                1 // version
        );
        store.put(reportId, created);
        typeIndex.computeIfAbsent(report.getReportType(), k -> new ArrayList<>()).add(reportId);
        userIndex.computeIfAbsent(report.getGeneratedBy(), k -> new ArrayList<>()).add(reportId);
        return created;
    }

    @Override
    public Optional<Report> findById(long reportId) {
        return Optional.ofNullable(store.get(reportId));
    }

    @Override
    public List<Report> listAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Report> listByType(String reportType) {
        return typeIndex.getOrDefault(reportType, Collections.emptyList()).stream()
                .map(id -> store.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Report> listByDateRange(LocalDate startDate, LocalDate endDate) {
        return store.values().stream()
                .filter(report -> {
                    LocalDate generatedDate = report.getGeneratedAt().toLocalDate();
                    return !generatedDate.isBefore(startDate) && !generatedDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Report> listByGeneratedBy(long generatedBy) {
        return userIndex.getOrDefault(generatedBy, Collections.emptyList()).stream()
                .map(id -> store.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(long reportId) {
        Report report = store.remove(reportId);
        if (report == null) return false;
        typeIndex.getOrDefault(report.getReportType(), new ArrayList<>()).remove(reportId);
        userIndex.getOrDefault(report.getGeneratedBy(), new ArrayList<>()).remove(reportId);
        return true;
    }
}
