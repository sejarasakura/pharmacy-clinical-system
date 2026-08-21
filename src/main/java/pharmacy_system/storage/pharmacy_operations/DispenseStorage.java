package pharmacy_system.storage.pharmacy_operations;

import pharmacy_system.model.pharmacy_operations.DispenseRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Storage contract for DispenseRecord entities.
 * Supports CRUD operations and duplicate-dispense detection queries.
 * Implements optimistic concurrency control via version fields.
 */
public interface DispenseStorage {

    /**
     * Creates and persists a new DispenseRecord.
     * @param dispenseRecord the DispenseRecord to create (must have prescriptionId, patientId, requiredQuantities set)
     * @return the created DispenseRecord with assigned dispenseId and version set to 1
     */
    DispenseRecord create(DispenseRecord dispenseRecord);

    /**
     * Retrieves a DispenseRecord by its unique ID.
     * @param dispenseId the dispense record ID
     * @return Optional containing the DispenseRecord if found, empty otherwise
     */
    Optional<DispenseRecord> findById(long dispenseId);

    /**
     * Retrieves all DispenseRecords for a specific Prescription.
     * @param prescriptionId the prescription ID
     * @return a list of DispenseRecords for this prescription
     */
    List<DispenseRecord> findByPrescriptionId(long prescriptionId);

    /**
     * Retrieves all DispenseRecords for a specific Patient.
     * @param patientId the patient ID
     * @return a list of DispenseRecords belonging to this patient
     */
    List<DispenseRecord> findByPatientId(long patientId);

    /**
     * Checks if a completed (DISPENSED) DispenseRecord already exists for a given Prescription.
     * Used to prevent duplicate dispensing and enforce the idempotent-fulfilment property.
     * @param prescriptionId the prescription ID
     * @return true if a completed DispenseRecord exists for this prescription, false otherwise
     */
    boolean existsCompletedDispense(long prescriptionId);

    /**
     * Updates an existing DispenseRecord with optimistic concurrency control.
     * @param dispenseRecord the updated DispenseRecord (must have version set to the current value)
     * @param expectedVersion the version at the time of the last read (must match current to succeed)
     * @return true if update succeeds, false if version conflict detected
     * @throws IllegalArgumentException if the dispense record does not exist
     */
    boolean update(DispenseRecord dispenseRecord, long expectedVersion);

    /**
     * Deletes a DispenseRecord by ID.
     * @param dispenseId the dispense record ID to delete
     * @return true if deletion succeeds, false if the dispense record does not exist
     */
    boolean delete(long dispenseId);

    /**
     * Retrieves all DispenseRecords.
     * @return a list of all DispenseRecords in the system
     */
    List<DispenseRecord> listAll();

    /**
     * Queries DispenseRecords for reporting purposes.
     * Used by the GenerateReportsController to retrieve dispensing reports.
     * @param startDate the start date range (inclusive)
     * @param endDate the end date range (inclusive)
     * @param filters optional filter criteria (implementation-dependent)
     * @return a list of Maps representing DispenseRecord rows matching the criteria
     */
    List<Map<String, Object>> queryForReport(LocalDate startDate, LocalDate endDate, Map<String, String> filters);
}
