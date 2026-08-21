package pharmacy_system.storage.pharmacy_operations;

import pharmacy_system.model.pharmacy_operations.Medicine;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Storage contract for Medicine entities.
 * Supports CRUD operations and lookup by medicine code.
 * Medicine is master data (no quantity; inventory is stored separately in InventoryItem).
 * Implements optimistic concurrency control via version fields.
 */
public interface MedicineStorage {

    /**
     * Creates and persists a new Medicine.
     * @param medicine the Medicine to create (must have medicineName and all mandatory fields set)
     * @return the created Medicine with assigned medicineId and version set to 1
     */
    Medicine create(Medicine medicine);

    /**
     * Retrieves a Medicine by its unique ID.
     * @param medicineId the medicine ID
     * @return Optional containing the Medicine if found, empty otherwise
     */
    Optional<Medicine> findById(long medicineId);

    /**
     * Retrieves a Medicine by its medicine code (unique identifier).
     * @param medicineCode the medicine code to search for
     * @return Optional containing the Medicine if found, empty otherwise
     */
    Optional<Medicine> findByCode(String medicineCode);

    /**
     * Updates an existing Medicine with optimistic concurrency control.
     * @param medicine the updated Medicine (must have version set to the current value)
     * @param expectedVersion the version at the time of the last read (must match current to succeed)
     * @return true if update succeeds, false if version conflict detected
     * @throws IllegalArgumentException if the medicine does not exist
     */
    boolean update(Medicine medicine, long expectedVersion);

    /**
     * Deletes a Medicine by ID.
     * @param medicineId the medicine ID to delete
     * @return true if deletion succeeds, false if the medicine does not exist
     */
    boolean delete(long medicineId);

    /**
     * Retrieves all Medicines.
     * @return a list of all Medicines in the system
     */
    List<Medicine> listAll();

    /**
     * Retrieves all active Medicines.
     * @return a list of Medicines with active flag set to true
     */
    List<Medicine> listActive();

    /**
     * Queries Medicines for reporting purposes.
     * Used by the GenerateReportsController to retrieve inventory reports.
     * @param filters optional filter criteria (implementation-dependent)
     * @return a list of Maps representing Medicine rows matching the filters
     */
    List<Map<String, Object>> queryForReport(Map<String, String> filters);
}
