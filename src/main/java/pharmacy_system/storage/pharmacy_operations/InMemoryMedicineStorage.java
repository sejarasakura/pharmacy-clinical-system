package pharmacy_system.storage.pharmacy_operations;

import pharmacy_system.model.pharmacy_operations.Medicine;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of MedicineStorage using HashMap.
 * Thread-safe with atomic ID generation and version-checked updates.
 * Maintains indices for code lookups and active medicine filtering.
 */
public class InMemoryMedicineStorage implements MedicineStorage {
    private final Map<Long, Medicine> store = new ConcurrentHashMap<>();
    private final Map<String, Long> codeIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Medicine create(Medicine medicine) {
        long medicineId = idGenerator.getAndIncrement();
        Medicine created = new Medicine(
                medicineId,
                medicine.getMedicineCode(),
                medicine.getMedicineName(),
                medicine.getGenericName(),
                medicine.getDosageForm(),
                medicine.getStrength(),
                medicine.getUnit(),
                medicine.getDescription(),
                medicine.isActive(),
                1 // version
        );
        store.put(medicineId, created);
        if (medicine.getMedicineCode() != null) {
            codeIndex.put(medicine.getMedicineCode(), medicineId);
        }
        return created;
    }

    @Override
    public Optional<Medicine> findById(long medicineId) {
        return Optional.ofNullable(store.get(medicineId));
    }

    @Override
    public Optional<Medicine> findByCode(String medicineCode) {
        Long medicineId = codeIndex.get(medicineCode);
        if (medicineId == null) return Optional.empty();
        return Optional.ofNullable(store.get(medicineId));
    }

    @Override
    public boolean update(Medicine medicine, long expectedVersion) {
        Medicine existing = store.get(medicine.getMedicineId());
        if (existing == null) {
            throw new IllegalArgumentException("Medicine not found: " + medicine.getMedicineId());
        }
        if (existing.getVersion() != expectedVersion) {
            return false; // version conflict
        }
        Medicine updated = new Medicine(
                medicine.getMedicineId(),
                medicine.getMedicineCode(),
                medicine.getMedicineName(),
                medicine.getGenericName(),
                medicine.getDosageForm(),
                medicine.getStrength(),
                medicine.getUnit(),
                medicine.getDescription(),
                medicine.isActive(),
                expectedVersion + 1 // increment version
        );
        store.put(medicine.getMedicineId(), updated);
        // Update code index if changed
        if (existing.getMedicineCode() != null && !existing.getMedicineCode().equals(medicine.getMedicineCode())) {
            codeIndex.remove(existing.getMedicineCode());
        }
        if (medicine.getMedicineCode() != null) {
            codeIndex.put(medicine.getMedicineCode(), medicine.getMedicineId());
        }
        return true;
    }

    @Override
    public boolean delete(long medicineId) {
        Medicine medicine = store.remove(medicineId);
        if (medicine == null) return false;
        if (medicine.getMedicineCode() != null) {
            codeIndex.remove(medicine.getMedicineCode());
        }
        return true;
    }

    @Override
    public List<Medicine> listAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Medicine> listActive() {
        return store.values().stream()
                .filter(Medicine::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> queryForReport(Map<String, String> filters) {
        return store.values().stream()
                .map(medicine -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("medicineId", medicine.getMedicineId());
                    row.put("medicineCode", medicine.getMedicineCode());
                    row.put("medicineName", medicine.getMedicineName());
                    row.put("genericName", medicine.getGenericName());
                    row.put("dosageForm", medicine.getDosageForm());
                    row.put("strength", medicine.getStrength());
                    row.put("active", medicine.isActive());
                    return row;
                })
                .collect(Collectors.toList());
    }
}
