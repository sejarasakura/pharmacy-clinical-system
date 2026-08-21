package pharmacy_system.storage.patient_information;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pharmacy_system.model.patient_information.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryNotificationStorageTest {
    private NotificationStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryNotificationStorage();
    }

    @Test
    void testCreateAndFindById() {
        Notification notif = new Notification(
                0, 100, 1, "PRESCRIPTION_ISSUED", "email", "Prescription Ready",
                "Your prescription is ready", "patient@example.com", "PENDING",
                LocalDateTime.now(), null, null, null, null
        );

        Notification created = storage.create(notif);
        assertEquals(1, created.getNotificationId());

        Optional<Notification> found = storage.findById(created.getNotificationId());
        assertTrue(found.isPresent());
        assertEquals(100, found.get().getPatientId());
    }

    @Test
    void testFindByPatientId() {
        Notification notif1 = new Notification(
                0, 200, 2, "MEDICATION_DISPENSED", "sms", "Medication Ready",
                "Pick up medication", "0555551234", "DELIVERED",
                LocalDateTime.now(), LocalDateTime.now(), null, null, null
        );
        Notification notif2 = new Notification(
                0, 200, 3, "PRESCRIPTION_EXPIRED", "email", "Prescription Expired",
                "Your prescription has expired", "patient@test.com", "PENDING",
                LocalDateTime.now(), null, null, null, null
        );

        storage.create(notif1);
        storage.create(notif2);

        List<Notification> patientNotifs = storage.findByPatientId(200);
        assertEquals(2, patientNotifs.size());
    }

    @Test
    void testFindByDeduplicationKey() {
        String deduplicationKey = "100_1_PRESCRIPTION_ISSUED";
        Notification notif = new Notification(
                0, 100, 1, "PRESCRIPTION_ISSUED", "email", "Prescription Ready",
                "Your prescription is ready", "patient@example.com", "DELIVERED",
                LocalDateTime.now(), LocalDateTime.now(), null, null, null
        );

        Notification created = storage.create(notif);
        
        Optional<Notification> found = storage.findByDeduplicationKey(deduplicationKey);
        assertTrue(found.isPresent());
        assertEquals("PRESCRIPTION_ISSUED", found.get().getEventType());
    }

    @Test
    void testExistsByDeduplicationKey() {
        String deduplicationKey = "300_5_MEDICATION_DISPENSED";
        Notification notif = new Notification(
                0, 300, 5, "MEDICATION_DISPENSED", "email", "Medication Dispensed",
                "Your medication has been dispensed", "patient@example.com", "DELIVERED",
                LocalDateTime.now(), LocalDateTime.now(), null, null, null
        );

        storage.create(notif);

        assertTrue(storage.existsByDeduplicationKey(deduplicationKey));
        assertFalse(storage.existsByDeduplicationKey("999_999_NONEXISTENT"));
    }

    @Test
    void testUpdate() {
        Notification notif = new Notification(
                0, 400, 6, "PRESCRIPTION_ISSUED", "email", "Prescription Ready",
                "Your prescription is ready", "patient@example.com", "PENDING",
                LocalDateTime.now(), null, null, null, null
        );

        Notification created = storage.create(notif);

        Notification updated = new Notification(
                created.getNotificationId(), 400, 6, "PRESCRIPTION_ISSUED", "email", "Prescription Ready",
                "Your prescription is ready", "patient@example.com", "DELIVERED",
                LocalDateTime.now(), LocalDateTime.now(), null, null, null
        );

        boolean success = storage.update(updated, 1);
        assertTrue(success);

        Optional<Notification> retrieved = storage.findById(created.getNotificationId());
        assertEquals("DELIVERED", retrieved.get().getDeliveryStatus());
    }

    @Test
    void testDelete() {
        Notification notif = new Notification(
                0, 500, 7, "PRESCRIPTION_ISSUED", "email", "Prescription Ready",
                "Your prescription is ready", "patient@example.com", "PENDING",
                LocalDateTime.now(), null, null, null, null
        );

        Notification created = storage.create(notif);

        boolean deleted = storage.delete(created.getNotificationId());
        assertTrue(deleted);

        Optional<Notification> found = storage.findById(created.getNotificationId());
        assertFalse(found.isPresent());
    }

    @Test
    void testListAll() {
        storage.create(new Notification(
                0, 600, 8, "EVENT1", "email", "Title1", "Msg1", "test@example.com",
                "PENDING", LocalDateTime.now(), null, null, null, null
        ));
        storage.create(new Notification(
                0, 600, 9, "EVENT2", "sms", "Title2", "Msg2", "0555551111",
                "DELIVERED", LocalDateTime.now(), LocalDateTime.now(), null, null, null
        ));

        List<Notification> all = storage.listAll();
        assertEquals(2, all.size());
    }

    @Test
    void testDeduplicationKeySupport() {
        // Test that deduplication key is properly generated from patientId + prescriptionId + eventType
        Notification notif1 = new Notification(
                0, 700, 10, "MEDICATION_DISPENSED", "email", "Title", "Message",
                "patient@example.com", "DELIVERED", LocalDateTime.now(), LocalDateTime.now(), null, null, null
        );
        Notification notif2 = new Notification(
                0, 700, 11, "MEDICATION_DISPENSED", "email", "Title", "Message",
                "patient@example.com", "DELIVERED", LocalDateTime.now(), LocalDateTime.now(), null, null, null
        );

        storage.create(notif1);
        storage.create(notif2);

        // Both should be retrievable by their respective dedup keys
        Optional<Notification> found1 = storage.findByDeduplicationKey("700_10_MEDICATION_DISPENSED");
        Optional<Notification> found2 = storage.findByDeduplicationKey("700_11_MEDICATION_DISPENSED");

        assertTrue(found1.isPresent());
        assertTrue(found2.isPresent());
        assertNotEquals(found1.get().getNotificationId(), found2.get().getNotificationId());
    }
}
