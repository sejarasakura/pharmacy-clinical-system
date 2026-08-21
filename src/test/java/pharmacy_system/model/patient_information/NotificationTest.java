package pharmacy_system.model.patient_information;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Notification} model.
 *
 * <p>Tests deduplication key generation, state transitions, validation,
 * and SUPPORTED_EVENT_TYPES constant.</p>
 *
 * <p>Validates: Requirements 7.1, 7.2, 7.3, 7.5</p>
 */
class NotificationTest {

    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testTimestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
    }

    @Test
    @DisplayName("constructor creates notification in unread pending state")
    void constructor_createsNotificationInUnreadPendingState() {
        Notification notification = new Notification(
            100L, 200L, 300L, "PRESCRIPTION_ISSUED", "ALERT", 
            "Prescription Issued", "Your prescription has been issued", "patient@example.com");
        
        assertEquals(100L, notification.getNotificationId());
        assertEquals(200L, notification.getPatientId());
        assertEquals(300L, notification.getPrescriptionId());
        assertEquals("PRESCRIPTION_ISSUED", notification.getEventType());
        assertEquals("ALERT", notification.getNotificationType());
        assertEquals("Prescription Issued", notification.getTitle());
        assertEquals("Your prescription has been issued", notification.getMessage());
        assertEquals("patient@example.com", notification.getRecipient());
        
        assertEquals(Notification.DELIVERY_STATUS_PENDING, notification.getDeliveryStatus());
        assertTrue(notification.isPending());
        assertFalse(notification.isDelivered());
        assertFalse(notification.isDeliveryFailed());
        
        assertTrue(notification.isUnread());
        assertFalse(notification.isRead());
        assertNull(notification.getReadAt());
        
        assertNotNull(notification.getCreatedAt());
        assertNull(notification.getDeliveredAt());
        assertNull(notification.getFailedAt());
        assertNull(notification.getFailureReason());
        
        // Verify deduplication key
        String expectedKey = "200:300:PRESCRIPTION_ISSUED";
        assertEquals(expectedKey, notification.getDeduplicationKey());
        assertEquals(expectedKey, notification.generateDeduplicationKey());
    }

    @Test
    @DisplayName("full constructor reconstructs persisted notification")
    void fullConstructor_reconstructsPersistedNotification() {
        LocalDateTime deliveredAt = testTimestamp.plusMinutes(5);
        LocalDateTime readAt = testTimestamp.plusMinutes(10);
        
        Notification notification = new Notification(
            100L, 200L, 300L, "MEDICATION_DISPENSED", "DISPENSE", 
            "Medication Dispensed", "Your medication is ready for collection", "patient@example.com",
            Notification.DELIVERY_STATUS_DELIVERED, testTimestamp,
            deliveredAt, null, null, readAt);
        
        assertEquals(100L, notification.getNotificationId());
        assertEquals(Notification.DELIVERY_STATUS_DELIVERED, notification.getDeliveryStatus());
        assertTrue(notification.isDelivered());
        assertEquals(deliveredAt, notification.getDeliveredAt());
        assertEquals(testTimestamp, notification.getCreatedAt());
        assertEquals(readAt, notification.getReadAt());
        assertTrue(notification.isRead());
        assertFalse(notification.isUnread());
    }

    @Test
    @DisplayName("buildDeduplicationKey generates deterministic key")
    void buildDeduplicationKey_generatesDeterministicKey() {
        String key = Notification.buildDeduplicationKey(200L, 300L, "PRESCRIPTION_CANCELLED");
        
        assertEquals("200:300:PRESCRIPTION_CANCELLED", key);
        
        // Same inputs should produce same key
        String key2 = Notification.buildDeduplicationKey(200L, 300L, "PRESCRIPTION_CANCELLED");
        assertEquals(key, key2);
        
        // Different inputs should produce different keys
        String key3 = Notification.buildDeduplicationKey(201L, 300L, "PRESCRIPTION_CANCELLED");
        assertNotEquals(key, key3);
    }

    @Test
    @DisplayName("generateDeduplicationKey matches instance deduplicationKey")
    void generateDeduplicationKey_matchesInstanceDeduplicationKey() {
        Notification notification = new Notification(
            100L, 200L, 300L, "MEDICATION_PREPARING", "ALERT",
            "Medication Preparing", "Your medication is being prepared", "patient@example.com");
        
        String generatedKey = notification.generateDeduplicationKey();
        String storedKey = notification.getDeduplicationKey();
        
        assertEquals(storedKey, generatedKey);
        assertEquals("200:300:MEDICATION_PREPARING", generatedKey);
    }

    @Test
    @DisplayName("markDelivered transitions to delivered state")
    void markDelivered_transitionsToDeliveredState() {
        Notification notification = createTestNotification();
        
        assertTrue(notification.isPending());
        assertNull(notification.getDeliveredAt());
        assertNull(notification.getFailureReason());
        
        notification.markDelivered();
        
        assertEquals(Notification.DELIVERY_STATUS_DELIVERED, notification.getDeliveryStatus());
        assertTrue(notification.isDelivered());
        assertFalse(notification.isPending());
        assertFalse(notification.isDeliveryFailed());
        
        assertNotNull(notification.getDeliveredAt());
        assertNull(notification.getFailedAt());
        assertNull(notification.getFailureReason());
    }

    @Test
    @DisplayName("markDelivered clears previous failure state")
    void markDelivered_clearsPreviousFailureState() {
        Notification notification = createTestNotification();
        notification.markDeliveryFailed("Network error");
        
        assertTrue(notification.isDeliveryFailed());
        assertNotNull(notification.getFailedAt());
        assertNotNull(notification.getFailureReason());
        
        notification.markDelivered();
        
        assertTrue(notification.isDelivered());
        assertFalse(notification.isDeliveryFailed());
        assertNull(notification.getFailedAt());
        assertNull(notification.getFailureReason());
        assertNotNull(notification.getDeliveredAt());
    }

    @Test
    @DisplayName("markDeliveryFailed records failure with reason")
    void markDeliveryFailed_recordsFailureWithReason() {
        Notification notification = createTestNotification();
        String failureReason = "Recipient email bounced";
        
        notification.markDeliveryFailed(failureReason);
        
        assertEquals(Notification.DELIVERY_STATUS_FAILED, notification.getDeliveryStatus());
        assertTrue(notification.isDeliveryFailed());
        assertFalse(notification.isPending());
        assertFalse(notification.isDelivered());
        
        assertNotNull(notification.getFailedAt());
        assertEquals(failureReason, notification.getFailureReason());
        assertNull(notification.getDeliveredAt());
    }

    @Test
    @DisplayName("markDeliveryFailed with null reason is allowed")
    void markDeliveryFailed_withNullReason_isAllowed() {
        Notification notification = createTestNotification();
        
        notification.markDeliveryFailed(null);
        
        assertEquals(Notification.DELIVERY_STATUS_FAILED, notification.getDeliveryStatus());
        assertTrue(notification.isDeliveryFailed());
        assertNull(notification.getFailureReason());
    }

    @Test
    @DisplayName("markRead transitions from unread to read state")
    void markRead_transitionsFromUnreadToReadState() {
        Notification notification = createTestNotification();
        
        assertTrue(notification.isUnread());
        assertFalse(notification.isRead());
        assertNull(notification.getReadAt());
        
        notification.markRead();
        
        assertTrue(notification.isRead());
        assertFalse(notification.isUnread());
        assertNotNull(notification.getReadAt());
    }

    @Test
    @DisplayName("markRead on already read notification retains original read timestamp")
    void markRead_onAlreadyReadNotification_retainsOriginalReadTimestamp() {
        Notification notification = createTestNotification();
        notification.markRead();
        
        LocalDateTime firstReadAt = notification.getReadAt();
        assertNotNull(firstReadAt);
        
        // Wait a bit
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        notification.markRead();
        
        assertEquals(firstReadAt, notification.getReadAt());
        assertTrue(notification.isRead());
    }

    @Test
    @DisplayName("markNotDeliverable marks as failed with reason")
    void markNotDeliverable_marksAsFailedWithReason() {
        Notification notification = createTestNotification();
        String reason = "Inactive patient account";
        
        notification.markNotDeliverable(reason);
        
        assertEquals(Notification.DELIVERY_STATUS_FAILED, notification.getDeliveryStatus());
        assertTrue(notification.isDeliveryFailed());
        assertEquals(reason, notification.getFailureReason());
        assertNotNull(notification.getFailedAt());
    }

    @Test
    @DisplayName("isSupportedEventType returns true for supported events")
    void isSupportedEventType_returnsTrueForSupportedEvents() {
        Set<String> supportedEvents = Notification.SUPPORTED_EVENT_TYPES;
        
        for (String eventType : supportedEvents) {
            assertTrue(Notification.isSupportedEventType(eventType), 
                      "Event type should be supported: " + eventType);
        }
    }

    @Test
    @DisplayName("isSupportedEventType returns false for unsupported events")
    void isSupportedEventType_returnsFalseForUnsupportedEvents() {
        assertFalse(Notification.isSupportedEventType("UNKNOWN_EVENT"));
        assertFalse(Notification.isSupportedEventType("PRESCRIPTION_EDITED"));
        assertFalse(Notification.isSupportedEventType("STOCK_LOW"));
        assertFalse(Notification.isSupportedEventType(null));
        assertFalse(Notification.isSupportedEventType(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PRESCRIPTION_ISSUED", "PRESCRIPTION_CANCELLED", 
                           "MEDICATION_PREPARING", "MEDICATION_READY_FOR_COLLECTION", 
                           "MEDICATION_DISPENSED"})
    @DisplayName("SUPPORTED_EVENT_TYPES contains all required event types")
    void supportedEventTypes_containsAllRequiredEventTypes(String eventType) {
        assertTrue(Notification.SUPPORTED_EVENT_TYPES.contains(eventType),
                  "Event type should be in SUPPORTED_EVENT_TYPES: " + eventType);
    }

    @Test
    @DisplayName("SUPPORTED_EVENT_TYPES constant is immutable")
    void supportedEventTypes_constantIsImmutable() {
        Set<String> supportedEvents = Notification.SUPPORTED_EVENT_TYPES;
        
        assertEquals(5, supportedEvents.size());
        
        // Attempt to modify should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, 
                    () -> supportedEvents.add("NEW_EVENT"));
        assertThrows(UnsupportedOperationException.class, 
                    () -> supportedEvents.remove("PRESCRIPTION_ISSUED"));
    }

    @Test
    @DisplayName("validateRecipient returns true for active patient with non-blank recipient")
    void validateRecipient_returnsTrueForActivePatientWithNonBlankRecipient() {
        Notification notification = new Notification(
            100L, 200L, 300L, "PRESCRIPTION_ISSUED", "ALERT",
            "Test", "Test message", "patient@example.com");
        
        assertTrue(notification.validateRecipient(true));
    }

    @Test
    @DisplayName("validateRecipient returns false for inactive patient")
    void validateRecipient_returnsFalseForInactivePatient() {
        Notification notification = createTestNotification();
        
        assertFalse(notification.validateRecipient(false));
    }

    @Test
    @DisplayName("validateRecipient returns false for blank recipient")
    void validateRecipient_returnsFalseForBlankRecipient() {
        Notification notification = new Notification(
            100L, 200L, 300L, "PRESCRIPTION_ISSUED", "ALERT",
            "Test", "Test message", "   ");
        
        assertFalse(notification.validateRecipient(true));
    }

    @Test
    @DisplayName("validateRecipient returns false for null recipient")
    void validateRecipient_returnsFalseForNullRecipient() {
        Notification notification = new Notification(
            100L, 200L, 300L, "PRESCRIPTION_ISSUED", "ALERT",
            "Test", "Test message", null);
        
        assertFalse(notification.validateRecipient(true));
    }

    @Test
    @DisplayName("delivery status constants are correctly defined")
    void deliveryStatusConstants_areCorrectlyDefined() {
        assertEquals("PENDING", Notification.DELIVERY_STATUS_PENDING);
        assertEquals("DELIVERED", Notification.DELIVERY_STATUS_DELIVERED);
        assertEquals("FAILED", Notification.DELIVERY_STATUS_FAILED);
    }

    @Test
    @DisplayName("notification with zero ID is valid (for new notifications)")
    void notificationWithZeroId_isValid() {
        Notification notification = new Notification(
            0L, 200L, 300L, "PRESCRIPTION_ISSUED", "ALERT",
            "Test", "Test message", "patient@example.com");
        
        assertEquals(0L, notification.getNotificationId());
        assertTrue(notification.isPending());
        assertTrue(notification.isUnread());
    }

    @Test
    @DisplayName("notification state methods work correctly together")
    void notificationStateMethods_workCorrectlyTogether() {
        Notification notification = createTestNotification();
        
        // Initial state
        assertTrue(notification.isPending());
        assertTrue(notification.isUnread());
        assertFalse(notification.isDelivered());
        assertFalse(notification.isDeliveryFailed());
        assertFalse(notification.isRead());
        
        // Mark as delivered
        notification.markDelivered();
        assertTrue(notification.isDelivered());
        assertFalse(notification.isPending());
        assertFalse(notification.isDeliveryFailed());
        
        // Mark as read
        notification.markRead();
        assertTrue(notification.isRead());
        assertFalse(notification.isUnread());
        
        // Should still be delivered
        assertTrue(notification.isDelivered());
    }

    private Notification createTestNotification() {
        return new Notification(
            100L, 200L, 300L, "PRESCRIPTION_ISSUED", "ALERT",
            "Test Notification", "This is a test notification", "test@example.com");
    }
}