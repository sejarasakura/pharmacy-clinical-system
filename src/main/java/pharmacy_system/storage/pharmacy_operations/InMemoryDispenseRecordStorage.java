package pharmacy_system.storage.pharmacy_operations;

import pharmacy_system.model.pharmacy_operations.DispenseRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of DispenseStorage using HashMap.
 * Thread-safe with atomic ID generation and version-checked updates.
 * Maintains indices for duplicate-dispense detection and fast lookups.
 */
public class InMemoryDispenseRecordStorage implements DispenseStorage {
    private final Map<Long, DispenseRecord> store = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> prescriptionIndex = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> patientIndex = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> completedDispensesByPrescription = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public DispenseRecord create(DispenseRecord dispenseRecord) {
        long dispenseId = idGenerator.getAndIncrement();
        DispenseRecord created = dispenseRecord.persistedCopy(dispenseId, 1L);
        store.put(dispenseId, created);
        prescriptionIndex.computeIfAbsent(dispenseRecord.getPrescriptionId(), k -> new ArrayList<>()).add(dispenseId);
        patientIndex.computeIfAbsent(dispenseRecord.getPatientId(), k -> new ArrayList<>()).add(dispenseId);
        if (created.isCompleted()) completedDispensesByPrescription.put(created.getPrescriptionId(), true);
        return created;
    }

    @Override
    public Optional<DispenseRecord> findById(long dispenseId) {
        return Optional.ofNullable(store.get(dispenseId));
    }

    @Override
    public List<DispenseRecord> findByPrescriptionId(long prescriptionId) {
        return prescriptionIndex.getOrDefault(prescriptionId, Collections.emptyList()).stream()
                .map(id -> store.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<DispenseRecord> findByPatientId(long patientId) {
        return patientIndex.getOrDefault(patientId, Collections.emptyList()).stream()
                .map(id -> store.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsCompletedDispense(long prescriptionId) {
        return completedDispensesByPrescription.getOrDefault(prescriptionId, false);
    }

    @Override
    public boolean update(DispenseRecord dispenseRecord, long expectedVersion) {
        DispenseRecord existing = store.get(dispenseRecord.getDispenseId());
        if (existing == null) {
            throw new IllegalArgumentException("DispenseRecord not found: " + dispenseRecord.getDispenseId());
        }
        if (existing.getVersion() != expectedVersion) {
            return false; // version conflict
        }
        DispenseRecord updated = dispenseRecord.persistedCopy(
                dispenseRecord.getDispenseId(), expectedVersion + 1);
        store.put(dispenseRecord.getDispenseId(), updated);
        // Track completed dispenses
        if ("DISPENSED".equals(updated.getStatus())) {
            completedDispensesByPrescription.put(dispenseRecord.getPrescriptionId(), true);
        }
        return true;
    }

    @Override
    public boolean delete(long dispenseId) {
        DispenseRecord dispenseRecord = store.remove(dispenseId);
        if (dispenseRecord == null) return false;
        prescriptionIndex.getOrDefault(dispenseRecord.getPrescriptionId(), new ArrayList<>()).remove(dispenseId);
        patientIndex.getOrDefault(dispenseRecord.getPatientId(), new ArrayList<>()).remove(dispenseId);
        return true;
    }

    @Override
    public List<DispenseRecord> listAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Map<String, Object>> queryForReport(LocalDate startDate, LocalDate endDate, Map<String, String> filters) {
        return store.values().stream()
                .filter(dr -> {
                    LocalDate dispensedDate = dr.getDispensedAt() != null ? dr.getDispensedAt().toLocalDate() : null;
                    if (dispensedDate != null) {
                        return !dispensedDate.isBefore(startDate) && !dispensedDate.isAfter(endDate);
                    }
                    return false;
                })
                .map(dr -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("dispenseId", dr.getDispenseId());
                    row.put("prescriptionId", dr.getPrescriptionId());
                    row.put("patientId", dr.getPatientId());
                    row.put("status", dr.getStatus());
                    row.put("dispensedAt", dr.getDispensedAt());
                    return row;
                })
                .collect(Collectors.toList());
    }
}
