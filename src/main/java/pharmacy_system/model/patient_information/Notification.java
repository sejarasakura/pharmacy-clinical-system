package pharmacy_system.model.patient_information;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Represents a persisted patient-facing notification.
 * Generated from domain events (e.g. prescription issued, medication ready).
 * Tracks delivery status, read state, and deduplication.
 * 
 * Supported events: Prescription Issued, Prescription Cancelled, Medication Preparing,
 *                  Medication Ready for Collection, Medication Dispensed.
 * 
 * [UCD-03]
 */
public class Notification {
    public static final String DELIVERY_STATUS_PENDING = "PENDING";
    public static final String DELIVERY_STATUS_DELIVERED = "DELIVERED";
    public static final String DELIVERY_STATUS_FAILED = "FAILED";
    public static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
            "PRESCRIPTION_ISSUED", "PRESCRIPTION_CANCELLED", "MEDICATION_PREPARING",
            "MEDICATION_READY_FOR_COLLECTION", "MEDICATION_DISPENSED");
    private long notificationId;
    private long patientId;
    private long prescriptionId;
    private String eventType;          // e.g. PRESCRIPTION_ISSUED
    private String notificationType;   // e.g. INFO, WARNING, SUCCESS
    private String title;
    private String message;
    private String recipient;          // e.g. patient email or phone
    private String deliveryStatus;     // PENDING, DELIVERED, FAILED
    private String deduplicationKey;   // patientId + prescriptionId + eventType
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime failedAt;
    private LocalDateTime readAt;
    private String failureReason;

    // Constructor
    public Notification(long notificationId, long patientId, long prescriptionId,
                       String eventType, String notificationType, String title,
                       String message, String recipient) {
        this.notificationId = notificationId;
        this.patientId = patientId;
        this.prescriptionId = prescriptionId;
        this.eventType = eventType;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.recipient = recipient;
        this.deliveryStatus = DELIVERY_STATUS_PENDING;
        this.deduplicationKey = patientId + ":" + prescriptionId + ":" + eventType;
        this.createdAt = LocalDateTime.now();
        this.deliveredAt = null;
        this.failedAt = null;
        this.readAt = null;
        this.failureReason = null;
    }

    public Notification(long notificationId, long patientId, long prescriptionId,
                        String eventType, String notificationType, String title,
                        String message, String recipient, String deliveryStatus,
                        LocalDateTime createdAt, LocalDateTime deliveredAt,
                        LocalDateTime failedAt, String failureReason, LocalDateTime readAt) {
        this.notificationId = notificationId;
        this.patientId = patientId;
        this.prescriptionId = prescriptionId;
        this.eventType = eventType;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.recipient = recipient;
        this.deliveryStatus = deliveryStatus;
        this.deduplicationKey = buildDeduplicationKey(patientId, prescriptionId, eventType);
        this.createdAt = createdAt;
        this.deliveredAt = deliveredAt;
        this.failedAt = failedAt;
        this.failureReason = failureReason;
        this.readAt = readAt;
    }

    // Getters
    public long getNotificationId() {
        return notificationId;
    }

    public long getPatientId() {
        return patientId;
    }

    public long getPrescriptionId() {
        return prescriptionId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    // Business logic
    /**
     * Mark notification as delivered successfully.
     */
    public void markDelivered() {
        this.deliveryStatus = DELIVERY_STATUS_DELIVERED;
        this.deliveredAt = LocalDateTime.now();
        this.failedAt = null;
        this.failureReason = null;
    }

    /**
     * Mark notification delivery as failed with reason.
     * @param reason the failure reason
     */
    public void markDeliveryFailed(String reason) {
        this.deliveryStatus = DELIVERY_STATUS_FAILED;
        this.failedAt = LocalDateTime.now();
        this.failureReason = reason;
        this.deliveredAt = null;
    }

    /**
     * Mark notification as read by patient.
     */
    public void markRead() {
        if (this.readAt == null) this.readAt = LocalDateTime.now();
    }

    /**
     * Check if notification is in unread state.
     * @return true if not yet read, false otherwise
     */
    public boolean isUnread() {
        return readAt == null;
    }

    public boolean isRead() { return readAt != null; }

    public boolean isPending() { return "PENDING".equals(deliveryStatus); }

    public boolean validateRecipient(boolean patientActive) {
        return patientActive && recipient != null && !recipient.isBlank();
    }

    public void markNotDeliverable(String reason) { markDeliveryFailed(reason); }

    public static String buildDeduplicationKey(long patientId, long prescriptionId, String eventType) {
        return patientId + ":" + prescriptionId + ":" + eventType;
    }

    public String generateDeduplicationKey() {
        return buildDeduplicationKey(patientId, prescriptionId, eventType);
    }

    /**
     * Check if notification delivery was successful.
     * @return true if deliveryStatus is DELIVERED, false otherwise
     */
    public boolean isDelivered() {
        return "DELIVERED".equals(deliveryStatus);
    }

    /**
     * Check if notification delivery failed.
     * @return true if deliveryStatus is FAILED, false otherwise
     */
    public boolean isDeliveryFailed() {
        return "FAILED".equals(deliveryStatus);
    }

    /**
     * Validate supported event types.
     * @param eventType the event type to validate
     * @return true if event type is supported, false otherwise
     */
    public static boolean isSupportedEventType(String eventType) {
        return eventType != null && SUPPORTED_EVENT_TYPES.contains(eventType);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", patientId=" + patientId +
                ", prescriptionId=" + prescriptionId +
                ", eventType='" + eventType + '\'' +
                ", deliveryStatus='" + deliveryStatus + '\'' +
                ", isUnread=" + isUnread() +
                ", createdAt=" + createdAt +
                '}';
    }
}
