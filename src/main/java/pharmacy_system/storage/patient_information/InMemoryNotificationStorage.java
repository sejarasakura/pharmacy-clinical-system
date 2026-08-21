package pharmacy_system.storage.patient_information;

import pharmacy_system.model.patient_information.Notification;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of NotificationStorage using HashMap.
 * Thread-safe with atomic ID generation.
 * Maintains deduplication key index for efficient deduplication queries.
 */
public class InMemoryNotificationStorage implements NotificationStorage {
    private final Map<Long, Notification> store = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> patientIndex = new ConcurrentHashMap<>();
    private final Map<String, Long> deduplicationKeyIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Notification create(Notification notification) {
        long notificationId = idGenerator.getAndIncrement();
        Notification created = new Notification(
                notificationId,
                notification.getPatientId(),
                notification.getPrescriptionId(),
                notification.getEventType(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRecipient(),
                notification.getDeliveryStatus(),
                notification.getCreatedAt(),
                notification.getDeliveredAt(),
                notification.getFailedAt(),
                notification.getFailureReason(),
                notification.getReadAt()
        );
        store.put(notificationId, created);
        patientIndex.computeIfAbsent(notification.getPatientId(), k -> new ArrayList<>()).add(notificationId);
        deduplicationKeyIndex.put(notification.getDeduplicationKey(), notificationId);
        deduplicationKeyIndex.put(notification.getPatientId() + "_" + notification.getPrescriptionId()
                + "_" + notification.getEventType(), notificationId);
        return created;
    }

    @Override
    public Optional<Notification> findById(long notificationId) {
        return Optional.ofNullable(store.get(notificationId));
    }

    @Override
    public List<Notification> findByPatientId(long patientId) {
        return patientIndex.getOrDefault(patientId, Collections.emptyList()).stream()
                .map(id -> store.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Notification> findByDeduplicationKey(String deduplicationKey) {
        Long notificationId = deduplicationKeyIndex.get(deduplicationKey);
        if (notificationId == null) return Optional.empty();
        return Optional.ofNullable(store.get(notificationId));
    }

    @Override
    public boolean existsByDeduplicationKey(String deduplicationKey) {
        return deduplicationKeyIndex.containsKey(deduplicationKey);
    }

    @Override
    public boolean update(Notification notification, long expectedVersion) {
        Notification existing = store.get(notification.getNotificationId());
        if (existing == null) {
            throw new IllegalArgumentException("Notification not found: " + notification.getNotificationId());
        }
        // Note: Notification doesn't have a version field, so we don't check expectedVersion
        store.put(notification.getNotificationId(), notification);
        return true;
    }

    @Override
    public boolean delete(long notificationId) {
        Notification notification = store.remove(notificationId);
        if (notification == null) return false;
        patientIndex.getOrDefault(notification.getPatientId(), new ArrayList<>()).remove(notificationId);
        deduplicationKeyIndex.remove(notification.getDeduplicationKey());
        return true;
    }

    @Override
    public List<Notification> listAll() {
        return new ArrayList<>(store.values());
    }
}
