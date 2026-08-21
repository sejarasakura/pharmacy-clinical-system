package pharmacy_system.storage.clinical_prescription;

import pharmacy_system.model.clinical_prescription.Prescription;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of PrescriptionStorage using HashMap.
 * Thread-safe with atomic ID generation and version-checked updates.
 */
public class InMemoryPrescriptionStorage implements PrescriptionStorage {
    private final Map<Long, Prescription> store = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> patientIndex = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> doctorIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Prescription create(Prescription prescription) {
        long prescriptionId = idGenerator.getAndIncrement();
        Prescription created = new Prescription(
                prescriptionId,
                prescription.getPatientId(),
                prescription.getDoctorId(),
                prescription.getClinicalNotes(),
                prescription.getStatus(),
                prescription.getIssuedAt(),
                prescription.getStatusChangedAt(),
                prescription.getStatusChangedBy(),
                prescription.getStatusChangeReason(),
                prescription.getCreatedAt(),
                prescription.getUpdatedAt(),
                1, // version
                prescription.getItems()
        );
        store.put(prescriptionId, created);
        patientIndex.computeIfAbsent(prescription.getPatientId(), k -> new ArrayList<>()).add(prescriptionId);
        doctorIndex.computeIfAbsent(prescription.getDoctorId(), k -> new ArrayList<>()).add(prescriptionId);
        return created;
    }

    @Override
    public Optional<Prescription> findById(long prescriptionId) {
        return Optional.ofNullable(store.get(prescriptionId));
    }

    @Override
    public List<Prescription> findByPatientId(long patientId) {
        return patientIndex.getOrDefault(patientId, Collections.emptyList()).stream()
                .map(id -> store.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Prescription> findByDoctorId(long doctorId) {
        return doctorIndex.getOrDefault(doctorId, Collections.emptyList()).stream()
                .map(id -> store.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean update(Prescription prescription, long expectedVersion) {
        Prescription existing = store.get(prescription.getPrescriptionId());
        if (existing == null) {
            throw new IllegalArgumentException("Prescription not found: " + prescription.getPrescriptionId());
        }
        if (existing.getVersion() != expectedVersion) {
            return false; // version conflict
        }
        Prescription updated = new Prescription(
                prescription.getPrescriptionId(),
                prescription.getPatientId(),
                prescription.getDoctorId(),
                prescription.getClinicalNotes(),
                prescription.getStatus(),
                prescription.getIssuedAt(),
                prescription.getStatusChangedAt(),
                prescription.getStatusChangedBy(),
                prescription.getStatusChangeReason(),
                prescription.getCreatedAt(),
                prescription.getUpdatedAt(),
                expectedVersion + 1, // increment version
                prescription.getItems()
        );
        store.put(prescription.getPrescriptionId(), updated);
        return true;
    }

    @Override
    public boolean delete(long prescriptionId) {
        Prescription prescription = store.remove(prescriptionId);
        if (prescription == null) return false;
        patientIndex.getOrDefault(prescription.getPatientId(), new ArrayList<>()).remove(prescriptionId);
        doctorIndex.getOrDefault(prescription.getDoctorId(), new ArrayList<>()).remove(prescriptionId);
        return true;
    }

    @Override
    public List<Prescription> listAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Map<String, Object>> queryForReport(LocalDate startDate, LocalDate endDate, Map<String, String> filters) {
        return store.values().stream()
                .filter(rx -> {
                    LocalDate issuedDate = rx.getIssuedAt() != null ? rx.getIssuedAt().toLocalDate() : null;
                    if (issuedDate != null) {
                        return !issuedDate.isBefore(startDate) && !issuedDate.isAfter(endDate);
                    }
                    return false;
                })
                .map(rx -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("prescriptionId", rx.getPrescriptionId());
                    row.put("patientId", rx.getPatientId());
                    row.put("doctorId", rx.getDoctorId());
                    row.put("status", rx.getStatus().name());
                    row.put("issuedAt", rx.getIssuedAt());
                    row.put("clinicalNotes", rx.getClinicalNotes());
                    return row;
                })
                .collect(Collectors.toList());
    }
}
