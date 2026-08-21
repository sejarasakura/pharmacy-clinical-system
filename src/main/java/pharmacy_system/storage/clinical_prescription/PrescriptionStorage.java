package pharmacy_system.storage.clinical_prescription;

import pharmacy_system.model.clinical_prescription.Prescription;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;

/**
 * Storage contract for Prescription entities.
 * Supports CRUD operations and domain-specific queries.
 * Implements optimistic concurrency control via version fields.
 */
public interface PrescriptionStorage {

    /**
     * Retrieves a Prescription by its unique ID.
     * 
     * @param prescriptionId the prescription ID
     * @return the Prescription if found, null otherwise
     */
    Optional<Prescription> findById(long prescriptionId);

    Prescription create(Prescription prescription);

    /**
     * Retrieves all Prescriptions for a specific Patient.
     * 
     * @param patientId the patient ID
     * @return a list of Prescriptions belonging to the patient
     */
    List<Prescription> findByPatientId(long patientId);

    /**
     * Retrieves all Prescriptions authored by a specific Doctor.
     * 
     * @param doctorId the doctor ID
     * @return a list of Prescriptions authored by the doctor
     */
    List<Prescription> findByDoctorId(long doctorId);

    /**
     * Persists a new or modified Prescription to storage.
     * 
     * @param prescription the Prescription to save
     * @return true if the save succeeds, false otherwise
     */
    default boolean save(Prescription prescription) {
        if (prescription == null) return false;
        if (prescription.getPrescriptionId() == 0L) return create(prescription) != null;
        return update(prescription, prescription.getVersion());
    }

    /**
     * Updates an existing Prescription with optimistic concurrency control.
     * The update only succeeds if the stored version matches expectedVersion.
     * 
     * @param prescription the updated Prescription (must have version set)
     * @param expectedVersion the version at the time of the last read
     * @return true if update succeeds, false if version conflict or prescription not found
     */
    boolean update(Prescription prescription, long expectedVersion);

    /**
     * Checks if a Prescription exists by ID.
     * 
     * @param prescriptionId the prescription ID
     * @return true if the prescription exists, false otherwise
     */
    default boolean existsById(long prescriptionId) { return findById(prescriptionId).isPresent(); }

    boolean delete(long prescriptionId);

    List<Prescription> listAll();

    List<Map<String, Object>> queryForReport(LocalDate startDate, LocalDate endDate,
                                              Map<String, String> filters);
}
