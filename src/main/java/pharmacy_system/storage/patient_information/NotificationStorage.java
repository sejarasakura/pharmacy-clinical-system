package pharmacy_system.storage.patient_information;

import pharmacy_system.model.patient_information.Notification;
import java.util.List;
import java.util.Optional;

/**
 * Storage contract for Notification entities.
 * Supports CRUD operations and deduplication queries.
 * Notifications are persisted when generated, and deduplication is enforced at storage layer.
 * Implements optimistic concurrency control via version fields.
 */
public interface NotificationStorage {

    /**
     * Creates and persists a new Notification.
     * @param notification the Notification to create (must have patientId, prescriptionId, eventType set)
     * @return the created Notification with assigned notificationId and version set to 1
     */
    Notification create(Notification notification);

    /**
     * Retrieves a Notification by its unique ID.
     * @param notificationId the notification ID
     * @return Optional containing the Notification if found, empty otherwise
     */
    Optional<Notification> findById(long notificationId);

    /**
     * Retrieves all Notifications for a specific Patient.
     * @param patientId the patient ID
     * @return a list of Notifications belonging to the patient
     */
    List<Notification> findByPatientId(long patientId);

    /**
     * Retrieves a Notification by its deduplication key.
     * The deduplication key is derived from (patientId, prescriptionId, eventType).
     * @param deduplicationKey the deduplication key
     * @return Optional containing the Notification if found, empty otherwise
     */
    Optional<Notification> findByDeduplicationKey(String deduplicationKey);

    /**
     * Checks if a Notification with a given deduplication key already exists.
     * Used for deduplication support to prevent duplicate successful notifications.
     * @param deduplicationKey the deduplication key
     * @return true if a notification with this key exists, false otherwise
     */
    boolean existsByDeduplicationKey(String deduplicationKey);

    /**
     * Updates an existing Notification with optimistic concurrency control.
     * @param notification the updated Notification (must have version set to the current value)
     * @param expectedVersion the version at the time of the last read (must match current to succeed)
     * @return true if update succeeds, false if version conflict detected
     * @throws IllegalArgumentException if the notification does not exist
     */
    boolean update(Notification notification, long expectedVersion);

    /**
     * Deletes a Notification by ID.
     * @param notificationId the notification ID to delete
     * @return true if deletion succeeds, false if the notification does not exist
     */
    boolean delete(long notificationId);

    /**
     * Retrieves all Notifications.
     * @return a list of all Notifications in the system
     */
    List<Notification> listAll();
}
